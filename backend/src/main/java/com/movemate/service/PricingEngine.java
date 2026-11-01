package com.movemate.service;

import com.movemate.config.PricingProperties;
import com.movemate.domain.HomeSize;
import com.movemate.domain.MoveType;
import com.movemate.dto.QuoteRequest;
import com.movemate.dto.QuoteResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns move parameters into an itemised price.
 *
 * <p>This is the only place in the application that knows how a quote is calculated. It is a
 * {@code @Component} with no repository, no HTTP and no database: pure input to output. That
 * makes it trivially unit-testable (see {@code PricingEngineTest}) and is the single most
 * important structural idea in the backend - <b>business rules do not belong in controllers</b>.
 *
 * <p>The formula:
 * <pre>
 *   base       = moveType.baseCharge + homeSize.volumeCharge
 *   distance   = max(0, distanceKm - freeDistanceKm) x perKmRate
 *   packing    = base x packingPercent                  (if requested)
 *   floors     = (pickupFloor + dropFloor) x perFloor    (only when there is no lift)
 *   insurance  = max(declaredValue x insurancePercent, minInsuranceCharge)  (if requested)
 *   storage    = storageDays x storagePerDay
 *   ---------------------------------------------------------------
 *   subtotal   = sum of the above
 *   gst        = subtotal x 18%
 *   total      = subtotal + gst
 * </pre>
 */
@Component
public class PricingEngine {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    /** Crew size shown for move types that have no home size. */
    private static final int DEFAULT_CREW_SIZE = 2;
    private static final int OFFICE_CREW_SIZE = 6;

    private final PricingProperties pricing;

    public PricingEngine(PricingProperties pricing) {
        this.pricing = pricing;
    }

