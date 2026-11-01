package com.movemate.service;

import com.movemate.domain.BookingStatus;
import com.movemate.dto.AdminStatsResponse;
import com.movemate.repository.BookingRepository;
import com.movemate.repository.ContactMessageRepository;
import com.movemate.repository.TestimonialRepository;
import com.movemate.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregates the numbers for the admin dashboard.
 *
 * <p>Every figure here is produced by a COUNT or SUM executed in MySQL. The alternative -
 * {@code findAll()} then counting in Java - would transfer every row over the network to
 * compute one integer. On a dashboard that loads on every admin page view, that difference is
 * the gap between a snappy panel and a timeout.
 *
 * <p>Honest tradeoff: this issues seven small queries. At real scale you would replace them
 * with one {@code GROUP BY status} projection, or cache the whole response for a minute. The
 * point worth making in an interview is knowing that the N queries are there.
 */
@Service
public class AdminStatsService {

    private final BookingRepository bookingRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final TestimonialRepository testimonialRepository;
    private final UserRepository userRepository;

    public AdminStatsService(BookingRepository bookingRepository,
                             ContactMessageRepository contactMessageRepository,
                             TestimonialRepository testimonialRepository,
                             UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.contactMessageRepository = contactMessageRepository;
        this.testimonialRepository = testimonialRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse collect() {

        // LinkedHashMap, not HashMap: it preserves insertion order, so the JSON comes back in
        // lifecycle order (PENDING, CONFIRMED, IN_TRANSIT, ...) and the dashboard tiles do not
        // shuffle between page loads. A small thing that makes a UI feel finished.
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (BookingStatus status : BookingStatus.values()) {
            byStatus.put(status.name(), bookingRepository.countByStatus(status));
        }

        BigDecimal pipeline = bookingRepository
                .sumEstimatedAmountExcludingStatus(BookingStatus.CANCELLED);

        // May be null when no reviews are approved yet; the DTO documents that and the
        // frontend hides the badge rather than printing "null stars".
        Double averageRating = testimonialRepository.findAverageRating();

        return new AdminStatsResponse(
                bookingRepository.count(),
                byStatus,
                pipeline,
                contactMessageRepository.countByHandledFalse(),
                testimonialRepository.countByApprovedFalse(),
                userRepository.count(),
                averageRating);
    }
}
