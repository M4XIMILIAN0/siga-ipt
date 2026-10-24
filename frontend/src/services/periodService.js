import httpClient from '../api/httpClient';

// Mirrors AcademicPeriodController
const getAll = () => httpClient.get('/periods');
const create = (period) => httpClient.post('/periods', period);

export default { getAll, create };