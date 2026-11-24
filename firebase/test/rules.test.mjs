// Firestore security rules tests. Run with: npm run test:rules (needs Java for the emulator).
import { after, before, beforeEach, describe, test } from 'node:test';
import { readFileSync } from 'node:fs';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  arrayUnion,
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
    await assertSucceeds(setDoc(mine, { mood: 'sleepy', note: 'rain again', updatedAt: serverTimestamp() }));
    await assertSucceeds(setDoc(mine, { mood: 'missing_you', note: null, updatedAt: serverTimestamp() }));
    await assertSucceeds(getDoc(doc(db('bob'), 'couples/c1/moods/alice')));
    await assertFails(setDoc(doc(db('bob'), 'couples/c1/moods/alice'), { mood: 'low', note: null, updatedAt: serverTimestamp() }));
    await assertFails(setDoc(mine, { mood: 'furious', note: null, updatedAt: serverTimestamp() }));
    await assertFails(setDoc(mine, { mood: 'calm', note: 'x'.repeat(81), updatedAt: serverTimestamp() }));
    await assertFails(getDoc(doc(db('mallory'), 'couples/c1/moods/alice')));
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
