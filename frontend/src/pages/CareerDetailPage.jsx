import { useEffect, useState } from 'react';
import { useParams, Link as RouterLink } from 'react-router-dom';
import {
  Typography, Paper, Table, TableHead, TableBody, TableRow, TableCell, TableContainer,
  Chip, Alert, Button, Box, TextField, Stack,
} from '@mui/material';
import careerService from '../services/careerService';
import studyPlanService from '../services/studyPlanService';
import subjectService from '../services/subjectService';

function CareerDetailPage() {
  const { careerId } = useParams();                        // "1" from /careers/1

  const [career, setCareer] = useState(null);
  const [plans, setPlans] = useState([]);
  const [selectedPlanId, setSelectedPlanId] = useState(null);
  const [subjects, setSubjects] = useState([]);
  const [newIdentifier, setNewIdentifier] = useState('');
  const [error, setError] = useState('');

  // Loads the career and its plans, and selects the CURRENT plan
  const loadCareerAndPlans = async () => {
    try {
      const careerResponse = await careerService.getById(careerId);
      setCareer(careerResponse.data);

      const plansResponse = await studyPlanService.getByCareer(careerId);
      setPlans(plansResponse.data);

      const currentPlan = plansResponse.data.find((plan) => plan.status === 'CURRENT');
      setSelectedPlanId(currentPlan ? currentPlan.id : null);
    } catch (err) {
      setError(err.response?.data?.message || 'No se pudo cargar la carrera');
    }
  };

  const loadSubjects = async (planId) => {
    try {
      const response = await subjectService.getByPlan(planId);
      setSubjects(response.data);
    } catch (err) {
      setError(err.response?.data?.message || 'No se pudieron cargar las asignaturas');
    }
  };

  // Runs when the page opens (and if the careerId in the URL changes)
  useEffect(() => {
    loadCareerAndPlans();
  }, [careerId]);

  // Runs every time the selected plan changes
  useEffect(() => {
    if (selectedPlanId === null) {
      setSubjects([]);
    } else {
      loadSubjects(selectedPlanId);
    }
  }, [selectedPlanId]);

  // RN-2.10: the new plan becomes CURRENT and the previous one NOT_CURRENT
  const handleCreatePlan = async () => {
    try {
      setError('');
      await studyPlanService.create(careerId, { identifier: newIdentifier });
      setNewIdentifier('');
      loadCareerAndPlans();
    } catch (err) {
      setError(err.response?.data?.message || 'Error inesperado');
    }
  };

  // RN-2.8: the backend rejects deleting a plan with students (or with subjects)
  const handleDeletePlan = async () => {
    if (!window.confirm('¿Eliminar el plan seleccionado?')) {
      return;
    }
    try {
      setError('');
      await studyPlanService.remove(selectedPlanId);
      loadCareerAndPlans();
    } catch (err) {
      setError(err.response?.data?.message || 'Error inesperado');
    }
  };

  if (!career) {
    return error ? <Alert severity="error">{error}</Alert> : <Typography>Cargando...</Typography>;
  }

  // Copy and sort by semester (never sort the state array directly)
  const sortedSubjects = [...subjects].sort((a, b) => a.semester - b.semester);

  return (
    <>
      <Button component={RouterLink} to="/careers" sx={{ mb: 2 }}>← Volver a carreras</Button>
      <Typography variant="h4">{career.code} - {career.name}</Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>{career.description}</Typography>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {/* ---------- Study plans ---------- */}
      <Typography variant="h6">Planes de estudio</Typography>
      <Stack direction="row" spacing={1} sx={{ my: 2 }}>
        {plans.map((plan) => (
          <Chip
            key={plan.id}
            label={`${plan.identifier} (${plan.status === 'CURRENT' ? 'Vigente' : 'No vigente'})`}
            color={plan.status === 'CURRENT' ? 'success' : 'default'}
            variant={plan.id === selectedPlanId ? 'filled' : 'outlined'}
            onClick={() => setSelectedPlanId(plan.id)}
          />
        ))}
      </Stack>

      <Box sx={{ display: 'flex', gap: 2, mb: 4 }}>
        <TextField
          label="Identificador del nuevo plan"
          size="small"
          value={newIdentifier}
          onChange={(e) => setNewIdentifier(e.target.value)}
        />
        <Button variant="contained" onClick={handleCreatePlan}>Crear plan</Button>
        <Button color="error" onClick={handleDeletePlan} disabled={selectedPlanId === null}>
          Eliminar plan seleccionado
        </Button>
      </Box>

      {/* ---------- Subjects of the selected plan ---------- */}
      <Typography variant="h6" sx={{ mb: 2 }}>Asignaturas</Typography>
      {sortedSubjects.length === 0 ? (
        <Typography color="text.secondary">Este plan no tiene asignaturas.</Typography>
      ) : (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Sem</TableCell>
                <TableCell>Código</TableCell>
                <TableCell>Asignatura</TableCell>
                <TableCell>TEL (T-E-L)</TableCell>
                <TableCell>SCT</TableCell>
                <TableCell>Prerrequisitos</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {sortedSubjects.map((subject) => (
                <TableRow key={subject.id}>
                  <TableCell>{subject.semester}</TableCell>
                  <TableCell>{subject.code}</TableCell>
                  <TableCell>{subject.name}</TableCell>
                  <TableCell>
                    {subject.theoryHours}-{subject.exerciseHours}-{subject.labHours} ({subject.telTotal} h)
                  </TableCell>
                  <TableCell>{subject.sctCredits}</TableCell>
                  <TableCell>
                    {subject.prerequisiteCodes.length > 0 ? subject.prerequisiteCodes.join(', ') : '—'}
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

export default CareerDetailPage;