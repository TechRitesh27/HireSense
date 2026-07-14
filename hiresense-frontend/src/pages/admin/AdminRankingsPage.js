import React, { useEffect, useState } from 'react';
import {
  Box, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, Chip, CircularProgress, Alert,
  Breadcrumbs, Link, Avatar, MenuItem, TextField,
} from '@mui/material';
import { EmojiEvents } from '@mui/icons-material';
import { useParams, useNavigate } from 'react-router-dom';
import AdminLayout from '../../components/AdminLayout';
import { getRankings } from '../../api/rankingApi';
import { getRounds } from '../../api/roundApi';
import { getCandidateWorkflowLabel } from '../../utils/interviewWorkflow';

const MEDAL_COLORS = ['#FFD700', '#C0C0C0', '#CD7F32'];

function AdminRankingsPage() {
  const { hiringDriveId } = useParams();
  const navigate = useNavigate();

  const [rounds, setRounds] = useState([]);
  const [selectedRound, setSelectedRound] = useState('');
  const [rankings, setRankings] = useState([]);
  const [loadingRounds, setLoadingRounds] = useState(true);
  const [loadingRankings, setLoadingRankings] = useState(false);
  const [error, setError] = useState('');

  // Load rounds for this hiring drive
  useEffect(() => {
    const fetchRounds = async () => {
      try {
        setLoadingRounds(true);
        const res = await getRounds(hiringDriveId);
        setRounds(res.data);
        if (res.data.length > 0) {
          setSelectedRound(res.data[0].id);
        }
      } catch {
        setError('Failed to load rounds.');
      } finally {
        setLoadingRounds(false);
      }
    };
    fetchRounds();
  }, [hiringDriveId]);

  // Load rankings whenever the selected round changes
  useEffect(() => {
    if (!selectedRound) return;
    const fetchRankings = async () => {
      try {
        setLoadingRankings(true);
        setError('');
        const res = await getRankings(hiringDriveId, selectedRound);
        setRankings(res.data);
      } catch {
        setError('Failed to load rankings.');
        setRankings([]);
      } finally {
        setLoadingRankings(false);
      }
    };
    fetchRankings();
  }, [hiringDriveId, selectedRound]);

  const loading = loadingRounds || loadingRankings;

  return (
    <AdminLayout>
      <Breadcrumbs sx={{ mb: 2 }}>
        <Link component="button" variant="body2" underline="hover" onClick={() => navigate('/admin/hiring-drives')}>
          Hiring Drives
        </Link>
        <Typography variant="body2" color="text.primary">Rankings</Typography>
      </Breadcrumbs>

      <Box sx={{ mb: 3, display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <EmojiEvents color="warning" />
          <Typography variant="h5">Candidate Rankings</Typography>
        </Box>

        {rounds.length > 0 && (
          <TextField
            select
            label="Interview Round"
            size="small"
            value={selectedRound}
            onChange={(e) => setSelectedRound(e.target.value)}
            sx={{ minWidth: 220 }}
          >
            {rounds.map((r) => (
              <MenuItem key={r.id} value={r.id}>
                {r.name} ({r.roundType})
              </MenuItem>
            ))}
          </TextField>
        )}
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}><CircularProgress /></Box>
      ) : rounds.length === 0 ? (
        <Box sx={{ textAlign: 'center', mt: 8 }}>
          <Typography color="text.secondary">No rounds configured for this hiring drive.</Typography>
        </Box>
      ) : rankings.length === 0 ? (
        <Box sx={{ textAlign: 'center', mt: 8 }}>
          <EmojiEvents sx={{ fontSize: 56, color: 'text.disabled', mb: 1 }} />
          <Typography color="text.secondary">No results yet for this round.</Typography>
          <Typography variant="body2" color="text.secondary">
            Rankings appear after interview sessions are submitted.
          </Typography>
        </Box>
      ) : (
        <TableContainer component={Paper} variant="outlined">
          <Table>
            <TableHead>
              <TableRow sx={{ bgcolor: 'grey.50' }}>
                <TableCell width={80}><b>Rank</b></TableCell>
                <TableCell><b>Candidate</b></TableCell>
                <TableCell><b>Email</b></TableCell>
                <TableCell align="center"><b>Round Score</b></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rankings.map((r) => (
                <TableRow
                  key={r.candidateId}
                  hover
                  sx={{ bgcolor: r.rank <= 3 ? ['#FFFDE7', '#F5F5F5', '#FFF3E0'][r.rank - 1] : 'inherit' }}
                >
                  <TableCell>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      {r.rank <= 3 ? (
                        <Avatar
                          sx={{
                            width: 28, height: 28,
                            bgcolor: MEDAL_COLORS[r.rank - 1],
                            fontSize: 13, fontWeight: 700, color: '#333',
                          }}
                        >
                          {r.rank}
                        </Avatar>
                      ) : (
                        <Typography fontWeight={500} color="text.secondary">#{r.rank}</Typography>
                      )}
                    </Box>
                  </TableCell>
                  <TableCell>
                    <Typography fontWeight={r.rank <= 3 ? 600 : 400}>{r.fullName}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      Workflow stage: {getCandidateWorkflowLabel('INTERVIEW_COMPLETED')}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" color="text.secondary">{r.email}</Typography>
                  </TableCell>
                  <TableCell align="center">
                    <Chip
                      label={r.roundScore}
                      color={r.rank === 1 ? 'success' : 'default'}
                      variant={r.rank === 1 ? 'filled' : 'outlined'}
                      sx={{ fontWeight: 700, minWidth: 56 }}
                    />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </AdminLayout>
  );
}

export default AdminRankingsPage;
