package com.movemate.dto;

import com.movemate.domain.ContactMessage;

import java.time.Instant;

/**
 * An enquiry as the admin inbox sees it. Admin-only, so every field can be exposed.
 */
public record ContactMessageResponse(
        Long id,
        String name,
        String email,
        String phone,
        String subject,
        String message,
        boolean handled,
        Instant createdAt
) {

    public static ContactMessageResponse from(ContactMessage m) {
        return new ContactMessageResponse(
                m.getId(),
                m.getName(),
                m.getEmail(),
                m.getPhone(),
                m.getSubject(),
                m.getMessage(),
                m.isHandled(),
                m.getCreatedAt()
        );
    }
}
