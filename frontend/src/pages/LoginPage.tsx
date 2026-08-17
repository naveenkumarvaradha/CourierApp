import { useEffect, useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import {
  Alert, Box, Button, CircularProgress, Divider,
  Link, MenuItem, Paper, Stack, TextField, Typography,
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
        bgcolor: '#f5f5f7',
        p: { xs: 2, sm: 3 },
      }}
    >
      {/* Card — the color change from parchment to white canvas is the only
          separation needed; no shadow, per the single-accent flat design language. */}
      <Paper
        elevation={0}
        sx={{
          width: '100%',
          maxWidth: 440,
          borderRadius: '18px',
          overflow: 'hidden',
          border: '1px solid',
          borderColor: 'divider',
        }}
      >
        {/* Brand header */}
        <Box
          sx={{
            px: { xs: 4, sm: 5 },
            pt: { xs: 5, sm: 6 },
            pb: { xs: 4, sm: 5 },
            textAlign: 'center',
          }}
        >
          <Box
            sx={{
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 64,
              height: 64,
              borderRadius: '18px',
              bgcolor: 'primary.main',
              mb: 2.5,
            }}
          >
            <LocalShippingIcon sx={{ fontSize: 32, color: 'white' }} />
          </Box>
          <Typography
            variant="h4"
            sx={{
              color: 'text.primary',
              fontWeight: 600,
              letterSpacing: '-0.02em',
              lineHeight: 1,
              mb: 0.75,
            }}
          >
            ShipDesk
          </Typography>
        </Box>

        {/* Form section */}
        <Box sx={{ px: { xs: 3, sm: 5 }, py: { xs: 4, sm: 5 }, bgcolor: 'background.paper' }}>
          <Typography variant="subtitle1" fontWeight={600} color="text.primary" sx={{ mb: 3 }}>
            Sign in to your account
          </Typography>

          <form onSubmit={submit}>
            <Stack spacing={2.5}>
              {error && (
                <Alert
                  severity="error"
                  sx={{ borderRadius: 2, fontSize: 13, py: 0.5 }}
                >
                  {error}
                </Alert>
              )}

              <TextField
                select
                label="Company"
                value={companyCode}
                onChange={(e) => setCompanyCode(e.target.value)}
                fullWidth
                required
                disabled={companies.length === 0}
                size="small"
                SelectProps={{ MenuProps: { transitionDuration: 0 } }}
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
                size="small"
              />

              <TextField
                label="Password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                fullWidth
                required
                autoComplete="current-password"
                size="small"
              />

              <Button
                type="submit"
                variant="contained"
                size="large"
                disabled={loading}
                fullWidth
                sx={{ mt: 0.5, py: 1.4, fontSize: '1rem' }}
              >
                {loading
                  ? <CircularProgress size={20} color="inherit" />
                  : 'Sign In'}
              </Button>

              <Box sx={{ textAlign: 'center' }}>
                <Link
                  component={RouterLink}
                  to="/forgot-password"
                  variant="body2"
                  underline="hover"
                  sx={{ color: 'primary.main', fontWeight: 400, fontSize: 13 }}
                >
                  Forgot Password?
                </Link>
              </Box>
            </Stack>
          </form>
        </Box>

        <Divider />
        {/* Footer */}
        <Box sx={{ bgcolor: 'grey.50', px: 4, py: 1.75, textAlign: 'center' }}>
          <Typography variant="caption" color="text.disabled" sx={{ fontSize: 11 }}>
            © {new Date().getFullYear()} ShipDesk
          </Typography>
        </Box>
      </Paper>
    </Box>
  );
}

