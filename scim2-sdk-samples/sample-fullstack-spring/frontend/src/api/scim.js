const API_BASE = '/api';

async function request(path, options = {}, getToken) {
  const token = await getToken();
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
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
  return request(`/users?${params}`, {}, getToken);
};

export const getUser = (getToken, id) =>
  request(`/users/${id}`, {}, getToken);

export const createUser = (getToken, user) =>
  request('/users', { method: 'POST', body: JSON.stringify(user) }, getToken);

export const updateUser = (getToken, id, user) =>
  request(`/users/${id}`, { method: 'PUT', body: JSON.stringify(user) }, getToken);

export const deleteUser = (getToken, id) =>
  request(`/users/${id}`, { method: 'DELETE' }, getToken);

// Groups
export const listGroups = (getToken, { startIndex = 1, count = 20, filter } = {}) => {
  const params = new URLSearchParams({ startIndex, count });
  if (filter) params.set('filter', filter);
  return request(`/groups?${params}`, {}, getToken);
};

export const getGroup = (getToken, id) =>
  request(`/groups/${id}`, {}, getToken);

export const createGroup = (getToken, group) =>
  request('/groups', { method: 'POST', body: JSON.stringify(group) }, getToken);

export const updateGroup = (getToken, id, group) =>
  request(`/groups/${id}`, { method: 'PUT', body: JSON.stringify(group) }, getToken);

export const deleteGroup = (getToken, id) =>
  request(`/groups/${id}`, { method: 'DELETE' }, getToken);
