package com.abyssaldominion.foodeffects.config;

import org.bukkit.NamespacedKey;

import java.util.Objects;

/** A string-valued PDC requirement used to identify a configured custom item. */
public record PdcRequirement(NamespacedKey key, String expectedValue) {

    public PdcRequirement {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(expectedValue, "expectedValue");
    }

    public boolean matches(String storedValue) {
        return expectedValue.equals(storedValue);
    }
}
