import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Autocomplete,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import VisibilityIcon from '@mui/icons-material/Visibility';
import UndoIcon from '@mui/icons-material/Undo';
import { extractErrorMessage } from '../../api/client';
import {
  useSearchDcReceiptsQuery,
  useListEligibleDcsForReceiptQuery,
  useDeleteDcReceiptMutation,
} from '../../store/api/dcReceiptApiSlice';
import { useNotification } from '../../context/NotificationContext';
import { useAuth } from '../../context/AuthContext';
import type { DcReceipt, DeliveryChallan } from '../../types';

function receiverLabel(dc: DeliveryChallan): string {
  return dc.receiverType === 'PARTY' ? (dc.receiverParty?.partyName ?? '—') : (dc.receiverUnit?.unitName ?? '—');
}

export default function ReceiptListPage() {
  const { notify } = useNotification();
  const { hasPermission } = useAuth();
  const navigate = useNavigate();
  const [filters, setFilters] = useState({ receiptNumber: '', fromDate: '', toDate: '' });
  const [pickerOpen, setPickerOpen] = useState(false);
  const [selectedDc, setSelectedDc] = useState<DeliveryChallan | null>(null);

  const queryParams = {
    page: 0, size: 100,
    ...(filters.receiptNumber && { receiptNumber: filters.receiptNumber }),
    ...(filters.fromDate && { fromDate: filters.fromDate }),
    ...(filters.toDate && { toDate: filters.toDate }),
  };
  const { data: page, isFetching: loading } = useSearchDcReceiptsQuery(queryParams);
  const rows: DcReceipt[] = page?.content ?? [];
  const { data: eligiblePage, isFetching: loadingEligible } = useListEligibleDcsForReceiptQuery(undefined, { skip: !pickerOpen });
  const eligibleDcs = eligiblePage?.content ?? [];
  const [deleteReceipt] = useDeleteDcReceiptMutation();

  const undo = async (id: number) => {
    if (!window.confirm('Undo this receipt? The originating DC will revert to its prior status.')) return;
    try {
      await deleteReceipt(id).unwrap();
      notify('Receipt undone', 'success');
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const confirmPicker = () => {
    if (!selectedDc) return;
    setPickerOpen(false);
    navigate(`/dc-receipts/new?dcId=${selectedDc.id}`);
  };

  const columns: GridColDef<DcReceipt>[] = [
    { field: 'receiptNumber', headerName: 'Receipt No.', width: 180 },
    { field: 'receiptDate', headerName: 'Date', width: 120 },
    {
      field: 'dcNumber', headerName: 'DC No.', width: 180, sortable: false,
      valueGetter: (_v, row) => row.dc.dcNumber,
    },
    {
      field: 'unit', headerName: 'Unit', flex: 1, sortable: false,
      valueGetter: (_v, row) => row.dc.unit.unitName,
    },
    {
      field: 'receiver', headerName: 'Receiver', flex: 1, sortable: false,
      valueGetter: (_v, row) => receiverLabel(row.dc),
    },
    { field: 'receivedBy', headerName: 'Received By', width: 140 },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 110,
      sortable: false,
      renderCell: (params) => (
        <Stack direction="row">
          <Tooltip title="View">
            <IconButton size="small" onClick={() => navigate(`/dc-receipts/${params.row.id}`)}>
              <VisibilityIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          {hasPermission('RECEIPT_DELETE') && (
            <Tooltip title="Undo receipt">
              <IconButton size="small" color="error" onClick={() => undo(params.row.id)}>
                <UndoIcon fontSize="small" />
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
        <Typography variant="h5" fontWeight={600}>DC Receipt</Typography>
        {hasPermission('RECEIPT_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => { setSelectedDc(null); setPickerOpen(true); }}>
            New Receipt
          </Button>
        )}
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Confirm receipt of Returnable DC Bookings once the goods come back. Only Returnable DCs that are Issued or
        Delivered, and don't already have a receipt, are eligible.
      </Typography>

      <Stack direction="row" spacing={2} flexWrap="wrap">
        <TextField label="Receipt No." size="small" value={filters.receiptNumber}
          onChange={(e) => setFilters({ ...filters, receiptNumber: e.target.value })} />
        <TextField label="From Date" type="date" size="small" InputLabelProps={{ shrink: true }}
          value={filters.fromDate} onChange={(e) => setFilters({ ...filters, fromDate: e.target.value })} />
        <TextField label="To Date" type="date" size="small" InputLabelProps={{ shrink: true }}
          value={filters.toDate} onChange={(e) => setFilters({ ...filters, toDate: e.target.value })} />
      </Stack>

      <Box sx={{ height: 500, bgcolor: 'background.paper' }}>
        <DataGrid rows={rows} columns={columns} loading={loading} disableRowSelectionOnClick
          pageSizeOptions={[10, 25, 50]} initialState={{ pagination: { paginationModel: { pageSize: 25 } } }} />
      </Box>

      <Dialog open={pickerOpen} onClose={() => setPickerOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New DC Receipt</DialogTitle>
        <DialogContent>
          <Autocomplete
            sx={{ mt: 1 }}
            options={eligibleDcs}
            loading={loadingEligible}
            value={selectedDc}
            onChange={(_e, value) => setSelectedDc(value)}
            getOptionLabel={(dc) => `${dc.dcNumber} — ${receiverLabel(dc)}`}
            isOptionEqualToValue={(a, b) => a.id === b.id}
            renderInput={(params) => (
              <TextField {...params} label="Select DC" placeholder="Search by DC number or receiver"
                helperText="Only Returnable DCs (Issued/Delivered) without an existing receipt are shown" />
            )}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPickerOpen(false)}>Cancel</Button>
          <Button variant="contained" disabled={!selectedDc} onClick={confirmPicker}>Continue</Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
