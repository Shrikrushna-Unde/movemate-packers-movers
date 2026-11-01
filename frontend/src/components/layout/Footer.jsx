import { Link } from 'react-router-dom';
import { Mail, MapPin, Phone, Truck } from 'lucide-react';

import { CONTACT_EMAIL, CONTACT_PHONE, SERVICE_CITIES } from '../../utils/constants.js';

const SERVICE_LINKS = [
  { to: '/services/home-shifting', label: 'Home Shifting' },
  { to: '/services/office-relocation', label: 'Office Relocation' },
  { to: '/services/vehicle-transport', label: 'Car & Bike Transport' },
  { to: '/services/domestic-relocation', label: 'Domestic Relocation' },
  { to: '/services/packing-unpacking', label: 'Packing & Unpacking' },
  { to: '/services/warehousing-storage', label: 'Warehousing & Storage' },
];

export default function Footer() {
  /*
   * Computed at render, not hardcoded, so the copyright year never goes stale. A tiny detail,
   * but a site still showing 2024 tells every visitor nobody is maintaining it.
   */
  const year = new Date().getFullYear();

  return (
    <footer className="mt-20 bg-brand-950 text-slate-300">
      <div className="container-page py-14">
        <div className="grid gap-10 md:grid-cols-2 lg:grid-cols-4">
          {/* Brand + pitch */}
          <div>
            <div className="flex items-center gap-2">
              <span className="grid h-9 w-9 place-items-center rounded-xl bg-white/10 text-accent-400">
                <Truck className="h-5 w-5" aria-hidden="true" />
              </span>
              <span className="text-lg font-extrabold tracking-tight text-white">
                Move<span className="text-accent-400">Mate</span>
              </span>
            </div>
            <p className="mt-4 text-sm leading-relaxed text-slate-400">
              Professional packers and movers with transparent, itemised pricing. Home shifting,
              office relocation, vehicle transport and warehousing across 28 Indian cities.
            </p>
          </div>

          {/* Services */}
          <div>
            <h3 className="text-sm font-semibold uppercase tracking-wider text-white">Services</h3>
            <ul className="mt-4 space-y-2.5 text-sm">
              {SERVICE_LINKS.map((link) => (
                <li key={link.to}>
                  <Link to={link.to} className="text-slate-400 transition-colors hover:text-accent-400">
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          {/* Company */}
          <div>
            <h3 className="text-sm font-semibold uppercase tracking-wider text-white">Company</h3>
            <ul className="mt-4 space-y-2.5 text-sm">
              <li>
                <Link to="/about" className="text-slate-400 hover:text-accent-400">
                  About us
                </Link>
              </li>
              <li>
                <Link to="/quote" className="text-slate-400 hover:text-accent-400">
                  Get a quote
                </Link>
              </li>
              <li>
                <Link to="/track" className="text-slate-400 hover:text-accent-400">
                  Track your booking
                </Link>
              </li>
              <li>
                <Link to="/contact" className="text-slate-400 hover:text-accent-400">
                  Contact
                </Link>
              </li>
              <li>
                {/*
                  Link to the live API docs. Having this on a portfolio site is a deliberate
                  move: a reviewer can see the backend is real without cloning anything.
                */}
                <a
                  href={`${import.meta.env.VITE_API_BASE_URL?.replace('/api/v1', '') || ''}/swagger-ui.html`}
                  target="_blank"
                  // noreferrer alongside noopener: the first stops the new tab from reaching
                  // back through window.opener, the second withholds the referrer header.
                  rel="noopener noreferrer"
                  className="text-slate-400 hover:text-accent-400"
                >
                  API documentation
                </a>
              </li>
            </ul>
          </div>

          {/* Contact */}
          <div>
            <h3 className="text-sm font-semibold uppercase tracking-wider text-white">Get in touch</h3>
            <ul className="mt-4 space-y-3 text-sm">
              <li className="flex items-start gap-2.5">
                <Phone className="mt-0.5 h-4 w-4 shrink-0 text-accent-400" aria-hidden="true" />
                {/* tel: and mailto: open the phone dialler and mail app on mobile - far better
                    than printing a number the user has to copy by hand. */}
                <a href={`tel:${CONTACT_PHONE}`} className="hover:text-accent-400">
                  {CONTACT_PHONE}
                </a>
              </li>
              <li className="flex items-start gap-2.5">
                <Mail className="mt-0.5 h-4 w-4 shrink-0 text-accent-400" aria-hidden="true" />
                <a href={`mailto:${CONTACT_EMAIL}`} className="hover:text-accent-400">
                  {CONTACT_EMAIL}
                </a>
              </li>
              <li className="flex items-start gap-2.5">
                <MapPin className="mt-0.5 h-4 w-4 shrink-0 text-accent-400" aria-hidden="true" />
                <span className="text-slate-400">
                  Office 204, Baner Road, Pune, Maharashtra 411045
                </span>
              </li>
            </ul>
          </div>
        </div>

        {/* Cities: real SEO value for a local service business. */}
        <div className="mt-12 border-t border-white/10 pt-8">
          <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400">
            Cities we serve
          </h3>
          <p className="mt-3 text-sm text-slate-500">{SERVICE_CITIES.join('  ·  ')}</p>
        </div>

        <div className="mt-8 flex flex-col items-start justify-between gap-3 border-t border-white/10 pt-6 text-xs text-slate-500 sm:flex-row sm:items-center">
          <p>© {year} MoveMate Packers & Movers. All rights reserved.</p>
          <p>
            Built with Java 17, Spring Boot 3, MySQL and React. A portfolio project - not a
            trading company.
          </p>
        </div>
      </div>
    </footer>
  );
}
