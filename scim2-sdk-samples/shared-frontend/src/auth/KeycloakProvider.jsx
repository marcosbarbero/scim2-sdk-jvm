import { createContext, useContext, useEffect, useState, useCallback, useRef } from 'react';
import keycloak from './keycloak';

const KeycloakContext = createContext(null);

export function KeycloakProvider({ children }) {
  const [state, setState] = useState({ initialized: false, authenticated: false });
  const initCalled = useRef(false);

  useEffect(() => {
    if (initCalled.current) return;
    initCalled.current = true;

    keycloak.init({ onLoad: 'check-sso', silentCheckSsoRedirectUri: undefined })
      .then((authenticated) => {
        setState({ initialized: true, authenticated });
      })
      .catch((err) => {
        console.error('Keycloak init failed', err);
        setState({ initialized: true, authenticated: false });
      });

    keycloak.onTokenExpired = () => {
      keycloak.updateToken(30).catch(() => keycloak.login());
    };
  }, []);

  const login = useCallback(() => keycloak.login(), []);

  const logout = useCallback(
    () => keycloak.logout({ redirectUri: window.location.origin }),
    []
  );

  const getToken = useCallback(async () => {
    try {
      await keycloak.updateToken(10);
    } catch {
      await keycloak.login();
    }
    return keycloak.token;
  }, []);

  const value = {
    initialized: state.initialized,
    authenticated: state.authenticated,
    user: keycloak.tokenParsed,
    login,
    logout,
    getToken,
    keycloak,
  };

  return (
    <KeycloakContext.Provider value={value}>
      {children}
    </KeycloakContext.Provider>
  );
}

export function useKeycloak() {
  const ctx = useContext(KeycloakContext);
  if (!ctx) throw new Error('useKeycloak must be used within KeycloakProvider');
  return ctx;
}
