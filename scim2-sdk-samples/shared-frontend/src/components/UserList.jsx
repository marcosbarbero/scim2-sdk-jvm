import { useEffect, useState, useCallback } from 'react';
import { useKeycloak } from '../auth/KeycloakProvider';
import { useNavigate } from 'react-router-dom';
import { listUsers, deleteUser } from '../api/scim';

export default function UserList() {
  const { getToken } = useKeycloak();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [filter, setFilter] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await listUsers(getToken, { filter: filter || undefined });
      setData(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [getToken, filter]);

  useEffect(() => { fetchUsers(); }, [fetchUsers]);

  const handleDelete = async (id, userName) => {
    if (!confirm(`Delete user "${userName}"?`)) return;
    try {
      await deleteUser(getToken, id);
      fetchUsers();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div>
      <div style={styles.header}>
        <h2>Users</h2>
        <button style={styles.createBtn} onClick={() => navigate('/users/new')}>
          + New User
        </button>
      </div>

      <div style={styles.filterRow}>
        <input
          style={styles.filterInput}
          placeholder='Search users or SCIM filter (e.g. userName eq "john")'
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && fetchUsers()}
        />
        <button style={styles.filterBtn} onClick={fetchUsers}>Search</button>
      </div>

      {error && <div style={styles.error}>{error}</div>}

      {loading ? (
        <p style={styles.muted}>Loading...</p>
      ) : (
        <>
          <p style={styles.muted}>
            {data?.totalResults ?? 0} user(s) found
          </p>
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>Username</th>
                <th style={styles.th}>Display Name</th>
                <th style={styles.th}>Email</th>
                <th style={styles.th}>Active</th>
                <th style={styles.th}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {(data?.Resources || []).map((u) => (
                <tr key={u.id} style={styles.tr}>
                  <td style={styles.td}>{u.userName}</td>
                  <td style={styles.td}>{u.displayName || '-'}</td>
                  <td style={styles.td}>
                    {u.emails?.find((e) => e.primary)?.value || u.emails?.[0]?.value || '-'}
                  </td>
                  <td style={styles.td}>
                    <span style={{ color: u.active !== false ? '#22c55e' : '#ef4444' }}>
                      {u.active !== false ? 'Yes' : 'No'}
                    </span>
                  </td>
                  <td style={styles.td}>
                    <button style={styles.editBtn} onClick={() => navigate(`/users/${u.id}`)}>
                      Edit
                    </button>
                    <button style={styles.deleteBtn} onClick={() => handleDelete(u.id, u.userName)}>
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
              {(!data?.Resources || data.Resources.length === 0) && (
                <tr>
                  <td colSpan={5} style={{ ...styles.td, textAlign: 'center', color: '#888' }}>
                    No users found
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </>
      )}
    </div>
  );
}

const styles = {
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' },
  createBtn: {
    background: '#635bff', color: '#fff', border: 'none', borderRadius: 8,
    padding: '0.625rem 1.25rem', fontWeight: 600, cursor: 'pointer',
  },
  filterRow: { display: 'flex', gap: '0.5rem', marginBottom: '1rem' },
  filterInput: {
    flex: 1, padding: '0.5rem 0.75rem', border: '1px solid #ddd', borderRadius: 6,
    fontSize: '0.9rem',
  },
  filterBtn: {
    background: '#e0e0e0', border: 'none', borderRadius: 6,
    padding: '0.5rem 1rem', cursor: 'pointer', fontWeight: 500,
  },
  error: {
    background: '#fef2f2', color: '#dc2626', border: '1px solid #fecaca',
    borderRadius: 8, padding: '0.75rem 1rem', marginBottom: '1rem',
  },
  muted: { color: '#888', fontSize: '0.85rem', marginBottom: '0.75rem' },
  table: { width: '100%', borderCollapse: 'collapse', background: '#fff', borderRadius: 8, overflow: 'hidden', boxShadow: '0 1px 3px rgba(0,0,0,0.08)' },
  th: { textAlign: 'left', padding: '0.75rem 1rem', background: '#f8f9fa', fontWeight: 600, fontSize: '0.85rem', borderBottom: '1px solid #eee' },
  tr: { borderBottom: '1px solid #f0f0f0' },
  td: { padding: '0.75rem 1rem', fontSize: '0.9rem' },
  editBtn: { background: '#f0f0ff', color: '#635bff', border: 'none', borderRadius: 4, padding: '0.25rem 0.75rem', cursor: 'pointer', marginRight: '0.5rem', fontWeight: 500 },
  deleteBtn: { background: '#fff0f0', color: '#dc2626', border: 'none', borderRadius: 4, padding: '0.25rem 0.75rem', cursor: 'pointer', fontWeight: 500 },
};
