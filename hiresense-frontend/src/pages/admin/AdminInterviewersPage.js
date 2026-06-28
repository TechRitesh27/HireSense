import React, { useEffect, useState } from 'react';
import {
  Box, Typography, Button, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Paper, Avatar,
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Stack, CircularProgress, Alert, Chip,
} from '@mui/material';
import { Add } from '@mui/icons-material';
import { useForm } from 'react-hook-form';
import AdminLayout from '../../components/AdminLayout';
import { getInterviewers, createInterviewer } from '../../api/userApi';

function AdminInterviewersPage() {
  const [interviewers, setInterviewers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const { register, handleSubmit, reset, formState: { errors } } = useForm();

  const fetchInterviewers = async () => {
    try {
      setLoading(true);
      const res = await getInterviewers();
      setInterviewers(res.data);
    } catch {
      setError('Failed to load interviewers.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchInterviewers(); }, []);

  const handleCreate = async (data) => {
    setSubmitting(true);
    try {
      await createInterviewer(data);
      reset();
      setCreateOpen(false);
      setSuccess('Interviewer created successfully.');
      fetchInterviewers();
    } catch (err) {
      setError(err.response?.data || 'Failed to create interviewer.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AdminLayout>
      <Box sx={{ mb: 3, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Box>
          <Typography variant="h5">Interviewers</Typography>
          <Typography variant="body2" color="text.secondary">
            Manage interviewers for your company
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<Add />} onClick={() => setCreateOpen(true)}>
          Add Interviewer
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess('')}>{success}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}><CircularProgress /></Box>
      ) : (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow sx={{ bgcolor: 'grey.50' }}>
                <TableCell><b>Interviewer</b></TableCell>
                <TableCell><b>Email</b></TableCell>
                <TableCell><b>Role</b></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {interviewers.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={3} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    No interviewers yet. Add one to get started.
                  </TableCell>
                </TableRow>
              ) : (
                interviewers.map((iv) => (
                  <TableRow key={iv.id} hover>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                        <Avatar sx={{ width: 32, height: 32, bgcolor: 'secondary.light', fontSize: 14 }}>
                          {iv.fullName[0]}
                        </Avatar>
                        <Typography variant="body2" fontWeight={500}>{iv.fullName}</Typography>
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">{iv.email}</Typography>
                    </TableCell>
                    <TableCell>
                      <Chip label="INTERVIEWER" size="small" color="secondary" variant="outlined" />
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Create Interviewer Dialog */}
      <Dialog open={createOpen} onClose={() => { setCreateOpen(false); reset(); }} maxWidth="xs" fullWidth>
        <DialogTitle>Add Interviewer</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Full Name"
              fullWidth
              size="small"
              {...register('fullName', { required: 'Full name is required' })}
              error={Boolean(errors.fullName)}
              helperText={errors.fullName?.message}
            />
            <TextField
              label="Email"
              type="email"
              fullWidth
              size="small"
              {...register('email', { required: 'Email is required' })}
              error={Boolean(errors.email)}
              helperText={errors.email?.message}
            />
            <TextField
              label="Password"
              type="password"
              fullWidth
              size="small"
              {...register('password', {
                required: 'Password is required',
                minLength: { value: 6, message: 'Minimum 6 characters' },
              })}
              error={Boolean(errors.password)}
              helperText={errors.password?.message}
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => { setCreateOpen(false); reset(); }}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit(handleCreate)} disabled={submitting}>
            {submitting ? <CircularProgress size={18} /> : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </AdminLayout>
  );
}

export default AdminInterviewersPage;
