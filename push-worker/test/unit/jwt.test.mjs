import { test } from 'node:test';
import assert from 'node:assert/strict';
import { AuthError, base64UrlDecode, base64UrlEncode, verifyIdToken } from '../../src/jwt.js';
import { accessTokenProvider } from '../../src/google.js';
import { idTokenClaims, makeKey, mint, unsigned } from '../keys.mjs';

const PROJECT = 'demo-choomhub';
const NOW = 1_800_000_000;
const key = await makeKey('k1');
const other = await makeKey('k2');
const jwks = { keys: [key.jwk] };

const rejects = (token, reason) =>
  assert.rejects(verifyIdToken(token, PROJECT, jwks, NOW), (e) => e instanceof AuthError && e.message === reason);

test('accepts a valid Firebase ID token and returns the uid', async () => {
  const payload = await verifyIdToken(await mint(key, idTokenClaims(PROJECT, 'alice', NOW)), PROJECT, jwks, NOW);
  assert.equal(payload.sub, 'alice');
});

test('rejects expired, future, wrong-project and wrong-issuer tokens', async () => {
  await rejects(await mint(key, idTokenClaims(PROJECT, 'a', NOW, { exp: NOW - 120 })), 'expired');
  await rejects(await mint(key, idTokenClaims(PROJECT, 'a', NOW, { iat: NOW + 600 })), 'issued in the future');
  await rejects(await mint(key, idTokenClaims(PROJECT, 'a', NOW, { aud: 'someone-else' })), 'wrong audience');
  await rejects(await mint(key, idTokenClaims(PROJECT, 'a', NOW, { iss: 'https://evil.example' })), 'wrong issuer');
  await rejects(await mint(key, idTokenClaims(PROJECT, 'a', NOW, { sub: '' })), 'no subject');
});

test('rejects tokens signed by an unknown or wrong key, and unsigned tokens', async () => {
  await rejects(await mint(other, idTokenClaims(PROJECT, 'a', NOW)), 'unknown key');
  const forged = await mint({ ...other, kid: 'k1' }, idTokenClaims(PROJECT, 'a', NOW));
  await rejects(forged, 'bad signature');
  await rejects(unsigned({ alg: 'none', kid: 'k1' }, idTokenClaims(PROJECT, 'a', NOW)), 'wrong algorithm');
  await rejects('not-a-jwt', 'malformed token');
  await rejects('a.b.c', 'malformed token');
});

test('base64url round-trips binary data', () => {
  const bytes = new Uint8Array([0, 255, 62, 63, 1, 2, 3]);
  assert.deepEqual(base64UrlDecode(base64UrlEncode(bytes)), bytes);
});

test('service-account assertion is signed correctly and the token is cached', async () => {
  let calls = 0;
  let assertion = null;
  const fetchFn = async (url, init) => {
    calls++;
    assert.equal(url, 'https://oauth2.googleapis.com/token');
    assertion = new URLSearchParams(init.body).get('assertion');
    return new Response(JSON.stringify({ access_token: 'ya29.test', expires_in: 3600 }), { status: 200 });
  };
  const account = { client_email: 'push@demo.iam.gserviceaccount.com', private_key: key.pem, private_key_id: 'k1' };
  const get = accessTokenProvider(account, fetchFn, () => NOW * 1000);
  assert.equal(await get(), 'ya29.test');
  assert.equal(await get(), 'ya29.test');
  assert.equal(calls, 1);

  const [h, p, s] = assertion.split('.');
  const ok = await crypto.subtle.verify({ name: 'RSASSA-PKCS1-v1_5' }, key.publicKey, base64UrlDecode(s), new TextEncoder().encode(`${h}.${p}`));
  assert.ok(ok);
  const claims = JSON.parse(new TextDecoder().decode(base64UrlDecode(p)));
  assert.equal(claims.iss, account.client_email);
  assert.equal(claims.aud, 'https://oauth2.googleapis.com/token');
  assert.match(claims.scope, /firebase\.messaging/);
  assert.match(claims.scope, /datastore/);
});
