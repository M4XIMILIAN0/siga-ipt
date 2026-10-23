import Keycloak from 'keycloak-js';

// Connection data comes from .env.development
const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL,
  realm: import.meta.env.VITE_KEYCLOAK_REALM,
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
});

let initPromise = null;

// React StrictMode runs effects TWICE in development,
// but Keycloak can only be initialized ONCE, so we reuse the same promise.
export const initKeycloak = () => {
  if (initPromise === null) {
    initPromise = keycloak.init({
      onLoad: 'login-required',   // not logged in? -> go to the Keycloak login page
      pkceMethod: 'S256',         // extra protection for public clients
      checkLoginIframe: false,    // avoids an iframe check that fails on localhost
    });
  }
  return initPromise;
};

export default keycloak;