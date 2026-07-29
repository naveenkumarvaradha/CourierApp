import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { DataGrid, GridColDef, GridToolbarColumnsButton, GridToolbarContainer } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import VisibilityIcon from '@mui/icons-material/Visibility';
import PrintIcon from '@mui/icons-material/Print';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import SendIcon from '@mui/icons-material/Send';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import EditNoteIcon from '@mui/icons-material/EditNote';
import CancelIcon from '@mui/icons-material/Cancel';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import { bookingApi } from '../../api/endpoints';
import { extractErrorMessage, extractBlobError } from '../../api/client';
import {
  useSearchBookingsQuery,
  useSubmitBookingMutation,
  useApproveBookingMutation,
  useRejectBookingMutation,
  useUpdateAwbMutation,
  useReviseBookingMutation,
  useRequestCancellationMutation,
  useApproveCancellationMutation,
  useRejectCancellationMutation,
} from '../../store/api/bookingApiSlice';
import { useNotification } from '../../context/NotificationContext';
import { useAuth } from '../../context/AuthContext';
import { useColumnVisibility } from '../../hooks/useColumnVisibility';
import type { Booking, BookingStatus } from '../../types';

const STATUS_COLORS: Record<BookingStatus, 'default' | 'info' | 'warning' | 'success' | 'error'> = {
  BOOKED: 'info',
  PENDING_APPROVAL: 'warning',
  APPROVED: 'success',
  IN_TRANSIT: 'info',
  DELIVERED: 'success',
  CANCELLED: 'default',
  REJECTED: 'error',
  PENDING_CANCELLATION: 'warning',
};

const PRINTABLE_STATUSES: BookingStatus[] = ['APPROVED', 'IN_TRANSIT', 'DELIVERED'];

function CustomToolbar() {
  return (
    <GridToolbarContainer>
      <GridToolbarColumnsButton />
    </GridToolbarContainer>
  );
}

