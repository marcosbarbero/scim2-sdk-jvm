/**
 * SCIM API client — works with both Spring Boot (/api) and plain Java (/scim/v2) backends.
 *
 * Set VITE_API_MODE=scim to call SCIM endpoints directly (plain Java backend).
 * Default is 'rest' which calls the /api REST endpoints (Spring Boot backend).
 */
const API_MODE = import.meta.env.VITE_API_MODE || 'rest';
const IS_SCIM = API_MODE === 'scim';

const API_BASE = IS_SCIM ? '/scim/v2' : '/api';
const CONTENT_TYPE = IS_SCIM ? 'application/scim+json' : 'application/json';
const USERS_PATH = IS_SCIM ? '/Users' : '/users';
const GROUPS_PATH = IS_SCIM ? '/Groups' : '/groups';
const USER_SCHEMA = 'urn:ietf:params:scim:schemas:core:2.0:User';
const GROUP_SCHEMA = 'urn:ietf:params:scim:schemas:core:2.0:Group';

function wrapBody(body, schema) {
  if (!IS_SCIM) return JSON.stringify(body);
  return JSON.stringify({ schemas: [schema], ...body });
}

async function request(path, options = {}, getToken) {
  const token = await getToken();
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': CONTENT_TYPE,
      Authorization: `Bearer ${token}`,
      ...options.headers,
    },
  });

  if (res.status === 204) return null;

  if (!res.ok) {
    const body = await res.text();
    throw new Error(`HTTP ${res.status}: ${body}`);
  }

  return res.json();
}

// Users
export const listUsers = (getToken, { startIndex = 1, count = 20, filter } = {}) => {
  const params = new URLSearchParams({ startIndex, count });
  if (filter) params.set('filter', filter);
  return request(`${USERS_PATH}?${params}`, {}, getToken);
};

export const getUser = (getToken, id) =>
  request(`${USERS_PATH}/${id}`, {}, getToken);

export const createUser = (getToken, user) =>
  request(USERS_PATH, { method: 'POST', body: wrapBody(user, USER_SCHEMA) }, getToken);

export const updateUser = (getToken, id, user) =>
  request(`${USERS_PATH}/${id}`, { method: 'PUT', body: wrapBody(user, USER_SCHEMA) }, getToken);

export const deleteUser = (getToken, id) =>
  request(`${USERS_PATH}/${id}`, { method: 'DELETE' }, getToken);

// Groups
export const listGroups = (getToken, { startIndex = 1, count = 20, filter } = {}) => {
  const params = new URLSearchParams({ startIndex, count });
  if (filter) params.set('filter', filter);
  return request(`${GROUPS_PATH}?${params}`, {}, getToken);
};

export const getGroup = (getToken, id) =>
  request(`${GROUPS_PATH}/${id}`, {}, getToken);

export const createGroup = (getToken, group) =>
  request(GROUPS_PATH, { method: 'POST', body: wrapBody(group, GROUP_SCHEMA) }, getToken);

export const updateGroup = (getToken, id, group) =>
  request(`${GROUPS_PATH}/${id}`, { method: 'PUT', body: wrapBody(group, GROUP_SCHEMA) }, getToken);

export const deleteGroup = (getToken, id) =>
  request(`${GROUPS_PATH}/${id}`, { method: 'DELETE' }, getToken);
