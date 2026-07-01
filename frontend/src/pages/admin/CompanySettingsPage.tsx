import { useEffect, useState } from 'react';
import {
  Button,
  Card,
  CardContent,
  Grid,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import BusinessIcon from '@mui/icons-material/Business';
import { adminApi } from '../../api/endpoints';
import { extractErrorMessage } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';
import type { CompanySettings } from '../../types';

const EMPTY: Omit<CompanySettings, 'id'> = {
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
};

export default function CompanySettingsPage() {
  const { notify } = useNotification();
  const [form, setForm] = useState<Omit<CompanySettings, 'id'>>(EMPTY);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setLoading(true);
    adminApi
      .getCompanySettings()
      .then((s) =>
        setForm({
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
        })
      )
      .catch((err) => notify(extractErrorMessage(err), 'error'))
      .finally(() => setLoading(false));
  }, [notify]);

  const f = (key: keyof typeof form) => ({
    value: form[key] ?? '',
    onChange: (e: React.ChangeEvent<HTMLInputElement>) =>
      setForm({ ...form, [key]: e.target.value || null }),
  });

  const save = async () => {
    setSaving(true);
    try {
      await adminApi.updateCompanySettings({
        companyName: form.companyName,
        addressLine1: form.addressLine1,
        addressLine2: form.addressLine2 || null,
        city: form.city,
        state: form.state,
        pincode: form.pincode,
        country: form.country,
        phone: form.phone || null,
        email: form.email || null,
        gstin: form.gstin || null,
      });
      notify('Company settings saved. Sender party updated.', 'success');
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Stack spacing={2}>
      <Stack direction="row" alignItems="center" spacing={1}>
        <BusinessIcon color="primary" />
        <Typography variant="h5" fontWeight={600}>Company Setup</Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        This information is printed as the <strong>Sender</strong> on every shipping sticker. Save
        to auto-update the sender party used across all bookings.
      </Typography>

      <Card>
        <CardContent>
          {loading ? (
            <Typography color="text.secondary">Loading…</Typography>
          ) : (
            <Grid container spacing={2}>
              <Grid item xs={12} sm={8}>
                <TextField
                  label="Company Name"
                  fullWidth
                  required
                  value={form.companyName}
                  onChange={(e) => setForm({ ...form, companyName: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField label="GSTIN" fullWidth {...f('gstin')} />
              </Grid>
              <Grid item xs={12}>
                <TextField label="Address Line 1" fullWidth required {...f('addressLine1')} />
              </Grid>
              <Grid item xs={12}>
                <TextField label="Address Line 2" fullWidth {...f('addressLine2')} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField label="City" fullWidth required {...f('city')} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField label="State" fullWidth required {...f('state')} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField label="Pincode" fullWidth required {...f('pincode')} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField label="Country" fullWidth required {...f('country')} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField label="Phone" fullWidth {...f('phone')} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField label="Email" fullWidth {...f('email')} />
              </Grid>
              <Grid item xs={12}>
                <Stack direction="row" justifyContent="flex-end">
                  <Button variant="contained" onClick={save} disabled={saving}>
                    {saving ? 'Saving…' : 'Save Settings'}
                  </Button>
                </Stack>
              </Grid>
            </Grid>
          )}
        </CardContent>
      </Card>
    </Stack>
  );
}
