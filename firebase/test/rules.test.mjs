// Firestore security rules tests. Run with: npm run test:rules (needs Java for the emulator).
import { after, before, beforeEach, describe, test } from 'node:test';
import { readFileSync } from 'node:fs';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  addDoc,
  arrayUnion,
  deleteField,
  Bytes,
  collection,
  deleteDoc,
  doc,
  getDocs,
  getDoc,
  serverTimestamp,
  setDoc,
  Timestamp,
  updateDoc,
  writeBatch,
} from 'firebase/firestore';

let env;
const HOURS = 3600 * 1000;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-choomhub',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});
after(async () => env.cleanup());
beforeEach(async () => env.clearFirestore());

const db = (uid) => env.authenticatedContext(uid).firestore();

/** Alice starts a space; returns its id. */
async function startSpace(uid = 'alice', code = 'RAVEN234', coupleId = 'c1') {
  const fs = db(uid);
  const batch = writeBatch(fs);
  batch.set(doc(fs, 'couples', coupleId), { members: [uid], createdBy: uid, createdAt: serverTimestamp(), inviteCode: code });
  batch.set(doc(fs, 'invites', code), {
    coupleId, createdBy: uid, usedBy: null, expiresAt: Timestamp.fromMillis(Date.now() + 48 * HOURS),
  });
  batch.set(doc(fs, 'users', uid), { coupleId, updatedAt: serverTimestamp() });
  await assertSucceeds(batch.commit());
  return coupleId;
}

function joinBatch(uid, code = 'RAVEN234', coupleId = 'c1') {
  const fs = db(uid);
  const batch = writeBatch(fs);
  batch.update(doc(fs, 'invites', code), { usedBy: uid });
  batch.update(doc(fs, 'couples', coupleId), { members: arrayUnion(uid) });
  batch.set(doc(fs, 'users', uid), { coupleId, updatedAt: serverTimestamp() });
  return batch;
}

describe('starting and joining a space', () => {
  test('anyone signed in can start a space with only themself in it', async () => {
    await startSpace();
    await assertSucceeds(getDoc(doc(db('alice'), 'couples', 'c1')));
  });

  test('you cannot start a space that already includes someone else', async () => {
    const fs = db('alice');
    const batch = writeBatch(fs);
    batch.set(doc(fs, 'couples', 'c2'), { members: ['alice', 'bob'], createdBy: 'alice', createdAt: serverTimestamp(), inviteCode: 'X1' });
    batch.set(doc(fs, 'invites', 'X1'), { coupleId: 'c2', createdBy: 'alice', usedBy: null, expiresAt: Timestamp.fromMillis(Date.now() + HOURS) });
    await assertFails(batch.commit());
  });

  test('signed-out users can do nothing', async () => {
    await startSpace();
    const anon = env.unauthenticatedContext().firestore();
    await assertFails(getDoc(doc(anon, 'invites', 'RAVEN234')));
    await assertFails(getDoc(doc(anon, 'couples', 'c1')));
  });

  test('a non-member cannot read the space, but can look up an invite by its code', async () => {
    await startSpace();
    await assertFails(getDoc(doc(db('bob'), 'couples', 'c1')));
    await assertSucceeds(getDoc(doc(db('bob'), 'invites', 'RAVEN234')));
  });

  test('the second person joins with the code and can then read the space', async () => {
    await startSpace();
    await assertSucceeds(joinBatch('bob').commit());
    const snap = await assertSucceeds(getDoc(doc(db('bob'), 'couples', 'c1')));
    if (JSON.stringify(snap.data().members) !== JSON.stringify(['alice', 'bob'])) throw new Error('members');
  });

  test('joining without consuming the invite is refused', async () => {
    await startSpace();
    await assertFails(updateDoc(doc(db('bob'), 'couples', 'c1'), { members: arrayUnion('bob') }));
  });

  test('a third person cannot join, even with the code', async () => {
    await startSpace();
    await assertSucceeds(joinBatch('bob').commit());
    await assertFails(joinBatch('carol').commit());
  });

  test('an expired invite cannot be used', async () => {
    await startSpace();
    await env.withSecurityRulesDisabled(async (ctx) => {
      await updateDoc(doc(ctx.firestore(), 'invites', 'RAVEN234'), { expiresAt: Timestamp.fromMillis(Date.now() - HOURS) });
    });
    await assertFails(joinBatch('bob').commit());
  });

  test('the creator cannot consume their own invite', async () => {
    await startSpace();
    const fs = db('alice');
    await assertFails(updateDoc(doc(fs, 'invites', 'RAVEN234'), { usedBy: 'alice' }));
  });

  test('you cannot point your user record at a space you are not in', async () => {
    await startSpace();
    await assertFails(setDoc(doc(db('mallory'), 'users', 'mallory'), { coupleId: 'c1', updatedAt: serverTimestamp() }));
  });

  test('user records are private', async () => {
    await startSpace();
    await assertFails(getDoc(doc(db('bob'), 'users', 'alice')));
  });

  test('invites cannot be created for someone else\'s space or last more than 3 days', async () => {
    await startSpace();
    await assertSucceeds(joinBatch('bob').commit());
    await assertFails(setDoc(doc(db('mallory'), 'invites', 'NEW1'), {
      coupleId: 'c1', createdBy: 'mallory', usedBy: null, expiresAt: Timestamp.fromMillis(Date.now() + HOURS),
    }));
    const fs = db('dave');
    const batch = writeBatch(fs);
    batch.set(doc(fs, 'couples', 'c9'), { members: ['dave'], createdBy: 'dave', createdAt: serverTimestamp(), inviteCode: 'LONG1' });
    batch.set(doc(fs, 'invites', 'LONG1'), { coupleId: 'c9', createdBy: 'dave', usedBy: null, expiresAt: Timestamp.fromMillis(Date.now() + 5 * 24 * HOURS) });
    await assertFails(batch.commit());
  });
});

