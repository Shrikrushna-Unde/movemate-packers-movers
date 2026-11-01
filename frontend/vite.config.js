import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

/**
 * Vite configuration.
 *
 * Why Vite instead of Create React App?
 *   - CRA is no longer maintained and was formally deprecated by the React team.
 *   - Vite serves raw ES modules in development, so the dev server starts in under a second
 *     regardless of project size, and Hot Module Replacement updates a component without
 *     losing its state.
 *   - For production it bundles with Rollup, producing hashed, tree-shaken, minified assets.
 *
 * The build output is a folder of static files (dist/), which is why the frontend can be hosted
 * free on Vercel, Netlify or a Render Static Site - there is no Node process in production.
 */
export default defineConfig({
  plugins: [react()],

  server: {
    port: 5173,
    // Fail loudly instead of silently moving to 5174 - a different port would no longer match
    // the CORS origin allow-list on the Spring Boot side, and the resulting errors are
    // confusing to debug.
    strictPort: true,
  },

  build: {
    outDir: 'dist',
    // Source maps make a production stack trace readable instead of pointing at minified
    // one-letter variable names. They cost nothing at runtime (the browser only fetches them
    // when devtools is open).
    sourcemap: true,
    // Warn later than the 500 kB default: our single bundle includes React, Router and icons,
    // which is normal for an app this size and not worth code-splitting yet.
    chunkSizeWarningLimit: 900,
  },
});
