import { useCallback, useEffect, useState } from 'react';
import {
  Alert, Box, Button, Chip, CircularProgress, Dialog, DialogActions,
  DialogContent, DialogTitle, Grid, IconButton, LinearProgress,
  Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow,
  Tooltip, Typography,
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import StorageIcon from '@mui/icons-material/Storage';
import MemoryIcon from '@mui/icons-material/Memory';
import DnsIcon from '@mui/icons-material/Dns';
import PeopleIcon from '@mui/icons-material/People';
import FolderIcon from '@mui/icons-material/Folder';
import SpeedIcon from '@mui/icons-material/Speed';
import LogoutIcon from '@mui/icons-material/Logout';
import LanIcon from '@mui/icons-material/Lan';
import DataObjectIcon from '@mui/icons-material/DataObject';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import MonitorHeartIcon from '@mui/icons-material/MonitorHeart';
import { api, extractErrorMessage } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';

interface SystemInfo {
  startedAt: string;
  uptimeSeconds: number;
  uptimeHuman: string;
  status: 'UP' | 'DOWN';
  dbStatus: string;
  redisStatus: string;
  heapUsedMb: number;
  heapMaxMb: number;
  heapCommittedMb: number;
  nonHeapUsedMb: number;
  heapUsedPercent: number;
  osName: string;
  osVersion: string;
  osArch: string;
  availableProcessors: number;
  systemLoadAverage: number;
  totalPhysicalMemoryMb: number;
  freePhysicalMemoryMb: number;
  usedPhysicalMemoryMb: number;
  physMemUsedPercent: number;
  jvmTotalUsedMb: number;
  jvmTotalCommittedMb: number;
  jvmMemUsedPercent: number;
  systemCpuLoadPercent: number;
  processCpuLoadPercent: number;
  javaVersion: string;
  javaVendor: string;
  jvmName: string;
  jvmArguments: string[];
  workingDirectory: string;
  jarPath: string;
  jarDirectory: string;
  hostname: string;
  localIpAddresses: string[];
  dbUrl: string;
  dbUsername: string;
  dbDriver: string;
  appName: string;
  springProfilesActive: string;
  threadCount: number;
  peakThreadCount: number;
  daemonThreadCount: number;
  businessStats: {
    totalBookings: number;
    pendingApprovalBookings: number;
    approvedBookings: number;
    totalParties: number;
    pendingApprovalParties: number;
    totalUsers: number;
  };
}

interface ActiveSession {
  userId: string;
  username: string;
  loginAt: string;
  ip: string;
  userAgent: string;
  expiry: string;
  expiresInSeconds: string;
}

function parseUserAgent(ua: string): string {
  if (!ua) return '';
  const browser =
    /Edg\//.test(ua) ? 'Edge' :
    /OPR\/|Opera/.test(ua) ? 'Opera' :
    /Chrome\//.test(ua) ? 'Chrome' :
    /Firefox\//.test(ua) ? 'Firefox' :
    /Safari\//.test(ua) ? 'Safari' : 'Browser';
  const os =
    /Windows NT 10/.test(ua) ? 'Windows 10/11' :
    /Windows NT/.test(ua) ? 'Windows' :
    /Mac OS X/.test(ua) ? 'macOS' :
    /Android/.test(ua) ? 'Android' :
    /iPhone|iPad/.test(ua) ? 'iOS' :
    /Linux/.test(ua) ? 'Linux' : '';
  return os ? `${browser} · ${os}` : browser;
}

function StatusBadge({ status }: { status: string }) {
  const up = status === 'UP';
  return (
    <Chip
      icon={up ? <CheckCircleIcon sx={{ fontSize: '14px !important' }} /> : <ErrorIcon sx={{ fontSize: '14px !important' }} />}
      label={status}
      color={up ? 'success' : 'error'}
      size="small"
      sx={{ fontWeight: 700, height: 22 }}
    />
  );
}

function GaugeBar({ value }: { value: number }) {
  const col = value > 85 ? 'error' : value > 65 ? 'warning' : 'success';
  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
        <Typography variant="caption" color="text.secondary">Usage</Typography>
        <Typography variant="caption" fontWeight={700}>{value}%</Typography>
      </Box>
      <LinearProgress
        variant="determinate"
        value={Math.min(value, 100)}
        color={col}
        sx={{ height: 8, borderRadius: 4, bgcolor: 'grey.100' }}
      />
    </Box>
  );
}