describe('names and meetup', () => {
  beforeEach(async () => {
    await startSpace();
    await joinBatch('bob').commit();
  });

  test('each person writes only the name they give the other', async () => {
    await assertSucceeds(setDoc(doc(db('alice'), 'couples/c1/names/alice'), { nameForPartner: 'Rook', updatedAt: serverTimestamp() }));
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/names/bob'), { nameForPartner: 'Me', updatedAt: serverTimestamp() }));
    await assertSucceeds(getDoc(doc(db('bob'), 'couples/c1/names/alice')));
  });

  test('names must be 1 to 24 characters', async () => {
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/names/alice'), { nameForPartner: '', updatedAt: serverTimestamp() }));
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/names/alice'), { nameForPartner: 'x'.repeat(25), updatedAt: serverTimestamp() }));
  });

  test('outsiders cannot read or write anything in the space', async () => {
    await assertFails(getDoc(doc(db('mallory'), 'couples/c1/names/alice')));
    await assertFails(getDoc(doc(db('mallory'), 'couples/c1/shared/meetup')));
    await assertFails(setDoc(doc(db('mallory'), 'couples/c1/shared/meetup'), { at: null, updatedBy: 'mallory', updatedAt: serverTimestamp() }));
  });

  test('either member can set or clear the meetup, signed as themself', async () => {
    const at = Timestamp.fromMillis(Date.now() + 72 * HOURS);
    await assertSucceeds(setDoc(doc(db('alice'), 'couples/c1/shared/meetup'), { at, updatedBy: 'alice', updatedAt: serverTimestamp() }));
    await assertSucceeds(setDoc(doc(db('bob'), 'couples/c1/shared/meetup'), { at: null, updatedBy: 'bob', updatedAt: serverTimestamp() }));
    await assertFails(setDoc(doc(db('bob'), 'couples/c1/shared/meetup'), { at, updatedBy: 'alice', updatedAt: serverTimestamp() }));
  });

  test('roles: first pick decides, nobody takes the other\'s role', async () => {
    const roles = (uid) => doc(db(uid), 'couples/c1/shared/roles');
    await assertSucceeds(setDoc(roles('alice'), { goodBoy: 'alice', goodGirl: null, updatedAt: serverTimestamp() }));
    // Bob can't take Good Boy from Alice, or claim both, or edit Alice out.
    await assertFails(setDoc(roles('bob'), { goodBoy: 'bob', goodGirl: null, updatedAt: serverTimestamp() }));
    await assertFails(setDoc(roles('bob'), { goodBoy: null, goodGirl: 'bob', updatedAt: serverTimestamp() }));
    await assertSucceeds(setDoc(roles('bob'), { goodBoy: 'alice', goodGirl: 'bob', updatedAt: serverTimestamp() }));
    // Alice can't now switch to Good Girl (Bob holds it) or assign roles for Bob.
    await assertFails(setDoc(roles('alice'), { goodBoy: null, goodGirl: 'alice', updatedAt: serverTimestamp() }));
    await assertFails(setDoc(roles('alice'), { goodBoy: 'bob', goodGirl: 'alice', updatedAt: serverTimestamp() }));
    await assertFails(getDoc(doc(db('mallory'), 'couples/c1/shared/roles')));
  });

  test('roles: you may switch while the other role is still free', async () => {
    const roles = (uid) => doc(db(uid), 'couples/c1/shared/roles');
    await assertSucceeds(setDoc(roles('alice'), { goodBoy: 'alice', goodGirl: null, updatedAt: serverTimestamp() }));
    await assertSucceeds(setDoc(roles('alice'), { goodBoy: null, goodGirl: 'alice', updatedAt: serverTimestamp() }));
    await assertFails(setDoc(roles('alice'), { goodBoy: 'alice', goodGirl: 'alice', updatedAt: serverTimestamp() }));
  });

  test('members cannot be removed or swapped', async () => {
    await assertFails(updateDoc(doc(db('alice'), 'couples', 'c1'), { members: ['alice', 'mallory'] }));
    await assertFails(updateDoc(doc(db('alice'), 'couples', 'c1'), { members: ['alice'] }));
  });
});

describe('moods and gestures', () => {
  beforeEach(async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'couples/c1'), { members: ['alice', 'bob'], createdBy: 'alice', inviteCode: 'X' });
    });
  });

  test('you set only your own mood, from the list, with a short note', async () => {
    const mine = doc(db('alice'), 'couples/c1/moods/alice');
    await assertSucceeds(setDoc(mine, { mood: 'sleepy', label: 'Sleepy', note: 'rain again', updatedAt: serverTimestamp() }));
    await assertSucceeds(setDoc(mine, { mood: 'c_k3v9q2xa', label: 'Angry bcs parents', note: null, updatedAt: serverTimestamp() }));
    await assertSucceeds(getDoc(doc(db('bob'), 'couples/c1/moods/alice')));
    await assertFails(setDoc(doc(db('bob'), 'couples/c1/moods/alice'), { mood: 'low', label: 'Low', note: null, updatedAt: serverTimestamp() }));
    await assertFails(setDoc(mine, { mood: 'calm', label: 'x'.repeat(25), note: null, updatedAt: serverTimestamp() }));
    await assertFails(setDoc(mine, { mood: 'calm', label: 'Calm', note: 'x'.repeat(81), updatedAt: serverTimestamp() }));
    await assertFails(setDoc(mine, { mood: 'calm', label: 'Calm', note: null, updatedAt: serverTimestamp(), extra: 1 }));
    await assertFails(getDoc(doc(db('mallory'), 'couples/c1/moods/alice')));
  });

  test('either member can edit the shared mood list, within limits', async () => {
    const list = (uid) => doc(db(uid), 'couples/c1/shared/moodOptions');
    const options = [{ key: 'sleepy', label: 'Sleepy' }, { key: 'c_ab', label: 'Very sad' }];
    await assertSucceeds(setDoc(list('alice'), { options, updatedBy: 'alice', updatedAt: serverTimestamp() }));
    await assertSucceeds(setDoc(list('bob'), { options: options.slice(1), updatedBy: 'bob', updatedAt: serverTimestamp() }));
    await assertFails(setDoc(list('bob'), { options: [], updatedBy: 'bob', updatedAt: serverTimestamp() }));
    await assertFails(setDoc(list('bob'), { options, updatedBy: 'alice', updatedAt: serverTimestamp() }));
    const tooMany = Array.from({ length: 41 }, (_, i) => ({ key: `k${i}`, label: `m${i}` }));
    await assertFails(setDoc(list('alice'), { options: tooMany, updatedBy: 'alice', updatedAt: serverTimestamp() }));
    await assertFails(setDoc(list('mallory'), { options, updatedBy: 'mallory', updatedAt: serverTimestamp() }));
  });

  test('gestures are added as yourself and can never be changed or removed', async () => {
    const ref = doc(db('alice'), 'couples/c1/gestures/g1');
    await assertSucceeds(setDoc(ref, { kind: 'hug', from: 'alice', sentAt: serverTimestamp() }));
    await assertSucceeds(getDocs(collection(db('bob'), 'couples/c1/gestures')));
    await assertFails(updateDoc(doc(db('alice'), 'couples/c1/gestures/g1'), { kind: 'tomato' }));
    await assertFails(deleteDoc(doc(db('bob'), 'couples/c1/gestures/g1')));
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/gestures/g2'), { kind: 'hug', from: 'bob', sentAt: serverTimestamp() }));
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/gestures/g3'), { kind: 'slap', from: 'alice', sentAt: serverTimestamp() }));
    await assertFails(setDoc(doc(db('mallory'), 'couples/c1/gestures/g4'), { kind: 'hug', from: 'mallory', sentAt: serverTimestamp() }));
    await assertFails(getDocs(collection(db('mallory'), 'couples/c1/gestures')));
  });
});

