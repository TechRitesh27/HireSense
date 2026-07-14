import React, { useEffect, useState } from 'react';
import {
  Box, Typography, Button, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Paper, Chip, Dialog,
  DialogTitle, DialogContent, DialogActions, TextField, Stack,
  CircularProgress, Alert, IconButton, Tooltip, Breadcrumbs,
  Link, Select, MenuItem, InputLabel, FormControl, OutlinedInput,
  Avatar, ListItemAvatar, ListItemText,
} from '@mui/material';
import {
  Add, Upload, PersonAddAlt, MeetingRoom, EmojiEvents, OpenInNew, Link as LinkIcon,
} from '@mui/icons-material';
import { useForm } from 'react-hook-form';
import { useParams, useNavigate } from 'react-router-dom';
import AdminLayout from '../../components/AdminLayout';
import { getCandidates, createCandidate, uploadCandidatesExcel, importCandidatesFromUrl } from '../../api/candidateApi';
import { assignInterviewers } from '../../api/assignmentApi';
import { getInterviewers } from '../../api/userApi';
import { getRounds } from '../../api/roundApi';
import { getCandidateWorkflowColor, getCandidateWorkflowHint, getCandidateWorkflowLabel } from '../../utils/interviewWorkflow';

const STATUS_COLORS = {
  IMPORTED: 'default',
  ASSIGNED: 'primary',
  INTERVIEW_IN_PROGRESS: 'warning',
  INTERVIEW_COMPLETED: 'info',
  SELECTED: 'success',
  REJECTED: 'error',
};

