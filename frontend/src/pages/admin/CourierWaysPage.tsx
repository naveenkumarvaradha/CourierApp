import { useState } from 'react';
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
import { extractErrorMessage } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';
import { useAuth } from '../../context/AuthContext';
import {
  useListCourierWaysQuery,
  useCreateCourierWayMutation,
  useUpdateCourierWayMutation,
  useDeleteCourierWayMutation,
} from '../../store/api/adminApiSlice';
import type { CourierWay } from '../../types';

interface FormState { id?: number; name: string; active: boolean }
const EMPTY: FormState = { name: '', active: true };

export default function CourierWaysPage() {
  const { notify } = useNotification();
  const { hasPermission } = useAuth();
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<FormState>(EMPTY);

  const { data: rows = [], isFetching: loading } = useListCourierWaysQuery();
  const [createCourierWay] = useCreateCourierWayMutation();
  const [updateCourierWay] = useUpdateCourierWayMutation();
  const [deleteCourierWay] = useDeleteCourierWayMutation();

  const openCreate = () => { setForm(EMPTY); setOpen(true); };
  const openEdit = (cw: CourierWay) => { setForm({ id: cw.id, name: cw.name, active: cw.active }); setOpen(true); };

  const save = async () => {
    if (!form.name.trim()) { notify('Name is required', 'warning'); return; }
    try {
      if (form.id) {
        await updateCourierWay({ id: form.id, name: form.name.trim(), active: form.active }).unwrap();
        notify('Courier way updated', 'success');
      } else {
        await createCourierWay({ name: form.name.trim(), active: form.active }).unwrap();
        notify('Courier way added', 'success');
      }
      setOpen(false);
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const remove = async (id: number) => {
    if (!window.confirm('Delete this courier way? Existing bookings using it will not be affected.')) return;
    try {
      await deleteCourierWay(id).unwrap();
      notify('Deleted', 'success');
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const columns: GridColDef<CourierWay>[] = [
    { field: 'name', headerName: 'Courier Way', flex: 1 },
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
        <Typography variant="h5" fontWeight={600}>Courier Ways</Typography>
        {hasPermission('ADMIN_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            Add Courier Way
          </Button>
        )}
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Manage the list of courier service providers (e.g. DHL, MARUTI). These appear as a
        dropdown when creating bookings.
      </Typography>

      <Box sx={{ height: 400, bgcolor: 'background.paper' }}>
        <DataGrid rows={rows} columns={columns} loading={loading} disableRowSelectionOnClick
          pageSizeOptions={[10, 25]} initialState={{ pagination: { paginationModel: { pageSize: 25 } } }} />
      </Box>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>{form.id ? 'Edit Courier Way' : 'Add Courier Way'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <TextField
              label="Name"
              fullWidth
              autoFocus
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value.toUpperCase() })}
              onKeyDown={(e) => { if (e.key === 'Enter') save(); }}
              helperText="e.g. DHL, MARUTI, FEDEX"
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
