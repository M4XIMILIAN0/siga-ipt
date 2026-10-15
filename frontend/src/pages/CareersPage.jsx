import { useEffect, useState } from 'react';
import {
  Typography, Paper, Table, TableHead, TableBody, TableRow, TableCell,
  TableContainer, Chip, Alert,
} from '@mui/material';
import careerService from '../services/careerService';

function CareersPage() {
  const [careers, setCareers] = useState([]);   // list shown in the table
  const [error, setError] = useState('');       // error message, if any

  // Calls GET /api/careers and saves the result in the state
  const loadCareers = async () => {
    try {
      const response = await careerService.getAll();
      setCareers(response.data);
    } catch (err) {
      setError('No se pudieron cargar las carreras');
    }
  };

  // Runs ONCE, when the page is shown for the first time
  useEffect(() => {
    loadCareers();
  }, []);

  return (
    <>
      <Typography variant="h4" gutterBottom>Carreras</Typography>
      {error && <Alert severity="error">{error}</Alert>}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Código</TableCell>
              <TableCell>Nombre</TableCell>
              <TableCell>Descripción</TableCell>
              <TableCell>Estado</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {careers.map((career) => (
              <TableRow key={career.id}>
                <TableCell>{career.code}</TableCell>
                <TableCell>{career.name}</TableCell>
                <TableCell>{career.description}</TableCell>
                <TableCell>
                  <Chip
                    label={career.status === 'ACTIVE' ? 'Activa' : 'Inactiva'}
                    color={career.status === 'ACTIVE' ? 'success' : 'default'}
                  />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
}

export default CareersPage;