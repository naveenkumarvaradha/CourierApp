import { useEffect, useState, useCallback } from 'react';
import {
  Box, Typography, Paper, Grid, TextField, MenuItem,
  Button, Chip,
} from '@mui/material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import { adminApi } from '../../api/endpoints';
import type { AuditLog } from '../../types';

const MODULES = [
  'ALL', 'USER', 'ROLE', 'PARTY', 'BOOKING', 'COMPANY',
  'COURIER_WAY', 'PACKAGE_TYPE', 'DEPARTMENT', 'FLEX_FIELD',
  'APPROVAL_ROUTING', 'AUTH',
];

const ACTIONS = [
  'ALL', 'CREATE', 'UPDATE', 'DELETE', 'APPROVE', 'REJECT',
  'SUBMIT', 'STATUS_CHANGE', 'AWB_UPDATE', 'LOGIN', 'LOGIN_FAILED', 'PASSWORD_CHANGE',
];

const actionColor = (action: string): 'default' | 'success' | 'error' | 'warning' | 'info' | 'primary' => {
  switch (action) {
    case 'CREATE': return 'success';
    case 'DELETE': return 'error';
    case 'REJECT': case 'LOGIN_FAILED': return 'error';
    case 'APPROVE': return 'primary';
    case 'UPDATE': case 'PASSWORD_CHANGE': return 'warning';
    case 'SUBMIT': case 'AWB_UPDATE': case 'STATUS_CHANGE': return 'info';
    default: return 'default';
  }
};

export default function AuditLogsPage() {
  const [rows, setRows] = useState<AuditLog[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(50);
  const [loading, setLoading] = useState(false);

  const [module, setModule] = useState('ALL');
  const [action, setAction] = useState('ALL');
  const [performedBy, setPerformedBy] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await adminApi.searchAuditLogs({
        module: module !== 'ALL' ? module : undefined,
        action: action !== 'ALL' ? action : undefined,
        performedBy: performedBy || undefined,
        fromDate: fromDate || undefined,
        toDate: toDate || undefined,
        page,
        size: pageSize,
      });
      setRows(res.content);
      setTotal(res.totalElements);
    } catch {
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [module, action, performedBy, fromDate, toDate, page, pageSize]);

  useEffect(() => { load(); }, [load]);

  const handleSearch = () => {
    setPage(0);
    load();
  };

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'module', headerName: 'Module', width: 130 },
    {
      field: 'action', headerName: 'Action', width: 140,
      renderCell: (params) => (
        <Chip label={params.value} color={actionColor(params.value)} size="small" />
      ),
    },
    { field: 'entityName', headerName: 'Entity', width: 200, valueFormatter: (v) => v ?? '—' },
    { field: 'performedBy', headerName: 'Performed By', width: 150 },
    { field: 'details', headerName: 'Details', flex: 1, valueFormatter: (v) => v ?? '' },
    {
      field: 'createdAt', headerName: 'Date / Time', width: 180,
      valueFormatter: (v) => v ? new Date(v).toLocaleString() : '',
    },
  ];

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} mb={2}>Audit Logs</Typography>

      <Paper sx={{ p: 2, mb: 2 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={6} md={2}>
            <TextField select fullWidth size="small" label="Module" value={module}
              onChange={(e) => setModule(e.target.value)}>
              {MODULES.map((m) => <MenuItem key={m} value={m}>{m}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <TextField select fullWidth size="small" label="Action" value={action}
              onChange={(e) => setAction(e.target.value)}>
              {ACTIONS.map((a) => <MenuItem key={a} value={a}>{a}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <TextField fullWidth size="small" label="Performed By" value={performedBy}
              onChange={(e) => setPerformedBy(e.target.value)} />
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <TextField fullWidth size="small" label="From Date" type="date"
              InputLabelProps={{ shrink: true }} value={fromDate}
              onChange={(e) => setFromDate(e.target.value)} />
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <TextField fullWidth size="small" label="To Date" type="date"
              InputLabelProps={{ shrink: true }} value={toDate}
              onChange={(e) => setToDate(e.target.value)} />
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <Button variant="contained" fullWidth onClick={handleSearch}>Search</Button>
          </Grid>
        </Grid>
      </Paper>

      <Paper>
        <DataGrid
          rows={rows}
          columns={columns}
          rowCount={total}
          paginationMode="server"
          paginationModel={{ page, pageSize }}
          onPaginationModelChange={(m) => setPage(m.page)}
          loading={loading}
          autoHeight
          disableRowSelectionOnClick
          density="compact"
        />
      </Paper>
    </Box>
  );
}
