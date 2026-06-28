import React from 'react';
import { Box, Typography, Button } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

function UnauthorizedPage() {
  const navigate = useNavigate();
  const { role, isAuthenticated } = useAuth();

  // Resolve role from context or localStorage
  const effectiveRole = role || (() => {
    try {
      const saved = localStorage.getItem('hiresense_user');
      return saved ? JSON.parse(saved).role : null;
    } catch { return null; }
  })();

  const handleBack = () => {
    if (effectiveRole === 'SUPER_ADMIN') navigate('/super-admin/companies');
    else if (effectiveRole === 'COMPANY_ADMIN') navigate('/admin/hiring-drives');
    else if (effectiveRole === 'INTERVIEWER') navigate('/interviewer/assignments');
    else navigate('/login');
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 2,
      }}
    >
      <Typography variant="h3" fontWeight={700} color="error">
        403
      </Typography>
      <Typography variant="h6" color="text.secondary">
        You don't have permission to access this page.
      </Typography>
      <Button variant="contained" onClick={handleBack}>
        Go to My Dashboard
      </Button>
    </Box>
  );
}

export default UnauthorizedPage;
