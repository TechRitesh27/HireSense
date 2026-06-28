import React, { useEffect, useState } from 'react';
import {
  Box, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, Chip, CircularProgress, Alert,
  Breadcrumbs, Link, Avatar,
} from '@mui/material';
import { EmojiEvents } from '@mui/icons-material';
import { useParams, useNavigate } from 'react-router-dom';
import AdminLayout from '../../components/AdminLayout';
import { getRankings } from '../../api/rankingApi';

const MEDAL_COLORS = ['#FFD700', '#C0C0C0', '#CD7F32'];

function AdminRankingsPage() {
  const { hiringDriveId } = useParams();
  const navigate = useNavigate();

  const [rankings, setRankings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchRankings = async () => {
      try {
        const res = await getRankings(hiringDriveId);
        setRankings(res.data);
      } catch {
        setError('Failed to load rankings.');
      } finally {
        setLoading(false);
      }
    };
    fetchRankings();
  }, [hiringDriveId]);

  return (
    <AdminLayout>
      <Breadcrumbs sx={{ mb: 2 }}>
        <Link component="button" variant="body2" underline="hover" onClick={() => navigate('/admin/hiring-drives')}>
          Hiring Drives
        </Link>
        <Typography variant="body2" color="text.primary">Rankings</Typography>
      </Breadcrumbs>

      <Box sx={{ mb: 3, display: 'flex', alignItems: 'center', gap: 1.5 }}>
        <EmojiEvents color="warning" />
        <Typography variant="h5">Candidate Rankings</Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}><CircularProgress /></Box>
      ) : rankings.length === 0 ? (
        <Box sx={{ textAlign: 'center', mt: 8 }}>
          <EmojiEvents sx={{ fontSize: 56, color: 'text.disabled', mb: 1 }} />
          <Typography color="text.secondary">No results yet.</Typography>
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
                <TableCell align="center"><b>Total Score</b></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rankings.map((r) => (
                <TableRow
                  key={r.candidateId}
                  hover
                  sx={{ bgcolor: r.rank <= 3 ? `${['#FFFDE7', '#F5F5F5', '#FFF3E0'][r.rank - 1]}` : 'inherit' }}
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
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" color="text.secondary">{r.email}</Typography>
                  </TableCell>
                  <TableCell align="center">
                    <Chip
                      label={r.totalScore}
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
