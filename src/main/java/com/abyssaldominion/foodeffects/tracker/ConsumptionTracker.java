package com.abyssaldominion.foodeffects.tracker;

import com.abyssaldominion.foodeffects.config.FoodRule;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe tracker for pending food/drink consumption transactions.
 * <p>
 * Only one context per player is retained at a time. A later consumption
 * supersedes an earlier pending one; the earlier delayed task cannot consume
 * or apply the newer transaction's effects.
 */
public class ConsumptionTracker {

    private final Map<UUID, ConsumptionContext> activeContexts = new ConcurrentHashMap<>();

    /**
     * Registers a new consumption context, replacing any existing one for the same player.
     */
    public void register(ConsumptionContext context) {
        if (context == null || context.playerUuid() == null) {
            return;
        }
        activeContexts.put(context.playerUuid(), context);
    }

    /**
     * Retrieves and removes the active context for a player.
     */
    public ConsumptionContext consume(UUID playerUuid) {
        if (playerUuid == null) {
            return null;
        }
        return activeContexts.remove(playerUuid);
    }

    /**
     * Removes a pending context only if it is the exact transaction supplied.
     * Identity comparison prevents an older delayed task from consuming a newer
     * context, even when two contexts have equal field values.
     */
    public boolean consumeIfCurrent(UUID playerUuid, ConsumptionContext expected) {
        if (playerUuid == null || expected == null || !playerUuid.equals(expected.playerUuid())) {
            return false;
        }
        java.util.concurrent.atomic.AtomicBoolean consumed = new java.util.concurrent.atomic.AtomicBoolean();
        activeContexts.computeIfPresent(playerUuid, (key, active) -> {
            if (active == expected) {
                consumed.set(true);
                return null;
            }
            return active;
        });
        return consumed.get();
    }

    /**
     * Peeks at the active context without removing it.
     */
    public ConsumptionContext peek(UUID playerUuid) {
        if (playerUuid == null) {
            return null;
        }
        return activeContexts.get(playerUuid);
    }

    /**
     * Removes the context for a player without processing it.
     * Used on disconnect to prevent stale processing.
     */
    public void clear(UUID playerUuid) {
        if (playerUuid == null) {
            return;
        }
        activeContexts.remove(playerUuid);
    }

    /**
     * Clears all pending contexts. Used on plugin shutdown.
     */
    public void clearAll() {
        activeContexts.clear();
    }

    /**
     * Returns the number of pending contexts. Exposed for diagnostics.
     */
    public int size() {
        return activeContexts.size();
    }

    /**
     * Immutable snapshot of a single consumption transaction.
     */
    public record ConsumptionContext(
        UUID playerUuid,
        String materialName,
        String customId,
        FoodRule rule,
        long createdAtMillis
    ) {
        private static final long STALE_THRESHOLD_MILLIS = 30_000; // 30 seconds

        public boolean isStale() {
            return (System.currentTimeMillis() - createdAtMillis) > STALE_THRESHOLD_MILLIS;
        }

        public boolean matchesPlayer(Player player) {
            return player != null && player.getUniqueId().equals(playerUuid);
        }
    }
}
