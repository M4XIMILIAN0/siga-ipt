import { useEffect, useState } from 'react';
import {
  Typography, Paper, Table, TableHead, TableBody, TableRow, TableCell, TableContainer,
  Chip, Alert, Button, Box, Stack, TextField, MenuItem,
  Dialog, DialogTitle, DialogContent, DialogActions,
} from '@mui/material';
import periodService from '../services/periodService';
import sectionService from '../services/sectionService';
import careerService from '../services/careerService';
import studyPlanService from '../services/studyPlanService';
import subjectService from '../services/subjectService';
import teacherService from '../services/teacherService';
import PeriodEnrollmentsPanel from '../components/PeriodEnrollmentsPanel';
import ClosePeriodButton from '../components/ClosePeriodButton';

const EMPTY_SECTION_FORM = { careerId: '', subjectId: '', teacherId: '', capacity: '', blocksText: '' };

// "L2, W3, V1" -> [{day: "L", moduleNumber: 2}, {day: "W", moduleNumber: 3}, {day: "V", moduleNumber: 1}]
const parseBlocks = (text) =>
  text
    .split(',')
    .map((part) => part.trim().toUpperCase())
    .filter((part) => part !== '')
    .map((part) => ({ day: part.substring(0, 1), moduleNumber: Number(part.substring(1)) }));

