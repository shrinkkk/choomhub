import { test } from 'node:test';
import assert from 'node:assert/strict';
import { Channels, MAX_DETAIL, compose, fcmMessage, parseRequest } from '../../src/events.js';

test('parseRequest accepts known events and rejects everything else', () => {
  assert.deepEqual(parseRequest({ event: 'meetup_cleared' }), { event: 'meetup_cleared', detail: '', kind: null });
  assert.deepEqual(parseRequest({ event: 'gesture', kind: 'hug' }), { event: 'gesture', detail: '', kind: 'hug' });
  assert.equal(parseRequest({ event: 'gesture' }), null);
  assert.equal(parseRequest({ event: 'gesture', kind: 'slap' }), null);
  assert.equal(parseRequest({ event: 'mood' }), null);
  assert.equal(parseRequest({ event: 'mood', kind: '   ' }), null);
  assert.deepEqual(parseRequest({ event: 'mood', kind: ' Angry  bcs parents ', detail: ' rain again ' }), { event: 'mood', detail: 'rain again', kind: 'Angry bcs parents' });
  assert.equal(parseRequest({ event: 'mood', kind: 'x'.repeat(60) }).kind.length, 24);
  assert.equal(parseRequest({ event: 'launch_missiles' }), null);
  assert.equal(parseRequest({ event: 'toString' }), null);
  assert.equal(parseRequest({ event: 'meetup_set', detail: 42 }), null);
  assert.equal(parseRequest(null), null);
  assert.equal(parseRequest([]), null);
  assert.equal(parseRequest('meetup_set'), null);
});

test('detail is only kept where the event uses it, and is cleaned and capped', () => {
  assert.equal(parseRequest({ event: 'meetup_set', detail: '  Sat 14 Dec,\n7:30 pm ' }).detail, 'Sat 14 Dec, 7:30 pm');
  assert.equal(parseRequest({ event: 'meetup_set', detail: 'x'.repeat(500) }).detail.length, MAX_DETAIL);
  assert.equal(parseRequest({ event: 'test', detail: 'ignored' }).detail, '');
});

test('compose uses the name the recipient gave the sender, with a plain fallback', () => {
  assert.equal(compose('meetup_set', { senderName: 'Shri', detail: 'Sat, 7:30 pm' }).title, 'Shri set the next meetup');
  assert.equal(compose('meetup_set', { senderName: 'Shri', detail: 'Sat, 7:30 pm' }).body, 'Sat, 7:30 pm');
  assert.equal(compose('meetup_cleared', {}).title, 'Your partner cleared the next meetup');
  assert.equal(compose('renamed', { senderName: 'Shri', recipientNewName: 'Raven' }).body, 'You\'re "Raven" to them now.');
  assert.equal(compose('partner_joined', { senderName: '' }).title, 'Your partner joined your space');
});

test('gestures and moods read plainly', () => {
  assert.deepEqual(compose('gesture', { senderName: 'Shri', kind: 'hug' }), { title: 'Shri sent you a hug', body: 'Consider yourself hugged.' });
  assert.equal(compose('gesture', { senderName: 'Shri', kind: 'tomato' }).title, 'Shri delivered a tomato');
  assert.deepEqual(compose('mood', { senderName: 'Wren', kind: 'Missing you', detail: 'rain again' }), { title: 'Wren updated their mood', body: 'Missing you \u00b7 rain again' });
  assert.equal(compose('mood', { senderName: 'Wren', kind: 'Calm', detail: '' }).body, 'Calm');
});

test('snaps carry their caption', () => {
  assert.deepEqual(compose('snap', { senderName: 'Wren', detail: 'crows again' }), { title: 'Wren sent a snap', body: 'crows again' });
  assert.equal(compose('snap', { senderName: 'Wren', detail: '' }).body, 'Open ChoomHub to see it.');
  const snapMessage = fcmMessage('f', 'snap', { title: 'Wren sent a snap', body: 'crows' }).message;
  assert.equal(snapMessage.notification, undefined, 'snaps are data-only so the app can refresh its widget');
  assert.deepEqual(snapMessage.data, { event: 'snap', category: 'snaps', title: 'Wren sent a snap', body: 'crows', channel: 'snaps', tag: 'snap' });
  assert.equal(snapMessage.android.priority, 'high');
});

