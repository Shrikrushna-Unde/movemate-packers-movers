import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { CheckCircle2, Copy, Search } from 'lucide-react';

import Button, { buttonClasses } from '../components/ui/Button.jsx';
import { Checkbox, Input, Select, Textarea } from '../components/ui/Field.jsx';
import { Card, SectionHeading } from '../components/ui/Card.jsx';
import { Alert } from '../components/ui/Feedback.jsx';
import QuoteSummary from '../components/booking/QuoteSummary.jsx';
import { bookingApi, catalogApi, quoteApi } from '../api/endpoints.js';
import { useApiData } from '../hooks/useApiData.js';
import { useAuth } from '../context/AuthContext.jsx';
import { todayIso } from '../utils/format.js';

/**
 * The quote + booking page.
 *
 * This is the most involved screen in the app, so the structure is worth reading closely:
 *
 *   1. ONE state object holds the whole form. Nineteen separate useState calls would be
 *      nineteen setters to thread into nineteen inputs, and every "reset the form" or
 *      "prefill from the logged-in user" operation would touch all of them.
 *
 *   2. The live estimate is DEBOUNCED. Firing a request on every keystroke in the distance
 *      field would mean a dozen calls while someone types "450". We wait 600 ms after typing
 *      stops. This is the single most useful performance pattern in form-heavy UIs.
 *
 *   3. The price shown is never sent to the server. The booking endpoint recalculates it from
 *      the same inputs. The estimate is a display convenience, not an authority.
 */

/** The initial form values. A named constant so "reset" is one assignment, not nineteen. */
const EMPTY_FORM = {
  // Move details - these drive the price
  moveType: 'LOCAL_HOUSE_SHIFTING',
  homeSize: 'TWO_BHK',
  distanceKm: 15,
  pickupFloor: 0,
  dropFloor: 0,
  liftAvailable: true,
  packingRequired: true,
  insuranceRequired: false,
  declaredValue: '',
  storageDays: 0,
  // Logistics
  pickupAddress: '',
  pickupCity: '',
  pickupPincode: '',
  dropAddress: '',
  dropCity: '',
  dropPincode: '',
  moveDate: '',
  // Contact
  customerName: '',
  customerEmail: '',
  customerPhone: '',
  customerNotes: '',
};

