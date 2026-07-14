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
import { getCandidateWorkflowHint, getCandidateWorkflowLabel } from '../../utils/interviewWorkflow';

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

  useEffect(() => {
    fetchProfile();
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
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
        Workflow: {getCandidateWorkflowLabel('IMPORTED')} · {getCandidateWorkflowHint('IMPORTED')}
      </Typography>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 1, borderBottom: 1, borderColor: 'divider' }}>
        <Tab label="Resume Profile" />
        <Tab label="Session Questions" />
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
                    {(() => {
                      const raw = profile.skills;
                      let list = [];
                      if (!raw) list = [];
                      else if (Array.isArray(raw)) list = raw;
                      else if (typeof raw === 'string') {
                        try {
                          const parsed = JSON.parse(raw);
                          if (Array.isArray(parsed)) list = parsed;
                          else if (parsed && typeof parsed === 'object') {
                            if (Array.isArray(parsed.skills)) list = parsed.skills;
                            else list = Object.values(parsed).flatMap(v => (typeof v === 'string' ? v.split(',') : []));
                          } else {
                            list = raw.split(',');
                          }
                        } catch (e) {
                          list = raw.split(',');
                        }
                      } else if (typeof raw === 'object') {
                        if (Array.isArray(raw.skills)) list = raw.skills;
                        else list = Object.values(raw).flatMap(v => (typeof v === 'string' ? v.split(',') : []));
                      }

                      return list.length === 0 ? (
                        <Typography variant="body2" color="text.secondary">No skills found.</Typography>
                      ) : (
                        list.map((skill) => (
                          <Chip key={String(skill)} label={String(skill).trim()} size="small" variant="outlined" color="primary" />
                        ))
                      );
                    })()}
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

      {/* ── Tab 1: Session Questions ── */}
      <TabPanel value={tab} index={1}>
        <Alert severity="info" sx={{ mb: 2 }}>
          Questions are generated when the interviewer starts an interview session for this candidate.
        </Alert>
        <Typography variant="body2" color="text.secondary">
          The interviewer selects the difficulty level and number of questions at the moment the session begins,
          and the questions appear inside the active interview session view.
        </Typography>
      </TabPanel>
    </AdminLayout>
  );
}

export default AdminCandidateDetailPage;
