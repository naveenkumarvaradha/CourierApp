import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Badge,
  Box,
  Button,
  Card,
  CardActionArea,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Grid,
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
import SendIcon from '@mui/icons-material/Send';
import PeopleIcon from '@mui/icons-material/People';
import ContactsIcon from '@mui/icons-material/Contacts';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import AssessmentIcon from '@mui/icons-material/Assessment';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import PrintIcon from '@mui/icons-material/Print';
import EditIcon from '@mui/icons-material/Edit';
import { useAuth } from '../context/AuthContext';
import { useNotification } from '../context/NotificationContext';
import { bookingApi } from '../api/endpoints';
import { extractErrorMessage } from '../api/client';
import { useGetDashboardTasksQuery, useGetReportSummaryQuery } from '../store/api/dashboardApiSlice';
import { useApproveBookingMutation, useRejectBookingMutation, useUpdateAwbMutation } from '../store/api/bookingApiSlice';
import type { Booking, Party } from '../types';

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

function ReadOnlyField({ label, value }: { label: string; value?: string | number | null }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary">{label}</Typography>
      <Typography variant="body2" fontWeight={500}>{value ?? '—'}</Typography>
    </Box>
  );
}

function StatCard({ label, value, color = '#2563eb', icon }: {
  label: string; value: string | number; color?: string; icon?: React.ReactNode;
}) {
  return (
    <Grid item xs={6} sm={6} md={3}>
      <Box
        sx={{
          bgcolor: 'white', borderRadius: 3, p: { xs: 2, md: 2.5 },
          boxShadow: '0 1px 3px rgba(0,0,0,0.07), 0 0 0 1px rgba(0,0,0,0.04)',
          borderLeft: `4px solid ${color}`,
          transition: 'box-shadow 0.2s',
          '&:hover': { boxShadow: '0 4px 16px rgba(0,0,0,0.1)' },
          height: '100%',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', mb: 1 }}>
          <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.8, fontSize: 11 }}>
            {label}
          </Typography>
          {icon && (
            <Box sx={{ color, opacity: 0.8 }}>{icon}</Box>
          )}
        </Box>
        <Typography sx={{ fontSize: { xs: 28, md: 32 }, fontWeight: 800, color: 'text.primary', lineHeight: 1 }}>
          {value}
        </Typography>
      </Box>
    </Grid>
  );
}

function SectionHeader({ icon, title, count }: { icon: React.ReactNode; title: string; count: number }) {
  return (
    <Stack direction="row" spacing={1} alignItems="center">
      {icon}
      <Typography variant="subtitle1" fontWeight={700}>{title}</Typography>
      <Badge badgeContent={count} color={count > 0 ? 'error' : 'default'} showZero>
        <Box />
      </Badge>
      <Chip size="small" label={count} color={count > 0 ? 'warning' : 'default'} />
    </Stack>
  );
}

