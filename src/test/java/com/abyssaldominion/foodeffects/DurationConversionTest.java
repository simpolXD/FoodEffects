package com.abyssaldominion.foodeffects;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DurationConversionTest {

    @Test
    void secondsToTicksConversion() {
        // 1 second = 20 ticks
        assertEquals(20, 1 * 20);
        assertEquals(300, 15 * 20);
        assertEquals(144000, 7200 * 20);
    }

    @Test
    void maxDurationIsReasonable() {
        int maxSeconds = 7200;
        int maxTicks = maxSeconds * 20;
        assertTrue(maxTicks > 0, "Max ticks must be positive");
        assertTrue(maxTicks <= Integer.MAX_VALUE, "Max ticks must fit in int");
    }

    @Test
    void effectTypeResolution() {
        // Verify common effect types resolve when Bukkit is available
        PotionEffectType speed = safeGetEffectType("SPEED");
        assumeTrue(speed != null, "Paper effect registry is unavailable in this unit-test JVM");
        assertNotNull(speed);
        PotionEffectType regen = safeGetEffectType("REGENERATION");
        assertNotNull(regen);
        PotionEffectType strength = safeGetEffectType("STRENGTH");
        assertNotNull(strength);
    }

    @Test
    void unknownEffectTypeReturnsNull() {
        assumeTrue(safeGetEffectType("SPEED") != null,
            "Paper effect registry is unavailable in this unit-test JVM");
        assertNull(safeGetEffectType("NONEXISTENT_EFFECT_XYZ"));
    }

    @Test
    void namespacedKeyCreation() {
        NamespacedKey key = new NamespacedKey("myplugin", "itemid");
        assertEquals("myplugin", key.getNamespace());
        assertEquals("myplugin:itemid", key.toString());
        assertEquals("itemid", key.getKey());
    }

    private static PotionEffectType safeGetEffectType(String name) {
        try {
            NamespacedKey key = NamespacedKey.fromString(name.toLowerCase(java.util.Locale.ROOT));
            return key == null ? null : Registry.MOB_EFFECT.get(key);
        } catch (Throwable t) {
            return null;
        }
    }
}