describe('snaps and Memory Lane', () => {
  beforeEach(async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'couples/c1'), { members: ['alice', 'bob'], createdBy: 'alice', inviteCode: 'X' });
    });
  });
  const bytes = (n) => Bytes.fromUint8Array(new Uint8Array(n));
  const snap = (from, extra = {}) => ({
    from, caption: 'rain again', sentAt: serverTimestamp(), width: 1280, height: 960,
    thumb: bytes(2000), collections: [], kept: false, ...extra,
  });
  async function send(uid, id) {
    const f = db(uid);
    const b = writeBatch(f);
    b.set(doc(f, `couples/c1/snaps/${id}`), snap(uid));
    b.set(doc(f, `couples/c1/snaps/${id}/image/full`), { data: bytes(300000) });
    return b.commit();
  }

  test('you send snaps as yourself, within size limits', async () => {
    await assertSucceeds(send('alice', 's1'));
    await assertSucceeds(getDoc(doc(db('bob'), 'couples/c1/snaps/s1/image/full')));
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/snaps/s2'), snap('bob')));
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/snaps/s3'), snap('alice', { thumb: bytes(70000) })));
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/snaps/s4'), snap('alice', { kept: true, collections: ['x'] })));
    // "Keep it" while sending: kept in a collection, with you as the one who put it there.
    await assertSucceeds(setDoc(doc(db('alice'), 'couples/c1/snaps/s7'), snap('alice', { kept: true, collections: ['x'], addedBy: { x: 'alice' } })));
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/snaps/s8'), snap('alice', { kept: true, collections: ['x'], addedBy: { x: 'bob' } })));
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/snaps/s9'), snap('alice', { kept: true, collections: ['x', 'y'], addedBy: { x: 'alice' } })));
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/snaps/s5'), snap('alice', { caption: 'x'.repeat(141) })));
    const fa = db('alice');
    const big = writeBatch(fa);
    big.set(doc(fa, 'couples/c1/snaps/s6'), snap('alice'));
    big.set(doc(fa, 'couples/c1/snaps/s6/image/full'), { data: bytes(950000) });
    await assertFails(big.commit());
    await assertFails(getDoc(doc(db('mallory'), 'couples/c1/snaps/s1')));
    await assertFails(getDoc(doc(db('mallory'), 'couples/c1/snaps/s1/image/full')));
  });

  test('unkept snaps can be removed by either of you, kept ones by nobody', async () => {
    await send('alice', 's0');
    await assertSucceeds(deleteDoc(doc(db('bob'), 'couples/c1/snaps/s0/image/full')));
    await assertSucceeds(deleteDoc(doc(db('bob'), 'couples/c1/snaps/s0')));
    await send('alice', 's1');
    await assertFails(deleteDoc(doc(db('mallory'), 'couples/c1/snaps/s1')));
    await assertSucceeds(setDoc(doc(db('bob'), 'couples/c1/collections/c9'), { name: 'crows', createdBy: 'bob', createdAt: serverTimestamp() }));
    await assertFails(updateDoc(doc(db('bob'), 'couples/c1/snaps/s1'), { collections: ['c9'], kept: true }));
    await assertSucceeds(updateDoc(doc(db('bob'), 'couples/c1/snaps/s1'), { collections: ['c9'], kept: true, 'addedBy.c9': 'bob' }));
    await assertFails(deleteDoc(doc(db('bob'), 'couples/c1/snaps/s1')));
    await assertFails(deleteDoc(doc(db('alice'), 'couples/c1/snaps/s1/image/full')));
    await assertFails(deleteDoc(doc(db('alice'), 'couples/c1/snaps/s1')));
    await assertFails(updateDoc(doc(db('bob'), 'couples/c1/snaps/s1'), { caption: 'edited' }));
    await assertFails(updateDoc(doc(db('bob'), 'couples/c1/snaps/s1'), { collections: [], kept: true, 'addedBy.c9': deleteField() }));
    // Only Bob put it in c9, so only Bob can take it out.
    await assertFails(updateDoc(doc(db('alice'), 'couples/c1/snaps/s1'), { collections: [], kept: false, 'addedBy.c9': deleteField() }));
    await assertSucceeds(updateDoc(doc(db('bob'), 'couples/c1/snaps/s1'), { collections: [], kept: false, 'addedBy.c9': deleteField() }));
    const fa = db('alice');
    const cleanup = writeBatch(fa);
    cleanup.delete(doc(fa, 'couples/c1/snaps/s1/image/full'));
    cleanup.delete(doc(fa, 'couples/c1/snaps/s1'));
    await assertSucceeds(cleanup.commit());
  });

  test('you react to a snap with any one emoji, only as yourself', async () => {
    await send('alice', 's1');
    const ref = (uid) => doc(db(uid), 'couples/c1/snaps/s1');
    await assertSucceeds(updateDoc(ref('bob'), { reactions: { bob: '\u{1F5A4}' } }));
    await assertSucceeds(updateDoc(ref('alice'), { reactions: { bob: '\u{1F5A4}', alice: '\u{1F345}' } }));
    await assertFails(updateDoc(ref('bob'), { reactions: { bob: '\u{1F5A4}', alice: '\u{1F525}' } }));
    await assertFails(updateDoc(ref('bob'), { reactions: { bob: 'lol', alice: '\u{1F345}' } }));
    // Any one emoji: skin tones, joined emoji, flags and keycaps too; never plain text.
    for (const e of ['\u{1F622}', '\u2764\uFE0F', '\u{1F469}\u200D\u{1F4BB}', '\u{1F1EE}\u{1F1F3}', '\u{1F44D}\u{1F3FD}', '1\uFE0F\u20E3']) {
      await assertSucceeds(updateDoc(ref('bob'), { reactions: { bob: e, alice: '\u{1F345}' } }));
    }
    await assertFails(updateDoc(ref('bob'), { reactions: { bob: '1', alice: '\u{1F345}' } }));
    await assertFails(updateDoc(ref('bob'), { reactions: { bob: '\u{1F5A4}a', alice: '\u{1F345}' } }));
    await assertFails(updateDoc(ref('bob'), { reactions: { bob: '\u{1F5A4}'.repeat(9), alice: '\u{1F345}' } }));
    await assertSucceeds(updateDoc(ref('bob'), { reactions: { alice: '\u{1F345}' } }));
    await assertFails(updateDoc(ref('mallory'), { reactions: { mallory: '\u{1F5A4}' } }));
  });

  test('points go only to your partner, 1 to 10 either way, and stay put', async () => {
    const give = (uid, data) => setDoc(doc(db(uid), `couples/c1/points/${uid}-${Math.random()}`), { from: uid, to: 'bob', amount: 5, reason: 'dishes', at: serverTimestamp(), ...data });
    await assertSucceeds(give('alice', {}));
    await assertSucceeds(give('alice', { amount: 10, reason: null }));
    await assertFails(give('bob', { to: 'bob' }));
    await assertFails(give('alice', { to: 'mallory' }));
    await assertSucceeds(give('alice', { amount: -5, reason: 'left the lights on' }));
    await assertSucceeds(give('alice', { amount: -10 }));
    await assertFails(give('alice', { amount: 0 }));
    await assertFails(give('alice', { amount: 11 }));
    await assertFails(give('alice', { amount: -11 }));
    await assertFails(give('alice', { amount: 2.5 }));
    await assertFails(give('alice', { from: 'bob' }));
    await assertFails(give('alice', { reason: 'x'.repeat(141) }));
    await assertSucceeds(setDoc(doc(db('alice'), 'couples/c1/points/p1'), { from: 'alice', to: 'bob', amount: 3, reason: null, at: serverTimestamp() }));
    await assertFails(updateDoc(doc(db('bob'), 'couples/c1/points/p1'), { amount: 99 }));
    await assertFails(deleteDoc(doc(db('alice'), 'couples/c1/points/p1')));
    await assertSucceeds(getDocs(collection(db('bob'), 'couples/c1/points')));
    await assertFails(getDocs(collection(db('mallory'), 'couples/c1/points')));
  });

  test('collections are made and renamed by either of you', async () => {
    const ref = (uid) => doc(db(uid), 'couples/c1/collections/k1');
    await assertSucceeds(setDoc(ref('alice'), { name: 'crows', createdBy: 'alice', createdAt: serverTimestamp() }));
    await assertSucceeds(updateDoc(ref('bob'), { name: 'the crows' }));
    await assertFails(updateDoc(ref('bob'), { createdBy: 'bob' }));
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/collections/k2'), { name: '', createdBy: 'alice', createdAt: serverTimestamp() }));
    await assertFails(setDoc(doc(db('mallory'), 'couples/c1/collections/k3'), { name: 'x', createdBy: 'mallory', createdAt: serverTimestamp() }));
  });

  test('collections get a cover, and deleting one takes both of you', async () => {
    const ref = (uid) => doc(db(uid), 'couples/c1/collections/k1');
    await assertSucceeds(setDoc(ref('alice'), { name: 'crows', createdBy: 'alice', createdAt: serverTimestamp() }));
    await assertSucceeds(updateDoc(ref('bob'), { cover: 's1' }));
    await assertSucceeds(updateDoc(ref('alice'), { cover: null }));
    await assertFails(updateDoc(ref('bob'), { cover: 5 }));
    await assertFails(updateDoc(ref('bob'), { cover: 'x'.repeat(65) }));
    await assertFails(deleteDoc(ref('mallory')));
    // Nobody asked yet: no one can delete it.
    await assertFails(deleteDoc(ref('bob')));
    await assertFails(updateDoc(ref('alice'), { deleteAskedBy: 'bob' }));
    await assertSucceeds(updateDoc(ref('alice'), { deleteAskedBy: 'alice' }));
    // The asker can't finish it alone.
    await assertFails(deleteDoc(ref('alice')));
    // Bob keeps it; then Alice asks again and Bob agrees, taking her snap out with it.
    await assertSucceeds(updateDoc(ref('bob'), { deleteAskedBy: deleteField() }));
    await assertSucceeds(updateDoc(ref('alice'), { deleteAskedBy: 'alice' }));
    await send('alice', 's1');
    await assertSucceeds(updateDoc(doc(db('alice'), 'couples/c1/snaps/s1'), { collections: ['k1'], kept: true, 'addedBy.k1': 'alice' }));
    // Bob can't take Alice's snap out on its own...
    await assertFails(updateDoc(doc(db('bob'), 'couples/c1/snaps/s1'), { collections: [], kept: false, 'addedBy.k1': deleteField() }));
    // ...but can as part of deleting the collection.
    const fb = db('bob');
    const del = writeBatch(fb);
    del.delete(doc(fb, 'couples/c1/collections/k1'));
    del.update(doc(fb, 'couples/c1/snaps/s1'), { collections: [], kept: false, 'addedBy.k1': deleteField() });
    await assertSucceeds(del.commit());
  });
});

