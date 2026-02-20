import { defineConfig } from 'vite';
import solid from 'vite-plugin-solid';

export default defineConfig({
  plugins: [solid()],
  server: {
    port: 5173,
    proxy: {
      // Backend (Pedestal) runs on http://localhost:8090
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true
      }
    }
  }
});
