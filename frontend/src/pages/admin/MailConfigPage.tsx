import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  FormControlLabel,
  Grid,
  InputAdornment,
  Stack,
  Switch,
  TextField,
  Typography,
} from '@mui/material';
import EmailIcon from '@mui/icons-material/Email';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import SendIcon from '@mui/icons-material/Send';
import SaveIcon from '@mui/icons-material/Save';
import { adminApi } from '../../api/endpoints';
import { extractErrorMessage } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';

interface MailForm {
  smtpHost: string;
  smtpPort: string;
  smtpUsername: string;
  smtpPassword: string;
  smtpFromName: string;
  smtpTls: boolean;
}

const EMPTY: MailForm = {
  smtpHost: '',
  smtpPort: '587',
  smtpUsername: '',
  smtpPassword: '',
  smtpFromName: '',
  smtpTls: true,
};

export default function MailConfigPage() {
  const { notify } = useNotification();
  const [form, setForm] = useState<MailForm>(EMPTY);
  const [configured, setConfigured] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [testEmail, setTestEmail] = useState('');
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<{ ok: boolean; message: string } | null>(null);

  useEffect(() => {
    adminApi.getMailConfig()
      .then((cfg) => {
        setConfigured(cfg.configured);
        setForm({
          smtpHost: cfg.smtpHost ?? '',
          smtpPort: String(cfg.smtpPort ?? 587),
          smtpUsername: cfg.smtpUsername ?? '',
          smtpPassword: '',
          smtpFromName: cfg.smtpFromName ?? '',
          smtpTls: cfg.smtpTls ?? true,
        });
      })
      .catch(() => notify('Failed to load mail config', 'error'))
      .finally(() => setLoading(false));
  }, []);

  function set(field: keyof MailForm, value: string | boolean) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function save() {
    setSaving(true);
    try {
      const res = await adminApi.saveMailConfig({
        smtpHost: form.smtpHost || null,
        smtpPort: form.smtpPort ? Number(form.smtpPort) : null,
        smtpUsername: form.smtpUsername || null,
        smtpPassword: form.smtpPassword || null,
        smtpFromName: form.smtpFromName || null,
        smtpTls: form.smtpTls,
      });
      setConfigured(res.configured);
      notify('Mail configuration saved', 'success');
    } catch (e) {
      notify(extractErrorMessage(e), 'error');
    } finally {
      setSaving(false);
    }
  }

  async function sendTest() {
    if (!testEmail.trim()) return;
    setTesting(true);
    setTestResult(null);
    try {
      // Pass current form SMTP values so test works without saving first
      const res = await adminApi.testMailConfig(testEmail.trim(), {
        smtpHost: form.smtpHost || undefined,
        smtpPort: form.smtpPort || undefined,
        smtpUsername: form.smtpUsername || undefined,
        smtpPassword: form.smtpPassword || undefined,
        smtpFromName: form.smtpFromName || undefined,
        smtpTls: form.smtpTls,
      });
      setTestResult({ ok: true, message: res.message });
    } catch (e: unknown) {
      // axios throws on 5xx — extract the backend error message from response body
      const axiosErr = e as { response?: { data?: { message?: string } }; message?: string };
      const msg = axiosErr?.response?.data?.message ?? axiosErr?.message ?? 'Connection failed';
      setTestResult({ ok: false, message: msg });
    } finally {
      setTesting(false);
    }
  }

  if (loading) return <CircularProgress sx={{ m: 4 }} />;

  return (
    <Stack spacing={3} maxWidth={720}>
      {/* Header */}
      <Stack direction="row" alignItems="center" spacing={1.5}>
        <EmailIcon color="primary" />
        <Typography variant="h5" fontWeight={700}>Mail Configuration</Typography>
        <Chip
          size="small"
          icon={configured ? <CheckCircleIcon /> : <WarningAmberIcon />}
          label={configured ? 'Configured' : 'Not Configured'}
          color={configured ? 'success' : 'warning'}
        />
      </Stack>

      <Typography variant="body2" color="text.secondary">
        Configure the SMTP server used for sending password reset emails and system notifications.
        If left blank the application falls back to the settings in <code>application.yml</code>.
      </Typography>

      {/* SMTP Settings */}
      <Card>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={700} mb={2}>SMTP Settings</Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={8}>
              <TextField
                label="SMTP Host"
                value={form.smtpHost}
                onChange={(e) => set('smtpHost', e.target.value)}
                fullWidth
                placeholder="smtp.office365.com"
                helperText="e.g. smtp.office365.com, smtp.gmail.com"
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                label="Port"
                value={form.smtpPort}
                onChange={(e) => set('smtpPort', e.target.value)}
                fullWidth
                type="number"
                placeholder="587"
                helperText="587 (STARTTLS) or 465 (SSL)"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Username / Email"
                value={form.smtpUsername}
                onChange={(e) => set('smtpUsername', e.target.value)}
                fullWidth
                placeholder="noreply@yourdomain.com"
                InputProps={{ startAdornment: <InputAdornment position="start"><EmailIcon fontSize="small" /></InputAdornment> }}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Password"
                value={form.smtpPassword}
                onChange={(e) => set('smtpPassword', e.target.value)}
                fullWidth
                type="password"
                placeholder={configured ? '(unchanged — enter new to update)' : ''}
                helperText={configured ? 'Leave blank to keep existing password' : ''}
              />
            </Grid>
            <Grid item xs={12} sm={8}>
              <TextField
                label="From Name"
                value={form.smtpFromName}
                onChange={(e) => set('smtpFromName', e.target.value)}
                fullWidth
                placeholder="Courier Booking"
                helperText="Display name shown in the From field"
              />
            </Grid>
            <Grid item xs={12} sm={4} sx={{ display: 'flex', alignItems: 'center' }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={form.smtpTls}
                    onChange={(e) => set('smtpTls', e.target.checked)}
                    color="primary"
                  />
                }
                label="STARTTLS"
              />
            </Grid>
          </Grid>

          <Box mt={2} display="flex" justifyContent="flex-end">
            <Button
              variant="contained"
              startIcon={saving ? <CircularProgress size={16} color="inherit" /> : <SaveIcon />}
              onClick={save}
              disabled={saving}
            >
              Save Configuration
            </Button>
          </Box>
        </CardContent>
      </Card>

      {/* Test Connection */}
      <Card>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={700} mb={1}>Test Connection</Typography>
          <Typography variant="body2" color="text.secondary" mb={2}>
            Send a test email to verify the SMTP settings are working correctly.
          </Typography>
          <Stack direction="row" spacing={2} alignItems="flex-start">
            <TextField
              label="Send test email to"
              value={testEmail}
              onChange={(e) => setTestEmail(e.target.value)}
              size="small"
              sx={{ flexGrow: 1 }}
              placeholder="yourname@email.com"
              type="email"
            />
            <Button
              variant="outlined"
              startIcon={testing ? <CircularProgress size={16} /> : <SendIcon />}
              onClick={sendTest}
              disabled={testing || !testEmail.trim()}
              sx={{ whiteSpace: 'nowrap', mt: 0.2 }}
            >
              Send Test
            </Button>
          </Stack>

          {testResult && (
            <Alert
              severity={testResult.ok ? 'success' : 'error'}
              sx={{ mt: 2 }}
              onClose={() => setTestResult(null)}
            >
              {testResult.message}
            </Alert>
          )}
        </CardContent>
      </Card>

      {/* Common Office365 tips */}
      <Card variant="outlined" sx={{ bgcolor: 'grey.50' }}>
        <CardContent>
          <Typography variant="subtitle2" fontWeight={700} mb={1}>Office 365 Tips</Typography>
          <Divider sx={{ mb: 1 }} />
          <Stack spacing={0.5}>
            {[
              'Host: smtp.office365.com | Port: 587 | STARTTLS: ON',
              'Use your full email address as the username.',
              'If MFA is enabled, create an App Password in Microsoft 365 admin portal.',
              'Ensure "SMTP AUTH" is enabled for the mailbox in Exchange Online.',
            ].map((tip) => (
              <Typography key={tip} variant="body2" color="text.secondary">• {tip}</Typography>
            ))}
          </Stack>
        </CardContent>
      </Card>
    </Stack>
  );
}
