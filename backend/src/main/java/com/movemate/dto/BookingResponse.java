package com.movemate.dto;

import com.movemate.domain.Booking;
import com.movemate.domain.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * The booking as the API exposes it.
 *
 * <p>Two things here are computed rather than stored:
 * <ul>
 *   <li>{@code allowedNextStatuses} - the admin UI renders buttons from this list instead of
 *       hardcoding the workflow in React. The state machine then lives in exactly one place
 *       (the {@link BookingStatus} enum) and the frontend can never offer an illegal action.</li>
 *   <li>{@code timeline} - the progress tracker the customer sees. Deriving it server-side
 *       keeps the "which step am I on" logic out of the browser.</li>
 * </ul>
 *
 * <p>{@code adminNotes} is deliberately absent. Internal remarks like "customer haggled, gave
 * 5% off" must never reach a customer-facing endpoint, and the surest way to guarantee that is
 * to leave the field out of the DTO entirely.
 */
public record BookingResponse(
        Long id,
        String reference,
        String customerName,
        String customerEmail,
        String customerPhone,
        String moveType,
        String moveTypeLabel,
        String homeSize,
        String homeSizeLabel,
        String pickupAddress,
        String pickupCity,
        String pickupPincode,
        String dropAddress,
        String dropCity,
        String dropPincode,
        int distanceKm,
        LocalDate moveDate,
        int pickupFloor,
        int dropFloor,
        boolean liftAvailable,
        boolean packingRequired,
        boolean insuranceRequired,
        BigDecimal declaredValue,
        int storageDays,
        BigDecimal estimatedAmount,
        BookingStatus status,
        String statusLabel,
        String statusMessage,
        List<BookingStatus> allowedNextStatuses,
        List<TimelineStep> timeline,
        String customerNotes,
        Instant createdAt
) {

    /**
     * One dot on the customer-facing progress bar.
     *
     * @param status  machine value
     * @param label   display text
     * @param message reassuring sentence for the current step
     * @param reached whether the booking has already passed (or is at) this step
     * @param current whether this is exactly where the booking is now
     */
    public record TimelineStep(
            BookingStatus status,
            String label,
            String message,
            boolean reached,
            boolean current
    ) {
    }

    /** The happy path. CANCELLED is deliberately excluded - it is an exit, not a step. */
    private static final List<BookingStatus> HAPPY_PATH = List.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.IN_TRANSIT,
            BookingStatus.DELIVERED
    );

    public static BookingResponse from(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getReference(),
                b.getCustomerName(),
                b.getCustomerEmail(),
                b.getCustomerPhone(),
                b.getMoveType().name(),
                b.getMoveType().getLabel(),
                // Null-safe: home size is absent for vehicle and office moves, and calling
                // .name() on null here would 500 the whole endpoint.
                b.getHomeSize() == null ? null : b.getHomeSize().name(),
                b.getHomeSize() == null ? null : b.getHomeSize().getLabel(),
                b.getPickupAddress(),
                b.getPickupCity(),
                b.getPickupPincode(),
                b.getDropAddress(),
                b.getDropCity(),
                b.getDropPincode(),
                b.getDistanceKm(),
                b.getMoveDate(),
                b.getPickupFloor(),
                b.getDropFloor(),
                b.isLiftAvailable(),
                b.isPackingRequired(),
                b.isInsuranceRequired(),
                b.getDeclaredValue(),
                b.getStorageDays(),
                b.getEstimatedAmount(),
                b.getStatus(),
                b.getStatus().getLabel(),
                b.getStatus().getCustomerMessage(),
                // Sorted so the admin UI button order is stable between requests. A HashSet
                // iteration order is unspecified, and a UI whose buttons move is a bug report.
                b.getStatus().allowedNextStates().stream()
                        .sorted()
                        .toList(),
                buildTimeline(b.getStatus()),
                b.getCustomerNotes(),
                b.getCreatedAt()
        );
    }

    /**
     * Marks every step up to the current one as reached.
     *
     * <p>Because the enum constants are declared in lifecycle order, {@code ordinal()}
     * comparison is enough to answer "have we passed this step?". That is a legitimate use of
     * ordinal - in memory, for ordering. Persisting the ordinal is the thing to avoid.
     */
    private static List<TimelineStep> buildTimeline(BookingStatus current) {
        if (current == BookingStatus.CANCELLED) {
            return List.of(new TimelineStep(
                    BookingStatus.CANCELLED,
                    BookingStatus.CANCELLED.getLabel(),
                    BookingStatus.CANCELLED.getCustomerMessage(),
                    true,
                    true));
        }
        return HAPPY_PATH.stream()
                .map(step -> new TimelineStep(
                        step,
                        step.getLabel(),
                        step.getCustomerMessage(),
                        step.ordinal() <= current.ordinal(),
                        step == current))
                .toList();
    }

    /** Exposed for the frontend's status filter dropdown. */
    public static List<BookingStatus> allStatuses() {
        return Arrays.asList(BookingStatus.values());
    }
}
