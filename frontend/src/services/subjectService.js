import httpClient from '../api/httpClient';

// Mirrors SubjectController
const getByPlan = (planId) => httpClient.get(`/study-plans/${planId}/subjects`);
const create = (planId, subject) => httpClient.post(`/study-plans/${planId}/subjects`, subject);
const update = (id, subject) => httpClient.put(`/subjects/${id}`, subject);
const remove = (id) => httpClient.delete(`/subjects/${id}`);

export default { getByPlan, create, update, remove };