test('points and reactions', () => {
  assert.deepEqual(parseRequest({ event: 'points', kind: '10', detail: 'dishes' }), { event: 'points', detail: 'dishes', kind: '10' });
  assert.equal(parseRequest({ event: 'points', kind: '0' }), null);
  assert.equal(parseRequest({ event: 'points', kind: '11' }), null);
  assert.equal(parseRequest({ event: 'points', kind: '-11' }), null);
  assert.equal(parseRequest({ event: 'points', kind: '1e2' }), null);
  assert.equal(parseRequest({ event: 'points', kind: '-3' }).kind, '-3');
  assert.deepEqual(compose('points', { senderName: 'Shri', kind: '10', detail: 'dishes', recipientRole: 'Good Girl' }), { title: 'Shri awarded you 10 Good Girl points', body: 'dishes' });
  assert.equal(compose('points', { senderName: 'Shri', kind: '1', detail: '' }).title, 'Shri awarded you 1 point');
  assert.deepEqual(compose('points', { senderName: 'Shri', kind: '-3', detail: '', recipientRole: 'Good Girl' }), { title: 'Shri took away 3 of your Good Girl points', body: 'No reason given.' });
  assert.equal(parseRequest({ event: 'snap_reaction', kind: 'lol' }), null);
  assert.equal(parseRequest({ event: 'snap_reaction', kind: '1' }), null);
  assert.equal(parseRequest({ event: 'snap_reaction', kind: '\u{1F5A4}x' }), null);
  for (const e of ['\u{1F5A4}', '\u{1F622}', '\u{1F62D}', '\u{1F525}', '\u2764\uFE0F', '\u{1F469}\u200D\u{1F4BB}', '\u{1F1EE}\u{1F1F3}', '\u{1F44D}\u{1F3FD}', '1\uFE0F\u20E3']) {
    assert.equal(parseRequest({ event: 'snap_reaction', kind: e })?.kind, e, e);
  }
  assert.equal(compose('snap_reaction', { senderName: 'Wren', kind: '\u{1F5A4}' }).title, 'Wren reacted \u{1F5A4} to your snap');
  const a = fcmMessage('f', 'points', { title: 't', body: 'b' }, '5').message.android.notification;
  assert.equal(a.channel_id, 'points');
  assert.match(a.tag, /^points_/);
});

test('each gesture kind gets its own tag', () => {
  const hug = fcmMessage('f', 'gesture', { title: 't', body: 'b' }, 'hug').message;
  assert.equal(hug.android.notification.tag, 'gesture_hug');
  assert.equal(hug.android.notification.channel_id, 'gestures');
  assert.equal(hug.data.kind, 'hug');
});

test('fcmMessage targets the installation and the right channel', () => {
  const m = fcmMessage('fid123', 'meetup_set', { title: 't', body: 'b' }).message;
  assert.equal(m.fid, 'fid123');
  assert.equal(m.token, undefined);
  assert.equal(m.android.notification.channel_id, Channels.meetup);
  assert.equal(m.android.notification.tag, 'meetup');
  assert.deepEqual(m.data, { event: 'meetup_set', category: 'meetup' });
  assert.equal(fcmMessage('f', 'test', { title: 't', body: 'b' }).message.data.category, 'general');
});

test('focus room events go on the focus channel and read plainly', () => {
  // Entering is quiet now; only nudges notify.
  assert.equal(parseRequest({ event: 'focus_enter' }), null);
  assert.equal(compose('focus_nudge', { senderName: 'Wren' }).title, 'Wren wants you in the focus room');
  assert.equal(compose('jam', { senderName: 'Wren' }).title, 'Wren left a jam in the record player');
  const msg = fcmMessage('fid1', 'focus_nudge', compose('focus_nudge', {}));
  assert.equal(msg.message.android.notification.channel_id, 'focus');
  assert.equal(msg.message.data.category, 'focus');
});

test('pet events ask for help and share name ideas', () => {
  assert.deepEqual(parseRequest({ event: 'pet_help', kind: 'vet', detail: 'Mochi' }), { event: 'pet_help', detail: 'Mochi', kind: 'vet' });
  assert.equal(parseRequest({ event: 'pet_help', kind: 'nap' }), null);
  assert.equal(compose('pet_help', { senderName: 'Wren', kind: 'vet', detail: 'Mochi' }).title, 'Wren needs your help taking Mochi to the vet');
  assert.equal(compose('pet_help', { senderName: 'Wren', kind: 'bath', detail: 'Mochi' }).title, 'Wren needs your help giving Mochi a bath');
  assert.match(compose('pet_named', { senderName: 'Rook', detail: 'Mochi' }).body, /Mochi/);
  assert.equal(fcmMessage('f', 'pet_named', compose('pet_named', {})).message.android.notification.channel_id, 'pet');
});

