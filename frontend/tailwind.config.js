/** @type {import('tailwindcss').Config} */
export default {
  // Scan all TSX/TS source files for class names
  content: ['./index.html', './src/**/*.{ts,tsx}'],

  // Disable Tailwind's preflight (base reset) so it doesn't fight MUI's CssBaseline
  corePlugins: {
    preflight: false,
  },

  // Use a prefix so tw-* classes never clash with MUI class names
  prefix: 'tw-',

  theme: {
    extend: {
      colors: {
        brand: {
          50:  '#eff6ff',
          100: '#dbeafe',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          900: '#1e3a8a',
        },
      },
    },
  },

  plugins: [],
};
