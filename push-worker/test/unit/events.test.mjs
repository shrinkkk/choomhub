import { test } from 'node:test';
import assert from 'node:assert/strict';
import { Channels, MAX_DETAIL, compose, fcmMessage, parseRequest } from '../../src/events.js';

test('parseRequest accepts known events and rejects everything else', () => {
  assert.deepEqual(parseRequest({ event: 'meetup_cleared' }), { event: 'meetup_cleared', detail: '' });
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

test('fcmMessage targets the installation and the right channel', () => {
  const m = fcmMessage('fid123', 'meetup_set', { title: 't', body: 'b' }).message;
  assert.equal(m.fid, 'fid123');
  assert.equal(m.token, undefined);
  assert.equal(m.android.notification.channel_id, Channels.meetup);
  assert.equal(m.android.notification.tag, 'meetup');
  assert.deepEqual(m.data, { event: 'meetup_set', category: 'meetup' });
  assert.equal(fcmMessage('f', 'test', { title: 't', body: 'b' }).message.data.category, 'general');
});
