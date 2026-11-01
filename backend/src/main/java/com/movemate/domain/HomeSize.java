package com.movemate.domain;

import java.math.BigDecimal;

/**
 * Household size, which in this business is really a proxy for two things:
 * goods volume (how much packing material and labour) and vehicle class
 * (which decides the per-km rate).
 *
 * <p>In Indian real-estate vocabulary "1 RK" is one room plus kitchen and
 * "2 BHK" is two bedrooms, hall, kitchen - so these constants are the terms a
 * customer already understands, which is why the enum mirrors them instead of
 * inventing SMALL/MEDIUM/LARGE.
 */
public enum HomeSize {

    ONE_RK("1 RK", BigDecimal.valueOf(1350), BigDecimal.valueOf(16), 2),
    ONE_BHK("1 BHK", BigDecimal.valueOf(1800), BigDecimal.valueOf(18), 3),
    TWO_BHK("2 BHK", BigDecimal.valueOf(3600), BigDecimal.valueOf(21), 4),
    THREE_BHK("3 BHK", BigDecimal.valueOf(5850), BigDecimal.valueOf(26), 5),
    FOUR_BHK_PLUS("4 BHK or larger", BigDecimal.valueOf(8550), BigDecimal.valueOf(32), 6);

    private final String label;
    /** Volume component of the base charge - material and handling effort. */
    private final BigDecimal volumeCharge;
    /** Per-km rate for the vehicle class this home size needs. */
    private final BigDecimal perKmRate;
    /** How many people the crew needs. Shown in the quote so the price feels justified. */
    private final int crewSize;

    HomeSize(String label, BigDecimal volumeCharge, BigDecimal perKmRate, int crewSize) {
        this.label = label;
        this.volumeCharge = volumeCharge;
        this.perKmRate = perKmRate;
        this.crewSize = crewSize;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getVolumeCharge() {
        return volumeCharge;
    }

    public BigDecimal getPerKmRate() {
        return perKmRate;
    }

    public int getCrewSize() {
        return crewSize;
    }
}
