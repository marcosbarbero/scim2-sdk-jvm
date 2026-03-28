import { useEffect, useState, useCallback } from 'react';
import { useKeycloak } from '../auth/KeycloakProvider';
import { useNavigate, useParams } from 'react-router-dom';
import { getUser, createUser, updateUser } from '../api/scim';

const EMPTY = {
  userName: '',
  displayName: '',
  active: true,
  name: { givenName: '', familyName: '' },
  emails: [{ value: '', type: 'work', primary: true }],
  phoneNumbers: [{ value: '', type: 'work' }],
};

export default function UserForm() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const { getToken } = useKeycloak();
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!isEdit) return;
    (async () => {
      try {
        const user = await getUser(getToken, id);
        setForm({
          userName: user.userName || '',
          displayName: user.displayName || '',
          active: user.active !== false,
          name: {
            givenName: user.name?.givenName || '',
            familyName: user.name?.familyName || '',
          },
          emails: user.emails?.length ? user.emails : EMPTY.emails,
          phoneNumbers: user.phoneNumbers?.length ? user.phoneNumbers : EMPTY.phoneNumbers,
        });
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
      schemas: ['urn:ietf:params:scim:schemas:core:2.0:User'],
      userName: form.userName,
      displayName: form.displayName,
      active: form.active,
      name: form.name,
      emails: form.emails.filter((em) => em.value),
      phoneNumbers: form.phoneNumbers.filter((ph) => ph.value),
    };

    try {
      if (isEdit) {
        await updateUser(getToken, id, payload);
      } else {
        await createUser(getToken, payload);
      }
      navigate('/users');
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const set = (field, value) => setForm((prev) => ({ ...prev, [field]: value }));
  const setName = (field, value) =>
    setForm((prev) => ({ ...prev, name: { ...prev.name, [field]: value } }));
  const setEmail = (idx, value) =>
    setForm((prev) => ({
      ...prev,
      emails: prev.emails.map((e, i) => (i === idx ? { ...e, value } : e)),
    }));
  const setPhone = (idx, value) =>
    setForm((prev) => ({
      ...prev,
      phoneNumbers: prev.phoneNumbers.map((p, i) => (i === idx ? { ...p, value } : p)),
    }));

  return (
    <div>
      <h2>{isEdit ? 'Edit User' : 'New User'}</h2>
      {error && <div style={styles.error}>{error}</div>}

      <form onSubmit={handleSubmit} style={styles.form}>
        <label style={styles.label}>
          Username *
          <input style={styles.input} required value={form.userName} onChange={(e) => set('userName', e.target.value)} />
        </label>

        <label style={styles.label}>
          Display Name
          <input style={styles.input} value={form.displayName} onChange={(e) => set('displayName', e.target.value)} />
        </label>

        <div style={styles.row}>
          <label style={{ ...styles.label, flex: 1 }}>
            Given Name
            <input style={styles.input} value={form.name.givenName} onChange={(e) => setName('givenName', e.target.value)} />
          </label>
          <label style={{ ...styles.label, flex: 1 }}>
            Family Name
            <input style={styles.input} value={form.name.familyName} onChange={(e) => setName('familyName', e.target.value)} />
          </label>
        </div>

        <label style={styles.label}>
          Email
          <input style={styles.input} type="email" value={form.emails[0]?.value || ''} onChange={(e) => setEmail(0, e.target.value)} />
        </label>

        <label style={styles.label}>
          Phone
          <input style={styles.input} value={form.phoneNumbers[0]?.value || ''} onChange={(e) => setPhone(0, e.target.value)} />
        </label>

        <label style={styles.checkboxLabel}>
          <input type="checkbox" checked={form.active} onChange={(e) => set('active', e.target.checked)} />
          Active
        </label>

        <div style={styles.actions}>
          <button type="button" style={styles.cancelBtn} onClick={() => navigate('/users')}>
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
  row: { display: 'flex', gap: '1rem' },
  checkboxLabel: { display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 500, fontSize: '0.9rem' },
  error: { background: '#fef2f2', color: '#dc2626', border: '1px solid #fecaca', borderRadius: 8, padding: '0.75rem 1rem', marginTop: '1rem' },
  actions: { display: 'flex', gap: '0.75rem', marginTop: '0.5rem' },
  cancelBtn: { background: '#e0e0e0', border: 'none', borderRadius: 8, padding: '0.625rem 1.25rem', cursor: 'pointer', fontWeight: 500 },
  saveBtn: { background: '#635bff', color: '#fff', border: 'none', borderRadius: 8, padding: '0.625rem 1.25rem', fontWeight: 600, cursor: 'pointer' },
};
