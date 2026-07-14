import React, { useEffect, useState } from 'react';
import {
  Box, Typography, Card, CardContent, CardHeader, Chip, Stack,
  Divider, CircularProgress, Alert, Grid, Button, Breadcrumbs,
} from '@mui/material';
import { ArrowBack, PersonSearch } from '@mui/icons-material';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import InterviewerLayout from '../../components/InterviewerLayout';
import { getCandidateProfile } from '../../api/resumeApi';
import { getCandidateWorkflowHint, getCandidateWorkflowLabel } from '../../utils/interviewWorkflow';

function InterviewerCandidateProfilePage() {
  const { candidateId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const candidateName = location.state?.candidateName || 'Candidate';

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const res = await getCandidateProfile(candidateId);
        setProfile(res.data);
      } catch (err) {
        setError(
          err.response?.status === 404
            ? 'Resume has not been parsed yet. Ask the admin to parse the resume first.'
            : 'Failed to load profile.'
        );
      } finally {
        setLoading(false);
      }
    };
    fetchProfile();
  }, [candidateId]);

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

      <Box sx={{ mb: 3 }}>
        <Typography variant="h5">{candidateName}</Typography>
        <Typography variant="body2" color="text.secondary">Resume Profile</Typography>
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
          Workflow: {getCandidateWorkflowLabel('ASSIGNED')} · {getCandidateWorkflowHint('ASSIGNED')}
        </Typography>
      </Box>

      {error && <Alert severity="warning" sx={{ mb: 2 }}>{error}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}><CircularProgress /></Box>
      ) : profile ? (
        <Grid container spacing={3}>
          {/* Status + Skills */}
          <Grid item xs={12} md={5}>
            <Card variant="outlined">
              <CardHeader
                title="Skills & Technologies"
                titleTypographyProps={{ variant: 'subtitle1', fontWeight: 600 }}
                action={
                  <Chip
                    label={profile.status}
                    color={profile.status === 'PARSED' ? 'success' : 'default'}
                    size="small"
                  />
                }
              />
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
                {profile.parsedAt && (
                  <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1.5 }}>
                    Parsed: {new Date(profile.parsedAt).toLocaleString()}
                  </Typography>
                )}
              </CardContent>
            </Card>
          </Grid>

          {/* Projects */}
          <Grid item xs={12} md={7}>
            <Card variant="outlined">
              <CardHeader
                title="Projects"
                titleTypographyProps={{ variant: 'subtitle1', fontWeight: 600 }}
              />
              <Divider />
              <CardContent>
                {profile.projects?.length === 0 ? (
                  <Typography variant="body2" color="text.secondary">No projects found.</Typography>
                ) : (
                  <Stack spacing={2.5}>
                    {profile.projects?.map((project) => (
                      <Box key={project.id}>
                        <Typography fontWeight={600}>{project.projectName}</Typography>
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 0.75 }}>
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
                )}
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      ) : null}
    </InterviewerLayout>
  );
}

export default InterviewerCandidateProfilePage;
