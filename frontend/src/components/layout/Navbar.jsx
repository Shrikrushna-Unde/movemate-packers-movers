import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { LayoutDashboard, LogOut, Menu, Phone, Truck, X } from 'lucide-react';

import { useAuth } from '../../context/AuthContext.jsx';
import { CONTACT_PHONE } from '../../utils/constants.js';
import Button, { buttonClasses } from '../ui/Button.jsx';

/** Routes shown in the main nav. Data-driven, so adding a page is one line. */
const LINKS = [
  { to: '/', label: 'Home' },
  { to: '/services', label: 'Services' },
  { to: '/quote', label: 'Get a Quote' },
  { to: '/track', label: 'Track Booking' },
  { to: '/about', label: 'About' },
  { to: '/contact', label: 'Contact' },
];

export default function Navbar() {
  const [open, setOpen] = useState(false);
  const { isAuthenticated, isAdmin, user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    setOpen(false);
    logout();
  };

  /**
   * NavLink (not Link) gives us an `isActive` flag, so the current page is highlighted
   * automatically. Doing this by comparing useLocation().pathname by hand is a common
   * reinvention of something the router already provides.
   */
  const navLinkClass = ({ isActive }) =>
    `rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
      isActive ? 'bg-brand-50 text-brand-800' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
    }`;

  return (
    // sticky + z-40 keeps the quote CTA reachable while the user scrolls a long page.
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur">
      <div className="container-page flex h-16 items-center justify-between gap-4">
        {/* Brand */}
        <Link to="/" className="flex shrink-0 items-center gap-2" onClick={() => setOpen(false)}>
          <span className="grid h-9 w-9 place-items-center rounded-xl bg-brand-900 text-accent-400">
            <Truck className="h-5 w-5" aria-hidden="true" />
          </span>
          <span className="text-lg font-extrabold tracking-tight text-brand-950">
            Move<span className="text-accent-500">Mate</span>
          </span>
        </Link>

        {/* Desktop navigation. `hidden lg:flex` is mobile-first: hidden by default, shown from
            the lg breakpoint up. Designing for the small screen first is why the mobile layout
            here is not an afterthought. */}
        <nav className="hidden items-center gap-1 lg:flex">
          {LINKS.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              // `end` stops "/" from matching every route - without it, Home would be
              // highlighted on every page, because every path starts with "/".
              end={link.to === '/'}
              className={navLinkClass}
            >
              {link.label}
            </NavLink>
          ))}
        </nav>

        {/* Desktop actions */}
        <div className="hidden items-center gap-2 lg:flex">
          <a
            href={`tel:${CONTACT_PHONE}`}
            className="flex items-center gap-1.5 text-sm font-semibold text-slate-700 hover:text-brand-700"
          >
            <Phone className="h-4 w-4" aria-hidden="true" />
            {CONTACT_PHONE}
          </a>

          {isAuthenticated ? (
            <>
              <Link
                to={isAdmin ? '/admin' : '/dashboard'}
                className={buttonClasses({ variant: 'outline', size: 'sm' })}
              >
                <LayoutDashboard className="h-4 w-4" aria-hidden="true" />
                {isAdmin ? 'Admin' : 'My bookings'}
              </Link>
              <Button variant="ghost" size="sm" onClick={handleLogout} title={user?.email}>
                <LogOut className="h-4 w-4" aria-hidden="true" />
                Sign out
              </Button>
            </>
          ) : (
            <>
              <Link to="/login" className={buttonClasses({ variant: 'ghost', size: 'sm' })}>
                Sign in
              </Link>
              <Button variant="accent" size="sm" onClick={() => navigate('/quote')}>
                Get a free quote
              </Button>
            </>
          )}
        </div>

        {/* Mobile menu toggle */}
        <button
          type="button"
          className="rounded-lg p-2 text-slate-600 hover:bg-slate-100 lg:hidden"
          onClick={() => setOpen((v) => !v)}
          // These two attributes are what make a hamburger menu accessible: the label names
          // the control, aria-expanded reports its state.
          aria-label={open ? 'Close menu' : 'Open menu'}
          aria-expanded={open}
        >
          {open ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </button>
      </div>

      {/* Mobile drawer: rendered only when open, so its links are not reachable by keyboard
          while hidden (a problem with the `display: none` approach). */}
      {open && (
        <div className="border-t border-slate-200 bg-white lg:hidden">
          <nav className="container-page flex flex-col gap-1 py-3">
            {LINKS.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.to === '/'}
                className={navLinkClass}
                onClick={() => setOpen(false)}
              >
                {link.label}
              </NavLink>
            ))}

            <div className="mt-2 border-t border-slate-200 pt-3">
              {isAuthenticated ? (
                <div className="flex flex-col gap-2">
                  <Link
                    to={isAdmin ? '/admin' : '/dashboard'}
                    className={buttonClasses({ variant: 'outline', fullWidth: true })}
                    onClick={() => setOpen(false)}
                  >
                    {isAdmin ? 'Admin panel' : 'My bookings'}
                  </Link>
                  <Button variant="ghost" fullWidth onClick={handleLogout}>
                    Sign out
                  </Button>
                </div>
              ) : (
                <div className="flex flex-col gap-2">
                  <Link
                    to="/quote"
                    className={buttonClasses({ variant: 'accent', fullWidth: true })}
                    onClick={() => setOpen(false)}
                  >
                    Get a free quote
                  </Link>
                  <Link
                    to="/login"
                    className={buttonClasses({ variant: 'outline', fullWidth: true })}
                    onClick={() => setOpen(false)}
                  >
                    Sign in
                  </Link>
                </div>
              )}
            </div>
          </nav>
        </div>
      )}
    </header>
  );
}
