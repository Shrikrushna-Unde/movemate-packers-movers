package com.movemate.dto;

import com.movemate.domain.HomeSize;
import com.movemate.domain.MoveType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The payload for creating a booking.
 *
 * <p>It intentionally does NOT accept a price. The browser shows an estimate from
 * {@code POST /quotes/estimate}, but the server recalculates the amount from these inputs
 * before saving. If we trusted a client-supplied total, anyone could book a 3 BHK intercity
 * move for 1 rupee with a modified request. <b>Never trust the client for money.</b>
 * That single sentence is worth memorising for interviews.
 */
public record BookingRequest(

        @NotBlank(message = "Your name is required")
        @Size(max = 120, message = "Name is too long")
        String customerName,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        String customerEmail,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
        String customerPhone,

        @NotNull(message = "Move type is required")
        MoveType moveType,

        HomeSize homeSize,

        @NotBlank(message = "Pickup address is required")
        @Size(max = 255, message = "Pickup address is too long")
        String pickupAddress,

        @NotBlank(message = "Pickup city is required")
        @Size(max = 80)
        String pickupCity,

        /* Indian PIN codes are exactly 6 digits and never start with 0. */
        @Pattern(regexp = "^[1-9]\\d{5}$", message = "Enter a valid 6-digit PIN code")
        String pickupPincode,

        @NotBlank(message = "Drop address is required")
        @Size(max = 255, message = "Drop address is too long")
        String dropAddress,

        @NotBlank(message = "Drop city is required")
        @Size(max = 80)
        String dropCity,

        @Pattern(regexp = "^[1-9]\\d{5}$", message = "Enter a valid 6-digit PIN code")
        String dropPincode,

        @NotNull(message = "Distance is required")
        @Min(value = 0, message = "Distance cannot be negative")
        @Max(value = 4000, message = "For moves beyond 4000 km please contact us directly")
        Integer distanceKm,

        /*
         * @FutureOrPresent, not @Future: a same-day emergency move is a real request, and
         * @Future would reject today's date.
         */
        @NotNull(message = "Move date is required")
        @FutureOrPresent(message = "Move date cannot be in the past")
        LocalDate moveDate,

        @Min(0) @Max(60) int pickupFloor,
        @Min(0) @Max(60) int dropFloor,

        boolean liftAvailable,
        boolean packingRequired,
        boolean insuranceRequired,

        @DecimalMin(value = "0", message = "Declared value cannot be negative")
        @DecimalMax(value = "100000000", message = "Declared value is unrealistically high")
        BigDecimal declaredValue,

        @Min(0) @Max(365) int storageDays,

        @Size(max = 500, message = "Please keep notes under 500 characters")
        String customerNotes
) {

    /**
     * Projects the pricing-relevant subset of this request onto a {@link QuoteRequest}.
     *
     * <p>This is how we guarantee the booking price and the displayed estimate come from the
     * same code path. The alternative - duplicating the formula for bookings - is how the
     * quote page and the invoice start disagreeing in production.
     */
    public QuoteRequest toQuoteRequest() {
        return new QuoteRequest(
                moveType,
                homeSize,
                distanceKm,
                pickupFloor,
                dropFloor,
                liftAvailable,
                packingRequired,
                insuranceRequired,
                declaredValue,
                storageDays
        );
    }
}
