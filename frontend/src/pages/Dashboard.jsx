import { useState } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { PackagePlus, Star } from 'lucide-react';

import Button, { buttonClasses } from '../components/ui/Button.jsx';
import { Input, Select, Textarea } from '../components/ui/Field.jsx';
import { Card } from '../components/ui/Card.jsx';
import { Alert, EmptyState, Spinner } from '../components/ui/Feedback.jsx';
import BookingCard from '../components/booking/BookingCard.jsx';
import { bookingApi, testimonialApi } from '../api/endpoints.js';
import { useApiData } from '../hooks/useApiData.js';
import { useAuth } from '../context/AuthContext.jsx';
import { formatCurrency } from '../utils/format.js';

const RATING_OPTIONS = [5, 4, 3, 2, 1].map((n) => ({
  value: String(n),
  label: `${n} star${n > 1 ? 's' : ''}`,
}));

/**
 * The signed-in customer's area: their bookings and a review form.
 *
 * Note that this page never sends a user id anywhere. `GET /bookings/me` derives the owner from
 * the JWT, so there is no parameter to tamper with - the API cannot be tricked into returning
 * someone else's moves. Designing endpoints that way removes an entire class of
 * authorisation bug.
 */
export default function Dashboard() {
  const { user } = useAuth();
  const { data: bookings, loading, error } = useApiData(() => bookingApi.mine());

  const [review, setReview] = useState({ city: '', rating: '5', message: '' });
  const [reviewErrors, setReviewErrors] = useState({});
  const [submittingReview, setSubmittingReview] = useState(false);
  const [reviewSent, setReviewSent] = useState(false);

  const handleReviewChange = (event) => {
    const { name, value } = event.target;
    setReview((previous) => ({ ...previous, [name]: value }));
    setReviewErrors((previous) => ({ ...previous, [name]: undefined }));
  };

  const submitReview = async (event) => {
    event.preventDefault();
    setSubmittingReview(true);
    setReviewErrors({});

    try {
      await testimonialApi.submit({
        ...review,
        // The select gives a string; the backend field is an Integer.
        rating: Number(review.rating),
      });
      setReviewSent(true);
      setReview({ city: '', rating: '5', message: '' });
      toast.success('Thanks. Your review will appear once our team approves it.');
    } catch (err) {
      if (err.fieldErrors) setReviewErrors(err.fieldErrors);
      else toast.error(err.message);
    } finally {
      setSubmittingReview(false);
    }
  };

  // Derived values, computed at render rather than stored in state. Storing a total in state
  // means remembering to recompute it on every change - a classic source of stale UI.
  const totalValue = bookings?.reduce((sum, b) => sum + Number(b.estimatedAmount), 0) ?? 0;
  const activeCount =
    bookings?.filter((b) => !['DELIVERED', 'CANCELLED'].includes(b.status)).length ?? 0;

  return (
    <div className="container-page py-12">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
            {/* Optional chaining: `user` is null for the single render before the profile
                arrives, and `user.fullName` would throw. */}
            Hello, {user?.fullName?.split(' ')[0] ?? 'there'}
          </h1>
          <p className="mt-1.5 text-slate-600">
            {activeCount > 0
              ? `You have ${activeCount} move${activeCount > 1 ? 's' : ''} in progress.`
              : 'You have no moves in progress right now.'}
          </p>
        </div>
        <Link to="/quote" className={buttonClasses({ variant: 'accent' })}>
          <PackagePlus className="h-4 w-4" aria-hidden="true" />
          Book another move
        </Link>
      </div>

      {/* Summary tiles, only once there is something to summarise. */}
      {bookings?.length > 0 && (
        <div className="mt-8 grid gap-4 sm:grid-cols-3">
          <Card className="p-5">
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Total bookings
            </p>
            <p className="mt-1 text-2xl font-extrabold text-slate-900">{bookings.length}</p>
          </Card>
          <Card className="p-5">
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              In progress
            </p>
            <p className="mt-1 text-2xl font-extrabold text-brand-800">{activeCount}</p>
          </Card>
          <Card className="p-5">
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Lifetime value
            </p>
            <p className="mt-1 text-2xl font-extrabold text-slate-900">
              {formatCurrency(totalValue)}
            </p>
          </Card>
        </div>
      )}

      <div className="mt-10 grid gap-8 lg:grid-cols-[1.5fr_1fr] lg:items-start">
        {/* Bookings */}
        <div>
          <h2 className="text-lg font-bold text-slate-900">Your bookings</h2>

          {loading && <Spinner label="Loading your bookings" />}

          {error && (
            <Alert type="error" className="mt-4">
              {error}
            </Alert>
          )}

          {!loading && !error && bookings?.length === 0 && (
            <div className="mt-4">
              <EmptyState
                title="No bookings yet"
                description="Once you book a move it will appear here with live status tracking."
                action={
                  <Link to="/quote" className={buttonClasses({ variant: 'primary' })}>
                    Get an instant quote
                  </Link>
                }
              />
            </div>
          )}

          <div className="mt-4 space-y-4">
            {bookings?.map((booking) => (
              <BookingCard key={booking.id} booking={booking} />
            ))}
          </div>
        </div>

        {/* Review form */}
        <Card className="p-6 lg:sticky lg:top-24">
          <div className="flex items-center gap-2">
            <Star className="h-5 w-5 text-accent-500" aria-hidden="true" />
            <h2 className="text-base font-bold text-slate-900">Leave a review</h2>
          </div>
          <p className="mt-2 text-sm text-slate-600">
            Reviews appear on the homepage after our team checks them. Your name is taken from
            your account.
          </p>

          {reviewSent ? (
            <Alert type="success" className="mt-5">
              Your review is in the moderation queue. Thank you.
            </Alert>
          ) : (
            <form onSubmit={submitReview} className="mt-5 space-y-4" noValidate>
              <Input
                label="Your city"
                name="city"
                value={review.city}
                onChange={handleReviewChange}
                required
                placeholder="Pune"
                error={reviewErrors.city}
              />
              <Select
                label="Rating"
                name="rating"
                value={review.rating}
                onChange={handleReviewChange}
                options={RATING_OPTIONS}
                required
                error={reviewErrors.rating}
              />
              <Textarea
                label="Your review"
                name="message"
                rows={5}
                value={review.message}
                onChange={handleReviewChange}
                required
                placeholder="How did the move go? Was the crew on time, was anything damaged, did the final invoice match the quote?"
                error={reviewErrors.message}
                hint="Between 20 and 600 characters"
              />
              <Button type="submit" fullWidth loading={submittingReview}>
                Submit review
              </Button>
            </form>
          )}
        </Card>
      </div>
    </div>
  );
}
