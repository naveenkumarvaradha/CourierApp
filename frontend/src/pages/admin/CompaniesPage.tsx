import { useCallback, useEffect, useRef, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Grid,
  IconButton,
  Stack,
  Switch,
  Tab,
  Tabs,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import SettingsIcon from '@mui/icons-material/Settings';
import ApartmentIcon from '@mui/icons-material/Apartment';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import WarningIcon from '@mui/icons-material/Warning';
import ImageIcon from '@mui/icons-material/Image';
import DeleteForeverIcon from '@mui/icons-material/DeleteForever';
import UploadIcon from '@mui/icons-material/Upload';
import { adminApi } from '../../api/endpoints';
import { api, BASE_URL, extractErrorMessage } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';
import type { Company } from '../../types';

// ── Company form ──────────────────────────────────────────────────────────────
interface CompanyForm { id?: number; name: string; active: boolean; }
const EMPTY_CO: CompanyForm = { name: '', active: true };

// ── Settings form ─────────────────────────────────────────────────────────────
interface SettingsForm {
  companyName: string; addressLine1: string; addressLine2: string;
  city: string; state: string; pincode: string; country: string;
  phone: string; email: string; gstin: string;
  smtpHost: string; smtpPort: string; smtpUsername: string;
  smtpPassword: string; smtpFromName: string; smtpTls: boolean;
}
const EMPTY_SETTINGS: SettingsForm = {
  companyName: '', addressLine1: '', addressLine2: '', city: '', state: '',
  pincode: '', country: 'India', phone: '', email: '', gstin: '',
  smtpHost: '', smtpPort: '587', smtpUsername: '', smtpPassword: '',
  smtpFromName: '', smtpTls: true,
};

export default function CompaniesPage() {
  const { notify } = useNotification();
  const [rows, setRows] = useState<Company[]>([]);
  const [loading, setLoading] = useState(false);

  // Company create/edit dialog
  const [coOpen, setCoOpen] = useState(false);
  const [coForm, setCoForm] = useState<CompanyForm>(EMPTY_CO);
  const [coSaving, setCoSaving] = useState(false);

  // Settings dialog
  const [settingsCompany, setSettingsCompany] = useState<Company | null>(null);
  const [settingsForm, setSettingsForm] = useState<SettingsForm>(EMPTY_SETTINGS);
  const [settingsTab, setSettingsTab] = useState(0);
  const [settingsSaving, setSettingsSaving] = useState(false);
  const [smtpConfigured, setSmtpConfigured] = useState(false);
  const [settingsLoading, setSettingsLoading] = useState(false);

  // Logo state
  const [logoUrl, setLogoUrl] = useState<string | null>(null);
  const [logoUploading, setLogoUploading] = useState(false);
  const logoInputRef = useRef<HTMLInputElement>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try { setRows(await adminApi.listCompanies()); }
    catch (err) { notify(extractErrorMessage(err), 'error'); }
    finally { setLoading(false); }
  }, [notify]);

  useEffect(() => { load(); }, [load]);

  // ── Company CRUD ────────────────────────────────────────────────────────────
  const openCreate = () => { setCoForm(EMPTY_CO); setCoOpen(true); };
  const openEdit = (c: Company) => {
    setCoForm({ id: c.id, name: c.name, active: c.active });
    setCoOpen(true);
  };

  const saveCo = async () => {
    if (!coForm.name.trim()) { notify('Company name is required', 'error'); return; }
    setCoSaving(true);
    try {
      if (coForm.id) {
        const existing = rows.find((r) => r.id === coForm.id)!;
        await adminApi.updateCompany(coForm.id, { companyCode: existing.companyCode, name: coForm.name, active: coForm.active });
        notify('Company updated', 'success');
      } else {
        await adminApi.createCompany({ companyCode: '', name: coForm.name, active: coForm.active });
        notify('Company created', 'success');
      }
      setCoOpen(false);
      load();
    } catch (err) { notify(extractErrorMessage(err), 'error'); }
    finally { setCoSaving(false); }
  };

  const del = async (id: number) => {
    if (!window.confirm('Delete this company?')) return;
    try { await adminApi.deleteCompany(id); load(); }
    catch (err) { notify(extractErrorMessage(err), 'error'); }
  };

  // ── Settings dialog ─────────────────────────────────────────────────────────
  const openSettings = async (c: Company) => {
    setSettingsCompany(c);
    setSettingsTab(0);
    setSettingsLoading(true);
    setLogoUrl(null);
    try {
      const s = await adminApi.getCompanySettingsById(c.id);
      setSettingsForm({
        companyName: s.companyName || c.name,
        addressLine1: s.addressLine1 || '',
        addressLine2: s.addressLine2 || '',
        city: s.city || '',
        state: s.state || '',
        pincode: s.pincode || '',
        country: s.country || 'India',
        phone: s.phone || '',
        email: s.email || '',
        gstin: s.gstin || '',
        smtpHost: s.smtpHost || '',
        smtpPort: String(s.smtpPort || 587),
        smtpUsername: s.smtpUsername || '',
        smtpPassword: '',
        smtpFromName: s.smtpFromName || '',
        smtpTls: s.smtpTls ?? true,
      });
      setSmtpConfigured(s.smtpConfigured);
      // Try loading logo — endpoint is public, 404 means no logo set
      const logoCheck = await fetch(`${BASE_URL}/admin/companies/${c.id}/logo`);
      if (logoCheck.ok) {
        setLogoUrl(`${BASE_URL}/admin/companies/${c.id}/logo?t=${Date.now()}`);
      }
    } catch (err) {
      setSettingsForm({ ...EMPTY_SETTINGS, companyName: c.name });
    } finally { setSettingsLoading(false); }
  };

  const uploadLogo = async (file: File) => {
    if (!settingsCompany) return;
    setLogoUploading(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      await api.post(`/admin/companies/${settingsCompany.id}/logo`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setLogoUrl(`${BASE_URL}/admin/companies/${settingsCompany.id}/logo?t=${Date.now()}`);
      notify('Logo uploaded successfully', 'success');
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    } finally {
      setLogoUploading(false);
    }
  };

  const deleteLogo = async () => {
    if (!settingsCompany) return;
    if (!window.confirm('Remove the company logo?')) return;
    try {
      await api.delete(`/admin/companies/${settingsCompany.id}/logo`);
      setLogoUrl(null);
      notify('Logo removed', 'success');
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const saveSettings = async () => {
    if (!settingsCompany) return;
    setSettingsSaving(true);
    try {
      const updated = await adminApi.updateCompanySettingsById(settingsCompany.id, {
        companyName: settingsForm.companyName || settingsCompany.name,
        addressLine1: settingsForm.addressLine1,
        addressLine2: settingsForm.addressLine2 || null,
        city: settingsForm.city,
        state: settingsForm.state,
        pincode: settingsForm.pincode,
        country: settingsForm.country,
        phone: settingsForm.phone || null,
        email: settingsForm.email || null,
        gstin: settingsForm.gstin || null,
        smtpHost: settingsForm.smtpHost || null,
        smtpPort: settingsForm.smtpPort ? parseInt(settingsForm.smtpPort) : null,
        smtpUsername: settingsForm.smtpUsername || null,
        smtpPassword: settingsForm.smtpPassword || null,
        smtpFromName: settingsForm.smtpFromName || null,
        smtpTls: settingsForm.smtpTls,
      });
      setSmtpConfigured(updated.smtpConfigured);
      setSettingsForm((f) => ({ ...f, smtpPassword: '' }));
      notify('Company settings saved', 'success');
    } catch (err) { notify(extractErrorMessage(err), 'error'); }
    finally { setSettingsSaving(false); }
  };

  const sf = (key: keyof SettingsForm) => ({
    value: settingsForm[key] as string,
    onChange: (e: React.ChangeEvent<HTMLInputElement>) =>
      setSettingsForm({ ...settingsForm, [key]: e.target.value }),
  });

  const columns: GridColDef<Company>[] = [
    { field: 'companyCode', headerName: 'Code', width: 90 },
    {
      field: 'logo', headerName: 'Logo', width: 80, sortable: false,
      renderCell: (p) => (
        <Box sx={{ display: 'flex', alignItems: 'center', height: '100%' }}>
          <img
            src={`${BASE_URL}/admin/companies/${p.row.id}/logo`}
            alt=""
            style={{ maxHeight: 36, maxWidth: 64, objectFit: 'contain' }}
            onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
          />
        </Box>
      ),
    },
    { field: 'name', headerName: 'Company Name', flex: 1 },
    {
      field: 'active', headerName: 'Status', width: 100,
      renderCell: (p) => (
        <Chip label={p.value ? 'Active' : 'Inactive'}
          color={p.value ? 'success' : 'default'} size="small" />
      ),
    },
    {
      field: 'actions', headerName: 'Actions', width: 130, sortable: false,
      renderCell: (p) => (
        <Stack direction="row">
          <Tooltip title="Setup (address, mail config, logo)">
            <IconButton size="small" color="primary" onClick={() => openSettings(p.row)}>
              <SettingsIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Edit name / status">
            <IconButton size="small" onClick={() => openEdit(p.row)}>
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Delete">
            <IconButton size="small" color="error" onClick={() => del(p.row.id)}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Stack>
      ),
    },
  ];

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Stack direction="row" spacing={1} alignItems="center">
          <ApartmentIcon color="primary" />
          <Typography variant="h5" fontWeight={600}>Companies</Typography>
        </Stack>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
          New Company
        </Button>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Each company can have its own address, sender details, and mail configuration.
        Click <SettingsIcon fontSize="small" sx={{ verticalAlign: 'middle' }} /> to configure.
      </Typography>

      <Box sx={{ height: 500, bgcolor: 'background.paper' }}>
        <DataGrid rows={rows} columns={columns} loading={loading}
          disableRowSelectionOnClick
          pageSizeOptions={[10, 25]}
          initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
        />
      </Box>

      {/* ── Create/Edit company dialog ─────────────────────────────────────── */}
      <Dialog open={coOpen} onClose={() => setCoOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>{coForm.id ? 'Edit Company' : 'New Company'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <TextField label="Company Name" fullWidth required
              value={coForm.name}
              onChange={(e) => setCoForm({ ...coForm, name: e.target.value })} />
            <FormControlLabel
              control={<Switch checked={coForm.active}
                onChange={(e) => setCoForm({ ...coForm, active: e.target.checked })} />}
              label="Active" />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCoOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={saveCo} disabled={coSaving}>
            {coSaving ? 'Saving…' : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* ── Company Settings dialog ────────────────────────────────────────── */}
      <Dialog open={!!settingsCompany} onClose={() => setSettingsCompany(null)} maxWidth="md" fullWidth>
        <DialogTitle>
          <Stack direction="row" spacing={1} alignItems="center">
            <SettingsIcon />
            <span>Company Setup — {settingsCompany?.name}</span>
          </Stack>
        </DialogTitle>
        <DialogContent>
          {settingsLoading ? (
            <Typography color="text.secondary" sx={{ py: 2 }}>Loading…</Typography>
          ) : (
            <>
              <Tabs value={settingsTab} onChange={(_, v) => setSettingsTab(v)} sx={{ mb: 2 }}>
                <Tab label="Address & Contact" />
                <Tab label={
                  <Stack direction="row" spacing={0.5} alignItems="center">
                    <span>Mail Config</span>
                    {smtpConfigured
                      ? <CheckCircleIcon fontSize="small" color="success" />
                      : <WarningIcon fontSize="small" color="warning" />}
                  </Stack>
                } />
                <Tab icon={<ImageIcon fontSize="small" />} iconPosition="start" label="Logo" />
              </Tabs>

              {settingsTab === 0 && (
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={8}>
                    <TextField label="Company Name" fullWidth required {...sf('companyName')} />
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <TextField label="GSTIN" fullWidth {...sf('gstin')} />
                  </Grid>
                  <Grid item xs={12}>
                    <TextField label="Address Line 1" fullWidth required {...sf('addressLine1')} />
                  </Grid>

                  <Grid item xs={12} sm={4}>
                    <TextField label="City" fullWidth required {...sf('city')} />
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <TextField label="State" fullWidth required {...sf('state')} />
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <TextField label="Pincode" fullWidth required {...sf('pincode')} />
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <TextField label="Country" fullWidth required {...sf('country')} />
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <TextField label="Phone" fullWidth {...sf('phone')} />
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <TextField label="Email" fullWidth {...sf('email')} />
                  </Grid>
                </Grid>
              )}

              {settingsTab === 1 && (
                <Stack spacing={2}>
                  <Alert severity="info">
                    Configure SMTP so <strong>Forgot Password</strong> and <strong>scheduled reports</strong> are sent
                    from your own mail server. Leave blank to use the server default (application.yml).
                  </Alert>
                  <Chip
                    label={smtpConfigured ? 'Mail Configured' : 'Not Configured — using server default'}
                    color={smtpConfigured ? 'success' : 'warning'}
                    size="small"
                    sx={{ alignSelf: 'flex-start' }}
                  />
                  <Grid container spacing={2}>
                    <Grid item xs={12} sm={8}>
                      <TextField label="SMTP Host" fullWidth placeholder="smtp.office365.com" {...sf('smtpHost')} />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                      <TextField label="Port" fullWidth placeholder="587" {...sf('smtpPort')} />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <TextField label="Username / Email" fullWidth placeholder="mail@company.com" {...sf('smtpUsername')} />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <TextField label="Password" type="password" fullWidth
                        placeholder={smtpConfigured ? '(leave blank to keep existing)' : 'Enter password'}
                        {...sf('smtpPassword')} />
                    </Grid>
                    <Grid item xs={12} sm={8}>
                      <TextField label="From Display Name" fullWidth placeholder="ShipDesk Notifications" {...sf('smtpFromName')} />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                      <Box sx={{ pt: 1 }}>
                        <FormControlLabel
                          control={<Switch checked={settingsForm.smtpTls}
                            onChange={(e) => setSettingsForm({ ...settingsForm, smtpTls: e.target.checked })} />}
                          label="STARTTLS" />
                      </Box>
                    </Grid>
                  </Grid>
                </Stack>
              )}

              {settingsTab === 2 && (
                <Stack spacing={3} alignItems="flex-start">
                  <Typography variant="body2" color="text.secondary">
                    Upload a company logo. It will appear in the <strong>top-left of printed stickers</strong> and
                    in the <strong>header of PDF reports</strong>.
                    Recommended: PNG or JPG, max 2 MB, ideally a wide/horizontal logo.
                  </Typography>

                  {/* Current logo preview */}
                  <Box
                    sx={{
                      width: 260, height: 100, border: '1.5px dashed',
                      borderColor: logoUrl ? 'success.main' : 'grey.400',
                      borderRadius: 2, display: 'flex', alignItems: 'center',
                      justifyContent: 'center', bgcolor: 'grey.50', overflow: 'hidden',
                    }}
                  >
                    {logoUrl ? (
                      <img
                        src={logoUrl}
                        alt="Company logo"
                        style={{ maxWidth: '100%', maxHeight: '100%', objectFit: 'contain' }}
                      />
                    ) : (
                      <Stack alignItems="center" spacing={0.5}>
                        <ImageIcon sx={{ color: 'grey.400', fontSize: 36 }} />
                        <Typography variant="caption" color="text.secondary">No logo uploaded</Typography>
                      </Stack>
                    )}
                  </Box>

                  {/* Upload / delete buttons */}
                  <Stack direction="row" spacing={1}>
                    <input
                      ref={logoInputRef}
                      type="file"
                      accept="image/png,image/jpeg,image/gif,image/webp"
                      style={{ display: 'none' }}
                      onChange={(e) => {
                        const file = e.target.files?.[0];
                        if (file) uploadLogo(file);
                        e.target.value = '';
                      }}
                    />
                    <Button
                      variant="contained"
                      startIcon={<UploadIcon />}
                      onClick={() => logoInputRef.current?.click()}
                      disabled={logoUploading}
                    >
                      {logoUploading ? 'Uploading…' : logoUrl ? 'Replace Logo' : 'Upload Logo'}
                    </Button>
                    {logoUrl && (
                      <Button
                        variant="outlined"
                        color="error"
                        startIcon={<DeleteForeverIcon />}
                        onClick={deleteLogo}
                      >
                        Remove
                      </Button>
                    )}
                  </Stack>

                  <Alert severity="info" sx={{ width: '100%' }}>
                    Changes take effect immediately on the next sticker print or report export.
                    No need to save — logo is stored directly.
                  </Alert>
                </Stack>
              )}
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSettingsCompany(null)}>Close</Button>
          {!settingsLoading && (
            <Button variant="contained" onClick={saveSettings} disabled={settingsSaving}>
              {settingsSaving ? 'Saving…' : 'Save Settings'}
            </Button>
          )}
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
