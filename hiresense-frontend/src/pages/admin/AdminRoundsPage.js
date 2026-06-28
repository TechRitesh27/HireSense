import React, { useEffect, useState } from 'react';
import {
  Box, Typography, Button, Card, CardContent, Chip, Stack,
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, MenuItem, CircularProgress, Alert, Breadcrumbs,
  Link, Divider,
} from '@mui/material';
import { Add, MeetingRoom } from '@mui/icons-material';
import { useForm } from 'react-hook-form';
import { useParams, useNavigate } from 'react-router-dom';
import AdminLayout from '../../components/AdminLayout';
import { getRounds, createRound } from '../../api/roundApi';

const ROUND_TYPE_COLORS = {
  TECHNICAL: 'primary',
  HR: 'secondary',
  CUSTOM: 'default',
};

const ROUND_TYPES = ['TECHNICAL', 'HR', 'CUSTOM'];

function AdminRoundsPage() {
  const { hiringDriveId } = useParams();
  const navigate = useNavigate();

  const [rounds, setRounds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const { register, handleSubmit, reset, watch, setValue, formState: { errors } } = useForm({
    defaultValues: { roundType: 'TECHNICAL' },
  });

  const fetchRounds = async () => {
    try {
      setLoading(true);
      const res = await getRounds(hiringDriveId);
      setRounds(res.data);
    } catch {
      setError('Failed to load rounds.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchRounds(); }, [hiringDriveId]);

  const handleCreate = async (data) => {
    setSubmitting(true);
    try {
      await createRound(hiringDriveId, data);
      reset({ roundType: 'TECHNICAL' });
      setCreateOpen(false);
      fetchRounds();
    } catch (err) {
      setError(err.response?.data || 'Failed to create round.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AdminLayout>
      <Breadcrumbs sx={{ mb: 2 }}>
        <Link component="button" variant="body2" underline="hover" onClick={() => navigate('/admin/hiring-drives')}>
          Hiring Drives
        </Link>
        <Typography variant="body2" color="text.primary">Interview Rounds</Typography>
      </Breadcrumbs>

      <Box sx={{ mb: 3, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Typography variant="h5">Interview Rounds</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => setCreateOpen(true)}>
          Add Round
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}><CircularProgress /></Box>
      ) : rounds.length === 0 ? (
        <Box sx={{ textAlign: 'center', mt: 8 }}>
          <MeetingRoom sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
          <Typography color="text.secondary">No rounds configured yet.</Typography>
          <Typography variant="body2" color="text.secondary">
            Add at least one round (TECHNICAL, HR) before generating questions.
          </Typography>
        </Box>
      ) : (
        <Stack spacing={2} sx={{ maxWidth: 600 }}>
          {rounds.map((round, index) => (
            <Card key={round.id} variant="outlined">
              <CardContent sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', py: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Box
                    sx={{
                      width: 36, height: 36, borderRadius: '50%',
                      bgcolor: 'primary.light', color: 'white',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontWeight: 700, fontSize: 14,
                    }}
                  >
                    {index + 1}
                  </Box>
                  <Box>
                    <Typography fontWeight={600}>{round.name}</Typography>
                    <Typography variant="caption" color="text.secondary">{round.id}</Typography>
                  </Box>
                </Box>
                <Chip
                  label={round.roundType}
                  color={ROUND_TYPE_COLORS[round.roundType] || 'default'}
                  size="small"
                />
              </CardContent>
            </Card>
          ))}
        </Stack>
      )}

      {/* Create Round Dialog */}
      <Dialog open={createOpen} onClose={() => { setCreateOpen(false); reset({ roundType: 'TECHNICAL' }); }} maxWidth="xs" fullWidth>
        <DialogTitle>Add Interview Round</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Round Name"
              fullWidth
              placeholder="e.g. Technical Round 1"
              {...register('name', { required: 'Round name is required' })}
              error={Boolean(errors.name)}
              helperText={errors.name?.message}
            />
            <TextField
              select
              label="Round Type"
              fullWidth
              value={watch('roundType')}
              onChange={(e) => setValue('roundType', e.target.value)}
            >
              {ROUND_TYPES.map((t) => (
                <MenuItem key={t} value={t}>{t}</MenuItem>
              ))}
            </TextField>
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => { setCreateOpen(false); reset({ roundType: 'TECHNICAL' }); }}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit(handleCreate)} disabled={submitting}>
            {submitting ? <CircularProgress size={18} /> : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </AdminLayout>
  );
}

export default AdminRoundsPage;
