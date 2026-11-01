package com.movemate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Tunable pricing knobs, bound from {@code app.pricing.*}.
 *
 * <p>The split is deliberate: facts that belong to a move type or a home size live on the
 * enums ({@code MoveType}, {@code HomeSize}) because they are part of the domain; the global
 * multipliers live here because the business changes them without a code change - a seasonal
 * discount, a GST rate revision, a fuel-driven per-km increase.
 *
 * <p>Relaxed binding means {@code app.pricing.free-distance-km} in the properties file maps to
 * {@code freeDistanceKm} here. Kebab-case in config, camelCase in Java, no configuration needed.
 *
 * @param freeDistanceKm     km included in the base charge before per-km billing starts
 * @param packingPercent     packing cost as a percentage of the base charge
 * @param perFloorCharge     rupees per floor, per end, when there is no lift
 * @param insurancePercent   premium as a percentage of declared goods value
 * @param minInsuranceCharge floor on the premium, because paperwork has a fixed cost
 * @param storagePerDay      warehousing rupees per day
 * @param gstPercent         Indian GST on moving services (18% at the time of writing)
 * @param quoteValidityDays  how long a quote is honoured
 */
@ConfigurationProperties(prefix = "app.pricing")
public record PricingProperties(
        int freeDistanceKm,
        BigDecimal packingPercent,
        BigDecimal perFloorCharge,
        BigDecimal insurancePercent,
        BigDecimal minInsuranceCharge,
        BigDecimal storagePerDay,
        BigDecimal gstPercent,
        int quoteValidityDays
) {
}
