package com.abyssaldominion.foodeffects.listener;

import com.abyssaldominion.foodeffects.AbyssalDominionFoodEffects;
import com.abyssaldominion.foodeffects.config.EffectRule;
import com.abyssaldominion.foodeffects.config.FoodEffectsConfig;
import com.abyssaldominion.foodeffects.config.FoodRule;
import com.abyssaldominion.foodeffects.matcher.ItemMatcher;
import com.abyssaldominion.foodeffects.tracker.ConsumptionTracker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

/**
 * Intercepts item consumption and potion-effect application to implement
 * the configured add / remove / replace semantics.
 * <p>
 * Event priorities:
 * <ul>
 *   <li>PlayerItemConsumeEvent: MONITOR (observe final cancellation outcome without modifying the event)</li>
 *   <li>EntityPotionEffectEvent: HIGH (intercept before most other plugins)</li>
 * </ul>
 */
public class FoodEffectsListener implements Listener {

    private final AbyssalDominionFoodEffects plugin;
    private final FoodEffectsConfig config;
    private final ConsumptionTracker tracker;
    private final java.util.logging.Logger logger;

    public FoodEffectsListener(AbyssalDominionFoodEffects plugin,
                               FoodEffectsConfig config,
                               ConsumptionTracker tracker,
                               java.util.logging.Logger logger) {
        this.plugin = plugin;
        this.config = config;
        this.tracker = tracker;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemConsume(@NotNull PlayerItemConsumeEvent event) {
        if (!config.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            return;
        }

        FoodRule rule = config.getRuleForItem(item);
        if (rule == null || !rule.hasAnyEffect()) {
            return;
        }

        // Record the consumption context so EntityPotionEffectEvent can attribute effects.
        ConsumptionTracker.ConsumptionContext context =
            new ConsumptionTracker.ConsumptionContext(
                player.getUniqueId(),
                item.getType().name(),
                ItemMatcher.detectCustomId(item),
                rule,
                System.currentTimeMillis()
            );
        tracker.register(context);

        // Schedule add-effects and replacements for the next server tick.
        // By then, all vanilla effect events for this consumption will have fired.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> applyEffects(player, context), 1L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityPotionEffect(@NotNull EntityPotionEffectEvent event) {
        if (!config.isEnabled()) {
            return;
        }
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Only intercept effects originating from consumption.
        EntityPotionEffectEvent.Cause cause = event.getCause();
        if (cause != EntityPotionEffectEvent.Cause.FOOD
            && cause != EntityPotionEffectEvent.Cause.POTION_DRINK) {
            return;
        }

        // Milk bucket clears effects; we must not interfere with clearing behavior.
        // CLEARED and REMOVED actions are about removing effects, not granting them.
        EntityPotionEffectEvent.Action action = event.getAction();
        if (action == EntityPotionEffectEvent.Action.CLEARED
            || action == EntityPotionEffectEvent.Action.REMOVED) {
            return;
        }

        // Only care about newly added or changed effects.
        if (action != EntityPotionEffectEvent.Action.ADDED
            && action != EntityPotionEffectEvent.Action.CHANGED) {
            return;
        }

        PotionEffectType type = event.getModifiedType();
        if (type == null) {
            return;
        }

        ConsumptionTracker.ConsumptionContext context = tracker.peek(player.getUniqueId());
        if (context == null || context.isStale() || !context.matchesPlayer(player)) {
            return;
        }

        FoodRule rule = context.rule();

        // Remove-effects: suppress the vanilla grant entirely.
        if (rule.shouldRemove(type)) {
            event.setCancelled(true);
            logger.fine(() -> "Suppressed effect " + type.getKey() + " from " + context.materialName() + " for " + player.getName());
            return;
        }

        // Replace-effects: suppress vanilla, defer replacement to the scheduled task.
        EffectRule replacement = rule.getReplacement(type);
        if (replacement != null) {
            event.setCancelled(true);
            logger.fine(() -> "Replaced effect " + type.getKey() + " from " + context.materialName() + " for " + player.getName());
        }
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        tracker.clear(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPluginDisable(@NotNull PluginDisableEvent event) {
        if (event.getPlugin().equals(plugin)) {
            tracker.clearAll();
        }
    }

    private void applyEffects(Player player, ConsumptionTracker.ConsumptionContext context) {
        if (player == null || !player.isOnline()) {
            tracker.clear(context.playerUuid());
            return;
        }

        // Consume only this transaction; a stale task must not remove a newer one.
        if (!tracker.consumeIfCurrent(context.playerUuid(), context)) {
            return;
        }

        if (!config.isEnabled()) {
            return;
        }

        FoodRule rule = context.rule();
        if (rule == null) {
            return;
        }

        // Apply replacements first, then add-effects using the non-forcing overload.
        for (EffectRule replacement : rule.getReplacements()) {
            if (!player.addPotionEffect(replacement.toPotionEffect())) {
                logger.fine(() -> "Replacement effect " + replacement.type().getKey() + " was not applied to " + player.getName());
            }
        }

        for (EffectRule addEffect : rule.getAddEffects()) {
            if (!player.addPotionEffect(addEffect.toPotionEffect())) {
                logger.fine(() -> "Add effect " + addEffect.type().getKey() + " was not applied to " + player.getName());
            }
        }
    }
}
