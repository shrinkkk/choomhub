// What the app may ask the Worker to send, and the words it sends. Kept free of
// I/O so it can be unit-tested. Copy is plain on purpose (see docs/DECISIONS.md).

/** Android notification channels; must match NotificationChannels.kt in the app. */
export const Channels = Object.freeze({ meetup: 'meetup', space: 'space', general: 'general' });

/**
 * event -> { category: preference key or null (always on), channel, tag, toSelf }
 * `toSelf` events go to the caller's own devices (the test button); everything
 * else goes only to the caller's partner.
 */
export const Events = Object.freeze({
  meetup_set: { category: 'meetup', channel: Channels.meetup, tag: 'meetup', toSelf: false, takesDetail: true },
  meetup_cleared: { category: 'meetup', channel: Channels.meetup, tag: 'meetup', toSelf: false, takesDetail: false },
  partner_joined: { category: 'space', channel: Channels.space, tag: 'space', toSelf: false, takesDetail: false },
  renamed: { category: 'space', channel: Channels.space, tag: 'renamed', toSelf: false, takesDetail: false },
  test: { category: null, channel: Channels.general, tag: 'test', toSelf: true, takesDetail: false },
});

export const MAX_DETAIL = 80;
const MAX_NAME = 24;

/**
 * Validates the request body. Returns `{ event, detail }` or null when invalid.
 * @param {unknown} body
 */
export function parseRequest(body) {
  if (!body || typeof body !== 'object' || Array.isArray(body)) return null;
  const { event, detail } = /** @type {Record<string, unknown>} */ (body);
  if (typeof event !== 'string' || !Object.hasOwn(Events, event)) return null;
  if (detail !== undefined && detail !== null && typeof detail !== 'string') return null;
  const cleanDetail = Events[event].takesDetail && typeof detail === 'string' ? clean(detail, MAX_DETAIL) : '';
  return { event, detail: cleanDetail };
}

function clean(text, max) {
  // Strip control characters and collapse whitespace; notifications are one or two lines.
  const flat = text.replace(/[\u0000-\u001f\u007f]/g, ' ').replace(/\s+/g, ' ').trim();
  return [...flat].slice(0, max).join('');
}

/**
 * The notification text.
 * @param {string} event
 * @param {{ senderName?: string | null, recipientNewName?: string | null, detail?: string }} ctx
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
    default:
      throw new Error(`unknown event ${event}`);
  }
}

/** The FCM HTTP v1 message for one device. */
export function fcmMessage(fid, event, text) {
  const spec = Events[event];
  return {
    message: {
      fid,
      notification: { title: text.title, body: text.body },
      data: { event, category: spec.category ?? 'general' },
      android: {
        priority: 'high',
        collapse_key: spec.tag,
        notification: { channel_id: spec.channel, tag: spec.tag, icon: 'ic_stat_choomhub', color: '#EDE6D6' },
      },
    },
  };
}
