import { STATUS_STYLES } from '../../utils/constants.js';

/**
 * Small presentational building blocks.
 */

/** A white rounded panel. `as` lets it render a <section> or <li> without losing the styling. */
export function Card({ children, className = '', as: Tag = 'div', ...rest }) {
  return (
    <Tag className={`card ${className}`} {...rest}>
      {children}
    </Tag>
  );
}

/**
 * The eyebrow + heading + subheading block used above every section.
 *
 * Extracted because it appears nine times across the site. The rule of thumb: once a pattern
 * appears a third time, give it a name - but not before, or you end up with an abstraction
 * shaped by too few examples.
 */
export function SectionHeading({ eyebrow, title, subtitle, align = 'center', className = '' }) {
  const alignment = align === 'center' ? 'text-center mx-auto' : 'text-left';
  return (
    <div className={`max-w-2xl ${alignment} ${className}`}>
      {eyebrow && (
        <p className="text-xs font-bold uppercase tracking-[0.18em] text-brand-600">{eyebrow}</p>
      )}
      {/* h2, not a styled div: heading levels are how screen readers and search engines
          understand page structure. */}
      <h2 className="mt-2 text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">{title}</h2>
      {subtitle && <p className="mt-3 text-slate-600">{subtitle}</p>}
    </div>
  );
}

/**
 * A coloured status pill.
 *
 * The colour comes from the STATUS_STYLES lookup, with a neutral fallback - so a status added
 * on the backend renders in grey instead of crashing on `undefined.split`. Defensive lookups
 * are cheap; a white screen in production is not.
 */
export function StatusBadge({ status, label, className = '' }) {
  const style = STATUS_STYLES[status] ?? 'bg-slate-100 text-slate-700 ring-slate-200';
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ring-inset ${style} ${className}`}
    >
      {label ?? status}
    </span>
  );
}

/** A plain neutral tag, for things that are not statuses. */
export function Badge({ children, className = '' }) {
  return (
    <span
      className={`inline-flex items-center rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-700 ${className}`}
    >
      {children}
    </span>
  );
}

/** Star rating, rendered from text rather than images so it costs no requests. */
export function Stars({ rating, className = '' }) {
  return (
    // aria-label gives a screen reader the meaning; the stars themselves are decorative.
    <span className={`text-accent-500 ${className}`} aria-label={`${rating} out of 5 stars`}>
      <span aria-hidden="true">
        {'★'.repeat(rating)}
        <span className="text-slate-300">{'★'.repeat(5 - rating)}</span>
      </span>
    </span>
  );
}
