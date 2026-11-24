// JWT helpers built on WebCrypto, which both Cloudflare Workers and Node 20+ provide.
// Two jobs: verify the Firebase ID token the app sends, and sign the service-account
// assertion that is exchanged for a Google access token.

const RS256 = { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' };
const encoder = new TextEncoder();

export function base64UrlEncode(bytes) {
  const data = typeof bytes === 'string' ? encoder.encode(bytes) : new Uint8Array(bytes);
  let binary = '';
  for (const b of data) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

export function base64UrlDecode(text) {
  const padded = text.replace(/-/g, '+').replace(/_/g, '/') + '==='.slice((text.length + 3) % 4);
  const binary = atob(padded);
  const out = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) out[i] = binary.charCodeAt(i);
  return out;
}

function decodeJson(part) {
  return JSON.parse(new TextDecoder().decode(base64UrlDecode(part)));
}

export class AuthError extends Error {}

/** Allowed clock difference between Google, Cloudflare and the phone, in seconds. */
const SKEW_SECONDS = 60;

/**
 * Verifies a Firebase Auth ID token and returns its payload (`sub` is the uid).
 * Follows Firebase's documented checks: RS256, a known key id, valid signature,
 * not expired, issued in the past, audience = project id, issuer = securetoken.
 *
 * @param {string} token
 * @param {string} projectId
 * @param {{ keys: JsonWebKey[] }} jwks Google's securetoken public keys.
 * @param {number} nowSeconds
 */
export async function verifyIdToken(token, projectId, jwks, nowSeconds) {
  const parts = typeof token === 'string' ? token.split('.') : [];
  if (parts.length !== 3) throw new AuthError('malformed token');
  let header;
  let payload;
  try {
    header = decodeJson(parts[0]);
    payload = decodeJson(parts[1]);
  } catch {
    throw new AuthError('malformed token');
  }
  if (header.alg !== 'RS256') throw new AuthError('wrong algorithm');
  const jwk = jwks.keys.find((k) => k.kid === header.kid);
  if (!jwk) throw new AuthError('unknown key');

  const key = await crypto.subtle.importKey('jwk', { kty: jwk.kty, n: jwk.n, e: jwk.e, alg: 'RS256', ext: true }, RS256, false, ['verify']);
  const valid = await crypto.subtle.verify(RS256, key, base64UrlDecode(parts[2]), encoder.encode(`${parts[0]}.${parts[1]}`));
  if (!valid) throw new AuthError('bad signature');

  if (typeof payload.exp !== 'number' || payload.exp + SKEW_SECONDS <= nowSeconds) throw new AuthError('expired');
  if (typeof payload.iat !== 'number' || payload.iat - SKEW_SECONDS > nowSeconds) throw new AuthError('issued in the future');
  if (typeof payload.auth_time === 'number' && payload.auth_time - SKEW_SECONDS > nowSeconds) throw new AuthError('auth in the future');
  if (payload.aud !== projectId) throw new AuthError('wrong audience');
  if (payload.iss !== `https://securetoken.google.com/${projectId}`) throw new AuthError('wrong issuer');
  if (typeof payload.sub !== 'string' || payload.sub.length === 0 || payload.sub.length > 128) throw new AuthError('no subject');
  return payload;
}

function pemToPkcs8(pem) {
  const body = pem.replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\s/g, '');
  const binary = atob(body);
  const out = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) out[i] = binary.charCodeAt(i);
  return out;
}

/** Signs an RS256 JWT with a service account's PEM private key. */
export async function signJwt(claims, privateKeyPem, keyId) {
  const header = { alg: 'RS256', typ: 'JWT', ...(keyId ? { kid: keyId } : {}) };
  const unsigned = `${base64UrlEncode(JSON.stringify(header))}.${base64UrlEncode(JSON.stringify(claims))}`;
  const key = await crypto.subtle.importKey('pkcs8', pemToPkcs8(privateKeyPem), RS256, false, ['sign']);
  const signature = await crypto.subtle.sign(RS256, key, encoder.encode(unsigned));
  return `${unsigned}.${base64UrlEncode(signature)}`;
}
