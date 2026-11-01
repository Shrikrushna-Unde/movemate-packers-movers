import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { CalendarDays, MapPin, PackageSearch, Route } from 'lucide-react';

import Button from '../components/ui/Button.jsx';
import { Input } from '../components/ui/Field.jsx';
import { Card, SectionHeading, StatusBadge } from '../components/ui/Card.jsx';
import { Alert } from '../components/ui/Feedback.jsx';
import BookingTimeline from '../components/booking/BookingTimeline.jsx';
import { bookingApi } from '../api/endpoints.js';
import { formatCurrency, formatDate } from '../utils/format.js';

/**
 * Public booking tracker.
 *
 * Requires BOTH the reference and the phone number, which is the whole authorisation model for
 * this page: the reference alone is not enough to read someone's home address. The backend
 * enforces it with `findByReferenceAndCustomerPhone`; this form just collects both.
 */
export default function TrackBooking() {
  /**
   * useSearchParams reads the query string, so the confirmation page can link straight to
   * /track?reference=MM-2026-XXXX and prefill the field. Small touch, noticeably better flow
   * than making someone copy a reference between two pages.
   *
   * Note that the phone number is NOT prefilled from the URL - putting a phone number in a URL
   * would leak it into browser history, server logs and any analytics script on the page.
   */
  const [searchParams] = useSearchParams();

  const [reference, setReference] = useState(searchParams.get('reference') ?? '');
  const [phone, setPhone] = useState('');
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Keep the field in sync if the query string changes while the page is mounted.
  useEffect(() => {
    const fromUrl = searchParams.get('reference');
    if (fromUrl) setReference(fromUrl);
  }, [searchParams]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setBooking(null);

    try {
      // toUpperCase because references are uppercase and nobody types them that way.
      // Normalising in the UI avoids a pointless "not found" for a correct reference.
      const result = await bookingApi.track(reference.trim().toUpperCase(), phone.trim());
      setBooking(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container-page max-w-3xl py-12 sm:py-16">
      <SectionHeading
        eyebrow="Track your move"
        title="Where are my goods?"
        subtitle="Enter the reference from your booking confirmation along with the phone number on that booking."
      />

      <Card className="mt-10 p-6">
        <form onSubmit={handleSubmit} className="grid gap-5 sm:grid-cols-[1.3fr_1fr_auto] sm:items-end">
          <Input
            label="Booking reference"
            value={reference}
            onChange={(event) => setReference(event.target.value)}
            required
            placeholder="MM-2026-4F7K2Q"
            // font-mono signals "this is a code, not a sentence". It works through the wrapper
            // because Tailwind's Preflight sets `font-family: inherit` on form elements, which
            // browsers do not do by default. Case is normalised in the submit handler, so the
            // user never has to hold shift.
            className="font-mono"
            autoComplete="off"
          />
          <Input
            label="Phone number"
            value={phone}
            onChange={(event) => setPhone(event.target.value)}
            required
            inputMode="numeric"
            maxLength={10}
            placeholder="9876543210"
          />
          <Button type="submit" loading={loading} className="sm:mb-0.5">
            <PackageSearch className="h-4 w-4" aria-hidden="true" />
            Track
          </Button>
        </form>
      </Card>

      {error && (
        <Alert type="error" title="We could not find that booking" className="mt-6">
          {error} Check that the reference and phone number match your confirmation exactly.
        </Alert>
      )}

      {booking && (
        <Card className="mt-6 p-6 sm:p-8">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="font-mono text-base font-bold tracking-wide text-brand-900">
                {booking.reference}
              </p>
              <p className="mt-1 text-sm text-slate-600">
                {booking.moveTypeLabel}
                {booking.homeSizeLabel ? ` · ${booking.homeSizeLabel}` : ''} ·{' '}
                {booking.customerName}
              </p>
            </div>
            <StatusBadge status={booking.status} label={booking.statusLabel} />
          </div>

          <div className="mt-6 grid gap-4 rounded-xl bg-slate-50 p-4 text-sm sm:grid-cols-3">
            <div className="flex items-start gap-2">
              <CalendarDays className="mt-0.5 h-4 w-4 text-slate-400" aria-hidden="true" />
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Move date</p>
                <p className="font-medium text-slate-800">{formatDate(booking.moveDate)}</p>
              </div>
            </div>
            <div className="flex items-start gap-2">
              <Route className="mt-0.5 h-4 w-4 text-slate-400" aria-hidden="true" />
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Distance</p>
                <p className="font-medium text-slate-800">{booking.distanceKm} km</p>
              </div>
            </div>
            <div className="flex items-start gap-2">
              <MapPin className="mt-0.5 h-4 w-4 text-slate-400" aria-hidden="true" />
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Route</p>
                <p className="font-medium text-slate-800">
                  {booking.pickupCity} → {booking.dropCity}
                </p>
              </div>
            </div>
          </div>

          <div className="mt-8">
            <h3 className="text-sm font-bold uppercase tracking-wider text-slate-700">Progress</h3>
            <div className="mt-5">
              <BookingTimeline
                timeline={booking.timeline}
                cancelled={booking.status === 'CANCELLED'}
              />
            </div>
          </div>

          <div className="mt-8 flex items-center justify-between border-t border-slate-100 pt-5">
            <span className="text-sm text-slate-600">Quoted amount</span>
            <span className="text-lg font-extrabold text-slate-900">
              {formatCurrency(booking.estimatedAmount)}
            </span>
          </div>
        </Card>
      )}
    </div>
  );
}
