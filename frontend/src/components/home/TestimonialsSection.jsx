import { Quote } from 'lucide-react';

import { Card, SectionHeading, Stars } from '../ui/Card.jsx';
import { Skeleton } from '../ui/Feedback.jsx';
import { testimonialApi } from '../../api/endpoints.js';
import { useApiData } from '../../hooks/useApiData.js';

/**
 * Approved customer reviews from GET /testimonials.
 *
 * Note the failure strategy: if the request fails, the whole section renders nothing. A
 * missing testimonial block is invisible to the visitor, whereas a red error banner on the
 * homepage damages trust far more than the absent reviews would. Compare that with
 * {@link ServicesGrid}, where the services ARE the page and an error must be surfaced. Error
 * handling is a product decision, not a single rule applied everywhere.
 */
export default function TestimonialsSection({ limit = 6 }) {
  const { data: testimonials, loading, error } = useApiData(() => testimonialApi.approved());

  if (error) return null;
  if (!loading && (!testimonials || testimonials.length === 0)) return null;

  return (
    <section className="bg-white py-16 sm:py-20">
      <div className="container-page">
        <SectionHeading
          eyebrow="Customer reviews"
          title="What people say after the truck leaves"
          subtitle="Every review here was submitted through the site by a customer with a booking, and approved by our team before going live."
        />

        <div className="mt-12 grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {loading &&
            Array.from({ length: 3 }).map((_, index) => (
              <Card key={index} className="p-6">
                <Skeleton className="h-4 w-24" />
                <Skeleton className="mt-4 h-4 w-full" />
                <Skeleton className="mt-2 h-4 w-full" />
                <Skeleton className="mt-2 h-4 w-2/3" />
              </Card>
            ))}

          {testimonials?.slice(0, limit).map((item) => (
            // <figure> + <blockquote> + <figcaption> is the semantically correct structure for
            // a quotation with an attribution.
            <Card as="figure" key={item.id} className="flex flex-col p-6">
              <Quote className="h-7 w-7 text-brand-200" aria-hidden="true" />
              <Stars rating={item.rating} className="mt-4 text-sm" />
              <blockquote className="mt-3 flex-1 text-sm leading-relaxed text-slate-700">
                “{item.message}”
              </blockquote>
              <figcaption className="mt-5 border-t border-slate-100 pt-4 text-sm">
                <span className="font-semibold text-slate-900">{item.authorName}</span>
                <span className="text-slate-500"> · {item.city}</span>
              </figcaption>
            </Card>
          ))}
        </div>
      </div>
    </section>
  );
}
