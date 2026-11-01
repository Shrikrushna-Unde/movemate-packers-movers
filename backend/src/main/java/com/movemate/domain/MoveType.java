package com.movemate.domain;

import java.math.BigDecimal;

/**
 * The kind of move a customer is requesting.
 *
 * <p>Each constant carries its own pricing facts. Putting them here rather than in a
 * {@code switch} inside the pricing service follows the "tell, don't ask" principle: adding
 * a sixth move type means adding one line to this enum, and the compiler will not let you
 * forget the price. A switch statement scattered across services would silently fall through
 * to a default.
 *
 * <p>All money is {@link BigDecimal}. {@code BigDecimal.valueOf(1500)} is used instead of
 * {@code new BigDecimal(1500.0)} because the double constructor carries binary rounding error
 * into the value.
 */
public enum MoveType {

    /**
     * Same-city household move. Needs a home size, because a 1 RK and a 4 BHK
     * are completely different jobs.
     */
    LOCAL_HOUSE_SHIFTING("Local House Shifting", BigDecimal.valueOf(1300), BigDecimal.valueOf(18), true),

    /** Intercity household move: bigger vehicle, driver allowances, longer haul. */
    DOMESTIC_RELOCATION("Domestic (Intercity) Relocation", BigDecimal.valueOf(3600), BigDecimal.valueOf(20), true),

    /** Commercial move. Priced per site survey, so home size does not apply. */
    OFFICE_RELOCATION("Office Relocation", BigDecimal.valueOf(10800), BigDecimal.valueOf(25), false),

    /** Car or bike on a covered carrier. */
    VEHICLE_TRANSPORT("Car / Bike Transport", BigDecimal.valueOf(5400), BigDecimal.valueOf(11), false),

    /** Pickup into our warehouse; the per-day storage charge is added separately. */
    STORAGE_ONLY("Warehousing / Storage", BigDecimal.valueOf(2250), BigDecimal.valueOf(13), false);

    private final String label;
    /** Fixed handling component: crew mobilisation, paperwork, basic material. */
    private final BigDecimal baseCharge;
    /** Per-km rate used when there is no home size to derive a vehicle class from. */
    private final BigDecimal defaultPerKmRate;
    /** Whether the quote form must collect a {@link HomeSize} for this type. */
    private final boolean requiresHomeSize;

    MoveType(String label, BigDecimal baseCharge, BigDecimal defaultPerKmRate, boolean requiresHomeSize) {
        this.label = label;
        this.baseCharge = baseCharge;
        this.defaultPerKmRate = defaultPerKmRate;
        this.requiresHomeSize = requiresHomeSize;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getBaseCharge() {
        return baseCharge;
    }

    public BigDecimal getDefaultPerKmRate() {
        return defaultPerKmRate;
    }

    public boolean isRequiresHomeSize() {
        return requiresHomeSize;
    }
}
