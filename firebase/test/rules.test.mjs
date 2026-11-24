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
  doc,
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

  test('members cannot be removed or swapped', async () => {
    await assertFails(updateDoc(doc(db('alice'), 'couples', 'c1'), { members: ['alice', 'mallory'] }));
    await assertFails(updateDoc(doc(db('alice'), 'couples', 'c1'), { members: ['alice'] }));
  });
});
