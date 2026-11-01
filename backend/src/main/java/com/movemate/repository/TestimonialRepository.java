package com.movemate.repository;

import com.movemate.domain.Testimonial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access for customer reviews.
 */
@Repository
public interface TestimonialRepository extends JpaRepository<Testimonial, Long> {

    /** Public homepage feed: approved reviews only, newest first. */
    List<Testimonial> findByApprovedTrueOrderByCreatedAtDesc();

    /** Admin moderation queue, paged. */
    Page<Testimonial> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByApprovedFalse();

    /**
     * Average star rating across approved reviews, for the "4.8 out of 5" badge.
     *
     * <p>Returns {@code Double} (the wrapper) rather than {@code double}: on an empty table
     * AVG returns NULL, and a primitive return type would throw during unboxing. The service
     * layer decides what to show in that case.
     */
    @Query("select avg(t.rating) from Testimonial t where t.approved = true")
    Double findAverageRating();
}
