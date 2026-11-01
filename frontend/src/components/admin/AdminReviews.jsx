import { useState } from 'react';
import toast from 'react-hot-toast';
import { Check, MessageSquareQuote, Trash2, X } from 'lucide-react';

import Button from '../ui/Button.jsx';
import { Badge, Card, Stars } from '../ui/Card.jsx';
import { Alert, EmptyState, Spinner } from '../ui/Feedback.jsx';
import Modal from '../ui/Modal.jsx';
import { adminApi } from '../../api/endpoints.js';
import { useApiData } from '../../hooks/useApiData.js';
import { formatDateTime } from '../../utils/format.js';

/**
 * Review moderation: approve, un-approve, delete.
 */
export default function AdminReviews() {
  const [page, setPage] = useState(0);
  const [busyId, setBusyId] = useState(null);
  /** The review queued for deletion, or null. Holding the object (not just a flag) lets the
      confirmation dialog name what is about to be deleted. */
  const [confirmDelete, setConfirmDelete] = useState(null);

  const { data: result, loading, error, reload } = useApiData(
    () => adminApi.testimonials({ page, size: 10 }),
    [page],
  );

  const setApproval = async (review, approved) => {
    setBusyId(review.id);
    try {
      await adminApi.setTestimonialApproval(review.id, approved);
      toast.success(approved ? 'Review published' : 'Review hidden');
      reload();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setBusyId(null);
    }
  };

  const doDelete = async () => {
    setBusyId(confirmDelete.id);
    try {
      await adminApi.deleteTestimonial(confirmDelete.id);
      toast.success('Review deleted');
      setConfirmDelete(null);
      reload();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setBusyId(null);
    }
  };

  if (loading) return <Spinner label="Loading reviews" />;
  if (error) return <Alert type="error">{error}</Alert>;
  if (!result?.content?.length) {
    return (
      <EmptyState
        icon={MessageSquareQuote}
        title="No reviews yet"
        description="Reviews submitted by signed-in customers appear here for approval before going live on the homepage."
      />
    );
  }

  return (
    <div className="space-y-4">
      {result.content.map((review) => (
        <Card key={review.id} className="p-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <span className="font-semibold text-slate-900">{review.authorName}</span>
                <span className="text-sm text-slate-500">· {review.city}</span>
                {review.approved ? (
                  <Badge className="bg-emerald-100 text-emerald-800">Live</Badge>
                ) : (
                  <Badge className="bg-amber-100 text-amber-800">Awaiting approval</Badge>
                )}
              </div>
              <div className="mt-1.5 flex items-center gap-3">
                <Stars rating={review.rating} className="text-sm" />
                <span className="text-xs text-slate-500">{formatDateTime(review.createdAt)}</span>
              </div>
            </div>

            <div className="flex gap-2">
              {review.approved ? (
                <Button
                  variant="outline"
                  size="sm"
                  loading={busyId === review.id}
                  onClick={() => setApproval(review, false)}
                >
                  <X className="h-4 w-4" aria-hidden="true" />
                  Hide
                </Button>
              ) : (
                <Button
                  variant="success"
                  size="sm"
                  loading={busyId === review.id}
                  onClick={() => setApproval(review, true)}
                >
                  <Check className="h-4 w-4" aria-hidden="true" />
                  Approve
                </Button>
              )}
              <Button
                variant="ghost"
                size="sm"
                // A destructive action always goes through a confirmation step. There is no
                // undo for a delete, so the dialog is the only safety net.
                onClick={() => setConfirmDelete(review)}
                aria-label={`Delete review by ${review.authorName}`}
              >
                <Trash2 className="h-4 w-4 text-rose-500" aria-hidden="true" />
              </Button>
            </div>
          </div>

          <p className="mt-3 text-sm leading-relaxed text-slate-700">“{review.message}”</p>
        </Card>
      ))}

      {result.totalPages > 1 && (
        <div className="flex items-center justify-between text-sm">
          <span className="text-slate-600">
            Page {result.page + 1} of {result.totalPages}
          </span>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={result.first}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Previous
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={result.last}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      )}

      <Modal
        open={Boolean(confirmDelete)}
        onClose={() => setConfirmDelete(null)}
        title="Delete this review?"
        description="This cannot be undone. Consider hiding it instead if you may want it back."
        footer={
          <>
            <Button variant="outline" onClick={() => setConfirmDelete(null)}>
              Keep it
            </Button>
            <Button variant="danger" loading={busyId === confirmDelete?.id} onClick={doDelete}>
              Delete permanently
            </Button>
          </>
        }
      >
        {confirmDelete && (
          <p className="text-sm text-slate-700">
            “{confirmDelete.message}” — <strong>{confirmDelete.authorName}</strong>
          </p>
        )}
      </Modal>
    </div>
  );
}
