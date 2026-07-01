import { useCallback, useEffect, useState } from 'react';
import {
  Box,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  IconButton,
  ListItemText,
  MenuItem,
  OutlinedInput,
  Select,
  Stack,
  TextField,
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
import type { Role, UserAccount } from '../../types';

interface FormState {
  id?: number;
  username: string;
  password: string;
  fullName: string;
  email: string;
  phone: string;
  active: boolean;
  roleIds: number[];
}

const EMPTY: FormState = {
  username: '',
  password: '',
  fullName: '',
  email: '',
  phone: '',
  active: true,
  roleIds: [],
};

export default function UsersPage() {
  const { notify } = useNotification();
  const { hasPermission } = useAuth();
  const [users, setUsers] = useState<UserAccount[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<FormState>(EMPTY);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await adminApi.listUsers(search, 0, 100);
      setUsers(data.content);
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    } finally {
      setLoading(false);
    }
  }, [search, notify]);

  useEffect(() => {
    adminApi.listRoles().then((r) => setRoles(r.content)).catch(() => undefined);
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const openCreate = () => {
    setForm(EMPTY);
    setOpen(true);
  };

  const openEdit = (u: UserAccount) => {
    setForm({
      id: u.id,
      username: u.username,
      password: '',
      fullName: u.fullName,
      email: u.email,
      phone: u.phone ?? '',
      active: u.active,
      roleIds: u.roles.map((r) => r.id),
    });
    setOpen(true);
  };

  const save = async () => {
    try {
      if (form.id) {
        await adminApi.updateUser(form.id, {
          fullName: form.fullName,
          email: form.email,
          phone: form.phone,
          active: form.active,
          password: form.password || undefined,
          roleIds: form.roleIds,
        });
        notify('User updated', 'success');
      } else {
        await adminApi.createUser({
          username: form.username,
          password: form.password,
          fullName: form.fullName,
          email: form.email,
          phone: form.phone,
          active: form.active,
          roleIds: form.roleIds,
        });
        notify('User created', 'success');
      }
      setOpen(false);
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const remove = async (id: number) => {
    if (!window.confirm('Delete this user?')) return;
    try {
      await adminApi.deleteUser(id);
      notify('User deleted', 'success');
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const columns: GridColDef<UserAccount>[] = [
    { field: 'username', headerName: 'Username', flex: 1 },
    { field: 'fullName', headerName: 'Full Name', flex: 1.2 },
    { field: 'email', headerName: 'Email', flex: 1.4 },
    {
      field: 'roles',
      headerName: 'Roles',
      flex: 1.2,
      valueGetter: (_v, row) => row.roles.map((r) => r.name).join(', '),
    },
    {
      field: 'active',
      headerName: 'Active',
      width: 90,
      valueGetter: (_v, row) => (row.active ? 'Yes' : 'No'),
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 120,
      sortable: false,
      renderCell: (params) => (
        <>
          {hasPermission('ADMIN_UPDATE') && (
            <IconButton size="small" onClick={() => openEdit(params.row)}>
              <EditIcon fontSize="small" />
            </IconButton>
          )}
          {hasPermission('ADMIN_DELETE') && (
            <IconButton size="small" color="error" onClick={() => remove(params.row.id)}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          )}
        </>
      ),
    },
  ];

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5" fontWeight={600}>
          Users
        </Typography>
        {hasPermission('ADMIN_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            New User
          </Button>
        )}
      </Stack>

      <TextField
        size="small"
        label="Search by username or name"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        sx={{ maxWidth: 360 }}
      />

      <Box sx={{ height: 560, bgcolor: 'background.paper' }}>
        <DataGrid
          rows={users}
          columns={columns}
          loading={loading}
          disableRowSelectionOnClick
          pageSizeOptions={[10, 25, 50]}
          initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
        />
      </Box>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{form.id ? 'Edit User' : 'New User'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <TextField
              label="Username"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              disabled={!!form.id}
              required
            />
            <TextField
              label={form.id ? 'New Password (leave blank to keep)' : 'Password'}
              type="password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required={!form.id}
            />
            <TextField
              label="Full Name"
              value={form.fullName}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              required
            />
            <TextField
              label="Email"
              type="email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              required
            />
            <TextField
              label="Phone"
              value={form.phone}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
            />
            <Select
              multiple
              value={form.roleIds}
              onChange={(e) => setForm({ ...form, roleIds: e.target.value as number[] })}
              input={<OutlinedInput label="Roles" />}
              renderValue={(selected) =>
                roles
                  .filter((r) => (selected as number[]).includes(r.id))
                  .map((r) => r.name)
                  .join(', ')
              }
            >
              {roles.map((r) => (
                <MenuItem key={r.id} value={r.id}>
                  <Checkbox checked={form.roleIds.includes(r.id)} />
                  <ListItemText primary={r.name} secondary={r.description} />
                </MenuItem>
              ))}
            </Select>
            <FormControlLabel
              control={
                <Checkbox
                  checked={form.active}
                  onChange={(e) => setForm({ ...form, active: e.target.checked })}
                />
              }
              label="Active"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={save}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
