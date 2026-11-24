package com.abyssaldominion.foodeffects.config;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Immutable definition of a single potion effect applied by this plugin.
 */
public record EffectRule(
    PotionEffectType type,
    int durationTicks,
    int amplifier,
    boolean ambient,
    boolean particles,
    boolean icon
) {

    /**
     * Creates a new Bukkit PotionEffect from this rule.
     */
    public PotionEffect toPotionEffect() {
        return new PotionEffect(type, durationTicks, amplifier, ambient, particles, icon);
    }

    /**
     * Validates that the rule fields are within acceptable bounds.
     */
    public boolean isValid() {
        if (type == null) {
            return false;
        }
        if (durationTicks <= 0) {
            return false;
        }
        if (amplifier < 0) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "EffectRule{" +
            "type=" + (type != null ? type.getKey() : "null") +
            ", durationTicks=" + durationTicks +
            ", amplifier=" + amplifier +
            ", ambient=" + ambient +
            ", particles=" + particles +
            ", icon=" + icon +
            '}';
    }
}
