package com.movemate.web;

import com.movemate.dto.TestimonialRequest;
import com.movemate.dto.TestimonialResponse;
import com.movemate.security.AuthenticatedUser;
import com.movemate.service.TestimonialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Customer reviews.
 *
 * <p>Note how the two methods on the same URL have different access rules: GET is public
 * (anyone can read approved reviews) while POST requires a login. That split is declared in
 * {@link com.movemate.config.SecurityConfig} by HTTP method, which is cleaner than splitting
 * the controller in two.
 */
@RestController
@RequestMapping("/api/v1/testimonials")
@Tag(name = "Testimonials", description = "Read approved reviews, submit your own")
public class TestimonialController {

    private final TestimonialService testimonialService;

    public TestimonialController(TestimonialService testimonialService) {
        this.testimonialService = testimonialService;
    }

    @GetMapping
    @Operation(summary = "Approved reviews, newest first")
    public ResponseEntity<List<TestimonialResponse>> approved() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                .body(testimonialService.findApproved());
    }

    /**
     * Submits a review for moderation.
     *
     * <p>Returns 201 with {@code approved: false}, so the UI can honestly say "thanks, your
     * review will appear once our team checks it" instead of pretending it is already live and
     * leaving the customer confused when they cannot find it.
     */
    @PostMapping
    @Operation(summary = "Submit a review (requires sign-in; appears after admin approval)")
    public ResponseEntity<TestimonialResponse> submit(
            @Valid @RequestBody TestimonialRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        TestimonialResponse created = testimonialService.submit(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
