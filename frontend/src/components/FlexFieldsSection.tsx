import { useEffect, useState } from 'react';
import {
  Autocomplete,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { flexFieldApi } from '../api/endpoints';
import { extractErrorMessage } from '../api/client';
import { useNotification } from '../context/NotificationContext';
import type { FlexFieldDefinition, FlexFieldValues } from '../types';

interface Props {
  module: string;
  entityId?: number;
  values: FlexFieldValues;
  onChange: (values: FlexFieldValues) => void;
  readOnly?: boolean;
}

export default function FlexFieldsSection({ module, entityId, values, onChange, readOnly }: Props) {
  const { notify } = useNotification();
  const [fields, setFields] = useState<FlexFieldDefinition[]>([]);

  useEffect(() => {
    flexFieldApi.getActiveFields(module).then(setFields).catch((err) => {
      notify(extractErrorMessage(err), 'error');
    });
  }, [module, notify]);

  useEffect(() => {
    if (entityId) {
      flexFieldApi.getValues(module, entityId).then((res) => {
        onChange(res.values ?? {});
      }).catch(() => {/* silent — no values yet */});
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [module, entityId]);

  if (fields.length === 0) return null;

  const set = (fieldId: number, val: string) => onChange({ ...values, [fieldId]: val });

  return (
    <Stack spacing={2}>
      <Typography variant="subtitle2" color="text.secondary" mt={1}>Additional Fields</Typography>
      {fields.map((f) => {
        const val = values[f.id] ?? '';

        if (f.fieldType === 'TEXT') {
          return (
            <TextField
              key={f.id}
              label={f.fieldLabel + (f.required ? ' *' : '')}
              value={val}
              onChange={(e) => set(f.id, e.target.value)}
              fullWidth
              disabled={readOnly}
            />
          );
        }

        if (f.fieldType === 'DROPDOWN_SINGLE') {
          const activeOptions = f.options.filter((o) => o.active).map((o) => o.optionValue);
          return (
            <Autocomplete
              key={f.id}
              options={activeOptions}
              value={val || null}
              onChange={(_, v) => set(f.id, v ?? '')}
              disabled={readOnly}
              renderInput={(params) => (
                <TextField {...params} label={f.fieldLabel + (f.required ? ' *' : '')} />
              )}
            />
          );
        }

        if (f.fieldType === 'DROPDOWN_MULTI') {
          const activeOptions = f.options.filter((o) => o.active).map((o) => o.optionValue);
          const selected = val ? val.split(',').map((s) => s.trim()).filter(Boolean) : [];
          return (
            <Autocomplete
              key={f.id}
              multiple
              options={activeOptions}
              value={selected}
              onChange={(_, v) => set(f.id, v.join(','))}
              disabled={readOnly}
              renderInput={(params) => (
                <TextField {...params} label={f.fieldLabel + (f.required ? ' *' : '')} />
              )}
            />
          );
        }

        return null;
      })}
    </Stack>
  );
}
