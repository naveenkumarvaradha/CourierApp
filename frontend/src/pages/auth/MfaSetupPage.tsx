import { useState } from 'react';
import {
  Alert, Box, Button, CircularProgress, Divider, Paper,
  Step, StepLabel, Stepper, TextField, Typography,
} from '@mui/material';
import QrCode2Icon from '@mui/icons-material/QrCode2';
import { authApi } from '../../api/endpoints';
import type { MfaSetupResponse } from '../../types';

const STEPS = ['Generate QR Code', 'Scan & Verify', 'Done'];

export default function MfaSetupPage() {
  const [activeStep, setActiveStep] = useState(0);
  const [setup, setSetup] = useState<MfaSetupResponse | null>(null);
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleGenerate = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await authApi.setupMfa();
      setSetup(data);
      setActiveStep(1);
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to generate QR code');
    } finally {
      setLoading(false);
    }
  };

  const handleEnable = async () => {
    if (!/^\d{6}$/.test(code)) {
      setError('Enter the 6-digit code from your app');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await authApi.enableMfa(code);
      setSuccess('MFA enabled! Your account is now protected with two-factor authentication.');
      setActiveStep(2);
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Invalid OTP — please try again');
    } finally {
      setLoading(false);
    }
  };

  const handleDisable = async () => {
    setLoading(true);
    setError('');
    try {
      await authApi.disableMfa();
      setSuccess('MFA disabled.');
      setSetup(null);
      setCode('');
      setActiveStep(0);
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to disable MFA');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box maxWidth={600} mx="auto" mt={4}>
      <Paper elevation={2} sx={{ p: 4 }}>
        <Box display="flex" alignItems="center" gap={1} mb={3}>
          <QrCode2Icon color="primary" sx={{ fontSize: 32 }} />
          <Typography variant="h5" fontWeight={700}>Two-Factor Authentication (MFA)</Typography>
        </Box>

        <Stepper activeStep={activeStep} sx={{ mb: 4 }}>
          {STEPS.map((label) => (
            <Step key={label}><StepLabel>{label}</StepLabel></Step>
          ))}
        </Stepper>

        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}

        {activeStep === 0 && (
          <Box>
            <Typography variant="body1" mb={2}>
              Protect your account with an Authenticator app (Google Authenticator, Microsoft Authenticator, Authy, etc.).
            </Typography>
            <Button variant="contained" onClick={handleGenerate} disabled={loading}>
              {loading ? <CircularProgress size={22} color="inherit" /> : 'Generate QR Code'}
            </Button>
          </Box>
        )}

        {activeStep === 1 && setup && (
          <Box>
            <Typography variant="body2" mb={2}>
              Scan this QR code with your Authenticator app, then enter the 6-digit code below to confirm.
            </Typography>
            <Box display="flex" justifyContent="center" mb={2}>
              <img src={setup.qrDataUri} alt="MFA QR Code" style={{ width: 200, height: 200 }} />
            </Box>
            <Divider sx={{ my: 2 }} />
            <Typography variant="caption" color="text.secondary">
              Can't scan? Enter this key manually: <strong>{setup.secret}</strong>
            </Typography>
            <TextField
              label="6-digit OTP from app"
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              inputProps={{ inputMode: 'numeric', maxLength: 6 }}
              fullWidth
              sx={{ mt: 2, mb: 2 }}
              autoFocus
            />
            <Button
              variant="contained"
              onClick={handleEnable}
              disabled={loading || code.length !== 6}
              fullWidth
            >
              {loading ? <CircularProgress size={22} color="inherit" /> : 'Activate MFA'}
            </Button>
          </Box>
        )}

        {activeStep === 2 && (
          <Box>
            <Alert severity="success" sx={{ mb: 2 }}>
              MFA is active on your account. You will be asked for an OTP on every login.
            </Alert>
            <Button variant="outlined" color="error" onClick={handleDisable} disabled={loading}>
              {loading ? <CircularProgress size={22} color="inherit" /> : 'Disable MFA'}
            </Button>
          </Box>
        )}
      </Paper>
    </Box>
  );
}
