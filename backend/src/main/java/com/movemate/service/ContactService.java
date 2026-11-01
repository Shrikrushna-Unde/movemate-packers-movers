package com.movemate.service;

import com.movemate.domain.ContactMessage;
import com.movemate.dto.ContactMessageResponse;
import com.movemate.dto.ContactRequest;
import com.movemate.dto.PageResponse;
import com.movemate.exception.BadRequestException;
import com.movemate.exception.ResourceNotFoundException;
import com.movemate.repository.ContactMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;

/**
 * Handles "Contact us" enquiries.
 */
@Service
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    /** At most this many messages from one email address per window. */
    private static final int MAX_MESSAGES_PER_WINDOW = 5;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);

    private final ContactMessageRepository repository;

    public ContactService(ContactMessageRepository repository) {
        this.repository = repository;
    }

    /**
     * Stores an enquiry.
     *
     * <p>This is an unauthenticated write endpoint, so it gets a rate limit. The check is
     * per-email and backed by an indexed COUNT, which is cheap. Be honest about its limits in
     * an interview: an attacker can simply change the email address, so this stops accidental
     * double-submits and lazy spam, not a determined flood. The real answers are a reverse-proxy
     * rate limit, a CAPTCHA, or Bucket4j keyed on IP.
     */
    @Transactional
    public ContactMessageResponse submit(ContactRequest request) {
        String email = request.email().trim().toLowerCase();
        Instant windowStart = Instant.now().minus(RATE_LIMIT_WINDOW);

        if (repository.countByEmailAndCreatedAtAfter(email, windowStart) >= MAX_MESSAGES_PER_WINDOW) {
            throw new BadRequestException(
                    "You have already sent several messages recently. Our team will reply shortly.");
        }

        ContactMessage message = new ContactMessage(
                request.name().trim(),
                email,
                StringUtils.hasText(request.phone()) ? request.phone().trim() : null,
                request.subject().trim(),
                request.message().trim());

        ContactMessage saved = repository.save(message);
        log.info("New contact enquiry stored with id {}", saved.getId());

        // Where an email or WhatsApp notification would go. Persisting FIRST and notifying
        // second is the right order: a failed notification must never lose the lead. In
        // production that notification belongs in an @Async method or a queue, so a slow SMTP
        // server cannot make the customer's form submission time out.

        return ContactMessageResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ContactMessageResponse> findAllForAdmin(Pageable pageable) {
        return PageResponse.from(
                repository.findAllByOrderByCreatedAtDesc(pageable),
                ContactMessageResponse::from);
    }

    @Transactional
    public ContactMessageResponse setHandled(Long id, boolean handled) {
        ContactMessage message = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry", "id", id));
        message.setHandled(handled);
        return ContactMessageResponse.from(message);
    }
}
