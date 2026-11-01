package com.movemate.repository;

import com.movemate.domain.Booking;
import com.movemate.domain.BookingStatus;
import com.movemate.domain.MoveType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Reusable, composable query fragments for the admin booking list.
 *
 * <p>A {@link Specification} is a lambda that receives the Criteria API triple
 * {@code (root, query, criteriaBuilder)} and returns a {@code Predicate} - one piece of a
 * WHERE clause. Because specifications compose with {@code and()} / {@code or()}, the service
 * can build exactly the filter the user asked for and nothing more.
 *
 * <p>Returning {@code null} from a specification is meaningful: Spring Data treats a null
 * predicate as "no restriction", which is how an absent filter disappears from the SQL
 * instead of becoming {@code 1 = 1}.
 *
 * <p>The class is final with a private constructor because it is a pure static factory -
 * nobody should ever instantiate or extend it.
 */
public final class BookingSpecifications {

    private BookingSpecifications() {
    }

    /** Restrict to one lifecycle state, or no restriction when {@code status} is null. */
    public static Specification<Booking> hasStatus(BookingStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    /** Restrict to one move type, or no restriction when {@code moveType} is null. */
    public static Specification<Booking> hasMoveType(MoveType moveType) {
        return (root, query, cb) -> moveType == null ? null : cb.equal(root.get("moveType"), moveType);
    }

    /**
     * Free-text search across the four fields a support agent actually has on a phone call:
     * reference, name, email, phone.
     *
     * <p>{@code lower(column) LIKE lower('%term%')} is a deliberately simple approach. Be
     * honest about its cost in an interview: a leading wildcard cannot use a B-tree index, so
     * this is a full scan. At tens of thousands of rows that is fine; beyond that you reach
     * for a MySQL FULLTEXT index or a search engine. Knowing the limit matters more than
     * pretending there is none.
     */
    public static Specification<Booking> matchesText(String term) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(term)) {
                return null;
            }
            String pattern = "%" + term.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("reference")), pattern),
                    cb.like(cb.lower(root.get("customerName")), pattern),
                    cb.like(cb.lower(root.get("customerEmail")), pattern),
                    cb.like(cb.lower(root.get("customerPhone")), pattern),
                    cb.like(cb.lower(root.get("pickupCity")), pattern),
                    cb.like(cb.lower(root.get("dropCity")), pattern)
            );
        };
    }
}
