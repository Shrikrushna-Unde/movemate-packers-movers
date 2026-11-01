import { useId } from 'react';

/**
 * Form field primitives: Input, Select, Textarea and Checkbox.
 *
 * All four share one wrapper so that a label, a hint, an error message and the accessibility
 * wiring are implemented once. On a project with a nineteen-field booking form, that is the
 * difference between a maintainable form and nineteen copies of the same five lines.
 *
 * Accessibility is the part most tutorials skip, and it is three attributes:
 *   htmlFor + id       clicking the label focuses the input
 *   aria-invalid       tells a screen reader the field is in an error state
 *   aria-describedby   points the screen reader at the error text to read out
 */

function FieldShell({ id, label, error, hint, required, children, className = '' }) {
  return (
    <div className={className}>
      {label && (
        <label htmlFor={id} className="label">
          {label}
          {/* aria-hidden on the asterisk: "required" is already announced by the input's
              own required attribute, so reading "star" aloud is noise. */}
          {required && (
            <span className="ml-0.5 text-rose-500" aria-hidden="true">
              *
            </span>
          )}
        </label>
      )}

      {children}

      {/* The error replaces the hint rather than stacking, so the layout does not jump. */}
      {error ? (
        <p id={`${id}-error`} className="mt-1.5 text-xs font-medium text-rose-600">
          {error}
        </p>
      ) : (
        hint && <p className="mt-1.5 text-xs text-slate-500">{hint}</p>
      )}
    </div>
  );
}

export function Input({ label, error, hint, required, className, ...rest }) {
  /*
   * useId generates a unique, stable id per component instance (React 18+). Hardcoding
   * id="email" would break the label-to-input link as soon as two forms appear on one page -
   * and duplicate ids are invalid HTML.
   */
  const id = useId();
  return (
    <FieldShell
      id={id}
      label={label}
      error={error}
      hint={hint}
      required={required}
      className={className}
    >
      <input
        id={id}
        required={required}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id}-error` : undefined}
        className={`input-base ${error ? 'input-error' : ''}`}
        {...rest}
      />
    </FieldShell>
  );
}

export function Select({ label, error, hint, required, options = [], placeholder, className, ...rest }) {
  const id = useId();
  return (
    <FieldShell
      id={id}
      label={label}
      error={error}
      hint={hint}
      required={required}
      className={className}
    >
      <select
        id={id}
        required={required}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id}-error` : undefined}
        className={`input-base ${error ? 'input-error' : ''}`}
        {...rest}
      >
        {/* value="" so an untouched select submits empty and `required` can catch it. */}
        {placeholder && <option value="">{placeholder}</option>}
        {options.map((option) => (
          // The key must be stable and unique - React uses it to match elements between
          // renders. Using the array index here would be wrong the moment the list is
          // reordered or filtered.
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </FieldShell>
  );
}

export function Textarea({ label, error, hint, required, rows = 4, className, ...rest }) {
  const id = useId();
  return (
    <FieldShell
      id={id}
      label={label}
      error={error}
      hint={hint}
      required={required}
      className={className}
    >
      <textarea
        id={id}
        rows={rows}
        required={required}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id}-error` : undefined}
        className={`input-base resize-y ${error ? 'input-error' : ''}`}
        {...rest}
      />
    </FieldShell>
  );
}

/**
 * A checkbox with its label and description.
 *
 * The whole block is wrapped in a <label>, so clicking the description text also toggles the
 * box - a small thing that makes a form noticeably easier to use on a phone.
 */
export function Checkbox({ label, description, checked, onChange, name, disabled }) {
  return (
    <label
      className={`flex cursor-pointer items-start gap-3 rounded-xl border p-3.5 transition-colors ${
        checked ? 'border-brand-300 bg-brand-50' : 'border-slate-200 bg-white hover:bg-slate-50'
      } ${disabled ? 'cursor-not-allowed opacity-60' : ''}`}
    >
      <input
        type="checkbox"
        name={name}
        /*
         * `checked` + `onChange` makes this a CONTROLLED input: React state is the single
         * source of truth and the DOM only reflects it. Passing `checked` without `onChange`
         * produces a read-only input and a React warning - one of the most common React
         * form bugs.
         */
        checked={checked}
        onChange={onChange}
        disabled={disabled}
        className="mt-0.5 h-4 w-4 shrink-0 rounded border-slate-300 text-brand-700 focus:ring-brand-500"
      />
      <span>
        <span className="block text-sm font-medium text-slate-800">{label}</span>
        {description && <span className="mt-0.5 block text-xs text-slate-500">{description}</span>}
      </span>
    </label>
  );
}
