import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';

import { Card, SectionHeading } from '../ui/Card.jsx';
import { Alert, Skeleton } from '../ui/Feedback.jsx';
import ServiceIcon from '../ui/ServiceIcon.jsx';
import { catalogApi } from '../../api/endpoints.js';
import { useApiData } from '../../hooks/useApiData.js';
import { formatCurrency } from '../../utils/format.js';

/**
 * The service cards, loaded from GET /catalog/services.
 *
 * This is the first component that actually talks to the backend, and it demonstrates the
 * pattern every data component in the app follows: render a skeleton while loading, an alert
 * on failure, and the content on success. Never assume the happy path.
 */
export default function ServicesGrid({ limit }) {
  const { data: services, loading, error } = useApiData(() => catalogApi.services());

  // `limit` lets the homepage show three cards and the services page show all of them, from
  // one component. `?.` guards against `services` still being null on the first render.
  const visible = limit ? services?.slice(0, limit) : services;

  return (
    <section className="container-page py-16 sm:py-20">
      <SectionHeading
        eyebrow="What we move"
        title="Services built around how people actually move"
        subtitle="From a 1 RK across town to a 40-desk office across states, each service is priced on its own cost drivers rather than a flat rate."
      />

      {error && (
        <Alert type="error" title="Could not load our services" className="mt-8">
          {error} Please refresh the page, or call us and we will quote over the phone.
        </Alert>
      )}

      <div className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {loading &&
          /*
           * Array.from({ length: 3 }) is the clean way to render a fixed number of
           * placeholders. The skeletons match the real card's shape, so nothing jumps when the
           * data arrives.
           */
          Array.from({ length: limit ?? 6 }).map((_, index) => (
            <Card key={index} className="p-6">
              <Skeleton className="h-11 w-11 rounded-xl" />
              <Skeleton className="mt-5 h-5 w-2/3" />
              <Skeleton className="mt-3 h-4 w-full" />
              <Skeleton className="mt-2 h-4 w-4/5" />
            </Card>
          ))}

        {visible?.map((service) => (
          /*
           * The key is the database id - stable, unique, and unchanged when the list is
           * reordered. React uses keys to decide which DOM nodes to reuse; an array index here
           * would make React reuse the wrong card after a sort.
           */
          <Card
            key={service.id}
            className="group flex flex-col p-6 transition-shadow hover:shadow-lift"
          >
            <span className="grid h-11 w-11 place-items-center rounded-xl bg-brand-50 text-brand-700">
              <ServiceIcon name={service.icon} />
            </span>

            <h3 className="mt-5 text-lg font-semibold text-slate-900">{service.title}</h3>
            <p className="mt-2 flex-1 text-sm leading-relaxed text-slate-600">
              {service.shortDescription}
            </p>

            <div className="mt-5 flex items-center justify-between border-t border-slate-100 pt-4">
              <span className="text-sm text-slate-500">
                Starting at{' '}
                <span className="font-bold text-slate-900">
                  {formatCurrency(service.startingPrice)}
                </span>
              </span>
              <Link
                to={`/services/${service.slug}`}
                className="inline-flex items-center gap-1 text-sm font-semibold text-brand-700 hover:text-brand-900"
              >
                Details
                {/* group-hover on the parent animates the arrow on card hover - a cheap touch
                    of polish that makes the card feel clickable. */}
                <ArrowRight
                  className="h-4 w-4 transition-transform group-hover:translate-x-0.5"
                  aria-hidden="true"
                />
              </Link>
            </div>
          </Card>
        ))}
      </div>

      {limit && services?.length > limit && (
        <div className="mt-10 text-center">
          <Link
            to="/services"
            className="inline-flex items-center gap-2 text-sm font-semibold text-brand-700 hover:text-brand-900"
          >
            View all {services.length} services
            <ArrowRight className="h-4 w-4" aria-hidden="true" />
          </Link>
        </div>
      )}
    </section>
  );
}
