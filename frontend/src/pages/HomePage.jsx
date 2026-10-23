import { Typography } from '@mui/material';
import { useAuth } from '../auth/AuthContext';

function HomePage() {
  const { username, email, roles } = useAuth();

  return (
    <>
      <Typography variant="h4" gutterBottom>Bienvenido a SIGA IPT, {username}</Typography>
      <Typography>Correo: {email}</Typography>
      <Typography>Roles: {roles.filter((role) => ['ADMIN', 'TEACHER', 'STUDENT'].includes(role)).join(', ')}</Typography>
    </>
  );
}

export default HomePage;