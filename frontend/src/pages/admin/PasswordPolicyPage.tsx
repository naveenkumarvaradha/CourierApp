import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  FormControlLabel,
  Grid,
  InputAdornment,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import LockIcon from '@mui/icons-material/Lock';
import SecurityIcon from '@mui/icons-material/Security';
import TimerIcon from '@mui/icons-material/Timer';
import { adminApi } from '../../api/endpoints';
import { extractErrorMessage } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';
import type { PasswordPolicy } from '../../types';

const DEFAULTS: PasswordPolicy = {
  id: null,
  restrictLastPasswords: 5,
  passwordExpiryDays: 90,
  expiryReminderDays: 5,
  sessionTimeoutHours: 0,
  sessionTimeoutMinutes: 30,
  maxLoginAttempts: 5,
  minPasswordLength: 8,
  requireUppercase: true,
  requireLowercase: true,
  requireDigit: true,
  requireSpecialChar: false,
};

function SectionHeader({ icon, title }: { icon: React.ReactNode; title: string }) {
  return (
    <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 3 }}>
      {icon}
      <Typography variant="subtitle1" fontWeight={700}>{title}</Typography>
    </Stack>
  );
}

export default function PasswordPolicyPage() {
  const { notify } = useNotification();
  const [form, setForm] = useState<PasswordPolicy>(DEFAULTS);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setLoading(true);
    adminApi.getPasswordPolicy()
      .then((p) => setForm(p))
      .catch((err) => notify(extractErrorMessage(err), 'error'))
      .finally(() => setLoading(false));
  }, [notify]);

  const setNum = (key: keyof PasswordPolicy) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm({ ...form, [key]: parseInt(e.target.value) || 0 });

  const setBool = (key: keyof PasswordPolicy) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm({ ...form, [key]: e.target.checked });

  const save = async () => {
    setSaving(true);
    try {
      const updated = await adminApi.updatePasswordPolicy({ ...form });
      setForm(updated);
      notify('Password policy saved', 'success');
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <Typography color="text.secondary" sx={{ p: 2 }}>Loading…</Typography>;

  return (
    <Stack spacing={3}>
      <Stack direction="row" spacing={1} alignItems="center">
        <SecurityIcon color="primary" />
        <Typography variant="h5" fontWeight={600}>Password Policy</Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Configure password rules, session expiry, and account lockout behavior.
      </Typography>

      {/* ── Password History & Expiry ─────────────────────────────────────── */}
      <Card>
        <CardContent>
          <SectionHeader icon={<LockIcon color="action" />} title="Password History & Expiry" />
          <Grid container spacing={3}>
            <Grid item xs={12} sm={4}>
              <TextField
                label="Restrict Last Passwords"
                type="number"
                fullWidth
                value={form.restrictLastPasswords}
                onChange={setNum('restrictLastPasswords')}
                inputProps={{ min: 0, max: 24 }}
                helperText="User cannot reuse the last N passwords"
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                label="Password Expiry (days)"
                type="number"
                fullWidth
                value={form.passwordExpiryDays}
                onChange={setNum('passwordExpiryDays')}
                inputProps={{ min: 0 }}
                InputProps={{ endAdornment: <InputAdornment position="end">days</InputAdornment> }}
                helperText="0 = never expires"
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                label="Expiry Reminder (days before)"
                type="number"
                fullWidth
                value={form.expiryReminderDays}
                onChange={setNum('expiryReminderDays')}
                inputProps={{ min: 0 }}
                InputProps={{ endAdornment: <InputAdornment position="end">days</InputAdornment> }}
                helperText="Warn user N days before expiry"
              />
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* ── Session & Lockout ─────────────────────────────────────────────── */}
      <Card>
        <CardContent>
          <SectionHeader icon={<TimerIcon color="action" />} title="Session & Account Lockout" />
          <Grid container spacing={3}>
            <Grid item xs={12} sm={3}>
              <TextField
                label="Session Timeout — Hours"
                type="number"
                fullWidth
                value={form.sessionTimeoutHours}
                onChange={setNum('sessionTimeoutHours')}
                inputProps={{ min: 0 }}
                InputProps={{ endAdornment: <InputAdornment position="end">hrs</InputAdornment> }}
                helperText="Inactive logout (hours part)"
              />
            </Grid>
            <Grid item xs={12} sm={3}>
              <TextField
                label="Session Timeout — Minutes"
                type="number"
                fullWidth
                value={form.sessionTimeoutMinutes}
                onChange={setNum('sessionTimeoutMinutes')}
                inputProps={{ min: 0, max: 59 }}
                InputProps={{ endAdornment: <InputAdornment position="end">min</InputAdornment> }}
                helperText="Inactive logout (minutes part)"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Max. Unsuccessful Login Attempts"
                type="number"
                fullWidth
                value={form.maxLoginAttempts}
                onChange={setNum('maxLoginAttempts')}
                inputProps={{ min: 1, max: 20 }}
                helperText="Account is locked after this many failed attempts"
              />
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* ── Password Complexity ──────────────────────────────────────────── */}
      <Card>
        <CardContent>
          <SectionHeader icon={<LockIcon color="action" />} title="Password Complexity" />
          <Grid container spacing={3}>
            <Grid item xs={12} sm={3}>
              <TextField
                label="Minimum Password Length"
                type="number"
                fullWidth
                value={form.minPasswordLength}
                onChange={setNum('minPasswordLength')}
                inputProps={{ min: 4, max: 64 }}
                helperText="Minimum number of characters"
              />
            </Grid>
            <Grid item xs={12} sm={9}>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                Required character types:
              </Typography>
              <Stack direction="row" flexWrap="wrap" gap={1}>
                <FormControlLabel
                  control={<Checkbox checked={form.requireUppercase} onChange={setBool('requireUppercase')} />}
                  label="Uppercase (A–Z)"
                />
                <FormControlLabel
                  control={<Checkbox checked={form.requireLowercase} onChange={setBool('requireLowercase')} />}
                  label="Lowercase (a–z)"
                />
                <FormControlLabel
                  control={<Checkbox checked={form.requireDigit} onChange={setBool('requireDigit')} />}
                  label="Digit (0–9)"
                />
                <FormControlLabel
                  control={<Checkbox checked={form.requireSpecialChar} onChange={setBool('requireSpecialChar')} />}
                  label="Special Character (!@#$…)"
                />
              </Stack>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <Box>
        <Button variant="contained" size="large" onClick={save} disabled={saving}>
          {saving ? 'Saving…' : 'Save Policy'}
        </Button>
      </Box>
    </Stack>
  );
}
