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
import BusinessIcon from '@mui/icons-material/Business';
import { adminApi, approvalApi, bookingApi, flexFieldApi, partyApi } from '../../api/endpoints';
import { useCreateBookingMutation, useUpdateBookingMutation } from '../../store/api/bookingApiSlice';
import { extractErrorMessage } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';
import FlexFieldsSection from '../../components/FlexFieldsSection';
import type { ApprovalInfo, Booking, CompanySettings, CourierWay, FlexFieldValues, PackageType, Party, Unit } from '../../types';

interface FormValues {
  receiverId: number | '';
  courierWayId: number | '';
  packageTypeId: number | '';
  unitId: number | '';
  itemDescription: string;
  weightKg: number | '';
  noOfPackages: number | '';
  courierMode: string;
  specialInstructions: string;
  companyPoNo: string;
}

const DEFAULTS: FormValues = {
  receiverId: '',
  courierWayId: '',
  packageTypeId: '',
  unitId: '',
  itemDescription: '',
  weightKg: '',
  noOfPackages: 1,
  courierMode: 'SURFACE',
  specialInstructions: '',
  companyPoNo: '',
};

const STATUS_COLORS: Record<string, 'default' | 'info' | 'warning' | 'success' | 'error'> = {
  BOOKED: 'info',
  PENDING_APPROVAL: 'warning',
  APPROVED: 'success',
  IN_TRANSIT: 'info',
  DELIVERED: 'success',
  CANCELLED: 'default',
  REJECTED: 'error',
  PENDING_CANCELLATION: 'warning',
};

function ReadOnlyField({ label, value }: { label: string; value?: string | number | null }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary">{label}</Typography>
      <Typography variant="body2" fontWeight={500}>{value ?? '—'}</Typography>
    </Box>
  );
}

