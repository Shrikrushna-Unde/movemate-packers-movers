package com.movemate.dto;

import com.movemate.domain.HomeSize;
import com.movemate.domain.MoveType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * An itemised estimate.
 *
 * <p>The design decision here is to return the BREAKDOWN, not just a total. Packers and movers
 * is an industry where customers expect hidden charges, so showing "base 5,500 + packing 1,210
 * + GST 1,208" converts far better than a bare "7,920". It also makes the engine testable and
 * debuggable: when a number looks wrong you can see which line produced it.
 *
 * <p>{@code lineItems} is a {@code List} rather than a fixed set of fields, so adding a
 * "toll charges" component later changes the engine and the UI renders it with no new field.
 */
public record QuoteResponse(
        MoveType moveType,
        String moveTypeLabel,
        HomeSize homeSize,
        String homeSizeLabel,
        int distanceKm,
        int crewSize,
        List<LineItem> lineItems,
        /* Sum of all line items, before tax. */
        BigDecimal subtotal,
        BigDecimal gstPercent,
        BigDecimal gstAmount,
        /* What the customer pays. */
        BigDecimal total,
        String currency,
        /* Quotes expire, so the price table can change without breaking a promise. */
        LocalDate validUntil,
        String disclaimer
) {

    /**
     * One charge in the estimate.
     *
     * <p>A record nested inside a record is legal and implicitly static. It keeps a type that
     * is only meaningful here from polluting the package.
     *
     * @param code        stable machine key ("BASE", "DISTANCE") the frontend can switch on
     * @param label       what the customer reads
     * @param explanation plain-language justification, e.g. "130 km beyond the free 20 km at 24/km"
     * @param amount      rupees, always BigDecimal
     */
    public record LineItem(
            String code,
            String label,
            String explanation,
            BigDecimal amount
    ) {
    }
}
