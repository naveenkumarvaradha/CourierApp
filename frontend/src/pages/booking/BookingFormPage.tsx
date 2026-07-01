import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Controller, useForm } from 'react-hook-form';
import {
  Alert,
  Button,
  Card,
  CardContent,
  Grid,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import BusinessIcon from '@mui/icons-material/Business';
import { adminApi, bookingApi, flexFieldApi, partyApi } from '../../api/endpoints';
import { extractErrorMessage } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';
import FlexFieldsSection from '../../components/FlexFieldsSection';
import type { CompanySettings, CourierWay, FlexFieldValues, PackageType, Party } from '../../types';

interface FormValues {
  receiverId: number | '';
  courierWayId: number | '';
  packageTypeId: number | '';
  itemDescription: string;
  weightKg: number | '';
  noOfPackages: number | '';
  courierMode: string;
  specialInstructions: string;
}

const DEFAULTS: FormValues = {
  receiverId: '',
  courierWayId: '',
  packageTypeId: '',
  itemDescription: '',
  weightKg: '',
  noOfPackages: 1,
  courierMode: 'SURFACE',
  specialInstructions: '',
};

export default function BookingFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { notify } = useNotification();
  const [parties, setParties] = useState<Party[]>([]);
  const [courierWays, setCourierWays] = useState<CourierWay[]>([]);
  const [packageTypes, setPackageTypes] = useState<PackageType[]>([]);
  const [company, setCompany] = useState<CompanySettings | null>(null);
  const [flexValues, setFlexValues] = useState<FlexFieldValues>({});
  const isEdit = Boolean(id);

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ defaultValues: DEFAULTS });

  useEffect(() => {
    partyApi.listActive().then(setParties).catch(() => undefined);
    adminApi.listActiveCourierWays().then(setCourierWays).catch(() => undefined);
    adminApi.listActivePackageTypes().then(setPackageTypes).catch(() => undefined);
    adminApi.getCompanySettings().then(setCompany).catch(() => undefined);
  }, []);

  useEffect(() => {
    if (id) {
      bookingApi.get(Number(id)).then((b) => {
        reset({
          receiverId: b.receiver.id,
          courierWayId: b.courierWay?.id ?? '',
          packageTypeId: b.packageType?.id ?? '',
          itemDescription: b.itemDescription,
          weightKg: b.weightKg,
          noOfPackages: b.noOfPackages,
          courierMode: b.courierMode,
          specialInstructions: b.specialInstructions ?? '',
        });
        flexFieldApi.getValues('BOOKING', Number(id))
          .then((res) => setFlexValues(res.values ?? {}))
          .catch(() => undefined);
      }).catch((err) => notify(extractErrorMessage(err), 'error'));
    }
  }, [id, reset, notify]);

  const onSubmit = async (values: FormValues) => {
    const body: Record<string, unknown> = {
      receiverId: Number(values.receiverId),
      courierWayId: Number(values.courierWayId),
      packageTypeId: values.packageTypeId !== '' ? Number(values.packageTypeId) : null,
      itemDescription: values.itemDescription,
      weightKg: Number(values.weightKg),
      noOfPackages: Number(values.noOfPackages),
      courierMode: values.courierMode,
      specialInstructions: values.specialInstructions || null,
    };
    try {
      let savedId: number;
      if (isEdit) {
        await bookingApi.update(Number(id), body);
        savedId = Number(id);
        notify('Booking updated', 'success');
      } else {
        const created = await bookingApi.create(body);
        savedId = created.id;
        notify('Booking created', 'success');
      }
      if (Object.keys(flexValues).length > 0) {
        await flexFieldApi.saveValues('BOOKING', savedId, flexValues).catch(() => undefined);
      }
      navigate('/bookings');
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  return (
    <Stack spacing={2}>
      <Typography variant="h5" fontWeight={600}>{isEdit ? 'Edit Booking' : 'New Booking'}</Typography>

      <Alert icon={<BusinessIcon />} severity="info" sx={{ alignItems: 'center' }}>
        <strong>Sender (auto):</strong>{' '}
        {company
          ? `${company.companyName} — ${company.addressLine1}, ${company.city}, ${company.pincode}`
          : 'Company not configured. Go to Admin → Company Setup first.'}
      </Alert>

      <Card>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)}>
            <Grid container spacing={2}>

              {/* Row 1: Receiver, Courier Way, Package Type */}
              <Grid item xs={12} sm={4}>
                <Controller name="receiverId" control={control} rules={{ required: 'Receiver is required' }}
                  render={({ field }) => (
                    <TextField {...field} select label="Receiver *" fullWidth
                      error={!!errors.receiverId} helperText={errors.receiverId?.message}>
                      {parties.map((p) => (
                        <MenuItem key={p.id} value={p.id}>{p.partyName} ({p.partyCode})</MenuItem>
                      ))}
                    </TextField>
                  )} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <Controller name="courierWayId" control={control} rules={{ required: 'Courier way is required' }}
                  render={({ field }) => (
                    <TextField {...field} select label="Courier Way *" fullWidth
                      error={!!errors.courierWayId} helperText={errors.courierWayId?.message}>
                      {courierWays.map((cw) => (
                        <MenuItem key={cw.id} value={cw.id}>{cw.name}</MenuItem>
                      ))}
                    </TextField>
                  )} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <Controller name="packageTypeId" control={control}
                  render={({ field }) => (
                    <TextField {...field} select label="Package Type" fullWidth>
                      <MenuItem value=""><em>None</em></MenuItem>
                      {packageTypes.map((pt) => (
                        <MenuItem key={pt.id} value={pt.id}>{pt.name}</MenuItem>
                      ))}
                    </TextField>
                  )} />
              </Grid>

              {/* Item description */}
              <Grid item xs={12}>
                <Controller name="itemDescription" control={control} rules={{ required: 'Item description is required' }}
                  render={({ field }) => (
                    <TextField {...field} label="Item Description *" fullWidth
                      error={!!errors.itemDescription} helperText={errors.itemDescription?.message} />
                  )} />
              </Grid>

              {/* Weight, Packages, Mode */}
              <Grid item xs={12} sm={4}>
                <Controller name="weightKg" control={control} rules={{ required: 'Weight is required', min: { value: 0.001, message: 'Must be > 0' } }}
                  render={({ field }) => (
                    <TextField {...field} label="Weight (kg) *" type="number" fullWidth
                      error={!!errors.weightKg} helperText={errors.weightKg?.message} />
                  )} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <Controller name="noOfPackages" control={control} rules={{ required: 'Required', min: { value: 1, message: 'Min 1' } }}
                  render={({ field }) => (
                    <TextField {...field} label="No. of Packages *" type="number" fullWidth
                      error={!!errors.noOfPackages} helperText={errors.noOfPackages?.message} />
                  )} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <Controller name="courierMode" control={control} rules={{ required: 'Mode is required' }}
                  render={({ field }) => (
                    <TextField {...field} select label="Mode *" fullWidth>
                      <MenuItem value="AIR">Air</MenuItem>
                      <MenuItem value="SURFACE">Surface</MenuItem>
                      <MenuItem value="EXPRESS">Express</MenuItem>
                    </TextField>
                  )} />
              </Grid>

              {/* Special instructions */}
              <Grid item xs={12}>
                <Controller name="specialInstructions" control={control}
                  render={({ field }) => (
                    <TextField {...field} label="Special Instructions" fullWidth multiline minRows={2} />
                  )} />
              </Grid>

              {/* Dynamic flex fields */}
              <Grid item xs={12}>
                <FlexFieldsSection module="BOOKING" entityId={id ? Number(id) : undefined}
                  values={flexValues} onChange={setFlexValues} />
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
