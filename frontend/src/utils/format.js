/**
 * Formatting helpers.
 *
 * All of these use the built-in Intl API rather than a date/number library. Intl ships with
 * every browser, so this file adds zero kilobytes to the bundle - whereas moment.js alone is
 * about 70 kB. Knowing that Intl covers most formatting needs is a practical, bundle-size-aware
 * answer to "how do you format dates in React?".
 */

/**
 * Formats rupees in the Indian numbering system: 1,25,000 rather than 125,000.
 *
 * The grouping (lakh/crore) is handled by the 'en-IN' locale - this is exactly the kind of
 * thing it is a mistake to implement by hand with regex.
 *
 * maximumFractionDigits: 0 because a moving quote in paise looks unserious. The precise value
 * is still stored as DECIMAL in MySQL; this is a display choice only.
 */
export const formatCurrency = (amount) => {
  if (amount === null || amount === undefined || Number.isNaN(Number(amount))) return '—';
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(Number(amount));
};

/** Same thing with decimals, for a quote breakdown where the lines must visibly add up. */
export const formatCurrencyExact = (amount) => {
  if (amount === null || amount === undefined) return '—';
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
  }).format(Number(amount));
};

/**
 * "4 Oct 2026" from an ISO date string.
 *
 * The backend sends LocalDate as "2026-10-04". `new Date('2026-10-04')` parses it as UTC
 * midnight, which in a timezone behind UTC would display as 3 October - the classic
 * off-by-one-day bug. Splitting the string and using the (year, monthIndex, day) constructor
 * builds a LOCAL date, so the day is always the one the backend meant.
 */
export const formatDate = (isoDate) => {
  if (!isoDate) return '—';
  const [year, month, day] = isoDate.split('-').map(Number);
  const date = new Date(year, month - 1, day);
  return new Intl.DateTimeFormat('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(date);
};

/**
 * "4 Oct 2026, 2:30 pm" from an ISO instant.
 *
 * Instants arrive as "2026-10-04T09:00:00Z" and are safe to pass straight to `new Date`
 * because they carry an explicit zone. The browser renders them in the viewer's local time,
 * which is what you want for a "created at" column.
 */
export const formatDateTime = (isoInstant) => {
  if (!isoInstant) return '—';
  return new Intl.DateTimeFormat('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  }).format(new Date(isoInstant));
};

/** "2 days ago" / "in 3 days", for a human sense of recency in admin lists. */
export const formatRelative = (isoInstant) => {
  if (!isoInstant) return '—';
  const diffMs = new Date(isoInstant).getTime() - Date.now();
  const diffDays = Math.round(diffMs / 86_400_000);

  if (Math.abs(diffDays) >= 1) {
    return new Intl.RelativeTimeFormat('en', { numeric: 'auto' }).format(diffDays, 'day');
  }
  const diffHours = Math.round(diffMs / 3_600_000);
  if (Math.abs(diffHours) >= 1) {
    return new Intl.RelativeTimeFormat('en', { numeric: 'auto' }).format(diffHours, 'hour');
  }
  const diffMinutes = Math.round(diffMs / 60_000);
  return new Intl.RelativeTimeFormat('en', { numeric: 'auto' }).format(diffMinutes, 'minute');
};

/** Today in YYYY-MM-DD, for the `min` attribute on a date input so past dates are unpickable. */
export const todayIso = () => {
  const now = new Date();
  // Manual zero-padding rather than toISOString(), which converts to UTC and can return
  // yesterday's date for anyone east of Greenwich in the early morning.
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
};