describe('focus room', () => {
  beforeEach(async () => {
    await startSpace();
    await joinBatch('bob').commit();
  });

  const session = (extra = {}) => ({ state: 'focus', bankedMs: 0, focusSince: Timestamp.now(), breakEndsAt: null, breakMs: 0, breakSince: null, startTotalMs: 0, updatedAt: serverTimestamp(), ...extra });

  test('you write only your own session, with a known state', async () => {
    await assertSucceeds(setDoc(doc(db('alice'), 'couples/c1/focus/alice'), session()));
    await assertSucceeds(getDoc(doc(db('bob'), 'couples/c1/focus/alice')));
    await assertFails(setDoc(doc(db('bob'), 'couples/c1/focus/alice'), session()));
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/focus/alice'), session({ state: 'napping' })));
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/focus/alice'), session({ bankedMs: -1 })));
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/focus/alice'), session({ breakMs: -5 })));
    await assertFails(getDoc(doc(db('mallory'), 'couples/c1/focus/alice')));
  });

  test('totals only grow, a stretch at a time, and hours become points in the same write', async () => {
    const totals = (uid) => doc(db(uid), `couples/c1/focusTotals/${uid}`);
    await assertSucceeds(setDoc(totals('alice'), { totalMs: 3000000, weekMs: 3000000, weekStart: '2026-11-23', pointHours: 0 }));
    await assertFails(setDoc(totals('alice'), { totalMs: 1000, weekMs: 1000, weekStart: '2026-11-23', pointHours: 0 }));
    await assertFails(setDoc(totals('alice'), { totalMs: 3000000 + 13 * 3600000, weekMs: 0, weekStart: '2026-11-23', pointHours: 0 }));
    await assertFails(setDoc(totals('alice'), { totalMs: 3600000, weekMs: 3600000, weekStart: '2026-11-23', pointHours: 2 }));
    await assertFails(setDoc(doc(db('bob'), 'couples/c1/focusTotals/alice'), { totalMs: 9000000, weekMs: 0, weekStart: null, pointHours: 0 }));

    const point = { from: 'alice', to: 'alice', amount: 1, reason: 'focus hours', at: serverTimestamp() };
    // A focus point on its own (no hour counted with it) is refused.
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/points/f0'), point));
    const fa = db('alice');
    const b = writeBatch(fa);
    b.set(doc(fa, 'couples/c1/focusTotals/alice'), { totalMs: 3700000, weekMs: 3700000, weekStart: '2026-11-23', pointHours: 1 });
    b.set(doc(fa, 'couples/c1/points/f1'), point);
    await assertSucceeds(b.commit());
    // Other self-awards stay refused.
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/points/f2'), { ...point, reason: 'being great' }));
  });

  test('the points rate changes only when both agree, and focus points follow it (double together)', async () => {
    const rate = (uid) => doc(db(uid), 'couples/c1/shared/focusRate');
    // Alice suggests 3; the rate itself can't jump without Bob.
    await assertFails(setDoc(rate('alice'), { rate: 3, proposed: null, proposedBy: null, at: serverTimestamp() }));
    await assertSucceeds(setDoc(rate('alice'), { rate: 1, proposed: 3, proposedBy: 'alice', at: serverTimestamp() }));
    // She can't agree to her own suggestion, or claim it was Bob's.
    await assertFails(setDoc(rate('alice'), { rate: 3, proposed: null, proposedBy: null, at: serverTimestamp() }));
    await assertFails(setDoc(rate('alice'), { rate: 1, proposed: 3, proposedBy: 'bob', at: serverTimestamp() }));
    // Bob agrees to exactly what she suggested.
    await assertFails(setDoc(rate('bob'), { rate: 5, proposed: null, proposedBy: null, at: serverTimestamp() }));
    await assertSucceeds(setDoc(rate('bob'), { rate: 3, proposed: null, proposedBy: null, at: serverTimestamp() }));
    await assertFails(setDoc(rate('bob'), { rate: 1, proposed: 11, proposedBy: 'bob', at: serverTimestamp() }));

    // At 3 a point: one hour, half of it together and an earlier together hour finishing = 3 + 3.
    const fa = db('alice');
    const first = writeBatch(fa);
    first.set(doc(fa, 'couples/c1/focusTotals/alice'), { totalMs: 3700000, weekMs: 3700000, weekStart: '2026-11-23', pointHours: 1, togetherMs: 3650000, togetherHours: 1 });
    first.set(doc(fa, 'couples/c1/points/r1'), { from: 'alice', to: 'alice', amount: 6, reason: 'focus hours', at: serverTimestamp() });
    await assertSucceeds(first.commit());
    // The old rate of one point an hour is refused now.
    const wrong = writeBatch(fa);
    wrong.set(doc(fa, 'couples/c1/focusTotals/alice'), { totalMs: 7300000, weekMs: 7300000, weekStart: '2026-11-23', pointHours: 2, togetherMs: 3650000, togetherHours: 1 });
    wrong.set(doc(fa, 'couples/c1/points/r2'), { from: 'alice', to: 'alice', amount: 1, reason: 'focus hours', at: serverTimestamp() });
    await assertFails(wrong.commit());
    // Together hours can't outrun together time.
    await assertFails(setDoc(doc(fa, 'couples/c1/focusTotals/alice'), { totalMs: 7300000, weekMs: 7300000, weekStart: '2026-11-23', pointHours: 2, togetherMs: 3650000, togetherHours: 2 }));
  });

  test('bounties: posted for the other, claimed by them, paid (in points) by the poster', async () => {
    const ref = (uid) => doc(db(uid), 'couples/c1/bounties/b1');
    const post = { task: 'Dishes', reward: 15, due: 'tonight', by: 'alice', status: 'open', at: serverTimestamp() };
    await assertFails(setDoc(ref('alice'), { ...post, reward: 21 }));
    await assertFails(setDoc(ref('alice'), { ...post, by: 'bob' }));
    await assertSucceeds(setDoc(ref('alice'), post));
    // Alice can't claim her own; Bob can.
    await assertFails(updateDoc(ref('alice'), { status: 'claimed', claimedAt: serverTimestamp() }));
    await assertSucceeds(updateDoc(ref('bob'), { status: 'claimed', claimedAt: serverTimestamp() }));
    // Bob can't pay himself, or take it down while it waits.
    await assertFails(updateDoc(ref('bob'), { status: 'paid', paidAt: serverTimestamp() }));
    await assertFails(deleteDoc(ref('alice')));
    // "Not yet", then claimed again, then paid with the points in the same batch.
    await assertSucceeds(updateDoc(ref('alice'), { status: 'open', claimedAt: deleteField() }));
    await assertSucceeds(updateDoc(ref('bob'), { status: 'claimed', claimedAt: serverTimestamp() }));
    const fa = db('alice');
    const pay = writeBatch(fa);
    pay.update(doc(fa, 'couples/c1/bounties/b1'), { status: 'paid', paidAt: serverTimestamp() });
    pay.set(doc(fa, 'couples/c1/points/bp1'), { from: 'alice', to: 'bob', amount: 15, reason: 'Bounty: Dishes', at: serverTimestamp() });
    await assertSucceeds(pay.commit());
    // More than 10 points only for a bounty.
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/points/bp2'), { from: 'alice', to: 'bob', amount: 15, reason: 'because', at: serverTimestamp() }));
    await assertSucceeds(deleteDoc(ref('alice')));
    await assertFails(getDoc(doc(db('mallory'), 'couples/c1/bounties/b1')));
  });

  test('meetups: either of you logs and edits them; only the date is required', async () => {
    const ref = (uid) => doc(db(uid), 'couples/c1/meetups/m1');
    const base = { date: '2026-11-27', live: true, startedAt: Timestamp.now(), did: [], photos: [], thumbs: {}, by: 'alice', createdAt: serverTimestamp(), updatedBy: 'alice', updatedAt: serverTimestamp() };
    await assertFails(setDoc(ref('alice'), { ...base, date: 'today' }));
    await assertFails(setDoc(ref('alice'), { ...base, by: 'bob' }));
    await assertSucceeds(setDoc(ref('alice'), base));
    // Bob fills things in and adds a photo.
    await assertSucceeds(updateDoc(ref('bob'), { live: false, minutes: 340, place: 'Lodhi Garden', lat: 28.5933, lon: 77.2197, did: ['walk', 'food'], note: 'momos after', updatedBy: 'bob', updatedAt: serverTimestamp() }));
    await assertFails(updateDoc(ref('bob'), { did: ['karaoke'], updatedBy: 'bob', updatedAt: serverTimestamp() }));
    await assertFails(updateDoc(ref('bob'), { by: 'bob', updatedBy: 'bob', updatedAt: serverTimestamp() }));
    await assertFails(updateDoc(ref('bob'), { minutes: 2000, updatedBy: 'bob', updatedAt: serverTimestamp() }));
    // Several places, in order; the old single place goes when the list is written.
    const lodhi = { name: 'Lodhi Garden', lat: 28.5933, lon: 77.2197 };
    await assertSucceeds(updateDoc(ref('alice'), { places: [lodhi, { name: 'Khan Market', lat: 28.6003, lon: 77.2270 }, { name: 'Somewhere unpinned' }], place: deleteField(), lat: deleteField(), lon: deleteField(), updatedBy: 'alice', updatedAt: serverTimestamp() }));
    await assertSucceeds(updateDoc(ref('alice'), { places: [{ name: '', lat: 28.61, lon: 77.21 }], updatedBy: 'alice', updatedAt: serverTimestamp() }));
    await assertFails(updateDoc(ref('alice'), { places: Array.from({ length: 7 }, () => lodhi), updatedBy: 'alice', updatedAt: serverTimestamp() }));
    await assertFails(updateDoc(ref('alice'), { places: [lodhi, { name: 'x'.repeat(81) }], updatedBy: 'alice', updatedAt: serverTimestamp() }));
    await assertFails(updateDoc(ref('alice'), { places: [lodhi, { name: 'Bad', lat: 200 }], updatedBy: 'alice', updatedAt: serverTimestamp() }));
    await assertFails(updateDoc(ref('alice'), { places: [{ name: 'Extra', note: 'no' }], updatedBy: 'alice', updatedAt: serverTimestamp() }));
    await assertFails(updateDoc(ref('alice'), { places: ['Lodhi Garden'], updatedBy: 'alice', updatedAt: serverTimestamp() }));
    const fb = db('bob');
    const add = writeBatch(fb);
    add.set(doc(fb, 'couples/c1/meetups/m1/photos/p1'), { data: Bytes.fromUint8Array(new Uint8Array(150000)), by: 'bob', at: serverTimestamp() });
    add.update(doc(fb, 'couples/c1/meetups/m1'), { photos: ['p1'], 'thumbs.p1': Bytes.fromUint8Array(new Uint8Array(20000)), updatedBy: 'bob', updatedAt: serverTimestamp() });
    await assertSucceeds(add.commit());
    await assertFails(updateDoc(ref('bob'), { photos: Array.from({ length: 13 }, (_, i) => `p${i}`), updatedBy: 'bob', updatedAt: serverTimestamp() }));
    await assertFails(getDoc(doc(db('mallory'), 'couples/c1/meetups/m1/photos/p1')));
    await assertSucceeds(deleteDoc(doc(db('alice'), 'couples/c1/meetups/m1/photos/p1')));
    await assertSucceeds(deleteDoc(ref('alice')));
  });

  test('records: each song you send goes on the shelf; only the other one rates it', async () => {
    const rec = { trackId: '4uLU6hMCjMI75M1A2tKUQC', title: 'Song', by: 'alice', sentAt: serverTimestamp() };
    await assertSucceeds(setDoc(doc(db('alice'), 'couples/c1/records/r1'), rec));
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/records/r2'), { ...rec, by: 'bob' }));
    await assertFails(updateDoc(doc(db('alice'), 'couples/c1/records/r1'), { rating: 9, ratedAt: serverTimestamp() }));
    await assertSucceeds(updateDoc(doc(db('bob'), 'couples/c1/records/r1'), { rating: 9, ratedAt: serverTimestamp() }));
    await assertSucceeds(setDoc(doc(db('alice'), 'couples/c1/records/r3'), { ...rec, kind: 'playlist' }));
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/records/r4'), { ...rec, kind: 'artist' }));
  });

  test('the pet: shared, but coins and bond are your own and coins never go negative', async () => {
    const cat = (uid) => doc(db(uid), 'couples/c1/pet/cat');
    await assertSucceeds(setDoc(cat('alice'), { name: 'Mochi', coins: { alice: 10 }, bond: { alice: 2 }, needs: { hunger: { v: 80, at: Timestamp.now() } } }));
    await assertSucceeds(updateDoc(cat('bob'), { coins: { alice: 10, bob: 5 }, 'needs.hunger': { v: 90, at: Timestamp.now() } }));
    await assertFails(updateDoc(cat('bob'), { coins: { alice: 999, bob: 5 } }));
    await assertFails(updateDoc(cat('alice'), { coins: { alice: -1, bob: 5 } }));
    await assertFails(updateDoc(cat('bob'), { bond: { alice: 50 } }));
    await assertFails(updateDoc(cat('bob'), { secret: true }));
    await assertFails(setDoc(doc(db('alice'), 'couples/c1/pet/dog'), { name: 'Rex' }));
    await assertFails(getDoc(cat('mallory')));
    await assertSucceeds(setDoc(doc(db('bob'), 'couples/c1/petDiary/d1'), { text: 'I sat on the keyboard.', at: serverTimestamp(), by: 'bob' }));
    await assertFails(setDoc(doc(db('bob'), 'couples/c1/petDiary/d2'), { text: 'x'.repeat(301), at: serverTimestamp(), by: 'bob' }));
  });

  test('either of you can send the cat somewhere', async () => {
    const cat = (uid) => doc(db(uid), 'couples/c1/shared/cat');
    await assertSucceeds(setDoc(cat('bob'), { activity: 'fish', since: Timestamp.now(), by: 'bob' }));
    await assertSucceeds(setDoc(cat('alice'), { activity: 'window', since: Timestamp.now(), by: 'alice' }));
    await assertFails(setDoc(cat('alice'), { activity: 'moon', since: Timestamp.now(), by: 'alice' }));
    await assertFails(setDoc(cat('mallory'), { activity: 'fish', since: Timestamp.now(), by: 'mallory' }));
  });

  test('either of you can leave a jam link or take it out', async () => {
    const jam = (uid) => doc(db(uid), 'couples/c1/shared/jam');
    await assertSucceeds(setDoc(jam('alice'), { url: 'https://open.spotify.com/socialsession/abc', by: 'alice', at: serverTimestamp() }));
    await assertSucceeds(getDoc(jam('bob')));
    await assertFails(setDoc(jam('alice'), { url: 'javascript:alert(1)', by: 'alice', at: serverTimestamp() }));
    await assertFails(setDoc(jam('alice'), { url: 'https://x.y/z', by: 'bob', at: serverTimestamp() }));
    await assertSucceeds(deleteDoc(jam('bob')));
    await assertFails(getDoc(jam('mallory')));
  });
});

