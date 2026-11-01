package com.movemate.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the booking state machine.
 *
 * <p>Business rules encoded in an enum deserve tests exactly as much as rules in a service.
 * These are the tests that stop a well-meaning future change ("let admins reopen a delivered
 * move") from silently corrupting reporting.
 */
@DisplayName("BookingStatus state machine")
class BookingStatusTest {

    /**
     * {@code @ParameterizedTest} with {@code @CsvSource} runs the same assertion over a table
     * of cases, and each row is reported as its own test. Far better than one test with ten
     * asserts, where the first failure hides the rest.
     */
    @ParameterizedTest(name = "{0} -> {1} should be {2}")
    @CsvSource({
            "PENDING,    CONFIRMED,  true",
            "PENDING,    CANCELLED,  true",
            "PENDING,    IN_TRANSIT, false",   // cannot skip confirmation
            "PENDING,    DELIVERED,  false",
            "CONFIRMED,  IN_TRANSIT, true",
            "CONFIRMED,  CANCELLED,  true",
            "CONFIRMED,  PENDING,    false",   // no going backwards
            "IN_TRANSIT, DELIVERED,  true",
            "IN_TRANSIT, CANCELLED,  false",   // goods are already on the truck
            "DELIVERED,  PENDING,    false",
            "DELIVERED,  CANCELLED,  false",
            "CANCELLED,  CONFIRMED,  false"    // a cancelled booking is re-created, not revived
    })
    void transitionRules(BookingStatus from, BookingStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }

    @Test
    @DisplayName("DELIVERED and CANCELLED are terminal")
    void terminalStates() {
        assertThat(BookingStatus.DELIVERED.isTerminal()).isTrue();
        assertThat(BookingStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(BookingStatus.PENDING.isTerminal()).isFalse();
        assertThat(BookingStatus.CONFIRMED.isTerminal()).isFalse();
        assertThat(BookingStatus.IN_TRANSIT.isTerminal()).isFalse();
    }

    /**
     * {@code @EnumSource} feeds every constant in, so adding a sixth status automatically
     * extends this test. That is the kind of test that keeps paying off after you write it.
     */
    @ParameterizedTest
    @EnumSource(BookingStatus.class)
    @DisplayName("every status has a label and a customer-facing message")
    void everyStatusIsPresentable(BookingStatus status) {
        assertThat(status.getLabel()).isNotBlank();
        assertThat(status.getCustomerMessage()).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(BookingStatus.class)
    @DisplayName("no status can transition to itself through the rule table")
    void noSelfTransitions(BookingStatus status) {
        // Booking.changeStatus treats "same status" as a no-op before consulting these rules,
        // so the rule table itself should never list a self-transition.
        assertThat(status.canTransitionTo(status)).isFalse();
    }

    @Test
    @DisplayName("allowedNextStates is defensive - callers cannot mutate the rules")
    void allowedNextStatesIsImmutable() {
        var next = BookingStatus.PENDING.allowedNextStates();
        assertThat(next).containsExactlyInAnyOrder(BookingStatus.CONFIRMED, BookingStatus.CANCELLED);

        // Set.copyOf returns an immutable set, so a caller that tried to add a state gets an
        // exception rather than silently rewriting the workflow for the whole application.
        // Asserting on the BEHAVIOUR, not on the implementation class name, keeps this test
        // valid across JDK versions.
        assertThatThrownBy(() -> next.add(BookingStatus.DELIVERED))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
