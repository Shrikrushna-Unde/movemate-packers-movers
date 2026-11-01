/**
 * Presentation-only constants.
 *
 * Note what is here and what is NOT. Move types, home sizes and booking statuses are NOT here:
 * those come from the backend via /catalog/options, because duplicating a Java enum in
 * JavaScript guarantees the two drift apart. What lives here is purely cosmetic - the colour
 * each status is painted, the icon each service uses - which the API has no business knowing.
 */

/** Tailwind classes per booking status. Keys match the backend enum names exactly. */
export const STATUS_STYLES = {
  PENDING: 'bg-amber-100 text-amber-800 ring-amber-200',
  CONFIRMED: 'bg-blue-100 text-blue-800 ring-blue-200',
  IN_TRANSIT: 'bg-indigo-100 text-indigo-800 ring-indigo-200',
  DELIVERED: 'bg-emerald-100 text-emerald-800 ring-emerald-200',
  CANCELLED: 'bg-rose-100 text-rose-800 ring-rose-200',
};

/*
 * Note: the icon-key to component mapping lives in components/ui/ServiceIcon.jsx rather than
 * here, because it must import the actual icon components by name for tree-shaking. Keeping a
 * second copy of the keys in this file would be dead code that silently goes out of date.
 */

/** Cities we advertise coverage in. Static marketing copy, not operational data. */
export const SERVICE_CITIES = [
  'Pune',
  'Mumbai',
  'Nagpur',
  'Nashik',
  'Bengaluru',
  'Hyderabad',
  'Delhi NCR',
  'Ahmedabad',
  'Chennai',
  'Kolkata',
  'Indore',
  'Jaipur',
];

/** Read from the environment so the number appears in one place, not fifteen. */
export const CONTACT_PHONE = import.meta.env.VITE_CONTACT_PHONE || '+918999115935';
export const CONTACT_EMAIL = import.meta.env.VITE_CONTACT_EMAIL || 'hello@movemate.in';

/**
 * Reduces the configured number to the bare digits wa.me needs, adding the country code when
 * the value is a plain 10-digit Indian mobile.
 *
 * This normalisation is the difference between a working button and a dead one. Handed a
 * number containing a `+`, a space, or no country code, wa.me does not raise an error - it
 * shows "Phone number shared via url is invalid" and the visitor gives up. Normalising here
 * means VITE_CONTACT_PHONE can be written as `+91 98765 43210`, `919876543210` or
 * `9876543210` and the link still resolves to the same chat.
 */
function toWhatsAppDigits(phone) {
  // The leading-zero strip comes first: Indians routinely write a mobile as 098765 43210 out
  // of STD-dialling habit, and wa.me reads that 0 as part of the country code and rejects it.
  const digits = phone.replace(/[^0-9]/g, '').replace(/^0+/, '');
  return digits.length === 10 ? `91${digits}` : digits;
}

/** Just the digits, e.g. 919876543210. Internal: whatsappUrl() is the public surface. */
const WHATSAPP_NUMBER = toWhatsAppDigits(CONTACT_PHONE);

/** What the visitor's WhatsApp opens pre-filled with, so the only thing left to do is send. */
const DEFAULT_WHATSAPP_MESSAGE = 'Hi MoveMate, I would like a quote for my move.';

/**
 * Builds a click-to-chat link with the first message already typed.
 *
 * `encodeURIComponent` is not optional here: an unescaped `&` or `#` would silently truncate
 * the message at that character, and raw spaces make the URL invalid. Callers pass their own
 * text so an enquiry arrives with context - "...about Office Relocation" rather than "hi" -
 * which is the whole reason this is a function and not a constant string.
 */
export function whatsappUrl(message = DEFAULT_WHATSAPP_MESSAGE) {
  return `https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent(message)}`;
}

/** Ready-made link for the floating button and anywhere without extra context to add. */
export const WHATSAPP_URL = whatsappUrl();

/** Trust signals for the homepage. Honest framing: these are illustrative figures. */
export const COMPANY_STATS = [
  { label: 'Moves completed', value: '12,400+' },
  { label: 'Cities covered', value: '28' },
  { label: 'Average rating', value: '4.8 / 5' },
  { label: 'On-time delivery', value: '96%' },
];

export const WHY_US = [
  {
    title: 'Transparent, itemised pricing',
    body: 'Every quote shows the base charge, distance, packing, insurance and GST as separate lines. The price you see online is the price we honour.',
  },
  {
    title: 'Trained, in-house crews',
    body: 'No day labour. Our packers are permanent staff, background-verified and trained on fragile handling, furniture dismantling and appliance packing.',
  },
  {
    title: 'GPS-tracked vehicles',
    body: 'Closed-body trucks with live tracking and a named coordinator, so you always know where your goods are and who to call.',
  },
  {
    title: 'Transit insurance available',
    body: 'Optional all-risk cover at 2.5% of your declared goods value, with claims settled against our pre-dispatch photo inventory.',
  },
];

export const HOW_IT_WORKS = [
  {
    step: '01',
    title: 'Get an instant estimate',
    body: 'Tell us your home size, the two addresses and the distance. The quote engine returns an itemised price in seconds - no phone call needed.',
  },
  {
    step: '02',
    title: 'Confirm your move date',
    body: 'Submit the booking and you get a reference number straight away. Our coordinator calls within two hours to confirm the survey and the date.',
  },
  {
    step: '03',
    title: 'We pack and load',
    body: 'The crew arrives with all material included in your quote, packs room by room with colour-coded labels, and loads under a supervisor.',
  },
  {
    step: '04',
    title: 'Track until delivery',
    body: 'Follow your booking status online from Confirmed to In Transit to Delivered, then we unpack and reassemble at your new address.',
  },
];
