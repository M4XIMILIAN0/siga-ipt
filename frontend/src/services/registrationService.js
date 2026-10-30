import httpClient from '../api/httpClient';

// Mirrors PeriodRegistrationController ("matrícula")
const getByPeriod = (periodId) => httpClient.get(`/periods/${periodId}/registrations`);
const register = (periodId, studentId) => httpClient.post(`/periods/${periodId}/registrations`, { studentId });

export default { getByPeriod, register };