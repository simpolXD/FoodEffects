package com.abyssaldominion.foodeffects.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigReloadSafetyTest {
    @TempDir Path directory;
    FoodEffectsConfig config() { return new FoodEffectsConfig(directory.toFile(), Logger.getAnonymousLogger()); }
    void write(String settings, String item) throws Exception {
        Files.writeString(directory.resolve("config.yml"), "settings:\n" + settings + "\nitems:\n  APPLE:\n" + item + "\n");
    }
    @Test void wrongSettingTypesAreRejectedWithoutReplacingPreviousConfig() throws Exception {
        write("  enabled: false\n  max-duration-seconds: 600\n  max-amplifier: 5", "    add-effects: {}");
        var config = config(); assertTrue(config.reload());
        for (String bad : new String[]{"  enabled: 'true'", "  max-duration-seconds: 4294967297", "  max-amplifier: 1.5", "  custom-item-matching: 'false'"}) {
            write(bad, "    add-effects: {}");
            assertFalse(config.reload(), bad);
            assertFalse(config.isEnabled());
            assertEquals(600, config.getMaxDurationSeconds());
        }
    }
    @Test void malformedEffectContainersAreRejectedInsteadOfSilentlyIgnored() throws Exception {
        write("  enabled: true", "    add-effects: {}");
        var config = config(); assertTrue(config.reload());
        for (String bad : new String[]{"    add-effects: 42", "    add-effects: [SPEED]", "    replace-effects: false"}) {
            write("  enabled: false", bad);
            assertFalse(config.reload(), bad);
            assertTrue(config.isEnabled());
        }
    }
    @Test void malformedYamlKeepsPreviousSnapshot() throws Exception {
        try (var bukkit = mockStatic(org.bukkit.Bukkit.class)) {
        bukkit.when(org.bukkit.Bukkit::getLogger).thenReturn(Logger.getAnonymousLogger());
        write("  enabled: false", "    add-effects: {}");
        var config = config(); assertTrue(config.reload());
        Files.writeString(directory.resolve("config.yml"), "settings: [");
        assertFalse(config.reload());
        assertFalse(config.isEnabled());
        }
    }
}
