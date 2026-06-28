import React, { useEffect, useState } from 'react';
import {
  Box, Typography, Card, CardContent, CardActions, Button,
  Chip, CircularProgress, Alert, Stack, TextField, MenuItem, Divider,
} from '@mui/material';
import { PlayArrow, PersonSearch, QuestionAnswer } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import InterviewerLayout from '../../components/InterviewerLayout';
import axiosInstance from '../../api/axiosInstance';
import { startSession } from '../../api/sessionApi';
import { getRounds } from '../../api/roundApi';

const STATUS_COLORS = {
  IMPORTED: 'default',
  ASSIGNED: 'primary',
  INTERVIEW_IN_PROGRESS: 'warning',
  INTERVIEW_COMPLETED: 'success',
  SELECTED: 'success',
  REJECTED: 'error',
};

function InterviewerAssignmentsPage() {
  const navigate = useNavigate();
  const [assignments, setAssignments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Per-card state: { [candidateId]: { rounds, selectedRound, starting } }
  const [cardState, setCardState] = useState({});

  const fetchAssignments = async () => {
    try {
      setLoading(true);
      const res = await axiosInstance.get('/interviewers/me/assignments');
      setAssignments(res.data);
    } catch {
      setError('Failed to load assignments.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchAssignments(); }, []);

  const loadRounds = async (assignment) => {
    const { hiringDriveId, candidateId } = assignment;
    if (cardState[candidateId]?.rounds) return; // already loaded
    try {
      const res = await getRounds(hiringDriveId);
      setCardState((prev) => ({
        ...prev,
        [candidateId]: {
          rounds: res.data,
          selectedRound: res.data[0]?.id || '',
          starting: false,
        },
      }));
    } catch {
      setCardState((prev) => ({
        ...prev,
        [candidateId]: { rounds: [], selectedRound: '', starting: false },
      }));
    }
  };

  const handleStartSession = async (candidateId) => {
    const state = cardState[candidateId];
    if (!state?.selectedRound) return;

    setCardState((prev) => ({
      ...prev,
      [candidateId]: { ...prev[candidateId], starting: true },
    }));

    try {
      const res = await startSession(candidateId, state.selectedRound);
      navigate(`/interviewer/sessions/${res.data.id}`);
    } catch (err) {
      setError(err.response?.data || 'Failed to start session.');
      setCardState((prev) => ({
        ...prev,
        [candidateId]: { ...prev[candidateId], starting: false },
      }));
    }
  };

  return (
    <InterviewerLayout>
      <Typography variant="h5" sx={{ mb: 3 }}>My Assigned Candidates</Typography>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}><CircularProgress /></Box>
      ) : assignments.length === 0 ? (
        <Box sx={{ textAlign: 'center', mt: 8 }}>
          <Typography color="text.secondary">No candidates assigned to you yet.</Typography>
        </Box>
      ) : (
        <Stack spacing={2} sx={{ maxWidth: 720 }}>
          {assignments.map((a) => {
            const state = cardState[a.candidateId];

            return (
              <Card key={a.assignmentId} variant="outlined">
                <CardContent>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                    <Box>
                      <Typography fontWeight={600}>{a.fullName}</Typography>
                      <Typography variant="body2" color="text.secondary">{a.email}</Typography>
                    </Box>
                    <Chip label={a.status} color={STATUS_COLORS[a.status] || 'default'} size="small" />
                  </Box>

                  <Divider sx={{ my: 1.5 }} />

                  <Stack direction="row" spacing={2} sx={{ flexWrap: 'wrap' }}>
                    <Typography variant="caption" color="text.secondary">
                      <b>College:</b> {a.collegeName}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      <b>Branch:</b> {a.branch}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      <b>Drive:</b> {a.hiringDriveTitle}
                    </Typography>
                  </Stack>

                  {/* Round selector — loaded on demand */}
                  {state?.rounds && (
                    <Box sx={{ mt: 2 }}>
                      <TextField
                        select
                        label="Select Round"
                        size="small"
                        value={state.selectedRound}
                        onChange={(e) =>
                          setCardState((prev) => ({
                            ...prev,
                            [a.candidateId]: { ...prev[a.candidateId], selectedRound: e.target.value },
                          }))
                        }
                        sx={{ minWidth: 220 }}
                        disabled={state.rounds.length === 0}
                        helperText={state.rounds.length === 0 ? 'No rounds configured' : ''}
                      >
                        {state.rounds.map((r) => (
                          <MenuItem key={r.id} value={r.id}>
                            {r.name} ({r.roundType})
                          </MenuItem>
                        ))}
                      </TextField>
                    </Box>
                  )}
                </CardContent>

                <CardActions sx={{ px: 2, pb: 2, gap: 1, flexWrap: 'wrap' }}>
                  {/* View Profile — always available */}
                  <Button
                    size="small"
                    variant="outlined"
                    startIcon={<PersonSearch />}
                    onClick={() => navigate(`/interviewer/candidates/${a.candidateId}/profile`, {
                      state: { candidateName: a.fullName }
                    })}
                  >
                    Profile
                  </Button>

                  {!state?.rounds ? (
                    <Button
                      size="small"
                      variant="outlined"
                      onClick={() => loadRounds(a)}
                    >
                      Select Round
                    </Button>
                  ) : (
                    <>
                      {/* Preview Questions for selected round */}
                      {state.selectedRound && (
                        <Button
                          size="small"
                          variant="outlined"
                          startIcon={<QuestionAnswer />}
                          onClick={() => navigate(
                            `/interviewer/candidates/${a.candidateId}/rounds/${state.selectedRound}/questions`,
                            { state: { candidateName: a.fullName, roundName: state.rounds.find(r => r.id === state.selectedRound)?.name } }
                          )}
                        >
                          Questions
                        </Button>
                      )}

                      {/* Start Interview */}
                      <Button
                        size="small"
                        variant="contained"
                        startIcon={state.starting ? <CircularProgress size={14} color="inherit" /> : <PlayArrow />}
                        onClick={() => handleStartSession(a.candidateId)}
                        disabled={!state.selectedRound || state.starting}
                      >
                        Start Interview
                      </Button>
                    </>
                  )}
                </CardActions>
              </Card>
            );
          })}
        </Stack>
      )}
    </InterviewerLayout>
  );
}

export default InterviewerAssignmentsPage;
