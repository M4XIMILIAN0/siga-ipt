import { AppBar, Toolbar, Typography, Button, Container, Box } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

function Layout({ children }) {
  const { username, hasRole, logout } = useAuth();

  return (
    <Box>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>SIGA IPT</Typography>
          <Button color="inherit" component={RouterLink} to="/">Inicio</Button>

          {/* Menu options depend on the role */}
          {hasRole('ADMIN') && (
            <>
              <Button color="inherit" component={RouterLink} to="/careers">Carreras</Button>
              <Button color="inherit" component={RouterLink} to="/students">Estudiantes</Button>
              <Button color="inherit" component={RouterLink} to="/teachers">Docentes</Button>
              <Button color="inherit" component={RouterLink} to="/periods">Oferta</Button>
            </>
          )}
          {hasRole('STUDENT') && (
          <Button color="inherit" component={RouterLink} to="/my-enrollments">Mis inscripciones</Button>
          )}

          <Typography sx={{ mx: 2 }}>{username}</Typography>
          <Button color="inherit" onClick={logout}>Salir</Button>
        </Toolbar>
      </AppBar>
      <Container sx={{ mt: 4 }}>{children}</Container>
    </Box>
  );
}

export default Layout;