export default function GetQuote() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState(EMPTY_FORM);
  const [quote, setQuote] = useState(null);
  const [quoting, setQuoting] = useState(false);
  const [quoteError, setQuoteError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  /** Field-level messages from the backend, keyed by field name. */
  const [fieldErrors, setFieldErrors] = useState({});
  /** Set once the booking succeeds; switches the page to the confirmation view. */
  const [confirmed, setConfirmed] = useState(null);

  // Dropdown options come from the backend enums - see CatalogController.options().
  const { data: options } = useApiData(() => catalogApi.options());

  /**
   * Prefill the contact fields for a signed-in customer.
   *
   * The dependency array is [user], so this runs when the user object arrives (which is after
   * the first render, because the profile is fetched asynchronously). The spread keeps anything
   * already typed - overwriting a field the user has edited would be infuriating.
   */
  useEffect(() => {
    if (!user) return;
    setForm((previous) => ({
      ...previous,
      customerName: previous.customerName || user.fullName || '',
      customerEmail: previous.customerEmail || user.email || '',
      customerPhone: previous.customerPhone || user.phone || '',
    }));
  }, [user]);

  /**
   * One change handler for every input.
   *
   * It reads `name`, `type` and `value` straight off the DOM event, so each input only needs a
   * matching `name` attribute - no per-field handler. The checkbox branch exists because a
   * checkbox carries its state in `checked`, not `value`.
   *
   * Note `(previous) => ...`: the functional form of setState. Using `setForm({...form, ...})`
   * would read a possibly stale `form` from the closure, which drops updates when two changes
   * land in the same tick.
   */
  const handleChange = (event) => {
    const { name, type, value, checked } = event.target;
    setForm((previous) => ({ ...previous, [name]: type === 'checkbox' ? checked : value }));
    // Clear this field's error as soon as the user edits it, instead of leaving a stale red
    // message under a field they have already fixed.
    setFieldErrors((previous) => {
      if (!previous[name]) return previous;
      const next = { ...previous };
      delete next[name];
      return next;
    });
  };

  /** Does this move type need a home size? Read from the backend metadata, not hardcoded. */
  const requiresHomeSize = useMemo(() => {
    const option = options?.moveTypes?.find((item) => item.value === form.moveType);
    // Defaults to true while options are still loading, so the field is not hidden and then
    // suddenly required.
    return option ? option.meta.requiresHomeSize : true;
  }, [options, form.moveType]);

  /**
   * Only the fields that affect the price. Serialised to a string so the effect below can use
   * it as a dependency - React compares dependencies by reference, and a new object literal
   * every render would re-trigger the effect endlessly.
   */
  const pricingInputs = JSON.stringify({
    moveType: form.moveType,
    homeSize: requiresHomeSize ? form.homeSize : null,
    distanceKm: form.distanceKm,
    pickupFloor: form.pickupFloor,
    dropFloor: form.dropFloor,
    liftAvailable: form.liftAvailable,
    packingRequired: form.packingRequired,
    insuranceRequired: form.insuranceRequired,
    declaredValue: form.declaredValue,
    storageDays: form.storageDays,
  });

  /**
   * The debounced live estimate.
   *
   * How the debounce works: every time `pricingInputs` changes, the previous effect's cleanup
   * runs and clears the pending timer, then a new 600 ms timer starts. The request only fires
   * once the user has stopped changing things for 600 ms. The cleanup function is what makes
   * this work - without it, every keystroke would simply queue another request.
   */
  useEffect(() => {
    const payload = JSON.parse(pricingInputs);

    // Guard: do not call the API with input the backend will reject anyway.
    if (payload.distanceKm === '' || Number(payload.distanceKm) < 0) return;
    if (requiresHomeSize && !payload.homeSize) return;
    if (payload.insuranceRequired && !payload.declaredValue) return;

    let cancelled = false;

    const timer = setTimeout(() => {
      setQuoting(true);
      setQuoteError(null);

      quoteApi
        .estimate({
          ...payload,
          // The HTML number input gives us strings; the API expects numbers.
          distanceKm: Number(payload.distanceKm),
          pickupFloor: Number(payload.pickupFloor) || 0,
          dropFloor: Number(payload.dropFloor) || 0,
          storageDays: Number(payload.storageDays) || 0,
          // null, not "": the backend treats an absent declared value as "no insurance", while
          // an empty string would fail BigDecimal parsing.
          declaredValue: payload.declaredValue ? Number(payload.declaredValue) : null,
        })
        .then((result) => {
          if (!cancelled) setQuote(result);
        })
        .catch((error) => {
          if (!cancelled) {
            setQuoteError(error.message);
            // Drop the stale quote: showing a price from the previous input set while the
            // current one is invalid is worse than showing none.
            setQuote(null);
          }
        })
        .finally(() => {
          if (!cancelled) setQuoting(false);
        });
    }, 600);

    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [pricingInputs, requiresHomeSize]);

  /** Creates the booking. */
  const handleSubmit = async (event) => {
    // Without this, the browser does a full page reload on submit and all React state is lost.
    event.preventDefault();

    // Hiding the submit button does not prevent submission: pressing Enter in any text input
    // still fires onSubmit. Without this guard a signed-out visitor would fire the request and
    // get a bare 401 toast instead of being taken somewhere useful.
    if (!user) {
      navigate('/login', { state: { from: '/quote' } });
      return;
    }

    setSubmitting(true);
    setFieldErrors({});

    try {
      const created = await bookingApi.create({
        ...form,
        homeSize: requiresHomeSize ? form.homeSize : null,
        distanceKm: Number(form.distanceKm),
        pickupFloor: Number(form.pickupFloor) || 0,
        dropFloor: Number(form.dropFloor) || 0,
        storageDays: Number(form.storageDays) || 0,
        declaredValue: form.declaredValue ? Number(form.declaredValue) : null,
        // Optional text fields: send null rather than "" so the column stays NULL.
        pickupPincode: form.pickupPincode || null,
        dropPincode: form.dropPincode || null,
        customerNotes: form.customerNotes || null,
      });

      setConfirmed(created);
      toast.success(`Booking confirmed · ${created.reference}`);
    } catch (error) {
      // The backend returns per-field messages, so they can be attached to the right inputs
      // rather than dumped in one banner. This is the payoff of GlobalExceptionHandler
      // flattening MethodArgumentNotValidException into a map.
      if (error.fieldErrors) {
        setFieldErrors(error.fieldErrors);
        toast.error('Please correct the highlighted fields');
      } else {
        toast.error(error.message);
      }
    } finally {
      // `finally` so the button is re-enabled whether the call succeeded or failed. Putting
      // this only in the success path is how forms get stuck on "Submitting..." forever.
      setSubmitting(false);
    }
  };

  // ------------------------------------------------------------------
  //  Confirmation view
  // ------------------------------------------------------------------
  if (confirmed) {
    return (
      <div className="container-page max-w-3xl py-16">
        <Card className="p-8 text-center">
          <CheckCircle2 className="mx-auto h-12 w-12 text-emerald-500" aria-hidden="true" />
          <h1 className="mt-5 text-2xl font-bold text-slate-900">Your move is booked</h1>
          <p className="mt-2 text-slate-600">
            A coordinator will call {confirmed.customerPhone} within two hours to confirm the
            survey and your move date.
          </p>

          <div className="mt-7 rounded-2xl bg-brand-50 p-5 ring-1 ring-inset ring-brand-100">
            <p className="text-xs font-semibold uppercase tracking-wider text-brand-700">
              Your booking reference
            </p>
            <div className="mt-2 flex items-center justify-center gap-3">
              <span className="text-2xl font-extrabold tracking-wider text-brand-950">
                {confirmed.reference}
              </span>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  // The Clipboard API is async and can be blocked (insecure origin, denied
                  // permission), so the failure path has to be handled - not assumed away.
                  navigator.clipboard
                    ?.writeText(confirmed.reference)
                    .then(() => toast.success('Reference copied'))
                    .catch(() => toast.error('Could not copy. Please note it down.'));
                }}
              >
                <Copy className="h-4 w-4" aria-hidden="true" />
                Copy
              </Button>
            </div>
            <p className="mt-3 text-xs text-slate-600">
              Save this. You will need it along with your phone number to track the booking.
            </p>
          </div>

          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Link
              to={`/track?reference=${confirmed.reference}`}
              className={buttonClasses({ variant: 'primary' })}
            >
              <Search className="h-4 w-4" aria-hidden="true" />
              Track this booking
            </Link>
            <Button
              variant="outline"
              onClick={() => {
                // Reset for a second booking rather than forcing a page reload.
                setConfirmed(null);
                setForm(EMPTY_FORM);
                setQuote(null);
              }}
            >
              Book another move
            </Button>
          </div>
        </Card>
      </div>
    );
  }

  // ------------------------------------------------------------------
  //  Form view
  // ------------------------------------------------------------------
  return (
    <div className="container-page py-12 sm:py-16">
      <SectionHeading
        align="left"
        eyebrow="Instant quote"
        title="Tell us about your move"
        subtitle="The estimate on the right updates as you type. Pricing is open to everyone - you only need an account at the point of confirming the booking."
      />

      {/*
        The layout: a two-column grid on large screens that collapses to a single column on
        mobile, with the summary ABOVE the form on small screens (order-first) so the price is
        visible without scrolling past nineteen inputs.
      */}
      <div className="mt-10 grid gap-8 lg:grid-cols-[1.4fr_1fr] lg:items-start">
        <form onSubmit={handleSubmit} className="space-y-6" noValidate>
          {/* ---------------- 1. Move details ---------------- */}
          <Card className="p-6">
            <h2 className="text-base font-bold text-slate-900">1. What are you moving?</h2>

            <div className="mt-5 grid gap-5 sm:grid-cols-2">
              <Select
                label="Move type"
                name="moveType"
                value={form.moveType}
                onChange={handleChange}
                required
                // Options come straight from the API, so this dropdown can never disagree with
                // the Java MoveType enum.
                options={options?.moveTypes ?? []}
                error={fieldErrors.moveType}
              />

              {/* Conditional field: hidden entirely for office, vehicle and storage jobs. */}
              {requiresHomeSize && (
                <Select
                  label="Home size"
                  name="homeSize"
                  value={form.homeSize}
                  onChange={handleChange}
                  required
                  options={options?.homeSizes ?? []}
                  error={fieldErrors.homeSize}
                  hint="Drives the goods volume, crew size and vehicle class"
                />
              )}

              <Input
                label="Distance (km)"
                name="distanceKm"
                type="number"
                min="0"
                max="4000"
                value={form.distanceKm}
                onChange={handleChange}
                required
                error={fieldErrors.distanceKm}
                hint="First 25 km are included in the base charge"
              />

              <Input
                label="Storage needed (days)"
                name="storageDays"
                type="number"
                min="0"
                max="365"
                value={form.storageDays}
                onChange={handleChange}
                error={fieldErrors.storageDays}
                hint="Leave as 0 if you do not need warehousing"
              />
            </div>

            <div className="mt-5 grid gap-5 sm:grid-cols-3">
              <Input
                label="Pickup floor"
                name="pickupFloor"
                type="number"
                min="0"
                max="60"
                value={form.pickupFloor}
                onChange={handleChange}
                error={fieldErrors.pickupFloor}
              />
              <Input
                label="Drop floor"
                name="dropFloor"
                type="number"
                min="0"
                max="60"
                value={form.dropFloor}
                onChange={handleChange}
                error={fieldErrors.dropFloor}
              />
              <div className="flex items-end">
                <Checkbox
                  name="liftAvailable"
                  checked={form.liftAvailable}
                  onChange={handleChange}
                  label="Lift available"
                  description="At both ends"
                />
              </div>
            </div>

            <div className="mt-5 grid gap-4 sm:grid-cols-2">
              <Checkbox
                name="packingRequired"
                checked={form.packingRequired}
                onChange={handleChange}
                label="Professional packing & unpacking"
                description="Cartons, bubble wrap, stretch film and labour included"
              />
              <Checkbox
                name="insuranceRequired"
                checked={form.insuranceRequired}
                onChange={handleChange}
                label="Transit insurance"
                description="2.5% of your declared goods value, minimum Rs 500"
              />
            </div>

            {/* Only asked for when insurance is ticked - the backend rejects insurance with no
                declared value, so the UI must not allow that combination. */}
            {form.insuranceRequired && (
              <Input
                className="mt-5 sm:max-w-xs"
                label="Declared value of goods (Rs)"
                name="declaredValue"
                type="number"
                min="1"
                value={form.declaredValue}
                onChange={handleChange}
                required
                error={fieldErrors.declaredValue}
                hint="Your honest estimate of total replacement value"
              />
            )}
          </Card>

          {/* ---------------- 2. Addresses ---------------- */}
          <Card className="p-6">
            <h2 className="text-base font-bold text-slate-900">2. Where and when?</h2>

            <div className="mt-5 grid gap-5 sm:grid-cols-2">
              <Textarea
                className="sm:col-span-2"
                label="Pickup address"
                name="pickupAddress"
                rows={2}
                value={form.pickupAddress}
                onChange={handleChange}
                required
                placeholder="Flat 402, Sunrise Residency, Baner Road"
                error={fieldErrors.pickupAddress}
              />
              <Input
                label="Pickup city"
                name="pickupCity"
                value={form.pickupCity}
                onChange={handleChange}
                required
                placeholder="Pune"
                error={fieldErrors.pickupCity}
              />
              <Input
                label="Pickup PIN code"
                name="pickupPincode"
                value={form.pickupPincode}
                onChange={handleChange}
                // inputMode="numeric" brings up the number pad on a phone without the
                // spinner arrows and leading-zero problems of type="number".
                inputMode="numeric"
                maxLength={6}
                placeholder="411045"
                error={fieldErrors.pickupPincode}
              />

              <Textarea
                className="sm:col-span-2"
                label="Drop address"
                name="dropAddress"
                rows={2}
                value={form.dropAddress}
                onChange={handleChange}
                required
                placeholder="B-12, Green Meadows, Hinjewadi Phase 2"
                error={fieldErrors.dropAddress}
              />
              <Input
                label="Drop city"
                name="dropCity"
                value={form.dropCity}
                onChange={handleChange}
                required
                placeholder="Pune"
                error={fieldErrors.dropCity}
              />
              <Input
                label="Drop PIN code"
                name="dropPincode"
                value={form.dropPincode}
                onChange={handleChange}
                inputMode="numeric"
                maxLength={6}
                placeholder="411057"
                error={fieldErrors.dropPincode}
              />

              <Input
                label="Preferred move date"
                name="moveDate"
                type="date"
                // `min` stops the native date picker offering past dates at all, which is far
                // better UX than validating after submission.
                min={todayIso()}
                value={form.moveDate}
                onChange={handleChange}
                required
                error={fieldErrors.moveDate}
              />
            </div>
          </Card>

          {/* ---------------- 3. Contact ---------------- */}
          <Card className="p-6">
            <h2 className="text-base font-bold text-slate-900">3. How do we reach you?</h2>

            <div className="mt-5 grid gap-5 sm:grid-cols-2">
              <Input
                label="Full name"
                name="customerName"
                value={form.customerName}
                onChange={handleChange}
                required
                error={fieldErrors.customerName}
              />
              <Input
                label="Email"
                name="customerEmail"
                type="email"
                value={form.customerEmail}
                onChange={handleChange}
                required
                error={fieldErrors.customerEmail}
              />
              <Input
                label="Mobile number"
                name="customerPhone"
                inputMode="numeric"
                maxLength={10}
                value={form.customerPhone}
                onChange={handleChange}
                required
                placeholder="9876543210"
                error={fieldErrors.customerPhone}
                hint="10 digits, used to track the booking later"
              />
              <Textarea
                className="sm:col-span-2"
                label="Anything else we should know? (optional)"
                name="customerNotes"
                rows={3}
                value={form.customerNotes}
                onChange={handleChange}
                placeholder="Piano on the first floor, narrow staircase, parking available only before 9 AM"
                error={fieldErrors.customerNotes}
              />
            </div>

            {/*
              Booking requires an account, so a signed-out visitor gets a sign-in prompt in
              place of the submit button rather than a button that fails with a 401. The quote
              above stays visible and fully usable - the price is the reason they came, and
              demanding a login before showing it would lose them.

              `state: { from: '/quote' }` is the same mechanism ProtectedRoute uses, so Login
              returns them here afterwards with the form still filled in.
            */}
            {!user ? (
              <div className="mt-6 rounded-xl border border-brand-100 bg-brand-50/60 p-5">
                <p className="text-sm font-semibold text-slate-800">
                  Sign in to confirm this booking
                </p>
                <p className="mt-1 text-sm text-slate-600">
                  We need an account on the booking so you can track it, see its status and keep
                  your move history. Your estimate above is saved while you sign in.
                </p>
                <div className="mt-4 flex flex-col gap-2 sm:flex-row">
                  <Button
                    type="button"
                    variant="accent"
                    onClick={() => navigate('/login', { state: { from: '/quote' } })}
                  >
                    Sign in to continue
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => navigate('/register', { state: { from: '/quote' } })}
                  >
                    Create an account
                  </Button>
                </div>
              </div>
            ) : (
              <div className="mt-6 flex flex-col gap-3 border-t border-slate-100 pt-5 sm:flex-row sm:items-center sm:justify-between">
                <p className="text-xs text-slate-500">
                  By submitting you agree to be contacted about this move. No payment is taken now.
                </p>
                {/*
                  type="submit" is explicit because our Button defaults to type="button".
                  `loading` disables it, which is what prevents a double booking from an
                  impatient second click.
                */}
                <Button type="submit" variant="accent" size="lg" loading={submitting}>
                  {submitting ? 'Confirming...' : 'Confirm my booking'}
                </Button>
              </div>
            )}
          </Card>
        </form>

        {/* ---------------- Live estimate ---------------- */}
        <div className="order-first lg:order-last lg:sticky lg:top-24">
          <QuoteSummary quote={quote} loading={quoting} />

          {quoteError && (
            <Alert type="warning" className="mt-4">
              {quoteError}
            </Alert>
          )}
        </div>
      </div>
    </div>
  );
}
