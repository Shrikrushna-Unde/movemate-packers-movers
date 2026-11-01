package com.movemate.web;

import com.movemate.domain.BookingStatus;
import com.movemate.domain.MoveType;
import com.movemate.dto.AdminStatsResponse;
import com.movemate.dto.BookingResponse;
import com.movemate.dto.BookingStatusUpdateRequest;
import com.movemate.dto.ContactMessageResponse;
import com.movemate.dto.MovingServiceResponse;
import com.movemate.dto.MovingServiceUpsertRequest;
import com.movemate.dto.PageResponse;
import com.movemate.dto.TestimonialResponse;
import com.movemate.service.AdminStatsService;
import com.movemate.service.BookingService;
import com.movemate.service.CatalogService;
import com.movemate.service.ContactService;
import com.movemate.service.TestimonialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Every privileged operation, behind one URL prefix.
 *
 * <p>Two independent layers protect this class:
 * <ol>
 *   <li>the URL rule {@code requestMatchers("/api/v1/admin/**").hasRole("ADMIN")} in
 *       {@link com.movemate.config.SecurityConfig};</li>
 *   <li>{@code @PreAuthorize} at class level here, which applies to every method.</li>
 * </ol>
 * Belt and braces on purpose: if a future refactor moves these endpoints to a different path
 * and nobody updates the URL rule, the annotation still blocks a non-admin caller. Security
 * controls should fail closed, and redundancy is how you get there.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Dashboard, booking workflow, enquiries, reviews and catalogue management")
public class AdminController {

    private final BookingService bookingService;
    private final ContactService contactService;
    private final TestimonialService testimonialService;
    private final CatalogService catalogService;
    private final AdminStatsService adminStatsService;

    public AdminController(BookingService bookingService,
                           ContactService contactService,
                           TestimonialService testimonialService,
                           CatalogService catalogService,
                           AdminStatsService adminStatsService) {
        this.bookingService = bookingService;
        this.contactService = contactService;
        this.testimonialService = testimonialService;
        this.catalogService = catalogService;
        this.adminStatsService = adminStatsService;
    }

    // ------------------------------------------------------------------
    //  Dashboard
    // ------------------------------------------------------------------

    @GetMapping("/stats")
    @Operation(summary = "Counts and totals for the dashboard tiles")
    public ResponseEntity<AdminStatsResponse> stats() {
        return ResponseEntity.ok(adminStatsService.collect());
    }

    // ------------------------------------------------------------------
    //  Bookings
    // ------------------------------------------------------------------

    /**
     * Paged, filterable booking list.
     *
     * <p>{@code required = false} makes each filter optional, so one endpoint serves "show
     * everything", "show only pending", and "find the Deshmukh booking".
     *
     * <p>{@code Pageable} is resolved by Spring Data's argument resolver from
     * {@code ?page=0&size=20&sort=createdAt,desc} - we never parse those parameters ourselves.
     * {@code @PageableDefault} supplies sane defaults so a caller who omits them gets the
     * newest 20 rather than the entire table in insertion order.
     */
    @GetMapping("/bookings")
    @Operation(summary = "Search bookings by status, move type or free text")
    public ResponseEntity<PageResponse<BookingResponse>> bookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) MoveType moveType,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(bookingService.search(status, moveType, query, pageable));
    }

    @GetMapping("/bookings/{reference}")
    @Operation(summary = "One booking by its public reference")
    public ResponseEntity<BookingResponse> booking(@PathVariable String reference) {
        return ResponseEntity.ok(bookingService.findByReference(reference));
    }

    /**
     * Advances the booking workflow.
     *
     * <p>PATCH, not PUT: we are modifying one aspect of the resource, not replacing it. PUT
     * implies the body is the complete new state, which would mean sending every address field
     * just to mark a move as delivered.
     *
     * <p>An illegal transition (DELIVERED back to PENDING) is rejected by the entity and
     * surfaces as a 400 with a readable message.
     */
    @PatchMapping("/bookings/{id}/status")
    @Operation(summary = "Change booking status, honouring the allowed state transitions")
    public ResponseEntity<BookingResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody BookingStatusUpdateRequest request) {
        return ResponseEntity.ok(bookingService.updateStatus(id, request));
    }

    // ------------------------------------------------------------------
    //  Enquiries
    // ------------------------------------------------------------------

    @GetMapping("/enquiries")
    @Operation(summary = "Contact-form inbox")
    public ResponseEntity<PageResponse<ContactMessageResponse>> enquiries(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(contactService.findAllForAdmin(pageable));
    }

    /**
     * Marks an enquiry handled or re-opens it.
     *
     * <p>The desired value is an explicit parameter rather than a toggle, which makes the call
     * idempotent: sending it twice leaves the same state. A toggle endpoint called twice by a
     * retrying client would undo itself.
     */
    @PatchMapping("/enquiries/{id}/handled")
    @Operation(summary = "Mark an enquiry as handled (or re-open it)")
    public ResponseEntity<ContactMessageResponse> setEnquiryHandled(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean handled) {
        return ResponseEntity.ok(contactService.setHandled(id, handled));
    }

    // ------------------------------------------------------------------
    //  Testimonials
    // ------------------------------------------------------------------

    @GetMapping("/testimonials")
    @Operation(summary = "All reviews, including those awaiting moderation")
    public ResponseEntity<PageResponse<TestimonialResponse>> testimonials(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(testimonialService.findAllForAdmin(pageable));
    }

    @PatchMapping("/testimonials/{id}/approval")
    @Operation(summary = "Approve or un-approve a review")
    public ResponseEntity<TestimonialResponse> setApproval(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean approved) {
        return ResponseEntity.ok(testimonialService.setApproved(id, approved));
    }

    /**
     * Deletes a review.
     *
     * <p>204 No Content is the correct status for a successful delete with nothing to return.
     * Returning 200 with an empty body, or a {@code {"success": true}} envelope, is a very
     * common and avoidable REST smell.
     */
    @DeleteMapping("/testimonials/{id}")
    @Operation(summary = "Delete a review permanently")
    public ResponseEntity<Void> deleteTestimonial(@PathVariable Long id) {
        testimonialService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    //  Service catalogue
    // ------------------------------------------------------------------

    @GetMapping("/services")
    @Operation(summary = "All catalogue entries, including hidden ones")
    public ResponseEntity<List<MovingServiceResponse>> services() {
        return ResponseEntity.ok(catalogService.findAllForAdmin());
    }

    @PostMapping("/services")
    @Operation(summary = "Add a catalogue entry")
    public ResponseEntity<MovingServiceResponse> createService(
            @Valid @RequestBody MovingServiceUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.create(request));
    }

    /** PUT, because the body carries the complete new state of the entry. */
    @PutMapping("/services/{id}")
    @Operation(summary = "Replace a catalogue entry")
    public ResponseEntity<MovingServiceResponse> updateService(
            @PathVariable Long id,
            @Valid @RequestBody MovingServiceUpsertRequest request) {
        return ResponseEntity.ok(catalogService.update(id, request));
    }

    @PatchMapping("/services/{id}/toggle")
    @Operation(summary = "Show or hide a catalogue entry (soft delete)")
    public ResponseEntity<MovingServiceResponse> toggleService(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.toggleActive(id));
    }
}
