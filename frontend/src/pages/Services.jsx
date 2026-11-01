import ServicesGrid from '../components/home/ServicesGrid.jsx';
import CtaBand from '../components/home/CtaBand.jsx';
import HowItWorks from '../components/home/HowItWorks.jsx';

/**
 * The services listing page.
 *
 * Reuses ServicesGrid with no `limit` prop, so the same component that renders three cards on
 * the homepage renders all of them here. One component, two contexts - the alternative would
 * be a near-duplicate "AllServicesGrid" that drifts out of sync the first time the card design
 * changes.
 */
export default function Services() {
  return (
    <>
      <div className="border-b border-slate-200 bg-white">
        <div className="container-page py-12">
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 sm:text-4xl">
            Our moving services
          </h1>
          <p className="mt-3 max-w-2xl text-lg text-slate-600">
            Six services, each priced on its own cost drivers. Pick one, or get a single quote
            that combines moving and storage.
          </p>
        </div>
      </div>

      <ServicesGrid />
      <HowItWorks />
      <CtaBand />
    </>
  );
}
