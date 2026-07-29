import { useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { Controller, useForm } from 'react-hook-form';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Divider,
  Grid,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { adminApi, bookingApi, dcApi } from '../../api/endpoints';
import { useCreateDcMutation, useUpdateDcMutation, useGetDcQuery, useGetDcByBookingQuery } from '../../store/api/dcApiSlice';
import { extractErrorMessage, extractBlobError } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';
import type { Booking, DcStatus, Unit } from '../../types';

interface FormValues {
  unitId: number | '';
  dcDate: string;
  vehicleNumber: string;
  driverName: string;
  status: DcStatus;
  remarks: string;
}

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

const DEFAULTS: FormValues = {
  unitId: '',
  dcDate: today(),
  vehicleNumber: '',
  driverName: '',
  status: 'DRAFT',
  remarks: '',
};

function ReadOnlyField({ label, value }: { label: string; value?: string | number | null }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary">{label}</Typography>
      <Typography variant="body2" fontWeight={500}>{value ?? '—'}</Typography>
    </Box>
  );
}

export default function DcFormPage() {
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const viewMode = searchParams.get('view') === '1';
  const bookingIdParam = searchParams.get('bookingId');
  const navigate = useNavigate();
  const { notify } = useNotification();
  const isEdit = Boolean(id);

  const [units, setUnits] = useState<Unit[]>([]);
  const [booking, setBooking] = useState<Booking | null>(null);

  const { data: existingDc, isLoading: loadingDc } = useGetDcQuery(Number(id), { skip: !isEdit });
  const { data: dcForBooking } = useGetDcByBookingQuery(Number(bookingIdParam), {
    skip: isEdit || !bookingIdParam,
  });
  const [createDc] = useCreateDcMutation();
  const [updateDc] = useUpdateDcMutation();

  // If this booking already has a DC, redirect straight to editing it instead of creating a duplicate
  useEffect(() => {
    if (!isEdit && dcForBooking) {
      navigate(`/dc/${dcForBooking.id}/edit`, { replace: true });
    }
  }, [isEdit, dcForBooking, navigate]);

  const {
    control,
    handleSubmit,
    reset,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ defaultValues: DEFAULTS });

  useEffect(() => {
    adminApi.listActiveUnits().then(setUnits).catch(() => undefined);
  }, []);

  // Create mode: resolve booking from the ?bookingId= query param
  useEffect(() => {
    if (!isEdit && bookingIdParam) {
      bookingApi.get(Number(bookingIdParam))
        .then((b) => {
          setBooking(b);
          if (b.unit) setValue('unitId', b.unit.id);
        })
        .catch((err) => notify(extractErrorMessage(err), 'error'));
    }
  }, [isEdit, bookingIdParam, setValue, notify]);

  // Edit mode: populate from the fetched DC
  useEffect(() => {
    if (isEdit && existingDc) {
      setBooking(existingDc.booking);
      reset({
        unitId: existingDc.unit.id,
        dcDate: existingDc.dcDate,
        vehicleNumber: existingDc.vehicleNumber ?? '',
        driverName: existingDc.driverName ?? '',
        status: existingDc.status,
        remarks: existingDc.remarks ?? '',
      });
    }
  }, [isEdit, existingDc, reset]);

  const onSubmit = async (values: FormValues) => {
    if (!booking) {
      notify('No booking selected for this delivery challan', 'error');
      return;
    }
    const body = {
      bookingId: booking.id,
      unitId: Number(values.unitId),
      dcDate: values.dcDate,
      vehicleNumber: values.vehicleNumber || null,
      driverName: values.driverName || null,
      status: values.status,
      remarks: values.remarks || null,
    };
    try {
      if (isEdit) {
        await updateDc({ id: Number(id), data: body }).unwrap();
        notify('Delivery challan updated', 'success');
      } else {
        await createDc(body).unwrap();
        notify('Delivery challan created', 'success');
      }
      navigate('/dc');
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const print = async () => {
    if (!id) return;
    try {
      const blob = await dcApi.fetchPrint(Number(id));
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank');
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch (err) {
      const msg = await extractBlobError(err);
      notify(msg, 'error');
    }
  };

  if (isEdit && loadingDc) {
    return <Typography>Loading…</Typography>;
  }

  if (!isEdit && !bookingIdParam) {
    return (
      <Stack spacing={2}>
        <Typography variant="h5" fontWeight={600}>New Delivery Challan</Typography>
        <Alert severity="warning">
          A delivery challan must be created from an existing booking. Go to Bookings and use
          the "Create DC" action on the booking you want to generate a challan for.
        </Alert>
        <Button onClick={() => navigate('/bookings')}>Go to Bookings</Button>
      </Stack>
    );
  }

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5" fontWeight={600}>
          {viewMode ? 'Delivery Challan Detail' : isEdit ? 'Edit Delivery Challan' : 'New Delivery Challan'}
        </Typography>
        <Stack direction="row" spacing={1}>
          {isEdit && (
            <Button variant="outlined" onClick={print}>Print</Button>
          )}
          <Button onClick={() => navigate('/dc')}>Back</Button>
        </Stack>
      </Stack>

      {booking && (
        <Card>
          <CardContent>
            {existingDc && (
              <Stack direction="row" spacing={2} alignItems="center" mb={2}>
                <Typography variant="h6">{existingDc.dcNumber}</Typography>
                <Chip size="small" label={existingDc.status} />
              </Stack>
            )}
            <Typography variant="subtitle2" color="primary" gutterBottom>Booking</Typography>
            <Grid container spacing={2} mb={2}>
              <Grid item xs={6} sm={3}><ReadOnlyField label="Booking No." value={booking.bookingNumber} /></Grid>
              <Grid item xs={6} sm={3}><ReadOnlyField label="Receiver" value={booking.receiver.partyName} /></Grid>
              <Grid item xs={6} sm={3}><ReadOnlyField label="Item" value={booking.itemDescription} /></Grid>
              <Grid item xs={6} sm={3}><ReadOnlyField label="Packages / Weight" value={`${booking.noOfPackages} / ${booking.weightKg} kg`} /></Grid>
            </Grid>

            <Divider sx={{ mb: 2 }} />

            {viewMode ? (
              <>
                <Typography variant="subtitle2" color="primary" gutterBottom>Delivery Challan Details</Typography>
                <Grid container spacing={2}>
                  <Grid item xs={6} sm={3}><ReadOnlyField label="Unit" value={existingDc?.unit.unitName} /></Grid>
                  <Grid item xs={6} sm={3}><ReadOnlyField label="DC Date" value={existingDc?.dcDate} /></Grid>
                  <Grid item xs={6} sm={3}><ReadOnlyField label="Vehicle No." value={existingDc?.vehicleNumber} /></Grid>
                  <Grid item xs={6} sm={3}><ReadOnlyField label="Driver Name" value={existingDc?.driverName} /></Grid>
                  <Grid item xs={12}><ReadOnlyField label="Remarks" value={existingDc?.remarks} /></Grid>
                </Grid>
              </>
            ) : (
              <form onSubmit={handleSubmit(onSubmit)}>
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={3}>
                    <Controller name="unitId" control={control} rules={{ required: 'Unit is required' }}
                      render={({ field }) => (
                        <TextField {...field} select label="Unit *" fullWidth
                          error={!!errors.unitId} helperText={errors.unitId?.message}>
                          {units.map((u) => (
                            <MenuItem key={u.id} value={u.id}>{u.unitName}</MenuItem>
                          ))}
                        </TextField>
                      )} />
                  </Grid>
                  <Grid item xs={12} sm={3}>
                    <Controller name="dcDate" control={control} rules={{ required: 'DC date is required' }}
                      render={({ field }) => (
                        <TextField {...field} label="DC Date *" type="date" fullWidth InputLabelProps={{ shrink: true }}
                          error={!!errors.dcDate} helperText={errors.dcDate?.message} />
                      )} />
                  </Grid>
                  <Grid item xs={12} sm={3}>
                    <Controller name="vehicleNumber" control={control}
                      render={({ field }) => (
                        <TextField {...field} label="Vehicle No." fullWidth />
                      )} />
                  </Grid>
                  <Grid item xs={12} sm={3}>
                    <Controller name="driverName" control={control}
                      render={({ field }) => (
                        <TextField {...field} label="Driver Name" fullWidth />
                      )} />
                  </Grid>
                  <Grid item xs={12} sm={3}>
                    <Controller name="status" control={control} rules={{ required: 'Status is required' }}
                      render={({ field }) => (
                        <TextField {...field} select label="Status *" fullWidth>
                          <MenuItem value="DRAFT">Draft</MenuItem>
                          <MenuItem value="ISSUED">Issued</MenuItem>
                          <MenuItem value="DELIVERED">Delivered</MenuItem>
                        </TextField>
                      )} />
                  </Grid>
                  <Grid item xs={12}>
                    <Controller name="remarks" control={control}
                      render={({ field }) => (
                        <TextField {...field} label="Remarks" fullWidth multiline minRows={2} />
                      )} />
                  </Grid>
                  <Grid item xs={12}>
                    <Stack direction="row" spacing={1} justifyContent="flex-end">
                      <Button onClick={() => navigate('/dc')}>Cancel</Button>
                      <Button type="submit" variant="contained" disabled={isSubmitting}>
                        {isEdit ? 'Update' : 'Create'}
                      </Button>
                    </Stack>
                  </Grid>
                </Grid>
              </form>
            )}
          </CardContent>
        </Card>
      )}
    </Stack>
  );
}
