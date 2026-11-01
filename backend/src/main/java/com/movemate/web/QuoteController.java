package com.movemate.web;

import com.movemate.dto.QuoteRequest;
import com.movemate.dto.QuoteResponse;
import com.movemate.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The instant quote calculator.
 */
@RestController
@RequestMapping("/api/v1/quotes")
@Tag(name = "Quotes", description = "Instant, itemised price estimates")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    /**
     * Returns an itemised estimate. Public: asking someone to create an account before showing
     * a price is the fastest way to lose the lead.
     *
     * <p>POST for a read-only operation is the right call here, and worth being able to defend:
     * the input is a structured object with ten fields, which does not belong in a query string
     * (length limits, no nesting, and it would be logged in full by every proxy). The REST
     * purist answer is GET with query params; the practical industry answer for a calculator is
     * POST. What matters is that it is idempotent and stores nothing.
     */
    @PostMapping("/estimate")
    @Operation(summary = "Calculate an itemised estimate without creating a booking")
    public ResponseEntity<QuoteResponse> estimate(@Valid @RequestBody QuoteRequest request) {
        return ResponseEntity.ok(quoteService.estimate(request));
    }
}
