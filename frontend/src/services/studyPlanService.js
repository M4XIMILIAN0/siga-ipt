import httpClient from '../api/httpClient';

// Mirrors StudyPlanController
const getByCareer = (careerId) => httpClient.get(`/careers/${careerId}/study-plans`);
const create = (careerId, plan) => httpClient.post(`/careers/${careerId}/study-plans`, plan);
const remove = (id) => httpClient.delete(`/study-plans/${id}`);

export default { getByCareer, create, remove };