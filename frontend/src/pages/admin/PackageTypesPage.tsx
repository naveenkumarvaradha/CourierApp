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
import type { PackageType } from '../../types';

interface FormState {
  id?: number;
  name: string;
  active: boolean;
}

const EMPTY: FormState = { name: '', active: true };

export default function PackageTypesPage() {
  const { notify } = useNotification();
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState<PackageType[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<FormState>(EMPTY);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setRows(await adminApi.listPackageTypes());
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    } finally {
      setLoading(false);
    }
  }, [notify]);

  useEffect(() => { load(); }, [load]);

  const openCreate = () => { setForm(EMPTY); setOpen(true); };
  const openEdit = (pt: PackageType) => { setForm({ id: pt.id, name: pt.name, active: pt.active }); setOpen(true); };

  const save = async () => {
    if (!form.name.trim()) { notify('Name is required', 'warning'); return; }
    try {
      if (form.id) {
        await adminApi.updatePackageType(form.id, { name: form.name.trim(), active: form.active });
        notify('Package type updated', 'success');
      } else {
        await adminApi.createPackageType({ name: form.name.trim(), active: form.active });
        notify('Package type added', 'success');
      }
      setOpen(false);
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const remove = async (id: number) => {
    if (!window.confirm('Delete this package type? Existing bookings using it will not be affected.')) return;
    try {
      await adminApi.deletePackageType(id);
      notify('Deleted', 'success');
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const columns: GridColDef<PackageType>[] = [
    { field: 'name', headerName: 'Package Type', flex: 1 },
    {
      field: 'active',
      headerName: 'Status',
      width: 120,
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
        <Typography variant="h5" fontWeight={600}>Package Types</Typography>
        {hasPermission('ADMIN_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            Add Package Type
          </Button>
        )}
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Manage package type options (e.g. BOX, COVER). These appear as a dropdown when creating bookings.
      </Typography>

      <Box sx={{ height: 400, bgcolor: 'background.paper' }}>
        <DataGrid rows={rows} columns={columns} loading={loading} disableRowSelectionOnClick
          pageSizeOptions={[10, 25]} initialState={{ pagination: { paginationModel: { pageSize: 25 } } }} />
      </Box>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>{form.id ? 'Edit Package Type' : 'Add Package Type'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <TextField
              label="Name"
              fullWidth
              autoFocus
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value.toUpperCase() })}
              onKeyDown={(e) => { if (e.key === 'Enter') save(); }}
              helperText="e.g. BOX, COVER, ENVELOPE"
            />
            <FormControlLabel
              control={<Checkbox checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />}
              label="Active (appears in booking dropdown)"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={save}>Save</Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
