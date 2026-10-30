import httpClient from '../api/httpClient';

// STUDENT: his own academic record
const getMyRecord = () => httpClient.get('/me/record');

export default { getMyRecord };