import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
  Alert, Box, Button, Card, CardContent, CircularProgress, Link, Stack, TextField, Typography,
} from '@mui/material';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import { authApi } from '../../api/endpoints';
import { extractErrorMessage } from '../../api/client';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);
    try {
      const res = await authApi.forgotPassword(email);
      setMessage(res.message);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'linear-gradient(135deg, #1565c0 0%, #0d47a1 100%)', p: 2 }}>
      <Card sx={{ width: 420, maxWidth: '100%' }} elevation={8}>
        <CardContent sx={{ p: 4 }}>
          <Stack alignItems="center" spacing={1} mb={3}>
            <LocalShippingIcon sx={{ fontSize: 48, color: 'primary.main' }} />
            <Typography variant="h5" fontWeight={700}>Forgot Password</Typography>
            <Typography variant="body2" color="text.secondary" textAlign="center">
              Enter your registered email address and we'll send you a reset link.
            </Typography>
          </Stack>
          {message ? (
            <Stack spacing={2}>
              <Alert severity="success">{message}</Alert>
              <Link component={RouterLink} to="/login" variant="body2" textAlign="center">
                Back to Login
              </Link>
            </Stack>
          ) : (
            <form onSubmit={submit}>
              <Stack spacing={2}>
                {error && <Alert severity="error">{error}</Alert>}
                <TextField
                  label="Email Address"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  fullWidth
                  required
                  autoFocus
                />
                <Button type="submit" variant="contained" size="large" disabled={loading}
                  startIcon={loading ? <CircularProgress size={18} color="inherit" /> : null}>
                  {loading ? 'Sending...' : 'Send Reset Link'}
                </Button>
                <Box textAlign="center">
                  <Link component={RouterLink} to="/login" variant="body2">Back to Login</Link>
                </Box>
              </Stack>
            </form>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
