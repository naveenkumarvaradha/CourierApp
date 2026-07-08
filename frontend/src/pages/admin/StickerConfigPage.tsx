import { useEffect, useRef, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  Divider,
  Grid,
  IconButton,
  Paper,
  Stack,
  Switch,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import SaveIcon from '@mui/icons-material/Save';
import RestartAltIcon from '@mui/icons-material/RestartAlt';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import DragIndicatorIcon from '@mui/icons-material/DragIndicator';
import EditIcon from '@mui/icons-material/Edit';
import CheckIcon from '@mui/icons-material/Check';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import { adminApi } from '../../api/endpoints';
import { extractErrorMessage } from '../../api/client';
import { useNotification } from '../../context/NotificationContext';
import { useAuth } from '../../context/AuthContext';
import type { Company, StickerField } from '../../types';

// ── Section definitions ───────────────────────────────────────────────────────
const SECTIONS: { key: string; label: string; color: string; bg: string; hint: string }[] = [
  { key: 'HEADER',        label: 'Page Header',             color: '#1565c0', bg: '#e3f2fd',
    hint: 'Centered at top: "COURIER SHIPPING LABEL" then Company Name (bold)' },
  { key: 'DETAILS_LEFT',  label: 'Detail Left (Booking)',   color: '#e65100', bg: '#fff3e0',
    hint: 'Left column of detail row: Booking No + Courier Mode (e.g. SURFACE via DHL)' },
  { key: 'DETAILS_RIGHT', label: 'Detail Right (Date/Wt)',  color: '#0277bd', bg: '#e1f5fe',
    hint: 'Right column of detail row: Date + Weight (and optionally Pkgs / Package Type)' },
  { key: 'FROM',          label: 'FROM (Sender)',            color: '#6a1b9a', bg: '#f3e5f5',
    hint: 'Sender block — order: Creator Name → Mobile → Company Name → Address' },
  { key: 'TO',            label: 'TO (Receiver)',            color: '#2e7d32', bg: '#e8f5e9',
    hint: 'Receiver block: company, party name (large), address, phone, GSTIN' },
  { key: 'BOTTOM',        label: 'Footer (AWB)',             color: '#424242', bg: '#f5f5f5',
    hint: 'Full-width footer: AWB Number in large bold text' },
];

// ── Live preview ──────────────────────────────────────────────────────────────
function StickerPreview({ fields }: { fields: StickerField[] }) {
  const vis = (key: string) => fields.find((f) => f.fieldKey === key)?.visible ?? true;

  return (
    <Paper elevation={3} sx={{
      width: 345, border: '1.5px solid #bbb', borderRadius: 1,
      overflow: 'hidden', fontFamily: 'monospace', bgcolor: '#fff', flexShrink: 0,
    }}>
      {/* ── Header: Shipping Label + Company Name (centered) ── */}
      <Box sx={{ textAlign: 'center', borderBottom: '1px solid #ccc', px: 1, pt: 0.6, pb: 1, bgcolor: '#fafafa' }}>
        {vis('SHIPPING_LABEL') && (
          <Typography sx={{ fontSize: 9, fontWeight: 900, lineHeight: 1.3, letterSpacing: 0.3 }}>
            COURIER SHIPPING LABEL
          </Typography>
        )}
        {vis('COMPANY_NAME') && (
          <Typography sx={{ fontSize: 9, fontWeight: 800, lineHeight: 1.5, color: '#111', mt: 0.3 }}>
            CTL India Private Limited
          </Typography>
        )}
      </Box>

      {/* ── Detail row: left = Booking/Mode, right = Date/Weight ── */}
      <Grid container sx={{ borderBottom: '1px solid #ccc', px: 0.5, py: 0.4 }}>
        <Grid item xs={6} sx={{ pr: 0.5 }}>
          {vis('BOOKING_NUMBER') && (
            <Typography sx={{ fontSize: 7.5, fontWeight: 700 }}>Booking No: C1-CB-2026-00002</Typography>
          )}
          {vis('COURIER_MODE') && (
            <Typography sx={{ fontSize: 7, color: '#555' }}>SURFACE via DHL</Typography>
          )}
        </Grid>
        <Grid item xs={6}>
          {vis('DETAIL_DATE')     && <Typography sx={{ fontSize: 7.5, fontWeight: 700 }}>Date: 02-Jul-2026</Typography>}
          {vis('DETAIL_WEIGHT')   && <Typography sx={{ fontSize: 7.5, fontWeight: 700 }}>Weight: 1.000 kg</Typography>}
          {vis('DETAIL_PACKAGES') && <Typography sx={{ fontSize: 7, color: '#555' }}>Pkgs: 1</Typography>}
          {vis('DETAIL_PKG_TYPE') && <Typography sx={{ fontSize: 7, color: '#555' }}>Type: COVER</Typography>}
        </Grid>
      </Grid>

      {/* ── FROM | TO ── */}
      <Grid container sx={{ borderBottom: '1px solid #ccc' }}>
        <Grid item xs={5} sx={{ borderRight: '1px solid #ccc', px: 1, py: 0.5 }}>
          <Typography sx={{ fontSize: 6.5, color: '#777', fontWeight: 700, mb: 0.2 }}>FROM (SENDER)</Typography>
          {vis('FROM_NAME') && (
            <Typography sx={{ fontSize: 8, fontWeight: 800, lineHeight: 1.3 }}>Naveen Kumar</Typography>
          )}
          {vis('FROM_PHONE') && (
            <Typography sx={{ fontSize: 7.5, fontWeight: 700, color: '#333' }}>Mob: 9876543210</Typography>
          )}
          {vis('SENDER_COMPANY') && (
            <Typography sx={{ fontSize: 7, lineHeight: 1.3, color: '#444' }}>CTL India Pvt Ltd</Typography>
          )}
          {vis('FROM_ADDRESS') && (
            <Typography sx={{ fontSize: 7, color: '#444', lineHeight: 1.4 }}>
              SF No. 577 &amp; 585 CRPF ROAD<br />
              COIMBATORE - 641017, TN
            </Typography>
          )}
        </Grid>
        <Grid item xs={7} sx={{ px: 1, py: 0.5 }}>
          <Typography sx={{ fontSize: 6.5, color: '#777', fontWeight: 700, mb: 0.2 }}>TO (RECEIVER)</Typography>
          {vis('TO_COMPANY') && <Typography sx={{ fontSize: 7, color: '#555' }}>IT HUB</Typography>}
          {vis('TO_NAME')    && <Typography sx={{ fontSize: 12, fontWeight: 900, lineHeight: 1.2 }}>SIVA</Typography>}
          {vis('TO_ADDRESS') && (
            <Typography sx={{ fontSize: 7, color: '#444', lineHeight: 1.4 }}>
              GANDHIPURAM FLY OVER<br />
              COIMBATORE - 641017, TAMIL NADU<br />
              INDIA
            </Typography>
          )}
          {vis('TO_PHONE') && <Typography sx={{ fontSize: 7, color: '#444' }}>Ph: 34565437</Typography>}
          {vis('TO_GSTIN') && <Typography sx={{ fontSize: 7, color: '#444' }}>GSTIN: 33AAAAA0000A1Z5</Typography>}
        </Grid>
      </Grid>

      {/* ── Bottom: AWB Number (full width, large) ── */}
      {vis('AWB_NUMBER') && (
        <Box sx={{ px: 1, py: 0.5, borderTop: '1px solid #ccc' }}>
          <Typography sx={{ fontSize: 6.5, color: '#777', fontWeight: 700 }}>AWB NO.</Typography>
          <Typography sx={{ fontSize: 14, fontWeight: 900, letterSpacing: 0.4, lineHeight: 1.3 }}>
            35464638452441457687
          </Typography>
        </Box>
      )}
    </Paper>
  );
}

// ── Editable field chip ───────────────────────────────────────────────────────
function FieldChip({
  field,
  onToggle,
  onLabelChange,
  dragHandleProps,
}: {
  field: StickerField;
  onToggle: () => void;
  onLabelChange: (v: string) => void;
  dragHandleProps: React.HTMLAttributes<HTMLElement>;
}) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(field.label);

  const commit = () => {
    onLabelChange(draft.trim() || field.label);
    setEditing(false);
  };

  return (
    <Box
      sx={{
        display: 'inline-flex', alignItems: 'center',
        border: '1px solid', borderColor: field.visible ? 'primary.main' : 'grey.400',
        borderRadius: 1, bgcolor: field.visible ? 'primary.50' : 'grey.100',
        px: 0.5, py: 0.3, mr: 0.8, mb: 0.8,
        opacity: field.visible ? 1 : 0.55,
        cursor: 'grab', userSelect: 'none',
        '&:active': { cursor: 'grabbing' },
        minWidth: 120,
      }}
      {...dragHandleProps}
    >
      <DragIndicatorIcon sx={{ fontSize: 14, color: 'text.disabled', mr: 0.3, flexShrink: 0 }} />

      {editing ? (
        <TextField
          size="small"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onBlur={commit}
          onKeyDown={(e) => { if (e.key === 'Enter') commit(); if (e.key === 'Escape') setEditing(false); }}
          autoFocus
          inputProps={{ style: { fontSize: 11, padding: '1px 4px', width: 100 } }}
          sx={{ '& fieldset': { border: 'none' } }}
          onClick={(e) => e.stopPropagation()}
          onMouseDown={(e) => e.stopPropagation()}
        />
      ) : (
        <Typography sx={{ fontSize: 11, fontWeight: 600, mx: 0.5, flex: 1 }}>{field.label}</Typography>
      )}

      <Tooltip title="Rename label">
        <IconButton size="small" sx={{ p: 0.2 }}
          onClick={(e) => { e.stopPropagation(); if (editing) commit(); else { setDraft(field.label); setEditing(true); } }}
          onMouseDown={(e) => e.stopPropagation()}>
          {editing ? <CheckIcon sx={{ fontSize: 12 }} /> : <EditIcon sx={{ fontSize: 12 }} />}
        </IconButton>
      </Tooltip>

      <Tooltip title={field.visible ? 'Click to hide' : 'Click to show'}>
        <IconButton size="small" sx={{ p: 0.2 }}
          onClick={(e) => { e.stopPropagation(); onToggle(); }}
          onMouseDown={(e) => e.stopPropagation()}>
          {field.visible
            ? <VisibilityIcon sx={{ fontSize: 12, color: 'primary.main' }} />
            : <VisibilityOffIcon sx={{ fontSize: 12, color: 'text.disabled' }} />}
        </IconButton>
      </Tooltip>
    </Box>
  );
}

