// What the app may ask the Worker to send, and the words it sends. Kept free of
// I/O so it can be unit-tested. Copy is plain on purpose (see docs/DECISIONS.md).

/** Android notification channels; must match NotificationChannels.kt in the app. */
export const Channels = Object.freeze({
  gestures: 'gestures',
  snaps: 'snaps',
  points: 'points',
  moods: 'moods',
  meetup: 'meetup',
  space: 'space',
  focus: 'focus',
  pet: 'pet',
  songs: 'songs',
  general: 'general',
});

/**
 * True for one emoji (a single picture, with any joiners, variation selectors, skin tones,
 * flags or keycaps that make it up). Snap reactions can be any emoji; the Firestore rules
 * apply the same length cap.
 */
const PICTURES = [[0xA9, 0xA9], [0xAE, 0xAE], [0x203C, 0x203C], [0x2049, 0x2049], [0x2122, 0x2122], [0x2139, 0x2139], [0x2194, 0x2199], [0x21A9, 0x21AA], [0x231A, 0x231B], [0x2328, 0x2328], [0x23CF, 0x23CF], [0x23E9, 0x23F3], [0x23F8, 0x23FA], [0x24C2, 0x24C2], [0x25AA, 0x25AB], [0x25B6, 0x25B6], [0x25C0, 0x25C0], [0x25FB, 0x25FE], [0x2600, 0x27BF], [0x2934, 0x2935], [0x2B05, 0x2B07], [0x2B1B, 0x2B1C], [0x2B50, 0x2B50], [0x2B55, 0x2B55], [0x3030, 0x3030], [0x303D, 0x303D], [0x3297, 0x3297], [0x3299, 0x3299], [0x1F000, 0x1FAFF]];
const isPicture = (cp) => PICTURES.some(([a, b]) => cp >= a && cp <= b);
const isPart = (cp) => cp === 0x200d || cp === 0xfe0e || cp === 0xfe0f || cp === 0x20e3 || (cp >= 0xe0020 && cp <= 0xe007f)
  || (cp >= 0x30 && cp <= 0x39) || cp === 0x23 || cp === 0x2a;

export function isEmoji(text) {
  if (typeof text !== 'string' || text.length === 0 || text.length > 16) return false;
  let picture = false;
  for (const ch of text) {
    const cp = ch.codePointAt(0);
    if (isPicture(cp) || cp === 0x20e3) picture = true;
    else if (!isPart(cp)) return false;
  }
  return picture;
}

/** Gesture kinds the app may name; must match AffectionKind in the app. */
export const Gestures = Object.freeze(['hug', 'forehead_kiss', 'head_pats', 'tomato']);

/** What happened in the points shop; must match ShopPush in the app. */
export const ShopKinds = Object.freeze(['bought', 'used', 'done', 'suggest_add', 'suggest_price', 'suggest_remove', 'agreed', 'declined']);

/** Mood names are the couple's own (they can edit the list), so only their length is limited. */
export const MAX_MOOD_LABEL = 24;

