// End-to-end test of the Worker's handler against the Firestore emulator.
// Only FCM is faked; Firestore reads/deletes and ID-token checks are real.
import { after, beforeEach, test } from 'node:test';
import assert from 'node:assert/strict';
import { handle } from '../../src/index.js';
import { idTokenClaims, makeKey, mint } from '../keys.mjs';

const PROJECT = 'demo-choomhub';
const EMULATOR = `http://${process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8080'}`;
const DOCS = `${EMULATOR}/v1/projects/${PROJECT}/databases/(default)/documents`;
const FCM = 'https://fcm.test';
const key = await makeKey('k1');

const toFields = (obj) =>
  Object.fromEntries(
    Object.entries(obj).map(([k, v]) => [
      k,
      Array.isArray(v) ? { arrayValue: { values: v.map((s) => ({ stringValue: s })) } } : typeof v === 'boolean' ? { booleanValue: v } : { stringValue: v },
    ]),
  );

async function put(path, data) {
  const r = await fetch(`${DOCS}/${path}`, { method: 'PATCH', headers: { authorization: 'Bearer owner' }, body: JSON.stringify({ fields: toFields(data) }) });
  assert.ok(r.ok, `seed ${path}: ${r.status}`);
}
async function exists(path) {
  return (await fetch(`${DOCS}/${path}`, { headers: { authorization: 'Bearer owner' } })).ok;
}

let sent;
let deadFids;
const fakeFetch = async (url, init) => {
  if (String(url).startsWith(FCM)) {
    const { message } = JSON.parse(init.body);
    if (deadFids.has(message.fid)) return new Response('{"error":{"status":"NOT_FOUND"}}', { status: 404 });
    sent.push(message);
    return new Response('{"name":"projects/x/messages/1"}', { status: 200 });
  }
  return fetch(url, init);
};
const deps = {
  fetch: fakeFetch,
  now: () => Date.now(),
  projectId: PROJECT,
  getJwks: async () => ({ keys: [key.jwk] }),
  getAccessToken: async () => 'owner',
  firestoreRoot: EMULATOR,
  fcmRoot: FCM,
};

async function notify(uid, body, { token } = {}) {
  const auth = token ?? (await mint(key, idTokenClaims(PROJECT, uid, Math.floor(Date.now() / 1000))));
  const request = new Request('https://push.example/v1/notify', {
    method: 'POST',
    headers: { authorization: `Bearer ${auth}`, 'content-type': 'application/json' },
    body: typeof body === 'string' ? body : JSON.stringify(body),
  });
  const response = await handle(request, {}, deps);
  return { status: response.status, body: await response.json() };
}

beforeEach(async () => {
  sent = [];
  deadFids = new Set();
  await fetch(`${EMULATOR}/emulator/v1/projects/${PROJECT}/databases/(default)/documents`, { method: 'DELETE' });
  await put('users/alice', { coupleId: 'c1' });
  await put('users/bob', { coupleId: 'c1' });
  await put('couples/c1', { members: ['alice', 'bob'] });
  await put('couples/c1/names/bob', { nameForPartner: 'Shri' }); // what bob calls alice
  await put('couples/c1/names/alice', { nameForPartner: 'Raven' }); // what alice calls bob
  await put('users/bob/devices/bobPhone', { platform: 'android' });
  await put('users/alice/devices/alicePhone', { platform: 'android' });
  // Someone outside the couple.
  await put('users/mallory', { coupleId: 'c2' });
  await put('couples/c2', { members: ['mallory'] });
});

after(() => {});

test("a meetup change reaches only the partner's phone, using the partner's name for the sender", async () => {
  const r = await notify('alice', { event: 'meetup_set', detail: 'Sat 14 Dec, 7:30 pm' });
  assert.deepEqual(r, { status: 200, body: { sent: 1, failed: 0 } });
  assert.equal(sent.length, 1);
  assert.equal(sent[0].fid, 'bobPhone');
  assert.equal(sent[0].notification.title, 'Shri set the next meetup');
  assert.equal(sent[0].notification.body, 'Sat 14 Dec, 7:30 pm');
  assert.equal(sent[0].android.notification.channel_id, 'meetup');
});

