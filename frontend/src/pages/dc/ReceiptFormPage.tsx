import { useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  Divider,
  Grid,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useGetDcQuery } from '../../store/api/dcApiSlice';
import { useGetDcReceiptQuery, useCreateDcReceiptMutation, useDeleteDcReceiptMutation } from '../../store/api/dcReceiptApiSlice';
import { extractErrorMessage } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';
import { useAuth } from '../../context/AuthContext';
import type { DeliveryChallan } from '../../types';

function ReadOnlyField({ label, value }: { label: string; value?: string | number | null }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary">{label}</Typography>
      <Typography variant="body2" fontWeight={500}>{value ?? '—'}</Typography>
    </Box>
  );
}

function receiverLabel(dc: DeliveryChallan): string {
  return dc.receiverType === 'PARTY' ? (dc.receiverParty?.partyName ?? '—') : (dc.receiverUnit?.unitName ?? '—');
}

export default function ReceiptFormPage() {
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const dcId = searchParams.get('dcId');
  const navigate = useNavigate();
  const { notify } = useNotification();
  const { hasPermission } = useAuth();
  const isView = Boolean(id);

  const [remarks, setRemarks] = useState('');

  const { data: dc, isLoading: loadingDc } = useGetDcQuery(Number(dcId), { skip: isView || !dcId });
  const { data: receipt, isLoading: loadingReceipt } = useGetDcReceiptQuery(Number(id), { skip: !isView });
  const [createReceipt, { isLoading: creating }] = useCreateDcReceiptMutation();
  const [deleteReceipt] = useDeleteDcReceiptMutation();

  const confirm = async () => {
    if (!dcId) return;
    try {
      await createReceipt({ dcId: Number(dcId), remarks: remarks || null }).unwrap();
      notify('Receipt confirmed — DC marked as Returned', 'success');
      navigate('/dc-receipts');
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const undo = async () => {
    if (!id) return;
    if (!window.confirm('Undo this receipt? The originating DC will revert to its prior status.')) return;
    try {
      await deleteReceipt(Number(id)).unwrap();
      notify('Receipt undone', 'success');
      navigate('/dc-receipts');
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  if (isView && loadingReceipt) return <Typography>Loading…</Typography>;
  if (!isView && loadingDc) return <Typography>Loading…</Typography>;

  if (!isView && !dcId) {
    return (
      <Stack spacing={2}>
        <Typography variant="h5" fontWeight={600}>New DC Receipt</Typography>
        <Typography color="text.secondary">
          Go to DC Receipt and use the "New Receipt" button to pick an eligible DC.
        </Typography>
        <Button onClick={() => navigate('/dc-receipts')}>Back</Button>
      </Stack>
    );
  }

  const summary: DeliveryChallan | undefined = isView ? receipt?.dc : dc;

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5" fontWeight={600}>
          {isView ? 'DC Receipt Detail' : 'Confirm DC Receipt'}
        </Typography>
        <Stack direction="row" spacing={1}>
          {isView && hasPermission('RECEIPT_DELETE') && (
            <Button variant="outlined" color="error" onClick={undo}>Undo Receipt</Button>
          )}
          <Button onClick={() => navigate('/dc-receipts')}>Back</Button>
        </Stack>
      </Stack>

      {summary && (
        <Card>
          <CardContent>
            {isView && receipt && (
              <Stack direction="row" spacing={2} alignItems="center" mb={2}>
                <Typography variant="h6">{receipt.receiptNumber}</Typography>
              </Stack>
            )}

            <Typography variant="subtitle2" color="primary" gutterBottom>Delivery Challan</Typography>
            <Grid container spacing={2} mb={2}>
              <Grid item xs={6} sm={3}><ReadOnlyField label="DC No." value={summary.dcNumber} /></Grid>
              <Grid item xs={6} sm={3}><ReadOnlyField label="Unit" value={summary.unit.unitName} /></Grid>
              <Grid item xs={6} sm={3}><ReadOnlyField label="Receiver" value={receiverLabel(summary)} /></Grid>
              <Grid item xs={6} sm={3}><ReadOnlyField label="DC Type" value={summary.dcType === 'RETURNABLE' ? 'Returnable' : 'Non-Returnable'} /></Grid>
              <Grid item xs={12} sm={6}><ReadOnlyField label="Item Description" value={summary.itemDescription} /></Grid>
              <Grid item xs={6} sm={3}><ReadOnlyField label="No. of Packages" value={summary.noOfPackages} /></Grid>
              <Grid item xs={6} sm={3}><ReadOnlyField label="Weight (kg)" value={summary.weightKg} /></Grid>
            </Grid>

            <Divider sx={{ mb: 2 }} />

            <Typography variant="subtitle2" color="primary" gutterBottom>Receipt</Typography>
            {isView && receipt ? (
              <Grid container spacing={2}>
                <Grid item xs={6} sm={3}><ReadOnlyField label="Receipt Date" value={receipt.receiptDate} /></Grid>
                <Grid item xs={6} sm={3}><ReadOnlyField label="Received By" value={receipt.receivedBy} /></Grid>
                <Grid item xs={12}><ReadOnlyField label="Remarks" value={receipt.remarks} /></Grid>
              </Grid>
            ) : (
              <Stack spacing={2}>
                <TextField label="Remarks" fullWidth multiline minRows={2} value={remarks}
                  onChange={(e) => setRemarks(e.target.value)}
                  helperText="Optional — condition of goods, notes about the return, etc." />
                <Stack direction="row" spacing={1} justifyContent="flex-end">
                  <Button onClick={() => navigate('/dc-receipts')}>Cancel</Button>
                  <Button variant="contained" disabled={creating} onClick={confirm}>Confirm Receipt</Button>
                </Stack>
              </Stack>
            )}
          </CardContent>
        </Card>
      )}
    </Stack>
  );
}
