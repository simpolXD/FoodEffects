package com.abyssaldominion.foodeffects.config;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodRuleRemovalTest {

    @Test
    void removalRulesMatchByEffectKeyWithoutRequiringDurationFields() {
        NamespacedKey speed = NamespacedKey.minecraft("speed");
        FoodRule rule = new FoodRule(Material.APPLE, "", List.of(), List.of(speed),
            Map.of(), List.of());

        assertTrue(rule.shouldRemove(speed));
        assertFalse(rule.shouldRemove(NamespacedKey.minecraft("regeneration")));
    }
}
