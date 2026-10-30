import httpClient from '../api/httpClient';

// TEACHER: mirrors GradeController (/api/teacher)
const getMySections = (periodId) => httpClient.get('/teacher/sections', { params: { periodId } });
const getSectionGrades = (sectionId) => httpClient.get(`/teacher/sections/${sectionId}/grades`);
const registerGrade = (enrollmentId, finalGrade) =>
  httpClient.put(`/teacher/enrollments/${enrollmentId}/grade`, { finalGrade });

export default { getMySections, getSectionGrades, registerGrade };