import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Fade,
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
  alpha,
} from '@mui/material';
import HourglassTopIcon from '@mui/icons-material/HourglassTop';
import ContactsIcon from '@mui/icons-material/Contacts';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import InboxIcon from '@mui/icons-material/Inbox';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import OutboxIcon from '@mui/icons-material/Outbox';
import TaskAltIcon from '@mui/icons-material/TaskAlt';
import { dashboardApi, bookingApi, partyApi } from '../api/endpoints';
import { extractErrorMessage } from '../api/client';
import { useNotification } from '../context/NotificationContext';
import { useAuth } from '../context/AuthContext';
import type { Booking, DashboardTasks, Party } from '../types';

const HERO_GRADIENT = 'linear-gradient(135deg, #0f172a 0%, #1e3a8a 60%, #312e81 100%)';
const ACCENT_GRADIENT = 'linear-gradient(135deg, #1d4ed8, #4f46e5)';

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
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth PaperProps={{ sx: { borderRadius: 3 } }}>
      <DialogTitle
        sx={{
          background: action === 'approve'
            ? 'linear-gradient(135deg, #16a34a, #22c55e)'
            : 'linear-gradient(135deg, #dc2626, #ef4444)',
          color: '#fff',
          display: 'flex',
          alignItems: 'center',
          gap: 1,
        }}
      >
        {action === 'approve' ? <CheckCircleIcon /> : <CancelIcon />}
        {action === 'approve' ? 'Approve' : 'Reject'} — {label}
      </DialogTitle>
      <DialogContent sx={{ pt: 3 }}>
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
      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        <Button onClick={onClose} disabled={busy}>Cancel</Button>
        <Button
          variant="contained"
          color={action === 'approve' ? 'success' : 'error'}
          onClick={submit}
          disabled={busy || (action === 'reject' && !remarks.trim())}
          sx={{ borderRadius: 2, px: 3 }}
        >
          {action === 'approve' ? 'Approve' : 'Reject'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

// ── Shared empty-state ───────────────────────────────────────────────────────
function EmptyState({ label }: { label: string }) {
  return (
    <Stack alignItems="center" spacing={1} sx={{ py: 4, opacity: 0.7 }}>
      <Box
        sx={{
          width: 44, height: 44, borderRadius: '50%',
          bgcolor: 'success.50', display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}
      >
        <TaskAltIcon sx={{ color: 'success.main', fontSize: 24 }} />
      </Box>
      <Typography variant="body2" color="text.secondary">{label}</Typography>
    </Stack>
  );
}

// ── Section card wrapper ─────────────────────────────────────────────────────
interface SectionCardProps {
  icon: React.ReactNode;
  accent: string;
  title: string;
  count: number;
  emphasize?: boolean;
  children: React.ReactNode;
}

function SectionCard({ icon, accent, title, count, emphasize, children }: SectionCardProps) {
  return (
    <Card
      elevation={0}
      sx={{
        borderRadius: 3,
        border: '1px solid',
        borderColor: 'divider',
        overflow: 'hidden',
        transition: 'box-shadow 0.2s ease, transform 0.2s ease',
        '&:hover': { boxShadow: '0 8px 24px rgba(15, 23, 42, 0.08)' },
      }}
    >
      <Box sx={{ height: 4, background: count > 0 && emphasize ? accent : 'transparent' }} />
      <CardContent sx={{ pb: '20px !important' }}>
        <Stack direction="row" spacing={1.5} alignItems="center" mb={2}>
          <Avatar
            variant="rounded"
            sx={{
              width: 36, height: 36, borderRadius: 2,
              background: count > 0 && emphasize ? accent : alpha('#64748b', 0.12),
              color: count > 0 && emphasize ? '#fff' : 'text.secondary',
            }}
          >
            {icon}
          </Avatar>
          <Typography variant="subtitle1" fontWeight={700} sx={{ flexGrow: 1 }}>
            {title}
          </Typography>
          <Chip
            size="small"
            label={count}
            sx={{
              fontWeight: 700,
              bgcolor: count > 0 && emphasize ? alpha('#f59e0b', 0.15) : 'grey.100',
              color: count > 0 && emphasize ? '#b45309' : 'text.secondary',
            }}
          />
        </Stack>
        {children}
      </CardContent>
    </Card>
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
  if (rows.length === 0) return <EmptyState label="No bookings pending approval" />;
  return (
    <Box sx={{ overflowX: 'auto', borderRadius: 2, border: '1px solid', borderColor: 'divider' }}>
      <Table size="small">
        <TableHead>
          <TableRow sx={{ '& th': { fontWeight: 700, bgcolor: alpha('#1d4ed8', 0.05), borderBottom: '2px solid', borderColor: alpha('#1d4ed8', 0.12) } }}>
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
            <TableRow
              key={b.id}
              hover
              sx={{ '&:nth-of-type(odd)': { bgcolor: alpha('#64748b', 0.03) } }}
            >
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
              <TableCell><Chip size="small" color="warning" label={`L${b.currentApprovalLevel}`} sx={{ fontWeight: 600 }} /></TableCell>
              <TableCell>
                <Chip size="small"
                  color={BOOKING_STATUS_COLORS[b.status] ?? 'default'}
                  label={b.status.replace(/_/g, ' ')}
                  sx={{ fontWeight: 600 }} />
              </TableCell>
              {showActions && (
                <TableCell align="center">
                  <Stack direction="row" spacing={0.5} justifyContent="center">
                    <Tooltip title="Approve">
                      <IconButton size="small" color="success"
                        sx={{ bgcolor: alpha('#16a34a', 0.1), '&:hover': { bgcolor: alpha('#16a34a', 0.2) } }}
                        onClick={() => onAction(b.id, 'approve', b.bookingNumber)}>
                        <CheckCircleIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Reject">
                      <IconButton size="small" color="error"
                        sx={{ bgcolor: alpha('#dc2626', 0.1), '&:hover': { bgcolor: alpha('#dc2626', 0.2) } }}
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
  if (rows.length === 0) return <EmptyState label="No parties pending approval" />;
  return (
    <Box sx={{ overflowX: 'auto', borderRadius: 2, border: '1px solid', borderColor: 'divider' }}>
      <Table size="small">
        <TableHead>
          <TableRow sx={{ '& th': { fontWeight: 700, bgcolor: alpha('#7c3aed', 0.05), borderBottom: '2px solid', borderColor: alpha('#7c3aed', 0.12) } }}>
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
            <TableRow
              key={p.id}
              hover
              sx={{ cursor: 'pointer', '&:nth-of-type(odd)': { bgcolor: alpha('#64748b', 0.03) } }}
              onClick={!showActions ? onNavigate : undefined}
            >
              <TableCell sx={{ color: 'primary.main', fontWeight: 600 }}>{p.partyCode}</TableCell>
              <TableCell>{p.partyName}</TableCell>
              <TableCell>{p.companyName ?? '—'}</TableCell>
              <TableCell>{p.city}</TableCell>
              <TableCell>{p.partyType}</TableCell>
              <TableCell>{p.createdBy ?? '—'}</TableCell>
              <TableCell><Chip size="small" color="warning" label={`L${p.currentApprovalLevel}`} sx={{ fontWeight: 600 }} /></TableCell>
              <TableCell>
                {p.pendingApprovers?.length
                  ? <Chip size="small" color="info" label={p.pendingApprovers.join(', ')} sx={{ fontWeight: 600 }} />
                  : <Typography variant="body2" color="text.secondary">—</Typography>}
              </TableCell>
              <TableCell>
                <Chip size="small"
                  color={p.partyStatus === 'PENDING_APPROVAL' ? 'warning' : 'default'}
                  label={p.partyStatus?.replace(/_/g, ' ') ?? '—'}
                  sx={{ fontWeight: 600 }} />
              </TableCell>
              {showActions && (
                <TableCell align="center">
                  <Stack direction="row" spacing={0.5} justifyContent="center">
                    <Tooltip title="Approve">
                      <IconButton size="small" color="success"
                        sx={{ bgcolor: alpha('#16a34a', 0.1), '&:hover': { bgcolor: alpha('#16a34a', 0.2) } }}
                        onClick={(e) => { e.stopPropagation(); onAction(p.id, 'approve', p.partyName); }}>
                        <CheckCircleIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Reject">
                      <IconButton size="small" color="error"
                        sx={{ bgcolor: alpha('#dc2626', 0.1), '&:hover': { bgcolor: alpha('#dc2626', 0.2) } }}
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

  const bookingsPendingCount = tasks?.bookingsPendingMyApproval.length ?? 0;
  const partiesPendingCount = tasks?.partiesPendingMyApproval.length ?? 0;
  const totalPending = bookingsPendingCount + partiesPendingCount;
  const myBookingsSentCount = tasks?.myBookingsPendingSent.length ?? 0;
  const myPartiesSentCount = tasks?.myPartiesPendingSent.length ?? 0;

  const statCards = [
    {
      label: 'Total Pending',
      value: totalPending,
      icon: <HourglassTopIcon />,
      accent: totalPending > 0 ? ACCENT_GRADIENT : 'linear-gradient(135deg, #94a3b8, #cbd5e1)',
    },
    {
      label: 'Bookings Pending',
      value: bookingsPendingCount,
      icon: <LocalShippingIcon />,
      accent: 'linear-gradient(135deg, #0891b2, #06b6d4)',
    },
    {
      label: 'Parties Pending',
      value: partiesPendingCount,
      icon: <ContactsIcon />,
      accent: 'linear-gradient(135deg, #7c3aed, #a78bfa)',
    },
    {
      label: 'My Submissions',
      value: myBookingsSentCount + myPartiesSentCount,
      icon: <OutboxIcon />,
      accent: 'linear-gradient(135deg, #64748b, #94a3b8)',
    },
  ];

  return (
    <Stack spacing={3}>
      {/* ── Hero header ─────────────────────────────────────────────────── */}
      <Box
        sx={{
          background: HERO_GRADIENT,
          borderRadius: 3,
          p: { xs: 2.5, md: 3 },
          color: '#fff',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        <Box
          sx={{
            position: 'absolute', top: -40, right: -40, width: 160, height: 160,
            borderRadius: '50%', background: 'rgba(255,255,255,0.06)',
          }}
        />
        <Stack direction={{ xs: 'column', sm: 'row' }} alignItems={{ xs: 'flex-start', sm: 'center' }} spacing={2} sx={{ position: 'relative' }}>
          <Avatar sx={{ width: 48, height: 48, bgcolor: 'rgba(255,255,255,0.15)' }}>
            <HourglassTopIcon />
          </Avatar>
          <Box sx={{ flexGrow: 1 }}>
            <Typography variant="h5" fontWeight={700}>Approval Center</Typography>
            <Typography variant="body2" sx={{ opacity: 0.75 }}>
              Review and action bookings and parties awaiting approval
            </Typography>
          </Box>
          {isAdmin && (
            <Chip
              icon={<AdminPanelSettingsIcon sx={{ color: '#fff !important' }} />}
              label="Admin View — All Pending"
              sx={{ bgcolor: 'rgba(255,255,255,0.15)', color: '#fff', fontWeight: 600 }}
            />
          )}
        </Stack>
      </Box>

      {/* ── Stat cards ──────────────────────────────────────────────────── */}
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        {statCards.map((s) => (
          <Card
            key={s.label}
            elevation={0}
            sx={{
              flex: 1,
              borderRadius: 3,
              border: '1px solid',
              borderColor: 'divider',
              transition: 'transform 0.15s ease, box-shadow 0.15s ease',
              '&:hover': { transform: 'translateY(-2px)', boxShadow: '0 8px 20px rgba(15,23,42,0.08)' },
            }}
          >
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 1.5, '&:last-child': { pb: 2 } }}>
              <Avatar sx={{ background: s.accent, width: 40, height: 40 }}>{s.icon}</Avatar>
              <Box>
                <Typography variant="h5" fontWeight={700} lineHeight={1.2}>{s.value}</Typography>
                <Typography variant="caption" color="text.secondary">{s.label}</Typography>
              </Box>
            </CardContent>
          </Card>
        ))}
      </Stack>

      {loading && (
        <Typography color="text.secondary" sx={{ py: 2 }}>Loading approval queue…</Typography>
      )}

      {tasks && (
        <Fade in timeout={300}>
          <Stack spacing={2.5}>
            {/* Pending approval — my queue (or all for admin) */}
            <SectionCard
              icon={<LocalShippingIcon fontSize="small" />}
              accent="linear-gradient(135deg, #0891b2, #06b6d4)"
              title={isAdmin ? 'All Bookings Pending Approval' : 'Bookings Pending My Approval'}
              count={bookingsPendingCount}
              emphasize
            >
              <BookingApprovalTable
                rows={tasks.bookingsPendingMyApproval}
                onNavigate={(id) => navigate(`/bookings/${id}/edit?view=1`)}
                showActions={hasAnyPermission(['BOOKING_APPROVE']) || isAdmin}
                onAction={(id, action, label) => openAction('booking', id, action, label)}
              />
            </SectionCard>

            <SectionCard
              icon={<ContactsIcon fontSize="small" />}
              accent="linear-gradient(135deg, #7c3aed, #a78bfa)"
              title={isAdmin ? 'All Parties Pending Approval' : 'Parties Pending My Approval'}
              count={partiesPendingCount}
              emphasize
            >
              <PartyApprovalTable
                rows={tasks.partiesPendingMyApproval}
                onNavigate={() => navigate('/master/parties')}
                showActions={hasAnyPermission(['MASTER_APPROVE']) || isAdmin}
                onAction={(id, action, label) => openAction('party', id, action, label)}
              />
            </SectionCard>

            {/* What I submitted */}
            <Stack direction="row" alignItems="center" spacing={1.5} sx={{ pt: 1 }}>
              <Box sx={{ flexGrow: 1, height: '1px', bgcolor: 'divider' }} />
              <Chip
                icon={<OutboxIcon fontSize="small" />}
                label="Documents I Submitted — Awaiting Others' Approval"
                variant="outlined"
                sx={{ fontWeight: 600, color: 'text.secondary', px: 1 }}
              />
              <Box sx={{ flexGrow: 1, height: '1px', bgcolor: 'divider' }} />
            </Stack>

            <SectionCard
              icon={<LocalShippingIcon fontSize="small" />}
              accent="linear-gradient(135deg, #64748b, #94a3b8)"
              title="My Bookings (Pending)"
              count={myBookingsSentCount}
            >
              <BookingApprovalTable
                rows={tasks.myBookingsPendingSent}
                onNavigate={(id) => navigate(`/bookings/${id}/edit?view=1`)}
                showActions={false}
                onAction={() => undefined}
              />
            </SectionCard>

            <SectionCard
              icon={<ContactsIcon fontSize="small" />}
              accent="linear-gradient(135deg, #64748b, #94a3b8)"
              title="My Parties (Pending)"
              count={myPartiesSentCount}
            >
              <PartyApprovalTable
                rows={tasks.myPartiesPendingSent}
                onNavigate={() => navigate('/master/parties')}
                showActions={false}
                onAction={() => undefined}
              />
            </SectionCard>
          </Stack>
        </Fade>
      )}

      {!loading && !tasks && (
        <Stack alignItems="center" spacing={1} sx={{ py: 6 }}>
          <InboxIcon sx={{ fontSize: 40, color: 'text.disabled' }} />
          <Typography color="text.secondary">Unable to load approval queue</Typography>
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
