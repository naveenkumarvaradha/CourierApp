import { useState } from 'react';
import { Link as RouterLink, useNavigate, useSearchParams } from 'react-router-dom';
import {
  Alert, Box, Button, Card, CardContent, CircularProgress, Link, Stack, TextField, Typography,
} from '@mui/material';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import { authApi } from '../../api/endpoints';
import { extractErrorMessage } from '../../api/client';

export default function ResetPasswordPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const token = params.get('token') ?? '';
  const [newPassword, setNewPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword !== confirm) { setError('Passwords do not match'); return; }
    if (newPassword.length < 6) { setError('Password must be at least 6 characters'); return; }
    setError('');
    setLoading(true);
    try {
      await authApi.resetPassword(token, newPassword);
      navigate('/login', { state: { message: 'Password reset successfully. Please log in.' } });
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  if (!token) {
    return (
      <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
        background: 'linear-gradient(135deg, #1565c0 0%, #0d47a1 100%)', p: 2 }}>
        <Card sx={{ width: 420, maxWidth: '100%' }} elevation={8}>
          <CardContent sx={{ p: 4 }}>
            <Alert severity="error">Invalid reset link. Please request a new one.</Alert>
            <Box textAlign="center" mt={2}>
              <Link component={RouterLink} to="/forgot-password">Request new link</Link>
            </Box>
          </CardContent>
        </Card>
      </Box>
    );
  }

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'linear-gradient(135deg, #1565c0 0%, #0d47a1 100%)', p: 2 }}>
      <Card sx={{ width: 420, maxWidth: '100%' }} elevation={8}>
        <CardContent sx={{ p: 4 }}>
          <Stack alignItems="center" spacing={1} mb={3}>
            <LocalShippingIcon sx={{ fontSize: 48, color: 'primary.main' }} />
            <Typography variant="h5" fontWeight={700}>Reset Password</Typography>
            <Typography variant="body2" color="text.secondary">Enter your new password below.</Typography>
          </Stack>
          <form onSubmit={submit}>
            <Stack spacing={2}>
              {error && <Alert severity="error">{error}</Alert>}
              <TextField
                label="New Password"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                fullWidth required autoFocus
                inputProps={{ minLength: 6 }}
              />
              <TextField
                label="Confirm Password"
                type="password"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                fullWidth required
              />
              <Button type="submit" variant="contained" size="large" disabled={loading}
                startIcon={loading ? <CircularProgress size={18} color="inherit" /> : null}>
                {loading ? 'Resetting...' : 'Reset Password'}
              </Button>
            </Stack>
          </form>
        </CardContent>
      </Card>
    </Box>
  );
}
