package com.movemate.dto;

import com.movemate.domain.HomeSize;
import com.movemate.domain.MoveType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A single {@code <option>} for a dropdown: machine value plus display label.
 *
 * <p>This exists so the React forms are driven by the API instead of duplicating every enum
 * in JavaScript. Add a move type in Java and the dropdown updates on the next page load with
 * zero frontend changes - and the two sides can never drift out of sync, which is the usual
 * bug when enums are copy-pasted into the client.
 *
 * @param value machine value, exactly the enum constant name
 * @param label human text
 * @param meta  extra hints the form needs (e.g. whether this move type requires a home size)
 */
public record OptionResponse(
        String value,
        String label,
        Map<String, Object> meta
) {

    public static List<OptionResponse> moveTypes() {
        return Arrays.stream(MoveType.values())
                .map(t -> new OptionResponse(
                        t.name(),
                        t.getLabel(),
                        // Map.of is immutable and allocation-light. The frontend reads
                        // requiresHomeSize to show or hide the home-size dropdown.
                        Map.of(
                                "requiresHomeSize", t.isRequiresHomeSize(),
                                "baseCharge", t.getBaseCharge()
                        )))
                .toList();
    }

    public static List<OptionResponse> homeSizes() {
        return Arrays.stream(HomeSize.values())
                .map(s -> new OptionResponse(
                        s.name(),
                        s.getLabel(),
                        Map.of(
                                "crewSize", s.getCrewSize(),
                                "perKmRate", s.getPerKmRate()
                        )))
                .toList();
    }
}
