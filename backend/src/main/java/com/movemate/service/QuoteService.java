package com.movemate.service;

import com.movemate.dto.QuoteRequest;
import com.movemate.dto.QuoteResponse;
import com.movemate.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Validates a quote request, then delegates the arithmetic to {@link PricingEngine}.
 *
 * <p>Why two classes for one feature? Separation of concerns:
 * <ul>
 *   <li>{@code QuoteService} enforces rules that depend on COMBINATIONS of fields - exactly
 *       what Bean Validation annotations cannot express.</li>
 *   <li>{@code PricingEngine} is pure calculation, so its unit tests never need a mock.</li>
 * </ul>
 * If they were one class, every pricing test would also have to satisfy the validation rules,
 * and the formula would be buried in guard clauses.
 */
@Service
public class QuoteService {

    private final PricingEngine pricingEngine;

    public QuoteService(PricingEngine pricingEngine) {
        this.pricingEngine = pricingEngine;
    }

    /**
     * @throws BadRequestException when the combination of fields is not viable
     */
    public QuoteResponse estimate(QuoteRequest request) {
        validate(request);
        return pricingEngine.calculate(request);
    }

    /**
     * Cross-field rules. Each one exists because of a way the form can be submitted that is
     * individually valid but collectively meaningless.
     */
    private void validate(QuoteRequest request) {

        // A household move with no home size would price a 4 BHK like a 1 RK.
        if (request.moveType().isRequiresHomeSize() && request.homeSize() == null) {
            throw new BadRequestException(
                    "Please select your home size for a " + request.moveType().getLabel().toLowerCase());
        }

        // Insurance is a percentage of declared value, so without a value the premium would be
        // zero - the customer would believe they are insured and they would not be.
        if (request.insuranceRequired()) {
            if (request.declaredValue() == null) {
                throw new BadRequestException(
                        "Please declare the approximate value of your goods to add transit insurance");
            }
            // compareTo, not equals: BigDecimal.equals("0.00") is false against
            // BigDecimal.ZERO because equals compares SCALE as well as value. This is one of
            // the most common BigDecimal bugs and a frequent interview question.
            if (request.declaredValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Declared goods value must be greater than zero");
            }
        }

        // A storage-only job that stores nothing is a mis-click, not a request.
        if (request.moveType() == com.movemate.domain.MoveType.STORAGE_ONLY
                && request.storageDays() <= 0) {
            throw new BadRequestException("Please tell us how many days of storage you need");
        }
    }
}
