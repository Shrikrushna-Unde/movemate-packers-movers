import { SectionHeading } from '../ui/Card.jsx';
import { HOW_IT_WORKS } from '../../utils/constants.js';

/**
 * The four-step process.
 *
 * Rendered from the HOW_IT_WORKS array rather than four copies of the same markup. The payoff
 * is not fewer characters - it is that a change to the step layout happens once, and the
 * content is editable by someone who does not read JSX.
 */
export default function HowItWorks() {
  return (
    <section className="bg-white py-16 sm:py-20">
      <div className="container-page">
        <SectionHeading
          eyebrow="How it works"
          title="Four steps from quote to delivered"
          subtitle="No site visit needed to get a price. The survey happens after you book, and the quote is honoured unless the actual volume differs from what you declared."
        />

        <ol className="mt-12 grid gap-8 md:grid-cols-2 lg:grid-cols-4">
          {HOW_IT_WORKS.map((item, index) => (
            // <ol> because the steps are genuinely ordered. A screen reader announces "list,
            // 4 items, item 1 of 4", which conveys the sequence that the visual design implies.
            <li key={item.step} className="relative">
              {/*
                The connecting line, drawn only between items and hidden on the last one and on
                small screens where the layout stacks vertically.
              */}
              {index < HOW_IT_WORKS.length - 1 && (
                <span
                  className="absolute left-12 top-5 hidden h-px w-[calc(100%-2rem)] bg-slate-200 lg:block"
                  aria-hidden="true"
                />
              )}

              <span className="relative grid h-10 w-10 place-items-center rounded-full bg-brand-900 text-sm font-bold text-accent-400">
                {item.step}
              </span>

              <h3 className="mt-5 text-base font-semibold text-slate-900">{item.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-slate-600">{item.body}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
