// What the app may ask the Worker to send, and the words it sends. Kept free of
// I/O so it can be unit-tested. Copy is plain on purpose (see docs/DECISIONS.md).

/** Android notification channels; must match NotificationChannels.kt in the app. */
export const Channels = Object.freeze({ gestures: 'gestures', moods: 'moods', meetup: 'meetup', space: 'space', general: 'general' });

/** Gesture kinds the app may name; must match AffectionKind in the app. */
export const Gestures = Object.freeze(['hug', 'forehead_kiss', 'head_pats', 'tomato']);

/** Mood names are the couple's own (they can edit the list), so only their length is limited. */
export const MAX_MOOD_LABEL = 24;

/**
 * event -> { category: preference key or null (always on), channel, tag, toSelf, kinds }
 * `kinds`, when present, is the list of values the request's `kind` must be one of
 * (`'label'` means free text: a mood's name, cleaned and capped);
 * a null tag means one per kind (`<event>_<kind>`), so a hug doesn't replace a tomato.
 * `toSelf` events go to the caller's own devices (the test button); everything
 * else goes only to the caller's partner.
 */
export const Events = Object.freeze({
  meetup_set: { category: 'meetup', channel: Channels.meetup, tag: 'meetup', toSelf: false, takesDetail: true },
  meetup_cleared: { category: 'meetup', channel: Channels.meetup, tag: 'meetup', toSelf: false, takesDetail: false },
  partner_joined: { category: 'space', channel: Channels.space, tag: 'space', toSelf: false, takesDetail: false },
  renamed: { category: 'space', channel: Channels.space, tag: 'renamed', toSelf: false, takesDetail: false },
  test: { category: null, channel: Channels.general, tag: 'test', toSelf: true, takesDetail: false },
  gesture: { category: 'gestures', channel: Channels.gestures, tag: null, toSelf: false, takesDetail: false, kinds: Gestures },
  mood: { category: 'moods', channel: Channels.moods, tag: 'mood', toSelf: false, takesDetail: true, kinds: 'label' },
});

export const MAX_DETAIL = 80;
const MAX_NAME = 24;

/**
 * Validates the request body. Returns `{ event, detail }` or null when invalid.
 * @param {unknown} body
 */
export function parseRequest(body) {
  if (!body || typeof body !== 'object' || Array.isArray(body)) return null;
  const { event, detail, kind } = /** @type {Record<string, unknown>} */ (body);
  if (typeof event !== 'string' || !Object.hasOwn(Events, event)) return null;
  if (detail !== undefined && detail !== null && typeof detail !== 'string') return null;
  const spec = Events[event];
  let cleanKind = null;
  if (spec.kinds === 'label') {
    cleanKind = typeof kind === 'string' ? clean(kind, MAX_MOOD_LABEL) : '';
    if (!cleanKind) return null;
  } else if (spec.kinds) {
    if (typeof kind !== 'string' || !spec.kinds.includes(kind)) return null;
    cleanKind = kind;
  }
  const cleanDetail = spec.takesDetail && typeof detail === 'string' ? clean(detail, MAX_DETAIL) : '';
  return { event, detail: cleanDetail, kind: cleanKind };
}

function clean(text, max) {
  // Strip control characters and collapse whitespace; notifications are one or two lines.
  const flat = text.replace(/[\u0000-\u001f\u007f]/g, ' ').replace(/\s+/g, ' ').trim();
  return [...flat].slice(0, max).join('');
}

/**
 * The notification text.
 * @param {string} event
 * @param {{ senderName?: string | null, recipientNewName?: string | null, detail?: string, kind?: string | null }} ctx
 *   senderName: what the recipient calls the sender; recipientNewName: what the sender now calls the recipient.
 */
export function compose(event, ctx) {
  const who = clean(ctx.senderName || '', MAX_NAME) || 'Your partner';
  switch (event) {
    case 'meetup_set':
      return { title: `${who} set the next meetup`, body: ctx.detail || 'Open ChoomHub to see when.' };
    case 'meetup_cleared':
      return { title: `${who} cleared the next meetup`, body: "Nothing's planned for now." };
    case 'partner_joined':
      return { title: `${who} joined your space`, body: "It's the two of you now." };
    case 'renamed': {
      const name = clean(ctx.recipientNewName || '', MAX_NAME);
      return { title: `${who} changed your name`, body: name ? `You're "${name}" to them now.` : 'Open ChoomHub to see it.' };
    }
    case 'test':
      return { title: 'Test notification', body: 'Notifications reach this phone.' };
    case 'gesture':
      return gestureText(who, ctx.kind);
    case 'mood': {
      const label = clean(ctx.kind || '', MAX_MOOD_LABEL) || 'Something new';
      return { title: `${who} updated their mood`, body: ctx.detail ? `${label} \u00b7 ${ctx.detail}` : label };
    }
    default:
      throw new Error(`unknown event ${event}`);
  }
}

function gestureText(who, kind) {
  switch (kind) {
    case 'hug':
      return { title: `${who} sent you a hug`, body: 'Consider yourself hugged.' };
    case 'forehead_kiss':
      return { title: `${who} sent you a forehead kiss`, body: 'Delivered to your forehead.' };
    case 'head_pats':
      return { title: `${who} gave you head pats`, body: 'Pat, pat.' };
    case 'tomato':
      return { title: `${who} delivered a tomato`, body: 'One tomato, freshly delivered.' };
    default:
      throw new Error(`unknown gesture ${kind}`);
  }
}

/** The FCM HTTP v1 message for one device. */
export function fcmMessage(fid, event, text, kind = null) {
  const spec = Events[event];
  const tag = spec.tag ?? `${event}_${kind}`;
  const kindData = kind && spec.kinds !== 'label' ? { kind } : {};
  return {
    message: {
      fid,
      notification: { title: text.title, body: text.body },
      data: { event, category: spec.category ?? 'general', ...kindData },
      android: {
        priority: 'high',
        collapse_key: tag,
        notification: { channel_id: spec.channel, tag, icon: 'ic_stat_choomhub', color: '#EDE6D6' },
      },
    },
  };
}
