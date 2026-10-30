import { useEffect, useState } from 'react';
import {
  Typography, Paper, Table, TableHead, TableBody, TableRow, TableCell, TableContainer,
  Chip, Alert, Button, Stack, Checkbox,
} from '@mui/material';
import periodService from '../services/periodService';
import enrollmentService from '../services/enrollmentService';

function MyEnrollmentsPage() {
  const [periods, setPeriods] = useState([]);
  const [selectedPeriodId, setSelectedPeriodId] = useState(null);
  const [offer, setOffer] = useState([]);
  const [myEnrollments, setMyEnrollments] = useState([]);
  const [selectedSectionIds, setSelectedSectionIds] = useState([]);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Loads the periods and selects the OPEN one by default
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

  const loadPeriodData = async (periodId) => {
    try {
      const offerResponse = await enrollmentService.getMyOffer(periodId);
      setOffer(offerResponse.data);
      const enrollmentsResponse = await enrollmentService.getMyEnrollments(periodId);
      setMyEnrollments(enrollmentsResponse.data);
    } catch (err) {
      setError(err.response?.data?.message || 'No se pudo cargar la oferta');
    }
  };

  useEffect(() => {
    loadPeriods();
  }, []);

  useEffect(() => {
    setSelectedSectionIds([]);
    if (selectedPeriodId !== null) {
      loadPeriodData(selectedPeriodId);
    }
  }, [selectedPeriodId]);

  const selectedPeriod = periods.find((period) => period.id === selectedPeriodId);
  const periodOpen = selectedPeriod !== undefined && selectedPeriod.status === 'OPEN';
  const enrolledCodes = myEnrollments.map((enrollment) => enrollment.subjectCode);

  // Why a subject cannot be selected. It is only a HINT: the backend checks every RN-6.x again
  const getBlockReason = (item) => {
    if (enrolledCodes.includes(item.subjectCode)) return 'Inscrita';                          // RN-6.6
    if (item.passed) return 'Aprobada';                                                       // RN-6.4
    if (!item.prerequisitesPassed) return `Requiere ${item.prerequisiteCodes.join(', ')}`;    // RN-6.5
    if (item.availableSeats <= 0) return 'Sin cupos';                                         // RN-6.9
    return '';
  };

  const toggleSection = (sectionId) => {
    if (selectedSectionIds.includes(sectionId)) {
      setSelectedSectionIds(selectedSectionIds.filter((id) => id !== sectionId));
    } else {
      setSelectedSectionIds([...selectedSectionIds, sectionId]);
    }
  };

  // Sends ALL the selected sections in one request (RN-6.7 and RN-6.10 check the whole set)
  const handleEnroll = async () => {
    try {
      setError('');
      setSuccess('');
      const response = await enrollmentService.enroll(selectedPeriodId, selectedSectionIds);
      setSuccess(`Se inscribieron ${response.data.length} asignatura(s)`);
      setSelectedSectionIds([]);
      loadPeriodData(selectedPeriodId);
    } catch (err) {
      setError(err.response?.data?.message || 'Error inesperado');
    }
  };

  const handleCancel = async (enrollment) => {
    if (!window.confirm(`¿Anular la inscripción de ${enrollment.subjectCode}?`)) {
      return;
    }
    try {
      setError('');
      setSuccess('');
      await enrollmentService.cancel(enrollment.id);
      setSuccess(`Se anuló ${enrollment.subjectCode}`);
      loadPeriodData(selectedPeriodId);
    } catch (err) {
      setError(err.response?.data?.message || 'Error inesperado');
    }
  };

  return (
    <>
      <Typography variant="h4" gutterBottom>Mis inscripciones</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}

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

      {/* ---------- Offer ---------- */}
      <Typography variant="h6" sx={{ mb: 1 }}>Oferta de mi plan</Typography>
      {offer.length === 0 ? (
        <Typography color="text.secondary" sx={{ mb: 3 }}>No hay secciones de tu plan en este período.</Typography>
      ) : (
        <TableContainer component={Paper} sx={{ mb: 2 }}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell />
                <TableCell>Sem</TableCell>
                <TableCell>Código</TableCell>
                <TableCell>Asignatura</TableCell>
                <TableCell>SCT</TableCell>
                <TableCell>Docente</TableCell>
                <TableCell>Horario</TableCell>
                <TableCell>Cupos</TableCell>
                <TableCell>Estado</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {offer.map((item) => {
                const reason = getBlockReason(item);
                return (
                  <TableRow key={item.sectionId}>
                    <TableCell>
                      <Checkbox
                        checked={selectedSectionIds.includes(item.sectionId)}
                        disabled={reason !== '' || !periodOpen}
                        onChange={() => toggleSection(item.sectionId)}
                      />
                    </TableCell>
                    <TableCell>{item.semester}</TableCell>
                    <TableCell>{item.subjectCode}</TableCell>
                    <TableCell>{item.subjectName}</TableCell>
                    <TableCell>{item.sctCredits}</TableCell>
                    <TableCell>{item.teacherName}</TableCell>
                    <TableCell>{item.blocks.join(', ')}</TableCell>
                    <TableCell>{item.availableSeats}</TableCell>
                    <TableCell>{reason === '' ? <Chip label="Disponible" color="success" size="small" /> : reason}</TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      )}
      <Button variant="contained" onClick={handleEnroll}
              disabled={selectedSectionIds.length === 0 || !periodOpen} sx={{ mb: 4 }}>
        Inscribir seleccionadas ({selectedSectionIds.length})
      </Button>

      {/* ---------- My enrollments ---------- */}
      <Typography variant="h6" sx={{ mb: 1 }}>Mis asignaturas inscritas</Typography>
      {myEnrollments.length === 0 ? (
        <Typography color="text.secondary">No tienes inscripciones en este período.</Typography>
      ) : (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Código</TableCell>
                <TableCell>Asignatura</TableCell>
                <TableCell>Docente</TableCell>
                <TableCell>Horario</TableCell>
                <TableCell>Fecha</TableCell>
                <TableCell>Nota</TableCell>
                <TableCell>Acciones</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {myEnrollments.map((enrollment) => (
                <TableRow key={enrollment.id}>
                  <TableCell>{enrollment.subjectCode}</TableCell>
                  <TableCell>{enrollment.subjectName}</TableCell>
                  <TableCell>{enrollment.teacherName}</TableCell>
                  <TableCell>{enrollment.blocks.join(', ')}</TableCell>
                  <TableCell>{enrollment.enrollmentDate}</TableCell>
                  <TableCell>{enrollment.finalGrade !== null ? enrollment.finalGrade : '—'}</TableCell>
                  <TableCell>
                    <Button size="small" color="error" disabled={!periodOpen} onClick={() => handleCancel(enrollment)}>
                      Anular
                    </Button>
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

export default MyEnrollmentsPage;