/**
 * event -> { category: preference key or null (always on), channel, tag, toSelf, kinds }
 * `kinds`, when present, is the list of values the request's `kind` must be one of
 * (`'label'` means free text: a mood's name, cleaned and capped; `'amount'` a whole number,
 * 1-10 awarded or -1 to -10 taken away);
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
  // Data-only, so the app wakes up for it (to refresh the home-screen widget) and shows it itself.
  snap: { category: 'snaps', channel: Channels.snaps, tag: 'snap', toSelf: false, takesDetail: true, dataOnly: true },
  snap_reaction: { category: 'snaps', channel: Channels.snaps, tag: 'snap_reaction', toSelf: false, takesDetail: false, kinds: 'emoji' },
  points: { category: 'points', channel: Channels.points, tag: null, toSelf: false, takesDetail: true, kinds: 'amount' },
  mood: { category: 'moods', channel: Channels.moods, tag: 'mood', toSelf: false, takesDetail: true, kinds: 'label' },
  focus_nudge: { category: 'focus', channel: Channels.focus, tag: 'focus_nudge', toSelf: false, takesDetail: false },
  jam: { category: 'focus', channel: Channels.focus, tag: 'jam', toSelf: false, takesDetail: false },
  pet_help: { category: 'pet', channel: Channels.pet, tag: 'pet_help', toSelf: false, takesDetail: true, kinds: ['vet', 'bath'] },
  pet_named: { category: 'pet', channel: Channels.pet, tag: 'pet_named', toSelf: false, takesDetail: true },
  song: { category: 'songs', channel: Channels.songs, tag: 'song', toSelf: false, takesDetail: true },
  song_rated: { category: 'songs', channel: Channels.songs, tag: 'song_rated', toSelf: false, takesDetail: true, kinds: ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10'] },
  shop: { category: 'points', channel: Channels.points, tag: null, toSelf: false, takesDetail: true, kinds: ShopKinds },
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
  if (spec.kinds === 'amount') {
    if (typeof kind !== 'string' || !/^-?\d{1,2}$/.test(kind)) return null;
    const n = Number(kind);
    if (n === 0 || n < -10 || n > 10) return null;
    cleanKind = String(Number(kind));
  } else if (spec.kinds === 'emoji') {
    if (!isEmoji(kind)) return null;
    cleanKind = kind;
  } else if (spec.kinds === 'label') {
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
 * @param {{ senderName?: string | null, recipientNewName?: string | null, detail?: string, kind?: string | null, recipientRole?: string | null }} ctx
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
    case 'snap':
      return { title: `${who} sent a snap`, body: ctx.detail || 'Open ChoomHub to see it.' };
    case 'snap_reaction':
      return { title: `${who} reacted ${ctx.kind} to your snap`, body: 'Open ChoomHub to see it.' };
    case 'points': {
      const signed = Number(ctx.kind);
      const n = Math.abs(signed);
      const noun = `point${n === 1 ? '' : 's'}`;
      const body = ctx.detail || 'No reason given.';
      if (signed < 0) {
        const whose = ctx.recipientRole ? `of your ${ctx.recipientRole} ${noun}` : `of your ${noun}`;
        return { title: `${who} took away ${n} ${whose}`, body };
      }
      const what = ctx.recipientRole ? `${ctx.recipientRole} ${noun}` : noun;
      return { title: `${who} awarded you ${n} ${what}`, body };
    }
    case 'mood': {
      const label = clean(ctx.kind || '', MAX_MOOD_LABEL) || 'Something new';
      return { title: `${who} updated their mood`, body: ctx.detail ? `${label} \u00b7 ${ctx.detail}` : label };
    }
    case 'focus_nudge':
      return { title: `${who} wants you in the focus room`, body: "There's a desk waiting for you." };
    case 'song':
      return { title: `${who} sent you a song`, body: ctx.detail || 'Open ChoomHub to play it.' };
    case 'song_rated':
      return { title: `${who} rated your song ${ctx.kind}/10`, body: ctx.detail || 'Open ChoomHub to see it.' };
    case 'shop': {
      const body = ctx.detail || 'Open the points shop to see it.';
      const titles = {
        bought: `${who} bought a reward from you`,
        used: `${who} is cashing in a reward`,
        done: `${who} marked a reward as done`,
        suggest_add: `${who} suggested a new reward`,
        suggest_price: `${who} suggested a new price`,
        suggest_remove: `${who} suggested taking a reward out of the shop`,
        agreed: `${who} agreed to your shop change`,
        declined: `${who} turned down your shop change`,
      };
      return { title: titles[ctx.kind] || `${who} changed something in the points shop`, body };
    }
    case 'pet_help': {
      const cat = ctx.detail || 'the cat';
      return ctx.kind === 'vet'
        ? { title: `${who} needs your help taking ${cat} to the vet`, body: 'Open the Pet House to help.' }
        : { title: `${who} needs your help giving ${cat} a bath`, body: 'It takes two. Open the Pet House.' };
    }
    case 'pet_named':
      return { title: `${who} has a name for the cat`, body: ctx.detail ? `"${ctx.detail}". Open the Pet House to agree (or suggest another).` : 'Open the Pet House.' };
    case 'jam':
      return { title: `${who} left a jam in the record player`, body: 'Open the focus room to join it.' };
    default:
      throw new Error(`unknown event ${event}`);
  }
}

function dataOnlyMessage(fid, event, text) {
  const spec = Events[event];
  return {
    message: {
      fid,
      data: { event, category: spec.category ?? 'general', title: text.title, body: text.body, channel: spec.channel, tag: spec.tag },
      android: { priority: 'high', collapse_key: spec.tag },
    },
  };
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
  if (Events[event].dataOnly) return dataOnlyMessage(fid, event, text);
  const spec = Events[event];
  // Points stack (each award is its own notification); gestures collapse per kind.
  const tag = spec.tag ?? (event === 'points' ? `points_${Date.now()}` : `${event}_${kind}`);
  const kindData = kind && (Array.isArray(spec.kinds) || spec.kinds === 'emoji') ? { kind } : {};
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
