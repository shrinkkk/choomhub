// ChoomHub push Worker (Cloudflare Workers, free plan).
//
// POST /v1/notify  { "event": "meetup_set", "detail": "Sat 14 Dec, 7:30 pm" }
//                  { "event": "gesture", "kind": "hug" }
//                  { "event": "mood", "kind": "sleepy", "detail": "headphones on" }
// Authorization: Bearer <Firebase ID token of the sender>
//
// The Worker checks who is asking, finds their partner through Firestore, respects
// the partner's notification settings and sends the notification with FCM. The app
// never holds the service-account key; only this Worker does (as a secret).

import { AuthError, verifyIdToken } from './jwt.js';
import { Events, compose, fcmMessage, parseRequest } from './events.js';
import { Firestore, accessTokenProvider, field, jwksProvider, readServiceAccount, sendFcm } from './google.js';

const MAX_BODY_BYTES = 2048;

function json(status, body) {
  return new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json' } });
}

/**
 * @param {Request} request
 * @param {{ SERVICE_ACCOUNT_JSON: string, FIRESTORE_DATABASE?: string }} env
 * @param {object} deps injectable for tests: fetch, now, getJwks, getAccessToken, firestoreRoot, fcmRoot, projectId
 */
export async function handle(request, env, deps) {
  const url = new URL(request.url);
  if (url.pathname !== '/v1/notify') return json(404, { error: 'not-found' });
  if (request.method !== 'POST') return json(405, { error: 'method-not-allowed' });

  const auth = request.headers.get('authorization') || '';
  if (!auth.startsWith('Bearer ')) return json(401, { error: 'unauthenticated' });

  let uid;
  try {
    const claims = await verifyIdToken(auth.slice(7), deps.projectId, await deps.getJwks(), Math.floor(deps.now() / 1000));
    uid = claims.sub;
  } catch (e) {
    if (e instanceof AuthError) return json(401, { error: 'unauthenticated' });
    throw e;
  }

  const raw = await request.text();
  if (raw.length > MAX_BODY_BYTES) return json(413, { error: 'too-large' });
  let parsed = null;
  try {
    parsed = parseRequest(JSON.parse(raw));
  } catch {
    parsed = null;
  }
  if (!parsed) return json(400, { error: 'bad-request' });
  const spec = Events[parsed.event];

  const db = new Firestore({
    root: deps.firestoreRoot,
    projectId: deps.projectId,
    database: env.FIRESTORE_DATABASE || '(default)',
    accessToken: deps.getAccessToken,
    fetchFn: deps.fetch,
  });

  // Who receives it: yourself for the test, otherwise your partner (never anyone else).
  let recipient = uid;
  let coupleId = null;
  if (!spec.toSelf) {
    coupleId = field.string(await db.get(`users/${uid}`), 'coupleId');
    if (!coupleId) return json(403, { error: 'not-paired' });
    const members = field.strings(await db.get(`couples/${coupleId}`), 'members');
    if (!members.includes(uid)) return json(403, { error: 'not-paired' });
    const partner = members.find((m) => m !== uid);
    if (!partner) return json(403, { error: 'not-paired' });
    recipient = partner;
  }

  if (spec.category) {
    const settings = await db.get(`users/${recipient}/settings/notifications`);
    if (field.bool(settings, spec.category) === false) return json(200, { sent: 0, reason: 'muted' });
  }

  const devices = await db.listIds(`users/${recipient}/devices`);
  if (devices.length === 0) return json(200, { sent: 0, reason: 'no-devices' });

  const ctx = { detail: parsed.detail, kind: parsed.kind };
  if (coupleId) {
    ctx.senderName = field.string(await db.get(`couples/${coupleId}/names/${recipient}`), 'nameForPartner');
    if (parsed.event === 'renamed') {
      ctx.recipientNewName = field.string(await db.get(`couples/${coupleId}/names/${uid}`), 'nameForPartner');
    }
  }
  const text = compose(parsed.event, ctx);

  const fcm = { root: deps.fcmRoot, projectId: deps.projectId, accessToken: deps.getAccessToken, fetchFn: deps.fetch };
  const results = await Promise.all(
    devices.map(async (fid) => {
      const outcome = await sendFcm(fcm, fcmMessage(fid, parsed.event, text, parsed.kind));
      if (outcome === 'unregistered') await db.delete(`users/${recipient}/devices/${fid}`);
      return outcome;
    }),
  );
  const sent = results.filter((r) => r === 'sent').length;
  return json(200, { sent, failed: results.length - sent, ...(sent === 0 ? { reason: 'undeliverable' } : {}) });
}

let shared = null;

function productionDeps(env) {
  // Built once per Worker instance so access tokens and signing keys are cached.
  if (!shared) {
    const account = readServiceAccount(env.SERVICE_ACCOUNT_JSON);
    const fetchFn = (...args) => fetch(...args);
    shared = {
      fetch: fetchFn,
      now: () => Date.now(),
      projectId: account.project_id,
      getJwks: jwksProvider(fetchFn),
      getAccessToken: accessTokenProvider(account, fetchFn),
      firestoreRoot: 'https://firestore.googleapis.com',
      fcmRoot: 'https://fcm.googleapis.com',
    };
  }
  return shared;
}

export default {
  async fetch(request, env) {
    try {
      return await handle(request, env, productionDeps(env));
    } catch (e) {
      // Never echo internals (or anything from the request) back to the caller.
      console.error('notify failed', e instanceof Error ? e.message : 'unknown');
      return json(500, { error: 'internal' });
    }
  },
};