describe('push registrations and notification settings', () => {
  const FID = 'cX9pQ2rS-tU_vW3x';

  test('you can register, list and remove only your own phones', async () => {
    const mine = doc(db('alice'), `users/alice/devices/${FID}`);
    await assertSucceeds(setDoc(mine, { platform: 'android', updatedAt: serverTimestamp() }));
    await assertSucceeds(getDocs(collection(db('alice'), 'users/alice/devices')));
    await assertSucceeds(deleteDoc(mine));
    await assertFails(setDoc(doc(db('mallory'), `users/alice/devices/${FID}`), { platform: 'android', updatedAt: serverTimestamp() }));
    await assertFails(getDocs(collection(db('bob'), 'users/alice/devices')));
    await assertFails(setDoc(doc(env.unauthenticatedContext().firestore(), `users/alice/devices/${FID}`), { platform: 'android', updatedAt: serverTimestamp() }));
  });

  test('registrations hold only the expected fields', async () => {
    const mine = doc(db('alice'), `users/alice/devices/${FID}`);
    await assertFails(setDoc(mine, { platform: 'android', updatedAt: serverTimestamp(), note: 'x' }));
    await assertFails(setDoc(mine, { platform: 'ios', updatedAt: serverTimestamp() }));
    await assertFails(setDoc(mine, { platform: 'android', updatedAt: Timestamp.fromMillis(0) }));
    await assertFails(setDoc(doc(db('alice'), 'users/alice/devices/short'), { platform: 'android', updatedAt: serverTimestamp() }));
  });

  test('notification settings are yours alone and only booleans', async () => {
    const mine = doc(db('alice'), 'users/alice/settings/notifications');
    await assertSucceeds(setDoc(mine, { meetup: false, space: true, updatedAt: serverTimestamp() }));
    await assertSucceeds(setDoc(mine, { gestures: false, moods: true, updatedAt: serverTimestamp() }));
    await assertSucceeds(getDoc(mine));
    await assertFails(setDoc(mine, { meetup: 'no', updatedAt: serverTimestamp() }));
    await assertFails(setDoc(mine, { meetup: true, spam: true, updatedAt: serverTimestamp() }));
    await assertFails(getDoc(doc(db('bob'), 'users/alice/settings/notifications')));
    await assertFails(setDoc(doc(db('bob'), 'users/alice/settings/notifications'), { meetup: true, updatedAt: serverTimestamp() }));
    await assertFails(setDoc(doc(db('alice'), 'users/alice/settings/other'), { meetup: true, updatedAt: serverTimestamp() }));
  });
});

