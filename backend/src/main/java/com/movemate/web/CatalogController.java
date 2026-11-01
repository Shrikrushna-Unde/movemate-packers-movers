package com.movemate.web;

import com.movemate.domain.BookingStatus;
import com.movemate.dto.MovingServiceResponse;
import com.movemate.dto.OptionResponse;
import com.movemate.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Public, read-only endpoints that drive the marketing site and the booking form.
 */
@RestController
@RequestMapping("/api/v1/catalog")
@Tag(name = "Catalogue", description = "Public service listings and form dropdown options")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * The service cards on the homepage and services page.
     *
     * <p>{@code Cache-Control: max-age=300, public} lets the browser and any CDN reuse this
     * response for five minutes. The catalogue changes a few times a year and is read on every
     * page load, so this is the single highest-value caching header in the API - and it costs
     * one line. {@code cachePublic()} is safe here precisely because the response contains no
     * per-user data.
     */
    @GetMapping("/services")
    @Operation(summary = "Active services, in display order")
    public ResponseEntity<List<MovingServiceResponse>> services() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(catalogService.findPublicServices());
    }

    /**
     * {@code @PathVariable} binds the {@code {slug}} segment to the parameter. The name matches,
     * so no explicit {@code @PathVariable("slug")} is needed.
     */
    @GetMapping("/services/{slug}")
    @Operation(summary = "One service by its URL slug")
    public ResponseEntity<MovingServiceResponse> service(@PathVariable String slug) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(catalogService.findPublicBySlug(slug));
    }

    /**
     * Dropdown options for the quote and booking forms, generated from the Java enums.
     *
     * <p>This endpoint exists so the React app never hardcodes a copy of {@code MoveType} or
     * {@code HomeSize}. Add a move type in Java and the form picks it up on the next load -
     * no frontend change, and no chance of the two lists drifting apart, which is the usual
     * outcome of copy-pasting enums into JavaScript.
     */
    @GetMapping("/options")
    @Operation(summary = "Enum-driven dropdown options for the booking form")
    public ResponseEntity<Map<String, Object>> options() {
        // LinkedHashMap to keep a stable key order in the JSON.
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("moveTypes", OptionResponse.moveTypes());
        options.put("homeSizes", OptionResponse.homeSizes());
        options.put("bookingStatuses", Arrays.stream(BookingStatus.values())
                .map(s -> new OptionResponse(s.name(), s.getLabel(), Map.of()))
                .toList());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(options);
    }
}
