import React, { useState } from 'react';
import {
  Box, Card, CardContent, TextField, Button, Typography,
  Alert, CircularProgress, InputAdornment, IconButton, Divider,
} from '@mui/material';
import { Visibility, VisibilityOff, AdminPanelSettings } from '@mui/icons-material';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { register as registerUser } from '../../api/authApi';
import { useAuth } from '../../context/AuthContext';

function SetupPage() {
  const navigate = useNavigate();
  const { loginUser } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm();

  const onSubmit = async (data) => {
    setLoading(true);
    setError('');
    try {
      const res = await registerUser({
        ...data,
        role: 'SUPER_ADMIN',
      });
      const { token, user } = res.data;
      loginUser(token, user);
      navigate('/super-admin/companies', { replace: true });
    } catch (err) {
      const msg = err.response?.data;
      if (typeof msg === 'string' && msg.toLowerCase().includes('already exists')) {
        setError('A Super Admin already exists. Please use the login page instead.');
      } else {
        setError(msg || 'Setup failed. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
        p: 2,
      }}
    >
      <Box sx={{ width: '100%', maxWidth: 440 }}>
        {/* Header */}
        <Box sx={{ textAlign: 'center', mb: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'center', mb: 1 }}>
            <AdminPanelSettings sx={{ fontSize: 48, color: 'error.main' }} />
          </Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>
            HireSense Setup
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Create the Super Admin account to get started
          </Typography>
        </Box>

        <Card>
          <CardContent sx={{ p: 4 }}>
            <Typography variant="h6" gutterBottom>
              Super Admin Registration
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              This can only be done once. After setup, use the login page.
            </Typography>

            {error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {error}
              </Alert>
            )}

            <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
              <TextField
                label="Full Name"
                fullWidth
                margin="normal"
                size="small"
                {...register('fullName', { required: 'Full name is required' })}
                error={Boolean(errors.fullName)}
                helperText={errors.fullName?.message}
              />

              <TextField
                label="Email"
                type="email"
                fullWidth
                margin="normal"
                size="small"
                {...register('email', {
                  required: 'Email is required',
                  pattern: {
                    value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                    message: 'Enter a valid email address',
                  },
                })}
                error={Boolean(errors.email)}
                helperText={errors.email?.message}
              />

              <TextField
                label="Password"
                type={showPassword ? 'text' : 'password'}
                fullWidth
                margin="normal"
                size="small"
                {...register('password', {
                  required: 'Password is required',
                  minLength: { value: 6, message: 'Minimum 6 characters' },
                })}
                error={Boolean(errors.password)}
                helperText={errors.password?.message}
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        onClick={() => setShowPassword((p) => !p)}
                        edge="end"
                        size="small"
                      >
                        {showPassword ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />

              <Button
                type="submit"
                variant="contained"
                color="error"
                fullWidth
                size="large"
                disabled={loading}
                sx={{ mt: 3, py: 1.4 }}
              >
                {loading ? <CircularProgress size={22} color="inherit" /> : 'Create Super Admin'}
              </Button>
            </Box>

            <Divider sx={{ my: 2.5 }} />

            <Box sx={{ textAlign: 'center' }}>
              <Typography variant="body2" color="text.secondary">
                Already set up?{' '}
                <Button
                  size="small"
                  onClick={() => navigate('/login')}
                  sx={{ textTransform: 'none', p: 0, minWidth: 0 }}
                >
                  Sign in
                </Button>
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Box>
    </Box>
  );
}

export default SetupPage;
