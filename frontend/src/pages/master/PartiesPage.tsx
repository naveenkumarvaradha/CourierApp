import { useCallback, useEffect, useState } from 'react';
import {
  Box,
  Button,
  Checkbox,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  Grid,
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
import DeleteIcon from '@mui/icons-material/Delete';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import { partyApi } from '../../api/endpoints';
import { extractErrorMessage } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';
import { useAuth } from '../../context/AuthContext';
import type { Party, PartyStatus, PartyType } from '../../types';

const STATUS_COLOR: Record<PartyStatus, 'default' | 'warning' | 'success' | 'error'> = {
  PENDING_APPROVAL: 'warning',
  ACTIVE: 'success',
  INACTIVE: 'default',
  REJECTED: 'error',
};

interface FormState {
  id?: number;
  partyName: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  state: string;
  pincode: string;
  country: string;
  phone: string;
  email: string;
  gstin: string;
  partyType: PartyType;
  active: boolean;
}

const EMPTY: FormState = {
  partyName: '',
  addressLine1: '',
  addressLine2: '',
  city: '',
  state: '',
  pincode: '',
  country: 'India',
  phone: '',
  email: '',
  gstin: '',
  partyType: 'BOTH',
  active: true,
};

export default function PartiesPage() {
  const { notify } = useNotification();
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState<Party[]>([]);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState({ name: '', city: '', pincode: '' });
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<FormState>(EMPTY);

  // Reject dialog
  const [rejectDialog, setRejectDialog] = useState<{ id: number } | null>(null);
  const [rejectRemarks, setRejectRemarks] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await partyApi.list({ ...filters, page: 0, size: 100 });
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

  const openCreate = () => {
    setForm(EMPTY);
    setOpen(true);
  };

  const openEdit = (p: Party) => {
    setForm({
      id: p.id,
      partyName: p.partyName,
      addressLine1: p.addressLine1,
      addressLine2: p.addressLine2 ?? '',
      city: p.city,
      state: p.state,
      pincode: p.pincode,
      country: p.country,
      phone: p.phone ?? '',
      email: p.email ?? '',
      gstin: p.gstin ?? '',
      partyType: p.partyType,
      active: p.active,
    });
    setOpen(true);
  };

  const save = async () => {
    try {
      const { id, ...body } = form;
      if (id) {
        await partyApi.update(id, body);
        notify('Party updated', 'success');
      } else {
        await partyApi.create(body);
        notify('Party submitted for approval', 'success');
      }
      setOpen(false);
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const remove = async (id: number) => {
    if (!window.confirm('Delete this party?')) return;
    try {
      await partyApi.remove(id);
      notify('Party deleted', 'success');
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const approve = async (id: number) => {
    try {
      await partyApi.approve(id);
      notify('Party approved and is now ACTIVE', 'success');
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const confirmReject = async () => {
    if (!rejectDialog) return;
    try {
      await partyApi.reject(rejectDialog.id, rejectRemarks || undefined);
      notify('Party rejected', 'success');
      setRejectDialog(null);
      setRejectRemarks('');
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const columns: GridColDef<Party>[] = [
    { field: 'partyCode', headerName: 'Code', width: 120 },
    { field: 'partyName', headerName: 'Name', flex: 1.2 },
    { field: 'city', headerName: 'City', flex: 0.8 },
    { field: 'state', headerName: 'State', flex: 0.8 },
    { field: 'pincode', headerName: 'Pincode', width: 110 },
    { field: 'partyType', headerName: 'Type', width: 100 },
    {
      field: 'partyStatus',
      headerName: 'Status',
      width: 160,
      renderCell: (p) => {
        const status: PartyStatus = p.row.partyStatus ?? (p.row.active ? 'ACTIVE' : 'INACTIVE');
        return (
          <Chip
            size="small"
            color={STATUS_COLOR[status]}
            label={status.replace('_', ' ')}
          />
        );
      },
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 160,
      sortable: false,
      renderCell: (params) => {
        const p = params.row;
        const isPending = p.partyStatus === 'PENDING_APPROVAL';
        return (
          <Stack direction="row">
            {hasPermission('MASTER_APPROVE') && isPending && (
              <>
                <Tooltip title="Approve party">
                  <IconButton size="small" color="success" onClick={() => approve(p.id)}>
                    <CheckIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Reject party">
                  <IconButton size="small" color="error" onClick={() => { setRejectDialog({ id: p.id }); setRejectRemarks(''); }}>
                    <CloseIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              </>
            )}
            {hasPermission('MASTER_UPDATE') && (
              <Tooltip title="Edit">
                <IconButton size="small" onClick={() => openEdit(p)}>
                  <EditIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
            {hasPermission('MASTER_DELETE') && (
              <Tooltip title="Delete">
                <IconButton size="small" color="error" onClick={() => remove(p.id)}>
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </Tooltip>
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
          Parties (Address Book)
        </Typography>
        {hasPermission('MASTER_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            New Party
          </Button>
        )}
      </Stack>

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
        <TextField
          size="small"
          label="Name"
          value={filters.name}
          onChange={(e) => setFilters({ ...filters, name: e.target.value })}
        />
        <TextField
          size="small"
          label="City"
          value={filters.city}
          onChange={(e) => setFilters({ ...filters, city: e.target.value })}
        />
        <TextField
          size="small"
          label="Pincode"
          value={filters.pincode}
          onChange={(e) => setFilters({ ...filters, pincode: e.target.value })}
        />
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

      {/* Create / Edit dialog */}
      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>{form.id ? 'Edit Party' : 'New Party'}</DialogTitle>
        <DialogContent>
          {!form.id && (
            <Typography variant="body2" color="warning.main" sx={{ mb: 1 }}>
              New parties require approval before they become active.
            </Typography>
          )}
          <Grid container spacing={2} mt={0}>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Party Name"
                fullWidth
                required
                value={form.partyName}
                onChange={(e) => setForm({ ...form, partyName: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Party Type</InputLabel>
                <Select
                  label="Party Type"
                  value={form.partyType}
                  onChange={(e) => setForm({ ...form, partyType: e.target.value as PartyType })}
                >
                  <MenuItem value="SENDER">Sender</MenuItem>
                  <MenuItem value="RECEIVER">Receiver</MenuItem>
                  <MenuItem value="BOTH">Both</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Address Line 1"
                fullWidth
                required
                value={form.addressLine1}
                onChange={(e) => setForm({ ...form, addressLine1: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Address Line 2"
                fullWidth
                value={form.addressLine2}
                onChange={(e) => setForm({ ...form, addressLine2: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                label="City"
                fullWidth
                required
                value={form.city}
                onChange={(e) => setForm({ ...form, city: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                label="State"
                fullWidth
                required
                value={form.state}
                onChange={(e) => setForm({ ...form, state: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                label="Pincode"
                fullWidth
                required
                value={form.pincode}
                onChange={(e) => setForm({ ...form, pincode: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                label="Country"
                fullWidth
                required
                value={form.country}
                onChange={(e) => setForm({ ...form, country: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                label="Phone"
                fullWidth
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                label="Email"
                fullWidth
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="GSTIN"
                fullWidth
                value={form.gstin}
                onChange={(e) => setForm({ ...form, gstin: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControlLabel
                control={
                  <Checkbox
                    checked={form.active}
                    onChange={(e) => setForm({ ...form, active: e.target.checked })}
                  />
                }
                label="Active"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={save}>
            {form.id ? 'Save' : 'Submit for Approval'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Reject dialog */}
      <Dialog open={!!rejectDialog} onClose={() => setRejectDialog(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Reject Party</DialogTitle>
        <DialogContent>
          <TextField
            label="Remarks (optional)"
            fullWidth
            multiline
            minRows={3}
            sx={{ mt: 1 }}
            value={rejectRemarks}
            onChange={(e) => setRejectRemarks(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectDialog(null)}>Cancel</Button>
          <Button variant="contained" color="error" onClick={confirmReject}>
            Reject
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