export default function BookingsPage() {
  const { notify } = useNotification();
  const { hasPermission, user } = useAuth();
  const navigate = useNavigate();
  const [filters, setFilters] = useState({
    bookingNumber: '',
    receiverName: '',
    receiverCompanyName: '',
    fromDate: '',
    toDate: '',
    status: '',
    mode: '',
  });

  const [colVisibility, setColVisibility] = useColumnVisibility(user?.id, 'bookings');

  // ── RTK Query ──────────────────────────────────────────────────────────────
  const queryParams = {
    page: 0, size: 100,
    ...(filters.bookingNumber && { bookingNumber: filters.bookingNumber }),
    ...(filters.receiverName && { receiverName: filters.receiverName }),
    ...(filters.receiverCompanyName && { receiverCompanyName: filters.receiverCompanyName }),
    ...(filters.fromDate && { fromDate: filters.fromDate }),
    ...(filters.toDate && { toDate: filters.toDate }),
    ...(filters.status && { status: filters.status }),
    ...(filters.mode && { mode: filters.mode }),
  };
  const { data: bookingPage, isFetching: loading } = useSearchBookingsQuery(queryParams);
  const rows: Booking[] = bookingPage?.content ?? [];

  const [submitBooking]      = useSubmitBookingMutation();
  const [approveBooking]     = useApproveBookingMutation();
  const [rejectBooking]      = useRejectBookingMutation();
  const [updateAwb]          = useUpdateAwbMutation();
  const [reviseBooking]      = useReviseBookingMutation();
  const [requestCancellation] = useRequestCancellationMutation();
  const [approveCancellation] = useApproveCancellationMutation();
  const [rejectCancellation]  = useRejectCancellationMutation();

  // Approve/Reject dialog
  const [decision, setDecision] = useState<{ id: number; action: 'approve' | 'reject' } | null>(null);
  const [remarks, setRemarks] = useState('');

  // AWB dialog
  const [awbDialog, setAwbDialog] = useState<{ id: number; bookingNumber: string } | null>(null);
  const [awbInput, setAwbInput] = useState('');

  // Cancel request dialog
  const [cancelDialog, setCancelDialog] = useState<{ id: number; bookingNumber: string } | null>(null);
  const [cancelRemarks, setCancelRemarks] = useState('');

  // Cancellation approval dialog
  const [cancelApproval, setCancelApproval] = useState<{ id: number; action: 'approve' | 'reject' } | null>(null);

  const printSticker = useCallback(async (b: Booking) => {
    if (!b.awbNumber) {
      notify('Set AWB number before printing', 'warning');
      setAwbDialog({ id: b.id, bookingNumber: b.bookingNumber });
      return;
    }
    try {
      const blob = await bookingApi.fetchSticker(b.id);
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank');
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch (err) {
      const msg = await extractBlobError(err);
      notify(msg, 'error');
    }
  }, [notify]);

  const submit = useCallback(async (id: number) => {
    try {
      await submitBooking(id).unwrap();
      notify('Submitted for approval', 'success');
    } catch (err) { notify(extractErrorMessage(err), 'error'); }
  }, [submitBooking, notify]);

  const revise = useCallback(async (id: number) => {
    try {
      await reviseBooking(id).unwrap();
      notify('Booking reset to BOOKED for revision', 'success');
    } catch (err) { notify(extractErrorMessage(err), 'error'); }
  }, [reviseBooking, notify]);

  const confirmDecision = async () => {
    if (!decision) return;
    try {
      if (decision.action === 'approve') {
        await approveBooking({ id: decision.id, remarks }).unwrap();
        notify('Booking approved', 'success');
      } else {
        await rejectBooking({ id: decision.id, remarks }).unwrap();
        notify('Booking rejected', 'success');
      }
      setDecision(null); setRemarks('');
    } catch (err) { notify(extractErrorMessage(err), 'error'); }
  };

  const saveAwb = async () => {
    if (!awbDialog || !awbInput.trim()) { notify('Enter AWB number', 'warning'); return; }
    try {
      await updateAwb({ id: awbDialog.id, awbNumber: awbInput.trim() }).unwrap();
      notify('AWB saved', 'success');
      setAwbDialog(null); setAwbInput('');
    } catch (err) { notify(extractErrorMessage(err), 'error'); }
  };

  const submitCancelRequest = async () => {
    if (!cancelDialog) return;
    try {
      await requestCancellation({ id: cancelDialog.id, remarks: cancelRemarks || undefined }).unwrap();
      notify('Cancellation request submitted for approval', 'success');
      setCancelDialog(null); setCancelRemarks('');
    } catch (err) { notify(extractErrorMessage(err), 'error'); }
  };

  const confirmCancelApproval = async () => {
    if (!cancelApproval) return;
    try {
      if (cancelApproval.action === 'approve') {
        await approveCancellation(cancelApproval.id).unwrap();
        notify('Cancellation approved — booking cancelled', 'success');
      } else {
        await rejectCancellation(cancelApproval.id).unwrap();
        notify('Cancellation rejected — booking restored to APPROVED', 'success');
      }
      setCancelApproval(null);
    } catch (err) { notify(extractErrorMessage(err), 'error'); }
  };

  const columns: GridColDef<Booking>[] = [
    { field: 'bookingNumber', headerName: 'Booking No', flex: 1.2 },
    { field: 'bookingDate', headerName: 'Date', width: 100 },
    { field: 'receiver', headerName: 'Party Name', flex: 1, valueGetter: (_v, row) => row.receiver?.partyName },
    { field: 'receiverCompany', headerName: 'Company', flex: 1, valueGetter: (_v, row) => row.receiver?.companyName ?? '—' },
    { field: 'courierMode', headerName: 'Mode', width: 80 },
    { field: 'courierWay', headerName: 'Via', width: 100, valueGetter: (_v, row) => row.courierWay?.name ?? '—' },
    { field: 'createdBy', headerName: 'Created By', width: 110, valueGetter: (_v, row) => row.createdBy ?? '—' },
    { field: 'createdAt', headerName: 'Created At', width: 150, valueGetter: (_v, row) => row.createdAt ? new Date(row.createdAt).toLocaleString() : '—' },
    {
      field: 'awbNumber', headerName: 'AWB No', width: 140,
      renderCell: (p) => p.row.awbNumber
        ? <Chip size="small" label={p.row.awbNumber} color="primary" variant="outlined" />
        : <Typography variant="caption" color="text.disabled">—</Typography>,
    },
    {
      field: 'status', headerName: 'Status', width: 170,
      renderCell: (p) => (
        <Stack direction="row" spacing={0.5} alignItems="center">
          <Chip size="small" color={STATUS_COLORS[p.row.status]} label={p.row.status.replace(/_/g, ' ')} />
          {p.row.printTaken && <Chip size="small" label="Printed" color="success" variant="outlined" />}
        </Stack>
      ),
    },
    {
      field: 'actions', headerName: 'Actions', width: 260, sortable: false,
      renderCell: (params) => {
        const b = params.row;
        const canPrint = PRINTABLE_STATUSES.includes(b.status);
        const hasAwb = !!b.awbNumber;
        const canRevise = b.status === 'APPROVED' && !hasAwb && !b.printTaken;
        const canRequestCancel = b.status === 'APPROVED' && !hasAwb;

        return (
          <Stack direction="row" flexWrap="wrap">
            <Tooltip title="View details">
              <IconButton size="small" onClick={() => navigate(`/bookings/${b.id}/edit?view=1`)}>
                <VisibilityIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            {hasPermission('BOOKING_UPDATE') && (b.status === 'BOOKED' || b.status === 'PENDING_APPROVAL') && (
              <Tooltip title="Edit">
                <IconButton size="small" onClick={() => navigate(`/bookings/${b.id}/edit`)}>
                  <EditIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
            {hasPermission('BOOKING_UPDATE') && b.status === 'BOOKED' && (
              <Tooltip title="Submit for approval">
                <IconButton size="small" color="primary" onClick={() => submit(b.id)}>
                  <SendIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
            {hasPermission('BOOKING_APPROVE') && b.status === 'PENDING_APPROVAL' && (
              <>
                <Tooltip title="Approve">
                  <IconButton size="small" color="success" onClick={() => setDecision({ id: b.id, action: 'approve' })}>
                    <CheckIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Reject">
                  <IconButton size="small" color="error" onClick={() => setDecision({ id: b.id, action: 'reject' })}>
                    <CloseIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              </>
            )}
            {(hasPermission('BOOKING_UPDATE') || b.createdBy === user?.username) && canPrint && !hasAwb && (
              <Tooltip title="Set AWB number">
                <IconButton size="small" color="warning"
                  onClick={() => { setAwbDialog({ id: b.id, bookingNumber: b.bookingNumber }); setAwbInput(''); }}>
                  <LocalShippingIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
            {(hasPermission('BOOKING_PRINT') || b.createdBy === user?.username) && canPrint && hasAwb && (
              <Tooltip title={b.printTaken ? 'Reprint sticker' : 'Print sticker'}>
                <IconButton size="small" color={b.printTaken ? 'default' : 'primary'} onClick={() => printSticker(b)}>
                  <PrintIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
            {hasPermission('DELIVERY_CHALLAN_CREATE') && canPrint && (
              <Tooltip title="Create delivery challan">
                <IconButton size="small" onClick={() => navigate(`/dc/new?bookingId=${b.id}`)}>
                  <ReceiptLongIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
            {hasPermission('BOOKING_REVISE') && canRevise && (
              <Tooltip title="Revise booking (reset to BOOKED)">
                <IconButton size="small" color="warning" onClick={() => revise(b.id)}>
                  <EditNoteIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
            {hasPermission('BOOKING_UPDATE') && canRequestCancel && (
              <Tooltip title="Request cancellation">
                <IconButton size="small" color="error"
                  onClick={() => { setCancelDialog({ id: b.id, bookingNumber: b.bookingNumber }); setCancelRemarks(''); }}>
                  <CancelIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
            {hasPermission('BOOKING_APPROVE') && b.status === 'PENDING_CANCELLATION' && (
              <>
                <Tooltip title="Approve cancellation">
                  <IconButton size="small" color="error" onClick={() => setCancelApproval({ id: b.id, action: 'approve' })}>
                    <CheckIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Reject cancellation">
                  <IconButton size="small" color="success" onClick={() => setCancelApproval({ id: b.id, action: 'reject' })}>
                    <CloseIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              </>
            )}
          </Stack>
        );
      },
    },
  ];

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5" fontWeight={600}>Shipments</Typography>
        {hasPermission('BOOKING_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/bookings/new')}>
            New Booking
          </Button>
        )}
      </Stack>

      {/* Filters */}
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} flexWrap="wrap" useFlexGap>
        <TextField size="small" label="Booking No" value={filters.bookingNumber}
          onChange={(e) => setFilters({ ...filters, bookingNumber: e.target.value })} sx={{ minWidth: 150 }} />
        <TextField size="small" label="Party Name" value={filters.receiverName}
          onChange={(e) => setFilters({ ...filters, receiverName: e.target.value })} sx={{ minWidth: 150 }} />
        <TextField size="small" label="Company" value={filters.receiverCompanyName}
          onChange={(e) => setFilters({ ...filters, receiverCompanyName: e.target.value })} sx={{ minWidth: 160 }} />
        <TextField size="small" label="From" type="date" InputLabelProps={{ shrink: true }}
          value={filters.fromDate} onChange={(e) => setFilters({ ...filters, fromDate: e.target.value })} />
        <TextField size="small" label="To" type="date" InputLabelProps={{ shrink: true }}
          value={filters.toDate} onChange={(e) => setFilters({ ...filters, toDate: e.target.value })} />
        <FormControl size="small" sx={{ minWidth: 170 }}>
          <InputLabel>Status</InputLabel>
          <Select label="Status" value={filters.status}
            onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
            <MenuItem value="">All</MenuItem>
            {(Object.keys(STATUS_COLORS) as BookingStatus[]).map((s) => (
              <MenuItem key={s} value={s}>{s.replace(/_/g, ' ')}</MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 120 }}>
          <InputLabel>Mode</InputLabel>
          <Select label="Mode" value={filters.mode}
            onChange={(e) => setFilters({ ...filters, mode: e.target.value })}>
            <MenuItem value="">All</MenuItem>
            <MenuItem value="AIR">Air</MenuItem>
            <MenuItem value="SURFACE">Surface</MenuItem>
            <MenuItem value="EXPRESS">Express</MenuItem>
          </Select>
        </FormControl>
      </Stack>

      <Box sx={{ height: 580, bgcolor: 'background.paper' }}>
        <DataGrid
          rows={rows}
          columns={columns}
          loading={loading}
          disableRowSelectionOnClick
          pageSizeOptions={[10, 25, 50]}
          initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
          columnVisibilityModel={colVisibility}
          onColumnVisibilityModelChange={setColVisibility}
          slots={{ toolbar: CustomToolbar }}
        />
      </Box>

      {/* Approve / Reject booking */}
      <Dialog open={!!decision} onClose={() => setDecision(null)} maxWidth="sm" fullWidth>
        <DialogTitle>{decision?.action === 'approve' ? 'Approve Booking' : 'Reject Booking'}</DialogTitle>
        <DialogContent>
          <TextField label="Remarks" fullWidth multiline minRows={3} sx={{ mt: 1 }}
            value={remarks} onChange={(e) => setRemarks(e.target.value)} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDecision(null)}>Cancel</Button>
          <Button variant="contained" color={decision?.action === 'approve' ? 'success' : 'error'}
            onClick={confirmDecision}>
            {decision?.action === 'approve' ? 'Approve' : 'Reject'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* AWB number dialog */}
      <Dialog open={!!awbDialog} onClose={() => setAwbDialog(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Set AWB Number</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" mb={2}>
            Booking: <strong>{awbDialog?.bookingNumber}</strong>
          </Typography>
          <TextField label="AWB Number" fullWidth autoFocus value={awbInput}
            onChange={(e) => setAwbInput(e.target.value.replace(/\D/g, ''))}
            inputProps={{ maxLength: 20, inputMode: 'numeric' }}
            onKeyDown={(e) => { if (e.key === 'Enter') saveAwb(); }}
            helperText="Numbers only. Must be unique." />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAwbDialog(null)}>Cancel</Button>
          <Button variant="contained" onClick={saveAwb} disabled={!awbInput.trim()}>Save</Button>
        </DialogActions>
      </Dialog>

      {/* Cancel request dialog */}
      <Dialog open={!!cancelDialog} onClose={() => setCancelDialog(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Request Cancellation</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" mb={2}>
            Booking <strong>{cancelDialog?.bookingNumber}</strong> will be sent for cancellation approval.
          </Typography>
          <TextField label="Cancellation Reason" fullWidth multiline minRows={3}
            value={cancelRemarks} onChange={(e) => setCancelRemarks(e.target.value)} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCancelDialog(null)}>Back</Button>
          <Button variant="contained" color="error" onClick={submitCancelRequest}>Submit for Approval</Button>
        </DialogActions>
      </Dialog>

      {/* Cancellation approve/reject dialog */}
      <Dialog open={!!cancelApproval} onClose={() => setCancelApproval(null)} maxWidth="xs" fullWidth>
        <DialogTitle>
          {cancelApproval?.action === 'approve' ? 'Approve Cancellation' : 'Reject Cancellation'}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2">
            {cancelApproval?.action === 'approve'
              ? 'Confirm to permanently cancel this booking.'
              : 'Reject the cancellation request — booking will be restored to APPROVED.'}
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCancelApproval(null)}>Back</Button>
          <Button variant="contained"
            color={cancelApproval?.action === 'approve' ? 'error' : 'success'}
            onClick={confirmCancelApproval}>
            {cancelApproval?.action === 'approve' ? 'Cancel Booking' : 'Reject & Restore'}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
