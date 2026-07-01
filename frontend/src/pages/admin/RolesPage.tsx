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
  Divider,
  FormControlLabel,
  FormGroup,
  IconButton,
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
import type { Permission, Role } from '../../types';

interface FormState {
  id?: number;
  name: string;
  description: string;
  permissionIds: number[];
  systemRole: boolean;
}

const EMPTY: FormState = { name: '', description: '', permissionIds: [], systemRole: false };

export default function RolesPage() {
  const { notify } = useNotification();
  const { hasPermission } = useAuth();
  const [roles, setRoles] = useState<Role[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<FormState>(EMPTY);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await adminApi.listRoles();
      setRoles(data.content);
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    } finally {
      setLoading(false);
    }
  }, [notify]);

  useEffect(() => {
    adminApi.listPermissions().then(setPermissions).catch(() => undefined);
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const openCreate = () => {
    setForm(EMPTY);
    setOpen(true);
  };

  const openEdit = (r: Role) => {
    setForm({
      id: r.id,
      name: r.name,
      description: r.description ?? '',
      permissionIds: r.permissions.map((p) => p.id),
      systemRole: r.systemRole,
    });
    setOpen(true);
  };

  const togglePerm = (id: number) => {
    setForm((f) => ({
      ...f,
      permissionIds: f.permissionIds.includes(id)
        ? f.permissionIds.filter((p) => p !== id)
        : [...f.permissionIds, id],
    }));
  };

  const save = async () => {
    try {
      const body = { name: form.name, description: form.description, permissionIds: form.permissionIds };
      if (form.id) {
        await adminApi.updateRole(form.id, body);
        notify('Role updated', 'success');
      } else {
        await adminApi.createRole(body);
        notify('Role created', 'success');
      }
      setOpen(false);
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const remove = async (id: number) => {
    if (!window.confirm('Delete this role?')) return;
    try {
      await adminApi.deleteRole(id);
      notify('Role deleted', 'success');
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const columns: GridColDef<Role>[] = [
    { field: 'name', headerName: 'Name', flex: 1 },
    { field: 'description', headerName: 'Description', flex: 1.6 },
    {
      field: 'permissions',
      headerName: 'Permissions',
      width: 130,
      valueGetter: (_v, row) => row.permissions.length,
    },
    {
      field: 'systemRole',
      headerName: 'System',
      width: 100,
      renderCell: (p) => (p.row.systemRole ? <Chip size="small" label="System" /> : null),
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
          {hasPermission('ADMIN_DELETE') && !params.row.systemRole && (
            <IconButton size="small" color="error" onClick={() => remove(params.row.id)}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          )}
        </>
      ),
    },
  ];

  const grouped = permissions.reduce<Record<string, Permission[]>>((acc, p) => {
    (acc[p.module] ||= []).push(p);
    return acc;
  }, {});

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5" fontWeight={600}>
          Roles &amp; Permissions
        </Typography>
        {hasPermission('ADMIN_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            New Role
          </Button>
        )}
      </Stack>

      <Box sx={{ height: 560, bgcolor: 'background.paper' }}>
        <DataGrid
          rows={roles}
          columns={columns}
          loading={loading}
          disableRowSelectionOnClick
          pageSizeOptions={[10, 25]}
          initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
        />
      </Box>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>{form.id ? 'Edit Role' : 'New Role'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <TextField
              label="Role Name"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              disabled={form.systemRole}
              required
            />
            <TextField
              label="Description"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
            <Divider textAlign="left">Permissions</Divider>
            {Object.entries(grouped).map(([module, perms]) => (
              <Box key={module}>
                <Typography variant="subtitle2" color="primary">
                  {module}
                </Typography>
                <FormGroup row>
                  {perms.map((p) => (
                    <FormControlLabel
                      key={p.id}
                      control={
                        <Checkbox
                          checked={form.permissionIds.includes(p.id)}
                          onChange={() => togglePerm(p.id)}
                        />
                      }
                      label={p.action}
                    />
                  ))}
                </FormGroup>
              </Box>
            ))}
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
