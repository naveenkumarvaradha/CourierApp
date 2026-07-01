import { useCallback, useEffect, useState } from 'react';
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Typography,
} from '@mui/material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import { adminApi } from '../../api/endpoints';
import { extractErrorMessage } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';
import { useAuth } from '../../context/AuthContext';
import type { ApprovalRouting, Role, UserAccount } from '../../types';

export default function ApprovalRoutingPage() {
  const { notify } = useNotification();
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState<ApprovalRouting[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [users, setUsers] = useState<UserAccount[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [target, setTarget] = useState<{ kind: 'ROLE' | 'USER'; id: number | '' }>({
    kind: 'ROLE',
    id: '',
  });
  const [creatorRoleId, setCreatorRoleId] = useState<number | ''>('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setRows(await adminApi.listApprovalRouting());
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    } finally {
      setLoading(false);
    }
  }, [notify]);

  useEffect(() => {
    adminApi.listRoles().then((r) => setRoles(r.content)).catch(() => undefined);
    adminApi.listUsers('', 0, 100).then((u) => setUsers(u.content)).catch(() => undefined);
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const save = async () => {
    if (target.id === '') {
      notify('Select a role or user to be the approver', 'warning');
      return;
    }
    try {
      await adminApi.createApprovalRouting({
        roleId: target.kind === 'ROLE' ? Number(target.id) : null,
        userId: target.kind === 'USER' ? Number(target.id) : null,
        creatorRoleId: creatorRoleId !== '' ? Number(creatorRoleId) : null,
        active: true,
      });
      notify('Approval routing added', 'success');
      setOpen(false);
      setTarget({ kind: 'ROLE', id: '' });
      setCreatorRoleId('');
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const remove = async (id: number) => {
    if (!window.confirm('Remove this approver designation?')) return;
    try {
      await adminApi.deleteApprovalRouting(id);
      notify('Removed', 'success');
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const columns: GridColDef<ApprovalRouting>[] = [
    {
      field: 'creatorRoleName',
      headerName: 'Booking Created By (Role)',
      flex: 1,
      renderCell: (p) =>
        p.row.creatorRoleName ? (
          <Chip size="small" label={p.row.creatorRoleName} color="info" variant="outlined" />
        ) : (
          <Typography variant="caption" color="text.secondary">
            Any
          </Typography>
        ),
    },
    {
      field: 'type',
      headerName: 'Approver Type',
      width: 120,
      valueGetter: (_v, row) => (row.roleName ? 'Role' : 'User'),
    },
    {
      field: 'target',
      headerName: 'Designated Approver',
      flex: 1,
      renderCell: (p) => (
        <Chip
          size="small"
          label={p.row.roleName ?? p.row.username}
          color="success"
          variant="outlined"
        />
      ),
    },
    {
      field: 'active',
      headerName: 'Active',
      width: 100,
      valueGetter: (_v, row) => (row.active ? 'Yes' : 'No'),
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 100,
      sortable: false,
      renderCell: (params) =>
        hasPermission('ADMIN_DELETE') ? (
          <IconButton size="small" color="error" onClick={() => remove(params.row.id)}>
            <DeleteIcon fontSize="small" />
          </IconButton>
        ) : null,
    },
  ];

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5" fontWeight={600}>
          Approval Routing
        </Typography>
        {hasPermission('ADMIN_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>
            Add Rule
          </Button>
        )}
      </Stack>

      <Typography variant="body2" color="text.secondary">
        Each rule maps: "when a booking is created by someone with [Creator Role], it must be
        approved by [Designated Approver role/user]". Leave Creator Role blank to apply the rule
        to all bookings regardless of who created them.
      </Typography>

      <Box sx={{ height: 480, bgcolor: 'background.paper' }}>
        <DataGrid
          rows={rows}
          columns={columns}
          loading={loading}
          disableRowSelectionOnClick
          pageSizeOptions={[10, 25]}
          initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
        />
      </Box>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Approval Routing Rule</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <FormControl fullWidth>
              <InputLabel>Creator Role (bookings created by…)</InputLabel>
              <Select
                label="Creator Role (bookings created by…)"
                value={creatorRoleId}
                onChange={(e) => setCreatorRoleId(e.target.value as number | '')}
              >
                <MenuItem value="">
                  <em>Any role (catch-all)</em>
                </MenuItem>
                {roles.map((r) => (
                  <MenuItem key={r.id} value={r.id}>
                    {r.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <FormControl fullWidth>
              <InputLabel>Designate approver by</InputLabel>
              <Select
                label="Designate approver by"
                value={target.kind}
                onChange={(e) =>
                  setTarget({ kind: e.target.value as 'ROLE' | 'USER', id: '' })
                }
              >
                <MenuItem value="ROLE">Role</MenuItem>
                <MenuItem value="USER">Specific User</MenuItem>
              </Select>
            </FormControl>

            <FormControl fullWidth>
              <InputLabel>{target.kind === 'ROLE' ? 'Approver Role' : 'Approver User'}</InputLabel>
              <Select
                label={target.kind === 'ROLE' ? 'Approver Role' : 'Approver User'}
                value={target.id}
                onChange={(e) => setTarget({ ...target, id: e.target.value as number })}
              >
                {target.kind === 'ROLE'
                  ? roles.map((r) => (
                      <MenuItem key={r.id} value={r.id}>
                        {r.name}
                      </MenuItem>
                    ))
                  : users.map((u) => (
                      <MenuItem key={u.id} value={u.id}>
                        {u.username} — {u.fullName}
                      </MenuItem>
                    ))}
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => {
              setOpen(false);
              setTarget({ kind: 'ROLE', id: '' });
              setCreatorRoleId('');
            }}
          >
            Cancel
          </Button>
          <Button variant="contained" onClick={save}>
            Add
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
