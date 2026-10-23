import { Alert } from '@mui/material';
import { useAuth } from '../auth/AuthContext';

// Shows the page only if the user has at least one of the allowed roles
function RoleRoute({ roles, children }) {
  const { hasRole } = useAuth();
  const allowed = roles.some((role) => hasRole(role));

  if (!allowed) {
    return <Alert severity="warning">No tienes permiso para ver esta página.</Alert>;
  }
  return children;
}

export default RoleRoute;