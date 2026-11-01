import axios from 'axios';

/**
 * The single configured HTTP client for the whole app.
 *
 * Every component imports THIS instance rather than axios directly. That one rule buys us:
 *   - the base URL in one place (so deploying to a new backend is one env var),
 *   - the Authorization header attached automatically,
 *   - uniform error handling,
 * and it means no component ever contains a hardcoded localhost URL - the mistake that breaks
 * the app the moment it is deployed.
 */

/** Key used for the token in localStorage. A constant so a typo cannot create a second key. */
export const TOKEN_STORAGE_KEY = 'movemate.token';

export const api = axios.create({
  // import.meta.env is Vite's equivalent of process.env. Values are substituted at BUILD time,
  // so the production bundle must be rebuilt (not just restarted) after changing them.
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',

  // 15 seconds. Without a timeout, axios waits indefinitely and a sleeping free-tier backend
  // leaves the user staring at a spinner with no error and no way to retry.
  timeout: 15000,

  headers: { 'Content-Type': 'application/json' },
});

/**
 * REQUEST interceptor: attach the bearer token to every outgoing call.
 *
 * Doing this centrally is what stops `headers: { Authorization: ... }` from being copy-pasted
 * into thirty call sites - and from being forgotten in the thirty-first.
 */
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * Where to send the user after their session expires. AuthContext registers a callback here
 * instead of this module importing the router, which would create a circular import.
 */
let onUnauthorized = null;

export function registerUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

/**
 * RESPONSE interceptor: normalise every failure into one predictable shape.
 *
 * An axios error can be any of three very different things, and the UI should not have to know
 * which: a response with an error status, a request that never got a response (network down,
 * CORS blocked, timeout), or a bug in our own code before the request was sent.
 */
api.interceptors.response.use(
  // Success path: unwrap `response.data` so components write `const data = await getServices()`
  // instead of `const { data } = await ...` everywhere.
  (response) => response,

  (error) => {
    // --- Case 1: the server answered with 4xx/5xx ---
    if (error.response) {
      const { status, data } = error.response;

      // A 401 on an authenticated request means the token is missing, expired or revoked.
      // Clear it and let AuthContext redirect, so the user gets a login screen rather than a
      // page of failed requests. The /auth/ exclusion matters: a wrong password on the login
      // form is also a 401, and bouncing the user off the login page would be absurd.
      if (status === 401 && !error.config?.url?.includes('/auth/')) {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        if (onUnauthorized) onUnauthorized();
      }

      return Promise.reject({
        status,
        // Our Spring Boot GlobalExceptionHandler always returns { message, fieldErrors },
        // which is exactly why the frontend error handling is this short.
        message: data?.message || 'Something went wrong. Please try again.',
        fieldErrors: data?.fieldErrors || null,
      });
    }

    // --- Case 2: the request was sent but no response came back ---
    if (error.request) {
      return Promise.reject({
        status: 0,
        message:
          error.code === 'ECONNABORTED'
            ? 'The server took too long to respond. Please try again.'
            : 'Cannot reach the server. Please check your connection and try again.',
        fieldErrors: null,
      });
    }

    // --- Case 3: something broke before the request left ---
    return Promise.reject({ status: -1, message: error.message, fieldErrors: null });
  },
);

export default api;
