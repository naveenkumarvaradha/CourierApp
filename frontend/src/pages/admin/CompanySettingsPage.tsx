import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  FormControlLabel,
  Grid,
  Stack,
  Switch,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import BusinessIcon from '@mui/icons-material/Business';
import EmailIcon from '@mui/icons-material/Email';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import WarningIcon from '@mui/icons-material/Warning';
import { adminApi } from '../../api/endpoints';
import { extractErrorMessage } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';
import type { CompanySettings } from '../../types';

interface SmtpForm {
  smtpHost: string;
  smtpPort: string;
  smtpUsername: string;
  smtpPassword: string;
  smtpFromName: string;
  smtpTls: boolean;
}

const EMPTY_SMTP: SmtpForm = {
  smtpHost: '',
  smtpPort: '587',
  smtpUsername: '',
  smtpPassword: '',
  smtpFromName: '',
  smtpTls: true,
};

export default function CompanySettingsPage() {
  const { notify } = useNotification();
  const [tab, setTab] = useState(0);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  // Company info form
  const [company, setCompany] = useState<Omit<CompanySettings,
    'id' | 'smtpHost' | 'smtpPort' | 'smtpUsername' | 'smtpFromName' | 'smtpTls' | 'smtpConfigured'>>({
    companyName: '',
    addressLine1: '',
    addressLine2: null,
    city: '',
    state: '',
    pincode: '',
    country: 'India',
    phone: null,
    email: null,
    gstin: null,
  });

  // SMTP form
  const [smtp, setSmtp] = useState<SmtpForm>(EMPTY_SMTP);
  const [smtpConfigured, setSmtpConfigured] = useState(false);

  useEffect(() => {
    setLoading(true);
    adminApi
      .getCompanySettings()
      .then((s) => {
        setCompany({
          companyName: s.companyName,
          addressLine1: s.addressLine1,
          addressLine2: s.addressLine2,
          city: s.city,
          state: s.state,
          pincode: s.pincode,
          country: s.country,
          phone: s.phone,
          email: s.email,
          gstin: s.gstin,
        });
        setSmtp({
          smtpHost: s.smtpHost ?? '',
          smtpPort: String(s.smtpPort ?? 587),
          smtpUsername: s.smtpUsername ?? '',
          smtpPassword: '',           // never pre-fill password
          smtpFromName: s.smtpFromName ?? '',
          smtpTls: s.smtpTls ?? true,
        });
        setSmtpConfigured(s.smtpConfigured);
      })
      .catch((err) => notify(extractErrorMessage(err), 'error'))
      .finally(() => setLoading(false));
  }, [notify]);

  const saveCompany = async () => {
    setSaving(true);
    try {
      await adminApi.updateCompanySettings({
        companyName: company.companyName,
        addressLine1: company.addressLine1,
        addressLine2: company.addressLine2 || null,
        city: company.city,
        state: company.state,
        pincode: company.pincode,
        country: company.country,
        phone: company.phone || null,
        email: company.email || null,
        gstin: company.gstin || null,
      });
      notify('Company settings saved. Sender party updated.', 'success');
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  const saveSmtp = async () => {
    setSaving(true);
    try {
      const updated = await adminApi.updateCompanySettings({
        // keep existing company fields unchanged by resending them
        companyName: company.companyName,
        addressLine1: company.addressLine1,
        addressLine2: company.addressLine2 || null,
        city: company.city,
        state: company.state,
        pincode: company.pincode,
        country: company.country,
        phone: company.phone || null,
        email: company.email || null,
        gstin: company.gstin || null,
        // SMTP
        smtpHost: smtp.smtpHost || null,
        smtpPort: smtp.smtpPort ? parseInt(smtp.smtpPort) : null,
        smtpUsername: smtp.smtpUsername || null,
        smtpPassword: smtp.smtpPassword || null,
        smtpFromName: smtp.smtpFromName || null,
        smtpTls: smtp.smtpTls,
      });
      setSmtpConfigured(updated.smtpConfigured);
      setSmtp((prev) => ({ ...prev, smtpPassword: '' }));
      notify('Mail configuration saved. Password reset emails will now use this SMTP.', 'success');
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  const cf = (key: keyof typeof company) => ({
    value: company[key] ?? '',
    onChange: (e: React.ChangeEvent<HTMLInputElement>) =>
      setCompany({ ...company, [key]: e.target.value || null }),
  });

  if (loading) return <Typography color="text.secondary" sx={{ p: 2 }}>Loading…</Typography>;

  return (
    <Stack spacing={2}>
      <Stack direction="row" alignItems="center" spacing={1}>
        <BusinessIcon color="primary" />
        <Typography variant="h5" fontWeight={600}>Company Setup</Typography>
      </Stack>

      <Tabs value={tab} onChange={(_, v) => setTab(v)}>
        <Tab label="Company Info" />
        <Tab label={
          <Stack direction="row" spacing={1} alignItems="center">
            <span>Mail Configuration</span>
            {smtpConfigured
              ? <CheckCircleIcon fontSize="small" color="success" />
              : <WarningIcon fontSize="small" color="warning" />}
          </Stack>
        } />
      </Tabs>

      {/* ── Company Info tab ───────────────────────────────────── */}
      {tab === 0 && (
        <Card>
          <CardContent>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              This information is printed as the <strong>Sender</strong> on every shipping sticker.
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={8}>
                <TextField
                  label="Company Name"
                  fullWidth
                  required
                  value={company.companyName}
                  onChange={(e) => setCompany({ ...company, companyName: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField label="GSTIN" fullWidth {...cf('gstin')} />
              </Grid>
              <Grid item xs={12}>
                <TextField label="Address Line 1" fullWidth required {...cf('addressLine1')} />
              </Grid>

              <Grid item xs={12} sm={4}>
                <TextField label="City" fullWidth required {...cf('city')} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField label="State" fullWidth required {...cf('state')} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField label="Pincode" fullWidth required {...cf('pincode')} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField label="Country" fullWidth required {...cf('country')} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField label="Phone" fullWidth {...cf('phone')} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField label="Email" fullWidth {...cf('email')} />
              </Grid>
              <Grid item xs={12}>
                <Stack direction="row" justifyContent="flex-end">
                  <Button variant="contained" onClick={saveCompany} disabled={saving}>
                    {saving ? 'Saving…' : 'Save Company Info'}
                  </Button>
                </Stack>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}

      {/* ── Mail Config tab ────────────────────────────────────── */}
      {tab === 1 && (
        <Card>
          <CardContent>
            <Stack spacing={2}>
              <Stack direction="row" spacing={1} alignItems="center">
                <EmailIcon color="primary" />
                <Typography variant="subtitle1" fontWeight={600}>SMTP Mail Configuration</Typography>
                {smtpConfigured
                  ? <Chip label="Configured" color="success" size="small" />
                  : <Chip label="Not Configured — using server default" color="warning" size="small" />}
              </Stack>

              <Alert severity="info">
                Configure these settings so that <strong>Forgot Password</strong> emails and
                <strong> scheduled reports</strong> are sent from your own mail server.
                Leave blank to use the server's default SMTP (configured in application.yml).
              </Alert>

              <Grid container spacing={2}>
                <Grid item xs={12} sm={8}>
                  <TextField
                    label="SMTP Host"
                    fullWidth
                    placeholder="smtp.office365.com"
                    value={smtp.smtpHost}
                    onChange={(e) => setSmtp({ ...smtp, smtpHost: e.target.value })}
                  />
                </Grid>
                <Grid item xs={12} sm={4}>
                  <TextField
                    label="Port"
                    fullWidth
                    placeholder="587"
                    value={smtp.smtpPort}
                    onChange={(e) => setSmtp({ ...smtp, smtpPort: e.target.value })}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    label="Username / Email"
                    fullWidth
                    placeholder="yourmail@company.com"
                    value={smtp.smtpUsername}
                    onChange={(e) => setSmtp({ ...smtp, smtpUsername: e.target.value })}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    label="Password"
                    type="password"
                    fullWidth
                    placeholder={smtpConfigured ? '(leave blank to keep existing)' : 'Enter password'}
                    value={smtp.smtpPassword}
                    onChange={(e) => setSmtp({ ...smtp, smtpPassword: e.target.value })}
                  />
                </Grid>
                <Grid item xs={12} sm={8}>
                  <TextField
                    label="From Display Name"
                    fullWidth
                    placeholder="ShipDesk Notifications"
                    value={smtp.smtpFromName}
                    onChange={(e) => setSmtp({ ...smtp, smtpFromName: e.target.value })}
                  />
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Box sx={{ pt: 1 }}>
                    <FormControlLabel
                      control={
                        <Switch
                          checked={smtp.smtpTls}
                          onChange={(e) => setSmtp({ ...smtp, smtpTls: e.target.checked })}
                        />
                      }
                      label="Enable STARTTLS"
                    />
                  </Box>
                </Grid>
                <Grid item xs={12}>
                  <Stack direction="row" justifyContent="flex-end">
                    <Button variant="contained" startIcon={<EmailIcon />}
                      onClick={saveSmtp} disabled={saving}>
                      {saving ? 'Saving…' : 'Save Mail Config'}
                    </Button>
                  </Stack>
                </Grid>
              </Grid>
            </Stack>
          </CardContent>
        </Card>
      )}
    </Stack>
  );
}
