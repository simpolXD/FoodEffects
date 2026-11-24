package com.abyssaldominion.foodeffects.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodEffectsListenerTest {

    @Test
    void consumptionContextIsRecordedOnlyAfterFinalSuccessfulEventOutcome() throws Exception {
        EventHandler handler = FoodEffectsListener.class
            .getMethod("onPlayerItemConsume", PlayerItemConsumeEvent.class)
            .getAnnotation(EventHandler.class);

        assertEquals(EventPriority.MONITOR, handler.priority());
        assertTrue(handler.ignoreCancelled());
    }
}
