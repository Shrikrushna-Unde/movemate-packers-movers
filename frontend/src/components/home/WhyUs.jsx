import { CheckCircle2 } from 'lucide-react';

import { Card, SectionHeading } from '../ui/Card.jsx';
import { SERVICE_CITIES, WHY_US } from '../../utils/constants.js';

/** The differentiators block, plus the coverage list. */
export default function WhyUs() {
  return (
    <section className="container-page py-16 sm:py-20">
      <div className="grid gap-12 lg:grid-cols-[1fr_1.1fr] lg:items-start">
        <div>
          <SectionHeading
            align="left"
            eyebrow="Why MoveMate"
            title="The part of moving nobody wants to think about"
            subtitle="Most complaints in this industry come down to two things: a price that changes on delivery day, and crews who are not accountable. Both are design decisions, so we designed them out."
          />

          <div className="mt-8 rounded-2xl bg-brand-50 p-6 ring-1 ring-inset ring-brand-100">
            <h3 className="text-sm font-bold uppercase tracking-wider text-brand-800">
              Cities we operate in
            </h3>
            <ul className="mt-4 flex flex-wrap gap-2">
              {SERVICE_CITIES.map((city) => (
                <li
                  key={city}
                  className="rounded-full bg-white px-3 py-1 text-xs font-medium text-brand-900 ring-1 ring-inset ring-brand-100"
                >
                  {city}
                </li>
              ))}
            </ul>
          </div>
        </div>

        {/* A two-column card grid that collapses to one column on mobile. The `sm:` prefix is
            the breakpoint - Tailwind is mobile-first, so unprefixed classes are the small
            screen and prefixed ones apply from that width UP. */}
        <div className="grid gap-5 sm:grid-cols-2">
          {WHY_US.map((item) => (
            <Card key={item.title} className="p-6">
              <CheckCircle2 className="h-6 w-6 text-emerald-500" aria-hidden="true" />
              <h3 className="mt-4 text-base font-semibold text-slate-900">{item.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-slate-600">{item.body}</p>
            </Card>
          ))}
        </div>
      </div>
    </section>
  );
}
