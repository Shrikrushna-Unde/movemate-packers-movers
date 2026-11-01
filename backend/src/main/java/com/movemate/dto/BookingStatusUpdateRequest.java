package com.movemate.dto;

import com.movemate.domain.BookingStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Admin action: move a booking to the next state and optionally leave an internal note.
 *
 * <p>Modelled as its own small DTO instead of reusing {@link BookingRequest} because the two
 * operations are nothing alike. A single fat "update booking" DTO would let a status change
 * silently overwrite the customer's address, and every field would have to be optional -
 * which makes validation meaningless. Narrow DTOs per use case keep each endpoint honest.
 */
public record BookingStatusUpdateRequest(

        @NotNull(message = "Status is required")
        BookingStatus status,

        @Size(max = 500, message = "Notes must be under 500 characters")
        String adminNotes
) {
}
