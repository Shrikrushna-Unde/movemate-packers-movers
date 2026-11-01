package com.movemate.web;

import com.movemate.dto.ContactMessageResponse;
import com.movemate.dto.ContactRequest;
import com.movemate.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * The "Contact us" form.
 */
@RestController
@RequestMapping("/api/v1/contact")
@Tag(name = "Contact", description = "Public enquiry form")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    /**
     * Stores an enquiry and returns a short acknowledgement.
     *
     * <p>The response deliberately does NOT echo the stored message back. There is no reason
     * for a public endpoint to reflect user input verbatim, and not doing so removes a whole
     * class of reflected-content concerns. The id is returned so support can reference it.
     */
    @PostMapping
    @Operation(summary = "Send an enquiry to the MoveMate team")
    public ResponseEntity<Map<String, Object>> submit(@Valid @RequestBody ContactRequest request) {
        ContactMessageResponse saved = contactService.submit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", saved.id(),
                "message", "Thank you for reaching out. Our team will contact you within 2 hours."));
    }
}
