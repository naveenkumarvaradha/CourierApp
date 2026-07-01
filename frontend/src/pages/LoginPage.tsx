import { useEffect, useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import {
  Alert, Box, Button, Card, CardContent, CircularProgress,
  Link, MenuItem, Stack, TextField, Typography,
} from '@mui/material';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../api/endpoints';
import { extractErrorMessage } from '../api/client';
import type { Company } from '../types';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [companies, setCompanies] = useState<Company[]>([]);
  const [companyCode, setCompanyCode] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    authApi.listCompanies()
      .then((list) => {
        setCompanies(list);
        if (list.length === 1) setCompanyCode(list[0].companyCode);
      })
      .catch(() => {});
  }, []);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(companyCode, username, password);
      navigate('/', { replace: true });
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #1565c0 0%, #0d47a1 100%)',
        p: 2,
      }}
    >
      <Card sx={{ width: 420, maxWidth: '100%' }} elevation={8}>
        <CardContent sx={{ p: 4 }}>
          <Stack alignItems="center" spacing={1} mb={3}>
            <LocalShippingIcon sx={{ fontSize: 48, color: 'primary.main' }} />
            <Typography variant="h5" fontWeight={700}>Courier Booking System</Typography>
            <Typography variant="body2" color="text.secondary">Sign in to continue</Typography>
          </Stack>
          <form onSubmit={submit}>
            <Stack spacing={2}>
              {error && <Alert severity="error">{error}</Alert>}
              <TextField
                select
                label="Company"
                value={companyCode}
                onChange={(e) => setCompanyCode(e.target.value)}
                fullWidth
                required
                disabled={companies.length === 0}
              >
                {companies.map((c) => (
                  <MenuItem key={c.id} value={c.companyCode}>{c.name}</MenuItem>
                ))}
              </TextField>
              <TextField
                label="Username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                fullWidth
                required
                autoComplete="username"
              />
              <TextField
                label="Password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                fullWidth
                required
                autoComplete="current-password"
              />
              <Button
                type="submit"
                variant="contained"
                size="large"
                disabled={loading}
                startIcon={loading ? <CircularProgress size={18} color="inherit" /> : null}
              >
                {loading ? 'Signing in...' : 'Sign In'}
              </Button>
              <Box textAlign="right">
                <Link component={RouterLink} to="/forgot-password" variant="body2">
                  Forgot Password?
                </Link>
              </Box>
            </Stack>
          </form>
        </CardContent>
      </Card>
    </Box>
  );
}
