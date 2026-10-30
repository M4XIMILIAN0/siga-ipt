import { useEffect, useState } from 'react';
import {
  Typography, Paper, Table, TableHead, TableBody, TableRow, TableCell, TableContainer,
  Alert, Button, Box, TextField, MenuItem, Chip, Stack,
} from '@mui/material';
import registrationService from '../services/registrationService';
import enrollmentService from '../services/enrollmentService';
import studentService from '../services/studentService';

// Admin panel shown under the sections of a period: registrations ("matrícula") and enrollments
function PeriodEnrollmentsPanel({ periodId, periodOpen }) {
  const [registrations, setRegistrations] = useState([]);
  const [enrollments, setEnrollments] = useState([]);
  const [students, setStudents] = useState([]);
  const [studentId, setStudentId] = useState('');
  const [error, setError] = useState('');

  const loadData = async () => {
    try {
      const registrationsResponse = await registrationService.getByPeriod(periodId);
      setRegistrations(registrationsResponse.data);
      const enrollmentsResponse = await enrollmentService.getByPeriod(periodId);
      setEnrollments(enrollmentsResponse.data);
      const studentsResponse = await studentService.getAll();
      setStudents(studentsResponse.data);
    } catch (err) {
      setError(err.response?.data?.message || 'No se pudieron cargar matrículas e inscripciones');
    }
  };

  // Reload when the selected period changes
  useEffect(() => {
    loadData();
  }, [periodId]);

  // RN-6.1: the admin registers a student in the period
  const handleRegister = async () => {
    try {
      setError('');
      await registrationService.register(periodId, studentId === '' ? null : studentId);
      setStudentId('');
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Error inesperado');
    }
  };

  const registeredIds = registrations.map((registration) => registration.studentId);
  const notRegistered = students.filter((student) => !registeredIds.includes(student.id));

  return (
    <Box sx={{ mt: 4 }}>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {/* ---------- Registrations ---------- */}
      <Typography variant="h6">Matrícula del período</Typography>
      <Stack direction="row" spacing={1} sx={{ my: 2, flexWrap: 'wrap' }}>
        {registrations.length === 0 && <Typography color="text.secondary">Nadie matriculado aún.</Typography>}
        {registrations.map((registration) => (
          <Chip key={registration.id} label={`${registration.studentRun} - ${registration.studentName}`} />
        ))}
      </Stack>
      <Box sx={{ display: 'flex', gap: 2, mb: 4 }}>
        <TextField select label="Estudiante" size="small" value={studentId} sx={{ minWidth: 320 }}
                   onChange={(e) => setStudentId(e.target.value)} disabled={!periodOpen}>
          {notRegistered.map((student) => (
            <MenuItem key={student.id} value={student.id}>{student.run} - {student.fullName}</MenuItem>
          ))}
        </TextField>
        <Button variant="contained" onClick={handleRegister} disabled={!periodOpen}>Matricular</Button>
      </Box>

      {/* ---------- Enrollments ---------- */}
      <Typography variant="h6" sx={{ mb: 1 }}>Inscripciones del período</Typography>
      {enrollments.length === 0 ? (
        <Typography color="text.secondary">No hay inscripciones.</Typography>
      ) : (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>RUN</TableCell>
                <TableCell>Estudiante</TableCell>
                <TableCell>Asignatura</TableCell>
                <TableCell>Horario</TableCell>
                <TableCell>Fecha</TableCell>
                <TableCell>Nota</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {enrollments.map((enrollment) => (
                <TableRow key={enrollment.id}>
                  <TableCell>{enrollment.studentRun}</TableCell>
                  <TableCell>{enrollment.studentName}</TableCell>
                  <TableCell>{enrollment.subjectCode} - {enrollment.subjectName}</TableCell>
                  <TableCell>{enrollment.blocks.join(', ')}</TableCell>
                  <TableCell>{enrollment.enrollmentDate}</TableCell>
                  <TableCell>{enrollment.finalGrade !== null ? enrollment.finalGrade : '—'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );
}

export default PeriodEnrollmentsPanel;