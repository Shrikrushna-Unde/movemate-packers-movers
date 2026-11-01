package com.movemate.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Lifecycle of a booking, modelled as an explicit state machine.
 *
 * <p>Most tutorial projects let an admin PATCH any status to any other status. That means a
 * DELIVERED move can silently go back to PENDING, which corrupts reporting and invoicing.
 * Encoding the legal transitions in the enum itself means the rule lives in exactly one
 * place and is impossible to forget at a call site.
 *
 * <pre>
 *   PENDING ---> CONFIRMED ---> IN_TRANSIT ---> DELIVERED   (terminal)
 *      |              |
 *      +------------- +--------> CANCELLED                  (terminal)
 * </pre>
 */
public enum BookingStatus {

    /** Request received, nobody has called the customer yet. */
    PENDING("Pending", "We have your request and will confirm within 2 hours."),

    /** Survey done, date locked, crew assigned. */
    CONFIRMED("Confirmed", "Your move is scheduled and a crew has been assigned."),

    /** Goods are loaded and on the road. */
    IN_TRANSIT("In Transit", "Your goods are packed, loaded and on the way."),

    /** Delivered and unpacked. Terminal state. */
    DELIVERED("Delivered", "Delivered at your new address. Thank you for choosing MoveMate."),

    /** Cancelled by the customer or by us. Terminal state. */
    CANCELLED("Cancelled", "This booking was cancelled.");

    private final String label;
    private final String customerMessage;

    /**
     * Legal next states.
     *
     * <p>Static fields of an enum are initialised AFTER all the constants exist, so it is safe
     * to reference PENDING, CONFIRMED and friends inside this static block. An EnumMap is used
     * rather than HashMap because it is backed by a plain array indexed by ordinal - no hashing,
     * no boxing, and it cannot hold a key outside the enum.
     */
    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(BookingStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(PENDING, EnumSet.of(CONFIRMED, CANCELLED));
        ALLOWED_TRANSITIONS.put(CONFIRMED, EnumSet.of(IN_TRANSIT, CANCELLED));
        ALLOWED_TRANSITIONS.put(IN_TRANSIT, EnumSet.of(DELIVERED));
        ALLOWED_TRANSITIONS.put(DELIVERED, EnumSet.noneOf(BookingStatus.class));
        ALLOWED_TRANSITIONS.put(CANCELLED, EnumSet.noneOf(BookingStatus.class));
    }

    BookingStatus(String label, String customerMessage) {
        this.label = label;
        this.customerMessage = customerMessage;
    }

    /**
     * @return true when moving from {@code this} state to {@code next} is allowed.
     */
    public boolean canTransitionTo(BookingStatus next) {
        // getOrDefault keeps this null-safe even if someone adds a constant
        // and forgets to register it in the static block above.
        return ALLOWED_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(BookingStatus.class)).contains(next);
    }

    /** A state nothing can follow. Used by the UI to stop rendering action buttons. */
    public boolean isTerminal() {
        return ALLOWED_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(BookingStatus.class)).isEmpty();
    }

    public Set<BookingStatus> allowedNextStates() {
        // Returned as an unmodifiable copy so a caller cannot mutate our internal rules.
        return Set.copyOf(ALLOWED_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(BookingStatus.class)));
    }

    public String getLabel() {
        return label;
    }

    public String getCustomerMessage() {
        return customerMessage;
    }
}
