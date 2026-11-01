import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, ArrowRight, CheckCircle2, MessageCircle } from 'lucide-react';

import { Card } from '../components/ui/Card.jsx';
import { Alert, Skeleton } from '../components/ui/Feedback.jsx';
import ServiceIcon from '../components/ui/ServiceIcon.jsx';
import { buttonClasses } from '../components/ui/Button.jsx';
import TestimonialsSection from '../components/home/TestimonialsSection.jsx';
import { catalogApi } from '../api/endpoints.js';
import { useApiData } from '../hooks/useApiData.js';
import { formatCurrency } from '../utils/format.js';
import { whatsappUrl } from '../utils/constants.js';

/** What is included, shown on every service page. Marketing copy, so it lives in the frontend. */
const INCLUSIONS = [
  'Dedicated move coordinator from survey to delivery',
  'Multi-layer packing material included in the quoted price',
  'Furniture dismantling and reassembly at the destination',
  'Closed-body, GPS-tracked vehicle with a named driver',
  'Photo inventory before loading, shared with you',
  'Itemised invoice matching the online quote',
];

export default function ServiceDetail() {
  /**
   * useParams reads the dynamic segment declared in App.jsx as `services/:slug`.
   * The key name must match the route exactly - a mismatch gives you undefined, not an error,
   * which is why it is worth checking both places when a detail page 404s.
   */
  const { slug } = useParams();

  /*
   * [slug] as the dependency: navigating from /services/home-shifting to
   * /services/office-relocation keeps this component MOUNTED (same route, different param),
   * so without the dependency the effect would not re-run and the page would keep showing the
   * previous service. A genuinely common SPA bug.
   */
  const { data: service, loading, error } = useApiData(() => catalogApi.service(slug), [slug]);

  if (loading) {
    return (
      <div className="container-page max-w-4xl py-14">
        <Skeleton className="h-6 w-32" />
        <Skeleton className="mt-6 h-10 w-2/3" />
        <Skeleton className="mt-4 h-4 w-full" />
        <Skeleton className="mt-2 h-4 w-5/6" />
        <Skeleton className="mt-8 h-48 w-full rounded-2xl" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="container-page max-w-2xl py-20">
        <Alert type="error" title="Service not found">
          {error}
        </Alert>
        <Link to="/services" className={`${buttonClasses({ variant: 'outline' })} mt-6`}>
          <ArrowLeft className="h-4 w-4" aria-hidden="true" />
          Back to all services
        </Link>
      </div>
    );
  }

  return (
    <>
      <div className="border-b border-slate-200 bg-white">
        <div className="container-page py-12">
          <Link
            to="/services"
            className="inline-flex items-center gap-1.5 text-sm font-medium text-slate-500 hover:text-brand-700"
          >
            <ArrowLeft className="h-4 w-4" aria-hidden="true" />
            All services
          </Link>

          <div className="mt-6 flex items-start gap-4">
            <span className="grid h-14 w-14 shrink-0 place-items-center rounded-2xl bg-brand-900 text-accent-400">
              <ServiceIcon name={service.icon} className="h-7 w-7" />
            </span>
            <div>
              <h1 className="text-3xl font-extrabold tracking-tight text-slate-900">
                {service.title}
              </h1>
              <p className="mt-2 max-w-2xl text-lg text-slate-600">{service.shortDescription}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="container-page grid gap-8 py-12 lg:grid-cols-[1.5fr_1fr] lg:items-start">
        <div className="space-y-8">
          <Card className="p-6 sm:p-8">
            <h2 className="text-lg font-bold text-slate-900">How this service works</h2>
            {/* leading-relaxed and max-w on body copy: a line of text longer than roughly 75
                characters is measurably harder to read. */}
            <p className="mt-4 max-w-prose leading-relaxed text-slate-700">
              {service.description}
            </p>
          </Card>

          <Card className="p-6 sm:p-8">
            <h2 className="text-lg font-bold text-slate-900">What is included</h2>
            <ul className="mt-5 grid gap-3 sm:grid-cols-2">
              {INCLUSIONS.map((item) => (
                <li key={item} className="flex items-start gap-2.5 text-sm text-slate-700">
                  <CheckCircle2
                    className="mt-0.5 h-4 w-4 shrink-0 text-emerald-500"
                    aria-hidden="true"
                  />
                  {item}
                </li>
              ))}
            </ul>
          </Card>
        </div>

        {/* sticky top-24 keeps the pricing CTA on screen while the description is read. The
            top offset accounts for the 4rem sticky navbar plus breathing room. */}
        <Card className="p-6 lg:sticky lg:top-24">
          <p className="text-xs font-semibold uppercase tracking-wider text-brand-600">
            Starting from
          </p>
          <p className="mt-1 text-3xl font-extrabold tracking-tight text-slate-900">
            {formatCurrency(service.startingPrice)}
          </p>
          <p className="mt-2 text-sm text-slate-500">
            Final price depends on your home size, distance, floor access and the add-ons you
            choose. The quote engine shows every component separately.
          </p>

          <Link
            to="/quote"
            className={`${buttonClasses({ variant: 'accent', fullWidth: true })} mt-6`}
          >
            Get my exact quote
            <ArrowRight className="h-4 w-4" aria-hidden="true" />
          </Link>

          {/*
            A plain <a>, not a react-router <Link>: Link is for in-app routes, and using it
            for an external URL would try to match "https://wa.me/..." against our route table.
            target="_blank" with rel="noopener noreferrer" is the standard pairing - noopener
            stops the opened tab from reaching back into window.opener.
          */}
          <a
            href={whatsappUrl(`Hi MoveMate, I have a question about ${service.title}.`)}
            target="_blank"
            rel="noopener noreferrer"
            className={`${buttonClasses({ variant: 'outline', fullWidth: true })} mt-3`}
          >
            <MessageCircle className="h-4 w-4" aria-hidden="true" />
            Ask on WhatsApp
          </a>
        </Card>
      </div>

      <TestimonialsSection limit={3} />
    </>
  );
}
