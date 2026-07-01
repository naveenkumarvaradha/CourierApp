import { useState } from 'react';
import {
  Alert, Button, CircularProgress, Dialog, DialogActions, DialogContent,
  DialogTitle, Stack, TextField,
} from '@mui/material';
import { authApi } from '../api/endpoints';
import { extractErrorMessage } from '../api/client';

interface Props {
  open: boolean;
  onClose: () => void;
}

export default function ChangePasswordDialog({ open, onClose }: Props) {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const reset = () => {
    setCurrentPassword(''); setNewPassword(''); setConfirm('');
    setError(''); setSuccess('');
  };

  const handleClose = () => { reset(); onClose(); };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword !== confirm) { setError('Passwords do not match'); return; }
    if (newPassword.length < 6) { setError('Password must be at least 6 characters'); return; }
    setError(''); setLoading(true);
    try {
      await authApi.changePassword(currentPassword, newPassword);
      setSuccess('Password changed successfully.');
      setTimeout(handleClose, 1500);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="xs" fullWidth>
      <DialogTitle>Change Password</DialogTitle>
      <form onSubmit={submit}>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            {error && <Alert severity="error">{error}</Alert>}
            {success && <Alert severity="success">{success}</Alert>}
            <TextField
              label="Current Password"
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              fullWidth required autoFocus
            />
            <TextField
              label="New Password"
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              fullWidth required inputProps={{ minLength: 6 }}
            />
            <TextField
              label="Confirm New Password"
              type="password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              fullWidth required
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={handleClose}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={loading}
            startIcon={loading ? <CircularProgress size={16} color="inherit" /> : null}>
            Change Password
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
