package com.abyssaldominion.foodeffects.matcher;

import com.abyssaldominion.foodeffects.config.PdcRequirement;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class ItemMatcher {

    private ItemMatcher() {
    }

    /**
     * Attempts to detect a stable custom-item identifier from the item's PDC.
     * Returns empty if no known custom identifier is found.
     * <p>
     * This does NOT match by display name or lore.
     */
    public static String detectCustomId(ItemStack item) {
        if (item == null || isAirMaterial(item.getType())) {
            return "";
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return "";
        }
        var pdc = meta.getPersistentDataContainer();
        // Check common custom-item namespaces known to Oraxen / ItemsAdder.
        // This is intentionally conservative: only well-known stable identifiers.
        String[] knownNamespaces = {
            "oraxen", "itemsadder", "mythicmobs", "citizens", "neocity"
        };
        for (String ns : knownNamespaces) {
            for (NamespacedKey key : pdc.getKeys()) {
                if (ns.equals(key.namespace())) {
                    Object valueObj = pdc.get(key, PersistentDataType.STRING);
                    if (valueObj instanceof String value && !value.isBlank()) {
                        return ns + ":" + key.key() + "=" + value;
                    }
                }
            }
        }
        return "";
    }

    /**
     * Checks whether the item's PDC matches all configured requirements.
     */
    public static boolean matchesPdc(ItemStack item, List<PdcRequirement> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            return true;
        }
        if (item == null || isAirMaterial(item.getType())) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        var pdc = meta.getPersistentDataContainer();
        for (PdcRequirement requirement : requirements) {
            String storedValue = pdc.get(requirement.key(), PersistentDataType.STRING);
            if (!requirement.matches(storedValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a safe immutable snapshot of the item's identity for matching.
     * This does not retain a live reference to an inventory ItemStack.
     */
    public static ItemSnapshot snapshot(ItemStack item) {
        if (item == null) {
            return ItemSnapshot.EMPTY;
        }

        return new ItemSnapshot(item.getType(), detectCustomId(item));
    }

    public static boolean isAirMaterial(Material material) {
        return material == null
            || material == Material.AIR
            || material == Material.CAVE_AIR
            || material == Material.VOID_AIR;
    }

    public record ItemSnapshot(
        org.bukkit.Material material,
        String customId
    ) {
        public static final ItemSnapshot EMPTY = new ItemSnapshot(org.bukkit.Material.AIR, "");

        public boolean isAir() {
            return ItemMatcher.isAirMaterial(material);
        }
    }
}
