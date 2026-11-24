package com.abyssaldominion.foodeffects.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DurationBoundsTest {

    @Test
    void convertsValidSecondsWithoutOverflow() {
        assertEquals(20, FoodEffectsConfig.toDurationTicks(1, 7200));
        assertEquals(144000, FoodEffectsConfig.toDurationTicks(7200, 7200));
    }

    @Test
    void rejectsDurationsAboveConfiguredMaximumBeforeMultiplication() {
        assertThrows(IllegalArgumentException.class,
            () -> FoodEffectsConfig.toDurationTicks(Integer.MAX_VALUE, 7200));
    }
}