export default function BookingFormPage() {
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const viewMode = searchParams.get('view') === '1';
  const navigate = useNavigate();
  const { notify } = useNotification();
  const [parties, setParties] = useState<Party[]>([]);
  const [courierWays, setCourierWays] = useState<CourierWay[]>([]);
  const [packageTypes, setPackageTypes] = useState<PackageType[]>([]);
  const [units, setUnits] = useState<Unit[]>([]);
  const [company, setCompany] = useState<CompanySettings | null>(null);
  const [flexValues, setFlexValues] = useState<FlexFieldValues>({});
  const [booking, setBooking] = useState<Booking | null>(null);
  const [approvalInfo, setApprovalInfo] = useState<ApprovalInfo | null>(null);
  const isEdit = Boolean(id) && !viewMode;

  const {
    control,
    handleSubmit,
    reset,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ defaultValues: DEFAULTS });

  const [createBooking] = useCreateBookingMutation();
  const [updateBooking] = useUpdateBookingMutation();

  useEffect(() => {
    // Exclude company-linked parties (partyCode starts with "COMPANY") from receiver dropdown
    partyApi.listActive()
      .then((list) => setParties(list.filter((p) => !p.partyCode.startsWith('COMPANY'))))
      .catch(() => undefined);
    adminApi.listActiveCourierWays().then(setCourierWays).catch(() => undefined);
    adminApi.listActivePackageTypes().then(setPackageTypes).catch(() => undefined);
    adminApi.listActiveUnits().then((list) => {
      setUnits(list);
      // Auto-select when creating: the only unit, or the company's default unit
      if (!isEdit) {
        if (list.length === 1) {
          setValue('unitId', list[0].id);
        } else {
          const def = list.find((u) => u.defaultUnit);
          if (def) setValue('unitId', def.id);
        }
      }
    }).catch(() => undefined);
    // Load sender address from the logged-in user's own company
    bookingApi.myCompanySettings().then(setCompany).catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (id) {
      bookingApi.get(Number(id)).then((b) => {
        setBooking(b);
        reset({
          receiverId: b.receiver.id,
          courierWayId: b.courierWay?.id ?? '',
          packageTypeId: b.packageType?.id ?? '',
          unitId: b.unit?.id ?? '',
          itemDescription: b.itemDescription,
          weightKg: b.weightKg,
          noOfPackages: b.noOfPackages,
          courierMode: b.courierMode,
          specialInstructions: b.specialInstructions ?? '',
          companyPoNo: b.companyPoNo ?? '',
        });
        flexFieldApi.getValues('BOOKING', Number(id))
          .then((res) => setFlexValues(res.values ?? {}))
          .catch(() => undefined);
        if (b.status === 'PENDING_APPROVAL') {
          approvalApi.bookingInfo(Number(id)).then(setApprovalInfo).catch(() => undefined);
        }
      }).catch((err) => notify(extractErrorMessage(err), 'error'));
    }
  }, [id, reset, notify]);

  const onSubmit = async (values: FormValues) => {
    const body: Record<string, unknown> = {
      receiverId: Number(values.receiverId),
      courierWayId: Number(values.courierWayId),
      packageTypeId: values.packageTypeId !== '' ? Number(values.packageTypeId) : null,
      unitId: values.unitId !== '' ? Number(values.unitId) : null,
      itemDescription: values.itemDescription,
      weightKg: Number(values.weightKg),
      noOfPackages: Number(values.noOfPackages),
      courierMode: values.courierMode,
      specialInstructions: values.specialInstructions || null,
      companyPoNo: values.companyPoNo || null,
    };
    try {
      let savedId: number;
      if (isEdit) {
        await updateBooking({ id: Number(id), data: body }).unwrap();
        savedId = Number(id);
        notify('Booking updated', 'success');
      } else {
        const created = await createBooking(body).unwrap();
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

  // ── View mode ─────────────────────────────────────────────────────────────
  if (viewMode && booking) {
    const r = booking.receiver;
    return (
      <Stack spacing={2}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Typography variant="h5" fontWeight={600}>Booking Detail</Typography>
          <Button onClick={() => navigate('/bookings')}>Back</Button>
        </Stack>

        <Card>
          <CardContent>
            <Stack direction="row" spacing={2} alignItems="center" mb={2}>
              <Typography variant="h6">{booking.bookingNumber}</Typography>
              <Chip
                size="small"
                label={booking.status.replace(/_/g, ' ')}
                color={STATUS_COLORS[booking.status] ?? 'default'}
              />
              {booking.awbNumber && (
                <Chip size="small" label={`AWB: ${booking.awbNumber}`} color="primary" variant="outlined" />
              )}
              {booking.printTaken && (
                <Chip size="small" label="Printed" color="success" variant="outlined" />
              )}
            </Stack>

            <Divider sx={{ mb: 2 }} />

            {/* FROM */}
            <Typography variant="subtitle2" color="primary" gutterBottom>FROM (Sender / Company)</Typography>
            <Grid container spacing={2} mb={2}>
              <Grid item xs={6} sm={4}><ReadOnlyField label="Created By" value={booking.createdBy} /></Grid>
              <Grid item xs={6} sm={4}><ReadOnlyField label="Company" value={company?.companyName} /></Grid>
              <Grid item xs={6} sm={4}>
                <ReadOnlyField
                  label="Address"
                  value={booking.unit
                    ? `${booking.unit.unitName} — ${booking.unit.addressLine1}, ${booking.unit.city}`
                    : (company ? `${company.addressLine1}, ${company.city}` : undefined)}
                />
              </Grid>
            </Grid>

            <Divider sx={{ mb: 2 }} />

            {/* TO */}
            <Typography variant="subtitle2" color="primary" gutterBottom>TO (Receiver)</Typography>
            <Grid container spacing={2} mb={2}>
              <Grid item xs={6} sm={4}><ReadOnlyField label="Party Name" value={r.partyName} /></Grid>
              <Grid item xs={6} sm={4}><ReadOnlyField label="Address Line 1" value={r.addressLine1} /></Grid>

              <Grid item xs={6} sm={3}><ReadOnlyField label="City" value={r.city} /></Grid>
              <Grid item xs={6} sm={3}><ReadOnlyField label="State" value={r.state} /></Grid>
              <Grid item xs={6} sm={3}><ReadOnlyField label="Pincode" value={r.pincode} /></Grid>
              <Grid item xs={6} sm={3}><ReadOnlyField label="Country" value={r.country} /></Grid>
              <Grid item xs={6} sm={3}><ReadOnlyField label="Phone" value={r.phone} /></Grid>
              <Grid item xs={6} sm={3}><ReadOnlyField label="Email" value={r.email} /></Grid>
              <Grid item xs={6} sm={3}><ReadOnlyField label="GSTIN" value={r.gstin} /></Grid>
            </Grid>

            <Divider sx={{ mb: 2 }} />

            {/* Shipment */}
            <Typography variant="subtitle2" color="primary" gutterBottom>Shipment Details</Typography>
            <Grid container spacing={2} mb={2}>
              <Grid item xs={6} sm={3}><ReadOnlyField label="Date" value={booking.bookingDate} /></Grid>
              <Grid item xs={6} sm={3}><ReadOnlyField label="Mode" value={booking.courierMode} /></Grid>
              <Grid item xs={6} sm={3}><ReadOnlyField label="Courier Way" value={booking.courierWay?.name} /></Grid>
              <Grid item xs={6} sm={3}><ReadOnlyField label="Package Type" value={booking.packageType?.name} /></Grid>
              <Grid item xs={6} sm={3}><ReadOnlyField label="Weight (kg)" value={booking.weightKg} /></Grid>
              <Grid item xs={6} sm={3}><ReadOnlyField label="No. of Packages" value={booking.noOfPackages} /></Grid>
              <Grid item xs={12} sm={6}><ReadOnlyField label="Item Description" value={booking.itemDescription} /></Grid>
            </Grid>

            {booking.status === 'PENDING_APPROVAL' && approvalInfo && (
              <Alert severity="warning" sx={{ mb: 2 }}>
                <strong>Pending Approval — {approvalInfo.summary}</strong>
                {approvalInfo.approvers.length > 0 && (
                  <Box mt={0.5}>
                    Awaiting approval from: <strong>{approvalInfo.approvers.join(', ')}</strong>
                  </Box>
                )}
              </Alert>
            )}
            {booking.approvalRemarks && (
              <Alert severity="info" sx={{ mb: 2 }}>
                <strong>Approval Remarks:</strong> {booking.approvalRemarks} — {booking.approverUsername}
              </Alert>
            )}
            {booking.cancellationRemarks && (
              <Alert severity="warning" sx={{ mb: 2 }}>
                <strong>Cancellation Remarks:</strong> {booking.cancellationRemarks}
              </Alert>
            )}
          </CardContent>
        </Card>
      </Stack>
    );
  }

  // ── Create / Edit mode ────────────────────────────────────────────────────
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

              {/* Row 1: Receiver, Courier Way, Package Type, Unit */}
              <Grid item xs={12} sm={3}>
                <Controller name="receiverId" control={control} rules={{ required: 'Receiver is required' }}
                  render={({ field }) => (
                    <TextField {...field} select label="Receiver *" fullWidth
                      error={!!errors.receiverId} helperText={errors.receiverId?.message}>
                      {parties.map((p) => (
                        <MenuItem key={p.id} value={p.id}>
                          {p.partyName}{p.companyName ? ` — ${p.companyName}` : ''} ({p.partyCode})
                        </MenuItem>
                      ))}
                    </TextField>
                  )} />
              </Grid>
              <Grid item xs={12} sm={3}>
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
              <Grid item xs={12} sm={3}>
                <Controller name="packageTypeId" control={control}
                  rules={{ required: 'Package type is required' }}
                  render={({ field }) => (
                    <TextField {...field} select label="Package Type *" fullWidth
                      error={!!errors.packageTypeId} helperText={errors.packageTypeId?.message}>
                      <MenuItem value=""><em>Select</em></MenuItem>
                      {packageTypes.map((pt) => (
                        <MenuItem key={pt.id} value={pt.id}>{pt.name}</MenuItem>
                      ))}
                    </TextField>
                  )} />
              </Grid>
              <Grid item xs={12} sm={3}>
                <Controller name="unitId" control={control}
                  render={({ field }) => (
                    <TextField {...field} select label="Sending Unit" fullWidth
                      helperText="Controls the FROM address on the sticker/DC">
                      <MenuItem value=""><em>None (use company address)</em></MenuItem>
                      {units.map((u) => (
                        <MenuItem key={u.id} value={u.id}>
                          {u.unitName}{u.defaultUnit ? ' (Default)' : ''}
                        </MenuItem>
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