    /**
     * Calculates an itemised estimate. Assumes the request has already been validated by
     * {@link QuoteService} - this method deliberately contains no validation so that the
     * formula stays readable.
     */
    public QuoteResponse calculate(QuoteRequest request) {
        MoveType moveType = request.moveType();
        HomeSize homeSize = request.homeSize();
        List<QuoteResponse.LineItem> items = new ArrayList<>();

        // ---------- 1. Base charge ----------
        // Fixed handling cost for this type of move, plus a volume component when we know the
        // home size. A 3 BHK needs more material and more hands than a 1 RK.
        BigDecimal base = moveType.getBaseCharge();
        if (homeSize != null) {
            base = base.add(homeSize.getVolumeCharge());
        }
        base = money(base);
        items.add(new QuoteResponse.LineItem(
                "BASE",
                "Base charge",
                homeSize == null
                        ? moveType.getLabel() + " handling, crew mobilisation and basic material"
                        : moveType.getLabel() + " for a " + homeSize.getLabel()
                          + ", including crew and basic packing material",
                base));

        // ---------- 2. Distance ----------
        // The first `freeDistanceKm` are already paid for inside the base charge, so only the
        // excess is billed. Math.max keeps a short move from producing a negative charge - the
        // kind of off-by-one that turns into a customer paying nothing.
        BigDecimal perKmRate = (homeSize != null)
                ? homeSize.getPerKmRate()          // bigger home implies a bigger vehicle class
                : moveType.getDefaultPerKmRate();
        int chargeableKm = Math.max(0, request.distanceKm() - pricing.freeDistanceKm());
        if (chargeableKm > 0) {
            BigDecimal distanceCharge = money(perKmRate.multiply(BigDecimal.valueOf(chargeableKm)));
            items.add(new QuoteResponse.LineItem(
                    "DISTANCE",
                    "Transport (" + chargeableKm + " km)",
                    "%d km total, first %d km included, remaining %d km at Rs %s per km"
                            .formatted(request.distanceKm(), pricing.freeDistanceKm(),
                                    chargeableKm, perKmRate.stripTrailingZeros().toPlainString()),
                    distanceCharge));
        }

        // ---------- 3. Professional packing ----------
        // Percentage of base rather than a flat fee, because packing effort scales with the
        // volume of goods - which is exactly what the base charge already encodes.
        if (request.packingRequired()) {
            BigDecimal packing = percentOf(base, pricing.packingPercent());
            items.add(new QuoteResponse.LineItem(
                    "PACKING",
                    "Professional packing & unpacking",
                    "%s%% of the base charge for bubble wrap, cartons, stretch film and labour"
                            .formatted(trim(pricing.packingPercent())),
                    packing));
        }

        // ---------- 4. Floor / staircase charge ----------
        // Charged only when there is no lift: carrying a wardrobe up five flights is real
        // labour. Both ends count, which is why the floors are added together.
        if (!request.liftAvailable()) {
            int floors = request.pickupFloor() + request.dropFloor();
            if (floors > 0) {
                BigDecimal floorCharge = money(pricing.perFloorCharge().multiply(BigDecimal.valueOf(floors)));
                items.add(new QuoteResponse.LineItem(
                        "FLOORS",
                        "Staircase charge (" + floors + " floors)",
                        "No lift available: %d floor(s) at pickup + %d floor(s) at drop, Rs %s per floor"
                                .formatted(request.pickupFloor(), request.dropFloor(),
                                        trim(pricing.perFloorCharge())),
                        floorCharge));
            }
        }

        // ---------- 5. Transit insurance ----------
        // A percentage of the customer's declared value, with a floor: the paperwork and
        // underwriting cost the same whether the goods are worth 20,000 or 2,00,000.
        if (request.insuranceRequired() && request.declaredValue() != null) {
            BigDecimal premium = percentOf(request.declaredValue(), pricing.insurancePercent());
            // max() on BigDecimal returns the larger of the two values - the idiomatic way to
            // apply a minimum without an if-statement.
            premium = premium.max(money(pricing.minInsuranceCharge()));
            items.add(new QuoteResponse.LineItem(
                    "INSURANCE",
                    "Transit insurance",
                    "%s%% of the declared goods value of Rs %s (minimum Rs %s)"
                            .formatted(trim(pricing.insurancePercent()),
                                    trim(request.declaredValue()),
                                    trim(pricing.minInsuranceCharge())),
                    money(premium)));
        }

        // ---------- 6. Warehousing ----------
        // Billed per day, not per month. That is a genuine product differentiator, so the
        // explanation string spells it out.
        if (request.storageDays() > 0) {
            BigDecimal storage = money(pricing.storagePerDay()
                    .multiply(BigDecimal.valueOf(request.storageDays())));
            items.add(new QuoteResponse.LineItem(
                    "STORAGE",
                    "Warehousing (" + request.storageDays() + " days)",
                    "%d day(s) of pest-controlled, CCTV-monitored storage at Rs %s per day"
                            .formatted(request.storageDays(), trim(pricing.storagePerDay())),
                    storage));
        }

        // ---------- 7. Totals ----------
        // reduce() with BigDecimal.ZERO as the identity is the stream idiom for summing
        // BigDecimals. There is no Collectors.summingBigDecimal in the JDK, and
        // mapToDouble(...).sum() would reintroduce floating point error into money.
        BigDecimal subtotal = money(items.stream()
                .map(QuoteResponse.LineItem::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal gstAmount = percentOf(subtotal, pricing.gstPercent());
        BigDecimal total = money(subtotal.add(gstAmount));

        return new QuoteResponse(
                moveType,
                moveType.getLabel(),
                homeSize,
                homeSize == null ? null : homeSize.getLabel(),
                request.distanceKm(),
                resolveCrewSize(moveType, homeSize),
                List.copyOf(items),   // defensive copy: the response must be immutable
                subtotal,
                pricing.gstPercent(),
                gstAmount,
                total,
                "INR",
                LocalDate.now().plusDays(pricing.quoteValidityDays()),
                "This is an estimate based on the details provided. The final invoice may vary "
                + "if the actual volume, distance or access conditions differ from what was declared.");
    }

    private int resolveCrewSize(MoveType moveType, HomeSize homeSize) {
        if (homeSize != null) {
            return homeSize.getCrewSize();
        }
        return moveType == MoveType.OFFICE_RELOCATION ? OFFICE_CREW_SIZE : DEFAULT_CREW_SIZE;
    }

    /**
     * {@code amount x percent / 100}, rounded to paise.
     *
     * <p>{@code divide} on BigDecimal MUST be given a scale and a RoundingMode. Without them,
     * a non-terminating result such as 1/3 throws {@code ArithmeticException} at runtime -
     * a classic production surprise that only appears with certain inputs.
     *
     * <p>{@code HALF_UP} is the rounding every accountant expects: 0.5 rounds away from zero.
     * (Java's {@code Math.round} uses HALF_UP too, but {@code double} arithmetic would have
     * already lost precision before we got here.)
     */
    private static BigDecimal percentOf(BigDecimal amount, BigDecimal percent) {
        return amount.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    /** Normalises any amount to two decimal places, so every line item has the same scale. */
    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Formats a BigDecimal for display without trailing zeros: 2.50 becomes "2.5", 400.00
     * becomes "400". {@code toPlainString} is used instead of {@code toString} because the
     * latter can emit scientific notation ("1E+2") for some scales.
     */
    private static String trim(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
