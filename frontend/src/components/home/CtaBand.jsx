import { Link } from 'react-router-dom';
import { ArrowRight, Phone } from 'lucide-react';

import { buttonClasses } from '../ui/Button.jsx';
import { CONTACT_PHONE } from '../../utils/constants.js';

/**
 * The closing call to action.
 *
 * A visitor who has scrolled this far is interested but has not clicked yet, so this repeats
 * the primary action without making them scroll back to the hero. Two routes are offered
 * because moving is a high-anxiety purchase - some people want a form, others want a phone
 * number, and forcing either one costs conversions.
 */
export default function CtaBand() {
  return (
    <section className="container-page py-16">
      <div className="overflow-hidden rounded-3xl bg-brand-900 px-6 py-12 text-center sm:px-12">
        <h2 className="text-2xl font-bold tracking-tight text-white sm:text-3xl">
          Get your itemised moving quote in under a minute
        </h2>
        <p className="mx-auto mt-3 max-w-xl text-slate-300">
          See your price before you sign up, with no obligation and no sales call unless you ask. The price
          breakdown is yours to compare.
        </p>

        <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
          <Link to="/quote" className={buttonClasses({ variant: 'accent', size: 'lg' })}>
            Calculate my moving cost
            <ArrowRight className="h-4 w-4" aria-hidden="true" />
          </Link>
          <a
            href={`tel:${CONTACT_PHONE}`}
            className="inline-flex items-center gap-2 rounded-xl border border-white/20 px-6 py-3 text-base font-medium text-white transition-colors hover:bg-white/10"
          >
            <Phone className="h-4 w-4" aria-hidden="true" />
            Talk to a coordinator
          </a>
        </div>
      </div>
    </section>
  );
}
