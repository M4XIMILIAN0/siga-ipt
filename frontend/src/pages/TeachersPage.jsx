import { useEffect, useState } from 'react';
import {
  Typography, Paper, Table, TableHead, TableBody, TableRow, TableCell, TableContainer,
  Chip, Alert, Button, Box, Dialog, DialogTitle, DialogContent, DialogActions, TextField,
} from '@mui/material';
import teacherService from '../services/teacherService';

const EMPTY_FORM = { run: '', fullName: '', email: '', professionalTitle: '', academicDegree: '' };

function TeachersPage() {
  const [teachers, setTeachers] = useState([]);
  const [error, setError] = useState('');

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);   // null = creating
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState('');

  const loadTeachers = async () => {
    try {
      const response = await teacherService.getAll();
      setTeachers(response.data);
    } catch (err) {
      setError('No se pudieron cargar los docentes');
    }
  };

  useEffect(() => {
    loadTeachers();
  }, []);

  // ---------- dialog ----------

  const openCreateDialog = () => {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFormError('');
    setDialogOpen(true);
  };

  const openEditDialog = (teacher) => {
    setEditingId(teacher.id);
    setForm({
      run: teacher.run,
      fullName: teacher.fullName,
      email: teacher.email,
      professionalTitle: teacher.professionalTitle,
      academicDegree: teacher.academicDegree,
    });
    setFormError('');
    setDialogOpen(true);
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSave = async () => {
    try {
      if (editingId === null) {
        await teacherService.create(form);                    // RN-4.1..4.4
      } else {
        await teacherService.update(editingId, {              // RUN is not sent: it cannot change
          fullName: form.fullName,
          email: form.email,
          professionalTitle: form.professionalTitle,
          academicDegree: form.academicDegree,
        });
      }
      setDialogOpen(false);
      loadTeachers();
    } catch (err) {
      setFormError(err.response?.data?.message || 'Error inesperado');
    }
  };

  // ---------- table actions ----------

  // RN-4.7: the backend rejects INACTIVE if the teacher has sections in an OPEN period
  const handleToggleStatus = async (teacher) => {
    const newStatus = teacher.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      setError('');
      await teacherService.changeStatus(teacher.id, newStatus);
      loadTeachers();
    } catch (err) {
      setError(err.response?.data?.message || 'Error inesperado');
    }
  };

  // RN-4.6: the backend rejects deleting a teacher with sections
  const handleDelete = async (teacher) => {
    if (!window.confirm(`¿Eliminar al docente ${teacher.fullName}?`)) {
      return;
    }
    try {
      setError('');
      await teacherService.remove(teacher.id);
      loadTeachers();
    } catch (err) {
      setError(err.response?.data?.message || 'Error inesperado');
    }
  };

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4">Docentes</Typography>
        <Button variant="contained" onClick={openCreateDialog}>Nuevo docente</Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>RUN</TableCell>
              <TableCell>Nombre</TableCell>
              <TableCell>Correo</TableCell>
              <TableCell>Título profesional</TableCell>
              <TableCell>Grado académico</TableCell>
              <TableCell>Estado</TableCell>
              <TableCell>Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {teachers.map((teacher) => (
              <TableRow key={teacher.id}>
                <TableCell>{teacher.run}</TableCell>
                <TableCell>{teacher.fullName}</TableCell>
                <TableCell>{teacher.email}</TableCell>
                <TableCell>{teacher.professionalTitle}</TableCell>
                <TableCell>{teacher.academicDegree}</TableCell>
                <TableCell>
                  <Chip
                    label={teacher.status === 'ACTIVE' ? 'Activo' : 'Inactivo'}
                    color={teacher.status === 'ACTIVE' ? 'success' : 'default'}
                  />
                </TableCell>
                <TableCell>
                  <Button size="small" onClick={() => openEditDialog(teacher)}>Editar</Button>
                  <Button size="small" onClick={() => handleToggleStatus(teacher)}>
                    {teacher.status === 'ACTIVE' ? 'Desactivar' : 'Activar'}
                  </Button>
                  <Button size="small" color="error" onClick={() => handleDelete(teacher)}>Eliminar</Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Same dialog for create and edit */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth>
        <DialogTitle>{editingId === null ? 'Nuevo docente' : 'Editar docente'}</DialogTitle>
        <DialogContent>
          {formError && <Alert severity="error" sx={{ mb: 2 }}>{formError}</Alert>}
          <TextField
            label="RUN" name="run" value={form.run} onChange={handleChange}
            fullWidth margin="normal"
            disabled={editingId !== null}
            helperText={editingId !== null ? 'El RUN no se puede modificar' : ''}
          />
          <TextField label="Nombre completo" name="fullName" value={form.fullName}
                     onChange={handleChange} fullWidth margin="normal" />
          <TextField label="Correo" name="email" value={form.email}
                     onChange={handleChange} fullWidth margin="normal" />
          <TextField label="Título profesional" name="professionalTitle" value={form.professionalTitle}
                     onChange={handleChange} fullWidth margin="normal" />
          <TextField label="Grado académico" name="academicDegree" value={form.academicDegree}
                     onChange={handleChange} fullWidth margin="normal" />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancelar</Button>
          <Button variant="contained" onClick={handleSave}>Guardar</Button>
        </DialogActions>
      </Dialog>
    </>
  );
}

export default TeachersPage;