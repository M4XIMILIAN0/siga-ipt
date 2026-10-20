import { useEffect, useState } from 'react';
import {
  Typography, Paper, Table, TableHead, TableBody, TableRow, TableCell, TableContainer,
  Chip, Alert, Button, Box, Dialog, DialogTitle, DialogContent, DialogActions, TextField,
} from '@mui/material';
import careerService from '../services/careerService';
import { Link as RouterLink } from 'react-router-dom';

const EMPTY_FORM = { code: '', name: '', description: '' };

function CareersPage() {
  const [careers, setCareers] = useState([]);
  const [error, setError] = useState('');             // page-level error (status, delete)

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);   // null = creating, number = editing
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState('');     // error shown inside the dialog

  const loadCareers = async () => {
    try {
      const response = await careerService.getAll();
      setCareers(response.data);
    } catch (err) {
      setError('No se pudieron cargar las carreras');
    }
  };

  useEffect(() => {
    loadCareers();
  }, []);

  // ---------- dialog ----------

  const openCreateDialog = () => {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFormError('');
    setDialogOpen(true);
  };

  const openEditDialog = (career) => {
    setEditingId(career.id);
    setForm({ code: career.code, name: career.name, description: career.description });
    setFormError('');
    setDialogOpen(true);
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSave = async () => {
    try {
      if (editingId === null) {
        await careerService.create(form);             // POST -> 201
      } else {
        await careerService.update(editingId, form);  // PUT  -> 200
      }
      setDialogOpen(false);
      loadCareers();
    } catch (err) {
      // Shows the backend message, e.g. "Career code 10450 already exists" (RN-2.1)
      setFormError(err.response?.data?.message || 'Error inesperado');
    }
  };

  // ---------- table actions ----------

  // RN-2.3: switch between ACTIVE and INACTIVE
  const handleToggleStatus = async (career) => {
    const newStatus = career.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      setError('');
      await careerService.changeStatus(career.id, newStatus);
      loadCareers();
    } catch (err) {
      setError(err.response?.data?.message || 'Error inesperado');
    }
  };

  // RN-2.9: the backend rejects deleting a career with study plans
  const handleDelete = async (career) => {
    if (!window.confirm(`¿Eliminar la carrera ${career.code}?`)) {
      return;
    }
    try {
      setError('');
      await careerService.remove(career.id);
      loadCareers();
    } catch (err) {
      setError(err.response?.data?.message || 'Error inesperado');
    }
  };

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4">Carreras</Typography>
        <Button variant="contained" onClick={openCreateDialog}>Nueva carrera</Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Código</TableCell>
              <TableCell>Nombre</TableCell>
              <TableCell>Descripción</TableCell>
              <TableCell>Estado</TableCell>
              <TableCell>Acciones</TableCell>
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
                <TableCell>
                  <Button size="small" component={RouterLink} to={`/careers/${career.id}`}>Ver</Button>
                  <Button size="small" onClick={() => openEditDialog(career)}>Editar</Button>
                  <Button size="small" onClick={() => handleToggleStatus(career)}>
                    {career.status === 'ACTIVE' ? 'Desactivar' : 'Activar'}
                  </Button>
                  <Button size="small" color="error" onClick={() => handleDelete(career)}>Eliminar</Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Same dialog for create and edit */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth>
        <DialogTitle>{editingId === null ? 'Nueva carrera' : 'Editar carrera'}</DialogTitle>
        <DialogContent>
          {formError && <Alert severity="error" sx={{ mb: 2 }}>{formError}</Alert>}
          <TextField label="Código" name="code" value={form.code}
                     onChange={handleChange} fullWidth margin="normal" />
          <TextField label="Nombre" name="name" value={form.name}
                     onChange={handleChange} fullWidth margin="normal" />
          <TextField label="Descripción" name="description" value={form.description}
                     onChange={handleChange} fullWidth margin="normal" multiline rows={3} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancelar</Button>
          <Button variant="contained" onClick={handleSave}>Guardar</Button>
        </DialogActions>
      </Dialog>
    </>
  );
}

export default CareersPage;