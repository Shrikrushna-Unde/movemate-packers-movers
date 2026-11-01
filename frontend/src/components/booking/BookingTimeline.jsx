import { Check, X } from 'lucide-react';

/**
 * The status progress tracker.
 *
 * Every step comes from the backend's `timeline` array, including which ones are `reached` and
 * which is `current`. Deriving that in React instead would mean duplicating the lifecycle
 * order on the client, and the two would disagree the day a status is added. The UI's only job
 * here is to paint what the server computed.
 */
export default function BookingTimeline({ timeline, cancelled }) {
  if (!timeline?.length) return null;

  // A cancelled booking gets its own single-step treatment rather than a greyed-out happy path,
  // because showing "Delivered" as a future step on a cancelled move is misleading.
  if (cancelled) {
    return (
      <div className="flex items-start gap-3 rounded-xl bg-rose-50 p-4 ring-1 ring-inset ring-rose-200">
        <span className="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-rose-500 text-white">
          <X className="h-4 w-4" aria-hidden="true" />
        </span>
        <div>
          <p className="text-sm font-semibold text-rose-900">{timeline[0].label}</p>
          <p className="mt-0.5 text-xs text-rose-700">{timeline[0].message}</p>
        </div>
      </div>
    );
  }

  return (
    <ol className="relative space-y-6">
      {timeline.map((step, index) => {
        const isLast = index === timeline.length - 1;
        return (
          <li key={step.status} className="relative flex gap-4">
            {/*
              The vertical connector between dots. Drawn with an absolutely positioned div
              rather than a border on the list item, so its colour can reflect progress: solid
              brand up to the current step, grey after it.
            */}
            {!isLast && (
              <span
                className={`absolute left-4 top-8 h-full w-0.5 -translate-x-1/2 ${
                  step.reached ? 'bg-brand-600' : 'bg-slate-200'
                }`}
                aria-hidden="true"
              />
            )}

            <span
              className={`relative z-10 grid h-8 w-8 shrink-0 place-items-center rounded-full text-xs font-bold ${
                step.reached ? 'bg-brand-700 text-white' : 'bg-slate-200 text-slate-500'
              } ${step.current ? 'ring-4 ring-brand-100' : ''}`}
            >
              {step.reached ? <Check className="h-4 w-4" aria-hidden="true" /> : index + 1}
            </span>

            <div className="pb-1">
              <p
                className={`text-sm font-semibold ${
                  step.current ? 'text-brand-800' : step.reached ? 'text-slate-900' : 'text-slate-500'
                }`}
              >
                {step.label}
                {step.current && (
                  <span className="ml-2 rounded-full bg-brand-100 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-brand-800">
                    Current
                  </span>
                )}
              </p>
              <p className="mt-0.5 text-xs leading-relaxed text-slate-500">{step.message}</p>
            </div>
          </li>
        );
      })}
    </ol>
  );
}
