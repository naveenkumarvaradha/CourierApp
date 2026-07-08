import { useState } from 'react';
import {
  Box,
  Button,
  ButtonGroup,
  Card,
  CardContent,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import TableChartIcon from '@mui/icons-material/TableChart';
import { reportApi } from '../../api/endpoints';
import { extractBlobError } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';

const today = new Date().toISOString().slice(0, 10);
const monthStart = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().slice(0, 10);

const BOOKING_STATUSES: { value: string; label: string }[] = [
  { value: 'ALL', label: 'All Statuses' },
  { value: 'BOOKED', label: 'BOOKED' },
  { value: 'PENDING_APPROVAL', label: 'PENDING APPROVAL' },
  { value: 'APPROVED', label: 'APPROVED' },
  { value: 'IN_TRANSIT', label: 'IN TRANSIT' },
  { value: 'DELIVERED', label: 'DELIVERED' },
  { value: 'CANCELLED', label: 'CANCELLED' },
  { value: 'REJECTED', label: 'REJECTED' },
  { value: 'PENDING_CANCELLATION', label: 'PENDING CANCELLATION' },
];

function triggerDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

interface DateRangeProps {
  from: string;
  to: string;
  onFromChange: (v: string) => void;
  onToChange: (v: string) => void;
}

function DateRangePicker({ from, to, onFromChange, onToChange }: DateRangeProps) {
  return (
    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">
      <TextField
        label="From"
        type="date"
        size="small"
        value={from}
        onChange={(e) => onFromChange(e.target.value)}
        InputLabelProps={{ shrink: true }}
      />
      <TextField
        label="To"
        type="date"
        size="small"
        value={to}
        onChange={(e) => onToChange(e.target.value)}
        InputLabelProps={{ shrink: true }}
      />
    </Stack>
  );
}

// ─── Booking Reports Tab ──────────────────────────────────────────────────────

function BookingReportsTab() {
  const { notify } = useNotification();
  const [from, setFrom] = useState(monthStart);
  const [to, setTo] = useState(today);
  const [status, setStatus] = useState('ALL');
  const [loading, setLoading] = useState('');

  const dl = async (type: 'excel' | 'pdf') => {
    setLoading(type);
    const statusParam = status === 'ALL' ? undefined : status;
    try {
      const blob = type === 'excel'
        ? await reportApi.bookingDetailExcel(from, to, statusParam)
        : await reportApi.bookingDetailPdf(from, to, statusParam);
      const ext = type === 'excel' ? 'xlsx' : 'pdf';
      triggerDownload(blob, `booking-report-${from}-${to}.${ext}`);
    } catch (err) {
      notify(await extractBlobError(err), 'error');
    } finally {
      setLoading('');
    }
  };

  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="subtitle1" fontWeight={600} mb={2}>
          Shipment Detail Report
        </Typography>
        <Typography variant="body2" color="text.secondary" mb={2}>
          All shipment details with full audit log for each booking. Filter by date range and status.
          Excel includes a separate Audit Log sheet.
        </Typography>
        <Stack spacing={2}>
          <DateRangePicker from={from} to={to} onFromChange={setFrom} onToChange={setTo} />
          <FormControl size="small" sx={{ minWidth: 200 }}>
            <InputLabel>Status (optional)</InputLabel>
            <Select
              label="Status (optional)"
              value={status}
              onChange={(e) => setStatus(e.target.value)}
            >
              {BOOKING_STATUSES.map((s) => (
                <MenuItem key={s.value} value={s.value}>{s.label}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <ButtonGroup variant="contained" size="small">
            <Button
              startIcon={<TableChartIcon />}
              onClick={() => dl('excel')}
              disabled={!!loading}
            >
              {loading === 'excel' ? 'Generating…' : 'Download Excel'}
            </Button>
            <Button
              startIcon={<PictureAsPdfIcon />}
              onClick={() => dl('pdf')}
              disabled={!!loading}
              color="error"
            >
              {loading === 'pdf' ? 'Generating…' : 'Download PDF'}
            </Button>
          </ButtonGroup>
        </Stack>
      </CardContent>
    </Card>
  );
}

// ─── User Reports Tab ─────────────────────────────────────────────────────────

function UserReportsTab() {
  const { notify } = useNotification();
  const [creationFrom, setCreationFrom] = useState(monthStart);
  const [creationTo, setCreationTo] = useState(today);
  const [inactiveFrom, setInactiveFrom] = useState(monthStart);
  const [inactiveTo, setInactiveTo] = useState(today);
  const [loading, setLoading] = useState('');

  const dl = async (kind: 'creationExcel' | 'creationPdf' | 'inactiveExcel' | 'inactivePdf') => {
    setLoading(kind);
    try {
      let blob: Blob;
      let filename: string;
      if (kind === 'creationExcel') {
        blob = await reportApi.userCreationExcel(creationFrom, creationTo);
        filename = `user-creation-${creationFrom}-${creationTo}.xlsx`;
      } else if (kind === 'creationPdf') {
        blob = await reportApi.userCreationPdf(creationFrom, creationTo);
        filename = `user-creation-${creationFrom}-${creationTo}.pdf`;
      } else if (kind === 'inactiveExcel') {
        blob = await reportApi.userInactiveExcel(inactiveFrom, inactiveTo);
        filename = `user-inactive-${inactiveFrom}-${inactiveTo}.xlsx`;
      } else {
        blob = await reportApi.userInactivePdf(inactiveFrom, inactiveTo);
        filename = `user-inactive-${inactiveFrom}-${inactiveTo}.pdf`;
      }
      triggerDownload(blob, filename);
    } catch (err) {
      notify(await extractBlobError(err), 'error');
    } finally {
      setLoading('');
    }
  };

  return (
    <Stack spacing={2}>
      <Card variant="outlined">
        <CardContent>
          <Typography variant="subtitle1" fontWeight={600} mb={1}>
            User Creation Report
          </Typography>
          <Typography variant="body2" color="text.secondary" mb={2}>
            Lists all users created within the date range with full profile details including roles and department.
          </Typography>
          <Stack spacing={2}>
            <DateRangePicker from={creationFrom} to={creationTo} onFromChange={setCreationFrom} onToChange={setCreationTo} />
            <ButtonGroup variant="contained" size="small">
              <Button startIcon={<TableChartIcon />} onClick={() => dl('creationExcel')} disabled={!!loading}>
                {loading === 'creationExcel' ? 'Generating…' : 'Download Excel'}
              </Button>
              <Button startIcon={<PictureAsPdfIcon />} onClick={() => dl('creationPdf')} disabled={!!loading} color="error">
                {loading === 'creationPdf' ? 'Generating…' : 'Download PDF'}
              </Button>
            </ButtonGroup>
          </Stack>
        </CardContent>
      </Card>

      <Card variant="outlined">
        <CardContent>
          <Typography variant="subtitle1" fontWeight={600} mb={1}>
            User Inactive / Disabled Report
          </Typography>
          <Typography variant="body2" color="text.secondary" mb={2}>
            Lists all users who were deactivated within the date range, including the exact date/time of deactivation.
          </Typography>
          <Stack spacing={2}>
            <DateRangePicker from={inactiveFrom} to={inactiveTo} onFromChange={setInactiveFrom} onToChange={setInactiveTo} />
            <ButtonGroup variant="contained" size="small">
              <Button startIcon={<TableChartIcon />} onClick={() => dl('inactiveExcel')} disabled={!!loading}>
                {loading === 'inactiveExcel' ? 'Generating…' : 'Download Excel'}
              </Button>
              <Button startIcon={<PictureAsPdfIcon />} onClick={() => dl('inactivePdf')} disabled={!!loading} color="error">
                {loading === 'inactivePdf' ? 'Generating…' : 'Download PDF'}
              </Button>
            </ButtonGroup>
          </Stack>
        </CardContent>
      </Card>
    </Stack>
  );
}

// ─── Master Reports Tab ───────────────────────────────────────────────────────

function MasterReportsTab() {
  const { notify } = useNotification();
  const [from, setFrom] = useState(monthStart);
  const [to, setTo] = useState(today);
  const [loading, setLoading] = useState('');

  const dl = async (type: 'excel' | 'pdf') => {
    setLoading(type);
    try {
      const blob = type === 'excel'
        ? await reportApi.partyExcel(from, to)
        : await reportApi.partyPdf(from, to);
      triggerDownload(blob, `party-report-${from}-${to}.${type === 'excel' ? 'xlsx' : 'pdf'}`);
    } catch (err) {
      notify(await extractBlobError(err), 'error');
    } finally {
      setLoading('');
    }
  };

  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="subtitle1" fontWeight={600} mb={2}>
          Master (Party) Report
        </Typography>
        <Typography variant="body2" color="text.secondary" mb={2}>
          All parties created within the date range with full address details, approval status, and complete audit history.
          Excel includes a separate Audit Log sheet.
        </Typography>
        <Stack spacing={2}>
          <DateRangePicker from={from} to={to} onFromChange={setFrom} onToChange={setTo} />
          <ButtonGroup variant="contained" size="small">
            <Button startIcon={<TableChartIcon />} onClick={() => dl('excel')} disabled={!!loading}>
              {loading === 'excel' ? 'Generating…' : 'Download Excel'}
            </Button>
            <Button startIcon={<PictureAsPdfIcon />} onClick={() => dl('pdf')} disabled={!!loading} color="error">
              {loading === 'pdf' ? 'Generating…' : 'Download PDF'}
            </Button>
          </ButtonGroup>
        </Stack>
      </CardContent>
    </Card>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function ReportsPage() {
  const [tab, setTab] = useState(0);

  return (
    <Stack spacing={2}>
      <Typography variant="h5" fontWeight={600}>
        Reports
      </Typography>
      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)}>
          <Tab label="Shipment Reports" />
          <Tab label="User Reports" />
          <Tab label="Master Reports" />
        </Tabs>
      </Box>
      <Box>
        {tab === 0 && <BookingReportsTab />}
        {tab === 1 && <UserReportsTab />}
        {tab === 2 && <MasterReportsTab />}
      </Box>
    </Stack>
  );
}
