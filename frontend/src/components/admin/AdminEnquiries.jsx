import { useState } from 'react';
import toast from 'react-hot-toast';
import { CheckCircle2, Mail, Phone, RotateCcw } from 'lucide-react';

import Button from '../ui/Button.jsx';
import { Badge, Card } from '../ui/Card.jsx';
import { Alert, EmptyState, Spinner } from '../ui/Feedback.jsx';
import { adminApi } from '../../api/endpoints.js';
import { useApiData } from '../../hooks/useApiData.js';
import { formatDateTime } from '../../utils/format.js';

/**
 * The contact-form inbox.
 *
 * Note the `busyId` pattern below: a single id, not a boolean. With one shared `loading` flag,
 * clicking "Mark handled" on row 3 would put every button on the page into a spinner. Tracking
 * WHICH row is busy keeps the feedback where the user clicked - a small detail that separates
 * a careful UI from a sloppy one.
 */
export default function AdminEnquiries() {
  const [page, setPage] = useState(0);
  const [busyId, setBusyId] = useState(null);

  const { data: result, loading, error, reload } = useApiData(
    () => adminApi.enquiries({ page, size: 10 }),
    [page],
  );

  const toggleHandled = async (enquiry) => {
    setBusyId(enquiry.id);
    try {
      // The desired value is sent explicitly rather than toggling server-side, so a double
      // click cannot flip the flag twice.
      await adminApi.setEnquiryHandled(enquiry.id, !enquiry.handled);
      toast.success(enquiry.handled ? 'Re-opened' : 'Marked as handled');
      reload();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setBusyId(null);
    }
  };

  if (loading) return <Spinner label="Loading enquiries" />;
  if (error) return <Alert type="error">{error}</Alert>;
  if (!result?.content?.length) {
    return (
      <EmptyState
        icon={Mail}
        title="No enquiries yet"
        description="Messages sent through the contact form on the website appear here."
      />
    );
  }

  return (
    <div className="space-y-4">
      {result.content.map((enquiry) => (
        <Card
          key={enquiry.id}
          // A subtle left border is enough to separate handled from unhandled at a glance,
          // without colouring the whole card and making the list look like an error log.
          className={`border-l-4 p-5 ${
            enquiry.handled ? 'border-l-emerald-400' : 'border-l-amber-400'
          }`}
        >
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2">
                <h3 className="font-semibold text-slate-900">{enquiry.subject}</h3>
                {enquiry.handled ? (
                  <Badge className="bg-emerald-100 text-emerald-800">Handled</Badge>
                ) : (
                  <Badge className="bg-amber-100 text-amber-800">New</Badge>
                )}
              </div>
              <p className="mt-1 text-xs text-slate-500">
                {enquiry.name} · {formatDateTime(enquiry.createdAt)}
              </p>
            </div>

            <Button
              variant={enquiry.handled ? 'ghost' : 'success'}
              size="sm"
              loading={busyId === enquiry.id}
              onClick={() => toggleHandled(enquiry)}
            >
              {enquiry.handled ? (
                <>
                  <RotateCcw className="h-4 w-4" aria-hidden="true" />
                  Re-open
                </>
              ) : (
                <>
                  <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
                  Mark handled
                </>
              )}
            </Button>
          </div>

          {/* whitespace-pre-line preserves the line breaks the customer typed. Without it the
              whole message collapses into one paragraph, because HTML folds whitespace. */}
          <p className="mt-3 whitespace-pre-line text-sm leading-relaxed text-slate-700">
            {enquiry.message}
          </p>

          <div className="mt-4 flex flex-wrap gap-4 border-t border-slate-100 pt-3 text-sm">
            <a
              href={`mailto:${enquiry.email}?subject=Re: ${encodeURIComponent(enquiry.subject)}`}
              className="flex items-center gap-1.5 text-brand-700 hover:underline"
            >
              <Mail className="h-4 w-4" aria-hidden="true" />
              {enquiry.email}
            </a>
            {enquiry.phone && (
              <a
                href={`tel:${enquiry.phone}`}
                className="flex items-center gap-1.5 text-brand-700 hover:underline"
              >
                <Phone className="h-4 w-4" aria-hidden="true" />
                {enquiry.phone}
              </a>
            )}
          </div>
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
    </div>
  );
}