test('points shop events name what happened and keep each kind separate', () => {
  assert.deepEqual(parseRequest({ event: 'shop', kind: 'bought', detail: 'Head massage' }), { event: 'shop', detail: 'Head massage', kind: 'bought' });
  assert.equal(parseRequest({ event: 'shop', kind: 'steal' }), null);
  assert.equal(parseRequest({ event: 'shop' }), null);
  assert.deepEqual(compose('shop', { senderName: 'Rook', kind: 'used', detail: 'Head massage' }), { title: 'Rook is cashing in a reward', body: 'Head massage' });
  assert.equal(compose('shop', { senderName: 'Rook', kind: 'suggest_price', detail: 'Playlist · 20 points' }).title, 'Rook suggested a new price');
  const msg = fcmMessage('f', 'shop', compose('shop', { kind: 'bought' }), 'bought').message;
  assert.equal(msg.android.notification.channel_id, 'points');
  assert.equal(msg.android.notification.tag, 'shop_bought');
});

test('songs: sending one, and the rating it got', () => {
  assert.deepEqual(parseRequest({ event: 'song', detail: 'Velvet Hours · The Paper Moths' }), { event: 'song', detail: 'Velvet Hours · The Paper Moths', kind: null });
  assert.deepEqual(compose('song_rated', { senderName: 'Jas', kind: '8', detail: 'Velvet Hours' }), { title: 'Jas rated your song 8/10', body: 'Velvet Hours' });
  assert.equal(parseRequest({ event: 'song_rated', kind: '11', detail: 'x' }), null);
  assert.equal(parseRequest({ event: 'song_rated', kind: '0' }), null);
  assert.equal(fcmMessage('f', 'song', compose('song', {})).message.android.notification.channel_id, 'songs');
});

test('songs: albums and playlists say so', () => {
  assert.deepEqual(parseRequest({ event: 'song', kind: 'album', detail: 'Global Warming · Pitbull' }), { event: 'song', detail: 'Global Warming · Pitbull', kind: 'album' });
  assert.equal(parseRequest({ event: 'song', kind: 'artist' }), null);
  assert.equal(compose('song', { senderName: 'Jas', kind: 'album' }).title, 'Jas sent you an album');
  assert.equal(compose('song', { senderName: 'Jas', kind: 'playlist' }).title, 'Jas sent you a playlist');
  assert.equal(compose('song', { senderName: 'Jas' }).title, 'Jas sent you a song');
  assert.equal(parseRequest({ event: 'song_rated', kind: 'playlist:7' }).kind, 'playlist:7');
  assert.equal(parseRequest({ event: 'song_rated', kind: 'playlist:11' }), null);
  assert.equal(compose('song_rated', { senderName: 'Jas', kind: 'playlist:7', detail: 'Mix' }).title, 'Jas rated your playlist 7/10');
  assert.equal(compose('song_rated', { senderName: 'Jas', kind: '7' }).title, 'Jas rated your song 7/10');
});

test('focus rate, bounties, collection deletes and meetup logs', () => {
  assert.deepEqual(parseRequest({ event: 'focus_rate', kind: 'suggest', detail: '3' }), { event: 'focus_rate', detail: '3', kind: 'suggest' });
  assert.equal(parseRequest({ event: 'focus_rate', kind: 'maybe', detail: '3' }), null);
  assert.equal(compose('focus_rate', { senderName: 'Jas', kind: 'suggest', detail: '3' }).title, 'Jas suggested 3 points per focus hour');
  assert.equal(compose('focus_rate', { senderName: 'Jas', kind: 'agreed', detail: '0' }).title, 'Jas agreed: no points per focus hour');
  assert.equal(compose('bounty', { senderName: 'Jas', kind: 'paid', detail: 'Dishes' }).title, 'Jas paid your bounty');
  assert.equal(compose('collection', { senderName: 'Jas', kind: 'delete_ask', detail: 'Goa' }).body, '"Goa". It\'s only deleted if you agree.');
  assert.equal(compose('meetup_log', { senderName: 'Jas', kind: 'started' }).title, 'Jas started logging your meetup');
});
