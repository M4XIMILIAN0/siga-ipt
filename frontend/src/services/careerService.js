import httpClient from '../api/httpClient';

// One function per endpoint of CareerController
const getAll = () => httpClient.get('/careers');
const getById = (id) => httpClient.get(`/careers/${id}`);
const create = (career) => httpClient.post('/careers', career);
const update = (id, career) => httpClient.put(`/careers/${id}`, career);
const changeStatus = (id, status) => httpClient.put(`/careers/${id}/status`, { status });
const remove = (id) => httpClient.delete(`/careers/${id}`);

export default { getAll, getById, create, update, changeStatus, remove };