function AdminCandidatesPage() {
  const { hiringDriveId } = useParams();
  const navigate = useNavigate();

  const [candidates, setCandidates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Dialogs
  const [addOpen, setAddOpen] = useState(false);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [assignOpen, setAssignOpen] = useState(false);
  const [selectedCandidate, setSelectedCandidate] = useState(null);

  const [submitting, setSubmitting] = useState(false);
  const [uploadFile, setUploadFile] = useState(null);

  // URL import
  const [importUrlOpen, setImportUrlOpen] = useState(false);
  const [importUrl, setImportUrl] = useState('');
  const [importUrlError, setImportUrlError] = useState('');

  // Interviewer list for assign dialog
  const [interviewers, setInterviewers] = useState([]);
  const [interviewersLoading, setInterviewersLoading] = useState(false);
  const [selectedInterviewerIds, setSelectedInterviewerIds] = useState([]);

  // Rounds list for assign dialog
  const [rounds, setRounds] = useState([]);
  const [selectedRoundId, setSelectedRoundId] = useState('');

  const { register, handleSubmit, reset, formState: { errors } } = useForm();

  const fetchCandidates = async () => {
    try {
      setLoading(true);
      const res = await getCandidates(hiringDriveId);
      setCandidates(res.data);
    } catch {
      setError('Failed to load candidates.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchCandidates(); }, [hiringDriveId]);

  // Load interviewers when assign dialog opens
  const openAssignDialog = async (candidate) => {
    setSelectedCandidate(candidate);
    setSelectedInterviewerIds([]);
    setSelectedRoundId('');
    setAssignOpen(true);

    setInterviewersLoading(true);
    try {
      const [ivRes, roundRes] = await Promise.all([
        interviewers.length === 0 ? getInterviewers() : Promise.resolve({ data: interviewers }),
        getRounds(hiringDriveId),
      ]);
      setInterviewers(ivRes.data);
      setRounds(roundRes.data);
      if (roundRes.data.length > 0) setSelectedRoundId(roundRes.data[0].id);
    } catch {
      setError('Failed to load interviewers or rounds.');
    } finally {
      setInterviewersLoading(false);
    }
  };

  const handleAddCandidate = async (data) => {
    setSubmitting(true);
    try {
      await createCandidate(hiringDriveId, { ...data, graduationYear: parseInt(data.graduationYear) });
      reset();
      setAddOpen(false);
      setSuccess('Candidate added successfully.');
      fetchCandidates();
    } catch (err) {
      setError(err.response?.data || 'Failed to add candidate.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpload = async () => {
    if (!uploadFile) return;
    setSubmitting(true);
    try {
      const res = await uploadCandidatesExcel(hiringDriveId, uploadFile);
      const { successCount, failedCount } = res.data;
      setSuccess(`Upload complete — ${successCount} added, ${failedCount} failed.`);
      setUploadOpen(false);
      setUploadFile(null);
      fetchCandidates();
    } catch (err) {
      setError(err.response?.data || 'Upload failed.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleImportFromUrl = async () => {
    if (!importUrl.trim()) {
      setImportUrlError('Please enter a URL.');
      return;
    }
    setSubmitting(true);
    setImportUrlError('');
    try {
      const res = await importCandidatesFromUrl(hiringDriveId, importUrl.trim());
      const { successCount, failedCount } = res.data;
      setSuccess(`Import complete — ${successCount} added, ${failedCount} failed.`);
      setImportUrlOpen(false);
      setImportUrl('');
      fetchCandidates();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'Import failed. Make sure the sheet is publicly accessible.';
      setImportUrlError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleAssign = async () => {
    if (!selectedInterviewerIds.length || !selectedCandidate || !selectedRoundId) return;
    setSubmitting(true);
    try {
      await assignInterviewers(hiringDriveId, selectedCandidate.id, selectedInterviewerIds, selectedRoundId);
      setSuccess('Interviewer(s) assigned successfully.');
      setAssignOpen(false);
      setSelectedInterviewerIds([]);
      setSelectedRoundId('');
      fetchCandidates();
    } catch (err) {
      setError(err.response?.data || 'Assignment failed.');
    } finally {
      setSubmitting(false);
    }
  };

  const closeAssignDialog = () => {
    setAssignOpen(false);
    setSelectedInterviewerIds([]);
    setSelectedRoundId('');
  };

  return (
    <AdminLayout>
      {/* Breadcrumb */}
      <Breadcrumbs sx={{ mb: 2 }}>
        <Link component="button" variant="body2" underline="hover" onClick={() => navigate('/admin/hiring-drives')}>
          Hiring Drives
        </Link>
        <Typography variant="body2" color="text.primary">Candidates</Typography>
      </Breadcrumbs>

      <Box sx={{ mb: 3, display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 1 }}>
        <Typography variant="h5">Candidates</Typography>
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          <Button variant="outlined" startIcon={<MeetingRoom />} onClick={() => navigate(`/admin/hiring-drives/${hiringDriveId}/rounds`)}>
            Manage Rounds
          </Button>
          <Button variant="outlined" startIcon={<EmojiEvents />} onClick={() => navigate(`/admin/hiring-drives/${hiringDriveId}/rankings`)}>
            Rankings
          </Button>
          <Button variant="outlined" startIcon={<Upload />} onClick={() => setUploadOpen(true)}>
            Upload Excel
          </Button>
          <Button variant="outlined" startIcon={<LinkIcon />} onClick={() => { setImportUrl(''); setImportUrlError(''); setImportUrlOpen(true); }}>
            Import from URL
          </Button>
          <Button variant="contained" startIcon={<Add />} onClick={() => setAddOpen(true)}>
            Add Candidate
          </Button>
        </Box>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess('')}>{success}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}><CircularProgress /></Box>
      ) : (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow sx={{ bgcolor: 'grey.50' }}>
                <TableCell><b>Name</b></TableCell>
                <TableCell><b>Email</b></TableCell>
                <TableCell><b>College</b></TableCell>
                <TableCell><b>Branch</b></TableCell>
                <TableCell><b>Status</b></TableCell>
                <TableCell align="center"><b>Actions</b></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {candidates.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    No candidates yet
                  </TableCell>
                </TableRow>
              ) : (
                candidates.map((c) => (
                  <TableRow key={c.id} hover>
                    <TableCell>{c.fullName}</TableCell>
                    <TableCell>{c.email}</TableCell>
                    <TableCell>{c.collegeName}</TableCell>
                    <TableCell>{c.branch}</TableCell>
                    <TableCell>
                      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                        <Chip label={c.status} color={STATUS_COLORS[c.status] || 'default'} size="small" />
                        <Chip label={getCandidateWorkflowLabel(c.status)} color={getCandidateWorkflowColor(c.status)} size="small" variant="outlined" />
                      </Stack>
                      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
                        {getCandidateWorkflowHint(c.status)}
                      </Typography>
                    </TableCell>
                    <TableCell align="center">
                      <Tooltip title="Assign Interviewer">
                        <IconButton size="small" onClick={() => openAssignDialog(c)}>
                          <PersonAddAlt fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Open candidate workflow details">
                        <IconButton
                          size="small"
                          onClick={() => navigate(`/admin/candidates/${c.id}`, {
                            state: { candidateName: c.fullName, hiringDriveId }
                          })}
                        >
                          <OpenInNew fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Add Candidate Dialog */}
      <Dialog open={addOpen} onClose={() => { setAddOpen(false); reset(); }} maxWidth="sm" fullWidth>
        <DialogTitle>Add Candidate</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {[
              { name: 'fullName', label: 'Full Name' },
              { name: 'email', label: 'Email', type: 'email' },
              { name: 'phone', label: 'Phone' },
              { name: 'collegeName', label: 'College Name' },
              { name: 'degree', label: 'Degree' },
              { name: 'branch', label: 'Branch' },
              { name: 'graduationYear', label: 'Graduation Year', type: 'number' },
              { name: 'resumeUrl', label: 'Resume URL' },
            ].map((field) => (
              <TextField
                key={field.name}
                label={field.label}
                type={field.type || 'text'}
                fullWidth
                size="small"
                {...register(field.name, { required: `${field.label} is required` })}
                error={Boolean(errors[field.name])}
                helperText={errors[field.name]?.message}
              />
            ))}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => { setAddOpen(false); reset(); }}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit(handleAddCandidate)} disabled={submitting}>
            {submitting ? <CircularProgress size={18} /> : 'Add'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Upload Excel Dialog */}
      <Dialog open={uploadOpen} onClose={() => setUploadOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Upload Candidates from Excel</DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 1 }}>
            <input
              type="file"
              accept=".xlsx,.xls"
              onChange={(e) => setUploadFile(e.target.files[0])}
              style={{ width: '100%' }}
            />
            {uploadFile && (
              <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                Selected: {uploadFile.name}
              </Typography>
            )}
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => { setUploadOpen(false); setUploadFile(null); }}>Cancel</Button>
          <Button variant="contained" onClick={handleUpload} disabled={!uploadFile || submitting}>
            {submitting ? <CircularProgress size={18} /> : 'Upload'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Import from URL Dialog */}
      <Dialog open={importUrlOpen} onClose={() => setImportUrlOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Import Candidates from URL</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Paste a Google Sheets link or a direct link to an Excel file (.xlsx).
            For Google Sheets, make sure the sheet is set to <b>Anyone with the link → Viewer</b>.
          </Typography>
          <TextField
            label="Spreadsheet URL"
            fullWidth
            size="small"
            value={importUrl}
            onChange={(e) => { setImportUrl(e.target.value); setImportUrlError(''); }}
            placeholder="https://docs.google.com/spreadsheets/d/..."
            error={Boolean(importUrlError)}
            helperText={importUrlError}
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setImportUrlOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            startIcon={<LinkIcon />}
            onClick={handleImportFromUrl}
            disabled={!importUrl.trim() || submitting}
          >
            {submitting ? <CircularProgress size={18} /> : 'Import'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Assign Interviewer Dialog */}
      <Dialog open={assignOpen} onClose={closeAssignDialog} maxWidth="xs" fullWidth>
        <DialogTitle>Assign Interviewer</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Candidate: <b>{selectedCandidate?.fullName}</b>
          </Typography>

          {interviewersLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
              <CircularProgress size={24} />
            </Box>
          ) : (
            <Stack spacing={2}>
              {/* Round selector */}
              {rounds.length === 0 ? (
                <Alert severity="warning">
                  No rounds configured for this hiring drive. Add a round first.
                </Alert>
              ) : (
                <FormControl fullWidth size="small">
                  <InputLabel>Interview Round</InputLabel>
                  <Select
                    value={selectedRoundId}
                    onChange={(e) => setSelectedRoundId(e.target.value)}
                    input={<OutlinedInput label="Interview Round" />}
                  >
                    {rounds.map((r) => (
                      <MenuItem key={r.id} value={r.id}>
                        {r.name} ({r.roundType})
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              )}

              {/* Interviewer selector */}
              {interviewers.length === 0 ? (
                <Alert severity="warning">
                  No interviewers found in this company. Add interviewers first.
                </Alert>
              ) : (
                <FormControl fullWidth size="small">
                  <InputLabel>Select Interviewer(s)</InputLabel>
                  <Select
                    multiple
                    value={selectedInterviewerIds}
                    onChange={(e) => setSelectedInterviewerIds(e.target.value)}
                    input={<OutlinedInput label="Select Interviewer(s)" />}
                    renderValue={(selected) => (
                      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                        {selected.map((id) => {
                          const iv = interviewers.find((i) => i.id === id);
                          return <Chip key={id} label={iv?.fullName || id} size="small" />;
                        })}
                      </Box>
                    )}
                  >
                    {interviewers.map((iv) => (
                      <MenuItem key={iv.id} value={iv.id}>
                        <ListItemAvatar sx={{ minWidth: 36 }}>
                          <Avatar sx={{ width: 28, height: 28, fontSize: 13, bgcolor: 'secondary.light' }}>
                            {iv.fullName[0]}
                          </Avatar>
                        </ListItemAvatar>
                        <ListItemText
                          primary={iv.fullName}
                          secondary={iv.email}
                          primaryTypographyProps={{ variant: 'body2' }}
                          secondaryTypographyProps={{ variant: 'caption' }}
                        />
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              )}
            </Stack>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={closeAssignDialog}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleAssign}
            disabled={!selectedInterviewerIds.length || !selectedRoundId || submitting}
          >
            {submitting ? <CircularProgress size={18} /> : `Assign (${selectedInterviewerIds.length})`}
          </Button>
        </DialogActions>
      </Dialog>
    </AdminLayout>
  );
}

export default AdminCandidatesPage;
