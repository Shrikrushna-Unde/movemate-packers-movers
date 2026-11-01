import { useState } from 'react';
import { Boxes, LayoutDashboard, Mail, MessageSquareQuote, Truck } from 'lucide-react';

import AdminOverview from '../components/admin/AdminOverview.jsx';
import AdminBookings from '../components/admin/AdminBookings.jsx';
import AdminEnquiries from '../components/admin/AdminEnquiries.jsx';
import AdminReviews from '../components/admin/AdminReviews.jsx';
import AdminServices from '../components/admin/AdminServices.jsx';
import { useAuth } from '../context/AuthContext.jsx';

/**
 * The admin panel.
 *
 * Tabs rather than five nested routes, deliberately: the five sections are peers that an
 * operator switches between constantly, nobody needs to deep-link to "the enquiries tab", and
 * this keeps the route table in App.jsx readable. The tradeoff is an honest one - if a URL per
 * tab were needed (to share a link, or to survive a refresh on the right tab), nested routes
 * with an <Outlet/> would be the correct structure instead.
 *
 * Each tab's component fetches its own data, so opening the panel does not load all five
 * datasets at once. The component for an unvisited tab never mounts, so its request never fires.
 */
const TABS = [
  { id: 'overview', label: 'Dashboard', icon: LayoutDashboard, Component: AdminOverview },
  { id: 'bookings', label: 'Bookings', icon: Truck, Component: AdminBookings },
  { id: 'enquiries', label: 'Enquiries', icon: Mail, Component: AdminEnquiries },
  { id: 'reviews', label: 'Reviews', icon: MessageSquareQuote, Component: AdminReviews },
  { id: 'services', label: 'Services', icon: Boxes, Component: AdminServices },
];

export default function Admin() {
  const [active, setActive] = useState('overview');
  const { user } = useAuth();

  // find() cannot return undefined here because `active` only ever holds a known tab id,
  // but the fallback costs nothing and prevents a blank panel if that ever changes.
  const activeTab = TABS.find((tab) => tab.id === active) ?? TABS[0];
  const ActiveComponent = activeTab.Component;

  return (
    <div className="container-page py-10">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
            Operations panel
          </h1>
          <p className="mt-1.5 text-slate-600">
            Signed in as {user?.email} · {user?.roleLabel}
          </p>
        </div>
      </div>

      {/*
        role="tablist" with aria-selected is what makes this an accessible tab control rather
        than five buttons that happen to look like tabs. Screen readers announce "tab 2 of 5,
        Bookings, selected".
      */}
      <div
        role="tablist"
        aria-label="Admin sections"
        className="mt-7 flex gap-1 overflow-x-auto border-b border-slate-200 pb-px"
      >
        {TABS.map((tab) => {
          const isActive = tab.id === active;
          return (
            <button
              key={tab.id}
              type="button"
              role="tab"
              aria-selected={isActive}
              onClick={() => setActive(tab.id)}
              className={`flex shrink-0 items-center gap-2 border-b-2 px-4 py-2.5 text-sm font-medium transition-colors ${
                isActive
                  ? 'border-brand-700 text-brand-800'
                  : 'border-transparent text-slate-500 hover:border-slate-300 hover:text-slate-800'
              }`}
            >
              <tab.icon className="h-4 w-4" aria-hidden="true" />
              {tab.label}
            </button>
          );
        })}
      </div>

      <div role="tabpanel" className="mt-8">
        {/*
          Rendering <ActiveComponent /> rather than a chain of ternaries keeps this file flat,
          and the `key` forces a REMOUNT when the tab changes. That matters: without it React
          would try to reuse the previous component's state across two unrelated screens.
        */}
        <ActiveComponent key={activeTab.id} />
      </div>
    </div>
  );
}
