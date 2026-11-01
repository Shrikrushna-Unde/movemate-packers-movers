import { Navigate, Outlet, useLocation } from 'react-router-dom';

import { useAuth } from '../context/AuthContext.jsx';
import { Spinner } from './ui/Feedback.jsx';

/**
 * Route guard.
 *
 * Wraps a group of routes and decides whether the current user may see them.
 *
 * Be clear about what this is and is not. It is a UX control: it keeps a logged-out visitor
 * from landing on an empty dashboard. It is NOT security - anyone can edit JavaScript in their
 * browser and render the admin page. The actual protection is the Spring Security rule
 * `requestMatchers("/api/v1/admin/**").hasRole("ADMIN")`, which means a faked admin UI would
 * load and then show nothing but 403s. Client-side guards are a convenience; the server is the
 * authority. Say that sentence in an interview.
 *
 * @param {boolean} adminOnly also require the ROLE_ADMIN role
 */
export default function ProtectedRoute({ adminOnly = false }) {
  const { isAuthenticated, isAdmin, loading } = useAuth();
  const location = useLocation();

  /*
   * The loading gate is essential. On a hard refresh AuthContext has a token but has not yet
   * fetched the profile, so `isAuthenticated` is briefly false. Without this check we would
   * redirect an authenticated user to /login every time they pressed F5.
   */
  if (loading) {
    return <Spinner label="Checking your session" className="min-h-[60vh]" />;
  }

  if (!isAuthenticated) {
    /*
     * `state` carries the page the user wanted, so Login can send them back there after
     * signing in instead of dumping everyone on the homepage.
     * `replace` swaps the history entry rather than pushing one, so the browser Back button
     * does not bounce the user between the guarded page and the login screen.
     */
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  // Authenticated but not an admin: send them somewhere useful rather than showing a
  // dead-end "forbidden" page.
  if (adminOnly && !isAdmin) {
    return <Navigate to="/dashboard" replace />;
  }

  // Access granted - render the matched child route.
  return <Outlet />;
}
