import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';

import { authApi } from '../api/endpoints.js';
import { registerUnauthorizedHandler, TOKEN_STORAGE_KEY } from '../api/client.js';

/**
 * Authentication state, shared across the whole app.
 *
 * Why Context instead of Redux? Auth state is one small object that changes rarely (login,
 * logout, refresh). Redux would add a store, actions and reducers to manage three fields.
 * Context is part of React, has no dependency, and is the right tool at this size - being able
 * to justify NOT using Redux is as valuable in an interview as knowing how to use it.
 *
 * Why Context instead of passing props? Because the navbar, the admin route guard, the
 * dashboard and the review form all need the user. Threading it through every intermediate
 * component is "prop drilling", and it couples components that have no business knowing about
 * auth.
 */
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  /**
   * Starts as true. This flag prevents a real bug: on a page refresh we have a token but no
   * user yet, and without it ProtectedRoute would briefly see `user === null` and bounce an
   * authenticated admin to the login page before the profile call finished.
   */
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  /**
   * useCallback memoises the function so its identity is stable between renders. That matters
   * because it goes into the useEffect dependency array below - an unstable function there
   * would re-run the effect on every render, in this case re-registering the handler forever.
   */
  const logout = useCallback(
    (message) => {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      setUser(null);
      if (message) toast.error(message);
      navigate('/login');
    },
    [navigate],
  );

  /**
   * Restores the session on page load (or hard refresh).
   *
   * The token lives in localStorage, so it survives a refresh - but the user PROFILE is
   * fetched from the server rather than also being cached. Two reasons: the cached copy could
   * be stale (role changed, account disabled), and anything in localStorage can be edited by
   * the user, so trusting a cached `role: "ROLE_ADMIN"` would be a client-side authorisation
   * decision. The server is the only authority.
   */
  useEffect(() => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (!token) {
      setLoading(false);
      return;
    }

    let cancelled = false;

    authApi
      .me()
      .then((profile) => {
        if (!cancelled) setUser(profile);
      })
      .catch(() => {
        // The token exists but the server rejected it - expired, or signed with an old secret.
        localStorage.removeItem(TOKEN_STORAGE_KEY);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    // Cleanup guards against setting state after the component unmounts, which React warns
    // about and which leaks memory in a long-lived app. StrictMode's double-invoke in
    // development makes this path easy to hit.
    return () => {
      cancelled = true;
    };
  }, []); // empty deps: run exactly once, on mount

  /**
   * Lets the axios interceptor trigger a logout when any request comes back 401.
   * The interceptor cannot import this module (circular import), so it accepts a callback.
   */
  useEffect(() => {
    registerUnauthorizedHandler(() => logout('Your session has expired. Please sign in again.'));
  }, [logout]);

  const login = useCallback(async (credentials) => {
    const data = await authApi.login(credentials);
    localStorage.setItem(TOKEN_STORAGE_KEY, data.token);
    setUser(data.user);
    return data.user;
  }, []);

  const register = useCallback(async (payload) => {
    const data = await authApi.register(payload);
    // The backend returns a token on registration too, so the user is signed in immediately
    // instead of being sent to a login form they just effectively filled in.
    localStorage.setItem(TOKEN_STORAGE_KEY, data.token);
    setUser(data.user);
    return data.user;
  }, []);

  /**
   * useMemo keeps the context VALUE object identity stable. Without it, a new object would be
   * created on every provider render, and every single consumer would re-render even when
   * nothing it cares about changed. This is the standard Context performance fix.
   */
  const value = useMemo(
    () => ({
      user,
      loading,
      login,
      register,
      logout,
      isAuthenticated: Boolean(user),
      // A convenience flag for the UI only. The real check is server-side: the backend rejects
      // non-admin calls to /admin/** regardless of what the browser believes.
      isAdmin: user?.role === 'ROLE_ADMIN',
    }),
    [user, loading, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/**
 * Custom hook for consumers.
 *
 * The null check turns a confusing downstream "cannot read property user of null" into a clear
 * message naming the actual mistake: the component is rendered outside the provider.
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside an <AuthProvider>');
  }
  return context;
}