// ── Section band ──────────────────────────────────────────────────────────────
function SectionBand({
  section, fields, onToggle, onLabelChange, onDragStart, onDragOver, onDragEnd,
}: {
  section: typeof SECTIONS[0];
  fields: StickerField[];
  onToggle: (key: string) => void;
  onLabelChange: (key: string, v: string) => void;
  onDragStart: (e: React.DragEvent, key: string, sec: string) => void;
  onDragOver: (e: React.DragEvent, targetKey: string | null, sec: string) => void;
  onDragEnd: () => void;
}) {
  const visCount = fields.filter((f) => f.visible).length;

  return (
    <Box sx={{ display: 'flex', border: '1px solid #ddd' }}>
      {/* Section sidebar */}
      <Box sx={{
        width: 110, flexShrink: 0, bgcolor: section.bg,
        borderRight: `3px solid ${section.color}`,
        display: 'flex', flexDirection: 'column',
        alignItems: 'center', justifyContent: 'center',
        px: 0.5, py: 1,
      }}>
        <Typography sx={{
          fontSize: 10, fontWeight: 800, color: section.color,
          textTransform: 'uppercase', letterSpacing: 0.5,
          textAlign: 'center', lineHeight: 1.3,
          writingMode: 'vertical-rl', transform: 'rotate(180deg)',
        }}>
          {section.label}
        </Typography>
        <Chip size="small" label={`${visCount}/${fields.length}`}
          sx={{ mt: 1, fontSize: 9, height: 16, bgcolor: section.color, color: '#fff',
            '& .MuiChip-label': { px: 0.5 } }} />
        <Tooltip title={section.hint}>
          <InfoOutlinedIcon sx={{ fontSize: 13, color: section.color, mt: 0.5, opacity: 0.7 }} />
        </Tooltip>
      </Box>

      {/* Fields area */}
      <Box
        sx={{
          flex: 1, minHeight: 56, p: 1,
          display: 'flex', flexWrap: 'wrap', alignContent: 'flex-start', bgcolor: '#fafafa',
        }}
        onDragOver={(e) => { e.preventDefault(); onDragOver(e, null, section.key); }}
      >
        {fields.length === 0 && (
          <Typography sx={{ fontSize: 11, color: '#bbb', fontStyle: 'italic', alignSelf: 'center' }}>
            Drop fields here
          </Typography>
        )}
        {fields.map((f) => (
          <Box key={f.fieldKey} draggable
            onDragStart={(e) => onDragStart(e, f.fieldKey, section.key)}
            onDragOver={(e) => { e.preventDefault(); e.stopPropagation(); onDragOver(e, f.fieldKey, section.key); }}
            onDragEnd={onDragEnd}>
            <FieldChip
              field={f}
              onToggle={() => onToggle(f.fieldKey)}
              onLabelChange={(v) => onLabelChange(f.fieldKey, v)}
              dragHandleProps={{}}
            />
          </Box>
        ))}
      </Box>
    </Box>
  );
}

