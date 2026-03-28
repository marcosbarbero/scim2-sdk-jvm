import { useKeycloak } from './auth/KeycloakProvider';
import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import UserList from './components/UserList';
import UserForm from './components/UserForm';
import GroupList from './components/GroupList';
import GroupForm from './components/GroupForm';

function App() {
  const { initialized, authenticated, login } = useKeycloak();

  if (!initialized) {
    return (
      <div style={styles.loading}>
        <div style={styles.spinner} />
        <p>Loading...</p>
      </div>
    );
  }

  if (!authenticated) {
    return (
      <div style={styles.loginContainer}>
        <div style={styles.loginCard}>
          <h1 style={styles.loginTitle}>SCIM Sample</h1>
          <p style={styles.loginSubtitle}>
            SCIM 2.0 User & Group Management with Keycloak
          </p>
          <button style={styles.loginButton} onClick={login}>
            Log In with Keycloak
          </button>
        </div>
      </div>
    );
  }

  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Navigate to="/users" replace />} />
        <Route path="/users" element={<UserList />} />
        <Route path="/users/new" element={<UserForm />} />
        <Route path="/users/:id" element={<UserForm />} />
        <Route path="/groups" element={<GroupList />} />
        <Route path="/groups/new" element={<GroupForm />} />
        <Route path="/groups/:id" element={<GroupForm />} />
      </Routes>
    </Layout>
  );
}

const styles = {
  loading: {
    display: 'flex', flexDirection: 'column', alignItems: 'center',
    justifyContent: 'center', height: '100vh', gap: '1rem',
  },
  spinner: {
    width: 40, height: 40, border: '4px solid #e0e0e0',
    borderTop: '4px solid #635bff', borderRadius: '50%',
    animation: 'spin 0.8s linear infinite',
  },
  loginContainer: {
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    height: '100vh', background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  },
  loginCard: {
    background: '#fff', borderRadius: 12, padding: '3rem',
    textAlign: 'center', boxShadow: '0 20px 60px rgba(0,0,0,0.15)',
    maxWidth: 400, width: '90%',
  },
  loginTitle: { fontSize: '2rem', marginBottom: '0.5rem', color: '#1a1a2e' },
  loginSubtitle: { color: '#666', marginBottom: '2rem', lineHeight: 1.5 },
  loginButton: {
    background: '#635bff', color: '#fff', border: 'none', borderRadius: 8,
    padding: '0.875rem 2rem', fontSize: '1rem', fontWeight: 600,
    cursor: 'pointer', transition: 'background 0.2s',
  },
};

export default App;
