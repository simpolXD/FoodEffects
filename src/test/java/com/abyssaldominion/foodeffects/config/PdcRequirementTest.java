package com.abyssaldominion.foodeffects.config;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdcRequirementTest {

    @Test
    void matchesOnlyTheConfiguredStringValue() {
        PdcRequirement requirement = new PdcRequirement(
            new NamespacedKey("myplugin", "itemid"), "super_apple");

        assertTrue(requirement.matches("super_apple"));
        assertFalse(requirement.matches("another_item"));
        assertFalse(requirement.matches(null));
    }
}