function BookingTaskTable({
  rows,
  showActions,
  showApprovedBy,
  showPendingWith,
  onView,
  onApprove,
  onReject,
  onNavigate,
}: {
  rows: Booking[];
  showActions?: boolean;
  showApprovedBy?: boolean;
  showPendingWith?: boolean;
  onView: (b: Booking) => void;
  onApprove?: (b: Booking) => void;
  onReject?: (b: Booking) => void;
  onNavigate: (id: number) => void;
}) {
  if (rows.length === 0)
    return <Typography variant="body2" color="text.secondary" sx={{ py: 1, pl: 1 }}>No items</Typography>;
  return (
    <Box sx={{ overflowX: 'auto' }}>
      <Table size="small">
        <TableHead>
          <TableRow sx={{ '& th': { fontWeight: 700, bgcolor: 'grey.100' } }}>
            <TableCell>Booking No</TableCell>
            <TableCell>Date</TableCell>
            <TableCell>Receiver</TableCell>
            <TableCell>Mode</TableCell>
            <TableCell>Via</TableCell>
            <TableCell>Created By</TableCell>
            <TableCell>Status</TableCell>
            {showApprovedBy && <TableCell>Approved By</TableCell>}
            {showPendingWith && <TableCell>Pending With</TableCell>}
            {showActions && <TableCell>Actions</TableCell>}
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((b) => (
            <TableRow key={b.id} hover>
              <TableCell>
                <Stack direction="row" alignItems="center" spacing={0.5}>
                  <Typography
                    variant="body2"
                    fontWeight={600}
                    color="primary.main"
                    sx={{ cursor: 'pointer', textDecoration: 'underline' }}
                    onClick={() => onView(b)}
                  >
                    {b.bookingNumber}
                  </Typography>
                  <Tooltip title="Open full view">
                    <IconButton size="small" onClick={() => onNavigate(b.id)} sx={{ p: 0.2 }}>
                      <OpenInNewIcon sx={{ fontSize: 14 }} />
                    </IconButton>
                  </Tooltip>
                </Stack>
              </TableCell>
              <TableCell>{b.bookingDate}</TableCell>
              <TableCell>{b.receiver?.partyName ?? '—'}</TableCell>
              <TableCell>{b.courierMode}</TableCell>
              <TableCell>{b.courierWay?.name ?? '—'}</TableCell>
              <TableCell>{b.createdBy ?? '—'}</TableCell>
              <TableCell>
                <Chip size="small"
                  color={BOOKING_STATUS_COLORS[b.status] ?? 'default'}
                  label={b.status.replace(/_/g, ' ')} />
              </TableCell>
              {showApprovedBy && (
                <TableCell>
                  <Typography variant="body2" color="success.main" fontWeight={500}>
                    {b.approverUsername ?? '—'}
                  </Typography>
                </TableCell>
              )}
              {showPendingWith && (
                <TableCell>
                  {b.pendingApprovers && b.pendingApprovers.length > 0 ? (
                    <Stack spacing={0.3}>
                      {b.pendingApprovers.map((a, i) => (
                        <Chip key={i} size="small" label={a} variant="outlined" color="warning" />
                      ))}
                    </Stack>
                  ) : (
                    <Typography variant="body2" color="text.secondary">—</Typography>
                  )}
                </TableCell>
              )}
              {showActions && (
                <TableCell>
                  <Stack direction="row" spacing={0.5}>
                    <Tooltip title="Approve">
                      <IconButton size="small" color="success" onClick={() => onApprove?.(b)}>
                        <CheckCircleIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Reject">
                      <IconButton size="small" color="error" onClick={() => onReject?.(b)}>
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

function PartyTaskTable({
  rows,
  onNavigate,
  showPendingWith,
}: {
  rows: Party[];
  onNavigate: () => void;
  showPendingWith?: boolean;
}) {
  if (rows.length === 0)
    return <Typography variant="body2" color="text.secondary" sx={{ py: 1, pl: 1 }}>No items</Typography>;
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
            {showPendingWith && <TableCell>Pending With</TableCell>}
            <TableCell>Status</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((p) => (
            <TableRow key={p.id} hover sx={{ cursor: 'pointer' }} onClick={onNavigate}>
              <TableCell sx={{ color: 'primary.main', fontWeight: 600 }}>{p.partyCode}</TableCell>
              <TableCell>{p.partyName}</TableCell>
              <TableCell>{p.companyName ?? '—'}</TableCell>
              <TableCell>{p.city}</TableCell>
              <TableCell>{p.partyType}</TableCell>
              <TableCell>{p.createdBy ?? '—'}</TableCell>
              {showPendingWith && (
                <TableCell>
                  {p.pendingApprovers?.length ? (
                    <Stack spacing={0.3}>
                      {p.pendingApprovers.map((a, i) => (
                        <Chip key={i} size="small" label={a} variant="outlined" color="warning" />
                      ))}
                    </Stack>
                  ) : (
                    <Typography variant="body2" color="text.secondary">—</Typography>
                  )}
                </TableCell>
              )}
              <TableCell>
                <Chip size="small"
                  color={p.partyStatus === 'PENDING_APPROVAL' ? 'warning' : 'default'}
                  label={p.partyStatus?.replace(/_/g, ' ') ?? '—'} />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Box>
  );
}

function PendingPrintTable({
  rows,
  onView,
  onSetAwb,
  onPrint,
  onNavigate,
}: {
  rows: Booking[];
  onView: (b: Booking) => void;
  onSetAwb: (b: Booking) => void;
  onPrint: (b: Booking) => void;
  onNavigate: (id: number) => void;
}) {
  if (rows.length === 0)
    return <Typography variant="body2" color="text.secondary" sx={{ py: 1, pl: 1 }}>No bookings pending print</Typography>;
  return (
    <Box sx={{ overflowX: 'auto' }}>
      <Table size="small">
        <TableHead>
          <TableRow sx={{ '& th': { fontWeight: 700, bgcolor: 'grey.100' } }}>
            <TableCell>Booking No</TableCell>
            <TableCell>Date</TableCell>
            <TableCell>Receiver</TableCell>
            <TableCell>Mode</TableCell>
            <TableCell>Via</TableCell>
            <TableCell>Created By</TableCell>
            <TableCell>Approved By</TableCell>
            <TableCell>AWB No</TableCell>
            <TableCell>Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((b) => (
            <TableRow key={b.id} hover>
              <TableCell>
                <Stack direction="row" alignItems="center" spacing={0.5}>
                  <Typography
                    variant="body2"
                    fontWeight={600}
                    color="primary.main"
                    sx={{ cursor: 'pointer', textDecoration: 'underline' }}
                    onClick={() => onView(b)}
                  >
                    {b.bookingNumber}
                  </Typography>
                  <Tooltip title="Open full view">
                    <IconButton size="small" onClick={() => onNavigate(b.id)} sx={{ p: 0.2 }}>
                      <OpenInNewIcon sx={{ fontSize: 14 }} />
                    </IconButton>
                  </Tooltip>
                </Stack>
              </TableCell>
              <TableCell>{b.bookingDate}</TableCell>
              <TableCell>
                <Box>
                  <Typography variant="body2">{b.receiver?.partyName ?? '—'}</Typography>
                  {b.receiver?.companyName && (
                    <Typography variant="caption" color="text.secondary">{b.receiver.companyName}</Typography>
                  )}
                </Box>
              </TableCell>
              <TableCell>{b.courierMode}</TableCell>
              <TableCell>{b.courierWay?.name ?? '—'}</TableCell>
              <TableCell>{b.createdBy ?? '—'}</TableCell>
              <TableCell>
                <Typography variant="body2" color="success.main" fontWeight={500}>
                  {b.approverUsername ?? '—'}
                </Typography>
              </TableCell>
              <TableCell>
                {b.awbNumber ? (
                  <Chip size="small" label={b.awbNumber} color="primary" variant="outlined" />
                ) : (
                  <Typography variant="caption" color="error.main">Not set</Typography>
                )}
              </TableCell>
              <TableCell>
                <Stack direction="row" spacing={0.5}>
                  <Tooltip title={b.awbNumber ? 'Update AWB' : 'Set AWB Number'}>
                    <IconButton size="small" color="primary" onClick={() => onSetAwb(b)}>
                      <EditIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title={b.awbNumber ? 'Print Sticker' : 'Set AWB first'}>
                    <span>
                      <IconButton
                        size="small"
                        color="secondary"
                        onClick={() => onPrint(b)}
                        disabled={!b.awbNumber}
                      >
                        <PrintIcon fontSize="small" />
                      </IconButton>
                    </span>
                  </Tooltip>
                </Stack>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Box>
  );
}

export default function DashboardPage() {
  const { user, hasAnyPermission } = useAuth();
  const { notify } = useNotification();
  const navigate = useNavigate();

  // ── RTK Query ──────────────────────────────────────────────────────────────
  const { data: tasks } = useGetDashboardTasksQuery();
  const { data: summary } = useGetReportSummaryQuery(
    { granularity: 'monthly' },
    { skip: !hasAnyPermission(['REPORTS_VIEW']) }
  );
  const [approveBooking, { isLoading: approving }] = useApproveBookingMutation();
  const [rejectBooking,  { isLoading: rejecting }]  = useRejectBookingMutation();
  const [updateAwb,      { isLoading: awbLoading }]  = useUpdateAwbMutation();

  const [viewBooking, setViewBooking] = useState<Booking | null>(null);
  const [actionBooking, setActionBooking] = useState<{ booking: Booking; type: 'approve' | 'reject' } | null>(null);
  const [remarks, setRemarks] = useState('');
  const actionLoading = approving || rejecting;

  // AWB dialog
  const [awbBooking, setAwbBooking] = useState<Booking | null>(null);
  const [awbValue, setAwbValue] = useState('');

  const openApprove = (b: Booking) => { setRemarks(''); setActionBooking({ booking: b, type: 'approve' }); };
  const openReject  = (b: Booking) => { setRemarks(''); setActionBooking({ booking: b, type: 'reject' }); };
  const closeAction = () => { setActionBooking(null); setRemarks(''); };

  const confirmAction = async () => {
    if (!actionBooking) return;
    if (actionBooking.type === 'reject' && !remarks.trim()) {
      notify('Remarks required for rejection', 'warning');
      return;
    }
    try {
      if (actionBooking.type === 'approve') {
        await approveBooking({ id: actionBooking.booking.id, remarks }).unwrap();
        notify('Booking approved', 'success');
      } else {
        await rejectBooking({ id: actionBooking.booking.id, remarks }).unwrap();
        notify('Booking rejected', 'success');
      }
      closeAction();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const openAwbDialog = (b: Booking) => { setAwbBooking(b); setAwbValue(b.awbNumber ?? ''); };
  const closeAwbDialog = () => { setAwbBooking(null); setAwbValue(''); };

  const saveAwb = async () => {
    if (!awbBooking || !awbValue.trim()) { notify('Enter AWB number', 'warning'); return; }
    try {
      await updateAwb({ id: awbBooking.id, awbNumber: awbValue.trim() }).unwrap();
      notify('AWB number updated', 'success');
      closeAwbDialog();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const printSticker = async (b: Booking) => {
    try {
      const blob = await bookingApi.fetchSticker(b.id);
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank');
      setTimeout(() => URL.revokeObjectURL(url), 60000);
      notify('Sticker opened — use browser Print (Ctrl+P)', 'success');
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const tiles = [
    { label: 'Users', icon: <PeopleIcon fontSize="large" />, to: '/admin/users', perm: ['ADMIN_VIEW'] },
    { label: 'Parties', icon: <ContactsIcon fontSize="large" />, to: '/master/parties', perm: ['MASTER_VIEW'] },
    { label: 'Bookings', icon: <LocalShippingIcon fontSize="large" />, to: '/bookings', perm: ['BOOKING_VIEW'] },
    { label: 'Reports', icon: <AssessmentIcon fontSize="large" />, to: '/reports', perm: ['REPORTS_VIEW'] },
  ].filter((t) => hasAnyPermission(t.perm));

  const canPrint = hasAnyPermission(['BOOKING_PRINT']);
  const canViewBookings = hasAnyPermission(['BOOKING_VIEW']);
  const canViewParties = hasAnyPermission(['MASTER_VIEW']);

  const totalMyApprovals =
    (tasks?.bookingsPendingMyApproval.length ?? 0) + (tasks?.partiesPendingMyApproval.length ?? 0);
  const totalMySent =
    (tasks?.myBookingsPendingSent.length ?? 0) + (tasks?.myPartiesPendingSent.length ?? 0);
  const totalPendingPrint = canPrint ? (tasks?.pendingToPrint.length ?? 0) : 0;
  const totalAllPending = canViewBookings ? (tasks?.allPendingApprovalBookings.length ?? 0) : 0;
  const totalAllPendingParties = canViewParties ? (tasks?.allPendingApprovalParties?.length ?? 0) : 0;

  return (
    <Stack spacing={3}>
      {/* Page header */}
      <Box
        sx={{
          background: 'linear-gradient(135deg, #1d4ed8 0%, #4f46e5 100%)',
          borderRadius: 3,
          px: { xs: 2.5, md: 4 },
          py: { xs: 2.5, md: 3 },
          display: 'flex',
          alignItems: { sm: 'center' },
          justifyContent: 'space-between',
          flexDirection: { xs: 'column', sm: 'row' },
          gap: 2,
        }}
      >
        <Box>
          <Typography variant="h5" fontWeight={800} sx={{ color: 'white', letterSpacing: '-0.3px' }}>
            Welcome back, {user?.fullName?.split(' ')[0]} 👋
          </Typography>
          <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.7)', mt: 0.25 }}>
            {new Date().toLocaleDateString('en-IN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
          {hasAnyPermission(['BOOKING_VIEW']) && (
            <Button
              variant="contained"
              size="small"
              startIcon={<LocalShippingIcon />}
              onClick={() => navigate('/bookings')}
              sx={{
                bgcolor: 'rgba(255,255,255,0.15)', color: 'white',
                backdropFilter: 'blur(4px)', borderRadius: 2, fontWeight: 600,
                border: '1px solid rgba(255,255,255,0.2)', textTransform: 'none',
                '&:hover': { bgcolor: 'rgba(255,255,255,0.25)' },
              }}
            >
              Bookings
            </Button>
          )}
        </Box>
      </Box>

      {/* ── Stats ────────────────────────────────────────────────────────────── */}
      {summary && (
        <Grid container spacing={2}>
          <StatCard label="Bookings (This Month)" value={summary.totalBookings} color="#2563eb" icon={<LocalShippingIcon fontSize="small" />} />
          {canViewBookings && <StatCard label="Pending Approval" value={totalAllPending} color="#d97706" icon={<HourglassTopIcon fontSize="small" />} />}
          {canViewParties && <StatCard label="Parties Pending" value={totalAllPendingParties} color="#ea580c" icon={<ContactsIcon fontSize="small" />} />}
          {canPrint && <StatCard label="Pending Print" value={totalPendingPrint} color="#7c3aed" icon={<PrintIcon fontSize="small" />} />}
        </Grid>
      )}

      {/* ── Pending to Print — only visible to users with BOOKING_PRINT permission */}
      {canPrint && tasks && totalPendingPrint > 0 && (
        <Card sx={{ border: '1px solid', borderColor: 'warning.light' }}>
          <CardContent>
            <SectionHeader
              icon={<PrintIcon color="warning" />}
              title="Pending to Print"
              count={totalPendingPrint}
            />
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
              Approved bookings — set AWB and print sticker
            </Typography>
            <PendingPrintTable
              rows={tasks.pendingToPrint}
              onView={setViewBooking}
              onSetAwb={openAwbDialog}
              onPrint={printSticker}
              onNavigate={(id) => navigate(`/bookings/${id}/edit?view=1`)}
            />
          </CardContent>
        </Card>
      )}

      {/* ── All Pending Approval Bookings (BOOKING_VIEW users) ───────────────── */}
      {canViewBookings && tasks && tasks.allPendingApprovalBookings.length > 0 && (
        <Card>
          <CardContent>
            <SectionHeader
              icon={<HourglassTopIcon color="warning" />}
              title="Bookings Pending Approval"
              count={totalAllPending}
            />
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
              All pending approval bookings in your company — who created and who needs to approve
            </Typography>
            <Box sx={{ overflowX: 'auto' }}>
              <Table size="small">
                <TableHead>
                  <TableRow sx={{ '& th': { fontWeight: 700, bgcolor: 'grey.100' } }}>
                    <TableCell>Booking No</TableCell>
                    <TableCell>Date</TableCell>
                    <TableCell>Receiver</TableCell>
                    <TableCell>Mode</TableCell>
                    <TableCell>Via</TableCell>
                    <TableCell>Created By</TableCell>
                    <TableCell>Level</TableCell>
                    <TableCell>Pending With (Approver)</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tasks.allPendingApprovalBookings.map((b) => (
                    <TableRow key={b.id} hover>
                      <TableCell>
                        <Stack direction="row" alignItems="center" spacing={0.5}>
                          <Typography
                            variant="body2"
                            fontWeight={600}
                            color="primary.main"
                            sx={{ cursor: 'pointer', textDecoration: 'underline' }}
                            onClick={() => setViewBooking(b)}
                          >
                            {b.bookingNumber}
                          </Typography>
                          <Tooltip title="Open full view">
                            <IconButton size="small" onClick={() => navigate(`/bookings/${b.id}/edit?view=1`)} sx={{ p: 0.2 }}>
                              <OpenInNewIcon sx={{ fontSize: 14 }} />
                            </IconButton>
                          </Tooltip>
                        </Stack>
                      </TableCell>
                      <TableCell>{b.bookingDate}</TableCell>
                      <TableCell>
                        <Box>
                          <Typography variant="body2">{b.receiver?.partyName ?? '—'}</Typography>
                          {b.receiver?.companyName && (
                            <Typography variant="caption" color="text.secondary">{b.receiver.companyName}</Typography>
                          )}
                        </Box>
                      </TableCell>
                      <TableCell>{b.courierMode}</TableCell>
                      <TableCell>{b.courierWay?.name ?? '—'}</TableCell>
                      <TableCell>
                        <Chip size="small" label={b.createdBy ?? '—'} variant="outlined" />
                      </TableCell>
                      <TableCell>
                        <Chip size="small" label={`Level ${b.currentApprovalLevel}`} color="info" variant="outlined" />
                      </TableCell>
                      <TableCell>
                        {b.pendingApprovers && b.pendingApprovers.length > 0 ? (
                          <Stack spacing={0.3}>
                            {b.pendingApprovers.map((a, i) => (
                              <Chip key={i} size="small" label={a} color="warning" variant="outlined" />
                            ))}
                          </Stack>
                        ) : (
                          <Typography variant="caption" color="text.secondary">—</Typography>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Box>
          </CardContent>
        </Card>
      )}

      {/* ── All Pending Approval Parties (MASTER_VIEW users) ────────────────── */}
      {canViewParties && tasks && totalAllPendingParties > 0 && (
        <Card>
          <CardContent>
            <SectionHeader
              icon={<ContactsIcon color="warning" />}
              title="Parties Pending Approval"
              count={totalAllPendingParties}
            />
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
              All pending approval parties in your company — who created and who needs to approve
            </Typography>
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
                    <TableCell>Pending With (Approver)</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tasks.allPendingApprovalParties.map((p) => (
                    <TableRow key={p.id} hover
                      sx={{ cursor: 'pointer' }}
                      onClick={() => navigate('/master/parties')}
                    >
                      <TableCell sx={{ color: 'primary.main', fontWeight: 600 }}>{p.partyCode}</TableCell>
                      <TableCell>{p.partyName}</TableCell>
                      <TableCell>{p.companyName ?? '—'}</TableCell>
                      <TableCell>{p.city}</TableCell>
                      <TableCell>{p.partyType}</TableCell>
                      <TableCell>
                        <Chip size="small" label={p.createdBy ?? '—'} variant="outlined" />
                      </TableCell>
                      <TableCell>
                        <Chip size="small" label={`Level ${p.currentApprovalLevel}`} color="info" variant="outlined" />
                      </TableCell>
                      <TableCell>
                        {p.pendingApprovers && p.pendingApprovers.length > 0 ? (
                          <Stack spacing={0.3}>
                            {p.pendingApprovers.map((a, i) => (
                              <Chip key={i} size="small" label={a} color="warning" variant="outlined" />
                            ))}
                          </Stack>
                        ) : (
                          <Typography variant="caption" color="text.secondary">—</Typography>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Box>
          </CardContent>
        </Card>
      )}

      {/* ── Approval Tasks ───────────────────────────────────────────────────── */}
      {tasks && (
        <Grid container spacing={2}>
          {/* Left: Items waiting for MY approval */}
          <Grid item xs={12} lg={6}>
            <Card>
              <CardContent>
                <SectionHeader
                  icon={<HourglassTopIcon color={totalMyApprovals > 0 ? 'warning' : 'action'} />}
                  title="Pending My Approval"
                  count={totalMyApprovals}
                />

                {(tasks.bookingsPendingMyApproval.length > 0 || tasks.partiesPendingMyApproval.length > 0) ? (
                  <>
                    {tasks.bookingsPendingMyApproval.length > 0 && (
                      <>
                        <Typography variant="caption" color="text.secondary" sx={{ mt: 2, mb: 0.5, display: 'block' }}>
                          BOOKINGS
                        </Typography>
                        <BookingTaskTable
                          rows={tasks.bookingsPendingMyApproval}
                          showActions
                          onView={setViewBooking}
                          onApprove={openApprove}
                          onReject={openReject}
                          onNavigate={(id) => navigate(`/bookings/${id}/edit?view=1`)}
                        />
                      </>
                    )}
                    {tasks.partiesPendingMyApproval.length > 0 && (
                      <>
                        {tasks.bookingsPendingMyApproval.length > 0 && <Divider sx={{ my: 1.5 }} />}
                        <Typography variant="caption" color="text.secondary" sx={{ mt: 2, mb: 0.5, display: 'block' }}>
                          PARTIES
                        </Typography>
                        <PartyTaskTable
                          rows={tasks.partiesPendingMyApproval}
                          onNavigate={() => navigate('/master/parties')}
                          showPendingWith
                        />
                      </>
                    )}
                  </>
                ) : (
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                    Nothing waiting for your approval
                  </Typography>
                )}
              </CardContent>
            </Card>
          </Grid>

          {/* Right: Items I sent, waiting for someone else's approval */}
          <Grid item xs={12} lg={6}>
            <Card>
              <CardContent>
                <SectionHeader
                  icon={<SendIcon color={totalMySent > 0 ? 'info' : 'action'} />}
                  title="Sent for Approval (awaiting)"
                  count={totalMySent}
                />

                {(tasks.myBookingsPendingSent.length > 0 || tasks.myPartiesPendingSent.length > 0) ? (
                  <>
                    {tasks.myBookingsPendingSent.length > 0 && (
                      <>
                        <Typography variant="caption" color="text.secondary" sx={{ mt: 2, mb: 0.5, display: 'block' }}>
                          BOOKINGS
                        </Typography>
                        <BookingTaskTable
                          rows={tasks.myBookingsPendingSent}
                          showPendingWith
                          onView={setViewBooking}
                          onNavigate={(id) => navigate(`/bookings/${id}/edit?view=1`)}
                        />
                      </>
                    )}
                    {tasks.myPartiesPendingSent.length > 0 && (
                      <>
                        {tasks.myBookingsPendingSent.length > 0 && <Divider sx={{ my: 1.5 }} />}
                        <Typography variant="caption" color="text.secondary" sx={{ mt: 2, mb: 0.5, display: 'block' }}>
                          PARTIES
                        </Typography>
                        <PartyTaskTable
                          rows={tasks.myPartiesPendingSent}
                          onNavigate={() => navigate('/master/parties')}
                          showPendingWith
                        />
                      </>
                    )}
                  </>
                ) : (
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                    No pending documents sent by you
                  </Typography>
                )}
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* ── Quick Access ─────────────────────────────────────────────────────── */}
      <Typography variant="h6">Quick Access</Typography>
      <Grid container spacing={2}>
        {tiles.map((tile) => (
          <Grid item xs={12} sm={6} md={3} key={tile.to}>
            <Card>
              <CardActionArea onClick={() => navigate(tile.to)}>
                <CardContent sx={{ textAlign: 'center', py: 4 }}>
                  <Stack alignItems="center" spacing={1} color="primary.main">
                    {tile.icon}
                    <Typography variant="subtitle1" color="text.primary">{tile.label}</Typography>
                  </Stack>
                </CardContent>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* ── Booking Detail Dialog ─────────────────────────────────────────────── */}
      <Dialog open={Boolean(viewBooking)} onClose={() => setViewBooking(null)} maxWidth="md" fullWidth>
        {viewBooking && (
          <>
            <DialogTitle>
              <Stack direction="row" alignItems="center" spacing={1}>
                <Typography variant="h6">{viewBooking.bookingNumber}</Typography>
                <Chip size="small"
                  label={viewBooking.status.replace(/_/g, ' ')}
                  color={BOOKING_STATUS_COLORS[viewBooking.status] ?? 'default'} />
                {viewBooking.printTaken && <Chip size="small" label="Printed" color="success" />}
              </Stack>
            </DialogTitle>
            <DialogContent dividers>
              <Stack spacing={2}>
                <Typography variant="subtitle2" color="primary">TO (Receiver)</Typography>
                <Grid container spacing={2}>
                  <Grid item xs={6} sm={4}><ReadOnlyField label="Party Name" value={viewBooking.receiver?.partyName} /></Grid>
                  <Grid item xs={6} sm={4}><ReadOnlyField label="Company" value={viewBooking.receiver?.companyName} /></Grid>
                  <Grid item xs={6} sm={4}><ReadOnlyField label="City" value={viewBooking.receiver?.city} /></Grid>
                  <Grid item xs={6} sm={4}><ReadOnlyField label="State" value={viewBooking.receiver?.state} /></Grid>
                  <Grid item xs={6} sm={4}><ReadOnlyField label="Pincode" value={viewBooking.receiver?.pincode} /></Grid>
                  <Grid item xs={6} sm={4}><ReadOnlyField label="Phone" value={viewBooking.receiver?.phone} /></Grid>
                </Grid>

                <Divider />

                <Typography variant="subtitle2" color="primary">Shipment Details</Typography>
                <Grid container spacing={2}>
                  <Grid item xs={6} sm={3}><ReadOnlyField label="Date" value={viewBooking.bookingDate} /></Grid>
                  <Grid item xs={6} sm={3}><ReadOnlyField label="Mode" value={viewBooking.courierMode} /></Grid>
                  <Grid item xs={6} sm={3}><ReadOnlyField label="Courier Way" value={viewBooking.courierWay?.name} /></Grid>
                  <Grid item xs={6} sm={3}><ReadOnlyField label="Package Type" value={viewBooking.packageType?.name} /></Grid>
                  <Grid item xs={6} sm={3}><ReadOnlyField label="Weight (kg)" value={viewBooking.weightKg} /></Grid>
                  <Grid item xs={6} sm={3}><ReadOnlyField label="No. of Packages" value={viewBooking.noOfPackages} /></Grid>
                  <Grid item xs={12} sm={6}><ReadOnlyField label="Item Description" value={viewBooking.itemDescription} /></Grid>
                  <Grid item xs={6} sm={3}><ReadOnlyField label="Created By" value={viewBooking.createdBy} /></Grid>
                  {viewBooking.awbNumber && (
                    <Grid item xs={6} sm={3}><ReadOnlyField label="AWB Number" value={viewBooking.awbNumber} /></Grid>
                  )}
                </Grid>

                {viewBooking.approverUsername && (
                  <>
                    <Divider />
                    <Typography variant="subtitle2" color="primary">Approval Info</Typography>
                    <Grid container spacing={2}>
                      <Grid item xs={6}><ReadOnlyField label="Approved By" value={viewBooking.approverUsername} /></Grid>
                      {viewBooking.approvalRemarks && (
                        <Grid item xs={6}><ReadOnlyField label="Remarks" value={viewBooking.approvalRemarks} /></Grid>
                      )}
                    </Grid>
                  </>
                )}

                {viewBooking.pendingApprovers && viewBooking.pendingApprovers.length > 0 && (
                  <>
                    <Divider />
                    <Box>
                      <Typography variant="caption" color="text.secondary">Approval Pending With</Typography>
                      <Stack direction="row" spacing={1} flexWrap="wrap" sx={{ mt: 0.5 }}>
                        {viewBooking.pendingApprovers.map((a, i) => (
                          <Chip key={i} size="small" label={a} color="warning" variant="outlined" />
                        ))}
                      </Stack>
                    </Box>
                  </>
                )}
              </Stack>
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setViewBooking(null)}>Close</Button>
              <Button
                variant="outlined"
                onClick={() => { setViewBooking(null); navigate(`/bookings/${viewBooking.id}/edit?view=1`); }}
                startIcon={<OpenInNewIcon />}
              >
                Full View
              </Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* ── Approve / Reject Dialog ───────────────────────────────────────────── */}
      <Dialog open={Boolean(actionBooking)} onClose={closeAction} maxWidth="xs" fullWidth>
        {actionBooking && (
          <>
            <DialogTitle>
              {actionBooking.type === 'approve' ? 'Approve Booking' : 'Reject Booking'}
            </DialogTitle>
            <DialogContent>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                {actionBooking.booking.bookingNumber} — {actionBooking.booking.receiver?.partyName}
              </Typography>
              <TextField
                label={actionBooking.type === 'reject' ? 'Remarks (required)' : 'Remarks (optional)'}
                value={remarks}
                onChange={(e) => setRemarks(e.target.value)}
                fullWidth
                multiline
                minRows={2}
                sx={{ mt: 1 }}
                autoFocus
              />
            </DialogContent>
            <DialogActions>
              <Button onClick={closeAction} disabled={actionLoading}>Cancel</Button>
              <Button
                variant="contained"
                color={actionBooking.type === 'approve' ? 'success' : 'error'}
                onClick={confirmAction}
                disabled={actionLoading}
              >
                {actionLoading ? 'Processing…' : actionBooking.type === 'approve' ? 'Approve' : 'Reject'}
              </Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* ── AWB Update Dialog ─────────────────────────────────────────────────── */}
      <Dialog open={Boolean(awbBooking)} onClose={closeAwbDialog} maxWidth="xs" fullWidth>
        {awbBooking && (
          <>
            <DialogTitle>Set AWB Number</DialogTitle>
            <DialogContent>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                {awbBooking.bookingNumber} — {awbBooking.receiver?.partyName}
              </Typography>
              <TextField
                label="AWB Number"
                value={awbValue}
                onChange={(e) => setAwbValue(e.target.value)}
                fullWidth
                sx={{ mt: 1 }}
                autoFocus
                onKeyDown={(e) => e.key === 'Enter' && saveAwb()}
              />
            </DialogContent>
            <DialogActions>
              <Button onClick={closeAwbDialog} disabled={awbLoading}>Cancel</Button>
              <Button variant="contained" onClick={saveAwb} disabled={awbLoading || !awbValue.trim()}>
                {awbLoading ? 'Saving…' : 'Save AWB'}
              </Button>
            </DialogActions>
          </>
        )}
      </Dialog>
    </Stack>
  );
}
