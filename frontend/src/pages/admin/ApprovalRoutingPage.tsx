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
  TextField,
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

type CreatorKind = 'ANY' | 'ROLE' | 'USER';
type ApproverKind = 'ROLE' | 'USER';
type Module = 'BOOKING' | 'MASTER';

export default function ApprovalRoutingPage() {
  const { notify } = useNotification();
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState<ApprovalRouting[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [users, setUsers] = useState<UserAccount[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);

  // Creator scope
  const [creatorKind, setCreatorKind] = useState<CreatorKind>('ANY');
  const [creatorRoleId, setCreatorRoleId] = useState<number | ''>('');
  const [creatorUserId, setCreatorUserId] = useState<number | ''>('');

  // Module scope
  const [module, setModule] = useState<Module>('BOOKING');

  // Level
  const [level, setLevel] = useState<number>(1);

  // Approver
  const [approverKind, setApproverKind] = useState<ApproverKind>('ROLE');
  const [approverId, setApproverId] = useState<number | ''>('');

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

  const resetForm = () => {
    setModule('BOOKING');
    setCreatorKind('ANY');
    setCreatorRoleId('');
    setCreatorUserId('');
    setApproverKind('ROLE');
    setApproverId('');
    setLevel(1);
  };

  const save = async () => {
    if (approverId === '') {
      notify('Select a role or user to be the approver', 'warning');
      return;
    }
    if (creatorKind === 'ROLE' && creatorRoleId === '') {
      notify('Select a creator role', 'warning');
      return;
    }
    if (creatorKind === 'USER' && creatorUserId === '') {
      notify('Select a creator user', 'warning');
      return;
    }
    try {
      await adminApi.createApprovalRouting({
        roleId: approverKind === 'ROLE' ? Number(approverId) : null,
        userId: approverKind === 'USER' ? Number(approverId) : null,
        creatorRoleId: creatorKind === 'ROLE' ? Number(creatorRoleId) : null,
        creatorUserId: creatorKind === 'USER' ? Number(creatorUserId) : null,
        active: true,
        module,
        level,
      });
      notify('Approval routing rule added', 'success');
      setOpen(false);
      resetForm();
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
      field: 'module',
      headerName: 'Module',
      width: 110,
      renderCell: (p) => (
        <Chip size="small" label={p.row.module ?? 'BOOKING'} color={p.row.module === 'MASTER' ? 'secondary' : 'primary'} variant="outlined" />
      ),
    },
    {
      field: 'creator',
      headerName: 'Created By',
      flex: 1,
      renderCell: (p) => {
        const label = p.row.creatorUsername ?? p.row.creatorRoleName;
        const kind = p.row.creatorUsername ? 'User' : p.row.creatorRoleName ? 'Role' : null;
        if (!label) {
          return <Typography variant="caption" color="text.secondary">Any</Typography>;
        }
        return (
          <Stack direction="row" spacing={0.5} alignItems="center">
            <Typography variant="caption" color="text.secondary">{kind}:</Typography>
            <Chip size="small" label={label} color="info" variant="outlined" />
          </Stack>
        );
      },
    },
    {
      field: 'approver',
      headerName: 'Designated Approver',
      flex: 1,
      renderCell: (p) => {
        const label = p.row.username ?? p.row.roleName;
        const kind = p.row.username ? 'User' : 'Role';
        return (
          <Stack direction="row" spacing={0.5} alignItems="center">
            <Typography variant="caption" color="text.secondary">{kind}:</Typography>
            <Chip size="small" label={label} color="success" variant="outlined" />
          </Stack>
        );
      },
    },
    {
      field: 'level',
      headerName: 'Level',
      width: 80,
      renderCell: (p) => (
        <Chip size="small" label={`L${p.row.level ?? 1}`} color="default" variant="outlined" />
      ),
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
      width: 90,
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
        Each rule maps: "when a record in [module] is created by [specific user / role / anyone], it must be
        approved by [designated approver]". Specific-user rules take priority over role rules.
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

      <Dialog open={open} onClose={() => { setOpen(false); resetForm(); }} maxWidth="sm" fullWidth>
        <DialogTitle>Add Approval Routing Rule</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            {/* ── Module ── */}
            <FormControl fullWidth size="small">
              <InputLabel>Module</InputLabel>
              <Select
                label="Module"
                value={module}
                onChange={(e) => setModule(e.target.value as Module)}
              >
                <MenuItem value="BOOKING">Booking</MenuItem>
                <MenuItem value="MASTER">Master (Parties)</MenuItem>
              </Select>
            </FormControl>

            {/* ── Creator scope ── */}
            <Typography variant="subtitle2" color="text.secondary">
              When record is created by…
            </Typography>

            <FormControl fullWidth size="small">
              <InputLabel>Creator scope</InputLabel>
              <Select
                label="Creator scope"
                value={creatorKind}
                onChange={(e) => {
                  setCreatorKind(e.target.value as CreatorKind);
                  setCreatorRoleId('');
                  setCreatorUserId('');
                }}
              >
                <MenuItem value="ANY">Anyone (catch-all)</MenuItem>
                <MenuItem value="ROLE">Specific Role</MenuItem>
                <MenuItem value="USER">Specific User</MenuItem>
              </Select>
            </FormControl>

            {creatorKind === 'ROLE' && (
              <FormControl fullWidth size="small">
                <InputLabel>Creator Role</InputLabel>
                <Select
                  label="Creator Role"
                  value={creatorRoleId}
                  onChange={(e) => setCreatorRoleId(e.target.value as number)}
                >
                  {roles.map((r) => (
                    <MenuItem key={r.id} value={r.id}>{r.name}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}

            {creatorKind === 'USER' && (
              <FormControl fullWidth size="small">
                <InputLabel>Creator User</InputLabel>
                <Select
                  label="Creator User"
                  value={creatorUserId}
                  onChange={(e) => setCreatorUserId(e.target.value as number)}
                >
                  {users.map((u) => (
                    <MenuItem key={u.id} value={u.id}>
                      {u.username} — {u.fullName}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}

            {/* ── Approver ── */}
            <Typography variant="subtitle2" color="text.secondary" sx={{ pt: 1 }}>
              …must be approved by…
            </Typography>

            <FormControl fullWidth size="small">
              <InputLabel>Designate approver by</InputLabel>
              <Select
                label="Designate approver by"
                value={approverKind}
                onChange={(e) => {
                  setApproverKind(e.target.value as ApproverKind);
                  setApproverId('');
                }}
              >
                <MenuItem value="ROLE">Role</MenuItem>
                <MenuItem value="USER">Specific User</MenuItem>
              </Select>
            </FormControl>

            <FormControl fullWidth size="small">
              <InputLabel>{approverKind === 'ROLE' ? 'Approver Role' : 'Approver User'}</InputLabel>
              <Select
                label={approverKind === 'ROLE' ? 'Approver Role' : 'Approver User'}
                value={approverId}
                onChange={(e) => setApproverId(e.target.value as number)}
              >
                {approverKind === 'ROLE'
                  ? roles.map((r) => <MenuItem key={r.id} value={r.id}>{r.name}</MenuItem>)
                  : users.map((u) => (
                      <MenuItem key={u.id} value={u.id}>
                        {u.username} — {u.fullName}
                      </MenuItem>
                    ))}
              </Select>
            </FormControl>

            {/* ── Approval Level ── */}
            <TextField
              label="Approval Level"
              type="number"
              size="small"
              fullWidth
              value={level}
              onChange={(e) => setLevel(Math.max(1, Number(e.target.value)))}
              inputProps={{ min: 1 }}
              helperText="Level 1 = first approver, Level 2 = second approver after Level 1 approves, etc."
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setOpen(false); resetForm(); }}>Cancel</Button>
          <Button variant="contained" onClick={save}>Add</Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