// ── Main page ─────────────────────────────────────────────────────────────────
export default function StickerConfigPage() {
  const { notify } = useNotification();
  const { user, hasPermission } = useAuth();
  const [companies, setCompanies] = useState<Company[]>([]);
  const [selectedCompanyId, setSelectedCompanyId] = useState<number | null>(null);
  const [fields, setFields] = useState<StickerField[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const dragging = useRef<{ fieldKey: string; fromSection: string } | null>(null);

  useEffect(() => {
    adminApi.listCompanies()
      .then((list) => {
        setCompanies(list);
        const own = list.find((c) => String(c.id) === String(user?.companyId ?? ''));
        const first = own ?? list[0];
        if (first) setSelectedCompanyId(first.id);
      })
      .catch(() => undefined);
  }, [user?.companyId]);

  useEffect(() => {
    if (!selectedCompanyId) return;
    setLoading(true);
    adminApi.getStickerConfig(selectedCompanyId)
      .then((f) => setFields([...f].sort((a, b) => a.sortOrder - b.sortOrder)))
      .catch((err) => notify(extractErrorMessage(err), 'error'))
      .finally(() => setLoading(false));
  }, [selectedCompanyId, notify]);

  const updateField = (fieldKey: string, patch: Partial<StickerField>) =>
    setFields((prev) => prev.map((f) => f.fieldKey === fieldKey ? { ...f, ...patch } : f));

  const toggleVisible = (fieldKey: string) =>
    setFields((prev) => prev.map((f) => f.fieldKey === fieldKey ? { ...f, visible: !f.visible } : f));

  // Drag-and-drop between sections
  const onDragStart = (_e: React.DragEvent, fieldKey: string, fromSection: string) => {
    dragging.current = { fieldKey, fromSection };
  };

  const onDragOver = (_e: React.DragEvent, targetFieldKey: string | null, targetSection: string) => {
    if (!dragging.current) return;
    const { fieldKey: srcKey } = dragging.current;
    if (srcKey === targetFieldKey) return;

    setFields((prev) => {
      const movedField = prev.find((f) => f.fieldKey === srcKey);
      if (!movedField) return prev;
      const allWithoutSrc = prev.filter((f) => f.fieldKey !== srcKey);
      const newSectionFields = allWithoutSrc.filter((f) => f.section === targetSection);
      const targetIdx = targetFieldKey
        ? newSectionFields.findIndex((f) => f.fieldKey === targetFieldKey)
        : newSectionFields.length;
      const insertAt = targetIdx >= 0 ? targetIdx : newSectionFields.length;
      newSectionFields.splice(insertAt, 0, { ...movedField, section: targetSection });
      return [
        ...allWithoutSrc.filter((f) => f.section !== targetSection),
        ...newSectionFields,
      ].map((f, i) => ({ ...f, sortOrder: i + 1 }));
    });

    dragging.current = { fieldKey: srcKey, fromSection: targetSection };
  };

  const onDragEnd = () => { dragging.current = null; };

  const save = async () => {
    if (!selectedCompanyId) return;
    setSaving(true);
    try {
      const payload = fields.map((f, i) => ({ ...f, sortOrder: i + 1 }));
      const result = await adminApi.saveStickerConfig(selectedCompanyId, payload);
      setFields([...result].sort((a, b) => a.sortOrder - b.sortOrder));
      notify('Sticker configuration saved', 'success');
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  const resetDefaults = async () => {
    if (!selectedCompanyId) return;
    if (!window.confirm('Reset to default field layout? This will clear any saved customization.')) return;
    setLoading(true);
    try {
      await adminApi.saveStickerConfig(selectedCompanyId, []);
      const result = await adminApi.getStickerConfig(selectedCompanyId);
      setFields([...result].sort((a, b) => a.sortOrder - b.sortOrder));
      notify('Reset to defaults', 'success');
    } catch (err) {
      notify(extractErrorMessage(err), 'error');
    } finally {
      setLoading(false);
    }
  };

  const fieldsInSection = (sectionKey: string) =>
    fields.filter((f) => f.section === sectionKey).sort((a, b) => a.sortOrder - b.sortOrder);

  const visibleCount = fields.filter((f) => f.visible).length;

  return (
    <Stack spacing={2}>
      {/* ── Toolbar ── */}
      <Stack direction="row" justifyContent="space-between" alignItems="center" flexWrap="wrap" gap={1}>
        <Stack direction="row" alignItems="center" spacing={1.5}>
          <Typography variant="h5" fontWeight={700}>Sticker Report Designer</Typography>
          <Chip size="small"
            label={`${visibleCount} of ${fields.length} fields visible`}
            color={visibleCount > 0 ? 'primary' : 'default'} />
        </Stack>
        <Stack direction="row" spacing={1}>
          {companies.length > 1 && (
            <TextField select label="Company" size="small"
              value={selectedCompanyId ?? ''} onChange={(e) => setSelectedCompanyId(Number(e.target.value))}
              sx={{ minWidth: 220 }} SelectProps={{ native: true }}>
              {companies.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </TextField>
          )}
          {hasPermission('ADMIN_UPDATE') && (
            <>
              <Button size="small" variant="outlined" startIcon={<RestartAltIcon />}
                onClick={resetDefaults} disabled={loading || saving}>Reset</Button>
              <Button size="small" variant="contained" startIcon={<SaveIcon />}
                onClick={save} disabled={loading || saving}>
                {saving ? 'Saving…' : 'Save'}
              </Button>
            </>
          )}
        </Stack>
      </Stack>

      <Alert severity="info" sx={{ py: 0.5 }}>
        <strong>Crystal Report Designer</strong> — Drag field chips between sections to reposition.
        Click <VisibilityIcon sx={{ fontSize: 13, verticalAlign: 'middle' }} /> to show/hide.
        Click <EditIcon sx={{ fontSize: 13, verticalAlign: 'middle' }} /> to rename a label.
        Hover <InfoOutlinedIcon sx={{ fontSize: 13, verticalAlign: 'middle' }} /> on each section for layout hints.
      </Alert>

      {loading && <Typography color="text.secondary">Loading…</Typography>}

      {!loading && (
        <Grid container spacing={2} alignItems="flex-start">
          {/* ── Left: Designer ── */}
          <Grid item xs={12} lg={7}>
            <Card sx={{ overflow: 'hidden' }}>
              {/* Crystal-style dark toolbar */}
              <Box sx={{
                bgcolor: '#263238', color: '#fff', px: 2, py: 1,
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              }}>
                <Typography sx={{ fontSize: 12, fontWeight: 700, letterSpacing: 1 }}>
                  REPORT DESIGN — COURIER SHIPPING LABEL
                </Typography>
                <Typography sx={{ fontSize: 11, color: '#90a4ae' }}>152mm × 101mm</Typography>
              </Box>

              {/* Section bands */}
              <Box sx={{ borderTop: '2px solid #263238' }}>
                {SECTIONS.map((section, si) => (
                  <Box key={section.key}>
                    <SectionBand
                      section={section}
                      fields={fieldsInSection(section.key)}
                      onToggle={toggleVisible}
                      onLabelChange={(key, v) => updateField(key, { label: v })}
                      onDragStart={onDragStart}
                      onDragOver={onDragOver}
                      onDragEnd={onDragEnd}
                    />
                    {si < SECTIONS.length - 1 && <Divider sx={{ borderColor: '#ccc' }} />}
                  </Box>
                ))}
              </Box>

              {/* Status bar */}
              <Box sx={{
                bgcolor: '#eceff1', px: 2, py: 0.5, borderTop: '1px solid #ccc',
                display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap',
              }}>
                {SECTIONS.map((s) => {
                  const cnt = fieldsInSection(s.key).filter((f) => f.visible).length;
                  return (
                    <Typography key={s.key} sx={{ fontSize: 10, color: s.color, fontWeight: 700 }}>
                      {s.label}: {cnt} visible
                    </Typography>
                  );
                })}
              </Box>
            </Card>

            {/* Section legend */}
            <Stack direction="row" flexWrap="wrap" gap={1} mt={1.5}>
              {SECTIONS.map((s) => (
                <Box key={s.key} sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  <Box sx={{ width: 10, height: 10, bgcolor: s.color, borderRadius: 0.5 }} />
                  <Typography sx={{ fontSize: 11, color: s.color, fontWeight: 700 }}>{s.label}</Typography>
                </Box>
              ))}
            </Stack>
          </Grid>

          {/* ── Right: Live preview + Quick toggles ── */}
          <Grid item xs={12} lg={5}>
            <Card>
              <Box sx={{ bgcolor: '#37474f', color: '#fff', px: 2, py: 1, display: 'flex', alignItems: 'center', gap: 1 }}>
                <VisibilityIcon sx={{ fontSize: 14 }} />
                <Typography sx={{ fontSize: 12, fontWeight: 700, letterSpacing: 1 }}>PRINT PREVIEW</Typography>
              </Box>
              <Box sx={{ bgcolor: '#607d8b', p: 3, display: 'flex', justifyContent: 'center', minHeight: 280 }}>
                <StickerPreview fields={fields} />
              </Box>
              <Box sx={{ px: 2, py: 0.8, bgcolor: '#eceff1', borderTop: '1px solid #ccc' }}>
                <Typography sx={{ fontSize: 10, color: '#555', textAlign: 'center' }}>
                  Preview updates live as you configure fields above
                </Typography>
              </Box>
            </Card>

            {/* Quick toggles */}
            <Card sx={{ mt: 2 }}>
              <Box sx={{ bgcolor: '#263238', color: '#fff', px: 2, py: 0.8 }}>
                <Typography sx={{ fontSize: 11, fontWeight: 700, letterSpacing: 1 }}>QUICK FIELD TOGGLES</Typography>
              </Box>
              <Box sx={{ p: 1.5 }}>
                {SECTIONS.map((section) => {
                  const sFields = fieldsInSection(section.key);
                  if (sFields.length === 0) return null;
                  return (
                    <Box key={section.key} sx={{ mb: 1 }}>
                      <Typography sx={{ fontSize: 10, fontWeight: 800, color: section.color,
                        textTransform: 'uppercase', mb: 0.3, borderBottom: `1px solid ${section.bg}`, pb: 0.2 }}>
                        {section.label}
                      </Typography>
                      <Grid container spacing={0.5}>
                        {sFields.map((f) => (
                          <Grid item xs={6} key={f.fieldKey}>
                            <Stack direction="row" alignItems="center" spacing={0.5}>
                              <Switch size="small" checked={f.visible}
                                onChange={() => toggleVisible(f.fieldKey)}
                                sx={{ '& .MuiSwitch-thumb': { bgcolor: f.visible ? section.color : undefined } }} />
                              <Typography sx={{
                                fontSize: 10,
                                color: f.visible ? 'text.primary' : 'text.disabled',
                                fontWeight: f.visible ? 600 : 400,
                                overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                              }}>
                                {f.label}
                              </Typography>
                            </Stack>
                          </Grid>
                        ))}
                      </Grid>
                    </Box>
                  );
                })}
              </Box>
            </Card>
          </Grid>
        </Grid>
      )}
    </Stack>
  );
}
