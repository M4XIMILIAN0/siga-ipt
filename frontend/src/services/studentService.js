import httpClient from '../api/httpClient';

// Mirrors StudentController
const getAll = () => httpClient.get('/students');
const getById = (id) => httpClient.get(`/students/${id}`);
const create = (student) => httpClient.post('/students', student);
const update = (id, student) => httpClient.put(`/students/${id}`, student);
const changeStatus = (id, status) => httpClient.put(`/students/${id}/status`, { status });
const changeCareer = (id, careerId) => httpClient.put(`/students/${id}/career`, { careerId });
const remove = (id) => httpClient.delete(`/students/${id}`);

export default { getAll, getById, create, update, changeStatus, changeCareer, remove };