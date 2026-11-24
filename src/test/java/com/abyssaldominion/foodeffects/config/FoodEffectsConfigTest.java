package com.abyssaldominion.foodeffects.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodEffectsConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesPdcKeysAndTheirExactStringValues() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("pdc.myplugin:itemid", "super_apple");
        ConfigurationSection section = yaml.getConfigurationSection("pdc");
        List<String> errors = new ArrayList<>();

        List<PdcRequirement> requirements =
            FoodEffectsConfig.parsePdcRequirements(section, "super_apple", errors);

        assertTrue(errors.isEmpty());
        assertEquals(1, requirements.size());
        assertEquals("myplugin:itemid", requirements.getFirst().key().toString());
        assertEquals("super_apple", requirements.getFirst().expectedValue());
    }

    @Test
    void parsesRemovalEffectsAsNamesWithoutDurationFields() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("items.apple.remove-effects", List.of("SPEED", "REGENERATION"));
        List<String> errors = new ArrayList<>();

        List<String> names = FoodEffectsConfig.parseRemovalEffectNames(
            yaml.get("items.apple.remove-effects"), "apple", errors);

        assertEquals(List.of("SPEED", "REGENERATION"), names);
        assertTrue(errors.isEmpty());
    }

    @Test
    void acceptsLegacyRemovalMapKeysWithoutReadingEffectDurations() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("items.apple.remove-effects.SPEED.duration", 15);
        List<String> errors = new ArrayList<>();

        List<String> names = FoodEffectsConfig.parseRemovalEffectNames(
            yaml.getConfigurationSection("items.apple.remove-effects"), "apple", errors);

        assertEquals(List.of("SPEED"), names);
        assertTrue(errors.isEmpty());
    }

    @Test
    void loadsCustomItemsAlongsideGenericMaterialRules() throws IOException {
        Path configPath = tempDir.resolve("config.yml");
        Files.writeString(configPath, """
            settings:
              enabled: true
              max-duration-seconds: 7200
              max-amplifier: 10
              custom-item-matching: true
            items:
              APPLE:
                remove-effects: {}
                replace-effects: {}
                add-effects: {}
            custom-items:
              super_apple:
                material: APPLE
                pdc:
                  "myplugin:itemid": "super_apple"
                remove-effects: {}
                replace-effects: {}
                add-effects: {}
            """);

        StringBuilder messages = new StringBuilder();
        Logger logger = Logger.getLogger("FoodEffectsConfigTest." + tempDir);
        Handler capture = new Handler() {
            @Override
            public void publish(LogRecord record) {
                messages.append(record.getMessage()).append('\n');
            }

            @Override
            public void flush() { }

            @Override
            public void close() { }
        };
        logger.addHandler(capture);
        try {
            FoodEffectsConfig config = new FoodEffectsConfig(tempDir.toFile(), logger);
            assertTrue(config.reload());
            assertTrue(messages.toString().contains("1 material rules, 1 PDC rules"), messages.toString());
        } finally {
            logger.removeHandler(capture);
        }
    }

    @Test
    void rejectsNonStringPdcValues() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("pdc.myplugin:itemid", 42);
        List<String> errors = new ArrayList<>();

        List<PdcRequirement> requirements = FoodEffectsConfig.parsePdcRequirements(
            yaml.getConfigurationSection("pdc"), "super_apple", errors);

        assertTrue(requirements.isEmpty());
        assertFalse(errors.isEmpty());
    }
}
