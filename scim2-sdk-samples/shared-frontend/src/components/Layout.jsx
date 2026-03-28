import { useKeycloak } from '../auth/KeycloakProvider';
import { NavLink } from 'react-router-dom';

export default function Layout({ children }) {
  const { user, logout } = useKeycloak();

  return (
    <div style={styles.container}>
      <nav style={styles.nav}>
        <div style={styles.navLeft}>
          <span style={styles.brand}>SCIM Sample</span>
          <NavLink to="/users" style={({ isActive }) => ({ ...styles.link, ...(isActive ? styles.activeLink : {}) })}>
            Users
          </NavLink>
          <NavLink to="/groups" style={({ isActive }) => ({ ...styles.link, ...(isActive ? styles.activeLink : {}) })}>
            Groups
          </NavLink>
        </div>
        <div style={styles.navRight}>
          <span style={styles.userEmail}>{user?.email || user?.preferred_username}</span>
          <button style={styles.logoutBtn} onClick={logout}>
            Log Out
          </button>
        </div>
      </nav>
      <main style={styles.main}>{children}</main>
    </div>
  );
}

const styles = {
  container: { minHeight: '100vh', display: 'flex', flexDirection: 'column' },
  nav: {
    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
    padding: '0 1.5rem', height: 56, background: '#1a1a2e',
    boxShadow: '0 2px 8px rgba(0,0,0,0.12)',
  },
  navLeft: { display: 'flex', alignItems: 'center', gap: '1.5rem' },
  navRight: { display: 'flex', alignItems: 'center', gap: '1rem' },
  brand: { color: '#fff', fontWeight: 700, fontSize: '1.1rem', marginRight: '1rem' },
  link: {
    color: '#a0a0c0', textDecoration: 'none', fontSize: '0.9rem',
    fontWeight: 500, padding: '0.25rem 0', borderBottom: '2px solid transparent',
  },
  activeLink: { color: '#fff', borderBottomColor: '#635bff' },
  userEmail: { color: '#a0a0c0', fontSize: '0.85rem' },
  logoutBtn: {
    background: 'transparent', color: '#ff6b6b', border: '1px solid #ff6b6b',
    borderRadius: 6, padding: '0.375rem 0.75rem', fontSize: '0.85rem',
    cursor: 'pointer',
  },
  main: { flex: 1, padding: '2rem', maxWidth: 960, width: '100%', margin: '0 auto' },
};
