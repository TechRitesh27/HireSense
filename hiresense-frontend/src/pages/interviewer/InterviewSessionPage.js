import React, { useEffect, useState, useCallback } from 'react';
import {
  Box, Typography, Accordion, AccordionSummary, AccordionDetails,
  TextField, Chip, Stack, Button, CircularProgress, Alert, Dialog,
  DialogTitle, DialogContent, DialogActions, Divider, LinearProgress,
  ToggleButton, ToggleButtonGroup, MenuItem,
} from '@mui/material';
import {
  ExpandMore, CheckCircle, Send, ArrowBack, Add,
} from '@mui/icons-material';
import { useParams, useNavigate } from 'react-router-dom';
import InterviewerLayout from '../../components/InterviewerLayout';
import { getSession, evaluateQuestion, submitSession, addCustomQuestion } from '../../api/sessionApi';
import { getCandidateWorkflowHint, getCandidateWorkflowLabel } from '../../utils/interviewWorkflow';

const DIFFICULTY_COLORS = { EASY: 'success', MEDIUM: 'warning', HARD: 'error' };
const DIFFICULTY_LEVELS = ['EASY', 'MEDIUM', 'HARD'];

const VERDICT_LABELS = {
  GOOD: { label: 'Good', color: 'success' },
  AVERAGE: { label: 'Average', color: 'warning' },
  POOR: { label: 'Poor', color: 'error' },
};

