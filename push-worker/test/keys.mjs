// Test-only signing keys and token minting. Nothing here is used in production.
import { base64UrlEncode, signJwt } from '../src/jwt.js';

const RS256 = { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256', modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]) };

export async function makeKey(kid) {
  const pair = await crypto.subtle.generateKey(RS256, true, ['sign', 'verify']);
  const jwk = { ...(await crypto.subtle.exportKey('jwk', pair.publicKey)), kid, alg: 'RS256', use: 'sig' };
  const pkcs8 = new Uint8Array(await crypto.subtle.exportKey('pkcs8', pair.privateKey));
  let binary = '';
  for (const b of pkcs8) binary += String.fromCharCode(b);
  const pem = `-----BEGIN PRIVATE KEY-----\n${btoa(binary).replace(/(.{64})/g, '$1\n')}\n-----END PRIVATE KEY-----\n`;
  return { kid, jwk, pem, publicKey: pair.publicKey };
}

export function idTokenClaims(projectId, uid, nowSeconds, overrides = {}) {
  return {
    iss: `https://securetoken.google.com/${projectId}`,
    aud: projectId,
    auth_time: nowSeconds - 100,
    user_id: uid,
    sub: uid,
    iat: nowSeconds - 10,
    exp: nowSeconds + 3600,
    ...overrides,
  };
}

export const mint = (key, claims) => signJwt(claims, key.pem, key.kid);

export function unsigned(header, claims) {
  return `${base64UrlEncode(JSON.stringify(header))}.${base64UrlEncode(JSON.stringify(claims))}.`;
}
