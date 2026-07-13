import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, CssBaseline, Box, CircularProgress } from '@mui/material';

import theme from './theme/theme';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './routes/ProtectedRoute';

// Auth
import LoginPage from './pages/auth/LoginPage';
import SetupPage from './pages/auth/SetupPage';
import OAuthCallbackPage from './pages/auth/OAuthCallbackPage';

// Shared
import UnauthorizedPage from './pages/UnauthorizedPage';

// Admin pages (Phase 2+ — placeholders for now)
const AdminHiringDrivesPage  = React.lazy(() => import('./pages/admin/AdminHiringDrivesPage'));
const AdminCandidatesPage    = React.lazy(() => import('./pages/admin/AdminCandidatesPage'));
const AdminRankingsPage      = React.lazy(() => import('./pages/admin/AdminRankingsPage'));

// Phase 3
const AdminCandidateDetailPage = React.lazy(() => import('./pages/admin/AdminCandidateDetailPage'));
const AdminRoundsPage          = React.lazy(() => import('./pages/admin/AdminRoundsPage'));
const AdminInterviewersPage    = React.lazy(() => import('./pages/admin/AdminInterviewersPage'));

// Super Admin
const SuperAdminCompaniesPage = React.lazy(() => import('./pages/superadmin/SuperAdminCompaniesPage'));

// Interviewer pages
const InterviewerAssignmentsPage     = React.lazy(() => import('./pages/interviewer/InterviewerAssignmentsPage'));
const InterviewSessionPage           = React.lazy(() => import('./pages/interviewer/InterviewSessionPage'));
const InterviewerCandidateProfilePage = React.lazy(() => import('./pages/interviewer/InterviewerCandidateProfilePage'));
const InterviewerQuestionsPage       = React.lazy(() => import('./pages/interviewer/InterviewerQuestionsPage'));

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AuthProvider>
        <BrowserRouter>
          <React.Suspense fallback={
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
              <CircularProgress />
            </Box>
          }>
            <Routes>
              {/* Public */}
              <Route path="/login" element={<LoginPage />} />
              <Route path="/setup" element={<SetupPage />} />
              <Route path="/oauth-callback" element={<OAuthCallbackPage />} />
              <Route path="/unauthorized" element={<UnauthorizedPage />} />

              {/* Default redirect */}
              <Route path="/" element={<Navigate to="/login" replace />} />

              {/* Company Admin */}
              <Route
                path="/admin/hiring-drives"
                element={
                  <ProtectedRoute allowedRoles={['COMPANY_ADMIN']}>
                    <AdminHiringDrivesPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/hiring-drives/:hiringDriveId/candidates"
                element={
                  <ProtectedRoute allowedRoles={['COMPANY_ADMIN']}>
                    <AdminCandidatesPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/hiring-drives/:hiringDriveId/rankings"
                element={
                  <ProtectedRoute allowedRoles={['COMPANY_ADMIN']}>
                    <AdminRankingsPage />
                  </ProtectedRoute>
                }
              />

              {/* Phase 3 */}
              <Route
                path="/admin/hiring-drives/:hiringDriveId/rounds"
                element={
                  <ProtectedRoute allowedRoles={['COMPANY_ADMIN']}>
                    <AdminRoundsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/candidates/:candidateId"
                element={
                  <ProtectedRoute allowedRoles={['COMPANY_ADMIN']}>
                    <AdminCandidateDetailPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/interviewers"
                element={
                  <ProtectedRoute allowedRoles={['COMPANY_ADMIN']}>
                    <AdminInterviewersPage />
                  </ProtectedRoute>
                }
              />

              {/* Interviewer */}
              <Route
                path="/interviewer/assignments"
                element={
                  <ProtectedRoute allowedRoles={['INTERVIEWER']}>
                    <InterviewerAssignmentsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/interviewer/sessions/:sessionId"
                element={
                  <ProtectedRoute allowedRoles={['INTERVIEWER']}>
                    <InterviewSessionPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/interviewer/candidates/:candidateId/profile"
                element={
                  <ProtectedRoute allowedRoles={['INTERVIEWER']}>
                    <InterviewerCandidateProfilePage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/interviewer/candidates/:candidateId/rounds/:roundId/questions"
                element={
                  <ProtectedRoute allowedRoles={['INTERVIEWER']}>
                    <InterviewerQuestionsPage />
                  </ProtectedRoute>
                }
              />

              {/* Super Admin */}
              <Route
                path="/super-admin/companies"
                element={
                  <ProtectedRoute allowedRoles={['SUPER_ADMIN']}>
                    <SuperAdminCompaniesPage />
                  </ProtectedRoute>
                }
              />

              {/* Catch-all */}
              <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
          </React.Suspense>
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  );
}

export default App;
