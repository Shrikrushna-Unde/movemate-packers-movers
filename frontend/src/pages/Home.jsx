import Hero from '../components/home/Hero.jsx';
import ServicesGrid from '../components/home/ServicesGrid.jsx';
import HowItWorks from '../components/home/HowItWorks.jsx';
import WhyUs from '../components/home/WhyUs.jsx';
import TestimonialsSection from '../components/home/TestimonialsSection.jsx';
import CtaBand from '../components/home/CtaBand.jsx';

/**
 * The homepage.
 *
 * Deliberately almost empty: it composes six sections and holds no state and no logic of its
 * own. That is the goal for a page component - it decides WHAT appears and in what order,
 * while each section owns its own data fetching and presentation.
 *
 * The practical benefit: the services grid failing to load cannot affect the testimonials, and
 * either section can be reused on another page (ServicesGrid appears again on /services)
 * without untangling it from the homepage.
 */
export default function Home() {
  return (
    <>
      <Hero />
      {/* limit={3} - the same component renders three cards here and all six on /services. */}
      <ServicesGrid limit={3} />
      <HowItWorks />
      <WhyUs />
      <TestimonialsSection limit={3} />
      <CtaBand />
    </>
  );
}
