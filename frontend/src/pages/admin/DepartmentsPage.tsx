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
import type { Department } from '../../types';

interface FormState { id?: number; name: string; active: boolean; }
const EMPTY: FormState = { name: '', active: true };

export default function DepartmentsPage() {
  const { notify } = useNotification();
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState<Department[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<FormState>(EMPTY);

  const load = useCallback(async () => {
    setLoading(true);
    try { setRows(await adminApi.listDepartments()); }
    catch (err) { notify(extractErrorMessage(err), 'error'); }
    finally { setLoading(false); }
  }, [notify]);

  useEffect(() => { load(); }, [load]);

  const openCreate = () => { setForm(EMPTY); setOpen(true); };
  const openEdit = (d: Department) => { setForm({ id: d.id, name: d.name, active: d.active }); setOpen(true); };

  const save = async () => {
    if (!form.name.trim()) { notify('Name is required', 'warning'); return; }
    try {
      if (form.id) {
        await adminApi.updateDepartment(form.id, { name: form.name.trim(), active: form.active });
        notify('Department updated', 'success');
      } else {
        await adminApi.createDepartment({ name: form.name.trim(), active: form.active });
        notify('Department added', 'success');
      }
      setOpen(false); load();
    } catch (err) { notify(extractErrorMessage(err), 'error'); }
  };

  const remove = async (id: number) => {
    if (!window.confirm('Delete this department?')) return;
    try { await adminApi.deleteDepartment(id); notify('Deleted', 'success'); load(); }
    catch (err) { notify(extractErrorMessage(err), 'error'); }
  };

  const columns: GridColDef<Department>[] = [
    { field: 'name', headerName: 'Department', flex: 1 },
    {
      field: 'active', headerName: 'Status', width: 120,
      renderCell: (p) => <Chip size="small" label={p.row.active ? 'Active' : 'Inactive'} color={p.row.active ? 'success' : 'default'} />,
    },
    {
      field: 'actions', headerName: 'Actions', width: 120, sortable: false,
      renderCell: (params) => (
        <Stack direction="row">
          {hasPermission('ADMIN_UPDATE') && (
            <Tooltip title="Edit"><IconButton size="small" onClick={() => openEdit(params.row)}><EditIcon fontSize="small" /></IconButton></Tooltip>
          )}
          {hasPermission('ADMIN_DELETE') && (
            <Tooltip title="Delete"><IconButton size="small" color="error" onClick={() => remove(params.row.id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
          )}
        </Stack>
      ),
    },
  ];

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5" fontWeight={600}>Departments</Typography>
        {hasPermission('ADMIN_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>Add Department</Button>
        )}
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Manage departments used to categorise users (e.g. Logistics, Accounts, Operations).
      </Typography>
      <Box sx={{ height: 420, bgcolor: 'background.paper' }}>
        <DataGrid rows={rows} columns={columns} loading={loading} disableRowSelectionOnClick
          pageSizeOptions={[10, 25]} initialState={{ pagination: { paginationModel: { pageSize: 25 } } }} />
      </Box>
      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>{form.id ? 'Edit Department' : 'Add Department'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <TextField label="Name" fullWidth autoFocus value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              onKeyDown={(e) => { if (e.key === 'Enter') save(); }} />
            <FormControlLabel
              control={<Checkbox checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />}
              label="Active" />
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