function VerdictSelector({ value, onChange, disabled }) {
  return (
    <ToggleButtonGroup
      value={value || null}
      exclusive
      onChange={(_, v) => { if (v !== null) onChange(v); }}
      size="small"
      disabled={disabled}
    >
      {Object.entries(VERDICT_LABELS).map(([key, { label, color }]) => (
        <ToggleButton
          key={key}
          value={key}
          color={color}
          sx={{ minWidth: 80, fontWeight: value === key ? 700 : 400 }}
        >
          {label}
        </ToggleButton>
      ))}
    </ToggleButtonGroup>
  );
}

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

  // Per-question local eval state: { [questionId]: { notes: '', verdict: null } }
  const [evalState, setEvalState] = useState({});

  // Custom question dialog
  const [customDialog, setCustomDialog] = useState(false);
  const [customForm, setCustomForm] = useState({ questionText: '', difficultyLevel: 'MEDIUM' });
  const [addingCustom, setAddingCustom] = useState(false);

  const fetchSession = useCallback(async () => {
    try {
      const res = await getSession(sessionId);
      const s = res.data;
      setSession(s);

      // Seed eval state from server-side data (notes/verdict already persisted)
      setEvalState((prev) => {
        const next = { ...prev };
        s.questions?.forEach((q) => {
          if (!next[q.id]) {
            next[q.id] = { notes: q.notes || '', verdict: q.verdict || null };
          }
        });
        return next;
      });
    } catch {
      setError('Failed to load session.');
    } finally {
      setLoading(false);
    }
  }, [sessionId]);

  useEffect(() => { fetchSession(); }, [fetchSession]);

  const isCompleted = session?.status === 'COMPLETED';

  const handleEvalSave = async (questionId) => {
    if (isCompleted) return;
    const ev = evalState[questionId];
    if (!ev?.verdict) return; // verdict is required
    setSaving(true);
    try {
      await evaluateQuestion(sessionId, questionId, {
        notes: ev.notes,
        verdict: ev.verdict,
      });
    } catch {
      // non-blocking save — errors surfaced on submit if needed
    } finally {
      setSaving(false);
    }
  };

  const handleVerdictChange = async (questionId, verdict) => {
    if (isCompleted) return;
    setEvalState((prev) => ({ ...prev, [questionId]: { ...prev[questionId], verdict } }));
    setSaving(true);
    try {
      await evaluateQuestion(sessionId, questionId, {
        notes: evalState[questionId]?.notes || '',
        verdict,
      });
    } catch {
      // non-blocking
    } finally {
      setSaving(false);
    }
  };

  const handleAddCustomQuestion = async () => {
    if (!customForm.questionText.trim()) return;
    setAddingCustom(true);
    try {
      await addCustomQuestion(sessionId, {
        questionText: customForm.questionText.trim(),
        difficultyLevel: customForm.difficultyLevel,
      });
      setCustomForm({ questionText: '', difficultyLevel: 'MEDIUM' });
      setCustomDialog(false);
      await fetchSession();
    } catch (err) {
      setError(err.response?.data || 'Failed to add question.');
    } finally {
      setAddingCustom(false);
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

  const grouped = session?.questions?.reduce((acc, q) => {
    acc[q.difficultyLevel] = acc[q.difficultyLevel] || [];
    acc[q.difficultyLevel].push(q);
    return acc;
  }, {}) || {};

  const evaluatedCount = session?.questions?.filter((q) => q.verdict).length || 0;
  const totalCount = session?.questions?.length || 0;

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
              {session?.difficultyLevel && (
                <> · <Chip label={session.difficultyLevel} color={DIFFICULTY_COLORS[session.difficultyLevel]} size="small" sx={{ ml: 0.5 }} /></>
              )}
            </Typography>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
              Workflow: {getCandidateWorkflowLabel('INTERVIEW_IN_PROGRESS')} · {getCandidateWorkflowHint('INTERVIEW_IN_PROGRESS')}
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            {isCompleted && session?.sessionScore != null && (
              <Chip label={`Score: ${session.sessionScore}`} color="success" variant="outlined" />
            )}
            <Chip
              label={session?.status}
              color={session?.status === 'COMPLETED' ? 'success' : 'warning'}
              icon={session?.status === 'COMPLETED' ? <CheckCircle /> : undefined}
            />
          </Box>
        </Box>

        {/* Evaluation progress */}
        {!isCompleted && (
          <Box sx={{ mt: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
              <Typography variant="caption" color="text.secondary">Questions Evaluated</Typography>
              <Typography variant="caption" color="text.secondary">{evaluatedCount}/{totalCount}</Typography>
            </Box>
            <LinearProgress
              variant="determinate"
              value={totalCount > 0 ? Math.round((evaluatedCount / totalCount) * 100) : 0}
              sx={{ height: 8, borderRadius: 4 }}
            />
          </Box>
        )}
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}

      {submitted && (
        <Alert severity="success" sx={{ mb: 2 }}>
          Session submitted. Score: <b>{session?.sessionScore ?? 0}</b> / 100
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
              {grouped[level].map((q, idx) => {
                const ev = evalState[q.id] || {};
                const isEvaluated = !!ev.verdict;
                return (
                  <Accordion key={q.id} variant="outlined" defaultExpanded={!isCompleted && level === 'EASY'}>
                    <AccordionSummary expandIcon={<ExpandMore />}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, width: '100%', pr: 1 }}>
                        <Typography variant="body2" fontWeight={500} sx={{ flex: 1 }}>
                          {idx + 1}. {q.questionText}
                        </Typography>
                        <Box sx={{ display: 'flex', gap: 0.5 }}>
                          {q.source === 'CUSTOM' && (
                            <Chip label="Custom" size="small" variant="outlined" />
                          )}
                          {q.skill && (
                            <Chip label={q.skill} size="small" variant="outlined" color="info" />
                          )}
                          {isEvaluated && (
                            <Chip
                              label={VERDICT_LABELS[ev.verdict]?.label}
                              color={VERDICT_LABELS[ev.verdict]?.color}
                              size="small"
                            />
                          )}
                        </Box>
                      </Box>
                    </AccordionSummary>

                    <AccordionDetails>
                      {/* Verdict */}
                      <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ display: 'block', mb: 1 }}>
                        VERDICT
                      </Typography>
                      <Box sx={{ mb: 2 }}>
                        <VerdictSelector
                          value={ev.verdict}
                          onChange={(v) => handleVerdictChange(q.id, v)}
                          disabled={isCompleted}
                        />
                        {!isCompleted && !ev.verdict && (
                          <Typography variant="caption" color="text.disabled" sx={{ display: 'block', mt: 0.5 }}>
                            Select a verdict to record this evaluation
                          </Typography>
                        )}
                      </Box>

                      <Divider sx={{ my: 1.5 }} />

                      {/* Notes */}
                      <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ display: 'block', mb: 1 }}>
                        NOTES
                      </Typography>
                      <TextField
                        multiline
                        rows={2}
                        fullWidth
                        size="small"
                        placeholder="Add your observations about the candidate's answer..."
                        value={ev.notes || ''}
                        onChange={(e) =>
                          setEvalState((prev) => ({
                            ...prev,
                            [q.id]: { ...prev[q.id], notes: e.target.value },
                          }))
                        }
                        onBlur={() => handleEvalSave(q.id)}
                        disabled={isCompleted}
                      />
                    </AccordionDetails>
                  </Accordion>
                );
              })}
            </Stack>
          </Box>
        ) : null
      )}

      {/* Bottom action bar */}
      {!isCompleted && (
        <Box sx={{ mt: 3, display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 2 }}>
          <Button
            variant="outlined"
            startIcon={<Add />}
            onClick={() => setCustomDialog(true)}
          >
            Add Custom Question
          </Button>

          {totalCount > 0 && (
            <Button
              variant="contained"
              size="large"
              color="success"
              startIcon={<Send />}
              onClick={() => setConfirmOpen(true)}
            >
              Submit Interview
            </Button>
          )}
        </Box>
      )}

      {/* Confirm Submit dialog */}
      <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Submit Interview Session?</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary">
            Once submitted, you cannot add questions or change evaluations. The candidate's score will be calculated automatically.
          </Typography>
          <Box sx={{ mt: 2, p: 2, bgcolor: 'grey.50', borderRadius: 2 }}>
            <Typography variant="body2">
              Evaluated: <b>{evaluatedCount}/{totalCount}</b> questions
            </Typography>
            {evaluatedCount < totalCount && (
              <Typography variant="caption" color="text.secondary">
                Unevaluated questions will not contribute to the score.
              </Typography>
            )}
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

      {/* Add Custom Question dialog */}
      <Dialog open={customDialog} onClose={() => !addingCustom && setCustomDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Custom Question</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Question Text"
              multiline
              rows={3}
              fullWidth
              value={customForm.questionText}
              onChange={(e) => setCustomForm((p) => ({ ...p, questionText: e.target.value }))}
              placeholder="Type your question here..."
            />
            <TextField
              select
              label="Difficulty Level"
              fullWidth
              value={customForm.difficultyLevel}
              onChange={(e) => setCustomForm((p) => ({ ...p, difficultyLevel: e.target.value }))}
            >
              {DIFFICULTY_LEVELS.map((d) => (
                <MenuItem key={d} value={d}>{d}</MenuItem>
              ))}
            </TextField>
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setCustomDialog(false)} disabled={addingCustom}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleAddCustomQuestion}
            disabled={addingCustom || !customForm.questionText.trim()}
          >
            {addingCustom ? <CircularProgress size={18} color="inherit" /> : 'Add Question'}
          </Button>
        </DialogActions>
      </Dialog>
    </InterviewerLayout>
  );
}

export default InterviewSessionPage;
