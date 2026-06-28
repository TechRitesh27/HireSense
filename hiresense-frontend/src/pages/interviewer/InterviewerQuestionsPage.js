import React, { useEffect, useState } from 'react';
import {
  Box, Typography, Accordion, AccordionSummary, AccordionDetails,
  Chip, Stack, CircularProgress, Alert, Button, List,
  ListItem, ListItemText, Breadcrumbs,
} from '@mui/material';
import {
  ExpandMore, CheckCircleOutline, ArrowBack, PlayArrow,
} from '@mui/icons-material';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import InterviewerLayout from '../../components/InterviewerLayout';
import { getQuestions } from '../../api/questionApi';
import { startSession } from '../../api/sessionApi';

const DIFFICULTY_COLORS = { EASY: 'success', MEDIUM: 'warning', HARD: 'error' };

function InterviewerQuestionsPage() {
  const { candidateId, roundId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const candidateName = location.state?.candidateName || 'Candidate';
  const roundName = location.state?.roundName || 'Round';

  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [starting, setStarting] = useState(false);

  useEffect(() => {
    const fetchQuestions = async () => {
      try {
        const res = await getQuestions(candidateId, roundId);
        setQuestions(res.data);
      } catch (err) {
        setError(
          err.response?.status === 404
            ? 'No questions generated yet. Ask the admin to generate questions first.'
            : 'Failed to load questions.'
        );
      } finally {
        setLoading(false);
      }
    };
    fetchQuestions();
  }, [candidateId, roundId]);

  const handleStartSession = async () => {
    setStarting(true);
    try {
      const res = await startSession(candidateId, roundId);
      navigate(`/interviewer/sessions/${res.data.id}`);
    } catch (err) {
      setError(err.response?.data || 'Failed to start session.');
      setStarting(false);
    }
  };

  // Group by difficulty
  const grouped = questions.reduce((acc, q) => {
    acc[q.difficultyLevel] = acc[q.difficultyLevel] || [];
    acc[q.difficultyLevel].push(q);
    return acc;
  }, {});

  return (
    <InterviewerLayout>
      <Button
        startIcon={<ArrowBack />}
        size="small"
        onClick={() => navigate('/interviewer/assignments')}
        sx={{ mb: 2 }}
      >
        Back to Assignments
      </Button>

      <Box sx={{ mb: 3, display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h5">{candidateName}</Typography>
          <Typography variant="body2" color="text.secondary">
            Round: <b>{roundName}</b> — Question Preview
          </Typography>
        </Box>
        {questions.length > 0 && (
          <Button
            variant="contained"
            color="success"
            startIcon={starting ? <CircularProgress size={16} color="inherit" /> : <PlayArrow />}
            onClick={handleStartSession}
            disabled={starting}
          >
            Start Interview
          </Button>
        )}
      </Box>

      {error && <Alert severity="warning" sx={{ mb: 2 }}>{error}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}><CircularProgress /></Box>
      ) : questions.length === 0 && !error ? (
        <Box sx={{ textAlign: 'center', mt: 6 }}>
          <Typography color="text.secondary">No questions found for this round.</Typography>
        </Box>
      ) : (
        <Stack spacing={2}>
          {['EASY', 'MEDIUM', 'HARD'].map((level) =>
            grouped[level] ? (
              <Box key={level}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  <Chip label={level} color={DIFFICULTY_COLORS[level]} size="small" />
                  <Typography variant="caption" color="text.secondary">
                    {grouped[level].length} question{grouped[level].length > 1 ? 's' : ''}
                  </Typography>
                </Box>

                {grouped[level].map((q, idx) => (
                  <Accordion key={q.id} variant="outlined" sx={{ mb: 1 }}>
                    <AccordionSummary expandIcon={<ExpandMore />}>
                      <Typography variant="body2" fontWeight={500}>
                        {idx + 1}. {q.questionText}
                      </Typography>
                    </AccordionSummary>
                    <AccordionDetails>
                      <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ display: 'block', mb: 1 }}>
                        KEY POINTS TO LISTEN FOR
                      </Typography>
                      <List dense disablePadding>
                        {q.keyPoints?.map((kp) => (
                          <ListItem key={kp.id} disablePadding sx={{ py: 0.25 }}>
                            <CheckCircleOutline sx={{ fontSize: 15, color: 'text.disabled', mr: 1, flexShrink: 0 }} />
                            <ListItemText
                              primary={kp.pointText}
                              primaryTypographyProps={{ variant: 'body2' }}
                            />
                          </ListItem>
                        ))}
                      </List>
                    </AccordionDetails>
                  </Accordion>
                ))}
              </Box>
            ) : null
          )}
        </Stack>
      )}
    </InterviewerLayout>
  );
}

export default InterviewerQuestionsPage;