function InfoCard({ title, icon, color, children }: {
  title: string; icon: React.ReactNode; color: string; children: React.ReactNode;
}) {
  return (
    <Paper
      elevation={0}
      sx={{
        borderRadius: 3, overflow: 'hidden',
        border: '1px solid', borderColor: 'divider',
        boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
      }}
    >
      <Box
        sx={{
          px: 2.5, py: 1.75, borderBottom: '1px solid', borderColor: 'divider',
          background: `${color}08`, display: 'flex', alignItems: 'center', gap: 1,
        }}
      >
        <Box sx={{ color, display: 'flex' }}>{icon}</Box>
        <Typography variant="subtitle2" fontWeight={700} sx={{ color: 'text.primary' }}>
          {title}
        </Typography>
      </Box>
      <Box sx={{ p: 2.5 }}>{children}</Box>
    </Paper>
  );
}

function Row({ label, value, mono }: { label: string; value?: string | number | null; mono?: boolean }) {
  if (value == null || value === '') return null;
  return (
    <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 2, py: 0.5, borderBottom: '1px solid', borderColor: 'grey.100', '&:last-child': { border: 0 } }}>
      <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: 'nowrap', minWidth: 130 }}>{label}</Typography>
      <Typography variant="caption" fontWeight={600} sx={{ textAlign: 'right', fontFamily: mono ? 'monospace' : 'inherit', wordBreak: 'break-all' }}>
        {value}
      </Typography>
    </Box>
  );
}

