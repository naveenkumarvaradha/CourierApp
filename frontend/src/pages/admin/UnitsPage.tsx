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
  FormControlLabel,
  Grid,
  IconButton,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { adminApi } from '../../api/endpoints';
import { extractErrorMessage } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';
import { useAuth } from '../../context/AuthContext';
import type { Unit } from '../../types';

interface FormState {
  id?: number;
  unitName: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  state: string;
  pincode: string;
  country: string;
  phone: string;
  email: string;
  gstin: string;
  defaultUnit: boolean;
  active: boolean;
}

const EMPTY: FormState = {
  unitName: '', addressLine1: '', addressLine2: '', city: '', state: '', pincode: '',
  country: 'India', phone: '', email: '', gstin: '', defaultUnit: false, active: true,
};

export default function UnitsPage() {
  const { notify } = useNotification();
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState<Unit[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<FormState>(EMPTY);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setRows(await adminApi.listUnits());
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    } finally {
      setLoading(false);
    }
  }, [notify]);

  useEffect(() => { load(); }, [load]);

  const openCreate = () => { setForm(EMPTY); setOpen(true); };
  const openEdit = (u: Unit) => {
    setForm({
      id: u.id,
      unitName: u.unitName,
      addressLine1: u.addressLine1,
      addressLine2: u.addressLine2 ?? '',
      city: u.city,
      state: u.state,
      pincode: u.pincode,
      country: u.country,
      phone: u.phone ?? '',
      email: u.email ?? '',
      gstin: u.gstin ?? '',
      defaultUnit: u.defaultUnit,
      active: u.active,
    });
    setOpen(true);
  };

  const save = async () => {
    if (!form.unitName.trim() || !form.addressLine1.trim() || !form.city.trim()
        || !form.state.trim() || !form.pincode.trim() || !form.country.trim()) {
      notify('Unit name, address line 1, city, state, pincode and country are required', 'warning');
      return;
    }
    const body = {
      unitName: form.unitName.trim(),
      addressLine1: form.addressLine1.trim(),
      addressLine2: form.addressLine2.trim() || null,
      city: form.city.trim(),
      state: form.state.trim(),
      pincode: form.pincode.trim(),
      country: form.country.trim(),
      phone: form.phone.trim() || null,
      email: form.email.trim() || null,
      gstin: form.gstin.trim() || null,
      defaultUnit: form.defaultUnit,
      active: form.active,
    };
    try {
      if (form.id) {
        await adminApi.updateUnit(form.id, body);
        notify('Unit updated', 'success');
      } else {
        await adminApi.createUnit(body);
        notify('Unit added', 'success');
      }
      setOpen(false);
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const remove = async (id: number) => {
    if (!window.confirm('Delete this unit? Bookings/DCs that already reference it will not be affected.')) return;
    try {
      await adminApi.deleteUnit(id);
      notify('Deleted', 'success');
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const columns: GridColDef<Unit>[] = [
    { field: 'unitName', headerName: 'Unit Name', flex: 1 },
    {
      field: 'address', headerName: 'Address', flex: 1.5, sortable: false,
      valueGetter: (_v, row) => `${row.city}, ${row.state} - ${row.pincode}`,
    },
    {
      field: 'defaultUnit', headerName: 'Default', width: 100,
      renderCell: (p) => (p.row.defaultUnit ? <Chip size="small" label="Default" color="primary" /> : null),
    },
    {
      field: 'active',
      headerName: 'Status',
      width: 110,
      renderCell: (p) => (
        <Chip size="small" label={p.row.active ? 'Active' : 'Inactive'} color={p.row.active ? 'success' : 'default'} />
      ),
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 120,
      sortable: false,
      renderCell: (params) => (
        <Stack direction="row">
          {hasPermission('ADMIN_UPDATE') && (
            <Tooltip title="Edit">
              <IconButton size="small" onClick={() => openEdit(params.row)}>
                <EditIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
          {hasPermission('ADMIN_DELETE') && (
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
        <Typography variant="h5" fontWeight={600}>Units</Typography>
        {hasPermission('ADMIN_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            Add Unit
          </Button>
        )}
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Manage company branch/unit addresses. Pick a unit when creating a booking or delivery challan
        to control which address prints as the shipment origin.
      </Typography>

      <Box sx={{ height: 420, bgcolor: 'background.paper' }}>
        <DataGrid rows={rows} columns={columns} loading={loading} disableRowSelectionOnClick
          pageSizeOptions={[10, 25]} initialState={{ pagination: { paginationModel: { pageSize: 25 } } }} />
      </Box>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{form.id ? 'Edit Unit' : 'Add Unit'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} mt={0.5}>
            <Grid item xs={12}>
              <TextField label="Unit Name *" fullWidth autoFocus value={form.unitName}
                onChange={(e) => setForm({ ...form, unitName: e.target.value })}
                helperText="e.g. Head Office, Chennai Branch" />
            </Grid>
            <Grid item xs={12}>
              <TextField label="Address Line 1 *" fullWidth value={form.addressLine1}
                onChange={(e) => setForm({ ...form, addressLine1: e.target.value })} />
            </Grid>
            <Grid item xs={12}>
              <TextField label="Address Line 2" fullWidth value={form.addressLine2}
                onChange={(e) => setForm({ ...form, addressLine2: e.target.value })} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="City *" fullWidth value={form.city}
                onChange={(e) => setForm({ ...form, city: e.target.value })} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="State *" fullWidth value={form.state}
                onChange={(e) => setForm({ ...form, state: e.target.value })} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="Pincode *" fullWidth value={form.pincode}
                onChange={(e) => setForm({ ...form, pincode: e.target.value })} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="Country *" fullWidth value={form.country}
                onChange={(e) => setForm({ ...form, country: e.target.value })} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="Phone" fullWidth value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="Email" fullWidth value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="GSTIN" fullWidth value={form.gstin}
                onChange={(e) => setForm({ ...form, gstin: e.target.value })} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Stack direction="row" spacing={2} alignItems="center" height="100%">
                <FormControlLabel
                  control={<Checkbox checked={form.defaultUnit}
                    onChange={(e) => setForm({ ...form, defaultUnit: e.target.checked })} />}
                  label="Default unit" />
                <FormControlLabel
                  control={<Checkbox checked={form.active}
                    onChange={(e) => setForm({ ...form, active: e.target.checked })} />}
                  label="Active" />
              </Stack>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={save}>Save</Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
