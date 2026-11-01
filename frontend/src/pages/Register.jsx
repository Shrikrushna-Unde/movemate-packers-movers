import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { Truck, UserPlus } from 'lucide-react';

import Button from '../components/ui/Button.jsx';
import { Input } from '../components/ui/Field.jsx';
import { Card } from '../components/ui/Card.jsx';
import { Alert } from '../components/ui/Feedback.jsx';
import { useAuth } from '../context/AuthContext.jsx';

const EMPTY = { fullName: '', email: '', phone: '', password: '', confirmPassword: '' };

export default function Register() {
  const [form, setForm] = useState(EMPTY);
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const { register } = useAuth();
  const navigate = useNavigate();

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((previous) => ({ ...previous, [name]: value }));
    setFieldErrors((previous) => ({ ...previous, [name]: undefined }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);

    /*
     * confirmPassword is validated ONLY on the client, and that is correct: it is a typo guard,
     * not a business rule, and the server has no use for a second copy of the password. Note
     * that it is also never sent - see the destructuring below.
     */
    if (form.password !== form.confirmPassword) {
      setFieldErrors({ confirmPassword: 'Passwords do not match' });
      return;
    }

    setSubmitting(true);
    setFieldErrors({});

    try {
      // Pull confirmPassword out of the payload. `rest` holds everything else, which is exactly
      // the shape the backend's RegisterRequest expects.
      const { confirmPassword, ...payload } = form;
      void confirmPassword; // referenced so linters do not flag it as unused
      const user = await register(payload);
      toast.success(`Account created. Welcome, ${user.fullName.split(' ')[0]}`);
      navigate('/dashboard', { replace: true });
    } catch (err) {
      if (err.fieldErrors) {
        setFieldErrors(err.fieldErrors);
      } else {
        // A 409 from the backend lands here: "An account with this email already exists."
        setError(err.message);
      }
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
        <h1 className="mt-5 text-2xl font-bold text-slate-900">Create your account</h1>
        <p className="mt-2 text-sm text-slate-600">
          You can book without an account - signing up just keeps your move history and lets you
          leave a review.
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
            label="Full name"
            name="fullName"
            value={form.fullName}
            onChange={handleChange}
            required
            autoComplete="name"
            error={fieldErrors.fullName}
          />
          <Input
            label="Email"
            name="email"
            type="email"
            value={form.email}
            onChange={handleChange}
            required
            autoComplete="email"
            error={fieldErrors.email}
          />
          <Input
            label="Mobile number"
            name="phone"
            inputMode="numeric"
            maxLength={10}
            value={form.phone}
            onChange={handleChange}
            required
            autoComplete="tel"
            placeholder="9876543210"
            error={fieldErrors.phone}
          />
          <Input
            label="Password"
            name="password"
            type="password"
            value={form.password}
            onChange={handleChange}
            required
            // new-password tells the browser to offer a generated password instead of
            // autofilling the user's existing one.
            autoComplete="new-password"
            error={fieldErrors.password}
            hint="At least 8 characters, with one letter and one number"
          />
          <Input
            label="Confirm password"
            name="confirmPassword"
            type="password"
            value={form.confirmPassword}
            onChange={handleChange}
            required
            autoComplete="new-password"
            error={fieldErrors.confirmPassword}
          />

          <Button type="submit" fullWidth size="lg" loading={submitting}>
            <UserPlus className="h-4 w-4" aria-hidden="true" />
            {submitting ? 'Creating account...' : 'Create account'}
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600">
          Already registered?{' '}
          <Link to="/login" className="font-semibold text-brand-700 hover:underline">
            Sign in
          </Link>
        </p>
      </Card>
    </div>
  );
}
