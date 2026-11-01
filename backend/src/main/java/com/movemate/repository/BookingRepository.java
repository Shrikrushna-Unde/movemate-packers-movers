package com.movemate.repository;

import com.movemate.domain.Booking;
import com.movemate.domain.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link Booking}.
 *
 * <p>Note the second interface: {@link JpaSpecificationExecutor}. The admin list filters by
 * status AND/OR a free-text search, and either can be absent. Three options exist:
 * <ol>
 *   <li>Write four query methods for the four combinations - doubles with every new filter.</li>
 *   <li>One JPQL query with {@code (:status is null or b.status = :status)} - works, but it
 *       forces the database to evaluate dead branches and gets ugly fast.</li>
 *   <li>Criteria API Specifications, composed at runtime - what we do. See
 *       {@link BookingSpecifications}.</li>
 * </ol>
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    /** Lookup by the public reference, used by admin detail view and status updates. */
    Optional<Booking> findByReference(String reference);

    /**
     * Guest tracking. Requiring BOTH the reference and the phone number turns the reference
     * into a weak shared secret: knowing (or guessing) a reference alone is not enough to read
     * somebody else's address. This is a deliberate authorisation decision, not a formality.
     */
    Optional<Booking> findByReferenceAndCustomerPhone(String reference, String customerPhone);

    boolean existsByReference(String reference);

    /** A logged-in customer's own bookings, newest first. */
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Paged variant for a customer with a long history. */
    Page<Booking> findByUserId(Long userId, Pageable pageable);

    /** Admin dashboard tile: how many bookings sit in each state. */
    long countByStatus(BookingStatus status);

    /**
     * Total value of everything not cancelled.
     *
     * <p>{@code coalesce(..., 0)} matters: {@code SUM} over zero rows returns NULL in SQL, and
     * unboxing that NULL into a primitive would throw. Aggregating in the database is also far
     * cheaper than loading every row into Java and summing it there.
     */
    @Query("select coalesce(sum(b.estimatedAmount), 0) from Booking b where b.status <> :excluded")
    BigDecimal sumEstimatedAmountExcludingStatus(@Param("excluded") BookingStatus excluded);
}
