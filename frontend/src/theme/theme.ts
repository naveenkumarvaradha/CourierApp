import { createTheme } from '@mui/material/styles';

// Design tokens adapted from Apple's web design language
// (https://github.com/VoltAgent/awesome-design-md/tree/main/design-md/apple)
// SF Pro is proprietary to Apple platforms; Inter is used as the open-source
// substitute the design doc itself recommends, with a small letter-spacing
// nudge to approximate SF Pro's tighter tracking.
const fontStack =
  "Inter, -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'SF Pro Display', system-ui, sans-serif";

const colors = {
  primary: '#0066cc',
  primaryFocus: '#0071e3',
  ink: '#1d1d1f',
  inkMuted80: '#333333',
  inkMuted48: '#7a7a7a',
  hairline: '#e0e0e0',
  dividerSoft: '#f0f0f0',
  canvas: '#ffffff',
  parchment: '#f5f5f7',
};

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: colors.primary, dark: colors.primaryFocus, contrastText: '#ffffff' },
    secondary: { main: colors.ink, contrastText: '#ffffff' },
    background: { default: colors.parchment, paper: colors.canvas },
    text: { primary: colors.ink, secondary: colors.inkMuted48 },
    divider: colors.hairline,
  },
  typography: {
    fontFamily: fontStack,
    // Apple's weight ladder is 300 / 400 / 600 / 700 — 500 is deliberately absent.
    fontWeightLight: 300,
    fontWeightRegular: 400,
    fontWeightMedium: 600,
    fontWeightBold: 700,
    h1: { fontSize: '2.5rem', fontWeight: 600, lineHeight: 1.1, letterSpacing: '-0.01em' },
    h2: { fontSize: '2.125rem', fontWeight: 600, lineHeight: 1.2, letterSpacing: '-0.015em' },
    h3: { fontSize: '1.75rem', fontWeight: 400, lineHeight: 1.2, letterSpacing: 0 },
    h4: { fontSize: '1.3125rem', fontWeight: 600, lineHeight: 1.2, letterSpacing: '0.01em' },
    h5: { fontSize: '1.0625rem', fontWeight: 600, lineHeight: 1.24, letterSpacing: '-0.015em' },
    h6: { fontSize: '1.0625rem', fontWeight: 600, lineHeight: 1.24, letterSpacing: '-0.015em' },
    subtitle1: { fontSize: '1.0625rem', fontWeight: 600, letterSpacing: '-0.015em' },
    subtitle2: { fontSize: '0.9375rem', fontWeight: 600, letterSpacing: '-0.01em' },
    body1: { fontSize: '1.0625rem', fontWeight: 400, lineHeight: 1.47, letterSpacing: '-0.015em' },
    body2: { fontSize: '0.875rem', fontWeight: 400, lineHeight: 1.43, letterSpacing: '-0.01em' },
    button: { fontSize: '1rem', fontWeight: 400, textTransform: 'none', letterSpacing: 0 },
    caption: { fontSize: '0.875rem', fontWeight: 400, lineHeight: 1.43, letterSpacing: '-0.01em' },
    overline: { letterSpacing: '0.08em' },
  },
  shape: { borderRadius: 12 },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          fontFeatureSettings: '"ss03"',
          backgroundColor: colors.parchment,
        },
      },
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: {
          borderRadius: 9999,
          textTransform: 'none',
          padding: '8px 20px',
          fontWeight: 400,
          transition: 'transform 0.15s ease',
          boxShadow: 'none',
          '&:active': { transform: 'scale(0.96)' },
          '&:hover': { boxShadow: 'none' },
        },
        sizeSmall: { padding: '5px 14px' },
        sizeLarge: { padding: '11px 22px', fontSize: '1.0625rem' },
        outlined: { borderWidth: 1, '&:hover': { borderWidth: 1 } },
        containedPrimary: { '&:hover': { backgroundColor: colors.primaryFocus } },
        text: { borderRadius: 8 },
      },
    },
    MuiIconButton: {
      styleOverrides: {
        root: { transition: 'transform 0.15s ease', '&:active': { transform: 'scale(0.92)' } },
      },
    },
    MuiPaper: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: { backgroundImage: 'none' },
        elevation1: { boxShadow: 'none', border: `1px solid ${colors.hairline}` },
      },
    },
    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: {
          borderRadius: 18,
          border: `1px solid ${colors.hairline}`,
          boxShadow: 'none',
        },
      },
    },
    MuiAppBar: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: { backgroundColor: '#000000', backgroundImage: 'none' },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 11,
          '& .MuiOutlinedInput-notchedOutline': { borderColor: colors.hairline },
          '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: colors.inkMuted48 },
          '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
            borderColor: colors.primaryFocus,
            borderWidth: 2,
          },
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: { borderRadius: 9999, fontWeight: 400 },
      },
    },
    MuiDialog: {
      styleOverrides: {
        paper: { borderRadius: 18, boxShadow: '0 8px 40px rgba(0,0,0,0.16)' },
      },
    },
    MuiMenu: {
      styleOverrides: {
        paper: {
          borderRadius: 14,
          border: `1px solid ${colors.hairline}`,
          boxShadow: '0 8px 32px rgba(0,0,0,0.12)',
        },
      },
    },
    MuiPopover: {
      styleOverrides: {
        paper: {
          borderRadius: 14,
          border: `1px solid ${colors.hairline}`,
          boxShadow: '0 8px 32px rgba(0,0,0,0.12)',
        },
      },
    },
    MuiMenuItem: {
      styleOverrides: {
        root: { borderRadius: 8, fontWeight: 400 },
      },
    },
    MuiAlert: {
      styleOverrides: {
        root: { borderRadius: 11 },
      },
    },
    MuiTooltip: {
      styleOverrides: {
        tooltip: { backgroundColor: colors.ink, borderRadius: 8, fontSize: '0.75rem' },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: { borderColor: colors.hairline },
      },
    },
    MuiDivider: {
      styleOverrides: {
        root: { borderColor: colors.hairline },
      },
    },
  },
});
