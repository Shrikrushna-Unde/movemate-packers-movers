import { ArrowRight, CalendarDays, MapPin, Route } from 'lucide-react';

import { Card, StatusBadge } from '../ui/Card.jsx';
import { formatCurrency, formatDate, formatRelative } from '../../utils/format.js';

/**
 * A booking summary card, used in the customer dashboard.
 *
 * Purely presentational - it receives a booking object and renders it. No fetching, no state.
 * That means the same component works for the dashboard list today and for an admin detail
 * panel later, which is exactly what reusability is supposed to mean.
 */
export default function BookingCard({ booking }) {
  return (
    <Card className="p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          {/* font-mono + tracking-wide makes the reference easy to read character by character,
              which matters when someone is reading it out over the phone. */}
          <p className="font-mono text-sm font-bold tracking-wide text-brand-900">
            {booking.reference}
          </p>
          <p className="mt-0.5 text-xs text-slate-500">
            Booked {formatRelative(booking.createdAt)} · {booking.moveTypeLabel}
            {booking.homeSizeLabel ? ` · ${booking.homeSizeLabel}` : ''}
          </p>
        </div>
        <StatusBadge status={booking.status} label={booking.statusLabel} />
      </div>

      <div className="mt-4 grid gap-3 text-sm sm:grid-cols-2">
        <div className="flex items-start gap-2">
          <MapPin className="mt-0.5 h-4 w-4 shrink-0 text-slate-400" aria-hidden="true" />
          <div className="min-w-0">
            <p className="text-xs uppercase tracking-wide text-slate-400">From</p>
            {/* truncate (text-overflow: ellipsis) stops a long address breaking the card
                layout. It needs the min-w-0 on the parent flex child to work. */}
            <p className="truncate font-medium text-slate-800">{booking.pickupCity}</p>
            <p className="truncate text-xs text-slate-500">{booking.pickupAddress}</p>
          </div>
        </div>

        <div className="flex items-start gap-2">
          <ArrowRight className="mt-0.5 h-4 w-4 shrink-0 text-slate-400" aria-hidden="true" />
          <div className="min-w-0">
            <p className="text-xs uppercase tracking-wide text-slate-400">To</p>
            <p className="truncate font-medium text-slate-800">{booking.dropCity}</p>
            <p className="truncate text-xs text-slate-500">{booking.dropAddress}</p>
          </div>
        </div>
      </div>

      <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-4 text-sm">
        <span className="flex items-center gap-1.5 text-slate-600">
          <CalendarDays className="h-4 w-4 text-slate-400" aria-hidden="true" />
          {formatDate(booking.moveDate)}
        </span>
        <span className="flex items-center gap-1.5 text-slate-600">
          <Route className="h-4 w-4 text-slate-400" aria-hidden="true" />
          {booking.distanceKm} km
        </span>
        <span className="font-bold text-slate-900">{formatCurrency(booking.estimatedAmount)}</span>
      </div>
    </Card>
  );
}
