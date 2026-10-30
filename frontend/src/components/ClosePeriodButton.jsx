import { useState } from 'react';
import { Typography, Alert, Button, Box } from '@mui/material';
import periodService from '../services/periodService';

// RN-7.6..7.11: the admin closes an OPEN period
function ClosePeriodButton({ period, onClosed }) {
  const [error, setError] = useState('');
  const [summary, setSummary] = useState('');

  const handleClose = async () => {
    if (!window.confirm(`¿Cerrar el período ${period.code}? Después no se podrán cambiar notas ni inscripciones.`)) {
      return;
    }
    try {
      setError('');
      const response = await periodService.close(period.id);
      const result = response.data;
      setSummary(`Período ${result.periodCode} cerrado: ${result.recordsCreated} registro(s) al historial, `
        + `${result.graduatedStudents} egresado(s), ${result.eliminatedStudents} eliminado(s).`);
      onClosed();   // tells the parent page to reload the periods
    } catch (err) {
      // RN-7.7: e.g. "Cannot close the period. Sections with pending grades: TAP102"
      setError(err.response?.data?.message || 'Error inesperado');
    }
  };

  return (
    <Box sx={{ mt: 4 }}>
      <Typography variant="h6" sx={{ mb: 1 }}>Cierre del período</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {summary && <Alert severity="success" sx={{ mb: 2 }}>{summary}</Alert>}
      <Button variant="contained" color="error" onClick={handleClose} disabled={period.status !== 'OPEN'}>
        Cerrar período {period.code}
      </Button>
    </Box>
  );
}

export default ClosePeriodButton;