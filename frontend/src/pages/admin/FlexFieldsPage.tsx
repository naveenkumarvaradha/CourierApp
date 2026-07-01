import { useCallback, useEffect, useState } from 'react';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Box,
  Button,
  Checkbox,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import { adminApi } from '../../api/endpoints';
import { extractErrorMessage } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';
import { useAuth } from '../../context/AuthContext';
import type { FlexFieldDefinition, FlexFieldType } from '../../types';

const MODULES = ['BOOKING', 'PARTY'];
const FIELD_TYPES: FlexFieldType[] = ['TEXT', 'DROPDOWN_SINGLE', 'DROPDOWN_MULTI'];
const FIELD_TYPE_LABELS: Record<FlexFieldType, string> = {
  TEXT: 'Text Box',
  DROPDOWN_SINGLE: 'Dropdown – Single Select',
  DROPDOWN_MULTI: 'Dropdown – Multi Select',
};

interface FieldForm {
  id?: number;
  module: string;
  fieldName: string;
  fieldLabel: string;
  fieldType: FlexFieldType;
  required: boolean;
  active: boolean;
  sortOrder: number;
}

const EMPTY_FIELD: FieldForm = {
  module: 'BOOKING',
  fieldName: '',
  fieldLabel: '',
  fieldType: 'TEXT',
  required: false,
  active: true,
  sortOrder: 0,
};

interface OptionForm {
  optionValue: string;
  sortOrder: number;
  active: boolean;
}

