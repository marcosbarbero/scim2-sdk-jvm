import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:9090',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'scim-sample',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'scim-sample-app',
});

export default keycloak;
