import { useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { Controller, useForm, useWatch } from 'react-hook-form';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Grid,
  MenuItem,
  Stack,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import { adminApi, approvalApi, dcApi, partyApi } from '../../api/endpoints';
import {
  useCreateDcMutation, useUpdateDcMutation, useGetDcQuery,
  useSubmitDcMutation, useApproveDcMutation, useRejectDcMutation,
} from '../../store/api/dcApiSlice';
import { extractErrorMessage, extractBlobError } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';
import type { ApprovalInfo, CourierWay, DcType, PackageType, Party, ReceiverType, Unit } from '../../types';

interface FormValues {
  unitId: number | '';
  dcType: DcType;
  receiverType: ReceiverType;
  receiverPartyId: number | '';
  receiverUnitId: number | '';
  courierWayId: number | '';
  packageTypeId: number | '';
  itemDescription: string;
  weightKg: number | '';
  noOfPackages: number | '';
  courierMode: string;
  vehicleNumber: string;
  driverName: string;
  remarks: string;
}

const DEFAULTS: FormValues = {
  unitId: '',
  dcType: 'NON_RETURNABLE',
  receiverType: 'PARTY',
  receiverPartyId: '',
  receiverUnitId: '',
  courierWayId: '',
  packageTypeId: '',
  itemDescription: '',
  weightKg: '',
  noOfPackages: 1,
  courierMode: 'SURFACE',
  vehicleNumber: '',
  driverName: '',
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
  const navigate = useNavigate();
  const { notify } = useNotification();
  const isEdit = Boolean(id);

  const [units, setUnits] = useState<Unit[]>([]);
  const [parties, setParties] = useState<Party[]>([]);
  const [courierWays, setCourierWays] = useState<CourierWay[]>([]);
  const [packageTypes, setPackageTypes] = useState<PackageType[]>([]);
  const [approvalInfo, setApprovalInfo] = useState<ApprovalInfo | null>(null);

  const { data: existingDc, isLoading: loadingDc } = useGetDcQuery(Number(id), { skip: !isEdit });
  const [createDc] = useCreateDcMutation();
  const [updateDc] = useUpdateDcMutation();
  const [submitDc] = useSubmitDcMutation();
  const [approveDc] = useApproveDcMutation();
  const [rejectDc] = useRejectDcMutation();
  const [decision, setDecision] = useState<'approve' | 'reject' | null>(null);
  const [decisionRemarks, setDecisionRemarks] = useState('');

  const {
    control,
    handleSubmit,
    reset,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ defaultValues: DEFAULTS });

  const receiverType = useWatch({ control, name: 'receiverType' });
  const senderUnitId = useWatch({ control, name: 'unitId' });

  useEffect(() => {
    adminApi.listActiveUnits().then((list) => {
      setUnits(list);
      if (!isEdit) {
        if (list.length === 1) {
          setValue('unitId', list[0].id);
        } else {
          const def = list.find((u) => u.defaultUnit);
          if (def) setValue('unitId', def.id);
        }
      }
    }).catch(() => undefined);
    partyApi.listActive()
      .then((list) => setParties(list.filter((p) => !p.partyCode.startsWith('COMPANY'))))
      .catch(() => undefined);
    adminApi.listActiveCourierWays().then(setCourierWays).catch(() => undefined);
    adminApi.listActivePackageTypes().then(setPackageTypes).catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (isEdit && existingDc) {
      reset({
        unitId: existingDc.unit.id,
        dcType: existingDc.dcType,
        receiverType: existingDc.receiverType,
        receiverPartyId: existingDc.receiverParty?.id ?? '',
        receiverUnitId: existingDc.receiverUnit?.id ?? '',
        courierWayId: existingDc.courierWay?.id ?? '',
        packageTypeId: existingDc.packageType?.id ?? '',
        itemDescription: existingDc.itemDescription,
        weightKg: existingDc.weightKg,
        noOfPackages: existingDc.noOfPackages,
        courierMode: existingDc.courierMode,
        vehicleNumber: existingDc.vehicleNumber ?? '',
        driverName: existingDc.driverName ?? '',
        remarks: existingDc.remarks ?? '',
      });
      if (existingDc.status === 'PENDING_APPROVAL') {
        approvalApi.dcInfo(existingDc.id).then(setApprovalInfo).catch(() => undefined);
      }
    }
  }, [isEdit, existingDc, reset]);

  const onSubmit = async (values: FormValues) => {
    const body = {
      unitId: Number(values.unitId),
      dcType: values.dcType,
      receiverType: values.receiverType,
      receiverPartyId: values.receiverType === 'PARTY' ? Number(values.receiverPartyId) : null,
      receiverUnitId: values.receiverType === 'UNIT' ? Number(values.receiverUnitId) : null,
      courierWayId: Number(values.courierWayId),
      packageTypeId: values.packageTypeId ? Number(values.packageTypeId) : null,
      itemDescription: values.itemDescription,
      weightKg: Number(values.weightKg),
      noOfPackages: Number(values.noOfPackages),
      courierMode: values.courierMode,
      vehicleNumber: values.vehicleNumber || null,
      driverName: values.driverName || null,
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

  const submit = async () => {
    if (!id) return;
    try {
      await submitDc(Number(id)).unwrap();
      notify('Submitted for approval', 'success');
    } catch (err) { notify(extractErrorMessage(err), 'error'); }
  };

  const confirmDecision = async () => {
    if (!id || !decision) return;
    try {
      if (decision === 'approve') {
        await approveDc({ id: Number(id), remarks: decisionRemarks }).unwrap();
        notify('Delivery challan approved', 'success');
      } else {
        await rejectDc({ id: Number(id), remarks: decisionRemarks }).unwrap();
        notify('Delivery challan rejected', 'success');
      }
      setDecision(null); setDecisionRemarks('');
    } catch (err) { notify(extractErrorMessage(err), 'error'); }
  };

  if (isEdit && loadingDc) {
    return <Typography>Loading…</Typography>;
  }

  const canEdit = !viewMode && (!isEdit || existingDc?.status === 'DRAFT' || existingDc?.status === 'PENDING_APPROVAL');
  const canSubmit = isEdit && existingDc?.status === 'DRAFT';
  const canDecide = isEdit && existingDc?.status === 'PENDING_APPROVAL';
  const receiverUnitOptions = units.filter((u) => u.id !== senderUnitId);

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5" fontWeight={600}>
          {viewMode ? 'DC Booking Detail' : isEdit ? 'Edit DC Booking' : 'New DC Booking'}
        </Typography>
        <Stack direction="row" spacing={1}>
          {canSubmit && (
            <Button variant="outlined" color="primary" onClick={submit}>Submit for Approval</Button>
          )}
          {canDecide && (
            <>
              <Button variant="contained" color="success" onClick={() => setDecision('approve')}>Approve</Button>
              <Button variant="contained" color="error" onClick={() => setDecision('reject')}>Reject</Button>
            </>
          )}
          {isEdit && (
            <Button variant="outlined" onClick={print}>Print</Button>
          )}
          <Button onClick={() => navigate('/dc')}>Back</Button>
        </Stack>
      </Stack>

      <Card>
        <CardContent>
          {existingDc && (
            <Stack direction="row" spacing={2} alignItems="center" mb={2}>
              <Typography variant="h6">{existingDc.dcNumber}</Typography>
              <Chip size="small" label={existingDc.status.replace(/_/g, ' ')} />
            </Stack>
          )}

          {existingDc?.status === 'PENDING_APPROVAL' && approvalInfo && (
            <Alert severity="warning" sx={{ mb: 2 }}>
              <strong>Pending Approval — {approvalInfo.summary}</strong>
              {approvalInfo.approvers.length > 0 && (
                <Box mt={0.5}>
                  Awaiting approval from: <strong>{approvalInfo.approvers.join(', ')}</strong>
                </Box>
              )}
            </Alert>
          )}
          {existingDc?.approvalRemarks && (
            <Alert severity="info" sx={{ mb: 2 }}>
              <strong>Approval Remarks:</strong> {existingDc.approvalRemarks} — {existingDc.approverUsername}
            </Alert>
          )}

          {viewMode || !canEdit ? (
            <>
              <Typography variant="subtitle2" color="primary" gutterBottom>From (Unit)</Typography>
              <Grid container spacing={2} mb={2}>
                <Grid item xs={12} sm={4}><ReadOnlyField label="Unit" value={existingDc?.unit.unitName} /></Grid>
                <Grid item xs={12} sm={4}><ReadOnlyField label="DC Date" value={existingDc?.dcDate} /></Grid>
                <Grid item xs={12} sm={4}><ReadOnlyField label="DC Type" value={existingDc?.dcType === 'RETURNABLE' ? 'Returnable' : 'Non-Returnable'} /></Grid>
              </Grid>

              <Divider sx={{ mb: 2 }} />

              <Typography variant="subtitle2" color="primary" gutterBottom>To (Receiver)</Typography>
              <Grid container spacing={2} mb={2}>
                <Grid item xs={12} sm={4}>
                  <ReadOnlyField label={existingDc?.receiverType === 'PARTY' ? 'Party' : 'Company Unit'}
                    value={existingDc?.receiverType === 'PARTY' ? existingDc?.receiverParty?.partyName : existingDc?.receiverUnit?.unitName} />
                </Grid>
              </Grid>

              <Divider sx={{ mb: 2 }} />

              <Typography variant="subtitle2" color="primary" gutterBottom>Shipment Details</Typography>
              <Grid container spacing={2}>
                <Grid item xs={6} sm={3}><ReadOnlyField label="Mode" value={existingDc?.courierMode} /></Grid>
                <Grid item xs={6} sm={3}><ReadOnlyField label="Courier Way" value={existingDc?.courierWay?.name} /></Grid>
                <Grid item xs={6} sm={3}><ReadOnlyField label="Package Type" value={existingDc?.packageType?.name} /></Grid>
                <Grid item xs={6} sm={3}><ReadOnlyField label="Weight (kg)" value={existingDc?.weightKg} /></Grid>
                <Grid item xs={6} sm={3}><ReadOnlyField label="No. of Packages" value={existingDc?.noOfPackages} /></Grid>
                <Grid item xs={6} sm={3}><ReadOnlyField label="Vehicle No." value={existingDc?.vehicleNumber} /></Grid>
                <Grid item xs={6} sm={3}><ReadOnlyField label="Driver Name" value={existingDc?.driverName} /></Grid>
                <Grid item xs={12} sm={6}><ReadOnlyField label="Item Description" value={existingDc?.itemDescription} /></Grid>
                <Grid item xs={12}><ReadOnlyField label="Remarks" value={existingDc?.remarks} /></Grid>
              </Grid>
            </>
          ) : (
            <form onSubmit={handleSubmit(onSubmit)}>
              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <Typography variant="subtitle2" color="primary" gutterBottom>From</Typography>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Controller name="unitId" control={control} rules={{ required: 'Sending unit is required' }}
                    render={({ field }) => (
                      <TextField {...field} select label="Sending Unit *" fullWidth
                        error={!!errors.unitId} helperText={errors.unitId?.message}>
                        {units.map((u) => (
                          <MenuItem key={u.id} value={u.id}>{u.unitName}{u.defaultUnit ? ' (Default)' : ''}</MenuItem>
                        ))}
                      </TextField>
                    )} />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Controller name="dcType" control={control} rules={{ required: 'DC type is required' }}
                    render={({ field }) => (
                      <TextField {...field} select label="DC Type *" fullWidth
                        error={!!errors.dcType} helperText={errors.dcType?.message}>
                        <MenuItem value="RETURNABLE">Returnable</MenuItem>
                        <MenuItem value="NON_RETURNABLE">Non-Returnable</MenuItem>
                      </TextField>
                    )} />
                </Grid>

                <Grid item xs={12}>
                  <Divider sx={{ my: 1 }} />
                  <Typography variant="subtitle2" color="primary" gutterBottom>To</Typography>
                  <Controller name="receiverType" control={control}
                    render={({ field }) => (
                      <ToggleButtonGroup {...field} exclusive size="small"
                        onChange={(_e, value) => value && field.onChange(value)}>
                        <ToggleButton value="PARTY">External Party</ToggleButton>
                        <ToggleButton value="UNIT">Company Unit</ToggleButton>
                      </ToggleButtonGroup>
                    )} />
                </Grid>
                <Grid item xs={12} sm={6}>
                  {receiverType === 'PARTY' ? (
                    <Controller name="receiverPartyId" control={control}
                      rules={{ required: 'Receiver party is required' }}
                      render={({ field }) => (
                        <TextField {...field} select label="Receiver Party *" fullWidth
                          error={!!errors.receiverPartyId} helperText={errors.receiverPartyId?.message}>
                          {parties.map((p) => (
                            <MenuItem key={p.id} value={p.id}>
                              {p.partyName}{p.companyName ? ` — ${p.companyName}` : ''} ({p.partyCode})
                            </MenuItem>
                          ))}
                        </TextField>
                      )} />
                  ) : (
                    <Controller name="receiverUnitId" control={control}
                      rules={{ required: 'Receiver unit is required' }}
                      render={({ field }) => (
                        <TextField {...field} select label="Receiver Unit *" fullWidth
                          error={!!errors.receiverUnitId} helperText={errors.receiverUnitId?.message}>
                          {receiverUnitOptions.map((u) => (
                            <MenuItem key={u.id} value={u.id}>{u.unitName}</MenuItem>
                          ))}
                        </TextField>
                      )} />
                  )}
                </Grid>

                <Grid item xs={12}>
                  <Divider sx={{ my: 1 }} />
                  <Typography variant="subtitle2" color="primary" gutterBottom>Shipment Details</Typography>
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
                    render={({ field }) => (
                      <TextField {...field} select label="Package Type" fullWidth>
                        <MenuItem value=""><em>None</em></MenuItem>
                        {packageTypes.map((pt) => (
                          <MenuItem key={pt.id} value={pt.id}>{pt.name}</MenuItem>
                        ))}
                      </TextField>
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

                <Grid item xs={12}>
                  <Controller name="itemDescription" control={control} rules={{ required: 'Item description is required' }}
                    render={({ field }) => (
                      <TextField {...field} label="Item Description *" fullWidth
                        error={!!errors.itemDescription} helperText={errors.itemDescription?.message} />
                    )} />
                </Grid>

                <Grid item xs={12} sm={4}>
                  <Controller name="weightKg" control={control}
                    rules={{ required: 'Weight is required', min: { value: 0.001, message: 'Must be > 0' } }}
                    render={({ field }) => (
                      <TextField {...field} label="Weight (kg) *" type="number" fullWidth
                        error={!!errors.weightKg} helperText={errors.weightKg?.message} />
                    )} />
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Controller name="noOfPackages" control={control}
                    rules={{ required: 'Required', min: { value: 1, message: 'Min 1' } }}
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

      {/* Approve / Reject dialog */}
      <Dialog open={!!decision} onClose={() => setDecision(null)} maxWidth="sm" fullWidth>
        <DialogTitle>{decision === 'approve' ? 'Approve Delivery Challan' : 'Reject Delivery Challan'}</DialogTitle>
        <DialogContent>
          <TextField label="Remarks" fullWidth multiline minRows={3} sx={{ mt: 1 }}
            value={decisionRemarks} onChange={(e) => setDecisionRemarks(e.target.value)} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDecision(null)}>Cancel</Button>
          <Button variant="contained" color={decision === 'approve' ? 'success' : 'error'}
            onClick={confirmDecision}>
            {decision === 'approve' ? 'Approve' : 'Reject'}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
