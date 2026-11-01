package com.movemate.service;

import com.movemate.domain.Booking;
import com.movemate.domain.BookingStatus;
import com.movemate.domain.MoveType;
import com.movemate.domain.User;
import com.movemate.dto.BookingRequest;
import com.movemate.dto.BookingResponse;
import com.movemate.dto.BookingStatusUpdateRequest;
import com.movemate.dto.PageResponse;
import com.movemate.dto.QuoteResponse;
import com.movemate.exception.ResourceNotFoundException;
import com.movemate.repository.BookingRepository;
import com.movemate.repository.BookingSpecifications;
import com.movemate.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Everything that happens to a booking.
 *
 * <p>This class is the reason the controllers are thin. A controller's job is HTTP: read the
 * request, call one method, return a status code. All the rules - recalculating the price,
 * generating a reference, enforcing the status workflow, deciding who may see what - live
 * here, where they can be unit-tested and reused by a future scheduled job or admin CLI.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final QuoteService quoteService;
    private final BookingReferenceGenerator referenceGenerator;

    public BookingService(BookingRepository bookingRepository,
                          UserRepository userRepository,
                          QuoteService quoteService,
                          BookingReferenceGenerator referenceGenerator) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.quoteService = quoteService;
        this.referenceGenerator = referenceGenerator;
    }

    /**
     * Creates a booking.
     *
     * @param request the move details
     * @param userId  the logged-in user's id, or {@code null} for a guest booking
     */
    @Transactional
    public BookingResponse create(BookingRequest request, Long userId) {

        // THE important line in this class: the price is recalculated server-side from the
        // submitted parameters. The browser's estimate is a display convenience; this is the
        // number that gets stored. Any client-supplied total would be an open invitation to
        // book a 50,000 rupee move for 1 rupee with a modified HTTP request.
        QuoteResponse quote = quoteService.estimate(request.toQuoteRequest());

        Booking booking = new Booking();
        booking.setReference(referenceGenerator.generate());

        // A guest booking has no user. Looking the user up by id (from the verified JWT) rather
        // than trusting an id in the request body is what stops one customer attaching a
        // booking to another customer's account.
        if (userId != null) {
            User owner = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            booking.setUser(owner);
        }

        booking.setCustomerName(request.customerName().trim());
        booking.setCustomerEmail(request.customerEmail().trim().toLowerCase());
        booking.setCustomerPhone(request.customerPhone().trim());
        booking.setMoveType(request.moveType());
        booking.setHomeSize(request.homeSize());
        booking.setPickupAddress(request.pickupAddress().trim());
        booking.setPickupCity(request.pickupCity().trim());
        booking.setPickupPincode(emptyToNull(request.pickupPincode()));
        booking.setDropAddress(request.dropAddress().trim());
        booking.setDropCity(request.dropCity().trim());
        booking.setDropPincode(emptyToNull(request.dropPincode()));
        booking.setDistanceKm(request.distanceKm());
        booking.setMoveDate(request.moveDate());
        booking.setPickupFloor(request.pickupFloor());
        booking.setDropFloor(request.dropFloor());
        booking.setLiftAvailable(request.liftAvailable());
        booking.setPackingRequired(request.packingRequired());
        booking.setInsuranceRequired(request.insuranceRequired());
        booking.setDeclaredValue(request.declaredValue());
        booking.setStorageDays(request.storageDays());
        booking.setEstimatedAmount(quote.total());
        booking.setCustomerNotes(emptyToNull(request.customerNotes()));
        booking.setStatus(BookingStatus.PENDING);

        Booking saved = bookingRepository.save(booking);

        // Log the reference and the amount, never the customer's address or phone number -
        // platform logs are not a safe place for personal data.
        log.info("Created booking {} for {} at an estimate of {}",
                saved.getReference(), saved.getMoveType(), saved.getEstimatedAmount());

        return BookingResponse.from(saved);
    }

    /**
     * Guest tracking: reference AND phone must both match.
     *
     * <p>Requiring two values turns the reference into a shared secret. With the reference
     * alone, anyone who glanced at a printed invoice could read the customer's home address.
     * Returning 404 (rather than 403) for a wrong phone also avoids confirming that the
     * reference exists.
     */
    @Transactional(readOnly = true)
    public BookingResponse track(String reference, String phone) {
        return bookingRepository
                .findByReferenceAndCustomerPhone(reference.trim().toUpperCase(), phone.trim())
                .map(BookingResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No booking found for that reference and phone number"));
    }

    /** A logged-in customer's own bookings. The id comes from the token, never from a parameter. */
    @Transactional(readOnly = true)
    public List<BookingResponse> findMyBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(BookingResponse::from)
                .toList();
    }

    /**
     * Admin list with optional filters and paging.
     *
     * <p>The three specifications are composed with {@code and()}; each returns null when its
     * filter is absent, and Spring Data drops null predicates. So "no filters" produces a
     * plain {@code SELECT ... LIMIT ?} with no dead WHERE clause.
     *
     * <p>Paging is not optional at this layer. {@code findAll()} on a table that grows every
     * day is the most common way a working application falls over in its second year.
     */
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> search(BookingStatus status,
                                                MoveType moveType,
                                                String query,
                                                Pageable pageable) {
        Specification<Booking> spec = Specification
                .where(BookingSpecifications.hasStatus(status))
                .and(BookingSpecifications.hasMoveType(moveType))
                .and(BookingSpecifications.matchesText(query));

        Page<Booking> page = bookingRepository.findAll(spec, pageable);
        // The method reference is the per-row mapper; see PageResponse.from.
        return PageResponse.from(page, BookingResponse::from);
    }

    @Transactional(readOnly = true)
    public BookingResponse findByReference(String reference) {
        return bookingRepository.findByReference(reference.trim().toUpperCase())
                .map(BookingResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "reference", reference));
    }

    /**
     * Admin action: advance the workflow.
     *
     * <p>Note that this method does not check whether the transition is legal - it calls
     * {@link Booking#changeStatus} and lets the entity enforce its own rules. Putting the check
     * here instead would mean every other caller (a test, a future bulk-update job) could
     * bypass it.
     *
     * <p>There is no explicit {@code save()} either. Inside a transaction the entity is
     * managed, so Hibernate's dirty checking flushes the change on commit. Calling save()
     * would work but would suggest the author does not know how JPA persistence contexts
     * behave - worth being able to explain either way.
     */
    @Transactional
    public BookingResponse updateStatus(Long id, BookingStatusUpdateRequest request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));

        BookingStatus previous = booking.getStatus();
        booking.changeStatus(request.status());

        if (StringUtils.hasText(request.adminNotes())) {
            booking.setAdminNotes(request.adminNotes().trim());
        }

        log.info("Booking {} moved from {} to {}", booking.getReference(), previous, booking.getStatus());
        return BookingResponse.from(booking);
    }

    /**
     * Normalises an absent optional field to null.
     *
     * <p>HTML forms submit "" for untouched inputs. Storing an empty string means
     * {@code WHERE pincode IS NULL} misses those rows and every read has to check for both -
     * so we pick one representation for "no value" and enforce it at the boundary.
     */
    private static String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
