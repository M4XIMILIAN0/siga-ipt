import { useEffect, useState } from 'react';
import {
  Typography, Paper, Table, TableHead, TableBody, TableRow, TableCell, TableContainer,
  Chip, Alert, Button, Box, Dialog, DialogTitle, DialogContent, DialogActions, TextField, MenuItem,
} from '@mui/material';
import studentService from '../services/studentService';
import careerService from '../services/careerService';

// Text shown to the user for each backend status
const STATUS_LABELS = {
  REGULAR: 'Regular',
  POSTPONED: 'Postergación',
  TEMPORARY_WITHDRAWAL: 'Retiro temporal',
  GRADUATED: 'Egresado',
  ELIMINATED: 'Eliminado',
};

// RN-1.7 / RN-1.9: only these can be assigned manually (the backend also checks it)
const MANUAL_STATUSES = ['REGULAR', 'POSTPONED', 'TEMPORARY_WITHDRAWAL'];

const EMPTY_FORM = { run: '', fullName: '', email: '', careerId: '' };

function StudentsPage() {
  const [students, setStudents] = useState([]);
  const [careers, setCareers] = useState([]);
  const [error, setError] = useState('');

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingStudent, setEditingStudent] = useState(null);   // null = creating
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState('');

  const loadData = async () => {
    try {
      const studentsResponse = await studentService.getAll();
      setStudents(studentsResponse.data);
      const careersResponse = await careerService.getAll();
      setCareers(careersResponse.data);
    } catch (err) {
      setError('No se pudieron cargar los datos');
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // GRADUATED and ELIMINATED cannot be changed manually (RN-1.7)
  const isFinalStatus = (status) => status === 'GRADUATED' || status === 'ELIMINATED';

  // ---------- dialog ----------

  const openCreateDialog = () => {
    setEditingStudent(null);
    setForm(EMPTY_FORM);
    setFormError('');
    setDialogOpen(true);
  };

  const openEditDialog = (student) => {
    setEditingStudent(student);
    setForm({ run: student.run, fullName: student.fullName, email: student.email, careerId: student.careerId });
    setFormError('');
    setDialogOpen(true);
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSave = async () => {
    try {
      if (editingStudent === null) {
        // RN-1.1 / RN-1.5: the backend assigns the current plan and REGULAR status
        await studentService.create({ ...form, careerId: form.careerId === '' ? null : form.careerId });
      } else {
        await studentService.update(editingStudent.id, { fullName: form.fullName, email: form.email });
        // RN-1.8: changing career is a separate endpoint
        if (form.careerId !== editingStudent.careerId) {
          await studentService.changeCareer(editingStudent.id, form.careerId);
        }
      }
      setDialogOpen(false);
      loadData();
    } catch (err) {
      setFormError(err.response?.data?.message || 'Error inesperado');
    }
  };

  // ---------- table actions ----------

  // RN-1.9: admin changes REGULAR / POSTPONED / TEMPORARY_WITHDRAWAL
  const handleStatusChange = async (student, newStatus) => {
    try {
      setError('');
      await studentService.changeStatus(student.id, newStatus);
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Error inesperado');
    }
  };

  // RN-1.6: the backend rejects deleting a student with enrollments or academic record
  const handleDelete = async (student) => {
    if (!window.confirm(`¿Eliminar al estudiante ${student.fullName}?`)) {
      return;
    }
    try {
      setError('');
      await studentService.remove(student.id);
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Error inesperado');
    }
  };

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4">Estudiantes</Typography>
        <Button variant="contained" onClick={openCreateDialog}>Nuevo estudiante</Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>RUN</TableCell>
              <TableCell>Nombre</TableCell>
              <TableCell>Correo</TableCell>
              <TableCell>Carrera</TableCell>
              <TableCell>Plan</TableCell>
              <TableCell>Estado</TableCell>
              <TableCell>Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {students.map((student) => (
              <TableRow key={student.id}>
                <TableCell>{student.run}</TableCell>
                <TableCell>{student.fullName}</TableCell>
                <TableCell>{student.email}</TableCell>
                <TableCell>{student.careerName}</TableCell>
                <TableCell>{student.studyPlanIdentifier}</TableCell>
                <TableCell>
                  {isFinalStatus(student.status) ? (
                    <Chip label={STATUS_LABELS[student.status]} />
                  ) : (
                    <TextField
                      select
                      size="small"
                      value={student.status}
                      onChange={(e) => handleStatusChange(student, e.target.value)}
                    >
                      {MANUAL_STATUSES.map((status) => (
                        <MenuItem key={status} value={status}>{STATUS_LABELS[status]}</MenuItem>
                      ))}
                    </TextField>
                  )}
                </TableCell>
                <TableCell>
                  <Button size="small" onClick={() => openEditDialog(student)}>Editar</Button>
                  <Button size="small" color="error" onClick={() => handleDelete(student)}>Eliminar</Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Same dialog for create and edit */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth>
        <DialogTitle>{editingStudent === null ? 'Nuevo estudiante' : 'Editar estudiante'}</DialogTitle>
        <DialogContent>
          {formError && <Alert severity="error" sx={{ mb: 2 }}>{formError}</Alert>}
          <TextField
            label="RUN" name="run" value={form.run} onChange={handleChange}
            fullWidth margin="normal"
            disabled={editingStudent !== null}
            helperText={editingStudent !== null ? 'El RUN no se puede modificar' : ''}
          />
          <TextField label="Nombre completo" name="fullName" value={form.fullName}
                     onChange={handleChange} fullWidth margin="normal" />
          <TextField label="Correo" name="email" value={form.email}
                     onChange={handleChange} fullWidth margin="normal" />
          <TextField select label="Carrera" name="careerId" value={form.careerId}
                     onChange={handleChange} fullWidth margin="normal">
            {careers.map((career) => (
              // RN-2.5: inactive careers are shown but cannot be chosen
              <MenuItem key={career.id} value={career.id} disabled={career.status !== 'ACTIVE'}>
                {career.code} - {career.name}{career.status !== 'ACTIVE' ? ' (inactiva)' : ''}
              </MenuItem>
            ))}
          </TextField>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancelar</Button>
          <Button variant="contained" onClick={handleSave}>Guardar</Button>
        </DialogActions>
      </Dialog>
    </>
  );
}

export default StudentsPage;