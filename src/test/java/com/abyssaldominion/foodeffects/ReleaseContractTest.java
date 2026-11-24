package com.abyssaldominion.foodeffects;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class ReleaseContractTest {
    @Test void descriptorExposesPublicFullNameVersionWithOptionalPapi() throws Exception {
        var descriptor = YamlConfiguration.loadConfiguration(new InputStreamReader(getClass().getResourceAsStream("/plugin.yml"), StandardCharsets.UTF_8));
        assertEquals("1.2.0", descriptor.getString("version"));
        assertEquals("26.2", descriptor.getString("api-version"));
        assertTrue(descriptor.getStringList("commands.foodfx.aliases").contains("abyssaldominionfoodeffects"));
        assertFalse(descriptor.contains("commands.foodfx.permission"));
        assertTrue(descriptor.getBoolean("permissions.abyssaldominion.foodeffects.version.default"));
        assertEquals("op", descriptor.getString("permissions.abyssaldominion.foodeffects.reload.default"));
        assertTrue(descriptor.getStringList("softdepend").contains("PlaceholderAPI"));
        assertFalse(descriptor.contains("author"));
        assertFalse(descriptor.contains("authors"));
    }
    @Test void identityNeverInventsOriginalCreationDate() throws Exception {
        try (var stream = getClass().getResourceAsStream("/suite-info.properties")) {
            assertNotNull(stream);
            Properties properties = new Properties();
            properties.load(stream);
            assertEquals("Unknown (not recorded)", properties.getProperty("created"));
            assertEquals("2026-11-06", properties.getProperty("updated"));
        }
    }
}
