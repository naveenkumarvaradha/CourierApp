import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Chip,
  IconButton,
  MenuItem,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import VisibilityIcon from '@mui/icons-material/Visibility';
import EditIcon from '@mui/icons-material/Edit';
import PrintIcon from '@mui/icons-material/Print';
import DeleteIcon from '@mui/icons-material/Delete';
import { dcApi } from '../../api/endpoints';
import { extractBlobError } from '../../api/client';
import { useSearchDcsQuery, useDeleteDcMutation } from '../../store/api/dcApiSlice';
import { useNotification } from '../../context/NotificationContext';
import { useAuth } from '../../context/AuthContext';
import type { DcStatus, DeliveryChallan } from '../../types';

const STATUS_COLORS: Record<DcStatus, 'default' | 'info' | 'success'> = {
  DRAFT: 'default',
  ISSUED: 'info',
  DELIVERED: 'success',
};

export default function DcListPage() {
  const { notify } = useNotification();
  const { hasPermission } = useAuth();
  const navigate = useNavigate();
  const [filters, setFilters] = useState({ dcNumber: '', status: '', fromDate: '', toDate: '' });

  const queryParams = {
    page: 0, size: 100,
    ...(filters.dcNumber && { dcNumber: filters.dcNumber }),
    ...(filters.status && { status: filters.status }),
    ...(filters.fromDate && { fromDate: filters.fromDate }),
    ...(filters.toDate && { toDate: filters.toDate }),
  };
  const { data: dcPage, isFetching: loading } = useSearchDcsQuery(queryParams);
  const rows: DeliveryChallan[] = dcPage?.content ?? [];
  const [deleteDc] = useDeleteDcMutation();

  const print = useCallback(async (dc: DeliveryChallan) => {
    try {
      const blob = await dcApi.fetchPrint(dc.id);
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank');
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch (err) {
      const msg = await extractBlobError(err);
      notify(msg, 'error');
    }
  }, [notify]);

  const remove = useCallback(async (id: number) => {
    if (!window.confirm('Delete this delivery challan?')) return;
    try {
      await deleteDc(id).unwrap();
      notify('Deleted', 'success');
    } catch (err) {
      notify('Failed to delete delivery challan', 'error');
    }
  }, [deleteDc, notify]);

  const columns: GridColDef<DeliveryChallan>[] = [
    { field: 'dcNumber', headerName: 'DC No.', width: 180 },
    { field: 'dcDate', headerName: 'Date', width: 120 },
    {
      field: 'bookingNumber', headerName: 'Booking No.', width: 180, sortable: false,
      valueGetter: (_v, row) => row.booking.bookingNumber,
    },
    {
      field: 'receiver', headerName: 'Receiver', flex: 1, sortable: false,
      valueGetter: (_v, row) => row.booking.receiver.partyName,
    },
    {
      field: 'unit', headerName: 'Unit', flex: 1, sortable: false,
      valueGetter: (_v, row) => row.unit.unitName,
    },
    {
      field: 'status', headerName: 'Status', width: 130,
      renderCell: (p) => <Chip size="small" label={p.row.status} color={STATUS_COLORS[p.row.status]} />,
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 150,
      sortable: false,
      renderCell: (params) => (
        <Stack direction="row">
          <Tooltip title="View">
            <IconButton size="small" onClick={() => navigate(`/dc/${params.row.id}/edit?view=1`)}>
              <VisibilityIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          {hasPermission('DELIVERY_CHALLAN_UPDATE') && (
            <Tooltip title="Edit">
              <IconButton size="small" onClick={() => navigate(`/dc/${params.row.id}/edit`)}>
                <EditIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
          {hasPermission('DELIVERY_CHALLAN_PRINT') && (
            <Tooltip title="Print">
              <IconButton size="small" color="primary" onClick={() => print(params.row)}>
                <PrintIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
          {hasPermission('DELIVERY_CHALLAN_DELETE') && (
            <Tooltip title="Delete">
              <IconButton size="small" color="error" onClick={() => remove(params.row.id)}>
                <DeleteIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
        </Stack>
      ),
    },
  ];

  return (
    <Stack spacing={2}>
      <Typography variant="h5" fontWeight={600}>Delivery Challans</Typography>

      <Stack direction="row" spacing={2} flexWrap="wrap">
        <TextField label="DC No." size="small" value={filters.dcNumber}
          onChange={(e) => setFilters({ ...filters, dcNumber: e.target.value })} />
        <TextField select label="Status" size="small" sx={{ minWidth: 140 }} value={filters.status}
          onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
          <MenuItem value="">All</MenuItem>
          <MenuItem value="DRAFT">Draft</MenuItem>
          <MenuItem value="ISSUED">Issued</MenuItem>
          <MenuItem value="DELIVERED">Delivered</MenuItem>
        </TextField>
        <TextField label="From Date" type="date" size="small" InputLabelProps={{ shrink: true }}
          value={filters.fromDate} onChange={(e) => setFilters({ ...filters, fromDate: e.target.value })} />
        <TextField label="To Date" type="date" size="small" InputLabelProps={{ shrink: true }}
          value={filters.toDate} onChange={(e) => setFilters({ ...filters, toDate: e.target.value })} />
      </Stack>

      <Box sx={{ height: 500, bgcolor: 'background.paper' }}>
        <DataGrid rows={rows} columns={columns} loading={loading} disableRowSelectionOnClick
          pageSizeOptions={[10, 25, 50]} initialState={{ pagination: { paginationModel: { pageSize: 25 } } }} />
      </Box>
    </Stack>
  );
}
