import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { LogIn, Truck } from 'lucide-react';

import Button from '../components/ui/Button.jsx';
import { Input } from '../components/ui/Field.jsx';
import { Card } from '../components/ui/Card.jsx';
import { Alert } from '../components/ui/Feedback.jsx';
import { useAuth } from '../context/AuthContext.jsx';

export default function Login() {
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  /**
   * Where to go after signing in.
   *
   * ProtectedRoute stores the attempted path in `location.state.from`, so a user who clicked a
   * link to /admin and got bounced here lands back on /admin rather than the homepage. The
   * fallback is null, which means "decide based on role" below.
   */
  const redirectTo = location.state?.from ?? null;

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((previous) => ({ ...previous, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      // login() returns the profile, so the role is available immediately. Reading `user` from
      // the context here would give the PREVIOUS value - state updates are not synchronous,
      // and this is one of the most common React misunderstandings.
      const user = await login(form);
      toast.success(`Welcome back, ${user.fullName.split(' ')[0]}`);
      navigate(redirectTo ?? (user.role === 'ROLE_ADMIN' ? '/admin' : '/dashboard'), {
        // replace so the Back button does not return to the login form of a now-active session.
        replace: true,
      });
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container-page flex min-h-[70vh] max-w-md flex-col justify-center py-12">
      <div className="text-center">
        <span className="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-brand-950 text-accent-400">
          <Truck className="h-6 w-6" aria-hidden="true" />
        </span>
        <h1 className="mt-5 text-2xl font-bold text-slate-900">Sign in to MoveMate</h1>
        <p className="mt-2 text-sm text-slate-600">
          Track your bookings, review past moves and manage your account.
        </p>
      </div>

      <Card className="mt-8 p-6 sm:p-7">
        {error && (
          <Alert type="error" className="mb-5">
            {error}
          </Alert>
        )}

        <form onSubmit={handleSubmit} className="space-y-5" noValidate>
          <Input
            label="Email"
            name="email"
            type="email"
            value={form.email}
            onChange={handleChange}
            required
            // autoComplete tells the browser's password manager what this field is, so saved
            // credentials actually offer to fill it. Omitting it is a small but real UX loss.
            autoComplete="email"
            placeholder="you@example.com"
          />
          <Input
            label="Password"
            name="password"
            type="password"
            value={form.password}
            onChange={handleChange}
            required
            autoComplete="current-password"
          />

          <Button type="submit" fullWidth size="lg" loading={submitting}>
            <LogIn className="h-4 w-4" aria-hidden="true" />
            {submitting ? 'Signing in...' : 'Sign in'}
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600">
          New here?{' '}
          <Link to="/register" className="font-semibold text-brand-700 hover:underline">
            Create an account
          </Link>
        </p>
      </Card>

      {/*
        Demo credentials, shown on purpose. This is a portfolio project, and a reviewer who has
        to email you for a password will simply close the tab. A real product would obviously
        never do this.
      */}
      <Card className="mt-5 bg-slate-50 p-5">
        <p className="text-xs font-bold uppercase tracking-wider text-slate-500">
          Demo accounts for reviewers
        </p>
        <dl className="mt-3 space-y-1.5 text-xs text-slate-600">
          <div className="flex justify-between gap-4">
            <dt>Admin</dt>
            <dd className="font-mono">admin@movemate.in / Admin@123</dd>
          </div>
          <div className="flex justify-between gap-4">
            <dt>Customer</dt>
            <dd className="font-mono">customer@movemate.in / Customer@123</dd>
          </div>
        </dl>
      </Card>
    </div>
  );
}
