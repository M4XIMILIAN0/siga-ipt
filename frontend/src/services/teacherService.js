import httpClient from '../api/httpClient';

// Mirrors TeacherController
const getAll = () => httpClient.get('/teachers');
const getById = (id) => httpClient.get(`/teachers/${id}`);
const create = (teacher) => httpClient.post('/teachers', teacher);
const update = (id, teacher) => httpClient.put(`/teachers/${id}`, teacher);
const changeStatus = (id, status) => httpClient.put(`/teachers/${id}/status`, { status });
const remove = (id) => httpClient.delete(`/teachers/${id}`);

export default { getAll, getById, create, update, changeStatus, remove };