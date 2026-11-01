package com.movemate.dto;

import com.movemate.domain.HomeSize;
import com.movemate.domain.MoveType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Everything the pricing engine needs to produce an estimate.
 *
 * <p>Jackson maps the JSON string {@code "TWO_BHK"} onto the {@link HomeSize} enum constant
 * automatically. An unknown value produces a 400 before any of our code runs, which means the
 * API cannot be called with a size we have no price for - the type system is doing validation
 * work for us.
 *
 * <p>{@code homeSize} has no {@code @NotNull}: it is required for household moves and
 * meaningless for a car transport. Bean Validation cannot express "required depending on
 * another field" without a custom class-level validator, so that one conditional rule lives in
 * {@code QuoteService}. Keeping the easy 90% declarative and the conditional 10% in code is a
 * pragmatic split worth explaining out loud.
 *
 * <p>Boxed {@code Integer} for {@code distanceKm} so that a missing field is null and
 * {@code @NotNull} can reject it. With a primitive {@code int}, Jackson would silently default
 * it to 0 and the customer would get a local-move price for an intercity move.
 */
public record QuoteRequest(

        @NotNull(message = "Move type is required")
        MoveType moveType,

        HomeSize homeSize,

        @NotNull(message = "Distance is required")
        @Min(value = 0, message = "Distance cannot be negative")
        @Max(value = 4000, message = "For moves beyond 4000 km please contact us directly")
        Integer distanceKm,

        @Min(value = 0, message = "Floor cannot be negative")
        @Max(value = 60, message = "Please contact us for floors above 60")
        int pickupFloor,

        @Min(value = 0, message = "Floor cannot be negative")
        @Max(value = 60, message = "Please contact us for floors above 60")
        int dropFloor,

        boolean liftAvailable,

        boolean packingRequired,

        boolean insuranceRequired,

        /*
         * Optional. Required only when insuranceRequired is true - again checked in the service.
         * DecimalMin/DecimalMax exist because BigDecimal is not an integer type, so @Min/@Max
         * are the wrong annotations for it.
         */
        @DecimalMin(value = "0", message = "Declared value cannot be negative")
        @DecimalMax(value = "100000000", message = "Declared value is unrealistically high")
        BigDecimal declaredValue,

        @Min(value = 0, message = "Storage days cannot be negative")
        @Max(value = 365, message = "For storage over a year please contact us directly")
        int storageDays
) {
}
