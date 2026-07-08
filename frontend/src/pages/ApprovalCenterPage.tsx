import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Badge,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  IconButton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import HourglassTopIcon from '@mui/icons-material/HourglassTop';
import ContactsIcon from '@mui/icons-material/Contacts';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import { dashboardApi, bookingApi, partyApi } from '../api/endpoints';
import { extractErrorMessage } from '../api/client';
import { useNotification } from '../context/NotificationContext';
import { useAuth } from '../context/AuthContext';
import type { Booking, DashboardTasks, Party } from '../types';

const BOOKING_STATUS_COLORS: Record<string, 'default' | 'info' | 'warning' | 'success' | 'error'> = {
  BOOKED: 'info',
  PENDING_APPROVAL: 'warning',
  APPROVED: 'success',
  IN_TRANSIT: 'info',
  DELIVERED: 'success',
  CANCELLED: 'default',
  REJECTED: 'error',
  PENDING_CANCELLATION: 'warning',
};

// ── Approve / Reject dialog ───────────────────────────────────────────────────
interface ActionDialogProps {
  open: boolean;
  action: 'approve' | 'reject';
  type: 'booking' | 'party';
  id: number;
  label: string;
  onClose: () => void;
  onDone: () => void;
}

function ActionDialog({ open, action, type, id, label, onClose, onDone }: ActionDialogProps) {
  const { notify } = useNotification();
  const [remarks, setRemarks] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => { if (open) setRemarks(''); }, [open]);

  async function submit() {
    if (action === 'reject' && !remarks.trim()) return;
    setBusy(true);
    try {
      if (type === 'booking') {
        if (action === 'approve') await bookingApi.approve(id, remarks || '');
        else await bookingApi.reject(id, remarks);
      } else {
        if (action === 'approve') await partyApi.approve(id);
        else await partyApi.reject(id, remarks);
      }
      notify(`${type === 'booking' ? 'Booking' : 'Party'} ${action === 'approve' ? 'approved' : 'rejected'}`, 'success');
      onDone();
    } catch (e) {
      notify(extractErrorMessage(e), 'error');
    } finally {
      setBusy(false);
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ bgcolor: action === 'approve' ? 'success.main' : 'error.main', color: '#fff' }}>
        {action === 'approve' ? 'Approve' : 'Reject'} — {label}
      </DialogTitle>
      <DialogContent sx={{ pt: 2 }}>
        <TextField
          label="Remarks"
          value={remarks}
          onChange={(e) => setRemarks(e.target.value)}
          multiline
          rows={3}
          fullWidth
          required={action === 'reject'}
          helperText={action === 'reject' ? 'Reason is required for rejection' : 'Optional'}
          sx={{ mt: 1 }}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={busy}>Cancel</Button>
        <Button
          variant="contained"
          color={action === 'approve' ? 'success' : 'error'}
          onClick={submit}
          disabled={busy || (action === 'reject' && !remarks.trim())}
        >
          {action === 'approve' ? 'Approve' : 'Reject'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

// ── Tables ────────────────────────────────────────────────────────────────────
interface BookingTableProps {
  rows: Booking[];
  onNavigate: (id: number) => void;
  showActions?: boolean;
  onAction: (id: number, action: 'approve' | 'reject', label: string) => void;
}

function BookingApprovalTable({ rows, onNavigate, showActions, onAction }: BookingTableProps) {
  if (rows.length === 0)
    return <Typography variant="body2" color="text.secondary" sx={{ py: 1, pl: 1 }}>No bookings pending approval</Typography>;
  return (
    <Box sx={{ overflowX: 'auto' }}>
      <Table size="small">
        <TableHead>
          <TableRow sx={{ '& th': { fontWeight: 700, bgcolor: 'grey.100' } }}>
            <TableCell>Booking No</TableCell>
            <TableCell>Date</TableCell>
            <TableCell>Receiver</TableCell>
            <TableCell>Company</TableCell>
            <TableCell>Mode</TableCell>
            <TableCell>Weight</TableCell>
            <TableCell>Created By</TableCell>
            <TableCell>Level</TableCell>
            <TableCell>Status</TableCell>
            {showActions && <TableCell align="center">Actions</TableCell>}
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((b) => (
            <TableRow key={b.id} hover>
              <TableCell>
                <Stack direction="row" alignItems="center" spacing={0.5}>
                  <Typography
                    variant="body2"
                    sx={{ color: 'primary.main', fontWeight: 600, cursor: 'pointer', textDecoration: 'underline' }}
                    onClick={() => onNavigate(b.id)}
                  >
                    {b.bookingNumber}
                  </Typography>
                  <Tooltip title="Open in full view">
                    <IconButton size="small" onClick={() => onNavigate(b.id)}>
                      <OpenInNewIcon fontSize="inherit" />
                    </IconButton>
                  </Tooltip>
                </Stack>
              </TableCell>
              <TableCell>{b.bookingDate}</TableCell>
              <TableCell>{b.receiver?.partyName ?? '—'}</TableCell>
              <TableCell>{b.receiver?.companyName ?? '—'}</TableCell>
              <TableCell>{b.courierMode}</TableCell>
              <TableCell>{b.weightKg} kg</TableCell>
              <TableCell>{b.createdBy ?? '—'}</TableCell>
              <TableCell><Chip size="small" color="warning" label={`L${b.currentApprovalLevel}`} /></TableCell>
              <TableCell>
                <Chip size="small"
                  color={BOOKING_STATUS_COLORS[b.status] ?? 'default'}
                  label={b.status.replace(/_/g, ' ')} />
              </TableCell>
              {showActions && (
                <TableCell align="center">
                  <Stack direction="row" spacing={0.5} justifyContent="center">
                    <Tooltip title="Approve">
                      <IconButton size="small" color="success"
                        onClick={() => onAction(b.id, 'approve', b.bookingNumber)}>
                        <CheckCircleIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Reject">
                      <IconButton size="small" color="error"
                        onClick={() => onAction(b.id, 'reject', b.bookingNumber)}>
                        <CancelIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </Stack>
                </TableCell>
              )}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Box>
  );
}

interface PartyTableProps {
  rows: Party[];
  onNavigate: () => void;
  showActions?: boolean;
  onAction: (id: number, action: 'approve' | 'reject', label: string) => void;
}

function PartyApprovalTable({ rows, onNavigate, showActions, onAction }: PartyTableProps) {
  if (rows.length === 0)
    return <Typography variant="body2" color="text.secondary" sx={{ py: 1, pl: 1 }}>No parties pending approval</Typography>;
  return (
    <Box sx={{ overflowX: 'auto' }}>
      <Table size="small">
        <TableHead>
          <TableRow sx={{ '& th': { fontWeight: 700, bgcolor: 'grey.100' } }}>
            <TableCell>Code</TableCell>
            <TableCell>Party Name</TableCell>
            <TableCell>Company</TableCell>
            <TableCell>City</TableCell>
            <TableCell>Type</TableCell>
            <TableCell>Created By</TableCell>
            <TableCell>Level</TableCell>
            <TableCell>Pending With</TableCell>
            <TableCell>Status</TableCell>
            {showActions && <TableCell align="center">Actions</TableCell>}
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((p) => (
            <TableRow key={p.id} hover sx={{ cursor: 'pointer' }} onClick={!showActions ? onNavigate : undefined}>
              <TableCell sx={{ color: 'primary.main', fontWeight: 600 }}>{p.partyCode}</TableCell>
              <TableCell>{p.partyName}</TableCell>
              <TableCell>{p.companyName ?? '—'}</TableCell>
              <TableCell>{p.city}</TableCell>
              <TableCell>{p.partyType}</TableCell>
              <TableCell>{p.createdBy ?? '—'}</TableCell>
              <TableCell><Chip size="small" color="warning" label={`L${p.currentApprovalLevel}`} /></TableCell>
              <TableCell>
                {p.pendingApprovers?.length
                  ? <Chip size="small" color="info" label={p.pendingApprovers.join(', ')} />
                  : <Typography variant="body2" color="text.secondary">—</Typography>}
              </TableCell>
              <TableCell>
                <Chip size="small"
                  color={p.partyStatus === 'PENDING_APPROVAL' ? 'warning' : 'default'}
                  label={p.partyStatus?.replace(/_/g, ' ') ?? '—'} />
              </TableCell>
              {showActions && (
                <TableCell align="center">
                  <Stack direction="row" spacing={0.5} justifyContent="center">
                    <Tooltip title="Approve">
                      <IconButton size="small" color="success"
                        onClick={(e) => { e.stopPropagation(); onAction(p.id, 'approve', p.partyName); }}>
                        <CheckCircleIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Reject">
                      <IconButton size="small" color="error"
                        onClick={(e) => { e.stopPropagation(); onAction(p.id, 'reject', p.partyName); }}>
                        <CancelIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </Stack>
                </TableCell>
              )}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Box>
  );
}

// ── Page ─────────────────────────────────────────────────────────────────────
export default function ApprovalCenterPage() {
  const navigate = useNavigate();
  const { hasAnyPermission } = useAuth();
  const isAdmin = hasAnyPermission(['ADMIN_VIEW']);
  const [tasks, setTasks] = useState<DashboardTasks | null>(null);
  const [loading, setLoading] = useState(true);

  // Action dialog state
  const [dialog, setDialog] = useState<{
    open: boolean;
    action: 'approve' | 'reject';
    type: 'booking' | 'party';
    id: number;
    label: string;
  }>({ open: false, action: 'approve', type: 'booking', id: 0, label: '' });

  const load = useCallback(() => {
    setLoading(true);
    dashboardApi.getTasks()
      .then(setTasks)
      .catch(() => undefined)
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  function openAction(type: 'booking' | 'party', id: number, action: 'approve' | 'reject', label: string) {
    setDialog({ open: true, action, type, id, label });
  }

  const totalPending =
    (tasks?.bookingsPendingMyApproval.length ?? 0) + (tasks?.partiesPendingMyApproval.length ?? 0);

  return (
    <Stack spacing={3}>
      <Stack direction="row" alignItems="center" spacing={1}>
        <HourglassTopIcon color={totalPending > 0 ? 'warning' : 'action'} />
        <Typography variant="h5" fontWeight={600}>Approval Center</Typography>
        <Badge badgeContent={totalPending} color="error" showZero>
          <Box />
        </Badge>
        <Chip size="small" label={`${totalPending} pending`} color={totalPending > 0 ? 'warning' : 'default'} />
        {isAdmin && (
          <Chip size="small" label="Admin View — All Pending" color="info" variant="outlined" />
        )}
      </Stack>

      {loading && <Typography color="text.secondary">Loading...</Typography>}

      {tasks && (
        <Stack spacing={2}>
          {/* Pending approval — my queue (or all for admin) */}
          <Card>
            <CardContent>
              <Stack direction="row" spacing={1} alignItems="center" mb={1.5}>
                <LocalShippingIcon color="primary" fontSize="small" />
                <Typography variant="subtitle1" fontWeight={700}>
                  {isAdmin ? 'All Bookings Pending Approval' : 'Bookings Pending My Approval'}
                </Typography>
                <Chip size="small"
                  label={tasks.bookingsPendingMyApproval.length}
                  color={tasks.bookingsPendingMyApproval.length > 0 ? 'warning' : 'default'} />
              </Stack>
              <BookingApprovalTable
                rows={tasks.bookingsPendingMyApproval}
                onNavigate={(id) => navigate(`/bookings/${id}/edit?view=1`)}
                showActions={hasAnyPermission(['BOOKING_APPROVE']) || isAdmin}
                onAction={(id, action, label) => openAction('booking', id, action, label)}
              />
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <Stack direction="row" spacing={1} alignItems="center" mb={1.5}>
                <ContactsIcon color="primary" fontSize="small" />
                <Typography variant="subtitle1" fontWeight={700}>
                  {isAdmin ? 'All Parties Pending Approval' : 'Parties Pending My Approval'}
                </Typography>
                <Chip size="small"
                  label={tasks.partiesPendingMyApproval.length}
                  color={tasks.partiesPendingMyApproval.length > 0 ? 'warning' : 'default'} />
              </Stack>
              <PartyApprovalTable
                rows={tasks.partiesPendingMyApproval}
                onNavigate={() => navigate('/master/parties')}
                showActions={hasAnyPermission(['MASTER_APPROVE']) || isAdmin}
                onAction={(id, action, label) => openAction('party', id, action, label)}
              />
            </CardContent>
          </Card>

          {/* What I submitted */}
          <Divider />
          <Typography variant="h6" fontWeight={600} color="text.secondary">
            Documents I Submitted — Awaiting Others' Approval
          </Typography>

          <Card>
            <CardContent>
              <Stack direction="row" spacing={1} alignItems="center" mb={1.5}>
                <LocalShippingIcon color="action" fontSize="small" />
                <Typography variant="subtitle1" fontWeight={700}>My Bookings (Pending)</Typography>
                <Chip size="small" label={tasks.myBookingsPendingSent.length} />
              </Stack>
              <BookingApprovalTable
                rows={tasks.myBookingsPendingSent}
                onNavigate={(id) => navigate(`/bookings/${id}/edit?view=1`)}
                showActions={false}
                onAction={() => undefined}
              />
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <Stack direction="row" spacing={1} alignItems="center" mb={1.5}>
                <ContactsIcon color="action" fontSize="small" />
                <Typography variant="subtitle1" fontWeight={700}>My Parties (Pending)</Typography>
                <Chip size="small" label={tasks.myPartiesPendingSent.length} />
              </Stack>
              <PartyApprovalTable
                rows={tasks.myPartiesPendingSent}
                onNavigate={() => navigate('/master/parties')}
                showActions={false}
                onAction={() => undefined}
              />
            </CardContent>
          </Card>
        </Stack>
      )}

      {/* Approve / Reject dialog */}
      <ActionDialog
        open={dialog.open}
        action={dialog.action}
        type={dialog.type}
        id={dialog.id}
        label={dialog.label}
        onClose={() => setDialog((d) => ({ ...d, open: false }))}
        onDone={() => { setDialog((d) => ({ ...d, open: false })); load(); }}
      />
    </Stack>
  );
}
