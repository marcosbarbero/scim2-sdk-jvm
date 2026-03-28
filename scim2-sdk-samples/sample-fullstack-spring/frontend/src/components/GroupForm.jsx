import { useEffect, useState, useCallback } from 'react';
import { useKeycloak } from '../auth/KeycloakProvider';
import { useNavigate, useParams } from 'react-router-dom';
import { getGroup, createGroup, updateGroup, listUsers } from '../api/scim';

export default function GroupForm() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const { getToken } = useKeycloak();
  const navigate = useNavigate();
  const [displayName, setDisplayName] = useState('');
  const [members, setMembers] = useState([]);
  const [availableUsers, setAvailableUsers] = useState([]);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const usersData = await listUsers(getToken, { count: 100 });
        setAvailableUsers(usersData.Resources || []);
      } catch (err) {
        setError(err.message);
      }
    })();
  }, [getToken]);

  useEffect(() => {
    if (!isEdit) return;
    (async () => {
      try {
        const group = await getGroup(getToken, id);
        setDisplayName(group.displayName || '');
        setMembers(group.members || []);
      } catch (err) {
        setError(err.message);
      }
    })();
  }, [id, isEdit, getToken]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);

    const payload = {
      schemas: ['urn:ietf:params:scim:schemas:core:2.0:Group'],
      displayName,
      members,
    };

    try {
      if (isEdit) {
        await updateGroup(getToken, id, payload);
      } else {
        await createGroup(getToken, payload);
      }
      navigate('/groups');
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const addMember = (userId) => {
    const user = availableUsers.find((u) => u.id === userId);
    if (!user || members.some((m) => m.value === userId)) return;
    setMembers([...members, { value: user.id, display: user.displayName || user.userName, type: 'User' }]);
  };

  const removeMember = (userId) => {
    setMembers(members.filter((m) => m.value !== userId));
  };

  return (
    <div>
      <h2>{isEdit ? 'Edit Group' : 'New Group'}</h2>
      {error && <div style={styles.error}>{error}</div>}

      <form onSubmit={handleSubmit} style={styles.form}>
        <label style={styles.label}>
          Display Name *
          <input
            style={styles.input}
            required
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
          />
        </label>

        <div style={styles.label}>
          Members
          <select style={styles.input} onChange={(e) => { addMember(e.target.value); e.target.value = ''; }}>
            <option value="">Add a member...</option>
            {availableUsers
              .filter((u) => !members.some((m) => m.value === u.id))
              .map((u) => (
                <option key={u.id} value={u.id}>
                  {u.displayName || u.userName}
                </option>
              ))}
          </select>

          {members.length > 0 && (
            <ul style={styles.memberList}>
              {members.map((m) => (
                <li key={m.value} style={styles.memberItem}>
                  <span>{m.display || m.value}</span>
                  <button type="button" style={styles.removeBtn} onClick={() => removeMember(m.value)}>
                    Remove
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div style={styles.actions}>
          <button type="button" style={styles.cancelBtn} onClick={() => navigate('/groups')}>
            Cancel
          </button>
          <button type="submit" style={styles.saveBtn} disabled={saving}>
            {saving ? 'Saving...' : isEdit ? 'Update' : 'Create'}
          </button>
        </div>
      </form>
    </div>
  );
}

const styles = {
  form: { marginTop: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem', maxWidth: 520 },
  label: { display: 'flex', flexDirection: 'column', gap: '0.25rem', fontWeight: 500, fontSize: '0.9rem' },
  input: { padding: '0.5rem 0.75rem', border: '1px solid #ddd', borderRadius: 6, fontSize: '0.9rem' },
  error: { background: '#fef2f2', color: '#dc2626', border: '1px solid #fecaca', borderRadius: 8, padding: '0.75rem 1rem', marginTop: '1rem' },
  memberList: { listStyle: 'none', marginTop: '0.5rem', display: 'flex', flexDirection: 'column', gap: '0.25rem' },
  memberItem: {
    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
    background: '#f8f9fa', padding: '0.5rem 0.75rem', borderRadius: 6,
  },
  removeBtn: { background: '#fff0f0', color: '#dc2626', border: 'none', borderRadius: 4, padding: '0.25rem 0.5rem', cursor: 'pointer', fontSize: '0.8rem' },
  actions: { display: 'flex', gap: '0.75rem', marginTop: '0.5rem' },
  cancelBtn: { background: '#e0e0e0', border: 'none', borderRadius: 8, padding: '0.625rem 1.25rem', cursor: 'pointer', fontWeight: 500 },
  saveBtn: { background: '#635bff', color: '#fff', border: 'none', borderRadius: 8, padding: '0.625rem 1.25rem', fontWeight: 600, cursor: 'pointer' },
};
