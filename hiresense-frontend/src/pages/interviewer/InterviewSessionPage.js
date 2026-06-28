import React, { useEffect, useState, useCallback } from 'react';
import {
  Box, Typography, Accordion, AccordionSummary, AccordionDetails,
  Checkbox, FormControlLabel, TextField, Slider, Chip, Stack,
  Button, CircularProgress, Alert, Dialog, DialogTitle,
  DialogContent, DialogActions, Divider, LinearProgress,
  Card, CardContent, IconButton, Tooltip,
} from '@mui/material';
import {
  ExpandMore, CheckCircle, Send, ArrowBack,
} from '@mui/icons-material';
import { useParams, useNavigate } from 'react-router-dom';
import InterviewerLayout from '../../components/InterviewerLayout';
import { getSession, markKeyPoints, evaluateQuestion, submitSession } from '../../api/sessionApi';

const DIFFICULTY_COLORS = { EASY: 'success', MEDIUM: 'warning', HARD: 'error' };

function InterviewSessionPage() {
  const { sessionId } = useParams();
  const navigate = useNavigate();

  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  // Local eval state: { [questionId]: { notes: '', score: 0 } }
  const [evalState, setEvalState] = useState({});

  const fetchSession = useCallback(async () => {
    try {
      const res = await getSession(sessionId);
      setSession(res.data);

      // Init eval state for each question
      const init = {};
      res.data.questions?.forEach((q) => {
        init[q.id] = { notes: '', score: 0 };
      });
      setEvalState((prev) => ({ ...init, ...prev }));
    } catch {
      setError('Failed to load session.');
    } finally {
      setLoading(false);
    }
  }, [sessionId]);

  useEffect(() => { fetchSession(); }, [fetchSession]);

  const isCompleted = session?.status === 'COMPLETED';

  // Toggle a single key point
  const handleKeyPointToggle = async (questionId, kpId, currentlyCovered) => {
    if (isCompleted) return;
    setSaving(true);
    try {
      // We only send newly covered points — backend marks them covered
      // For uncovering, we reload session to reflect DB state
      if (!currentlyCovered) {
        await markKeyPoints(sessionId, questionId, [kpId]);
      }
      await fetchSession();
    } catch (err) {
      setError(err.response?.data || 'Failed to save key point.');
    } finally {
      setSaving(false);
    }
  };

  // Save notes + score on blur
  const handleEvalSave = async (questionId) => {
    if (isCompleted) return;
    const ev = evalState[questionId];
    if (!ev) return;
    try {
      await evaluateQuestion(sessionId, questionId, {
        evaluatorNotes: ev.notes,
        additionalScore: ev.score,
      });
    } catch {
      // non-blocking — show subtle error if needed
    }
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      const res = await submitSession(sessionId);
      setSession(res.data);
      setSubmitted(true);
      setConfirmOpen(false);
    } catch (err) {
      setError(err.response?.data || 'Failed to submit session.');
    } finally {
      setSubmitting(false);
    }
  };

  // Group questions by difficulty
  const grouped = session?.questions?.reduce((acc, q) => {
    acc[q.difficultyLevel] = acc[q.difficultyLevel] || [];
    acc[q.difficultyLevel].push(q);
    return acc;
  }, {}) || {};

  const totalKeyPoints = session?.questions?.flatMap((q) => q.keyPoints).length || 0;
  const coveredKeyPoints = session?.questions?.flatMap((q) => q.keyPoints).filter((kp) => kp.covered).length || 0;
  const progress = totalKeyPoints > 0 ? Math.round((coveredKeyPoints / totalKeyPoints) * 100) : 0;

  if (loading) {
    return (
      <InterviewerLayout>
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}><CircularProgress /></Box>
      </InterviewerLayout>
    );
  }

  return (
    <InterviewerLayout>
      {/* Header */}
      <Box sx={{ mb: 3 }}>
        <Button
          startIcon={<ArrowBack />}
          size="small"
          onClick={() => navigate('/interviewer/assignments')}
          sx={{ mb: 1 }}
        >
          Back to Assignments
        </Button>

        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 2 }}>
          <Box>
            <Typography variant="h5">{session?.candidateFullName}</Typography>
            <Typography variant="body2" color="text.secondary">
              Round: <b>{session?.interviewRoundName}</b>
            </Typography>
          </Box>
          <Chip
            label={session?.status}
            color={session?.status === 'COMPLETED' ? 'success' : 'warning'}
            icon={session?.status === 'COMPLETED' ? <CheckCircle /> : undefined}
          />
        </Box>

        {/* Progress bar */}
        <Box sx={{ mt: 2 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
            <Typography variant="caption" color="text.secondary">Key Points Covered</Typography>
            <Typography variant="caption" color="text.secondary">{coveredKeyPoints}/{totalKeyPoints}</Typography>
          </Box>
          <LinearProgress variant="determinate" value={progress} sx={{ height: 8, borderRadius: 4 }} />
        </Box>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}

      {submitted && (
        <Alert severity="success" sx={{ mb: 2 }}>
          Session submitted successfully! The candidate's score has been calculated.
        </Alert>
      )}

      {saving && <LinearProgress sx={{ mb: 1 }} />}

      {/* Questions grouped by difficulty */}
      {['EASY', 'MEDIUM', 'HARD'].map((level) =>
        grouped[level] ? (
          <Box key={level} sx={{ mb: 3 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1.5 }}>
              <Chip label={level} color={DIFFICULTY_COLORS[level]} size="small" />
              <Typography variant="body2" color="text.secondary">
                {grouped[level].length} question{grouped[level].length > 1 ? 's' : ''}
              </Typography>
            </Box>

            <Stack spacing={1.5}>
              {grouped[level].map((q, idx) => (
                <Accordion key={q.id} variant="outlined" defaultExpanded={level === 'EASY'}>
                  <AccordionSummary expandIcon={<ExpandMore />}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, width: '100%', pr: 1 }}>
                      <Typography variant="body2" fontWeight={500} sx={{ flex: 1 }}>
                        {idx + 1}. {q.questionText}
                      </Typography>
                      <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: 'nowrap' }}>
                        {q.keyPoints?.filter((kp) => kp.covered).length}/{q.keyPoints?.length} pts
                      </Typography>
                    </Box>
                  </AccordionSummary>

                  <AccordionDetails>
                    {/* Key Points */}
                    <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ display: 'block', mb: 1 }}>
                      KEY POINTS
                    </Typography>
                    <Stack spacing={0.5} sx={{ mb: 2 }}>
                      {q.keyPoints?.map((kp) => (
                        <FormControlLabel
                          key={kp.id}
                          control={
                            <Checkbox
                              checked={kp.covered}
                              onChange={() => handleKeyPointToggle(q.id, kp.id, kp.covered)}
                              disabled={isCompleted || kp.covered} // covered points lock
                              size="small"
                              color="success"
                            />
                          }
                          label={
                            <Typography
                              variant="body2"
                              sx={{ textDecoration: kp.covered ? 'line-through' : 'none', color: kp.covered ? 'text.disabled' : 'text.primary' }}
                            >
                              {kp.pointText}
                            </Typography>
                          }
                        />
                      ))}
                    </Stack>

                    <Divider sx={{ my: 1.5 }} />

                    {/* Evaluator Notes */}
                    <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ display: 'block', mb: 1 }}>
                      EVALUATOR NOTES
                    </Typography>
                    <TextField
                      multiline
                      rows={2}
                      fullWidth
                      size="small"
                      placeholder="Add your observations about the candidate's answer..."
                      value={evalState[q.id]?.notes || ''}
                      onChange={(e) =>
                        setEvalState((prev) => ({
                          ...prev,
                          [q.id]: { ...prev[q.id], notes: e.target.value },
                        }))
                      }
                      onBlur={() => handleEvalSave(q.id)}
                      disabled={isCompleted}
                      sx={{ mb: 2 }}
                    />

                    {/* Additional Score */}
                    <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ display: 'block', mb: 0.5 }}>
                      ADDITIONAL SCORE: {evalState[q.id]?.score || 0}/10
                    </Typography>
                    <Slider
                      value={evalState[q.id]?.score || 0}
                      min={0}
                      max={10}
                      step={1}
                      marks
                      valueLabelDisplay="auto"
                      onChange={(_, val) =>
                        setEvalState((prev) => ({
                          ...prev,
                          [q.id]: { ...prev[q.id], score: val },
                        }))
                      }
                      onChangeCommitted={() => handleEvalSave(q.id)}
                      disabled={isCompleted}
                      color="primary"
                      sx={{ maxWidth: 320 }}
                    />
                  </AccordionDetails>
                </Accordion>
              ))}
            </Stack>
          </Box>
        ) : null
      )}

      {/* Submit button */}
      {!isCompleted && session?.questions?.length > 0 && (
        <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
          <Button
            variant="contained"
            size="large"
            color="success"
            startIcon={<Send />}
            onClick={() => setConfirmOpen(true)}
          >
            Submit Interview
          </Button>
        </Box>
      )}

      {/* Confirm dialog */}
      <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Submit Interview Session?</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary">
            Once submitted, you cannot modify key points, notes, or scores. The candidate's score will be calculated automatically.
          </Typography>
          <Box sx={{ mt: 2, p: 2, bgcolor: 'grey.50', borderRadius: 2 }}>
            <Typography variant="body2">
              Key points covered: <b>{coveredKeyPoints}/{totalKeyPoints}</b>
            </Typography>
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setConfirmOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            color="success"
            onClick={handleSubmit}
            disabled={submitting}
          >
            {submitting ? <CircularProgress size={18} color="inherit" /> : 'Confirm Submit'}
          </Button>
        </DialogActions>
      </Dialog>
    </InterviewerLayout>
  );
}

export default InterviewSessionPage;