describe('leaving and coming back', () => {
  async function paired() {
    await startSpace();
    await assertSucceeds(joinBatch('bob').commit());
  }

  function leaveBatch(uid, memberId = uid) {
    const fs = db(uid);
    const batch = writeBatch(fs);
    batch.update(doc(fs, 'couples', 'c1'), { members: ['alice', 'bob'].filter((m) => m !== uid), vacancy: memberId, inviteCode: '' });
    batch.set(doc(fs, 'users', uid), { coupleId: null, updatedAt: serverTimestamp() });
    return batch;
  }

  /** Alice (still in) makes a fresh code that carries Bob's kept place. */
  async function newCode(code = 'MOTH2345', vacancy = 'bob') {
    const fs = db('alice');
    const batch = writeBatch(fs);
    batch.set(doc(fs, 'invites', code), {
      coupleId: 'c1', createdBy: 'alice', usedBy: null, vacancy, expiresAt: Timestamp.fromMillis(Date.now() + 48 * HOURS),
    });
    batch.update(doc(fs, 'couples', 'c1'), { inviteCode: code });
    await assertSucceeds(batch.commit());
  }

  function rejoinBatch(uid, code = 'MOTH2345', vacancy = 'bob') {
    const fs = db(uid);
    const batch = writeBatch(fs);
    batch.update(doc(fs, 'invites', code), { usedBy: uid });
    batch.update(doc(fs, 'couples', 'c1'), { members: arrayUnion(uid), [`ids.${uid}`]: vacancy, vacancy: deleteField() });
    batch.set(doc(fs, 'users', uid), { coupleId: 'c1', updatedAt: serverTimestamp() });
    return batch;
  }

  test('a member can leave while the other stays, and loses access', async () => {
    await paired();
    await assertSucceeds(leaveBatch('bob').commit());
    await assertFails(getDoc(doc(db('bob'), 'couples', 'c1')));
    await assertSucceeds(getDoc(doc(db('alice'), 'couples', 'c1')));
  });

  test('you cannot leave someone else out, or keep a place that is not yours', async () => {
    await paired();
    const fs = db('alice');
    await assertFails(updateDoc(doc(fs, 'couples', 'c1'), { members: ['alice'], vacancy: 'bob', inviteCode: '' }));
    await assertFails(updateDoc(doc(db('bob'), 'couples', 'c1'), { members: ['alice'], vacancy: 'alice', inviteCode: '' }));
  });

  test('coming back on a different account takes over the old place and keeps its points', async () => {
    await paired();
    await assertSucceeds(addDoc(collection(db('alice'), 'couples', 'c1', 'points'), { from: 'alice', to: 'bob', amount: 5, reason: null, at: serverTimestamp() }));
    await assertSucceeds(leaveBatch('bob').commit());
    await newCode();
    await assertSucceeds(rejoinBatch('bob2').commit());
    const couple = (await assertSucceeds(getDoc(doc(db('bob2'), 'couples', 'c1')))).data();
    if (couple.ids.bob2 !== 'bob' || 'vacancy' in couple) throw new Error('place not taken over');
    // Bob's old points are readable and he acts as "bob" from his new account.
    await assertSucceeds(getDocs(collection(db('bob2'), 'couples', 'c1', 'points')));
    await assertSucceeds(addDoc(collection(db('bob2'), 'couples', 'c1', 'points'), { from: 'bob', to: 'alice', amount: 2, reason: null, at: serverTimestamp() }));
    await assertFails(addDoc(collection(db('bob2'), 'couples', 'c1', 'points'), { from: 'bob2', to: 'alice', amount: 2, reason: null, at: serverTimestamp() }));
    await assertSucceeds(setDoc(doc(db('bob2'), 'couples', 'c1', 'moods', 'bob'), { mood: 'ok', label: 'Okay', note: null, updatedAt: serverTimestamp() }));
    await assertFails(setDoc(doc(db('bob2'), 'couples', 'c1', 'moods', 'bob2'), { mood: 'ok', label: 'Okay', note: null, updatedAt: serverTimestamp() }));
    // Alice can still award points to him under his old id.
    await assertSucceeds(addDoc(collection(db('alice'), 'couples', 'c1', 'points'), { from: 'alice', to: 'bob', amount: 1, reason: null, at: serverTimestamp() }));
  });

  test('coming back cannot claim a place other than the one that was kept', async () => {
    await paired();
    await assertSucceeds(leaveBatch('bob').commit());
    await newCode();
    await assertFails(rejoinBatch('mallory', 'MOTH2345', 'alice').commit());
  });

  test('a code made after someone left must carry their place', async () => {
    await paired();
    await assertSucceeds(leaveBatch('bob').commit());
    const fs = db('alice');
    const batch = writeBatch(fs);
    batch.set(doc(fs, 'invites', 'MOTH2345'), { coupleId: 'c1', createdBy: 'alice', usedBy: null, expiresAt: Timestamp.fromMillis(Date.now() + HOURS) });
    batch.update(doc(fs, 'couples', 'c1'), { inviteCode: 'MOTH2345' });
    await assertFails(batch.commit());
  });

  test('a plain join is refused while a place is kept', async () => {
    await paired();
    await assertSucceeds(leaveBatch('bob').commit());
    await newCode();
    await assertFails(joinBatch('carol', 'MOTH2345').commit());
  });

  test('the last one out can delete everything, but only after marking it closing', async () => {
    await paired();
    await assertSucceeds(addDoc(collection(db('alice'), 'couples', 'c1', 'gestures'), { kind: 'hug', from: 'alice', sentAt: serverTimestamp() }));
    const points = await assertSucceeds(addDoc(collection(db('alice'), 'couples', 'c1', 'points'), { from: 'alice', to: 'bob', amount: 5, reason: null, at: serverTimestamp() }));
    // Not while both are in.
    await assertFails(updateDoc(doc(db('alice'), 'couples', 'c1'), { closing: true }));
    await assertFails(deleteDoc(points));
    await assertSucceeds(leaveBatch('bob').commit());
    await assertFails(deleteDoc(doc(db('alice'), 'couples', 'c1', 'points', points.id)));
    await assertSucceeds(updateDoc(doc(db('alice'), 'couples', 'c1'), { closing: true }));
    await assertSucceeds(deleteDoc(doc(db('alice'), 'couples', 'c1', 'points', points.id)));
    const gestures = await getDocs(collection(db('alice'), 'couples', 'c1', 'gestures'));
    for (const g of gestures.docs) await assertSucceeds(deleteDoc(g.ref));
    // The one who left can't touch it.
    await assertFails(deleteDoc(doc(db('bob'), 'couples', 'c1')));
    const fs = db('alice');
    const batch = writeBatch(fs);
    batch.delete(doc(fs, 'couples', 'c1'));
    batch.set(doc(fs, 'users', 'alice'), { coupleId: null, updatedAt: serverTimestamp() });
    await assertSucceeds(batch.commit());
  });

  test('nobody can join a space that is being deleted', async () => {
    await startSpace();
    await assertSucceeds(updateDoc(doc(db('alice'), 'couples', 'c1'), { closing: true }));
    await assertFails(joinBatch('bob').commit());
  });

  test('someone alone in a space can close it straight away', async () => {
    await startSpace();
    await assertSucceeds(updateDoc(doc(db('alice'), 'couples', 'c1'), { closing: true }));
    await assertSucceeds(deleteDoc(doc(db('alice'), 'couples', 'c1')));
  });
});

