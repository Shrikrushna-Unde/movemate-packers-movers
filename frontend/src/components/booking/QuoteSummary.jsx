import { Calculator, Info, Users } from 'lucide-react';

import { Card } from '../ui/Card.jsx';
import { Spinner } from '../ui/Feedback.jsx';
import { formatCurrency, formatCurrencyExact, formatDate } from '../../utils/format.js';

/**
 * Renders the itemised quote returned by POST /quotes/estimate.
 *
 * The whole point of this panel is that the customer can see HOW the number was reached. Each
 * line carries the backend's own `explanation` string - "125 km beyond the free 25 km at 21 per
 * km" - so the price is auditable rather than a mystery. In an industry known for surprise
 * charges at delivery, that transparency is the product.
 *
 * It is a pure presentational component: it takes data and renders it, with no fetching and no
 * state. That makes it reusable (the booking confirmation reuses it) and trivial to test.
 */
export default function QuoteSummary({ quote, loading, className = '' }) {
  // --- empty state: nothing calculated yet ---
  if (!quote && !loading) {
    return (
      <Card className={`p-6 ${className}`}>
        <div className="flex flex-col items-center py-8 text-center">
          <Calculator className="h-9 w-9 text-slate-300" aria-hidden="true" />
          <h3 className="mt-4 text-base font-semibold text-slate-800">Your estimate appears here</h3>
          <p className="mt-1.5 max-w-xs text-sm text-slate-500">
            Choose your move type, home size and distance and the price updates automatically.
          </p>
        </div>
      </Card>
    );
  }

  // --- loading state: keep the panel, swap the body, so the layout does not jump ---
  if (loading && !quote) {
    return (
      <Card className={`p-6 ${className}`}>
        <Spinner label="Calculating your estimate" />
      </Card>
    );
  }

  return (
    <Card className={`overflow-hidden ${className}`}>
      {/* Header: the one number the customer is looking for, shown first and largest. */}
      <div className="bg-brand-950 px-6 py-5 text-white">
        <p className="text-xs font-semibold uppercase tracking-wider text-accent-400">
          Estimated total
        </p>
        <p className="mt-1 text-3xl font-extrabold tracking-tight">{formatCurrency(quote.total)}</p>
        <p className="mt-1 text-xs text-slate-400">
          Inclusive of {Number(quote.gstPercent)}% GST · {quote.moveTypeLabel}
          {quote.homeSizeLabel ? ` · ${quote.homeSizeLabel}` : ''}
        </p>
      </div>

      <div className="px-6 py-5">
        {/* Line items */}
        <ul className="space-y-4">
          {quote.lineItems.map((item) => (
            <li key={item.code} className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <p className="text-sm font-medium text-slate-800">{item.label}</p>
                {/* The justification for the charge, straight from the pricing engine. */}
                <p className="mt-0.5 text-xs leading-relaxed text-slate-500">{item.explanation}</p>
              </div>
              {/* tabular-nums makes digits a fixed width, so the amounts line up in a column
                  instead of wobbling. whitespace-nowrap stops a price wrapping mid-number. */}
              <span className="shrink-0 whitespace-nowrap text-sm font-semibold tabular-nums text-slate-900">
                {formatCurrencyExact(item.amount)}
              </span>
            </li>
          ))}
        </ul>

        {/* Totals */}
        <dl className="mt-5 space-y-2 border-t border-slate-200 pt-4 text-sm">
          <div className="flex justify-between">
            <dt className="text-slate-600">Subtotal</dt>
            <dd className="font-medium tabular-nums text-slate-900">
              {formatCurrencyExact(quote.subtotal)}
            </dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-600">GST ({Number(quote.gstPercent)}%)</dt>
            <dd className="font-medium tabular-nums text-slate-900">
              {formatCurrencyExact(quote.gstAmount)}
            </dd>
          </div>
          <div className="flex justify-between border-t border-slate-200 pt-2 text-base">
            <dt className="font-bold text-slate-900">Total payable</dt>
            <dd className="font-extrabold tabular-nums text-brand-800">
              {formatCurrencyExact(quote.total)}
            </dd>
          </div>
        </dl>

        {/* Meta */}
        <div className="mt-5 space-y-2.5 rounded-xl bg-slate-50 p-4 text-xs text-slate-600">
          <p className="flex items-center gap-2">
            <Users className="h-3.5 w-3.5 shrink-0 text-brand-600" aria-hidden="true" />
            Crew of {quote.crewSize} assigned to this move
          </p>
          <p className="flex items-start gap-2">
            <Info className="mt-0.5 h-3.5 w-3.5 shrink-0 text-brand-600" aria-hidden="true" />
            <span>
              Quote valid until <strong>{formatDate(quote.validUntil)}</strong>. {quote.disclaimer}
            </span>
          </p>
        </div>
      </div>
    </Card>
  );
}
