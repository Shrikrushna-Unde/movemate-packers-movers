import { useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { MessageCircle } from 'lucide-react';

import Navbar from './Navbar.jsx';
import Footer from './Footer.jsx';
import { WHATSAPP_URL } from '../../utils/constants.js';

/**
 * The shared page shell: navbar, page content, footer.
 *
 * `<Outlet />` is where react-router renders whichever child route matched. Using a layout
 * route this way means the navbar and footer are mounted ONCE and never re-render on
 * navigation - unlike importing them into every page, which remounts them on each route change
 * and would reset the mobile menu state and re-trigger any animations.
 */
export default function Layout() {
  const { pathname } = useLocation();

  /**
   * Scroll to the top on every navigation.
   *
   * A single-page app does not reload the document, so the browser keeps the previous scroll
   * position - click a footer link and you land halfway down the new page. Every SPA needs
   * this, and almost every tutorial app forgets it.
   */
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'instant' });
  }, [pathname]); // re-runs whenever the path changes

  return (
    // min-h-screen + flex-col + flex-1 on the main is the standard "sticky footer" layout:
    // the footer sits at the bottom of the viewport even when the page is short.
    <div className="flex min-h-screen flex-col">
      <Navbar />

      <main className="flex-1">
        <Outlet />
      </main>

      <Footer />

      {/* Floating WhatsApp button - the default contact channel for this industry in India. */}
      <a
        href={WHATSAPP_URL}
        target="_blank"
        rel="noopener noreferrer"
        aria-label="Chat with us on WhatsApp"
        className="fixed bottom-5 right-5 z-30 grid place-items-center rounded-full bg-emerald-500 p-3.5 text-white shadow-lift transition-transform hover:scale-105"
      >
        <MessageCircle className="h-6 w-6" aria-hidden="true" />
      </a>
    </div>
  );
}
