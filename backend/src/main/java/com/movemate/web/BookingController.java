package com.movemate.web;

import com.movemate.dto.BookingRequest;
import com.movemate.dto.BookingResponse;
import com.movemate.security.AuthenticatedUser;
import com.movemate.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Customer-facing booking endpoints. Admin operations live in {@link AdminController}.
 */
@RestController
@RequestMapping("/api/v1/bookings")
// @Validated (Spring) is NOT the same as @Valid (Jakarta). @Valid cascades into a request
// BODY; @Validated on the class enables validation of individual method PARAMETERS such as
// the @NotBlank @RequestParam values below. Without it those annotations are silently ignored.
@Validated
@Tag(name = "Bookings", description = "Create a booking, track it, and list your own")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Creates a booking for the signed-in customer.
     *
     * <p>This endpoint requires authentication. It is deliberately absent from the permitAll
     * list in {@code SecurityConfig}, so it falls through to {@code anyRequest().authenticated()}
     * and an anonymous caller is rejected with 401 before this method is ever entered.
     *
     * <p>{@code currentUser} is therefore never null here. It also cannot be forged: the
     * account is taken from the VERIFIED JWT, never from a user id in the request body, which
     * is what stops one customer filing a booking against another customer's account.
     *
     * <p>201 Created with a {@code Location} header pointing at the tracking URL - the HTTP
     * contract for "I made you a new resource, here is where it lives".
     */
    @PostMapping
    @Operation(summary = "Create a booking (requires a signed-in customer)")
    public ResponseEntity<BookingResponse> create(
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        BookingResponse created = bookingService.create(request, currentUser.getId());

        // Builds an absolute URL from the current request, so it is correct behind a proxy on
        // Railway without hardcoding the host anywhere.
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/track")
                .queryParam("reference", created.reference())
                .build()
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    /**
     * Public tracking by reference + phone.
     *
     * <p>{@code @RequestParam} binds query-string values; both are required by default, so a
     * missing one produces a 400 rather than a confusing empty result. {@code @NotBlank}
     * additionally rejects {@code ?reference=&phone=}, which would otherwise reach the service
     * as empty strings.
     */
    @GetMapping("/track")
    @Operation(summary = "Track a booking using its reference and the phone number on it")
    public ResponseEntity<BookingResponse> track(
            @RequestParam @NotBlank String reference,
            @RequestParam @NotBlank String phone) {
        return ResponseEntity.ok(bookingService.track(reference, phone));
    }

    /**
     * The signed-in customer's own bookings.
     *
     * <p>Returns a bare list rather than a page. That is a judgement call: a customer has a
     * handful of moves in a lifetime, so paging would add complexity to both sides for no
     * benefit. The admin list, which grows without bound, IS paged. Choosing per endpoint
     * instead of applying one rule everywhere is the point.
     */
    @GetMapping("/me")
    @Operation(summary = "Bookings belonging to the signed-in customer")
    public ResponseEntity<List<BookingResponse>> myBookings(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(bookingService.findMyBookings(currentUser.getId()));
    }
}
