import { useCallback, useEffect, useState } from 'react';
import {
  Box, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle,
  FormControlLabel, IconButton, Stack, Switch, TextField, Tooltip, Typography,
} from '@mui/material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { adminApi } from '../../api/endpoints';
import type { Company } from '../../types';

const empty = { companyCode: '', name: '', active: true };

export default function CompaniesPage() {
  const [rows, setRows] = useState<Company[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Company | null>(null);
  const [form, setForm] = useState(empty);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try { setRows(await adminApi.listCompanies()); } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); }, [load]);

  const openCreate = () => { setEditing(null); setForm(empty); setError(''); setOpen(true); };
  const openEdit = (row: Company) => {
    setEditing(row);
    setForm({ companyCode: row.companyCode, name: row.name, active: row.active });
    setError(''); setOpen(true);
  };
  const handleClose = () => { setOpen(false); };

  const save = async () => {
    if (!form.companyCode.trim() || !form.name.trim()) { setError('Company Code and Name are required'); return; }
    setSaving(true);
    try {
      if (editing) {
        await adminApi.updateCompany(editing.id, form);
      } else {
        await adminApi.createCompany(form);
      }
      handleClose();
      load();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Save failed');
    } finally {
      setSaving(false);
    }
  };

  const del = async (id: number) => {
    if (!window.confirm('Delete this company?')) return;
    try { await adminApi.deleteCompany(id); load(); } catch { /* ignore */ }
  };

  const columns: GridColDef[] = [
    { field: 'companyCode', headerName: 'Code', width: 140 },
    { field: 'name', headerName: 'Company Name', flex: 1 },
    {
      field: 'active', headerName: 'Status', width: 110,
      renderCell: (p) => <Chip label={p.value ? 'Active' : 'Inactive'}
        color={p.value ? 'success' : 'default'} size="small" />,
    },
    {
      field: 'actions', headerName: 'Actions', width: 100, sortable: false,
      renderCell: (p) => (
        <>
          <Tooltip title="Edit"><IconButton size="small" onClick={() => openEdit(p.row)}><EditIcon fontSize="small" /></IconButton></Tooltip>
          <Tooltip title="Delete"><IconButton size="small" color="error" onClick={() => del(p.row.id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
        </>
      ),
    },
  ];

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5" fontWeight={700}>Companies</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>New Company</Button>
      </Stack>

      <DataGrid rows={rows} columns={columns} loading={loading} autoHeight
        pageSizeOptions={[25, 50]} disableRowSelectionOnClick density="compact" />

      <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
        <DialogTitle>{editing ? 'Edit Company' : 'New Company'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            {error && <Typography color="error" variant="body2">{error}</Typography>}
            <TextField label="Company Code" value={form.companyCode}
              onChange={(e) => setForm((f) => ({ ...f, companyCode: e.target.value.toUpperCase() }))}
              fullWidth required inputProps={{ maxLength: 20 }}
              helperText="Short unique code used for login (e.g. CTL)" />
            <TextField label="Company Name" value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              fullWidth required inputProps={{ maxLength: 255 }} />
            <FormControlLabel control={
              <Switch checked={form.active} onChange={(e) => setForm((f) => ({ ...f, active: e.target.checked }))} />
            } label="Active" />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={handleClose}>Cancel</Button>
          <Button variant="contained" onClick={save} disabled={saving}>
            {saving ? 'Saving...' : editing ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
