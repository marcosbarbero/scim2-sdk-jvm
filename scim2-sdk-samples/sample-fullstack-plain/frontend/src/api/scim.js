const API_BASE = '/scim/v2';

async function request(path, options = {}, getToken) {
  const token = await getToken();
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/scim+json',
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
  return request(`/Users?${params}`, {}, getToken);
};

export const getUser = (getToken, id) =>
  request(`/Users/${id}`, {}, getToken);

export const createUser = (getToken, user) =>
  request('/Users', {
    method: 'POST',
    body: JSON.stringify({
      schemas: ['urn:ietf:params:scim:schemas:core:2.0:User'],
      ...user,
    }),
  }, getToken);

export const updateUser = (getToken, id, user) =>
  request(`/Users/${id}`, {
    method: 'PUT',
    body: JSON.stringify({
      schemas: ['urn:ietf:params:scim:schemas:core:2.0:User'],
      ...user,
    }),
  }, getToken);

export const deleteUser = (getToken, id) =>
  request(`/Users/${id}`, { method: 'DELETE' }, getToken);

// Groups
export const listGroups = (getToken, { startIndex = 1, count = 20, filter } = {}) => {
  const params = new URLSearchParams({ startIndex, count });
  if (filter) params.set('filter', filter);
  return request(`/Groups?${params}`, {}, getToken);
};

export const getGroup = (getToken, id) =>
  request(`/Groups/${id}`, {}, getToken);

export const createGroup = (getToken, group) =>
  request('/Groups', {
    method: 'POST',
    body: JSON.stringify({
      schemas: ['urn:ietf:params:scim:schemas:core:2.0:Group'],
      ...group,
    }),
  }, getToken);

export const updateGroup = (getToken, id, group) =>
  request(`/Groups/${id}`, {
    method: 'PUT',
    body: JSON.stringify({
      schemas: ['urn:ietf:params:scim:schemas:core:2.0:Group'],
      ...group,
    }),
  }, getToken);

export const deleteGroup = (getToken, id) =>
  request(`/Groups/${id}`, { method: 'DELETE' }, getToken);
