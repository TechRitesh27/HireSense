import React, { useEffect, useState } from 'react';
import {
  Box, Typography, Button, Card, CardContent, CardActions,
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Stack, CircularProgress, Alert, Grid,
  Chip, Divider, Collapse, List, ListItem, ListItemText,
  ListItemAvatar, Avatar, IconButton, Tooltip,
} from '@mui/material';
import {
  Add, Business, PersonAdd, ExpandMore, ExpandLess,
  AdminPanelSettings, Email,
} from '@mui/icons-material';
import { useForm } from 'react-hook-form';
import SuperAdminLayout from '../../components/SuperAdminLayout';
import {
  getAllCompanies, createCompany,
  getAdminsForCompany, createCompanyAdmin,
} from '../../api/superAdminApi';

function SuperAdminCompaniesPage() {
  const [companies, setCompanies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Create company dialog
  const [createCompanyOpen, setCreateCompanyOpen] = useState(false);
  const [submittingCompany, setSubmittingCompany] = useState(false);

  // Create admin dialog
  const [createAdminOpen, setCreateAdminOpen] = useState(false);
  const [selectedCompany, setSelectedCompany] = useState(null);
  const [submittingAdmin, setSubmittingAdmin] = useState(false);

  // Expanded admins per company
  const [expandedAdmins, setExpandedAdmins] = useState({});
  const [adminsMap, setAdminsMap] = useState({});
  const [loadingAdmins, setLoadingAdmins] = useState({});

  const {
    register: regCompany,
    handleSubmit: submitCompany,
    reset: resetCompany,
    formState: { errors: errorsCompany },
  } = useForm();

  const {
    register: regAdmin,
    handleSubmit: submitAdmin,
    reset: resetAdmin,
    formState: { errors: errorsAdmin },
  } = useForm();

  const fetchCompanies = async () => {
    try {
      setLoading(true);
      const res = await getAllCompanies();
      setCompanies(res.data);
    } catch {
      setError('Failed to load companies.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchCompanies(); }, []);

  const handleCreateCompany = async (data) => {
    setSubmittingCompany(true);
    try {
      await createCompany(data);
      resetCompany();
      setCreateCompanyOpen(false);
      setSuccess('Company created successfully.');
      fetchCompanies();
    } catch (err) {
      setError(err.response?.data || 'Failed to create company.');
    } finally {
      setSubmittingCompany(false);
    }
  };

  const handleCreateAdmin = async (data) => {
    if (!selectedCompany) return;
    setSubmittingAdmin(true);
    try {
      await createCompanyAdmin(selectedCompany.id, data);
      resetAdmin();
      setCreateAdminOpen(false);
      setSuccess(`Admin created for ${selectedCompany.name}.`);
      // Refresh admins for this company if expanded
      if (expandedAdmins[selectedCompany.id]) {
        loadAdmins(selectedCompany.id, true);
      }
    } catch (err) {
      setError(err.response?.data || 'Failed to create admin.');
    } finally {
      setSubmittingAdmin(false);
    }
  };

  const loadAdmins = async (companyId, forceRefresh = false) => {
    if (adminsMap[companyId] && !forceRefresh) return;
    setLoadingAdmins((prev) => ({ ...prev, [companyId]: true }));
    try {
      const res = await getAdminsForCompany(companyId);
      setAdminsMap((prev) => ({ ...prev, [companyId]: res.data }));
    } catch {
      setAdminsMap((prev) => ({ ...prev, [companyId]: [] }));
    } finally {
      setLoadingAdmins((prev) => ({ ...prev, [companyId]: false }));
    }
  };

  const toggleAdmins = (companyId) => {
    const nowExpanded = !expandedAdmins[companyId];
    setExpandedAdmins((prev) => ({ ...prev, [companyId]: nowExpanded }));
    if (nowExpanded) loadAdmins(companyId);
  };

  return (
    <SuperAdminLayout>
      <Box sx={{ mb: 3, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Box>
          <Typography variant="h5">Companies</Typography>
          <Typography variant="body2" color="text.secondary">
            Manage all companies and their admins
          </Typography>
        </Box>
        <Button
          variant="contained"
          color="error"
          startIcon={<Add />}
          onClick={() => setCreateCompanyOpen(true)}
        >
          New Company
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess('')}>{success}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}><CircularProgress /></Box>
      ) : companies.length === 0 ? (
        <Box sx={{ textAlign: 'center', mt: 8 }}>
          <Business sx={{ fontSize: 56, color: 'text.disabled', mb: 1 }} />
          <Typography color="text.secondary">No companies yet. Create one to get started.</Typography>
        </Box>
      ) : (
        <Grid container spacing={2}>
          {companies.map((company) => {
            const admins = adminsMap[company.id] || [];
            const isExpanded = expandedAdmins[company.id];

            return (
              <Grid item xs={12} md={6} key={company.id}>
                <Card variant="outlined">
                  <CardContent>
                    <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 2, mb: 1 }}>
                      <Avatar sx={{ bgcolor: 'error.light', color: 'error.dark' }}>
                        <Business />
                      </Avatar>
                      <Box sx={{ flex: 1 }}>
                        <Typography variant="h6" fontWeight={600} sx={{ fontSize: '1rem' }}>
                          {company.name}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">{company.email}</Typography>
                        <Typography variant="caption" color="text.secondary">{company.address}</Typography>
                      </Box>
                    </Box>

                    <Divider sx={{ my: 1.5 }} />

                    {/* Admins section */}
                    <Box
                      sx={{ display: 'flex', alignItems: 'center', cursor: 'pointer', userSelect: 'none' }}
                      onClick={() => toggleAdmins(company.id)}
                    >
                      <AdminPanelSettings fontSize="small" color="action" sx={{ mr: 1 }} />
                      <Typography variant="body2" fontWeight={500}>
                        Company Admins
                        {adminsMap[company.id] !== undefined && (
                          <Chip
                            label={admins.length}
                            size="small"
                            sx={{ ml: 1, height: 18, fontSize: '0.7rem' }}
                          />
                        )}
                      </Typography>
                      <Box sx={{ flex: 1 }} />
                      {isExpanded ? <ExpandLess fontSize="small" /> : <ExpandMore fontSize="small" />}
                    </Box>

                    <Collapse in={isExpanded}>
                      {loadingAdmins[company.id] ? (
                        <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
                          <CircularProgress size={20} />
                        </Box>
                      ) : admins.length === 0 ? (
                        <Typography variant="body2" color="text.secondary" sx={{ pl: 1, pt: 1 }}>
                          No admins assigned yet.
                        </Typography>
                      ) : (
                        <List dense disablePadding sx={{ mt: 0.5 }}>
                          {admins.map((admin) => (
                            <ListItem key={admin.id} disablePadding sx={{ py: 0.25 }}>
                              <ListItemAvatar sx={{ minWidth: 36 }}>
                                <Avatar sx={{ width: 28, height: 28, fontSize: 13, bgcolor: 'primary.light' }}>
                                  {admin.fullName[0]}
                                </Avatar>
                              </ListItemAvatar>
                              <ListItemText
                                primary={admin.fullName}
                                secondary={admin.email}
                                primaryTypographyProps={{ variant: 'body2', fontWeight: 500 }}
                                secondaryTypographyProps={{ variant: 'caption' }}
                              />
                            </ListItem>
                          ))}
                        </List>
                      )}
                    </Collapse>
                  </CardContent>

                  <CardActions sx={{ px: 2, pb: 2 }}>
                    <Button
                      size="small"
                      startIcon={<PersonAdd />}
                      onClick={() => { setSelectedCompany(company); setCreateAdminOpen(true); }}
                    >
                      Add Admin
                    </Button>
                  </CardActions>
                </Card>
              </Grid>
            );
          })}
        </Grid>
      )}

      {/* Create Company Dialog */}
      <Dialog open={createCompanyOpen} onClose={() => { setCreateCompanyOpen(false); resetCompany(); }} maxWidth="xs" fullWidth>
        <DialogTitle>Create Company</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Company Name"
              fullWidth
              size="small"
              {...regCompany('name', { required: 'Name is required' })}
              error={Boolean(errorsCompany.name)}
              helperText={errorsCompany.name?.message}
            />
            <TextField
              label="Address"
              fullWidth
              size="small"
              {...regCompany('address', { required: 'Address is required' })}
              error={Boolean(errorsCompany.address)}
              helperText={errorsCompany.address?.message}
            />
            <TextField
              label="Email"
              type="email"
              fullWidth
              size="small"
              {...regCompany('email', { required: 'Email is required' })}
              error={Boolean(errorsCompany.email)}
              helperText={errorsCompany.email?.message}
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => { setCreateCompanyOpen(false); resetCompany(); }}>Cancel</Button>
          <Button variant="contained" color="error" onClick={submitCompany(handleCreateCompany)} disabled={submittingCompany}>
            {submittingCompany ? <CircularProgress size={18} /> : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Create Admin Dialog */}
      <Dialog open={createAdminOpen} onClose={() => { setCreateAdminOpen(false); resetAdmin(); }} maxWidth="xs" fullWidth>
        <DialogTitle>Add Admin to {selectedCompany?.name}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Full Name"
              fullWidth
              size="small"
              {...regAdmin('fullName', { required: 'Full name is required' })}
              error={Boolean(errorsAdmin.fullName)}
              helperText={errorsAdmin.fullName?.message}
            />
            <TextField
              label="Email"
              type="email"
              fullWidth
              size="small"
              {...regAdmin('email', { required: 'Email is required' })}
              error={Boolean(errorsAdmin.email)}
              helperText={errorsAdmin.email?.message}
            />
            <TextField
              label="Password"
              type="password"
              fullWidth
              size="small"
              {...regAdmin('password', { required: 'Password is required', minLength: { value: 6, message: 'Min 6 characters' } })}
              error={Boolean(errorsAdmin.password)}
              helperText={errorsAdmin.password?.message}
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => { setCreateAdminOpen(false); resetAdmin(); }}>Cancel</Button>
          <Button variant="contained" onClick={submitAdmin(handleCreateAdmin)} disabled={submittingAdmin}>
            {submittingAdmin ? <CircularProgress size={18} /> : 'Create Admin'}
          </Button>
        </DialogActions>
      </Dialog>
    </SuperAdminLayout>
  );
}

export default SuperAdminCompaniesPage;
