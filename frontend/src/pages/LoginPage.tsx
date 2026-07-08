import { useEffect, useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import {
  Alert, Box, Button, CircularProgress, Divider,
  Link, MenuItem, Paper, Stack, TextField, Typography,
} from '@mui/material';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import { useAuth, MfaRequiredError } from '../context/AuthContext';
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
      if (err instanceof MfaRequiredError) {
        navigate('/mfa-confirm', { state: { mfaPendingToken: err.mfaPendingToken }, replace: true });
        return;
      }
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
        background: 'linear-gradient(145deg, #0f172a 0%, #1e3a8a 45%, #312e81 100%)',
        p: { xs: 2, sm: 3 },
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      {/* Decorative blobs */}
      <Box sx={{
        position: 'absolute', top: '-20%', right: '-10%', width: 500, height: 500,
        borderRadius: '50%', background: 'radial-gradient(circle, rgba(99,102,241,0.3) 0%, transparent 70%)',
        pointerEvents: 'none',
      }} />
      <Box sx={{
        position: 'absolute', bottom: '-15%', left: '-8%', width: 400, height: 400,
        borderRadius: '50%', background: 'radial-gradient(circle, rgba(59,130,246,0.25) 0%, transparent 70%)',
        pointerEvents: 'none',
      }} />

      {/* Card */}
      <Paper
        elevation={0}
        sx={{
          width: '100%',
          maxWidth: 440,
          borderRadius: 4,
          overflow: 'hidden',
          boxShadow: '0 32px 80px rgba(0,0,0,0.5), 0 0 0 1px rgba(255,255,255,0.08)',
          position: 'relative',
          zIndex: 1,
        }}
      >
        {/* Brand header */}
        <Box
          sx={{
            background: 'linear-gradient(135deg, #1d4ed8 0%, #4f46e5 100%)',
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
              width: 72,
              height: 72,
              borderRadius: '22px',
              background: 'rgba(255,255,255,0.15)',
              backdropFilter: 'blur(10px)',
              border: '1px solid rgba(255,255,255,0.2)',
              mb: 2.5,
              boxShadow: '0 8px 32px rgba(0,0,0,0.2)',
            }}
          >
            <LocalShippingIcon sx={{ fontSize: 36, color: 'white' }} />
          </Box>
          <Typography
            variant="h4"
            sx={{
              color: 'white',
              fontWeight: 800,
              letterSpacing: '-0.5px',
              lineHeight: 1,
              mb: 0.75,
            }}
          >
            ShipDesk
          </Typography>
        </Box>

        {/* Form section */}
        <Box sx={{ px: { xs: 3, sm: 5 }, py: { xs: 4, sm: 5 }, bgcolor: 'background.paper' }}>
          <Typography variant="subtitle1" fontWeight={700} color="text.primary" sx={{ mb: 3 }}>
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
                sx={fieldSx}
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
                sx={fieldSx}
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
                sx={fieldSx}
              />

              <Button
                type="submit"
                variant="contained"
                size="large"
                disabled={loading}
                fullWidth
                sx={{
                  mt: 0.5,
                  py: 1.4,
                  fontWeight: 700,
                  fontSize: '0.95rem',
                  borderRadius: 2.5,
                  background: 'linear-gradient(135deg, #1d4ed8 0%, #4f46e5 100%)',
                  boxShadow: '0 4px 15px rgba(79,70,229,0.4)',
                  textTransform: 'none',
                  letterSpacing: 0.2,
                  '&:hover': {
                    background: 'linear-gradient(135deg, #1e40af 0%, #4338ca 100%)',
                    boxShadow: '0 6px 20px rgba(79,70,229,0.5)',
                    transform: 'translateY(-1px)',
                  },
                  '&:active': { transform: 'translateY(0)' },
                  transition: 'all 0.2s ease',
                }}
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
                  sx={{ color: 'primary.main', fontWeight: 500, fontSize: 13 }}
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

const fieldSx = {
  '& .MuiOutlinedInput-root': {
    borderRadius: 2,
    '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: 'primary.main' },
  },
};