function PeriodsPage() {
  const [periods, setPeriods] = useState([]);
  const [selectedPeriodId, setSelectedPeriodId] = useState(null);
  const [sections, setSections] = useState([]);
  const [newPeriod, setNewPeriod] = useState({ year: '', semester: 1 });
  const [error, setError] = useState('');

  // Data for the "new section" dialog
  const [dialogOpen, setDialogOpen] = useState(false);
  const [careers, setCareers] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [form, setForm] = useState(EMPTY_SECTION_FORM);
  const [formError, setFormError] = useState('');

  const loadPeriods = async () => {
    try {
      const response = await periodService.getAll();
      setPeriods(response.data);
    } catch (err) {
      setError('No se pudieron cargar los períodos');
    }
  };

  const loadSections = async (periodId) => {
    try {
      const response = await sectionService.getByPeriod(periodId);
      setSections(response.data);
    } catch (err) {
      setError(err.response?.data?.message || 'No se pudieron cargar las secciones');
    }
  };

  useEffect(() => {
    loadPeriods();
  }, []);

  // Reload the offer every time another period is selected
  useEffect(() => {
    if (selectedPeriodId === null) {
      setSections([]);
    } else {
      loadSections(selectedPeriodId);
    }
  }, [selectedPeriodId]);

  const selectedPeriod = periods.find((period) => period.id === selectedPeriodId);

  // RN-5.1 / RN-5.2: new period, always OPEN
  const handleCreatePeriod = async () => {
    try {
      setError('');
      await periodService.create({ year: Number(newPeriod.year), semester: newPeriod.semester });
      setNewPeriod({ year: '', semester: 1 });
      loadPeriods();
    } catch (err) {
      setError(err.response?.data?.message || 'Error inesperado');
    }
  };

  // ---------- new section dialog ----------

  const openSectionDialog = async () => {
    setForm(EMPTY_SECTION_FORM);
    setSubjects([]);
    setFormError('');
    setDialogOpen(true);
    try {
      const careersResponse = await careerService.getAll();
      setCareers(careersResponse.data);
      const teachersResponse = await teacherService.getAll();
      // RN-5.5: only ACTIVE teachers are offered (the backend also checks it)
      setTeachers(teachersResponse.data.filter((teacher) => teacher.status === 'ACTIVE'));
    } catch (err) {
      setFormError('No se pudieron cargar carreras o docentes');
    }
  };

  // When the career changes, load the subjects of its CURRENT plan
  const handleCareerChange = async (e) => {
    const careerId = e.target.value;
    setForm({ ...form, careerId, subjectId: '' });
    try {
      const plansResponse = await studyPlanService.getByCareer(careerId);
      const currentPlan = plansResponse.data.find((plan) => plan.status === 'CURRENT');
      if (!currentPlan) {
        setSubjects([]);
        return;
      }
      const subjectsResponse = await subjectService.getByPlan(currentPlan.id);
      setSubjects([...subjectsResponse.data].sort((a, b) => a.semester - b.semester));
    } catch (err) {
      setFormError('No se pudieron cargar las asignaturas');
    }
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  // RN-5.3 to RN-5.11 are validated by the backend; here we only show its message
  const handleSaveSection = async () => {
    try {
      await sectionService.create(selectedPeriodId, {
        subjectId: form.subjectId === '' ? null : form.subjectId,
        teacherId: form.teacherId === '' ? null : form.teacherId,
        capacity: form.capacity === '' ? null : Number(form.capacity),
        blocks: parseBlocks(form.blocksText),
      });
      setDialogOpen(false);
      loadSections(selectedPeriodId);
    } catch (err) {
      setFormError(err.response?.data?.message || 'Error inesperado');
    }
  };

  // RN-5.12: the backend rejects deleting a section with enrolled students
  const handleDeleteSection = async (section) => {
    if (!window.confirm(`¿Eliminar la sección de ${section.subjectCode}?`)) {
      return;
    }
    try {
      setError('');
      await sectionService.remove(section.id);
      loadSections(selectedPeriodId);
    } catch (err) {
      setError(err.response?.data?.message || 'Error inesperado');
    }
  };

  // Hint for RN-5.9: how many blocks the chosen subject needs
  const chosenSubject = subjects.find((subject) => subject.id === form.subjectId);

  return (
    <>
      <Typography variant="h4" gutterBottom>Oferta académica</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {/* ---------- Periods ---------- */}
      <Typography variant="h6">Períodos</Typography>
      <Stack direction="row" spacing={1} sx={{ my: 2, flexWrap: 'wrap' }}>
        {periods.map((period) => (
          <Chip
            key={period.id}
            label={`${period.code} (${period.status === 'OPEN' ? 'Abierto' : 'Cerrado'})`}
            color={period.status === 'OPEN' ? 'success' : 'default'}
            variant={period.id === selectedPeriodId ? 'filled' : 'outlined'}
            onClick={() => setSelectedPeriodId(period.id)}
          />
        ))}
      </Stack>

      <Box sx={{ display: 'flex', gap: 2, mb: 4 }}>
        <TextField label="Año" size="small" type="number" value={newPeriod.year}
                   onChange={(e) => setNewPeriod({ ...newPeriod, year: e.target.value })} />
        <TextField select label="Semestre" size="small" value={newPeriod.semester} sx={{ width: 120 }}
                   onChange={(e) => setNewPeriod({ ...newPeriod, semester: e.target.value })}>
          <MenuItem value={1}>1</MenuItem>
          <MenuItem value={2}>2</MenuItem>
        </TextField>
        <Button variant="contained" onClick={handleCreatePeriod}>Crear período</Button>
      </Box>

      {/* ---------- Sections of the selected period ---------- */}
      {selectedPeriod && (
        <>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6">Secciones de {selectedPeriod.code}</Typography>
            <Button variant="contained" onClick={openSectionDialog} disabled={selectedPeriod.status !== 'OPEN'}>
              Nueva sección
            </Button>
          </Box>

          {sections.length === 0 ? (
            <Typography color="text.secondary">Este período no tiene secciones.</Typography>
          ) : (
            <TableContainer component={Paper}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Código</TableCell>
                    <TableCell>Asignatura</TableCell>
                    <TableCell>Sem</TableCell>
                    <TableCell>Docente</TableCell>
                    <TableCell>Capacidad</TableCell>
                    <TableCell>Horario</TableCell>
                    <TableCell>Acciones</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {sections.map((section) => (
                    <TableRow key={section.id}>
                      <TableCell>{section.subjectCode}</TableCell>
                      <TableCell>{section.subjectName}</TableCell>
                      <TableCell>{section.semester}</TableCell>
                      <TableCell>{section.teacherName}</TableCell>
                      <TableCell>{section.capacity}</TableCell>
                      <TableCell>{section.blocks.join(', ')}</TableCell>
                      <TableCell>
                        <Button size="small" color="error" onClick={() => handleDeleteSection(section)}>
                          Eliminar
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
          <PeriodEnrollmentsPanel periodId={selectedPeriodId} periodOpen={selectedPeriod.status === 'OPEN'} />
          <ClosePeriodButton period={selectedPeriod} onClosed={loadPeriods} />
        </>
      )}

      {/* ---------- New section dialog ---------- */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth>
        <DialogTitle>Nueva sección</DialogTitle>
        <DialogContent>
          {formError && <Alert severity="error" sx={{ mb: 2 }}>{formError}</Alert>}

          <TextField select label="Carrera" value={form.careerId} onChange={handleCareerChange}
                     fullWidth margin="normal">
            {careers.map((career) => (
              <MenuItem key={career.id} value={career.id}>{career.code} - {career.name}</MenuItem>
            ))}
          </TextField>

          <TextField select label="Asignatura" name="subjectId" value={form.subjectId} onChange={handleChange}
                     fullWidth margin="normal" disabled={subjects.length === 0}>
            {subjects.map((subject) => (
              <MenuItem key={subject.id} value={subject.id}>
                Sem {subject.semester} · {subject.code} - {subject.name}
              </MenuItem>
            ))}
          </TextField>

          <TextField select label="Docente" name="teacherId" value={form.teacherId} onChange={handleChange}
                     fullWidth margin="normal">
            {teachers.map((teacher) => (
              <MenuItem key={teacher.id} value={teacher.id}>{teacher.fullName}</MenuItem>
            ))}
          </TextField>

          <TextField label="Capacidad (1 a 60)" name="capacity" type="number" value={form.capacity}
                     onChange={handleChange} fullWidth margin="normal" />

          <TextField
            label="Bloques horarios" name="blocksText" value={form.blocksText} onChange={handleChange}
            fullWidth margin="normal"
            helperText={
              chosenSubject
                ? `Ej: L2, W3, V1 · ${chosenSubject.code} tiene ${chosenSubject.telTotal} h → necesita ${chosenSubject.telTotal / 2} bloques`
                : 'Ej: L2, W3, V1 (días L, M, W, J, V y módulos 1 a 6)'
            }
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancelar</Button>
          <Button variant="contained" onClick={handleSaveSection}>Guardar</Button>
        </DialogActions>
      </Dialog>
    </>
  );
}

export default PeriodsPage;