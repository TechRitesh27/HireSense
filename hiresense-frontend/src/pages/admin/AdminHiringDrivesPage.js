import React, { useEffect, useState } from 'react';
import {
  Box, Typography, Button, Card, CardContent, CardActions,
  Chip, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, MenuItem, Grid, CircularProgress, Alert, Stack,
  IconButton, Tooltip,
} from '@mui/material';
import { Add, PeopleAlt, ArrowForward } from '@mui/icons-material';
import { useForm, Controller } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import AdminLayout from '../../components/AdminLayout';
import { getHiringDrives, createHiringDrive, updateHiringDriveStatus } from '../../api/hiringDriveApi';

const STATUS_COLORS = {
  DRAFT: 'default',
  ACTIVE: 'success',
  COMPLETED: 'info',
  CANCELLED: 'error',
};

const STATUS_OPTIONS = ['DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED'];

function AdminHiringDrivesPage() {
  const navigate = useNavigate();
  const [drives, setDrives] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const { register, handleSubmit, reset, control, formState: { errors } } = useForm();

  const fetchDrives = async () => {
    try {
      setLoading(true);
      const res = await getHiringDrives();
      setDrives(res.data);
    } catch {
      setError('Failed to load hiring drives.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchDrives(); }, []);

  const handleCreate = async (data) => {
    setSubmitting(true);
    try {
      await createHiringDrive(data);
      reset();
      setCreateOpen(false);
      fetchDrives();
    } catch (err) {
      setError(err.response?.data || 'Failed to create hiring drive.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleStatusChange = async (driveId, newStatus) => {
    try {
      await updateHiringDriveStatus(driveId, newStatus);
      fetchDrives();
    } catch (err) {
      setError(err.response?.data || 'Failed to update status.');
    }
  };

  return (
    <AdminLayout>
      <Box sx={{ mb: 3, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Typography variant="h5">Hiring Drives</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => setCreateOpen(true)}>
          New Drive
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}>
          <CircularProgress />
        </Box>
      ) : drives.length === 0 ? (
        <Box sx={{ textAlign: 'center', mt: 8 }}>
          <Typography color="text.secondary">No hiring drives yet. Create one to get started.</Typography>
        </Box>
      ) : (
        <Grid container spacing={2}>
          {drives.map((drive) => (
            <Grid item xs={12} sm={6} md={4} key={drive.id}>
              <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                <CardContent sx={{ flex: 1 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                    <Typography variant="h6" fontWeight={600} sx={{ fontSize: '1rem' }}>
                      {drive.title}
                    </Typography>
                    <Chip
                      label={drive.status}
                      color={STATUS_COLORS[drive.status]}
                      size="small"
                    />
                  </Box>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5, minHeight: 40 }}>
                    {drive.description || 'No description'}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {drive.startDate} → {drive.endDate}
                  </Typography>

                  {/* Status change */}
                  <TextField
                    select
                    size="small"
                    label="Change Status"
                    value={drive.status}
                    onChange={(e) => handleStatusChange(drive.id, e.target.value)}
                    sx={{ mt: 2, width: '100%' }}
                  >
                    {STATUS_OPTIONS.map((s) => (
                      <MenuItem key={s} value={s}>{s}</MenuItem>
                    ))}
                  </TextField>
                </CardContent>
                <CardActions sx={{ px: 2, pb: 2 }}>
                  <Button
                    size="small"
                    endIcon={<ArrowForward />}
                    onClick={() => navigate(`/admin/hiring-drives/${drive.id}/candidates`)}
                  >
                    View Candidates
                  </Button>
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Create Dialog */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create Hiring Drive</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Title"
              fullWidth
              {...register('title', { required: 'Title is required' })}
              error={Boolean(errors.title)}
              helperText={errors.title?.message}
            />
            <TextField
              label="Description"
              fullWidth
              multiline
              rows={3}
              {...register('description')}
            />
            <TextField
              label="Start Date"
              type="date"
              fullWidth
              InputLabelProps={{ shrink: true }}
              {...register('startDate', { required: 'Start date is required' })}
              error={Boolean(errors.startDate)}
              helperText={errors.startDate?.message}
            />
            <TextField
              label="End Date"
              type="date"
              fullWidth
              InputLabelProps={{ shrink: true }}
              {...register('endDate', { required: 'End date is required' })}
              error={Boolean(errors.endDate)}
              helperText={errors.endDate?.message}
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

export default AdminHiringDrivesPage;
