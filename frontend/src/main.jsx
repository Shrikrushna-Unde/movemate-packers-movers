import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';

import App from './App.jsx';
import { AuthProvider } from './context/AuthContext.jsx';
import './index.css';

/**
 * Application bootstrap.
 *
 * The order of these wrappers is not arbitrary - each provider must sit OUTSIDE anything that
 * consumes it:
 *
 *   StrictMode        development-only checks
 *     BrowserRouter   provides routing context -> useNavigate, <Link>, useParams
 *       AuthProvider  needs useNavigate, so it must be inside the router
 *         App         consumes both
 *
 * Putting AuthProvider outside BrowserRouter is a classic mistake: it throws
 * "useNavigate() may be used only in the context of a <Router>" at runtime.
 */
ReactDOM.createRoot(document.getElementById('root')).render(
  /*
   * StrictMode deliberately double-invokes component bodies and effects in DEVELOPMENT ONLY,
   * to surface side effects that are not idempotent. If you ever see a request fire twice
   * locally but once in production, this is why - and it is telling you the effect has a bug
   * worth fixing, not that StrictMode should be removed.
   */
  <React.StrictMode>
    {/*
      BrowserRouter uses the HTML5 History API, so URLs are clean (/services, not /#/services).
      The deployment consequence matters: the static host must rewrite every unknown path to
      index.html, or a hard refresh on /services returns 404. That is what the rewrite rule in
      vercel.json / netlify.toml is for.
    */}
    <BrowserRouter>
      <AuthProvider>
        <App />
        {/*
          One Toaster at the root renders every toast fired from anywhere in the tree, so no
          component needs its own notification state.
        */}
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 4000,
            style: { borderRadius: '12px', background: '#0f172a', color: '#f8fafc' },
          }}
        />
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
