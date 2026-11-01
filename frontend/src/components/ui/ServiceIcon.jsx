import { Building2, Car, Home, Package, Truck, Warehouse } from 'lucide-react';

/**
 * Renders the right icon for a catalogue entry.
 *
 * The backend stores a plain string ("home", "truck"). This component owns the mapping from
 * that string to a React component, which keeps the API free of frontend concerns.
 *
 * The icons are imported BY NAME rather than with `import * as Icons from 'lucide-react'`.
 * The namespace import defeats tree-shaking and pulls the entire icon library - over a
 * thousand components - into the bundle. Naming the six we use keeps it to six. This is a
 * genuinely common and invisible bundle-size mistake.
 */
const ICON_MAP = {
  home: Home,
  building: Building2,
  car: Car,
  truck: Truck,
  box: Package,
  warehouse: Warehouse,
};

export default function ServiceIcon({ name, className = 'h-6 w-6' }) {
  // Fallback for an icon key the frontend does not know yet, so adding a service on the
  // backend can never blank out the page.
  const Icon = ICON_MAP[name] ?? Package;
  return <Icon className={className} aria-hidden="true" />;
}
