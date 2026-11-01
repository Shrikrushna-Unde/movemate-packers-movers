import { useState } from 'react';
import toast from 'react-hot-toast';
import { ChevronLeft, ChevronRight, Search } from 'lucide-react';

import Button from '../ui/Button.jsx';
import { Input, Select, Textarea } from '../ui/Field.jsx';
import { Card, StatusBadge } from '../ui/Card.jsx';
import { Alert, EmptyState, Spinner } from '../ui/Feedback.jsx';
import Modal from '../ui/Modal.jsx';
import { adminApi, catalogApi } from '../../api/endpoints.js';
import { useApiData } from '../../hooks/useApiData.js';
import { formatCurrency, formatDate, formatRelative } from '../../utils/format.js';

/**
 * The admin booking list: server-side filtering, paging and status updates.
 *
 * The important architectural point is that filtering and paging happen in MySQL, not in
 * JavaScript. Fetching every booking and calling `.filter()` in the browser works beautifully
 * with 30 rows and falls over at 30,000 - and it would also mean sending every customer's
 * address to the client just to display one page of twenty.
 */
export default function AdminBookings() {
  // Filter state. `page` is reset to 0 whenever a filter changes, otherwise a user on page 4
  // of all bookings would land on an empty page 4 of the filtered set.
  const [status, setStatus] = useState('');
  const [query, setQuery] = useState('');
  const [appliedQuery, setAppliedQuery] = useState('');
  const [page, setPage] = useState(0);

  // Status-update dialog state.
  const [editing, setEditing] = useState(null);
  const [nextStatus, setNextStatus] = useState('');
  const [adminNotes, setAdminNotes] = useState('');
  const [saving, setSaving] = useState(false);

  const { data: options } = useApiData(() => catalogApi.options());

  /*
   * The dependency array is what makes this refetch: change the status filter or the page and
   * the hook runs a fresh request with the new parameters. `reload` is for after a mutation,
   * where the parameters have not changed but the data has.
   */
  const { data: result, loading, error, reload } = useApiData(
    () =>
      adminApi.bookings({
        // `|| undefined` so an empty filter is omitted from the query string entirely rather
        // than sent as `status=`, which the backend would try to parse as an enum.
        status: status || undefined,
        query: appliedQuery || undefined,
        page,
        size: 10,
      }),
    [status, appliedQuery, page],
  );

  const openStatusModal = (booking) => {
    setEditing(booking);
    // Preselect the first legal transition. The list comes from the backend's state machine,
    // so the dropdown can only ever offer valid moves.
    setNextStatus(booking.allowedNextStatuses[0] ?? '');
    setAdminNotes('');
  };

  const saveStatus = async () => {
    setSaving(true);
    try {
      await adminApi.updateBookingStatus(editing.id, {
        status: nextStatus,
        adminNotes: adminNotes || null,
      });
      toast.success(`${editing.reference} updated to ${nextStatus.replace('_', ' ')}`);
      setEditing(null);
      // Refetch rather than patching the row in local state. One extra request buys guaranteed
      // consistency with the server, including anything another admin changed meanwhile.
      reload();
    } catch (err) {
      // An illegal transition comes back as a 400 with a readable message from the entity.
      toast.error(err.message);
    } finally {
      setSaving(false);
    }
  };

  const statusOptions = [
    { value: '', label: 'All statuses' },
    ...(options?.bookingStatuses ?? []),
  ];

  return (
    <div className="space-y-5">
      {/* Filters */}
      <Card className="p-4">
        <form
          onSubmit={(event) => {
            event.preventDefault();
            // Search runs on submit, not on every keystroke. For a table query that is the
            // right call: it avoids hammering the database with a request per character.
            setAppliedQuery(query);
            setPage(0);
          }}
          className="grid gap-3 sm:grid-cols-[1fr_200px_auto] sm:items-end"
        >
          <Input
            label="Search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Reference, name, email, phone or city"
          />
          <Select
            label="Status"
            value={status}
            onChange={(event) => {
              setStatus(event.target.value);
              setPage(0);
            }}
            options={statusOptions}
          />
          <Button type="submit" className="sm:mb-0.5">
            <Search className="h-4 w-4" aria-hidden="true" />
            Search
          </Button>
        </form>
      </Card>

      {loading && <Spinner label="Loading bookings" />}
      {error && <Alert type="error">{error}</Alert>}

      {!loading && result?.content?.length === 0 && (
        <EmptyState
          title="No bookings match these filters"
          description="Try clearing the search term or choosing a different status."
        />
      )}

      {/* Table */}
      {result?.content?.length > 0 && (
        <Card className="overflow-hidden">
          {/*
            overflow-x-auto on the wrapper lets a wide table scroll sideways on a phone instead
            of either overflowing the viewport or squashing columns into unreadable slivers.
          */}
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              {/* A real <table> with <thead>/<th>: screen readers announce "column 3 of 6,
                  Status" as you move through the cells. A grid of divs conveys nothing. */}
              <thead className="bg-slate-50 text-left text-xs uppercase tracking-wider text-slate-500">
                <tr>
                  <th className="px-4 py-3 font-semibold">Reference</th>
                  <th className="px-4 py-3 font-semibold">Customer</th>
                  <th className="px-4 py-3 font-semibold">Route</th>
                  <th className="px-4 py-3 font-semibold">Move date</th>
                  <th className="px-4 py-3 text-right font-semibold">Amount</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 font-semibold">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {result.content.map((booking) => (
                  <tr key={booking.id} className="hover:bg-slate-50">
                    <td className="whitespace-nowrap px-4 py-3">
                      <span className="font-mono text-xs font-bold text-brand-900">
                        {booking.reference}
                      </span>
                      <span className="mt-0.5 block text-xs text-slate-400">
                        {formatRelative(booking.createdAt)}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="block font-medium text-slate-800">
                        {booking.customerName}
                      </span>
                      <span className="block text-xs text-slate-500">{booking.customerPhone}</span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="block text-slate-700">
                        {booking.pickupCity} → {booking.dropCity}
                      </span>
                      <span className="block text-xs text-slate-500">
                        {booking.distanceKm} km · {booking.moveTypeLabel}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-slate-700">
                      {formatDate(booking.moveDate)}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right font-semibold tabular-nums text-slate-900">
                      {formatCurrency(booking.estimatedAmount)}
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={booking.status} label={booking.statusLabel} />
                    </td>
                    <td className="px-4 py-3">
                      {/*
                        The button only appears when the backend says a transition is possible.
                        A DELIVERED booking has an empty allowedNextStatuses array, so there is
                        nothing to click - the UI cannot offer an action the API would reject.
                      */}
                      {booking.allowedNextStatuses.length > 0 ? (
                        <Button variant="outline" size="sm" onClick={() => openStatusModal(booking)}>
                          Update
                        </Button>
                      ) : (
                        <span className="text-xs text-slate-400">Closed</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          <div className="flex items-center justify-between gap-4 border-t border-slate-200 px-4 py-3 text-sm">
            <span className="text-slate-600">
              Page {result.page + 1} of {result.totalPages} · {result.totalElements} bookings
            </span>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                // `first` and `last` come from PageResponse, so the buttons disable themselves
                // at the boundaries without the frontend recalculating anything.
                disabled={result.first}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                <ChevronLeft className="h-4 w-4" aria-hidden="true" />
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={result.last}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
                <ChevronRight className="h-4 w-4" aria-hidden="true" />
              </Button>
            </div>
          </div>
        </Card>
      )}

      {/* Status update dialog */}
      <Modal
        open={Boolean(editing)}
        onClose={() => setEditing(null)}
        title={`Update ${editing?.reference ?? ''}`}
        description="Only transitions allowed by the booking workflow are offered."
        footer={
          <>
            <Button variant="outline" onClick={() => setEditing(null)}>
              Cancel
            </Button>
            <Button onClick={saveStatus} loading={saving} disabled={!nextStatus}>
              Save change
            </Button>
          </>
        }
      >
        {editing && (
          <div className="space-y-4">
            <div className="rounded-xl bg-slate-50 p-4 text-sm">
              <p className="text-slate-600">
                Current status: <StatusBadge status={editing.status} label={editing.statusLabel} />
              </p>
              <p className="mt-2 text-slate-600">
                {editing.customerName} · {editing.pickupCity} → {editing.dropCity} ·{' '}
                {formatDate(editing.moveDate)}
              </p>
              {editing.customerNotes && (
                <p className="mt-2 text-xs italic text-slate-500">
                  Customer note: {editing.customerNotes}
                </p>
              )}
            </div>

            <Select
              label="New status"
              value={nextStatus}
              onChange={(event) => setNextStatus(event.target.value)}
              options={editing.allowedNextStatuses.map((s) => ({
                value: s,
                label: s.replace('_', ' '),
              }))}
              required
            />

            <Textarea
              label="Internal note (optional)"
              rows={3}
              value={adminNotes}
              onChange={(event) => setAdminNotes(event.target.value)}
              placeholder="Crew assigned, vehicle MH12 AB 1234, customer asked for an 8 AM start"
              hint="Visible to staff only - never returned on customer-facing endpoints"
            />
          </div>
        )}
      </Modal>
    </div>
  );
}
