import { Link } from 'react-router-dom';
import { Compass, Home, PackageSearch } from 'lucide-react';

import { buttonClasses } from '../components/ui/Button.jsx';

/**
 * The 404 page.
 *
 * A good 404 does two things: it says clearly that the page does not exist, and it offers the
 * routes the visitor most likely wanted. A bare "404" with no navigation is a dead end that
 * sends people back to Google.
 *
 * Worth knowing: this component only renders when the SPA router finds no matching route. For
 * a hard refresh on an unknown URL, the STATIC HOST must first rewrite the request to
 * index.html - otherwise the visitor gets the host's own 404 and React never loads. That is
 * what the rewrite rules in vercel.json / netlify.toml are for.
 */
export default function NotFound() {
  return (
    <div className="container-page flex min-h-[70vh] max-w-xl flex-col items-center justify-center py-16 text-center">
      <Compass className="h-12 w-12 text-brand-300" aria-hidden="true" />
      <p className="mt-6 text-sm font-bold uppercase tracking-widest text-brand-600">Error 404</p>
      <h1 className="mt-2 text-3xl font-extrabold tracking-tight text-slate-900">
        This page has moved out
      </h1>
      <p className="mt-3 text-slate-600">
        The link may be broken or the page may have been removed. Here is where most people are
        heading.
      </p>

      <div className="mt-8 flex flex-wrap justify-center gap-3">
        <Link to="/" className={buttonClasses({ variant: 'primary' })}>
          <Home className="h-4 w-4" aria-hidden="true" />
          Back to home
        </Link>
        <Link to="/quote" className={buttonClasses({ variant: 'accent' })}>
          Get a moving quote
        </Link>
        <Link to="/track" className={buttonClasses({ variant: 'outline' })}>
          <PackageSearch className="h-4 w-4" aria-hidden="true" />
          Track a booking
        </Link>
      </div>
    </div>
  );
}
