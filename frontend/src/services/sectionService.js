import httpClient from '../api/httpClient';

// Mirrors SectionController
const getByPeriod = (periodId) => httpClient.get(`/periods/${periodId}/sections`);
const create = (periodId, section) => httpClient.post(`/periods/${periodId}/sections`, section);
const remove = (id) => httpClient.delete(`/sections/${id}`);

export default { getByPeriod, create, remove };