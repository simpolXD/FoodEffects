package com.abyssaldominion.foodeffects.config;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Immutable rule defining how one item or item category modifies potion effects.
 */
public record FoodRule(
    Material material,
    String customId,
    List<PdcRequirement> pdcRequirements,
    List<NamespacedKey> removeEffects,
    Map<PotionEffectType, EffectRule> replaceEffects,
    List<EffectRule> addEffects
) {

    public FoodRule {
        material = material != null ? material : Material.AIR;
        customId = customId != null ? customId : "";
        pdcRequirements = pdcRequirements != null ? List.copyOf(pdcRequirements) : List.of();
        removeEffects = removeEffects != null ? List.copyOf(removeEffects) : List.of();
        replaceEffects = replaceEffects != null ? Map.copyOf(replaceEffects) : Map.of();
        addEffects = addEffects != null ? List.copyOf(addEffects) : List.of();
    }

    public boolean hasAnyEffect() {
        return !removeEffects.isEmpty() || !replaceEffects.isEmpty() || !addEffects.isEmpty();
    }

    public boolean hasPdcRequirements() {
        return !pdcRequirements.isEmpty();
    }

    public boolean shouldRemove(NamespacedKey effectKey) {
        return effectKey != null && removeEffects.contains(effectKey);
    }

    public boolean shouldRemove(PotionEffectType type) {
        return type != null && shouldRemove(type.getKey());
    }

    public EffectRule getReplacement(PotionEffectType type) {
        return replaceEffects.get(type);
    }

    public List<EffectRule> getAddEffects() {
        return addEffects;
    }

    public List<EffectRule> getReplacements() {
        return new ArrayList<>(replaceEffects.values());
    }

    @Override
    public String toString() {
        return "FoodRule{" +
            "material=" + material +
            ", customId='" + customId + '\'' +
            ", pdcRequirements=" + pdcRequirements +
            ", removeEffects=" + removeEffects +
            ", replaceEffects=" + replaceEffects.keySet() +
            ", addEffects=" + addEffects +
            '}';
    }
}
