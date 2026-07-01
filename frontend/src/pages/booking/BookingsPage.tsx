import { useCallback, useEffect, useState } from 'react';
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
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import PrintIcon from '@mui/icons-material/Print';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import SendIcon from '@mui/icons-material/Send';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import { bookingApi } from '../../api/endpoints';
import { extractErrorMessage } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';
import { useAuth } from '../../context/AuthContext';
import type { Booking, BookingStatus } from '../../types';

const STATUS_COLORS: Record<BookingStatus, 'default' | 'info' | 'warning' | 'success' | 'error'> = {
  BOOKED: 'info',
  PENDING_APPROVAL: 'warning',
  APPROVED: 'success',
  IN_TRANSIT: 'info',
  DELIVERED: 'success',
  CANCELLED: 'default',
  REJECTED: 'error',
};

const PRINTABLE_STATUSES: BookingStatus[] = ['APPROVED', 'IN_TRANSIT', 'DELIVERED'];

export default function BookingsPage() {
  const { notify } = useNotification();
  const { hasPermission } = useAuth();
  const navigate = useNavigate();
  const [rows, setRows] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState({
    bookingNumber: '',
    fromDate: '',
    toDate: '',
    status: '',
    mode: '',
  });

  // Approve/Reject dialog
  const [decision, setDecision] = useState<{ id: number; action: 'approve' | 'reject' } | null>(null);
  const [remarks, setRemarks] = useState('');

  // AWB dialog
  const [awbDialog, setAwbDialog] = useState<{ id: number; bookingNumber: string; current: string | null } | null>(null);
  const [awbInput, setAwbInput] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, unknown> = { page: 0, size: 100 };
      Object.entries(filters).forEach(([k, v]) => { if (v) params[k] = v; });
      const data = await bookingApi.search(params);
      setRows(data.content);
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    } finally {
      setLoading(false);
    }
  }, [filters, notify]);

  useEffect(() => { load(); }, [load]);

  const printSticker = async (b: Booking) => {
    if (!PRINTABLE_STATUSES.includes(b.status)) {
      notify('Sticker can only be printed for APPROVED bookings', 'warning');
      return;
    }
    if (!b.awbNumber) {
      notify('Set the AWB number before printing the sticker', 'warning');
      setAwbDialog({ id: b.id, bookingNumber: b.bookingNumber, current: null });
      return;
    }
    try {
      const blob = await bookingApi.fetchSticker(b.id);
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank');
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const submit = async (id: number) => {
    try {
      await bookingApi.submit(id);
      notify('Submitted for approval', 'success');
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const confirmDecision = async () => {
    if (!decision) return;
    try {
      if (decision.action === 'approve') {
        await bookingApi.approve(decision.id, remarks);
        notify('Booking approved', 'success');
      } else {
        await bookingApi.reject(decision.id, remarks);
        notify('Booking rejected', 'success');
      }
      setDecision(null);
      setRemarks('');
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const saveAwb = async () => {
    if (!awbDialog || !awbInput.trim()) {
      notify('Enter AWB number', 'warning');
      return;
    }
    try {
      await bookingApi.updateAwb(awbDialog.id, awbInput.trim());
      notify('AWB number saved', 'success');
      setAwbDialog(null);
      setAwbInput('');
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const columns: GridColDef<Booking>[] = [
    { field: 'bookingNumber', headerName: 'Booking No', flex: 1.2 },
    { field: 'bookingDate', headerName: 'Date', width: 110 },
    {
      field: 'sender',
      headerName: 'Sender',
      flex: 1,
      valueGetter: (_v, row) => row.sender?.partyName,
    },
    {
      field: 'receiver',
      headerName: 'Receiver',
      flex: 1,
      valueGetter: (_v, row) => row.receiver?.partyName,
    },
    { field: 'courierMode', headerName: 'Mode', width: 90 },
    {
      field: 'courierWay',
      headerName: 'Via',
      width: 100,
      valueGetter: (_v, row) => row.courierWay?.name ?? '—',
    },
    {
      field: 'awbNumber',
      headerName: 'AWB No',
      width: 140,
      renderCell: (p) =>
        p.row.awbNumber ? (
          <Chip size="small" label={p.row.awbNumber} color="primary" variant="outlined" />
        ) : (
          <Typography variant="caption" color="text.disabled">—</Typography>
        ),
    },
    {
      field: 'status',
      headerName: 'Status',
      width: 150,
      renderCell: (p) => (
        <Chip size="small" color={STATUS_COLORS[p.row.status]} label={p.row.status.replace('_', ' ')} />
      ),
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 230,
      sortable: false,
      renderCell: (params) => {
        const b = params.row;
        const canPrint = PRINTABLE_STATUSES.includes(b.status);
        const needsAwb = canPrint && !b.awbNumber;
        return (
          <Stack direction="row">
            {/* Set AWB — only for APPROVED+ without AWB */}
            {hasPermission('BOOKING_UPDATE') && needsAwb && (
              <Tooltip title="Set AWB number (required before print)">
                <IconButton
                  size="small"
                  color="warning"
                  onClick={() => {
                    setAwbDialog({ id: b.id, bookingNumber: b.bookingNumber, current: b.awbNumber });
                    setAwbInput('');
                  }}
                >
                  <LocalShippingIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}

            {/* Print sticker — only when APPROVED+ AND AWB set */}
            {canPrint && b.awbNumber && (
              <Tooltip title="Print sticker">
                <IconButton size="small" onClick={() => printSticker(b)}>
                  <PrintIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}

            {/* Edit */}
            {hasPermission('BOOKING_UPDATE') &&
              (b.status === 'BOOKED' || b.status === 'PENDING_APPROVAL') && (
                <Tooltip title="Edit">
                  <IconButton size="small" onClick={() => navigate(`/bookings/${b.id}/edit`)}>
                    <EditIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              )}

            {/* Submit for approval */}
            {hasPermission('BOOKING_UPDATE') && b.status === 'BOOKED' && (
              <Tooltip title="Submit for approval">
                <IconButton size="small" color="primary" onClick={() => submit(b.id)}>
                  <SendIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}

            {/* Approve / Reject */}
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
          </Stack>
        );
      },
    },
  ];

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5" fontWeight={600}>Courier Bookings</Typography>
        {hasPermission('BOOKING_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/bookings/new')}>
            New Booking
          </Button>
        )}
      </Stack>

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} flexWrap="wrap">
        <TextField size="small" label="Booking No" value={filters.bookingNumber}
          onChange={(e) => setFilters({ ...filters, bookingNumber: e.target.value })} />
        <TextField size="small" label="From" type="date" InputLabelProps={{ shrink: true }}
          value={filters.fromDate} onChange={(e) => setFilters({ ...filters, fromDate: e.target.value })} />
        <TextField size="small" label="To" type="date" InputLabelProps={{ shrink: true }}
          value={filters.toDate} onChange={(e) => setFilters({ ...filters, toDate: e.target.value })} />
        <FormControl size="small" sx={{ minWidth: 150 }}>
          <InputLabel>Status</InputLabel>
          <Select label="Status" value={filters.status}
            onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
            <MenuItem value="">All</MenuItem>
            {Object.keys(STATUS_COLORS).map((s) => (
              <MenuItem key={s} value={s}>{s.replace('_', ' ')}</MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 130 }}>
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

      <Box sx={{ height: 560, bgcolor: 'background.paper' }}>
        <DataGrid rows={rows} columns={columns} loading={loading} disableRowSelectionOnClick
          pageSizeOptions={[10, 25, 50]}
          initialState={{ pagination: { paginationModel: { pageSize: 25 } } }} />
      </Box>

      {/* Approve / Reject dialog */}
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
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Booking: <strong>{awbDialog?.bookingNumber}</strong>
            <br />
            Enter the unique Air Waybill number. This will be printed on the shipping sticker.
          </Typography>
          <TextField
            label="AWB Number"
            fullWidth
            autoFocus
            value={awbInput}
            onChange={(e) => setAwbInput(e.target.value.replace(/\D/g, ''))}
            inputProps={{ maxLength: 20, inputMode: 'numeric' }}
            onKeyDown={(e) => { if (e.key === 'Enter') saveAwb(); }}
            helperText="Numbers only. Must be unique across all bookings."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAwbDialog(null)}>Cancel</Button>
          <Button variant="contained" color="primary" onClick={saveAwb} disabled={!awbInput.trim()}>
            Save & Print
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
