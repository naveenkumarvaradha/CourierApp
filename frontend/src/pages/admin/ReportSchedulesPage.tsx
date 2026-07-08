import { useCallback, useEffect, useState } from 'react';
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Switch,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import ScheduleIcon from '@mui/icons-material/Schedule';
import { adminApi } from '../../api/endpoints';
import { extractErrorMessage } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';
import type { ReportSchedule } from '../../types';

const REPORT_TYPES = [
  { value: 'BOOKING_DETAIL', label: 'Booking Detail' },
  { value: 'USER_CREATION',  label: 'User Creation' },
  { value: 'USER_INACTIVE',  label: 'User Inactive' },
  { value: 'PARTY',          label: 'Party / Address Book' },
];

const FREQUENCIES = [
  { value: 'DAILY',   label: 'Daily' },
  { value: 'WEEKLY',  label: 'Weekly' },
  { value: 'MONTHLY', label: 'Monthly' },
  { value: 'YEARLY',  label: 'Yearly' },
];

const DAYS_OF_WEEK = [
  { value: 1, label: 'Monday' },
  { value: 2, label: 'Tuesday' },
  { value: 3, label: 'Wednesday' },
  { value: 4, label: 'Thursday' },
  { value: 5, label: 'Friday' },
  { value: 6, label: 'Saturday' },
  { value: 7, label: 'Sunday' },
];

const MONTHS = [
  { value: 1, label: 'January' }, { value: 2, label: 'February' },
  { value: 3, label: 'March' },   { value: 4, label: 'April' },
  { value: 5, label: 'May' },     { value: 6, label: 'June' },
  { value: 7, label: 'July' },    { value: 8, label: 'August' },
  { value: 9, label: 'September' },{ value: 10, label: 'October' },
  { value: 11, label: 'November' },{ value: 12, label: 'December' },
];

interface FormState {
  id?: number;
  scheduleName: string;
  reportType: string;
  frequency: string;
  dayOfWeek: string;
  dayOfMonth: string;
  monthOfYear: string;
  recipientEmails: string;
  fileFormat: string;
  enabled: boolean;
}

const EMPTY: FormState = {
  scheduleName: '',
  reportType: 'BOOKING_DETAIL',
  frequency: 'DAILY',
  dayOfWeek: '',
  dayOfMonth: '',
  monthOfYear: '',
  recipientEmails: '',
  fileFormat: 'EXCEL',
  enabled: true,
};

