import React, { useEffect, useState } from 'react';
import {
  Box, Typography, Button, Card, CardContent, CardHeader,
  Chip, Stack, Divider, CircularProgress, Alert,
  Accordion, AccordionSummary, AccordionDetails,
  List, ListItem, ListItemText, TextField, MenuItem,
  Breadcrumbs, Link, Grid, Tabs, Tab,
} from '@mui/material';
import {
  ExpandMore, AutoAwesome, PersonSearch, CheckCircleOutline,
} from '@mui/icons-material';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import AdminLayout from '../../components/AdminLayout';
import { parseResume, getCandidateProfile } from '../../api/resumeApi';
import { generateQuestions, getQuestions } from '../../api/questionApi';
import { getRounds } from '../../api/roundApi';

const DIFFICULTY_COLORS = { EASY: 'success', MEDIUM: 'warning', HARD: 'error' };

function TabPanel({ children, value, index }) {
  return value === index ? <Box sx={{ pt: 3 }}>{children}</Box> : null;
}

function AdminCandidateDetailPage() {
  const { candidateId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  // Candidate name may be passed via navigation state for breadcrumb
  const candidateName = location.state?.candidateName || 'Candidate';
  const hiringDriveId = location.state?.hiringDriveId;

  const [tab, setTab] = useState(0);
  const [profile, setProfile] = useState(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const [profileError, setProfileError] = useState('');
  const [profileSuccess, setProfileSuccess] = useState('');

  const [rounds, setRounds] = useState([]);
  const [selectedRoundId, setSelectedRoundId] = useState('');
  const [questions, setQuestions] = useState([]);
  const [questionsLoading, setQuestionsLoading] = useState(false);
  const [questionsError, setQuestionsError] = useState('');
  const [generating, setGenerating] = useState(false);

  // Load existing profile
  const fetchProfile = async () => {
    try {
      setProfileLoading(true);
      const res = await getCandidateProfile(candidateId);
      setProfile(res.data);
    } catch {
      // No profile yet — that's fine
    } finally {
      setProfileLoading(false);
    }
  };

  // Load rounds for question generation
  const fetchRounds = async () => {
    if (!hiringDriveId) return;
    try {
      const res = await getRounds(hiringDriveId);
      setRounds(res.data);
      if (res.data.length > 0) setSelectedRoundId(res.data[0].id);
    } catch {
      // ignore
    }
  };

  useEffect(() => {
    fetchProfile();
    fetchRounds();
  }, [candidateId]);

  const handleParseResume = async () => {
    setProfileLoading(true);
    setProfileError('');
    setProfileSuccess('');
    try {
      const res = await parseResume(candidateId);
      setProfile(res.data);
      setProfileSuccess('Resume parsed successfully.');
    } catch (err) {
      setProfileError(err.response?.data || 'Failed to parse resume.');
    } finally {
      setProfileLoading(false);
    }
  };

  const handleGenerateQuestions = async () => {
    if (!selectedRoundId) return;
    setGenerating(true);
    setQuestionsError('');
    try {
      const res = await generateQuestions(candidateId, selectedRoundId);
      setQuestions(res.data);
    } catch (err) {
      setQuestionsError(err.response?.data || 'Failed to generate questions.');
    } finally {
      setGenerating(false);
    }
  };

  const handleLoadQuestions = async () => {
    if (!selectedRoundId) return;
    setQuestionsLoading(true);
    setQuestionsError('');
    try {
      const res = await getQuestions(candidateId, selectedRoundId);
      setQuestions(res.data);
    } catch (err) {
      setQuestionsError(err.response?.data || 'No questions found for this round.');
    } finally {
      setQuestionsLoading(false);
    }
  };

  // Group questions by difficulty
  const grouped = questions.reduce((acc, q) => {
    acc[q.difficultyLevel] = acc[q.difficultyLevel] || [];
    acc[q.difficultyLevel].push(q);
    return acc;
  }, {});

  return (
    <AdminLayout>
      <Breadcrumbs sx={{ mb: 2 }}>
        <Link component="button" variant="body2" underline="hover" onClick={() => navigate('/admin/hiring-drives')}>
          Hiring Drives
        </Link>
        {hiringDriveId && (
          <Link
            component="button"
            variant="body2"
            underline="hover"
            onClick={() => navigate(`/admin/hiring-drives/${hiringDriveId}/candidates`)}
          >
            Candidates
          </Link>
        )}
        <Typography variant="body2" color="text.primary">{candidateName}</Typography>
      </Breadcrumbs>

      <Typography variant="h5" sx={{ mb: 1 }}>{candidateName}</Typography>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 1, borderBottom: 1, borderColor: 'divider' }}>
        <Tab label="Resume Profile" />
        <Tab label="Questions" />
      </Tabs>

      {/* ── Tab 0: Resume Profile ── */}
      <TabPanel value={tab} index={0}>
        {profileError && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setProfileError('')}>{profileError}</Alert>}
        {profileSuccess && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setProfileSuccess('')}>{profileSuccess}</Alert>}

        <Box sx={{ mb: 2 }}>
          <Button
            variant="contained"
            startIcon={profileLoading ? <CircularProgress size={16} color="inherit" /> : <PersonSearch />}
            onClick={handleParseResume}
            disabled={profileLoading}
          >
            {profile ? 'Re-Parse Resume' : 'Parse Resume'}
          </Button>
        </Box>

        {profileLoading && !profile ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}><CircularProgress /></Box>
        ) : profile ? (
          <Grid container spacing={3}>
            {/* Skills */}
            <Grid item xs={12} md={5}>
              <Card variant="outlined">
                <CardHeader title="Skills & Technologies" titleTypographyProps={{ variant: 'subtitle1', fontWeight: 600 }} />
                <Divider />
                <CardContent>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                    {profile.skills?.map((skill) => (
                      <Chip key={skill} label={skill} size="small" variant="outlined" color="primary" />
                    ))}
                  </Box>
                  <Box sx={{ mt: 1.5 }}>
                    <Chip
                      label={`Status: ${profile.status}`}
                      color={profile.status === 'PARSED' ? 'success' : 'default'}
                      size="small"
                    />
                  </Box>
                </CardContent>
              </Card>
            </Grid>

            {/* Projects */}
            <Grid item xs={12} md={7}>
              <Card variant="outlined">
                <CardHeader title="Projects" titleTypographyProps={{ variant: 'subtitle1', fontWeight: 600 }} />
                <Divider />
                <CardContent>
                  <Stack spacing={2}>
                    {profile.projects?.map((project) => (
                      <Box key={project.id}>
                        <Typography fontWeight={600}>{project.projectName}</Typography>
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
                          {project.description}
                        </Typography>
                        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                          {project.techStack?.split(',').map((t) => (
                            <Chip key={t} label={t.trim()} size="small" sx={{ fontSize: '0.7rem' }} />
                          ))}
                        </Box>
                      </Box>
                    ))}
                  </Stack>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        ) : (
          <Box sx={{ textAlign: 'center', mt: 6 }}>
            <Typography color="text.secondary">No profile yet. Click "Parse Resume" to extract skills and projects.</Typography>
          </Box>
        )}
      </TabPanel>

      {/* ── Tab 1: Questions ── */}
      <TabPanel value={tab} index={1}>
        {questionsError && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setQuestionsError('')}>{questionsError}</Alert>}

        {/* Round selector + actions */}
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 3, alignItems: 'flex-start' }}>
          <TextField
            select
            label="Select Round"
            size="small"
            value={selectedRoundId}
            onChange={(e) => { setSelectedRoundId(e.target.value); setQuestions([]); }}
            sx={{ minWidth: 220 }}
            disabled={rounds.length === 0}
            helperText={rounds.length === 0 ? 'No rounds found. Create rounds first.' : ''}
          >
            {rounds.map((r) => (
              <MenuItem key={r.id} value={r.id}>{r.name} ({r.roundType})</MenuItem>
            ))}
          </TextField>

          <Button
            variant="outlined"
            onClick={handleLoadQuestions}
            disabled={!selectedRoundId || questionsLoading}
          >
            Load Existing
          </Button>

          <Button
            variant="contained"
            startIcon={generating ? <CircularProgress size={16} color="inherit" /> : <AutoAwesome />}
            onClick={handleGenerateQuestions}
            disabled={!selectedRoundId || generating}
          >
            Generate Questions
          </Button>
        </Stack>

        {questionsLoading || generating ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}><CircularProgress /></Box>
        ) : questions.length === 0 ? (
          <Box sx={{ textAlign: 'center', mt: 6 }}>
            <AutoAwesome sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
            <Typography color="text.secondary">
              Select a round and click "Generate Questions" to create interview questions.
            </Typography>
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
                        <Typography variant="caption" color="text.secondary" sx={{ mb: 1, display: 'block' }}>
                          Key Points
                        </Typography>
                        <List dense disablePadding>
                          {q.keyPoints?.map((kp) => (
                            <ListItem key={kp.id} disablePadding sx={{ py: 0.25 }}>
                              <CheckCircleOutline sx={{ fontSize: 16, color: 'text.disabled', mr: 1 }} />
                              <ListItemText primary={kp.pointText} primaryTypographyProps={{ variant: 'body2' }} />
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
      </TabPanel>
    </AdminLayout>
  );
}

export default AdminCandidateDetailPage;
