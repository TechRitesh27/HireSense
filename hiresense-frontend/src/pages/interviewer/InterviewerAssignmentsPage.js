import React, { useEffect, useState } from 'react';
import {
  Box, Typography, Card, CardContent, CardActions, Button,
  Chip, CircularProgress, Alert, Stack, TextField, MenuItem,
  Divider, Dialog, DialogTitle, DialogContent, DialogActions,
} from '@mui/material';
import { PlayArrow, PersonSearch } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import InterviewerLayout from '../../components/InterviewerLayout';
import { getAssignedCandidates, startSession } from '../../api/sessionApi';
import { getCandidateWorkflowColor, getCandidateWorkflowHint, getCandidateWorkflowLabel } from '../../utils/interviewWorkflow';

const SESSION_STATUS_COLORS = {
  PENDING: 'default',
  IN_PROGRESS: 'warning',
  COMPLETED: 'success',
};

const DIFFICULTY_LEVELS = ['EASY', 'MEDIUM', 'HARD'];

function InterviewerAssignmentsPage() {
  const navigate = useNavigate();
  const [candidates, setCandidates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Dialog state for starting a session
  const [startDialog, setStartDialog] = useState({ open: false, candidate: null });
  const [startForm, setStartForm] = useState({ difficultyLevel: 'MEDIUM', questionCount: 10 });
  const [starting, setStarting] = useState(false);

  const fetchCandidates = async () => {
    try {
      setLoading(true);
      const res = await getAssignedCandidates();
      setCandidates(res.data);
    } catch {
      setError('Failed to load assigned candidates.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchCandidates(); }, []);

  const openStartDialog = (candidate) => {
    setStartForm({ difficultyLevel: 'MEDIUM', questionCount: 10 });
    setStartDialog({ open: true, candidate });
  };

  const handleStartSession = async () => {
    const { candidate } = startDialog;
    if (!candidate?.sessionId) return;
    setStarting(true);
    try {
      await startSession(candidate.sessionId, {
        difficultyLevel: startForm.difficultyLevel,
        questionCount: Number(startForm.questionCount),
      });
      navigate(`/interviewer/sessions/${candidate.sessionId}`);
    } catch (err) {
      setError(err.response?.data || 'Failed to start session.');
      setStarting(false);
    }
  };

  const handleResumeSession = (sessionId) => {
    navigate(`/interviewer/sessions/${sessionId}`);
  };

  return (
    <InterviewerLayout>
      <Typography variant="h5" sx={{ mb: 3 }}>My Assigned Candidates</Typography>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}><CircularProgress /></Box>
      ) : candidates.length === 0 ? (
        <Box sx={{ textAlign: 'center', mt: 8 }}>
          <Typography color="text.secondary">No candidates assigned to you yet.</Typography>
        </Box>
      ) : (
        <Stack spacing={2} sx={{ maxWidth: 720 }}>
          {candidates.map((c) => (
            <Card key={c.candidateId} variant="outlined">
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                  <Box>
                    <Typography fontWeight={600}>{c.fullName}</Typography>
                    <Typography variant="body2" color="text.secondary">{c.email}</Typography>
                  </Box>
                  <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                    {c.sessionStatus && (
                      <Chip
                        label={c.sessionStatus}
                        color={SESSION_STATUS_COLORS[c.sessionStatus] || 'default'}
                        size="small"
                      />
                    )}
                    <Chip
                      label={getCandidateWorkflowLabel('ASSIGNED')}
                      color={getCandidateWorkflowColor('ASSIGNED')}
                      size="small"
                      variant="outlined"
                    />
                  </Stack>
                </Box>

                <Divider sx={{ my: 1.5 }} />

                <Typography variant="caption" color="text.secondary">
                  <b>Drive:</b> {c.hiringDriveName}
                </Typography>
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
                  {getCandidateWorkflowHint('ASSIGNED')}
                </Typography>
              </CardContent>

              <CardActions sx={{ px: 2, pb: 2, gap: 1, flexWrap: 'wrap' }}>
                <Button
                  size="small"
                  variant="outlined"
                  startIcon={<PersonSearch />}
                  onClick={() => navigate(`/interviewer/candidates/${c.candidateId}/profile`, {
                    state: { candidateName: c.fullName },
                  })}
                >
                  Profile
                </Button>

                {/* Session not yet started */}
                {c.sessionId && c.sessionStatus === 'PENDING' && (
                  <Button
                    size="small"
                    variant="contained"
                    startIcon={<PlayArrow />}
                    onClick={() => openStartDialog(c)}
                  >
                    Start Interview
                  </Button>
                )}

                {/* Session in progress */}
                {c.sessionId && c.sessionStatus === 'IN_PROGRESS' && (
                  <Button
                    size="small"
                    variant="contained"
                    color="warning"
                    onClick={() => handleResumeSession(c.sessionId)}
                  >
                    Resume Interview
                  </Button>
                )}

                {/* Session completed */}
                {c.sessionId && c.sessionStatus === 'COMPLETED' && (
                  <Button
                    size="small"
                    variant="outlined"
                    color="success"
                    onClick={() => handleResumeSession(c.sessionId)}
                  >
                    View Session
                  </Button>
                )}

                {/* No session assigned yet */}
                {!c.sessionId && (
                  <Chip label="No session assigned" size="small" variant="outlined" />
                )}
              </CardActions>
            </Card>
          ))}
        </Stack>
      )}

      {/* Start Session Dialog */}
      <Dialog
        open={startDialog.open}
        onClose={() => !starting && setStartDialog({ open: false, candidate: null })}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle>Start Interview — {startDialog.candidate?.fullName}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              select
              label="Difficulty Level"
              fullWidth
              value={startForm.difficultyLevel}
              onChange={(e) => setStartForm((p) => ({ ...p, difficultyLevel: e.target.value }))}
            >
              {DIFFICULTY_LEVELS.map((d) => (
                <MenuItem key={d} value={d}>{d}</MenuItem>
              ))}
            </TextField>
            <TextField
              label="Number of Questions"
              type="number"
              fullWidth
              inputProps={{ min: 1, max: 50 }}
              value={startForm.questionCount}
              onChange={(e) => setStartForm((p) => ({ ...p, questionCount: e.target.value }))}
              helperText="Between 1 and 50"
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setStartDialog({ open: false, candidate: null })} disabled={starting}>
            Cancel
          </Button>
          <Button
            variant="contained"
            onClick={handleStartSession}
            disabled={starting || !startForm.difficultyLevel || !startForm.questionCount}
          >
            {starting ? <CircularProgress size={18} color="inherit" /> : 'Start'}
          </Button>
        </DialogActions>
      </Dialog>
    </InterviewerLayout>
  );
}

export default InterviewerAssignmentsPage;