describe('points shop', () => {
  async function paired() {
    await startSpace();
    await assertSucceeds(joinBatch('bob').commit());
  }
  const proposal = (fs, id, data) => setDoc(doc(fs, 'couples', 'c1', 'rewardProposals', id), { ...data, at: serverTimestamp() });
  function acceptBatch(uid, pid, rid, reward) {
    const fs = db(uid);
    const batch = writeBatch(fs);
    batch.set(doc(fs, 'couples', 'c1', 'rewards', rid), { ...reward, proposal: pid });
    batch.delete(doc(fs, 'couples', 'c1', 'rewardProposals', pid));
    return batch;
  }

  test('a new reward needs the other one to agree, exactly as suggested', async () => {
    await paired();
    await assertSucceeds(proposal(db('alice'), 'p1', { kind: 'add', name: 'Back scratch', price: 20, by: 'alice' }));
    // Not by the one who suggested it.
    await assertFails(acceptBatch('alice', 'p1', 'p1', { name: 'Back scratch', price: 20, custom: true, removed: false }).commit());
    // Not changed on the way in.
    await assertFails(acceptBatch('bob', 'p1', 'p1', { name: 'Back scratch', price: 2, custom: true, removed: false }).commit());
    // Not without removing the suggestion.
    await assertFails(setDoc(doc(db('bob'), 'couples', 'c1', 'rewards', 'p1'), { name: 'Back scratch', price: 20, custom: true, removed: false, proposal: 'p1' }));
    await assertSucceeds(acceptBatch('bob', 'p1', 'p1', { name: 'Back scratch', price: 20, custom: true, removed: false }).commit());
  });

  test('prices of the starting rewards change only when the other agrees', async () => {
    await paired();
    await assertFails(setDoc(doc(db('alice'), 'couples', 'c1', 'rewards', 'selfie'), { price: 1, custom: false, removed: false, proposal: 'none' }));
    await assertSucceeds(proposal(db('bob'), 'p2', { kind: 'price', rewardId: 'selfie', price: 8, by: 'bob' }));
    await assertFails(acceptBatch('alice', 'p2', 'selfie', { price: 9, custom: false, removed: false }).commit());
    await assertFails(acceptBatch('alice', 'p2', 'playlist', { price: 8, custom: false, removed: false }).commit());
    await assertSucceeds(acceptBatch('alice', 'p2', 'selfie', { price: 8, custom: false, removed: false }).commit());
    // Then taking it out.
    await assertSucceeds(proposal(db('alice'), 'p3', { kind: 'remove', rewardId: 'selfie', by: 'alice' }));
    await assertSucceeds(acceptBatch('bob', 'p3', 'selfie', { price: 8, custom: false, removed: true }).commit());
  });

  test('suggestions are made as yourself, with sensible prices', async () => {
    await paired();
    await assertFails(proposal(db('alice'), 'p4', { kind: 'add', name: 'x', price: 20, by: 'bob' }));
    await assertFails(proposal(db('alice'), 'p4', { kind: 'add', name: 'x', price: 0, by: 'alice' }));
    await assertFails(proposal(db('alice'), 'p4', { kind: 'add', name: 'x', price: 5000, by: 'alice' }));
    await assertFails(proposal(db('alice'), 'p4', { kind: 'add', price: 5, by: 'alice' }));
    await assertSucceeds(proposal(db('alice'), 'p4', { kind: 'add', name: 'x', price: 5, by: 'alice' }));
    await assertSucceeds(deleteDoc(doc(db('bob'), 'couples', 'c1', 'rewardProposals', 'p4')));
  });

  test('a purchase is cashed in by its buyer and marked done by the other', async () => {
    await paired();
    const ref = doc(db('alice'), 'couples', 'c1', 'purchases', 'b1');
    await assertFails(setDoc(ref, { rewardId: 'selfie', name: 'Selfie', price: 5, by: 'bob', at: serverTimestamp(), state: 'owned' }));
    await assertFails(setDoc(ref, { rewardId: 'selfie', name: 'Selfie', price: 5, by: 'alice', at: serverTimestamp(), state: 'done' }));
    await assertSucceeds(setDoc(ref, { rewardId: 'selfie', name: 'Selfie', price: 5, by: 'alice', at: serverTimestamp(), state: 'owned' }));
    const bobs = doc(db('bob'), 'couples', 'c1', 'purchases', 'b1');
    // Bob can't use or close Alice's purchase before she cashes it in.
    await assertFails(updateDoc(bobs, { state: 'used', usedAt: serverTimestamp() }));
    await assertFails(updateDoc(bobs, { state: 'done', doneAt: serverTimestamp() }));
    await assertSucceeds(updateDoc(ref, { state: 'used', usedAt: serverTimestamp() }));
    // Alice can't mark her own reward done, or give it back once used.
    await assertFails(updateDoc(ref, { state: 'done', doneAt: serverTimestamp() }));
    await assertFails(updateDoc(ref, { state: 'returned' }));
    await assertSucceeds(updateDoc(bobs, { state: 'done', doneAt: serverTimestamp() }));
    await assertFails(deleteDoc(bobs));
  });

  test('an unused purchase can be given back', async () => {
    await paired();
    const ref = doc(db('alice'), 'couples', 'c1', 'purchases', 'b2');
    await assertSucceeds(setDoc(ref, { rewardId: 'drawing', name: 'Drawing', price: 25, by: 'alice', at: serverTimestamp(), state: 'owned' }));
    await assertFails(updateDoc(doc(db('bob'), 'couples', 'c1', 'purchases', 'b2'), { state: 'returned' }));
    await assertSucceeds(updateDoc(ref, { state: 'returned' }));
  });
});

