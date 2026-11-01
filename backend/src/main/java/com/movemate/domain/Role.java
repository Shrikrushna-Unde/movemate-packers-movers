package com.movemate.domain;

/**
 * User roles.
 *
 * <p>The {@code ROLE_} prefix is not decoration - it is Spring Security's convention.
 * {@code hasRole("ADMIN")} internally checks for the authority {@code "ROLE_ADMIN"}, while
 * {@code hasAuthority("X")} checks for exactly {@code "X"}. By naming the enum constants
 * {@code ROLE_ADMIN} / {@code ROLE_CUSTOMER} we can hand {@code name()} straight to a
 * {@code SimpleGrantedAuthority} and both styles work. Forgetting this prefix is the single
 * most common reason a correctly-authenticated user still gets 403.
 *
 * <p>Stored in MySQL as a string via {@code @Enumerated(EnumType.STRING)}, never as an
 * ordinal int. Ordinals break the moment someone reorders the enum, and they make the raw
 * table unreadable.
 */
public enum Role {

    /** A customer: can book, see their own bookings, submit a review. */
    ROLE_CUSTOMER("Customer"),

    /** Staff: full access to every booking, enquiry and review moderation. */
    ROLE_ADMIN("Administrator");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    /** Human-readable name for the UI, so the frontend never has to prettify "ROLE_CUSTOMER". */
    public String getLabel() {
        return label;
    }
}
