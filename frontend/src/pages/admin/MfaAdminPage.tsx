import { useState } from 'react';
import {
  Alert, Box, Button, Chip, CircularProgress, Dialog, DialogActions,
  DialogContent, DialogTitle, InputAdornment, Pagination, Paper,
  Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import LockOpenIcon from '@mui/icons-material/LockOpen';
import RestartAltIcon from '@mui/icons-material/RestartAlt';
import SecurityIcon from '@mui/icons-material/Security';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import { useListUserMfaStatusQuery, useAdminDisableMfaMutation, useAdminResetMfaMutation } from '../../store/api/adminApiSlice';
import type { UserMfaStatus } from '../../types';

const PAGE_SIZE = 15;

export default function MfaAdminPage() {
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [confirm, setConfirm] = useState<{ user: UserMfaStatus; action: 'disable' | 'reset' } | null>(null);

  const { data, isLoading } = useListUserMfaStatusQuery({
    search: search || undefined,
    page,
    size: PAGE_SIZE,
  });

  const [disableMfa, { isLoading: disabling }] = useAdminDisableMfaMutation();
  const [resetMfa, { isLoading: resetting }] = useAdminResetMfaMutation();

  const handleAction = async () => {
    if (!confirm) return;
    try {
      if (confirm.action === 'disable') await disableMfa(confirm.user.id).unwrap();
      else await resetMfa(confirm.user.id).unwrap();
      setConfirm(null);
    } catch {
      // errors shown inline via RTK query
    }
  };

  const rows = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <Box>
      {/* Page header */}
      <div className="tw-flex tw-items-center tw-gap-3 tw-mb-6">
        <div className="tw-w-10 tw-h-10 tw-rounded-xl tw-bg-blue-600 tw-flex tw-items-center tw-justify-center">
          <SecurityIcon sx={{ color: 'white', fontSize: 22 }} />
        </div>
        <div>
          <Typography variant="h5" fontWeight={700} color="text.primary">
            MFA Management
          </Typography>
          <Typography variant="body2" color="text.secondary">
            View and control two-factor authentication for all users
          </Typography>
        </div>
      </div>

      {/* Stats bar */}
      {data && (
        <div className="tw-grid tw-grid-cols-2 sm:tw-grid-cols-4 tw-gap-4 tw-mb-6">
          <StatCard label="Total Users" value={data.totalElements} color="tw-border-blue-500" />
          <StatCard
            label="MFA Enabled"
            value={rows.filter((u) => u.mfaEnabled).length}
            note="on this page"
            color="tw-border-green-500"
          />
          <StatCard
            label="MFA Configured"
            value={rows.filter((u) => u.mfaConfigured).length}
            note="on this page"
            color="tw-border-yellow-500"
          />
          <StatCard
            label="No MFA"
            value={rows.filter((u) => !u.mfaEnabled && !u.mfaConfigured).length}
            note="on this page"
            color="tw-border-gray-400"
          />
        </div>
      )}

      {/* Search + table */}
      <Paper elevation={0} className="tw-border tw-border-gray-200 tw-rounded-xl tw-overflow-hidden">
        <div className="tw-px-4 tw-py-3 tw-border-b tw-border-gray-100 tw-bg-gray-50">
          <TextField
            size="small"
            placeholder="Search by username or name…"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0); }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" sx={{ color: 'text.disabled' }} />
                </InputAdornment>
              ),
            }}
            sx={{ width: 300 }}
          />
        </div>

        {isLoading ? (
          <Box className="tw-flex tw-justify-center tw-py-16">
            <CircularProgress />
          </Box>
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow sx={{ bgcolor: 'grey.50' }}>
                <TableCell sx={{ fontWeight: 700 }}>Username</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Full Name</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Email</TableCell>
                <TableCell sx={{ fontWeight: 700 }} align="center">MFA Enabled</TableCell>
                <TableCell sx={{ fontWeight: 700 }} align="center">Secret Configured</TableCell>
                <TableCell sx={{ fontWeight: 700 }} align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 6, color: 'text.secondary' }}>
                    No users found
                  </TableCell>
                </TableRow>
              ) : rows.map((u) => (
                <TableRow key={u.id} hover>
                  <TableCell>
                    <Typography variant="body2" fontWeight={600}>{u.username}</Typography>
                  </TableCell>
                  <TableCell>{u.fullName}</TableCell>
                  <TableCell sx={{ color: 'text.secondary', fontSize: 12 }}>{u.email}</TableCell>
                  <TableCell align="center">
                    {u.mfaEnabled
                      ? <Chip icon={<CheckCircleIcon />} label="Enabled" size="small" color="success" variant="outlined" />
                      : <Chip icon={<CancelIcon />} label="Disabled" size="small" color="default" variant="outlined" />}
                  </TableCell>
                  <TableCell align="center">
                    {u.mfaConfigured
                      ? <Chip label="Has Secret" size="small" color="warning" variant="outlined" />
                      : <Chip label="Not Set" size="small" variant="outlined" />}
                  </TableCell>
                  <TableCell align="right">
                    <div className="tw-flex tw-gap-2 tw-justify-end">
                      {u.mfaEnabled && (
                        <Button
                          size="small"
                          variant="outlined"
                          color="warning"
                          startIcon={<LockOpenIcon />}
                          onClick={() => setConfirm({ user: u, action: 'disable' })}
                        >
                          Disable
                        </Button>
                      )}
                      {u.mfaConfigured && (
                        <Button
                          size="small"
                          variant="outlined"
                          color="error"
                          startIcon={<RestartAltIcon />}
                          onClick={() => setConfirm({ user: u, action: 'reset' })}
                        >
                          Reset
                        </Button>
                      )}
                      {!u.mfaEnabled && !u.mfaConfigured && (
                        <Typography variant="caption" color="text.disabled" sx={{ py: 0.5 }}>
                          No MFA
                        </Typography>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        {totalPages > 1 && (
          <Box className="tw-flex tw-justify-center tw-py-3 tw-border-t tw-border-gray-100">
            <Pagination
              count={totalPages}
              page={page + 1}
              onChange={(_, v) => setPage(v - 1)}
              size="small"
              color="primary"
            />
          </Box>
        )}
      </Paper>

      {/* Confirm dialog */}
      <Dialog open={!!confirm} onClose={() => setConfirm(null)} maxWidth="xs" fullWidth>
        <DialogTitle>
          {confirm?.action === 'reset' ? 'Reset MFA' : 'Disable MFA'}
        </DialogTitle>
        <DialogContent>
          {confirm?.action === 'reset' ? (
            <Alert severity="error" sx={{ mb: 1 }}>
              This will <strong>completely remove</strong> the MFA secret and disable MFA for{' '}
              <strong>{confirm?.user.username}</strong>. They will need to set up MFA again from scratch.
            </Alert>
          ) : (
            <Alert severity="warning" sx={{ mb: 1 }}>
              This will <strong>disable MFA</strong> for <strong>{confirm?.user.username}</strong>.
              Their existing secret is kept and MFA can be re-enabled without re-scanning.
            </Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirm(null)} disabled={disabling || resetting}>
            Cancel
          </Button>
          <Button
            variant="contained"
            color={confirm?.action === 'reset' ? 'error' : 'warning'}
            onClick={handleAction}
            disabled={disabling || resetting}
            startIcon={(disabling || resetting) ? <CircularProgress size={16} color="inherit" /> : null}
          >
            {confirm?.action === 'reset' ? 'Reset MFA' : 'Disable MFA'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

function StatCard({ label, value, note, color }: {
  label: string; value: number; note?: string; color: string;
}) {
  return (
    <div className={`tw-bg-white tw-rounded-xl tw-border-l-4 tw-border tw-border-gray-100 ${color} tw-p-4 tw-shadow-sm`}>
      <Typography variant="h4" fontWeight={800} color="text.primary">{value}</Typography>
      <Typography variant="body2" fontWeight={600} color="text.primary">{label}</Typography>
      {note && <Typography variant="caption" color="text.secondary">{note}</Typography>}
    </div>
  );
}
