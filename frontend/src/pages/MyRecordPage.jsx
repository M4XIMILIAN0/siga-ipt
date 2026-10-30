import { useEffect, useState } from 'react';
import {
  Typography, Paper, Table, TableHead, TableBody, TableRow, TableCell, TableContainer, Chip, Alert,
} from '@mui/material';
import recordService from '../services/recordService';

const RESULT_LABELS = { PASSED: 'Aprobada', FAILED: 'Reprobada' };
const RESULT_COLORS = { PASSED: 'success', FAILED: 'error' };

function MyRecordPage() {
  const [records, setRecords] = useState([]);
  const [error, setError] = useState('');

  const loadRecord = async () => {
    try {
      const response = await recordService.getMyRecord();
      setRecords(response.data);
    } catch (err) {
      setError(err.response?.data?.message || 'No se pudo cargar el historial');
    }
  };

  useEffect(() => {
    loadRecord();
  }, []);

  const passedCount = records.filter((record) => record.result === 'PASSED').length;

  return (
    <>
      <Typography variant="h4" gutterBottom>Mi historial académico</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      <Typography sx={{ mb: 2 }}>Asignaturas aprobadas: {passedCount}</Typography>

      {records.length === 0 ? (
        <Typography color="text.secondary">Aún no tienes asignaturas en tu historial.</Typography>
      ) : (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Período</TableCell>
                <TableCell>Sem</TableCell>
                <TableCell>Código</TableCell>
                <TableCell>Asignatura</TableCell>
                <TableCell>SCT</TableCell>
                <TableCell>Nota</TableCell>
                <TableCell>Resultado</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {records.map((record, index) => (
                <TableRow key={index}>
                  <TableCell>{record.periodCode}</TableCell>
                  <TableCell>{record.semester}</TableCell>
                  <TableCell>{record.subjectCode}</TableCell>
                  <TableCell>{record.subjectName}</TableCell>
                  <TableCell>{record.sctCredits}</TableCell>
                  <TableCell>{record.finalGrade}</TableCell>
                  <TableCell>
                    <Chip label={RESULT_LABELS[record.result]} color={RESULT_COLORS[record.result]} size="small" />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </>
  );
}

export default MyRecordPage;