describe('songs', () => {
  async function paired() {
    await startSpace();
    await assertSucceeds(joinBatch('bob').commit());
  }
  const song = (extra = {}) => ({ trackId: '4uLU6hMCjMI75M1A2tKUQC', title: 'Velvet Hours', artist: 'The Paper Moths', sentAt: serverTimestamp(), ...extra });

  test('you send under your own name, and a new one clears the old rating', async () => {
    await paired();
    await assertFails(setDoc(doc(db('alice'), 'couples', 'c1', 'songs', 'bob'), song()));
    await assertFails(setDoc(doc(db('alice'), 'couples', 'c1', 'songs', 'alice'), song({ trackId: 'not-an-id' })));
    await assertFails(setDoc(doc(db('alice'), 'couples', 'c1', 'songs', 'alice'), song({ rating: 10 })));
    await assertSucceeds(setDoc(doc(db('alice'), 'couples', 'c1', 'songs', 'alice'), song({ cover: Bytes.fromUint8Array(new Uint8Array(1000)) })));
    await assertFails(setDoc(doc(db('alice'), 'couples', 'c1', 'songs', 'alice'), song({ cover: Bytes.fromUint8Array(new Uint8Array(70000)) })));
  });

  test('only the other one rates it, 1 to 10', async () => {
    await paired();
    await assertSucceeds(setDoc(doc(db('alice'), 'couples', 'c1', 'songs', 'alice'), song()));
    await assertFails(updateDoc(doc(db('alice'), 'couples', 'c1', 'songs', 'alice'), { rating: 10, ratedAt: serverTimestamp() }));
    await assertFails(updateDoc(doc(db('bob'), 'couples', 'c1', 'songs', 'alice'), { rating: 11, ratedAt: serverTimestamp() }));
    await assertFails(updateDoc(doc(db('bob'), 'couples', 'c1', 'songs', 'alice'), { rating: 8, ratedAt: serverTimestamp(), title: 'x' }));
    await assertSucceeds(updateDoc(doc(db('bob'), 'couples', 'c1', 'songs', 'alice'), { rating: 8, ratedAt: serverTimestamp() }));
    await assertSucceeds(updateDoc(doc(db('bob'), 'couples', 'c1', 'songs', 'alice'), { rating: 9, ratedAt: serverTimestamp() }));
  });

  test('an album or a playlist goes the same way; nothing else does', async () => {
    await paired();
    await assertSucceeds(setDoc(doc(db('alice'), 'couples', 'c1', 'songs', 'alice'), song({ kind: 'album', trackId: '4aawyAB9vmqN3uQ7FjRGTy' })));
    await assertSucceeds(setDoc(doc(db('alice'), 'couples', 'c1', 'songs', 'alice'), song({ kind: 'playlist', trackId: '37i9dQZF1DXcBWIGoYBM5M' })));
    await assertSucceeds(updateDoc(doc(db('bob'), 'couples', 'c1', 'songs', 'alice'), { rating: 6, ratedAt: serverTimestamp() }));
    await assertFails(setDoc(doc(db('alice'), 'couples', 'c1', 'songs', 'alice'), song({ kind: 'episode' })));
    await assertFails(setDoc(doc(db('alice'), 'couples', 'c1', 'songs', 'alice'), song({ kind: 3 })));
  });
});
