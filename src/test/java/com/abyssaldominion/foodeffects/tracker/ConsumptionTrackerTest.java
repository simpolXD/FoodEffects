package com.abyssaldominion.foodeffects.tracker;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsumptionTrackerTest {

    @Test
    void staleScheduledTaskCannotConsumeTheNewerPendingContext() {
        ConsumptionTracker tracker = new ConsumptionTracker();
        UUID playerId = UUID.randomUUID();
        ConsumptionTracker.ConsumptionContext first = context(playerId, 100L);
        ConsumptionTracker.ConsumptionContext second = context(playerId, 100L);
        assertEquals(first, second, "fixture models two same-millisecond consumptions");

        tracker.register(first);
        tracker.register(second);

        assertFalse(tracker.consumeIfCurrent(playerId, first));
        assertSame(second, tracker.peek(playerId));
        assertTrue(tracker.consumeIfCurrent(playerId, second));
        assertEquals(0, tracker.size());
    }

    private static ConsumptionTracker.ConsumptionContext context(UUID playerId, long createdAt) {
        return new ConsumptionTracker.ConsumptionContext(
            playerId, "APPLE", "", null, createdAt);
    }
}
