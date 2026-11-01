import { AlertCircle, CheckCircle2, Info, Loader2, PackageOpen } from 'lucide-react';

/**
 * The three states every data-driven screen needs: loading, error, and empty.
 *
 * Most tutorial React apps render only the success case, so a slow network shows a blank page
 * and a failed request shows nothing at all. Handling all three is the difference between a
 * demo and something you would put in front of a customer - and it is the first thing a
 * reviewer notices.
 */

/** Inline spinner for a section that is still loading. */
export function Spinner({ label = 'Loading', className = '' }) {
  return (
    <div className={`flex items-center justify-center gap-2 py-12 text-slate-500 ${className}`}>
      <Loader2 className="h-5 w-5 animate-spin" aria-hidden="true" />
      {/* role="status" makes a screen reader announce the change without stealing focus. */}
      <span role="status" className="text-sm">
        {label}...
      </span>
    </div>
  );
}

const ALERT_STYLES = {
  error: { wrap: 'bg-rose-50 text-rose-800 border-rose-200', Icon: AlertCircle },
  success: { wrap: 'bg-emerald-50 text-emerald-800 border-emerald-200', Icon: CheckCircle2 },
  info: { wrap: 'bg-blue-50 text-blue-800 border-blue-200', Icon: Info },
  warning: { wrap: 'bg-amber-50 text-amber-900 border-amber-200', Icon: AlertCircle },
};

/**
 * A message block.
 *
 * `role="alert"` on an error makes assistive technology announce it immediately, which matters
 * when a form submission fails and the message appears above the fold the user is not looking at.
 */
export function Alert({ type = 'info', title, children, className = '' }) {
  const { wrap, Icon } = ALERT_STYLES[type] ?? ALERT_STYLES.info;
  return (
    <div
      role={type === 'error' ? 'alert' : 'status'}
      className={`flex items-start gap-3 rounded-xl border p-3.5 text-sm ${wrap} ${className}`}
    >
      <Icon className="mt-0.5 h-5 w-5 shrink-0" aria-hidden="true" />
      <div>
        {title && <p className="font-semibold">{title}</p>}
        {children && <div className={title ? 'mt-0.5' : ''}>{children}</div>}
      </div>
    </div>
  );
}

/**
 * The empty state.
 *
 * An empty list should explain WHY it is empty and offer the next action. "No bookings yet -
 * get a quote" is useful; a blank rectangle makes the user think the page is broken.
 */
export function EmptyState({ title, description, action, icon: Icon = PackageOpen }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-14 text-center">
      <Icon className="h-10 w-10 text-slate-300" aria-hidden="true" />
      <h3 className="mt-4 text-base font-semibold text-slate-800">{title}</h3>
      {description && <p className="mt-1 max-w-sm text-sm text-slate-500">{description}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}

/**
 * Skeleton placeholder.
 *
 * Preferred over a spinner for content whose shape is known: the layout does not shift when the
 * real data arrives, which is both less jarring and better for Cumulative Layout Shift.
 */
export function Skeleton({ className = 'h-4 w-full' }) {
  return <div className={`animate-pulse rounded-md bg-slate-200 ${className}`} aria-hidden="true" />;
}
