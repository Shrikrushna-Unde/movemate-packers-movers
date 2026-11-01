package com.movemate.service;

import com.movemate.config.PricingProperties;
import com.movemate.domain.HomeSize;
import com.movemate.domain.MoveType;
import com.movemate.dto.QuoteRequest;
import com.movemate.dto.QuoteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the pricing formula.
 *
 * <p>Notice what is NOT here: no {@code @SpringBootTest}, no database, no mocks. Because
 * {@link PricingEngine} takes its configuration as a constructor argument and touches nothing
 * else, the whole class is testable with {@code new}. These tests run in milliseconds, and that
 * is a direct payoff of constructor injection and keeping business logic out of controllers.
 *
 * <p>A {@code @SpringBootTest} that boots the context, connects to MySQL and runs migrations
 * just to check that 18% of 4900 is 882 would take ten seconds and break when the database is
 * down. Knowing which kind of test to reach for is a senior-level signal.
 */
@DisplayName("PricingEngine")
class PricingEngineTest {

    private PricingEngine pricingEngine;

    /**
     * {@code @BeforeEach} runs before every test method, giving each one a fresh engine.
     * Shared mutable state between tests is the classic cause of "passes alone, fails in the
     * suite" flakiness.
     *
     * <p>The properties are built with the same values as application.properties, so the tests
     * document the production configuration as well as verifying the maths.
     */
    @BeforeEach
    void setUp() {
        PricingProperties properties = new PricingProperties(
                25,                           // freeDistanceKm
                BigDecimal.valueOf(18),       // packingPercent
                BigDecimal.valueOf(350),      // perFloorCharge
                BigDecimal.valueOf(2.5),      // insurancePercent
                BigDecimal.valueOf(400),      // minInsuranceCharge
                BigDecimal.valueOf(100),      // storagePerDay
                BigDecimal.valueOf(18),       // gstPercent
                7                             // quoteValidityDays
        );
        pricingEngine = new PricingEngine(properties);
    }

    @Test
    @DisplayName("prices a short local 2 BHK move with packing")
    void localTwoBhkWithinFreeDistance() {
        QuoteRequest request = new QuoteRequest(
                MoveType.LOCAL_HOUSE_SHIFTING,
                HomeSize.TWO_BHK,
                15,                 // distanceKm - inside the free 25 km
                0, 0,               // floors
                true,               // lift available
                true,               // packing required
                false, null,        // no insurance
                0                   // no storage
        );

        QuoteResponse quote = pricingEngine.calculate(request);

        // base = move type 1300 + home size 3600
        assertThat(amountOf(quote, "BASE")).isEqualByComparingTo("4900.00");
        // packing = 18% of 4900
        assertThat(amountOf(quote, "PACKING")).isEqualByComparingTo("882.00");
        // Inside the free distance, so the line item must be ABSENT rather than zero -
        // showing "Transport: Rs 0" on a quote looks like a bug to a customer.
        assertThat(lineItem(quote, "DISTANCE")).isEmpty();

        assertThat(quote.subtotal()).isEqualByComparingTo("5782.00");
        assertThat(quote.gstAmount()).isEqualByComparingTo("1040.76");
        assertThat(quote.total()).isEqualByComparingTo("6822.76");
    }

    @Test
    @DisplayName("charges only the distance beyond the free kilometres")
    void intercityMoveChargesExcessDistanceOnly() {
        QuoteRequest request = new QuoteRequest(
                MoveType.DOMESTIC_RELOCATION,
                HomeSize.TWO_BHK,
                150,
                0, 0,
                true,
                false,              // no packing, to isolate the distance charge
                false, null,
                0
        );

        QuoteResponse quote = pricingEngine.calculate(request);

        // base = 3600 + 3600
        assertThat(amountOf(quote, "BASE")).isEqualByComparingTo("7200.00");
        // (150 - 25) km x 21 per km for a 2 BHK vehicle class
        assertThat(amountOf(quote, "DISTANCE")).isEqualByComparingTo("2625.00");
        assertThat(quote.subtotal()).isEqualByComparingTo("9825.00");
    }

    @Test
    @DisplayName("adds a staircase charge for both ends only when there is no lift")
    void staircaseChargeAppliesWithoutLift() {
        QuoteRequest withoutLift = new QuoteRequest(
                MoveType.LOCAL_HOUSE_SHIFTING, HomeSize.ONE_BHK, 10,
                2, 1,               // 2 floors at pickup + 1 at drop = 3
                false,              // no lift
                false, false, null, 0);

        QuoteResponse quote = pricingEngine.calculate(withoutLift);
        assertThat(amountOf(quote, "FLOORS")).isEqualByComparingTo("1050.00");  // 3 x 350

        // Same move, lift available -> the charge disappears entirely.
        QuoteRequest withLift = new QuoteRequest(
                MoveType.LOCAL_HOUSE_SHIFTING, HomeSize.ONE_BHK, 10,
                2, 1, true, false, false, null, 0);

        assertThat(lineItem(pricingEngine.calculate(withLift), "FLOORS")).isEmpty();
    }