export default function ReportSchedulesPage() {
  const { notify } = useNotification();
  const [rows, setRows] = useState<ReportSchedule[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<FormState>(EMPTY);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setRows(await adminApi.listReportSchedules());
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    } finally {
      setLoading(false);
    }
  }, [notify]);

  useEffect(() => { load(); }, [load]);

  const openCreate = () => { setForm(EMPTY); setOpen(true); };
  const openEdit = (r: ReportSchedule) => {
    setForm({
      id: r.id,
      scheduleName: r.scheduleName,
      reportType: r.reportType,
      frequency: r.frequency,
      dayOfWeek: r.dayOfWeek != null ? String(r.dayOfWeek) : '',
      dayOfMonth: r.dayOfMonth != null ? String(r.dayOfMonth) : '',
      monthOfYear: r.monthOfYear != null ? String(r.monthOfYear) : '',
      recipientEmails: r.recipientEmails,
      fileFormat: r.fileFormat,
      enabled: r.enabled,
    });
    setOpen(true);
  };

  const save = async () => {
    const body = {
      scheduleName: form.scheduleName,
      reportType: form.reportType,
      frequency: form.frequency,
      dayOfWeek: form.dayOfWeek ? parseInt(form.dayOfWeek) : null,
      dayOfMonth: form.dayOfMonth ? parseInt(form.dayOfMonth) : null,
      monthOfYear: form.monthOfYear ? parseInt(form.monthOfYear) : null,
      recipientEmails: form.recipientEmails,
      fileFormat: form.fileFormat,
      enabled: form.enabled,
    };
    try {
      if (form.id) {
        await adminApi.updateReportSchedule(form.id, body);
        notify('Schedule updated', 'success');
      } else {
        await adminApi.createReportSchedule(body);
        notify('Schedule created', 'success');
      }
      setOpen(false);
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const remove = async (id: number) => {
    if (!window.confirm('Delete this report schedule?')) return;
    try {
      await adminApi.deleteReportSchedule(id);
      notify('Schedule deleted', 'success');
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const columns: GridColDef<ReportSchedule>[] = [
    { field: 'scheduleName', headerName: 'Name', flex: 1.2 },
    {
      field: 'reportType', headerName: 'Report', width: 150,
      valueGetter: (_v, row) => REPORT_TYPES.find((t) => t.value === row.reportType)?.label ?? row.reportType,
    },
    {
      field: 'frequency', headerName: 'Frequency', width: 100,
      renderCell: (p) => <Chip size="small" label={p.value} />,
    },
    { field: 'fileFormat', headerName: 'Format', width: 80 },
    {
      field: 'recipientEmails', headerName: 'Recipients', flex: 1,
      valueGetter: (_v, row) => row.recipientEmails,
    },
    {
      field: 'nextRunAt', headerName: 'Next Run', width: 160,
      valueGetter: (_v, row) => row.nextRunAt ? new Date(row.nextRunAt).toLocaleString() : '—',
    },
    {
      field: 'lastRunAt', headerName: 'Last Run', width: 160,
      valueGetter: (_v, row) => row.lastRunAt ? new Date(row.lastRunAt).toLocaleString() : 'Never',
    },
    {
      field: 'enabled', headerName: 'Status', width: 90,
      renderCell: (p) => (
        <Chip size="small" label={p.value ? 'Active' : 'Paused'}
          color={p.value ? 'success' : 'default'} />
      ),
    },
    {
      field: 'actions', headerName: 'Actions', width: 100, sortable: false,
      renderCell: (p) => (
        <Stack direction="row">
          <Tooltip title="Edit">
            <IconButton size="small" onClick={() => openEdit(p.row)}><EditIcon fontSize="small" /></IconButton>
          </Tooltip>
          <Tooltip title="Delete">
            <IconButton size="small" color="error" onClick={() => remove(p.row.id)}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Stack>
      ),
    },
  ];

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Stack direction="row" spacing={1} alignItems="center">
          <ScheduleIcon color="primary" />
          <Typography variant="h5" fontWeight={600}>Report Schedules</Typography>
        </Stack>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
          New Schedule
        </Button>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Automated reports are generated and emailed to recipients at the configured frequency.
        Reports run at midnight daily and are sent if due.
      </Typography>

      <Box sx={{ height: 520, bgcolor: 'background.paper' }}>
        <DataGrid rows={rows} columns={columns} loading={loading}
          disableRowSelectionOnClick
          pageSizeOptions={[10, 25]}
          initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
        />
      </Box>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{form.id ? 'Edit Schedule' : 'New Report Schedule'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} mt={0}>
            <Grid item xs={12}>
              <TextField label="Schedule Name" fullWidth required
                value={form.scheduleName}
                onChange={(e) => setForm({ ...form, scheduleName: e.target.value })} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Report Type</InputLabel>
                <Select label="Report Type" value={form.reportType}
                  onChange={(e) => setForm({ ...form, reportType: e.target.value })}>
                  {REPORT_TYPES.map((t) => <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>)}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>File Format</InputLabel>
                <Select label="File Format" value={form.fileFormat}
                  onChange={(e) => setForm({ ...form, fileFormat: e.target.value })}>
                  <MenuItem value="EXCEL">Excel (.xlsx)</MenuItem>
                  <MenuItem value="PDF">PDF</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Frequency</InputLabel>
                <Select label="Frequency" value={form.frequency}
                  onChange={(e) => setForm({ ...form, frequency: e.target.value, dayOfWeek: '', dayOfMonth: '', monthOfYear: '' })}>
                  {FREQUENCIES.map((f) => <MenuItem key={f.value} value={f.value}>{f.label}</MenuItem>)}
                </Select>
              </FormControl>
            </Grid>

            {form.frequency === 'WEEKLY' && (
              <Grid item xs={12} sm={6}>
                <FormControl fullWidth>
                  <InputLabel>Day of Week</InputLabel>
                  <Select label="Day of Week" value={form.dayOfWeek}
                    onChange={(e) => setForm({ ...form, dayOfWeek: String(e.target.value) })}>
                    {DAYS_OF_WEEK.map((d) => <MenuItem key={d.value} value={d.value}>{d.label}</MenuItem>)}
                  </Select>
                </FormControl>
              </Grid>
            )}

            {(form.frequency === 'MONTHLY' || form.frequency === 'YEARLY') && (
              <Grid item xs={12} sm={6}>
                <TextField label="Day of Month (1-28)" fullWidth type="number"
                  inputProps={{ min: 1, max: 28 }}
                  value={form.dayOfMonth}
                  onChange={(e) => setForm({ ...form, dayOfMonth: e.target.value })} />
              </Grid>
            )}

            {form.frequency === 'YEARLY' && (
              <Grid item xs={12} sm={6}>
                <FormControl fullWidth>
                  <InputLabel>Month</InputLabel>
                  <Select label="Month" value={form.monthOfYear}
                    onChange={(e) => setForm({ ...form, monthOfYear: String(e.target.value) })}>
                    {MONTHS.map((m) => <MenuItem key={m.value} value={m.value}>{m.label}</MenuItem>)}
                  </Select>
                </FormControl>
              </Grid>
            )}

            <Grid item xs={12}>
              <TextField
                label="Recipient Emails (comma-separated)"
                fullWidth
                required
                placeholder="admin@company.com, reports@company.com"
                value={form.recipientEmails}
                onChange={(e) => setForm({ ...form, recipientEmails: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={<Switch checked={form.enabled}
                  onChange={(e) => setForm({ ...form, enabled: e.target.checked })} />}
                label="Schedule Active"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={save}>
            {form.id ? 'Save' : 'Create Schedule'}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
