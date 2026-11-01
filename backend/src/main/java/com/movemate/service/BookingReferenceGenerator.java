package com.movemate.service;

import com.movemate.repository.BookingRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;

/**
 * Generates the public booking reference, e.g. {@code MM-2026-7K4F2Q}.
 *
 * <p>Design requirements, and how each is met:
 * <ul>
 *   <li><b>Not guessable.</b> Sequential ids let anyone enumerate every booking. We use
 *       {@link SecureRandom} (a cryptographically strong PRNG) rather than
 *       {@code java.util.Random}, whose output is predictable from a few samples.</li>
 *   <li><b>Readable over the phone.</b> The alphabet excludes I, O, 0 and 1, which customers
 *       and agents mishear and mistype constantly. 32 symbols to the power of 6 is about one
 *       billion combinations - plenty, while staying short.</li>
 *   <li><b>Sortable by eye.</b> The year prefix tells support how old a booking is instantly.</li>
 *   <li><b>Actually unique.</b> We re-roll on collision, and the database still has a UNIQUE
 *       constraint as the final authority.</li>
 * </ul>
 */
@Component
public class BookingReferenceGenerator {

    /** No I, O, 0 or 1 - the four characters humans confuse most. */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final String PREFIX = "MM";
    /** Give up after this many collisions rather than loop forever. */
    private static final int MAX_ATTEMPTS = 10;

    /** SecureRandom is thread-safe, so one shared instance is correct and cheaper. */
    private final SecureRandom random = new SecureRandom();
    private final BookingRepository bookingRepository;

    public BookingReferenceGenerator(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    /**
     * @return a reference that did not exist when this method checked
     * @throws IllegalStateException if a unique value could not be found, which would mean
     *         something is badly wrong rather than bad luck
     */
    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = build();
            // Note the honest limitation: between this check and the INSERT, another request
            // could take the same value. That race is why the UNIQUE constraint exists - the
            // database rejects the duplicate and GlobalExceptionHandler turns it into a 409.
            // "Check then act" is never atomic on its own.
            if (!bookingRepository.existsByReference(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Could not generate a unique booking reference after " + MAX_ATTEMPTS + " attempts");
    }

    private String build() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            // nextInt(bound) is uniformly distributed over [0, bound).
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return "%s-%d-%s".formatted(PREFIX, LocalDate.now().getYear(), code);
    }
}
