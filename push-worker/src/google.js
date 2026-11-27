// Small clients for the Google APIs the Worker needs: OAuth (service account),
// Firestore REST (read-only, plus deleting dead device registrations) and FCM HTTP v1.

import { signJwt } from './jwt.js';

const TOKEN_URL = 'https://oauth2.googleapis.com/token';
const SCOPES = 'https://www.googleapis.com/auth/datastore https://www.googleapis.com/auth/firebase.messaging';
const JWKS_URL = 'https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com';

/** Parses and sanity-checks the SERVICE_ACCOUNT_JSON secret. */
export function readServiceAccount(json) {
  let account;
  try {
    account = JSON.parse(json || '');
  } catch {
    throw new Error('SERVICE_ACCOUNT_JSON is not valid JSON');
  }
  for (const field of ['project_id', 'client_email', 'private_key']) {
    if (typeof account[field] !== 'string' || account[field].length === 0) {
      throw new Error(`SERVICE_ACCOUNT_JSON is missing ${field}`);
    }
  }
  return account;
}

/** A cached Google access token for the service account (tokens live an hour). */
export function accessTokenProvider(account, fetchFn, now = Date.now) {
  let cached = null;
  return async () => {
    if (cached && cached.expiresAt - 60_000 > now()) return cached.token;
    const iat = Math.floor(now() / 1000);
    const assertion = await signJwt(
      { iss: account.client_email, scope: SCOPES, aud: TOKEN_URL, iat, exp: iat + 3600 },
      account.private_key,
      account.private_key_id,
    );
    const response = await fetchFn(TOKEN_URL, {
      method: 'POST',
      headers: { 'content-type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer', assertion }),
    });
    if (!response.ok) throw new Error(`token exchange failed: ${response.status}`);
    const body = await response.json();
    cached = { token: body.access_token, expiresAt: now() + (body.expires_in ?? 3600) * 1000 };
    return cached.token;
  };
}

/** Google's ID-token signing keys, cached for as long as Google says. */
export function jwksProvider(fetchFn, now = Date.now) {
  let cached = null;
  return async () => {
    if (cached && cached.expiresAt > now()) return cached.jwks;
    const response = await fetchFn(JWKS_URL);
    if (!response.ok) throw new Error(`could not load signing keys: ${response.status}`);
    const maxAge = /max-age=(\d+)/.exec(response.headers.get('cache-control') || '');
    cached = { jwks: await response.json(), expiresAt: now() + (maxAge ? Number(maxAge[1]) : 3600) * 1000 };
    return cached.jwks;
  };
}

/** Minimal Firestore REST reader. Missing documents come back as null. */
export class Firestore {
  constructor({ root, projectId, database = '(default)', accessToken, fetchFn }) {
    this.root = root;
    this.documents = `projects/${projectId}/databases/${database}/documents`;
    this.accessToken = accessToken;
    this.fetchFn = fetchFn;
  }

  async request(path, init = {}) {
    const token = await this.accessToken();
    return this.fetchFn(`${this.root}/v1/${path}`, { ...init, headers: { ...init.headers, authorization: `Bearer ${token}` } });
  }

  async get(docPath) {
    const response = await this.request(`${this.documents}/${docPath}`);
    if (response.status === 404) return null;
    if (!response.ok) throw new Error(`firestore get ${response.status}`);
    return (await response.json()).fields ?? {};
  }

  /** Document ids in a collection (first page only; a person has a handful of phones). */
  async listIds(collectionPath, pageSize = 20) {
    const response = await this.request(`${this.documents}/${collectionPath}?pageSize=${pageSize}&mask.fieldPaths=updatedAt`);
    if (response.status === 404) return [];
    if (!response.ok) throw new Error(`firestore list ${response.status}`);
    const body = await response.json();
    return (body.documents ?? []).map((d) => d.name.slice(d.name.lastIndexOf('/') + 1));
  }

  /**
   * Sets some entries of a map field and removes others, touching nothing else in the document
   * (which is created if it's missing). Field-level, so two at once don't undo each other.
   */
  async setMapEntries(docPath, mapField, set, remove = []) {
    const quote = (k) => `${mapField}.\`${k.replace(/[`\\]/g, '')}\``;
    const mask = [...Object.keys(set), ...remove].map((k) => `updateMask.fieldPaths=${encodeURIComponent(quote(k))}`).join('&');
    const fields = { [mapField]: { mapValue: { fields: Object.fromEntries(Object.entries(set).map(([k, v]) => [k, { integerValue: String(v) }])) } } };
    const response = await this.request(`${this.documents}/${docPath}?${mask}`, {
      method: 'PATCH',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ fields }),
    });
    return response.ok;
  }

  async delete(docPath) {
    const response = await this.request(`${this.documents}/${docPath}`, { method: 'DELETE' });
    return response.ok;
  }
}

export const field = {
  string: (fields, name) => (fields && typeof fields[name]?.stringValue === 'string' ? fields[name].stringValue : null),
  bool: (fields, name) => (fields && typeof fields[name]?.booleanValue === 'boolean' ? fields[name].booleanValue : null),
  strings: (fields, name) => (fields?.[name]?.arrayValue?.values ?? []).map((v) => v.stringValue).filter((v) => typeof v === 'string'),
  /** A map of strings, e.g. couples/{id}.ids. */
  /** A map of whole numbers, e.g. users/{uid}/push/recent.sent (push id -> when, epoch ms). */
  intMap: (fields, name) => Object.fromEntries(Object.entries(fields?.[name]?.mapValue?.fields ?? {}).filter(([, v]) => v?.integerValue !== undefined).map(([k, v]) => [k, Number(v.integerValue)])),
  stringMap: (fields, name) => Object.fromEntries(Object.entries(fields?.[name]?.mapValue?.fields ?? {}).filter(([, v]) => typeof v?.stringValue === 'string').map(([k, v]) => [k, v.stringValue])),
};

/**
 * Sends one FCM HTTP v1 message.
 * @returns {Promise<'sent' | 'unregistered' | 'failed'>}
 */
export async function sendFcm({ root, projectId, accessToken, fetchFn }, message) {
  const token = await accessToken();
  const response = await fetchFn(`${root}/v1/projects/${projectId}/messages:send`, {
    method: 'POST',
    headers: { authorization: `Bearer ${token}`, 'content-type': 'application/json' },
    body: JSON.stringify(message),
  });
  if (response.ok) return 'sent';
  // Only UNREGISTERED (404) means the registration is gone for good; a 400 could
  // also be our own payload, so those registrations are kept (Firebase's guidance).
  if (response.status === 404) return 'unregistered';
  return 'failed';
}
