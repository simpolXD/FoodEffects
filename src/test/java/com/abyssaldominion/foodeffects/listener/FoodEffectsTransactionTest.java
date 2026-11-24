package com.abyssaldominion.foodeffects.listener;

import com.abyssaldominion.foodeffects.AbyssalDominionFoodEffects;
import com.abyssaldominion.foodeffects.config.FoodEffectsConfig;
import com.abyssaldominion.foodeffects.config.FoodRule;
import com.abyssaldominion.foodeffects.tracker.ConsumptionTracker;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FoodEffectsTransactionTest {
    AbyssalDominionFoodEffects plugin;
    FoodEffectsConfig config;
    ConsumptionTracker tracker;
    FoodEffectsListener listener;
    Player player;
    ItemStack item;
    FoodRule rule;
    BukkitScheduler scheduler;
    List<Runnable> tasks;
    List<BukkitTask> handles;
    @BeforeEach void setup() {
        plugin = mock(AbyssalDominionFoodEffects.class);
        var server = mock(Server.class);
        scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        config = mock(FoodEffectsConfig.class);
        when(config.isEnabled()).thenReturn(true);
        tracker = new ConsumptionTracker();
        listener = new FoodEffectsListener(plugin, config, tracker, Logger.getAnonymousLogger());
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.ENCHANTED_GOLDEN_APPLE);
        rule = new FoodRule(Material.ENCHANTED_GOLDEN_APPLE, "", List.of(), List.of(NamespacedKey.minecraft("regeneration")), Map.of(), List.of());
        when(config.getRuleForItem(item)).thenReturn(rule);
        tasks = new ArrayList<>(); handles = new ArrayList<>();
        when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), eq(1L))).thenAnswer(call -> {
            tasks.add(call.getArgument(1)); var handle = mock(BukkitTask.class); handles.add(handle); return handle;
        });
    }
    PlayerItemConsumeEvent event(boolean cancelled) {
        var event = mock(PlayerItemConsumeEvent.class);
        when(event.getPlayer()).thenReturn(player); when(event.getItem()).thenReturn(item); when(event.isCancelled()).thenReturn(cancelled);
        return event;
    }
    @Test void cancelledConsumptionCannotRegisterOrSchedule() {
        listener.onPlayerItemConsume(event(true));
        assertEquals(0, tracker.size()); assertTrue(tasks.isEmpty());
    }
    @Test void oldOfflineCallbackCannotClearNewerTransaction() {
        listener.onPlayerItemConsume(event(false));
        var newer = new ConsumptionTracker.ConsumptionContext(player.getUniqueId(), "APPLE", "", rule, System.currentTimeMillis());
        tracker.register(newer);
        when(player.isOnline()).thenReturn(false);
        tasks.getFirst().run();
        assertSame(newer, tracker.peek(player.getUniqueId()));
    }
    @Test void laterUnmatchedConsumptionInvalidatesPreviousContext() {
        listener.onPlayerItemConsume(event(false));
        when(config.getRuleForItem(item)).thenReturn(null);
        listener.onPlayerItemConsume(event(false));
        assertEquals(0, tracker.size());
        verify(handles.getFirst()).cancel();
    }
    @Test void supersededTaskIsCancelled() {
        listener.onPlayerItemConsume(event(false)); listener.onPlayerItemConsume(event(false));
        verify(handles.getFirst()).cancel(); assertEquals(1, tracker.size());
        tasks.getFirst().run(); assertEquals(1, tracker.size());
    }
    @Test void quitCancelsPendingTask() {
        listener.onPlayerItemConsume(event(false));
        var quit = mock(PlayerQuitEvent.class); when(quit.getPlayer()).thenReturn(player);
        listener.onPlayerQuit(quit);
        assertEquals(0, tracker.size()); verify(handles.getFirst()).cancel();
    }
    @Test void disableCancelsTasksAndPendingContexts() {
        listener.onPlayerItemConsume(event(false));
        listener.onPluginDisable(new PluginDisableEvent(plugin));
        assertEquals(0, tracker.size()); verify(handles.getFirst()).cancel();
        tasks.getFirst().run(); verify(player, never()).addPotionEffect(any());
    }
}