export default function FlexFieldsPage() {
  const { notify } = useNotification();
  const { hasPermission } = useAuth();
  const [fields, setFields] = useState<FlexFieldDefinition[]>([]);
  const [loading, setLoading] = useState(false);
  const [fieldOpen, setFieldOpen] = useState(false);
  const [form, setForm] = useState<FieldForm>(EMPTY_FIELD);
  const [optionOpen, setOptionOpen] = useState(false);
  const [optionFieldId, setOptionFieldId] = useState<number | null>(null);
  const [optionForm, setOptionForm] = useState<OptionForm>({ optionValue: '', sortOrder: 0, active: true });

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setFields(await adminApi.listFlexFields());
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    } finally {
      setLoading(false);
    }
  }, [notify]);

  useEffect(() => { load(); }, [load]);

  const openCreate = (module?: string) => {
    setForm({ ...EMPTY_FIELD, module: module ?? 'BOOKING' });
    setFieldOpen(true);
  };

  const openEdit = (f: FlexFieldDefinition) => {
    setForm({
      id: f.id,
      module: f.module,
      fieldName: f.fieldName,
      fieldLabel: f.fieldLabel,
      fieldType: f.fieldType,
      required: f.required,
      active: f.active,
      sortOrder: f.sortOrder,
    });
    setFieldOpen(true);
  };

  const saveField = async () => {
    if (!form.fieldName.trim() || !form.fieldLabel.trim()) {
      notify('Field name and label are required', 'warning');
      return;
    }
    try {
      if (form.id) {
        await adminApi.updateFlexField(form.id, { ...form });
        notify('Field updated', 'success');
      } else {
        await adminApi.createFlexField({ ...form });
        notify('Field created', 'success');
      }
      setFieldOpen(false);
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const deleteField = async (id: number) => {
    if (!window.confirm('Delete this flex field and all its options/values?')) return;
    try {
      await adminApi.deleteFlexField(id);
      notify('Deleted', 'success');
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const openAddOption = (fieldId: number) => {
    setOptionFieldId(fieldId);
    setOptionForm({ optionValue: '', sortOrder: 0, active: true });
    setOptionOpen(true);
  };

  const saveOption = async () => {
    if (!optionForm.optionValue.trim() || !optionFieldId) return;
    try {
      await adminApi.addFlexFieldOption(optionFieldId, {
        optionValue: optionForm.optionValue.trim(),
        sortOrder: optionForm.sortOrder,
        active: optionForm.active,
      });
      notify('Option added', 'success');
      setOptionOpen(false);
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const deleteOption = async (fieldId: number, optionId: number) => {
    try {
      await adminApi.deleteFlexFieldOption(fieldId, optionId);
      notify('Option removed', 'success');
      load();
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    }
  };

  const fieldsByModule = MODULES.reduce<Record<string, FlexFieldDefinition[]>>((acc, mod) => {
    acc[mod] = fields.filter((f) => f.module === mod);
    return acc;
  }, {});

  if (loading && fields.length === 0) return <Typography>Loading…</Typography>;

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5" fontWeight={600}>Flex Fields</Typography>
        {hasPermission('ADMIN_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => openCreate()}>
            Add Flex Field
          </Button>
        )}
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Define custom fields per module. Text fields accept free-text input. Dropdown fields require
        options to be added below.
      </Typography>

      {MODULES.map((mod) => (
        <Accordion key={mod} defaultExpanded>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Stack direction="row" spacing={2} alignItems="center" width="100%">
              <Typography fontWeight={600}>{mod}</Typography>
              <Chip size="small" label={`${fieldsByModule[mod].length} fields`} />
              {hasPermission('ADMIN_CREATE') && (
                <Button
                  size="small"
                  startIcon={<AddIcon />}
                  onClick={(e) => { e.stopPropagation(); openCreate(mod); }}
                  sx={{ ml: 'auto', mr: 1 }}
                >
                  Add to {mod}
                </Button>
              )}
            </Stack>
          </AccordionSummary>
          <AccordionDetails>
            {fieldsByModule[mod].length === 0 ? (
              <Typography variant="body2" color="text.secondary">No custom fields defined for {mod}.</Typography>
            ) : (
              <Stack spacing={1}>
                {fieldsByModule[mod].map((f) => (
                  <Box key={f.id} sx={{ border: 1, borderColor: 'divider', borderRadius: 1, p: 1.5 }}>
                    <Stack direction="row" alignItems="center" spacing={1} flexWrap="wrap">
                      <Typography fontWeight={500}>{f.fieldLabel}</Typography>
                      <Typography variant="caption" color="text.secondary">({f.fieldName})</Typography>
                      <Chip size="small" label={FIELD_TYPE_LABELS[f.fieldType]} variant="outlined" />
                      {f.required && <Chip size="small" label="Required" color="warning" />}
                      <Chip size="small" label={f.active ? 'Active' : 'Inactive'} color={f.active ? 'success' : 'default'} />
                      <Box flex={1} />
                      {hasPermission('ADMIN_UPDATE') && (
                        <Tooltip title="Edit field">
                          <IconButton size="small" onClick={() => openEdit(f)}><EditIcon fontSize="small" /></IconButton>
                        </Tooltip>
                      )}
                      {hasPermission('ADMIN_DELETE') && (
                        <Tooltip title="Delete field">
                          <IconButton size="small" color="error" onClick={() => deleteField(f.id)}>
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                    </Stack>

                    {(f.fieldType === 'DROPDOWN_SINGLE' || f.fieldType === 'DROPDOWN_MULTI') && (
                      <Box mt={1} ml={1}>
                        <Stack direction="row" alignItems="center" spacing={1} mb={0.5}>
                          <Typography variant="caption" color="text.secondary" fontWeight={600}>OPTIONS</Typography>
                          {hasPermission('ADMIN_CREATE') && (
                            <Tooltip title="Add option">
                              <IconButton size="small" onClick={() => openAddOption(f.id)}>
                                <AddCircleOutlineIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                        </Stack>
                        <Stack direction="row" flexWrap="wrap" spacing={0.5}>
                          {f.options.filter((o) => o.active).map((o) => (
                            <Chip
                              key={o.id}
                              size="small"
                              label={o.optionValue}
                              onDelete={hasPermission('ADMIN_DELETE') ? () => deleteOption(f.id, o.id) : undefined}
                            />
                          ))}
                          {f.options.filter((o) => o.active).length === 0 && (
                            <Typography variant="caption" color="text.secondary">No options yet — click + to add.</Typography>
                          )}
                        </Stack>
                      </Box>
                    )}
                  </Box>
                ))}
              </Stack>
            )}
          </AccordionDetails>
        </Accordion>
      ))}

      {/* Field dialog */}
      <Dialog open={fieldOpen} onClose={() => setFieldOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{form.id ? 'Edit Flex Field' : 'Add Flex Field'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <FormControl fullWidth>
              <InputLabel>Module</InputLabel>
              <Select value={form.module} label="Module" onChange={(e) => setForm({ ...form, module: e.target.value })}>
                {MODULES.map((m) => <MenuItem key={m} value={m}>{m}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField
              label="Field Name (internal key)"
              fullWidth
              value={form.fieldName}
              onChange={(e) => setForm({ ...form, fieldName: e.target.value })}
              helperText="Unique identifier, no spaces (e.g. invoice_no)"
            />
            <TextField
              label="Field Label (shown to users)"
              fullWidth
              value={form.fieldLabel}
              onChange={(e) => setForm({ ...form, fieldLabel: e.target.value })}
              helperText="e.g. Invoice Number"
            />
            <FormControl fullWidth>
              <InputLabel>Field Type</InputLabel>
              <Select
                value={form.fieldType}
                label="Field Type"
                onChange={(e) => setForm({ ...form, fieldType: e.target.value as FlexFieldType })}
              >
                {FIELD_TYPES.map((t) => <MenuItem key={t} value={t}>{FIELD_TYPE_LABELS[t]}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField
              label="Sort Order"
              type="number"
              value={form.sortOrder}
              onChange={(e) => setForm({ ...form, sortOrder: parseInt(e.target.value) || 0 })}
            />
            <Stack direction="row" spacing={2}>
              <FormControlLabel
                control={<Checkbox checked={form.required} onChange={(e) => setForm({ ...form, required: e.target.checked })} />}
                label="Required"
              />
              <FormControlLabel
                control={<Checkbox checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />}
                label="Active"
              />
            </Stack>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFieldOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={saveField}>Save</Button>
        </DialogActions>
      </Dialog>

      {/* Option dialog */}
      <Dialog open={optionOpen} onClose={() => setOptionOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Add Option</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <TextField
              label="Option Value"
              fullWidth
              autoFocus
              value={optionForm.optionValue}
              onChange={(e) => setOptionForm({ ...optionForm, optionValue: e.target.value })}
              onKeyDown={(e) => { if (e.key === 'Enter') saveOption(); }}
            />
            <TextField
              label="Sort Order"
              type="number"
              value={optionForm.sortOrder}
              onChange={(e) => setOptionForm({ ...optionForm, sortOrder: parseInt(e.target.value) || 0 })}
            />
            <FormControlLabel
              control={<Checkbox checked={optionForm.active} onChange={(e) => setOptionForm({ ...optionForm, active: e.target.checked })} />}
              label="Active"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOptionOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={saveOption}>Add</Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
