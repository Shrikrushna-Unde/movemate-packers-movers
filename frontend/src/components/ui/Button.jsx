import { Loader2 } from 'lucide-react';

/**
 * Button styles, exported separately from the component.
 *
 * Why export the class builder? Because a styled <Link> from react-router must render an <a>
 * tag, not a <button> - wrapping a button in a link produces invalid HTML and breaks keyboard
 * navigation. Sharing the class function keeps a link-button and a real button visually
 * identical without faking either element.
 */
const VARIANTS = {
  primary: 'bg-brand-700 text-white hover:bg-brand-800 shadow-sm',
  accent: 'bg-accent-500 text-slate-900 hover:bg-accent-400 shadow-sm font-semibold',
  outline: 'border border-slate-300 bg-white text-slate-700 hover:bg-slate-50',
  ghost: 'text-slate-600 hover:bg-slate-100',
  danger: 'bg-rose-600 text-white hover:bg-rose-700',
  success: 'bg-emerald-600 text-white hover:bg-emerald-700',
};

const SIZES = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2.5 text-sm',
  lg: 'px-6 py-3 text-base',
};

export function buttonClasses({ variant = 'primary', size = 'md', fullWidth = false } = {}) {
  return [
    'inline-flex items-center justify-center gap-2 rounded-xl font-medium transition-colors',
    // disabled:pointer-events-none stops a double submit while a request is in flight.
    'disabled:cursor-not-allowed disabled:opacity-60 disabled:pointer-events-none',
    VARIANTS[variant] ?? VARIANTS.primary,
    SIZES[size] ?? SIZES.md,
    fullWidth ? 'w-full' : '',
  ].join(' ');
}

/**
 * The app's button.
 *
 * @param {boolean} loading shows a spinner and disables the button, so the caller never has to
 *                          manage both pieces of state separately
 * @param {string}  variant primary | accent | outline | ghost | danger | success
 */
export default function Button({
  children,
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  loading = false,
  className = '',
  type = 'button',
  disabled,
  ...rest
}) {
  return (
    <button
      /*
       * Defaults to type="button" on purpose. A <button> inside a <form> defaults to
       * type="submit" in HTML, so a "Cancel" button with no type submits the form - a bug that
       * is genuinely hard to spot. Forms opt in explicitly with type="submit".
       */
      type={type}
      disabled={disabled || loading}
      className={`${buttonClasses({ variant, size, fullWidth })} ${className}`}
      /*
       * ...rest forwards onClick, aria-label, form, data-* and anything else the caller passes.
       * It is placed AFTER className so an explicit prop from the caller wins - and it is what
       * keeps this wrapper from hiding the native button API.
       */
      {...rest}
    >
      {loading && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
      {children}
    </button>
  );
}
