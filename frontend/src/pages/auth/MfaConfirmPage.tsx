import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Alert, Box, Button, CircularProgress, Paper, Stack, TextField, Typography,
} from '@mui/material';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import ShieldIcon from '@mui/icons-material/Shield';
import { authApi } from '../../api/endpoints';
import { tokenStore } from '../../api/client';

export default function MfaConfirmPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const mfaPendingToken: string = (location.state as any)?.mfaPendingToken ?? '';

  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!mfaPendingToken) {
    navigate('/login', { replace: true });
    return null;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!/^\d{6}$/.test(code)) { setError('Enter the 6-digit code from your Authenticator app'); return; }
    setLoading(true); setError('');
    try {
      const tokens = await authApi.confirmMfa(mfaPendingToken, code);
      tokenStore.set(tokens.accessToken!, tokens.refreshToken!);
      navigate('/');
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Invalid OTP. Please try again.');
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
        p: 2,
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      <Box sx={{
        position: 'absolute', top: '-20%', right: '-10%', width: 400, height: 400,
        borderRadius: '50%', background: 'radial-gradient(circle, rgba(99,102,241,0.3) 0%, transparent 70%)',
        pointerEvents: 'none',
      }} />

      <Paper
        elevation={0}
        sx={{
          width: '100%',
          maxWidth: 400,
          borderRadius: 4,
          overflow: 'hidden',
          boxShadow: '0 32px 80px rgba(0,0,0,0.5), 0 0 0 1px rgba(255,255,255,0.08)',
          position: 'relative',
          zIndex: 1,
        }}
      >
        {/* Header */}
        <Box
          sx={{
            background: 'linear-gradient(135deg, #1d4ed8 0%, #4f46e5 100%)',
            px: 4, pt: 5, pb: 4, textAlign: 'center',
          }}
        >
          <Box
            sx={{
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
              width: 64, height: 64, borderRadius: '18px',
              background: 'rgba(255,255,255,0.15)', backdropFilter: 'blur(10px)',
              border: '1px solid rgba(255,255,255,0.2)', mb: 2,
              boxShadow: '0 8px 32px rgba(0,0,0,0.2)',
            }}
          >
            <ShieldIcon sx={{ fontSize: 32, color: 'white' }} />
          </Box>
          <Typography variant="h5" fontWeight={800} sx={{ color: 'white', letterSpacing: '-0.3px', mb: 0.5 }}>
            Two-Factor Auth
          </Typography>
          <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.65)' }}>
            Enter the 6-digit code from your Authenticator app
          </Typography>
        </Box>

        {/* Body */}
        <Box sx={{ px: 4, py: 4 }}>
          {error && <Alert severity="error" sx={{ mb: 2.5, borderRadius: 2, py: 0.5 }}>{error}</Alert>}

          <form onSubmit={handleSubmit}>
            <Stack spacing={3}>
              <TextField
                label="6-digit OTP"
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                inputProps={{ inputMode: 'numeric', maxLength: 6 }}
                fullWidth
                autoFocus
                size="small"
                sx={{
                  '& .MuiOutlinedInput-root': { borderRadius: 2 },
                  '& input': { textAlign: 'center', letterSpacing: 8, fontSize: '1.2rem', fontWeight: 700 },
                }}
              />

              {/* Progress dots */}
              <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1 }}>
                {Array.from({ length: 6 }).map((_, i) => (
                  <Box
                    key={i}
                    sx={{
                      width: 28, height: 4, borderRadius: 2,
                      bgcolor: i < code.length ? 'primary.main' : 'grey.200',
                      transition: 'background-color 0.2s ease',
                    }}
                  />
                ))}
              </Box>

              <Button
                type="submit"
                variant="contained"
                fullWidth
                size="large"
                disabled={loading || code.length !== 6}
                sx={{
                  py: 1.4, fontWeight: 700, borderRadius: 2.5, textTransform: 'none',
                  background: 'linear-gradient(135deg, #1d4ed8 0%, #4f46e5 100%)',
                  boxShadow: '0 4px 15px rgba(79,70,229,0.4)',
                  '&:hover': { transform: 'translateY(-1px)', boxShadow: '0 6px 20px rgba(79,70,229,0.5)' },
                  transition: 'all 0.2s',
                }}
              >
                {loading ? <CircularProgress size={20} color="inherit" /> : 'Verify & Sign In'}
              </Button>

              <Button
                variant="text"
                fullWidth
                onClick={() => navigate('/login')}
                startIcon={<LockOutlinedIcon fontSize="small" />}
                sx={{ textTransform: 'none', color: 'text.secondary' }}
              >
                Back to Login
              </Button>
            </Stack>
          </form>
        </Box>
      </Paper>
    </Box>
  );
}
