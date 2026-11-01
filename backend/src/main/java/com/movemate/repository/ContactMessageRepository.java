package com.movemate.repository;

import com.movemate.domain.ContactMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Data access for contact-form enquiries.
 */
@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    /** Admin inbox, newest first, paged. */
    Page<ContactMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Unread badge count on the admin dashboard. */
    long countByHandledFalse();

    /**
     * Simple anti-spam helper: how many messages this email sent recently.
     *
     * <p>The service rejects a submission when this exceeds a threshold. It is rate limiting
     * by identity rather than by IP - crude, but it costs one indexed count and stops the
     * most common case of someone hammering the form.
     */
    long countByEmailAndCreatedAtAfter(String email, Instant since);
}
