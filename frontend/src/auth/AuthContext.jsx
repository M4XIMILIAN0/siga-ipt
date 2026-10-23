import { createContext, useContext, useEffect, useState } from 'react';
import { Typography } from '@mui/material';
import keycloak, { initKeycloak } from './keycloak';

const AuthContext = createContext(null);

// Wraps the whole app: logs in with Keycloak and shares the user data
export function AuthProvider({ children }) {
  const [ready, setReady] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    initKeycloak()
      .then(() => setReady(true))
      .catch(() => setError('No se pudo conectar con Keycloak'));
  }, []);

  if (error) {
    return <Typography sx={{ m: 4 }}>{error}</Typography>;
  }
  if (!ready) {
    return <Typography sx={{ m: 4 }}>Cargando...</Typography>;
  }

  // Roles come from the token, in realm_access.roles
  const roles = keycloak.tokenParsed?.realm_access?.roles || [];

  const value = {
    username: keycloak.tokenParsed?.preferred_username,
    email: keycloak.tokenParsed?.email,
    roles,
    hasRole: (role) => roles.includes(role),
    logout: () => keycloak.logout({ redirectUri: window.location.origin }),
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// Any component calls useAuth() to know who is logged in
export const useAuth = () => useContext(AuthContext);