    @Test
    @DisplayName("applies the minimum premium when the percentage is lower")
    void insuranceRespectsMinimumCharge() {
        // 2.5% of 10,000 = 250, which is below the 400 minimum.
        QuoteRequest lowValue = new QuoteRequest(
                MoveType.LOCAL_HOUSE_SHIFTING, HomeSize.ONE_RK, 5,
                0, 0, true, false,
                true, BigDecimal.valueOf(10_000),
                0);

        assertThat(amountOf(pricingEngine.calculate(lowValue), "INSURANCE"))
                .isEqualByComparingTo("400.00");

        // 2.5% of 4,00,000 = 10,000, comfortably above the minimum.
        QuoteRequest highValue = new QuoteRequest(
                MoveType.LOCAL_HOUSE_SHIFTING, HomeSize.THREE_BHK, 5,
                0, 0, true, false,
                true, BigDecimal.valueOf(400_000),
                0);

        assertThat(amountOf(pricingEngine.calculate(highValue), "INSURANCE"))
                .isEqualByComparingTo("10000.00");
    }

    @Test
    @DisplayName("bills storage per day")
    void storageIsBilledPerDay() {
        QuoteRequest request = new QuoteRequest(
                MoveType.STORAGE_ONLY, null, 25,
                0, 0, true, false, false, null,
                11);

        QuoteResponse quote = pricingEngine.calculate(request);
        assertThat(amountOf(quote, "STORAGE")).isEqualByComparingTo("1100.00");  // 11 x 100
    }

    @Test
    @DisplayName("falls back to the move type per-km rate when there is no home size")
    void vehicleTransportUsesMoveTypeRate() {
        QuoteRequest request = new QuoteRequest(
                MoveType.VEHICLE_TRANSPORT, null, 300,
                0, 0, true, false, false, null, 0);

        QuoteResponse quote = pricingEngine.calculate(request);

        assertThat(amountOf(quote, "BASE")).isEqualByComparingTo("5400.00");
        // (300 - 25) x 11, the VEHICLE_TRANSPORT default rate
        assertThat(amountOf(quote, "DISTANCE")).isEqualByComparingTo("3025.00");
        assertThat(quote.homeSize()).isNull();
        assertThat(quote.homeSizeLabel()).isNull();
    }

    @Test
    @DisplayName("subtotal always equals the sum of the line items")
    void subtotalMatchesLineItems() {
        QuoteRequest request = new QuoteRequest(
                MoveType.DOMESTIC_RELOCATION, HomeSize.THREE_BHK, 420,
                3, 2, false, true,
                true, BigDecimal.valueOf(500_000), 5);

        QuoteResponse quote = pricingEngine.calculate(request);

        BigDecimal sum = quote.lineItems().stream()
                .map(QuoteResponse.LineItem::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // This invariant is worth a dedicated test: if it ever breaks, the customer sees a
        // breakdown that does not add up to the total they are asked to pay, which destroys
        // trust faster than a wrong price would.
        assertThat(quote.subtotal()).isEqualByComparingTo(sum);
        assertThat(quote.total()).isEqualByComparingTo(sum.add(quote.gstAmount()));
    }

    @Test
    @DisplayName("quote carries a validity date and INR currency")
    void quoteMetadata() {
        QuoteResponse quote = pricingEngine.calculate(new QuoteRequest(
                MoveType.LOCAL_HOUSE_SHIFTING, HomeSize.ONE_BHK, 10,
                0, 0, true, true, false, null, 0));

        assertThat(quote.currency()).isEqualTo("INR");
        assertThat(quote.validUntil()).isAfter(java.time.LocalDate.now());
        assertThat(quote.crewSize()).isEqualTo(HomeSize.ONE_BHK.getCrewSize());
        assertThat(quote.moveTypeLabel()).isEqualTo(MoveType.LOCAL_HOUSE_SHIFTING.getLabel());
    }

    // ---------------- helpers ----------------

    /**
     * Finds a line item by its code.
     *
     * <p>Returning {@link Optional} lets a test assert either presence or absence, which is how
     * {@code isEmpty()} above can check that a charge was correctly NOT applied.
     */
    private Optional<QuoteResponse.LineItem> lineItem(QuoteResponse quote, String code) {
        return quote.lineItems().stream()
                .filter(item -> item.code().equals(code))
                .findFirst();
    }

    /** The amount for a code, failing the test with a clear message when the item is missing. */
    private BigDecimal amountOf(QuoteResponse quote, String code) {
        return lineItem(quote, code)
                .orElseThrow(() -> new AssertionError("Expected a '" + code + "' line item in the quote"))
                .amount();
    }
}
