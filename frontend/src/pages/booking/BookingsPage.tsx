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
  const [decision, setDecision] = useState<{ id: number; action: 'approve' | 'reject' } | null>(null);
  const [remarks, setRemarks] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, unknown> = { page: 0, size: 100 };
      Object.entries(filters).forEach(([k, v]) => {
        if (v) params[k] = v;
      });
      const data = await bookingApi.search(params);
      setRows(data.content);
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    } finally {
      setLoading(false);
    }
  }, [filters, notify]);

  useEffect(() => {
    load();
  }, [load]);

  const printSticker = async (id: number) => {
    try {
      const blob = await bookingApi.fetchSticker(id);
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
    { field: 'courierMode', headerName: 'Mode', width: 100 },
    { field: 'totalCharges', headerName: 'Charges', width: 100 },
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
      width: 210,
      sortable: false,
      renderCell: (params) => {
        const b = params.row;
        return (
          <Stack direction="row">
            <Tooltip title="Print sticker">
              <IconButton size="small" onClick={() => printSticker(b.id)}>
                <PrintIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            {hasPermission('BOOKING_UPDATE') &&
              (b.status === 'BOOKED' || b.status === 'PENDING_APPROVAL') && (
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
                  <IconButton
                    size="small"
                    color="success"
                    onClick={() => setDecision({ id: b.id, action: 'approve' })}
                  >
                    <CheckIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Reject">
                  <IconButton
                    size="small"
                    color="error"
                    onClick={() => setDecision({ id: b.id, action: 'reject' })}
                  >
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
        <Typography variant="h5" fontWeight={600}>
          Courier Bookings
        </Typography>
        {hasPermission('BOOKING_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/bookings/new')}>
            New Booking
          </Button>
        )}
      </Stack>

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} flexWrap="wrap">
        <TextField
          size="small"
          label="Booking No"
          value={filters.bookingNumber}
          onChange={(e) => setFilters({ ...filters, bookingNumber: e.target.value })}
        />
        <TextField
          size="small"
          label="From"
          type="date"
          InputLabelProps={{ shrink: true }}
          value={filters.fromDate}
          onChange={(e) => setFilters({ ...filters, fromDate: e.target.value })}
        />
        <TextField
          size="small"
          label="To"
          type="date"
          InputLabelProps={{ shrink: true }}
          value={filters.toDate}
          onChange={(e) => setFilters({ ...filters, toDate: e.target.value })}
        />
        <FormControl size="small" sx={{ minWidth: 150 }}>
          <InputLabel>Status</InputLabel>
          <Select
            label="Status"
            value={filters.status}
            onChange={(e) => setFilters({ ...filters, status: e.target.value })}
          >
            <MenuItem value="">All</MenuItem>
            {Object.keys(STATUS_COLORS).map((s) => (
              <MenuItem key={s} value={s}>
                {s.replace('_', ' ')}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 130 }}>
          <InputLabel>Mode</InputLabel>
          <Select
            label="Mode"
            value={filters.mode}
            onChange={(e) => setFilters({ ...filters, mode: e.target.value })}
          >
            <MenuItem value="">All</MenuItem>
            <MenuItem value="AIR">Air</MenuItem>
            <MenuItem value="SURFACE">Surface</MenuItem>
            <MenuItem value="EXPRESS">Express</MenuItem>
          </Select>
        </FormControl>
      </Stack>

      <Box sx={{ height: 560, bgcolor: 'background.paper' }}>
        <DataGrid
          rows={rows}
          columns={columns}
          loading={loading}
          disableRowSelectionOnClick
          pageSizeOptions={[10, 25, 50]}
          initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
        />
      </Box>

      <Dialog open={!!decision} onClose={() => setDecision(null)} maxWidth="sm" fullWidth>
        <DialogTitle>
          {decision?.action === 'approve' ? 'Approve Booking' : 'Reject Booking'}
        </DialogTitle>
        <DialogContent>
          <TextField
            label="Remarks"
            fullWidth
            multiline
            minRows={3}
            sx={{ mt: 1 }}
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDecision(null)}>Cancel</Button>
          <Button
            variant="contained"
            color={decision?.action === 'approve' ? 'success' : 'error'}
            onClick={confirmDecision}
          >
            {decision?.action === 'approve' ? 'Approve' : 'Reject'}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
