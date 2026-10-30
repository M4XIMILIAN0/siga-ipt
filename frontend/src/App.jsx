import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import RoleRoute from './components/RoleRoute';
import HomePage from './pages/HomePage';
import CareersPage from './pages/CareersPage';
import CareerDetailPage from './pages/CareerDetailPage';
import StudentsPage from './pages/StudentsPage';
import TeachersPage from './pages/TeachersPage';
import PeriodsPage from './pages/PeriodsPage';
import MyEnrollmentsPage from './pages/MyEnrollmentsPage';
import TeacherGradesPage from './pages/TeacherGradesPage';
import MyRecordPage from './pages/MyRecordPage';


function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<HomePage />} />

        {/* ADMIN only (section 4.3) */}
        <Route path="/careers" element={<RoleRoute roles={['ADMIN']}><CareersPage /></RoleRoute>} />
        <Route path="/careers/:careerId" element={<RoleRoute roles={['ADMIN']}><CareerDetailPage /></RoleRoute>} />
        <Route path="/students" element={<RoleRoute roles={['ADMIN']}><StudentsPage /></RoleRoute>} />
        <Route path="/teachers" element={<RoleRoute roles={['ADMIN']}><TeachersPage /></RoleRoute>} />
        <Route path="/periods" element={<RoleRoute roles={['ADMIN']}><PeriodsPage /></RoleRoute>} />
        <Route path="/my-enrollments" element={<RoleRoute roles={['STUDENT']}><MyEnrollmentsPage /></RoleRoute>} />
        <Route path="/my-sections" element={<RoleRoute roles={['TEACHER']}><TeacherGradesPage /></RoleRoute>} />
        <Route path="/my-record" element={<RoleRoute roles={['STUDENT']}><MyRecordPage /></RoleRoute>} />
      </Routes>
    </Layout>
  );
}

export default App;