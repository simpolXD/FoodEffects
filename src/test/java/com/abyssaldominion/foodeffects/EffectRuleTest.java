package com.abyssaldominion.foodeffects;

import com.abyssaldominion.foodeffects.config.EffectRule;
import com.abyssaldominion.foodeffects.config.FoodRule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class EffectRuleTest {

    @Test
    void effectRuleValidation() {
        PotionEffectType speed = safeGetEffectType("speed");
        assumeTrue(speed != null, "Paper effect registry is unavailable in this unit-test JVM");

        EffectRule rule = new EffectRule(speed, 300, 1, false, true, true);
        assertTrue(rule.isValid());
        assertEquals(300, rule.durationTicks());
        assertEquals(1, rule.amplifier());
    }

    @Test
    void invalidEffectRuleRejectsNullType() {
        EffectRule rule = new EffectRule(null, 100, 0, false, true, true);
        assertFalse(rule.isValid());
    }

    @Test
    void invalidEffectRuleRejectsNonPositiveDuration() {
        PotionEffectType speed = safeGetEffectType("speed");
        assumeTrue(speed != null, "Paper effect registry is unavailable in this unit-test JVM");
        EffectRule rule = new EffectRule(speed, 0, 0, false, true, true);
        assertFalse(rule.isValid());
        EffectRule rule2 = new EffectRule(speed, -5, 0, false, true, true);
        assertFalse(rule2.isValid());
    }

    @Test
    void foodRuleHasAnyEffect() {
        EffectRule add = new EffectRule(null, 100, 0, false, true, true);
        FoodRule rule = new FoodRule(Material.GOLDEN_APPLE, "", List.of(),
            List.of(), Map.of(), List.of(add));
        assertTrue(rule.hasAnyEffect());

        FoodRule empty = new FoodRule(Material.APPLE, "", List.of(),
            List.of(), Map.of(), List.of());
        assertFalse(empty.hasAnyEffect());
    }

    private static PotionEffectType safeGetEffectType(String name) {
        try {
            if (Bukkit.getServer() == null) return null;
            NamespacedKey key = NamespacedKey.fromString(name.toLowerCase(java.util.Locale.ROOT));
            return key == null ? null : Registry.MOB_EFFECT.get(key);
        } catch (Throwable t) {
            return null;
        }
    }
}
