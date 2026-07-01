import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Controller, useForm } from 'react-hook-form';
import {
  Button,
  Card,
  CardContent,
  Grid,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import dayjs from 'dayjs';
import { bookingApi, partyApi } from '../../api/endpoints';
import { extractErrorMessage } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';
import type { Party } from '../../types';

interface FormValues {
  bookingDate: string;
  senderId: number | '';
  receiverId: number | '';
  itemDescription: string;
  weightKg: number | '';
  noOfPackages: number | '';
  courierMode: string;
  declaredValue: number | '';
  freightCharges: number | '';
  totalCharges: number | '';
  paymentMode: string;
  specialInstructions: string;
}

const DEFAULTS: FormValues = {
  bookingDate: dayjs().format('YYYY-MM-DD'),
  senderId: '',
  receiverId: '',
  itemDescription: '',
  weightKg: '',
  noOfPackages: 1,
  courierMode: 'SURFACE',
  declaredValue: '',
  freightCharges: '',
  totalCharges: '',
  paymentMode: 'PREPAID',
  specialInstructions: '',
};

export default function BookingFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { notify } = useNotification();
  const [parties, setParties] = useState<Party[]>([]);
  const isEdit = Boolean(id);

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ defaultValues: DEFAULTS });

  useEffect(() => {
    partyApi.listActive().then(setParties).catch(() => undefined);
  }, []);

  useEffect(() => {
    if (id) {
      bookingApi
        .get(Number(id))
        .then((b) =>
          reset({
            bookingDate: b.bookingDate,
            senderId: b.sender.id,
            receiverId: b.receiver.id,
            itemDescription: b.itemDescription,
            weightKg: b.weightKg,
            noOfPackages: b.noOfPackages,
            courierMode: b.courierMode,
            declaredValue: b.declaredValue ?? '',
            freightCharges: b.freightCharges,
            totalCharges: b.totalCharges,
            paymentMode: b.paymentMode,
            specialInstructions: b.specialInstructions ?? '',
          })
        )
        .catch((err) => notify(extractErrorMessage(err), 'error'));
    }
  }, [id, reset, notify]);

  const onSubmit = async (values: FormValues) => {
    const body = {
      ...values,
      senderId: Number(values.senderId),
      receiverId: Number(values.receiverId),
      weightKg: Number(values.weightKg),
      noOfPackages: Number(values.noOfPackages),
      declaredValue: values.declaredValue === '' ? null : Number(values.declaredValue),
      freightCharges: Number(values.freightCharges),
      totalCharges: Number(values.totalCharges),
    };
    try {
      if (isEdit) {
        await bookingApi.update(Number(id), body);
        notify('Booking updated', 'success');
      } else {
        await bookingApi.create(body);
        notify('Booking created', 'success');
      }
      navigate('/bookings');
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  return (
    <Stack spacing={2}>
      <Typography variant="h5" fontWeight={600}>
        {isEdit ? 'Edit Booking' : 'New Booking'}
      </Typography>
      <Card>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)}>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={4}>
                <Controller
                  name="bookingDate"
                  control={control}
                  rules={{ required: 'Required' }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Booking Date"
                      type="date"
                      fullWidth
                      InputLabelProps={{ shrink: true }}
                      error={!!errors.bookingDate}
                      helperText={errors.bookingDate?.message}
                    />
                  )}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <Controller
                  name="senderId"
                  control={control}
                  rules={{ required: 'Required' }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      select
                      label="Sender"
                      fullWidth
                      error={!!errors.senderId}
                      helperText={errors.senderId?.message}
                    >
                      {parties.map((p) => (
                        <MenuItem key={p.id} value={p.id}>
                          {p.partyName} ({p.partyCode})
                        </MenuItem>
                      ))}
                    </TextField>
                  )}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <Controller
                  name="receiverId"
                  control={control}
                  rules={{ required: 'Required' }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      select
                      label="Receiver"
                      fullWidth
                      error={!!errors.receiverId}
                      helperText={errors.receiverId?.message}
                    >
                      {parties.map((p) => (
                        <MenuItem key={p.id} value={p.id}>
                          {p.partyName} ({p.partyCode})
                        </MenuItem>
                      ))}
                    </TextField>
                  )}
                />
              </Grid>
              <Grid item xs={12}>
                <Controller
                  name="itemDescription"
                  control={control}
                  rules={{ required: 'Required' }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Item Description"
                      fullWidth
                      error={!!errors.itemDescription}
                      helperText={errors.itemDescription?.message}
                    />
                  )}
                />
              </Grid>
              <Grid item xs={6} sm={3}>
                <Controller
                  name="weightKg"
                  control={control}
                  rules={{ required: 'Required' }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Weight (kg)"
                      type="number"
                      fullWidth
                      error={!!errors.weightKg}
                      helperText={errors.weightKg?.message}
                    />
                  )}
                />
              </Grid>
              <Grid item xs={6} sm={3}>
                <Controller
                  name="noOfPackages"
                  control={control}
                  rules={{ required: 'Required' }}
                  render={({ field }) => (
                    <TextField {...field} label="Packages" type="number" fullWidth />
                  )}
                />
              </Grid>
              <Grid item xs={6} sm={3}>
                <Controller
                  name="courierMode"
                  control={control}
                  render={({ field }) => (
                    <TextField {...field} select label="Mode" fullWidth>
                      <MenuItem value="AIR">Air</MenuItem>
                      <MenuItem value="SURFACE">Surface</MenuItem>
                      <MenuItem value="EXPRESS">Express</MenuItem>
                    </TextField>
                  )}
                />
              </Grid>
              <Grid item xs={6} sm={3}>
                <Controller
                  name="paymentMode"
                  control={control}
                  render={({ field }) => (
                    <TextField {...field} select label="Payment Mode" fullWidth>
                      <MenuItem value="PREPAID">Prepaid</MenuItem>
                      <MenuItem value="TOPAY">To Pay</MenuItem>
                    </TextField>
                  )}
                />
              </Grid>
              <Grid item xs={6} sm={4}>
                <Controller
                  name="declaredValue"
                  control={control}
                  render={({ field }) => (
                    <TextField {...field} label="Declared Value" type="number" fullWidth />
                  )}
                />
              </Grid>
              <Grid item xs={6} sm={4}>
                <Controller
                  name="freightCharges"
                  control={control}
                  rules={{ required: 'Required' }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Freight Charges"
                      type="number"
                      fullWidth
                      error={!!errors.freightCharges}
                      helperText={errors.freightCharges?.message}
                    />
                  )}
                />
              </Grid>
              <Grid item xs={6} sm={4}>
                <Controller
                  name="totalCharges"
                  control={control}
                  rules={{ required: 'Required' }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Total Charges"
                      type="number"
                      fullWidth
                      error={!!errors.totalCharges}
                      helperText={errors.totalCharges?.message}
                    />
                  )}
                />
              </Grid>
              <Grid item xs={12}>
                <Controller
                  name="specialInstructions"
                  control={control}
                  render={({ field }) => (
                    <TextField {...field} label="Special Instructions" fullWidth multiline minRows={2} />
                  )}
                />
              </Grid>
              <Grid item xs={12}>
                <Stack direction="row" spacing={1} justifyContent="flex-end">
                  <Button onClick={() => navigate('/bookings')}>Cancel</Button>
                  <Button type="submit" variant="contained" disabled={isSubmitting}>
                    {isEdit ? 'Update' : 'Create'}
                  </Button>
                </Stack>
              </Grid>
            </Grid>
          </form>
        </CardContent>
      </Card>
    </Stack>
  );
}
