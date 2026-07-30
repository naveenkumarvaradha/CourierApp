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
  IconButton,
  MenuItem,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import VisibilityIcon from '@mui/icons-material/Visibility';
import EditIcon from '@mui/icons-material/Edit';
import PrintIcon from '@mui/icons-material/Print';
import DeleteIcon from '@mui/icons-material/Delete';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import SendIcon from '@mui/icons-material/Send';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import DoneAllIcon from '@mui/icons-material/DoneAll';
import { dcApi } from '../../api/endpoints';
import { extractBlobError, extractErrorMessage } from '../../api/client';
import {
  useSearchDcsQuery,
  useDeleteDcMutation,
  useSubmitDcMutation,
  useApproveDcMutation,
  useRejectDcMutation,
  useChangeDcStatusMutation,
} from '../../store/api/dcApiSlice';
import { useNotification } from '../../context/NotificationContext';
import { useAuth } from '../../context/AuthContext';
import type { DcStatus, DeliveryChallan } from '../../types';

const STATUS_COLORS: Record<DcStatus, 'default' | 'info' | 'warning' | 'success' | 'error'> = {
  DRAFT: 'default',
  PENDING_APPROVAL: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
  ISSUED: 'info',
  DELIVERED: 'success',
  RETURNED: 'default',
};

const EDITABLE_STATUSES: DcStatus[] = ['DRAFT', 'PENDING_APPROVAL'];

export default function DcListPage() {
  const { notify } = useNotification();
  const { hasPermission } = useAuth();
  const navigate = useNavigate();
  const [filters, setFilters] = useState({ dcNumber: '', status: '', fromDate: '', toDate: '' });
  const [decision, setDecision] = useState<{ id: number; action: 'approve' | 'reject' } | null>(null);
  const [remarks, setRemarks] = useState('');

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
  const [submitDc] = useSubmitDcMutation();
  const [approveDc] = useApproveDcMutation();
  const [rejectDc] = useRejectDcMutation();
  const [changeDcStatus] = useChangeDcStatusMutation();

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

  const submit = useCallback(async (id: number) => {
    try {
      await submitDc(id).unwrap();
      notify('Submitted for approval', 'success');
    } catch (err) { notify(extractErrorMessage(err), 'error'); }
  }, [submitDc, notify]);

  const changeStatus = useCallback(async (id: number, status: string) => {
    try {
      await changeDcStatus({ id, status }).unwrap();
      notify(`Marked as ${status.charAt(0) + status.slice(1).toLowerCase()}`, 'success');
    } catch (err) { notify(extractErrorMessage(err), 'error'); }
  }, [changeDcStatus, notify]);

  const confirmDecision = async () => {
    if (!decision) return;
    try {
      if (decision.action === 'approve') {
        await approveDc({ id: decision.id, remarks }).unwrap();
        notify('Delivery challan approved', 'success');
      } else {
        await rejectDc({ id: decision.id, remarks }).unwrap();
        notify('Delivery challan rejected', 'success');
      }
      setDecision(null); setRemarks('');
    } catch (err) { notify(extractErrorMessage(err), 'error'); }
  };

  const columns: GridColDef<DeliveryChallan>[] = [
    { field: 'dcNumber', headerName: 'DC No.', width: 180 },
    { field: 'dcDate', headerName: 'Date', width: 120 },
    {
      field: 'receiver', headerName: 'Receiver', flex: 1, sortable: false,
      valueGetter: (_v, row) => row.receiverType === 'PARTY' ? row.receiverParty?.partyName : row.receiverUnit?.unitName,
    },
    {
      field: 'unit', headerName: 'Unit', flex: 1, sortable: false,
      valueGetter: (_v, row) => row.unit.unitName,
    },
    {
      field: 'dcType', headerName: 'Type', width: 130,
      renderCell: (p) => (
        <Chip size="small" variant="outlined" label={p.row.dcType === 'RETURNABLE' ? 'Returnable' : 'Non-Returnable'}
          color={p.row.dcType === 'RETURNABLE' ? 'warning' : 'default'} />
      ),
    },
    {
      field: 'status', headerName: 'Status', width: 150,
      renderCell: (p) => (
        <Chip size="small" label={p.row.status.replace(/_/g, ' ')} color={STATUS_COLORS[p.row.status]} />
      ),
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 210,
      sortable: false,
      renderCell: (params) => (
        <Stack direction="row">
          <Tooltip title="View">
            <IconButton size="small" onClick={() => navigate(`/dc/${params.row.id}/edit?view=1`)}>
              <VisibilityIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          {hasPermission('DELIVERY_CHALLAN_UPDATE') && EDITABLE_STATUSES.includes(params.row.status) && (
            <Tooltip title="Edit">
              <IconButton size="small" onClick={() => navigate(`/dc/${params.row.id}/edit`)}>
                <EditIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
          {hasPermission('DELIVERY_CHALLAN_UPDATE') && params.row.status === 'DRAFT' && (
            <Tooltip title="Submit for approval">
              <IconButton size="small" color="primary" onClick={() => submit(params.row.id)}>
                <SendIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
          {hasPermission('DELIVERY_CHALLAN_APPROVE') && params.row.status === 'PENDING_APPROVAL' && (
            <>
              <Tooltip title="Approve">
                <IconButton size="small" color="success" onClick={() => setDecision({ id: params.row.id, action: 'approve' })}>
                  <CheckIcon fontSize="small" />
                </IconButton>
              </Tooltip>
              <Tooltip title="Reject">
                <IconButton size="small" color="error" onClick={() => setDecision({ id: params.row.id, action: 'reject' })}>
                  <CloseIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </>
          )}
          {hasPermission('DELIVERY_CHALLAN_UPDATE') && params.row.status === 'APPROVED' && (
            <Tooltip title="Mark as Issued">
              <IconButton size="small" color="info" onClick={() => changeStatus(params.row.id, 'ISSUED')}>
                <LocalShippingIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
          {hasPermission('DELIVERY_CHALLAN_UPDATE') && params.row.status === 'ISSUED' && (
            <Tooltip title="Mark as Delivered">
              <IconButton size="small" color="success" onClick={() => changeStatus(params.row.id, 'DELIVERED')}>
                <DoneAllIcon fontSize="small" />
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
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5" fontWeight={600}>DC Booking</Typography>
        {hasPermission('DELIVERY_CHALLAN_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/dc/new')}>
            New DC
          </Button>
        )}
      </Stack>

      <Stack direction="row" spacing={2} flexWrap="wrap">
        <TextField label="DC No." size="small" value={filters.dcNumber}
          onChange={(e) => setFilters({ ...filters, dcNumber: e.target.value })} />
        <TextField select label="Status" size="small" sx={{ minWidth: 160 }} value={filters.status}
          onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
          <MenuItem value="">All</MenuItem>
          {(Object.keys(STATUS_COLORS) as DcStatus[]).map((s) => (
            <MenuItem key={s} value={s}>{s.replace(/_/g, ' ')}</MenuItem>
          ))}
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

      {/* Approve / Reject delivery challan */}
      <Dialog open={!!decision} onClose={() => setDecision(null)} maxWidth="sm" fullWidth>
        <DialogTitle>{decision?.action === 'approve' ? 'Approve Delivery Challan' : 'Reject Delivery Challan'}</DialogTitle>
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
    </Stack>
  );
}
