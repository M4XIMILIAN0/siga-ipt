import httpClient from '../api/httpClient';

// Mirrors AcademicPeriodController + the closing endpoint of AcademicRecordController
const getAll = () => httpClient.get('/periods');
const create = (period) => httpClient.post('/periods', period);
const close = (id) => httpClient.post(`/periods/${id}/close`);

export default { getAll, create, close };