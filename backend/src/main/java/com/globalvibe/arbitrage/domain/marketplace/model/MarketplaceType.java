package com.globalvibe.arbitrage.domain.marketplace.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum MarketplaceType {
    ALIBABA_1688("1688", "ALIBABA_1688", "TAOBAO"),
    AMAZON("AMAZON");

    private final String value;
    private final List<String> databaseValues;

    MarketplaceType(String value, String... aliases) {
        this.value = value;
        this.databaseValues = java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(value),
                        Arrays.stream(aliases)
                )
                .distinct()
                .toList();
    }

    @JsonValue
    public String value() {
        return value;
    }

    public List<String> databaseValues() {
        return databaseValues;
    }

    @JsonCreator
    public static MarketplaceType fromValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Marketplace type must not be blank.");
        }
        return Arrays.stream(values())
                .filter(type -> type.matches(rawValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown marketplace type: " + rawValue));
    }

    private boolean matches(String rawValue) {
        String normalized = normalize(rawValue);
        return databaseValues.stream().anyMatch(value -> normalize(value).equals(normalized));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