export default function SystemStatusPage() {
  const { notify } = useNotification();
  const [info, setInfo] = useState<SystemInfo | null>(null);
  const [sessions, setSessions] = useState<ActiveSession[]>([]);
  const [loading, setLoading] = useState(false);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [lastRefresh, setLastRefresh] = useState<Date | null>(null);
  const [terminateTarget, setTerminateTarget] = useState<ActiveSession | null>(null);
  const [terminating, setTerminating] = useState(false);

  const loadInfo = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get<SystemInfo>('/system/info');
      setInfo(data);
      setLastRefresh(new Date());
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    } finally {
      setLoading(false);
    }
  }, [notify]);

  const loadSessions = useCallback(async () => {
    setSessionsLoading(true);
    try {
      const { data } = await api.get<ActiveSession[]>('/system/sessions');
      setSessions(data);
    } catch {
      // Non-critical
    } finally {
      setSessionsLoading(false);
    }
  }, []);

  const refresh = useCallback(() => { loadInfo(); loadSessions(); }, [loadInfo, loadSessions]);

  useEffect(() => {
    refresh();
    const interval = setInterval(refresh, 30000);
    return () => clearInterval(interval);
  }, [refresh]);

  const handleTerminate = async () => {
    if (!terminateTarget) return;
    setTerminating(true);
    try {
      await api.post(`/system/sessions/${terminateTarget.userId}/terminate`);
      notify(`Session terminated for ${terminateTarget.username}`, 'success');
      setTerminateTarget(null);
      loadSessions();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    } finally {
      setTerminating(false);
    }
  };

  const isUp = info?.status === 'UP';

  return (
    <Stack spacing={3}>
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Box
            sx={{
              width: 44, height: 44, borderRadius: 2.5,
              background: 'linear-gradient(135deg, #1d4ed8, #4f46e5)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}
          >
            <MonitorHeartIcon sx={{ color: 'white', fontSize: 22 }} />
          </Box>
          <Box>
            <Typography variant="h5" fontWeight={800} color="text.primary">System Control</Typography>
            <Typography variant="caption" color="text.secondary">
              Server health, active sessions, infrastructure details
            </Typography>
          </Box>
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          {lastRefresh && (
            <Typography variant="caption" color="text.disabled">
              Refreshed {lastRefresh.toLocaleTimeString()}
            </Typography>
          )}
          <Button
            variant="outlined"
            size="small"
            startIcon={loading ? <CircularProgress size={14} /> : <RefreshIcon />}
            onClick={refresh}
            disabled={loading}
            sx={{ borderRadius: 2, textTransform: 'none' }}
          >
            Refresh
          </Button>
        </Box>
      </Box>

      {loading && !info && <LinearProgress sx={{ borderRadius: 2 }} />}

      {info && (
        <Stack spacing={3}>
          {/* ── Status Banner ─────────────────────────────────────────────── */}
          <Box
            sx={{
              borderRadius: 3, px: 3, py: 2.5,
              background: isUp
                ? 'linear-gradient(135deg, #052e16, #14532d)'
                : 'linear-gradient(135deg, #450a0a, #7f1d1d)',
              border: '1px solid',
              borderColor: isUp ? '#16a34a40' : '#dc262640',
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              flexWrap: 'wrap', gap: 2,
            }}
          >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              {isUp
                ? <CheckCircleIcon sx={{ fontSize: 36, color: '#4ade80' }} />
                : <ErrorIcon sx={{ fontSize: 36, color: '#f87171' }} />}
              <Box>
                <Typography variant="h6" fontWeight={800} sx={{ color: 'white', lineHeight: 1.2 }}>
                  {isUp ? 'All Systems Operational' : 'System Degraded'}
                </Typography>
                <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.6)', mt: 0.25 }}>
                  Uptime: {info.uptimeHuman} · Started {new Date(info.startedAt).toLocaleString()}
                </Typography>
              </Box>
            </Box>
            <Stack direction="row" spacing={1}>
              <StatusBadge status={info.dbStatus} />
              <StatusBadge status={info.redisStatus} />
            </Stack>
          </Box>

          {/* ── Resource Gauges ────────────────────────────────────────────── */}
          <Grid container spacing={2}>
            {/* JVM Heap */}
            <Grid item xs={12} sm={6} md={3}>
              <Paper elevation={0} sx={{ borderRadius: 3, p: 2.5, border: '1px solid', borderColor: 'divider', boxShadow: '0 1px 4px rgba(0,0,0,0.06)' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1.5 }}>
                  <MemoryIcon sx={{ color: '#2563eb', fontSize: 20 }} />
                  <Typography variant="subtitle2" fontWeight={700}>JVM Heap</Typography>
                </Box>
                <Typography variant="h4" fontWeight={800} sx={{ mb: 0.25 }}>{info.heapUsedMb}</Typography>
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1.5 }}>
                  of {info.heapMaxMb} MB max
                </Typography>
                <GaugeBar value={info.heapUsedPercent} />
              </Paper>
            </Grid>

            {/* Physical RAM */}
            {info.totalPhysicalMemoryMb > 0 && (
              <Grid item xs={12} sm={6} md={3}>
                <Paper elevation={0} sx={{ borderRadius: 3, p: 2.5, border: '1px solid', borderColor: 'divider', boxShadow: '0 1px 4px rgba(0,0,0,0.06)' }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1.5 }}>
                    <SpeedIcon sx={{ color: '#7c3aed', fontSize: 20 }} />
                    <Typography variant="subtitle2" fontWeight={700}>App Memory (JVM)</Typography>
                  </Box>
                  <Typography variant="h4" fontWeight={800} sx={{ mb: 0.25 }}>{info.jvmTotalUsedMb ?? info.heapUsedMb} MB</Typography>
                  <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.5 }}>
                    Heap {info.heapUsedMb} MB · Non-Heap {info.nonHeapUsedMb} MB
                  </Typography>
                  <Typography variant="caption" sx={{ display: 'block', mb: 1, color: 'text.disabled', fontSize: '0.68rem' }}>
                    {info.heapUsedPercent}% of {info.heapMaxMb} MB heap max
                  </Typography>
                  <GaugeBar value={info.heapUsedPercent} />
                </Paper>
              </Grid>
            )}

            {/* CPU */}
            <Grid item xs={12} sm={6} md={3}>
              <Paper elevation={0} sx={{ borderRadius: 3, p: 2.5, border: '1px solid', borderColor: 'divider', boxShadow: '0 1px 4px rgba(0,0,0,0.06)' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1.5 }}>
                  <SpeedIcon sx={{ color: '#d97706', fontSize: 20 }} />
                  <Typography variant="subtitle2" fontWeight={700}>CPU</Typography>
                </Box>
                <Typography variant="h4" fontWeight={800} sx={{ mb: 0.25 }}>{info.systemCpuLoadPercent < 0 ? '—' : `${info.systemCpuLoadPercent}%`}</Typography>
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1.5 }}>
                  {info.availableProcessors} cores · Process: {info.processCpuLoadPercent}%
                </Typography>
                {info.systemCpuLoadPercent >= 0 && <GaugeBar value={info.systemCpuLoadPercent} />}
              </Paper>
            </Grid>

            {/* Threads */}
            <Grid item xs={12} sm={6} md={3}>
              <Paper elevation={0} sx={{ borderRadius: 3, p: 2.5, border: '1px solid', borderColor: 'divider', boxShadow: '0 1px 4px rgba(0,0,0,0.06)' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1.5 }}>
                  <AccountTreeIcon sx={{ color: '#059669', fontSize: 20 }} />
                  <Typography variant="subtitle2" fontWeight={700}>JVM Threads</Typography>
                </Box>
                <Typography variant="h4" fontWeight={800} sx={{ mb: 0.25 }}>{info.threadCount}</Typography>
                <Typography variant="caption" color="text.secondary">
                  Peak: {info.peakThreadCount} · Daemon: {info.daemonThreadCount}
                </Typography>
              </Paper>
            </Grid>
          </Grid>

          {/* ── Info Cards Grid ────────────────────────────────────────────── */}
          <Grid container spacing={2}>
            {/* Server */}
            <Grid item xs={12} md={6}>
              <InfoCard title="Server & OS" icon={<DnsIcon fontSize="small" />} color="#2563eb">
                <Row label="Hostname" value={info.hostname} />
                <Row label="OS" value={`${info.osName} ${info.osVersion}`} />
                <Row label="Architecture" value={info.osArch} />
                <Row label="CPU Cores" value={info.availableProcessors} />
                <Row label="Load Average" value={info.systemLoadAverage >= 0 ? info.systemLoadAverage.toFixed(2) : 'N/A'} />
                {info.localIpAddresses.map((ip, i) => (
                  <Row key={i} label={i === 0 ? 'Network IP(s)' : ''} value={ip} mono />
                ))}
              </InfoCard>
            </Grid>

            {/* JVM */}
            <Grid item xs={12} md={6}>
              <InfoCard title="Java / JVM" icon={<DataObjectIcon fontSize="small" />} color="#7c3aed">
                <Row label="Java Version" value={info.javaVersion} />
                <Row label="Vendor" value={info.javaVendor} />
                <Row label="JVM" value={info.jvmName} />
                <Row label="Non-Heap" value={`${info.nonHeapUsedMb} MB`} />
                <Row label="Heap Committed" value={`${info.heapCommittedMb} MB`} />
                <Row label="Profile" value={info.springProfilesActive} />
              </InfoCard>
            </Grid>

            {/* App / Paths */}
            <Grid item xs={12} md={6}>
              <InfoCard title="Application Paths" icon={<FolderIcon fontSize="small" />} color="#d97706">
                <Row label="App Name" value={info.appName} />
                <Row label="JAR Path" value={info.jarPath} mono />
                <Row label="JAR Directory" value={info.jarDirectory} mono />
                <Row label="Working Dir" value={info.workingDirectory} mono />
              </InfoCard>
            </Grid>

            {/* Database */}
            <Grid item xs={12} md={6}>
              <InfoCard title="Database" icon={<StorageIcon fontSize="small" />} color="#059669">
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1.5 }}>
                  <StatusBadge status={info.dbStatus} />
                </Box>
                <Row label="JDBC URL" value={info.dbUrl} mono />
                <Row label="Username" value={info.dbUsername} mono />
                <Row label="Driver" value={info.dbDriver} />
                <Row label="Redis" value={info.redisStatus} />
              </InfoCard>
            </Grid>

            {/* JVM Args */}
            {info.jvmArguments.length > 0 && (
              <Grid item xs={12}>
                <InfoCard title="JVM Arguments" icon={<DataObjectIcon fontSize="small" />} color="#64748b">
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.75 }}>
                    {info.jvmArguments.map((arg, i) => (
                      <Chip key={i} label={arg} size="small" sx={{ fontFamily: 'monospace', fontSize: 11, height: 22 }} />
                    ))}
                  </Box>
                </InfoCard>
              </Grid>
            )}
          </Grid>

          {/* ── Active Sessions ─────────────────────────────────────────────── */}
          <InfoCard title={`Active Sessions (${sessions.length})`} icon={<PeopleIcon fontSize="small" />} color="#dc2626">
            {sessionsLoading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}><CircularProgress size={24} /></Box>
            ) : sessions.length === 0 ? (
              <Box sx={{ textAlign: 'center', py: 4 }}>
                <PeopleIcon sx={{ fontSize: 36, color: 'text.disabled', mb: 1 }} />
                <Typography variant="body2" color="text.secondary">No active sessions tracked</Typography>
                <Typography variant="caption" color="text.disabled">Sessions appear here when users log in</Typography>
              </Box>
            ) : (
              <Box sx={{ overflowX: 'auto' }}>
                <Table size="small">
                  <TableHead>
                    <TableRow sx={{ '& th': { fontWeight: 700, bgcolor: 'grey.50', fontSize: 12 } }}>
                      <TableCell>User</TableCell>
                      <TableCell>User ID</TableCell>
                      <TableCell>Login Time</TableCell>
                      <TableCell>IP / Device</TableCell>
                      <TableCell>Session Expires</TableCell>
                      <TableCell align="right">Action</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {sessions.map((s) => (
                      <TableRow key={s.userId} hover>
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Box
                              sx={{
                                width: 28, height: 28, borderRadius: '50%',
                                background: 'linear-gradient(135deg, #2563eb, #7c3aed)',
                                display: 'flex', alignItems: 'center', justifyContent: 'center',
                                color: 'white', fontSize: 11, fontWeight: 700,
                              }}
                            >
                              {s.username?.charAt(0).toUpperCase()}
                            </Box>
                            <Typography variant="body2" fontWeight={600}>{s.username}</Typography>
                          </Box>
                        </TableCell>
                        <TableCell sx={{ fontFamily: 'monospace', fontSize: 12 }}>{s.userId}</TableCell>
                        <TableCell sx={{ fontSize: 12, whiteSpace: 'nowrap' }}>
                          {s.loginAt ? new Date(s.loginAt).toLocaleString() : '—'}
                        </TableCell>
                        <TableCell>
                          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.2 }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                              <LanIcon sx={{ fontSize: 14, color: 'text.disabled' }} />
                              <Typography variant="caption" fontFamily="monospace">{s.ip}</Typography>
                            </Box>
                            <Typography variant="caption" color="text.secondary" sx={{ fontSize: 10 }}>
                              {parseUserAgent(s.userAgent)}
                            </Typography>
                          </Box>
                        </TableCell>
                        <TableCell sx={{ fontSize: 12 }}>
                          {s.expiresInSeconds
                            ? `${Math.floor(Number(s.expiresInSeconds) / 60)} min`
                            : '—'}
                        </TableCell>
                        <TableCell align="right">
                          <Tooltip title="Force logout this user">
                            <IconButton
                              size="small"
                              color="error"
                              onClick={() => setTerminateTarget(s)}
                            >
                              <LogoutIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Box>
            )}
          </InfoCard>

          {/* ── Business Stats ────────────────────────────────────────────── */}
          <InfoCard title="Business Statistics" icon={<StorageIcon fontSize="small" />} color="#2563eb">
            <Grid container spacing={2}>
              {[
                { label: 'Total Bookings', value: info.businessStats.totalBookings, color: '#2563eb' },
                { label: 'Pending Approval', value: info.businessStats.pendingApprovalBookings, color: '#d97706' },
                { label: 'Approved', value: info.businessStats.approvedBookings, color: '#16a34a' },
                { label: 'Total Parties', value: info.businessStats.totalParties, color: '#7c3aed' },
                { label: 'Parties Pending', value: info.businessStats.pendingApprovalParties, color: '#ea580c' },
                { label: 'Total Users', value: info.businessStats.totalUsers, color: '#0891b2' },
              ].map(({ label, value, color }) => (
                <Grid item xs={6} sm={4} key={label}>
                  <Box sx={{ borderLeft: `3px solid ${color}`, pl: 2, py: 0.5 }}>
                    <Typography variant="caption" color="text.secondary">{label}</Typography>
                    <Typography variant="h5" fontWeight={800} sx={{ color, lineHeight: 1.2 }}>{value}</Typography>
                  </Box>
                </Grid>
              ))}
            </Grid>
          </InfoCard>
        </Stack>
      )}

      {/* Terminate confirm dialog */}
      <Dialog open={!!terminateTarget} onClose={() => setTerminateTarget(null)} maxWidth="xs" fullWidth>
        <DialogTitle sx={{ fontWeight: 700 }}>Terminate Session</DialogTitle>
        <DialogContent>
          <Alert severity="error" sx={{ borderRadius: 2 }}>
            Force-logout <strong>{terminateTarget?.username}</strong> (IP: {terminateTarget?.ip})?
            Their current session will be immediately invalidated.
          </Alert>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTerminateTarget(null)} disabled={terminating}>Cancel</Button>
          <Button
            variant="contained"
            color="error"
            onClick={handleTerminate}
            disabled={terminating}
            startIcon={terminating ? <CircularProgress size={16} color="inherit" /> : <LogoutIcon />}
          >
            Terminate Session
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
