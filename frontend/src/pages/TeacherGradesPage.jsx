import { useEffect, useState } from 'react';
import {
  Typography, Paper, Table, TableHead, TableBody, TableRow, TableCell, TableContainer,
  Chip, Alert, Button, Stack, TextField,
} from '@mui/material';
import periodService from '../services/periodService';
import gradeService from '../services/gradeService';

// Text and color of the result calculated by the backend (RN-7.5)
const RESULT_LABELS = { PASSED: 'Aprobado', FAILED: 'Reprobado' };
const RESULT_COLORS = { PASSED: 'success', FAILED: 'error' };

function TeacherGradesPage() {
  const [periods, setPeriods] = useState([]);
  const [selectedPeriodId, setSelectedPeriodId] = useState(null);
  const [sections, setSections] = useState([]);
  const [selectedSectionId, setSelectedSectionId] = useState(null);
  const [grades, setGrades] = useState([]);
  const [gradeInputs, setGradeInputs] = useState({});   // { enrollmentId: "5.5" }
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadPeriods = async () => {
    try {
      const response = await periodService.getAll();
      setPeriods(response.data);
      const openPeriod = response.data.find((period) => period.status === 'OPEN');
      if (openPeriod) {
        setSelectedPeriodId(openPeriod.id);
      }
    } catch (err) {
      setError('No se pudieron cargar los períodos');
    }
  };

  const loadSections = async (periodId) => {
    try {
      const response = await gradeService.getMySections(periodId);
      setSections(response.data);
    } catch (err) {
      setError(err.response?.data?.message || 'No se pudieron cargar tus secciones');
    }
  };

  // Loads the students of the section and fills one input per student with his current grade
  const loadGrades = async (sectionId) => {
    try {
      const response = await gradeService.getSectionGrades(sectionId);
      setGrades(response.data);
      const inputs = {};
      response.data.forEach((grade) => {
        inputs[grade.enrollmentId] = grade.finalGrade !== null ? String(grade.finalGrade) : '';
      });
      setGradeInputs(inputs);
    } catch (err) {
      setError(err.response?.data?.message || 'No se pudieron cargar las notas');
    }
  };

  useEffect(() => {
    loadPeriods();
  }, []);

  useEffect(() => {
    setSelectedSectionId(null);
    setGrades([]);
    if (selectedPeriodId !== null) {
      loadSections(selectedPeriodId);
    }
  }, [selectedPeriodId]);

  useEffect(() => {
    if (selectedSectionId !== null) {
      loadGrades(selectedSectionId);
    }
  }, [selectedSectionId]);

  const selectedPeriod = periods.find((period) => period.id === selectedPeriodId);
  const periodOpen = selectedPeriod !== undefined && selectedPeriod.status === 'OPEN';

  const handleInputChange = (enrollmentId, value) => {
    setGradeInputs({ ...gradeInputs, [enrollmentId]: value });
  };

  // RN-7.1..7.4 are validated by the backend (403 / 409 / 400); here we only show its message
  const handleSave = async (grade) => {
    const value = gradeInputs[grade.enrollmentId];
    try {
      setError('');
      setSuccess('');
      await gradeService.registerGrade(grade.enrollmentId, value === '' ? null : Number(value));
      setSuccess(`Nota de ${grade.studentName} guardada`);
      loadGrades(selectedSectionId);
    } catch (err) {
      setError(err.response?.data?.message || 'Error inesperado');
    }
  };

  return (
    <>
      <Typography variant="h4" gutterBottom>Mis secciones</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}

      {/* ---------- Periods ---------- */}
      <Stack direction="row" spacing={1} sx={{ mb: 3, flexWrap: 'wrap' }}>
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

      {/* ---------- My sections in the period ---------- */}
      {sections.length === 0 ? (
        <Typography color="text.secondary">No tienes secciones en este período.</Typography>
      ) : (
        <Stack direction="row" spacing={1} sx={{ mb: 3, flexWrap: 'wrap' }}>
          {sections.map((section) => (
            <Chip
              key={section.id}
              label={`${section.subjectCode} - ${section.subjectName} (${section.blocks.join(', ')})`}
              color="primary"
              variant={section.id === selectedSectionId ? 'filled' : 'outlined'}
              onClick={() => setSelectedSectionId(section.id)}
            />
          ))}
        </Stack>
      )}

      {/* ---------- Students and grades ---------- */}
      {selectedSectionId !== null && (
        grades.length === 0 ? (
          <Typography color="text.secondary">No hay estudiantes inscritos en esta sección.</Typography>
        ) : (
          <TableContainer component={Paper}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>RUN</TableCell>
                  <TableCell>Estudiante</TableCell>
                  <TableCell>Nota final (1.0 a 7.0)</TableCell>
                  <TableCell>Resultado</TableCell>
                  <TableCell>Acciones</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {grades.map((grade) => (
                  <TableRow key={grade.enrollmentId}>
                    <TableCell>{grade.studentRun}</TableCell>
                    <TableCell>{grade.studentName}</TableCell>
                    <TableCell>
                      <TextField
                        size="small"
                        type="number"
                        value={gradeInputs[grade.enrollmentId] ?? ''}
                        onChange={(e) => handleInputChange(grade.enrollmentId, e.target.value)}
                        disabled={!periodOpen}
                        slotProps={{ htmlInput: { step: 0.1, min: 1, max: 7 } }}
                        sx={{ width: 110 }}
                      />
                    </TableCell>
                    <TableCell>
                      {grade.result === null
                        ? <Chip label="Pendiente" size="small" />
                        : <Chip label={RESULT_LABELS[grade.result]} color={RESULT_COLORS[grade.result]} size="small" />}
                    </TableCell>
                    <TableCell>
                      <Button size="small" variant="contained" disabled={!periodOpen} onClick={() => handleSave(grade)}>
                        Guardar
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )
      )}
    </>
  );
}

export default TeacherGradesPage;