test('a hug reaches the partner on the gestures channel, and can be muted', async () => {
  const r = await notify('alice', { event: 'gesture', kind: 'hug' });
  assert.equal(r.body.sent, 1);
  assert.equal(sent[0].notification.title, 'Shri sent you a hug');
  assert.equal(sent[0].android.notification.channel_id, 'gestures');
  await put('users/bob/settings/notifications', { gestures: false });
  assert.deepEqual((await notify('alice', { event: 'gesture', kind: 'tomato' })).body, { sent: 0, reason: 'muted' });
  assert.equal((await notify('alice', { event: 'gesture', kind: 'slap' })).status, 400);
});

test('a mood change carries the mood and the note', async () => {
  await notify('alice', { event: 'mood', kind: 'Sleepy', detail: 'headphones on' });
  assert.equal(sent[0].notification.title, 'Shri updated their mood');
  assert.equal(sent[0].notification.body, 'Sleepy \u00b7 headphones on');
});

test('points name the role the recipient holds', async () => {
  await put('couples/c1/shared/roles', { goodBoy: 'alice', goodGirl: 'bob' });
  await notify('alice', { event: 'points', kind: '25', detail: 'for the playlist' });
  assert.equal(sent[0].notification.title, 'Shri gave you 25 Good Girl points');
  assert.equal(sent[0].notification.body, 'for the playlist');
});

test('renamed tells the partner their new name', async () => {
  await notify('alice', { event: 'renamed' });
  assert.equal(sent[0].notification.body, 'You\'re "Raven" to them now.');
});

test('the test event goes to your own phone', async () => {
  const r = await notify('alice', { event: 'test' });
  assert.equal(r.body.sent, 1);
  assert.equal(sent[0].fid, 'alicePhone');
});

test('a muted category is not sent', async () => {
  await put('users/bob/settings/notifications', { meetup: false, space: true });
  const r = await notify('alice', { event: 'meetup_cleared' });
  assert.deepEqual(r.body, { sent: 0, reason: 'muted' });
  assert.equal(sent.length, 0);
  assert.equal((await notify('alice', { event: 'renamed' })).body.sent, 1);
});

test('someone without a partner cannot send to anyone', async () => {
  const r = await notify('mallory', { event: 'meetup_set', detail: 'hi' });
  assert.equal(r.status, 403);
  assert.equal(sent.length, 0);
});

test("pointing your user doc at someone else's couple does not make you a member", async () => {
  await put('users/mallory', { coupleId: 'c1' });
  const r = await notify('mallory', { event: 'meetup_set' });
  assert.equal(r.status, 403);
  assert.equal(sent.length, 0);
});

test('a registration FCM reports as unregistered is removed', async () => {
  await put('users/bob/devices/oldPhone', { platform: 'android' });
  deadFids.add('oldPhone');
  const r = await notify('alice', { event: 'partner_joined' });
  assert.deepEqual(r.body, { sent: 1, failed: 1 });
  assert.equal(await exists('users/bob/devices/oldPhone'), false);
  assert.equal(await exists('users/bob/devices/bobPhone'), true);
});

test('no registered phones is reported, not an error', async () => {
  await fetch(`${DOCS}/users/bob/devices/bobPhone`, { method: 'DELETE', headers: { authorization: 'Bearer owner' } });
  assert.deepEqual((await notify('alice', { event: 'meetup_cleared' })).body, { sent: 0, reason: 'no-devices' });
});

test('bad tokens, bad bodies and other paths are refused', async () => {
  assert.equal((await notify('alice', { event: 'test' }, { token: 'nope' })).status, 401);
  const expired = await mint(key, idTokenClaims(PROJECT, 'alice', Math.floor(Date.now() / 1000) - 10_000));
  assert.equal((await notify('alice', { event: 'test' }, { token: expired })).status, 401);
  assert.equal((await notify('alice', { event: 'delete_everything' })).status, 400);
  assert.equal((await notify('alice', '{not json')).status, 400);
  assert.equal((await notify('alice', { event: 'test', detail: 'x'.repeat(3000) })).status, 413);
  const get = await handle(new Request('https://push.example/v1/notify'), {}, deps);
  assert.equal(get.status, 405);
  const other = await handle(new Request('https://push.example/admin', { method: 'POST' }), {}, deps);
  assert.equal(other.status, 404);
  assert.equal(sent.length, 0);
});
