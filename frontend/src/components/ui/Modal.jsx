import { useEffect } from 'react';
import { X } from 'lucide-react';

/**
 * A dialog.
 *
 * Three behaviours that most hand-rolled modals get wrong, and that make the difference
 * between "looks like a modal" and "works like one":
 *   1. Escape closes it.
 *   2. The page behind it does not scroll while it is open.
 *   3. Clicking the backdrop closes it, but clicking inside does not.
 *
 * In a production app the better answer is the native <dialog> element or a library such as
 * Radix, which also handle focus trapping and returning focus to the trigger on close. This
 * implementation is deliberate about what it does NOT solve - knowing the gap is more valuable
 * than pretending there isn't one.
 */
export default function Modal({ open, onClose, title, description, children, footer }) {
  /**
   * Escape-to-close and scroll locking, both torn down on unmount.
   *
   * The cleanup function is the important half: without removing the listener, every open
   * would add another one, and without restoring `overflow` the page would stay frozen after
   * the modal closed.
   */
  useEffect(() => {
    if (!open) return undefined;

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') onClose();
    };

    document.addEventListener('keydown', handleKeyDown);
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [open, onClose]);

  // Returning null unmounts the content entirely, so its inputs are not focusable while
  // hidden - unlike a CSS-only `display: none`, which leaves them in the tab order.
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center p-4 sm:items-center">
      {/* Backdrop. A button, not a div, so it is reachable by keyboard and announced. */}
      <button
        type="button"
        aria-label="Close dialog"
        onClick={onClose}
        className="absolute inset-0 cursor-default bg-slate-900/50 backdrop-blur-sm"
      />

      <div
        // role="dialog" + aria-modal tell assistive technology that the rest of the page is
        // inert; aria-labelledby points at the title so the dialog announces itself.
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        className="relative z-10 w-full max-w-lg rounded-2xl bg-white shadow-lift"
      >
        <div className="flex items-start justify-between gap-4 border-b border-slate-200 px-6 py-4">
          <div>
            <h2 id="modal-title" className="text-base font-bold text-slate-900">
              {title}
            </h2>
            {description && <p className="mt-1 text-sm text-slate-600">{description}</p>}
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-700"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* max-h + overflow-y-auto so a tall body scrolls inside the dialog rather than
            pushing the footer off the screen on a phone. */}
        <div className="max-h-[60vh] overflow-y-auto px-6 py-5">{children}</div>

        {footer && (
          <div className="flex justify-end gap-3 border-t border-slate-200 px-6 py-4">{footer}</div>
        )}
      </div>
    </div>
  );
}
