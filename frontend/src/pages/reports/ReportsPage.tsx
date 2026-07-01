import { useCallback, useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Grid,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import DownloadIcon from '@mui/icons-material/Download';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import { reportApi } from '../../api/endpoints';
import { extractErrorMessage } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';
import type { Booking, ReportSummary } from '../../types';

export default function ReportsPage() {
  const { notify } = useNotification();
  const [granularity, setGranularity] = useState('monthly');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [summary, setSummary] = useState<ReportSummary | null>(null);
  const [loading, setLoading] = useState(false);

  const params = useCallback(() => {
    const p: { granularity: string; from?: string; to?: string } = { granularity };
    if (granularity === 'custom') {
      p.from = from;
      p.to = to;
    }
    return p;
  }, [granularity, from, to]);

  const load = useCallback(async () => {
    if (granularity === 'custom' && (!from || !to)) return;
    setLoading(true);
    try {
      setSummary(await reportApi.summary(params()));
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    } finally {
      setLoading(false);
    }
  }, [granularity, from, to, params, notify]);

  useEffect(() => {
    load();
  }, [load]);

  const exportExcel = async () => {
    try {
      const blob = await reportApi.exportExcel(params());
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `booking-report-${granularity}.xlsx`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const bookingColumns: GridColDef<Booking>[] = [
    { field: 'bookingNumber', headerName: 'Booking No', flex: 1.2 },
    { field: 'bookingDate', headerName: 'Date', width: 110 },
    { field: 'courierMode', headerName: 'Mode', width: 100 },
    { field: 'status', headerName: 'Status', width: 140 },
    {
      field: 'sender',
      headerName: 'Sender',
      flex: 1,
      valueGetter: (_v, row) => row.sender?.partyName,
    },
    { field: 'totalCharges', headerName: 'Charges', width: 110 },
  ];

  return (
    <Stack spacing={2}>
      <Typography variant="h5" fontWeight={600}>
        Reports
      </Typography>

      <Card>
        <CardContent>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems="center">
            <ToggleButtonGroup
              exclusive
              size="small"
              value={granularity}
              onChange={(_e, v) => v && setGranularity(v)}
            >
              <ToggleButton value="weekly">Weekly</ToggleButton>
              <ToggleButton value="monthly">Monthly</ToggleButton>
              <ToggleButton value="yearly">Yearly</ToggleButton>
              <ToggleButton value="custom">Custom</ToggleButton>
            </ToggleButtonGroup>

            {granularity === 'custom' && (
              <>
                <TextField
                  size="small"
                  label="From"
                  type="date"
                  InputLabelProps={{ shrink: true }}
                  value={from}
                  onChange={(e) => setFrom(e.target.value)}
                />
                <TextField
                  size="small"
                  label="To"
                  type="date"
                  InputLabelProps={{ shrink: true }}
                  value={to}
                  onChange={(e) => setTo(e.target.value)}
                />
                <Button variant="outlined" onClick={load}>
                  Apply
                </Button>
              </>
            )}

            <Box flexGrow={1} />
            <Button variant="contained" startIcon={<DownloadIcon />} onClick={exportExcel}>
              Export Excel
            </Button>
          </Stack>
        </CardContent>
      </Card>

      {summary && (
        <>
          <Grid container spacing={2}>
            <StatCard label="Total Bookings" value={summary.totalBookings} />
            <StatCard label="Total Charges" value={`₹ ${summary.totalCharges}`} />
            <StatCard label="Total Freight" value={`₹ ${summary.totalFreight}`} />
            <StatCard label="Declared Value" value={`₹ ${summary.totalDeclaredValue}`} />
          </Grid>

          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <BreakdownTable title="By Status" data={summary.countByStatus} />
            </Grid>
            <Grid item xs={12} md={6}>
              <BreakdownTable title="By Courier Mode" data={summary.countByMode} />
            </Grid>
          </Grid>

          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <PartyTable title="Top Senders" rows={summary.bySender} />
            </Grid>
            <Grid item xs={12} md={6}>
              <PartyTable title="Top Receivers" rows={summary.byReceiver} />
            </Grid>
          </Grid>

          <Typography variant="h6">Bookings ({summary.fromDate} to {summary.toDate})</Typography>
          <Box sx={{ height: 460, bgcolor: 'background.paper' }}>
            <DataGrid
              rows={summary.bookings}
              columns={bookingColumns}
              loading={loading}
              disableRowSelectionOnClick
              pageSizeOptions={[10, 25, 50]}
              initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
            />
          </Box>
        </>
      )}
    </Stack>
  );
}

function StatCard({ label, value }: { label: string; value: string | number }) {
  return (
    <Grid item xs={12} sm={6} md={3}>
      <Card>
        <CardContent>
          <Typography variant="body2" color="text.secondary">
            {label}
          </Typography>
          <Typography variant="h5" fontWeight={600}>
            {value}
          </Typography>
        </CardContent>
      </Card>
    </Grid>
  );
}

function BreakdownTable({ title, data }: { title: string; data: Record<string, number> }) {
  return (
    <TableContainer component={Paper}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell colSpan={2}>
              <Typography fontWeight={600}>{title}</Typography>
            </TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {Object.entries(data).length === 0 && (
            <TableRow>
              <TableCell colSpan={2}>No data</TableCell>
            </TableRow>
          )}
          {Object.entries(data).map(([k, v]) => (
            <TableRow key={k}>
              <TableCell>{k.replace('_', ' ')}</TableCell>
              <TableCell align="right">{v}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

function PartyTable({
  title,
  rows,
}: {
  title: string;
  rows: { partyCode: string; partyName: string; bookingCount: number; totalCharges: number }[];
}) {
  return (
    <TableContainer component={Paper}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell colSpan={3}>
              <Typography fontWeight={600}>{title}</Typography>
            </TableCell>
          </TableRow>
          <TableRow>
            <TableCell>Party</TableCell>
            <TableCell align="right">Bookings</TableCell>
            <TableCell align="right">Charges</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.length === 0 && (
            <TableRow>
              <TableCell colSpan={3}>No data</TableCell>
            </TableRow>
          )}
          {rows.slice(0, 10).map((r) => (
            <TableRow key={r.partyCode}>
              <TableCell>{r.partyName}</TableCell>
              <TableCell align="right">{r.bookingCount}</TableCell>
              <TableCell align="right">{r.totalCharges}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
