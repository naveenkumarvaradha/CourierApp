import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    chunkSizeWarningLimit: 800,
    rollupOptions: {
      output: {
        manualChunks(id) {
          // React ecosystem — smallest, most cacheable
          if (
            id.includes('/node_modules/react/') ||
            id.includes('/node_modules/react-dom/') ||
            id.includes('/node_modules/scheduler/')
          ) {
            return 'vendor-react';
          }
          // React Router
          if (id.includes('/node_modules/react-router')) {
            return 'vendor-router';
          }
          // MUI DataGrid — large, standalone
          if (id.includes('/node_modules/@mui/x-data-grid')) {
            return 'vendor-datagrid';
          }
          // MUI + Emotion — unavoidably large
          if (
            id.includes('/node_modules/@mui/') ||
            id.includes('/node_modules/@emotion/')
          ) {
            return 'vendor-mui';
          }
          // Everything else from node_modules (axios, date-fns, etc.)
          if (id.includes('/node_modules/')) {
            return 'vendor-misc';
          }
        },
      },
    },
  },
});
