import httpClient from '../api/httpClient';

// STUDENT: mirrors the /api/me endpoints of EnrollmentController
const getMyOffer = (periodId) => httpClient.get('/me/offer', { params: { periodId } });
const getMyEnrollments = (periodId) => httpClient.get('/me/enrollments', { params: { periodId } });
const enroll = (periodId, sectionIds) => httpClient.post('/me/enrollments', { periodId, sectionIds });
const cancel = (id) => httpClient.delete(`/me/enrollments/${id}`);

// ADMIN: all enrollments of a period
const getByPeriod = (periodId) => httpClient.get(`/periods/${periodId}/enrollments`);

export default { getMyOffer, getMyEnrollments, enroll, cancel, getByPeriod };