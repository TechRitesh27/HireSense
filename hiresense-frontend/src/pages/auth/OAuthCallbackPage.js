import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Box, CircularProgress, Typography, Alert } from '@mui/material';
import { useAuth } from '../../context/AuthContext';

function decodeToken(token) {
  try {
    const payload = token.split('.')[1];
    return JSON.parse(atob(payload));
  } catch {
    return null;
  }
}

function OAuthCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { loginUser } = useAuth();
  const [error, setError] = useState('');

  useEffect(() => {
    const token = searchParams.get('token');

    if (!token) {
      setError('Authentication token was not provided by the server.');
      return;
    }

    const claims = decodeToken(token);
    const email = claims?.sub || claims?.email || null;
    const role = claims?.role || 'INTERVIEWER';

    if (!email) {
      setError('The authentication token did not contain a valid user identity.');
      return;
    }

    const userData = { email, role };
    loginUser(token, userData);

    if (role === 'SUPER_ADMIN') {
      navigate('/super-admin/companies', { replace: true });
    } else if (role === 'COMPANY_ADMIN') {
      navigate('/admin/hiring-drives', { replace: true });
    } else if (role === 'INTERVIEWER') {
      navigate('/interviewer/assignments', { replace: true });
    } else {
      navigate('/login', { replace: true });
    }
  }, [loginUser, navigate, searchParams]);

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 2,
        bgcolor: 'background.default',
        p: 2,
      }}
    >
      {error ? (
        <Alert severity="error">{error}</Alert>
      ) : (
        <>
          <CircularProgress />
          <Typography variant="body1" color="text.secondary">
            Completing sign-in...
          </Typography>
        </>
      )}
    </Box>
  );
}

export default OAuthCallbackPage;
