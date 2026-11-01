import { CalendarCheck, IndianRupee, Mail, MessageSquareWarning, Star, Users } from 'lucide-react';

import { Card, StatusBadge } from '../ui/Card.jsx';
import { Alert, Spinner } from '../ui/Feedback.jsx';
import { adminApi } from '../../api/endpoints.js';
import { useApiData } from '../../hooks/useApiData.js';
import { formatCurrency } from '../../utils/format.js';

/** One dashboard tile. Extracted because six copies of the same markup is five too many. */
function StatTile({ icon: Icon, label, value, tone = 'brand' }) {
  const tones = {
    brand: 'bg-brand-50 text-brand-700',
    amber: 'bg-amber-50 text-amber-700',
    emerald: 'bg-emerald-50 text-emerald-700',
    rose: 'bg-rose-50 text-rose-700',
  };
  return (
    <Card className="p-5">
      <span className={`grid h-10 w-10 place-items-center rounded-xl ${tones[tone]}`}>
        <Icon className="h-5 w-5" aria-hidden="true" />
      </span>
      <p className="mt-4 text-xs font-semibold uppercase tracking-wider text-slate-500">{label}</p>
      <p className="mt-1 text-2xl font-extrabold text-slate-900">{value}</p>
    </Card>
  );
}

/**
 * The admin dashboard tiles, from GET /admin/stats.
 *
 * Every number is aggregated by MySQL (COUNT, SUM) rather than fetched and counted in
 * JavaScript. That is not premature optimisation: loading every booking row to display one
 * integer is the kind of thing that works fine with 30 bookings and times out with 30,000.
 */
export default function AdminOverview() {
  const { data: stats, loading, error } = useApiData(() => adminApi.stats());

  if (loading) return <Spinner label="Loading dashboard" />;
  if (error) return <Alert type="error">{error}</Alert>;

  return (
    <div className="space-y-8">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <StatTile icon={CalendarCheck} label="Total bookings" value={stats.totalBookings} />
        <StatTile
          icon={IndianRupee}
          label="Pipeline value"
          value={formatCurrency(stats.pipelineValue)}
          tone="emerald"
        />
        <StatTile icon={Users} label="Registered customers" value={stats.totalCustomers} />
        <StatTile
          icon={Mail}
          label="Unhandled enquiries"
          value={stats.unhandledEnquiries}
          tone={stats.unhandledEnquiries > 0 ? 'amber' : 'brand'}
        />
        <StatTile
          icon={MessageSquareWarning}
          label="Reviews awaiting approval"
          value={stats.pendingTestimonials}
          tone={stats.pendingTestimonials > 0 ? 'amber' : 'brand'}
        />
        <StatTile
          icon={Star}
          label="Average rating"
          // The backend returns null when nothing is approved yet, so we show a dash rather
          // than "null" or a misleading 0.0.
          value={stats.averageRating ? `${stats.averageRating.toFixed(1)} / 5` : '—'}
          tone="amber"
        />
      </div>

      <Card className="p-6">
        <h3 className="text-base font-bold text-slate-900">Bookings by status</h3>
        <p className="mt-1 text-sm text-slate-600">
          The pipeline value above excludes cancelled bookings.
        </p>

        <div className="mt-5 space-y-3">
          {/*
            Object.entries turns the { PENDING: 4, CONFIRMED: 2 } map into [key, value] pairs.
            The backend sends it as a LinkedHashMap in lifecycle order, and JSON objects
            preserve insertion order, so these rows come out in the right sequence without the
            frontend knowing what that sequence is.
          */}
          {Object.entries(stats.bookingsByStatus).map(([status, count]) => {
            const percentage =
              stats.totalBookings > 0 ? Math.round((count / stats.totalBookings) * 100) : 0;
            return (
              <div key={status} className="flex items-center gap-4">
                <div className="w-28 shrink-0">
                  <StatusBadge status={status} label={status.replace('_', ' ')} />
                </div>
                {/* A CSS bar chart. No charting library for one bar per row - that would be
                    100 kB of JavaScript to draw a rectangle. */}
                <div className="h-2 flex-1 overflow-hidden rounded-full bg-slate-100">
                  <div
                    className="h-full rounded-full bg-brand-600 transition-all"
                    style={{ width: `${percentage}%` }}
                  />
                </div>
                <span className="w-16 shrink-0 text-right text-sm font-semibold tabular-nums text-slate-700">
                  {count}
                </span>
              </div>
            );
          })}
        </div>
      </Card>
    </div>
  );
}
