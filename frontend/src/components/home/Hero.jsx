import { Link } from 'react-router-dom';
import { ArrowRight, BadgeCheck, Clock, ShieldCheck } from 'lucide-react';

import { buttonClasses } from '../ui/Button.jsx';
import { COMPANY_STATS } from '../../utils/constants.js';

/**
 * The hero section.
 *
 * Structured around one conversion goal: get the visitor to the quote form. There is a single
 * primary action ("Get my instant quote") in the accent colour, and one quiet secondary action.
 * Two equally loud buttons would split attention and lower the click-through on both.
 */
export default function Hero() {
  return (
    <section className="relative overflow-hidden bg-brand-950">
      {/*
        Decorative background. Pure CSS gradients instead of an image: no extra network
        request, no layout shift while it loads, and it stays sharp on every screen density.
      */}
      <div
        className="pointer-events-none absolute inset-0 opacity-40"
        style={{
          background:
            'radial-gradient(60% 60% at 15% 20%, #1b40f5 0%, transparent 60%), radial-gradient(50% 50% at 85% 10%, #f59e0b33 0%, transparent 65%)',
        }}
        aria-hidden="true"
      />

      <div className="container-page relative py-16 sm:py-24">
        <div className="grid items-center gap-12 lg:grid-cols-2">
          {/* Copy */}
          <div className="animate-fade-up">
            <span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-xs font-semibold text-accent-300 ring-1 ring-inset ring-white/15">
              <BadgeCheck className="h-3.5 w-3.5" aria-hidden="true" />
              Itemised pricing · No hidden charges
            </span>

            {/*
              Exactly one <h1> per page, and it contains the primary keyword. Both matter for
              SEO and for screen-reader users who navigate by heading.
            */}
            <h1 className="mt-5 text-4xl font-extrabold leading-[1.1] tracking-tight text-white sm:text-5xl">
              Moving home?
              <span className="block text-accent-400">Know the price before you call.</span>
            </h1>

            <p className="mt-5 max-w-xl text-lg leading-relaxed text-slate-300">
              Trained in-house crews, GPS-tracked closed-body trucks and optional transit
              insurance. Fill in four details and get a full cost breakdown in seconds - base
              charge, distance, packing, insurance and GST, all shown separately.
            </p>

            <div className="mt-8 flex flex-wrap items-center gap-3">
              <Link to="/quote" className={buttonClasses({ variant: 'accent', size: 'lg' })}>
                Get my instant quote
                <ArrowRight className="h-4 w-4" aria-hidden="true" />
              </Link>
              <Link
                to="/track"
                className="inline-flex items-center gap-2 rounded-xl border border-white/20 px-6 py-3 text-base font-medium text-white transition-colors hover:bg-white/10"
              >
                Track an existing booking
              </Link>
            </div>

            <div className="mt-8 flex flex-wrap gap-x-6 gap-y-3 text-sm text-slate-300">
              <span className="flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-accent-400" aria-hidden="true" />
                Transit insurance available
              </span>
              <span className="flex items-center gap-2">
                <Clock className="h-4 w-4 text-accent-400" aria-hidden="true" />
                Confirmation call within 2 hours
              </span>
            </div>
          </div>

          {/* Stats panel */}
          <div className="animate-fade-up rounded-3xl border border-white/10 bg-white/5 p-6 backdrop-blur sm:p-8">
            <p className="text-sm font-semibold uppercase tracking-wider text-accent-400">
              Why customers choose MoveMate
            </p>
            <dl className="mt-6 grid grid-cols-2 gap-6">
              {COMPANY_STATS.map((stat) => (
                // <dl>/<dt>/<dd> is the semantically correct element set for label-value
                // pairs. A grid of divs would look identical and mean nothing to assistive tech.
                <div key={stat.label}>
                  <dd className="text-3xl font-extrabold tracking-tight text-white">{stat.value}</dd>
                  <dt className="mt-1 text-sm text-slate-400">{stat.label}</dt>
                </div>
              ))}
            </dl>

            <div className="mt-8 rounded-2xl bg-brand-900/70 p-5 ring-1 ring-inset ring-white/10">
              <p className="text-sm leading-relaxed text-slate-300">
                <span className="font-semibold text-white">How the quote works:</span> our pricing
                engine starts from your home size and move type, adds only the distance beyond the
                first 25 km, then packing, staircase access, insurance and storage - and shows GST
                as its own line.
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
