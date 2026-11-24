package com.abyssaldominion.foodeffects.command;

import com.abyssaldominion.foodeffects.AbyssalDominionFoodEffects;
import io.papermc.paper.plugin.configuration.PluginMeta;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FoodEffectsCommandTest {
    AbyssalDominionFoodEffects plugin;
    CommandSender sender;
    FoodEffectsCommand command;
    @BeforeEach void setup() {
        plugin = mock(AbyssalDominionFoodEffects.class);
        sender = mock(CommandSender.class);
        PluginMeta meta = mock(PluginMeta.class);
        when(plugin.getPluginMeta()).thenReturn(meta);
        when(meta.getName()).thenReturn("AbyssalDominionFoodEffects");
        when(meta.getVersion()).thenReturn("9.8.7-test");
        command = new FoodEffectsCommand(plugin);
    }
    private List<Component> messages() {
        var captor = ArgumentCaptor.forClass(Component.class);
        verify(sender, atLeastOnce()).sendMessage(captor.capture());
        return captor.getAllValues();
    }
    private String text() { return messages().stream().map(PlainTextComponentSerializer.plainText()::serialize).reduce("", (a,b) -> a + "\n" + b); }
    @Test void publicVersionIsDynamicStyledAndHasTruthfulProvenance() {
        when(sender.hasPermission("abyssaldominion.foodeffects.version")).thenReturn(true);
        assertTrue(command.onCommand(sender, null, "abyssaldominionfoodeffects", new String[]{"version"}));
        assertTrue(text().contains("v9.8.7-test"));
        assertTrue(text().contains("Author: simp0ol"));
        assertTrue(text().contains("Created: Unknown (not recorded)"));
        for (Component message : messages()) {
            assertEquals(Key.key("minecraft:default"), message.font());
            assertEquals(TextDecoration.State.FALSE, message.decoration(TextDecoration.ITALIC));
            assertTrue(PlainTextComponentSerializer.plainText().serialize(message).startsWith("[Abyssal Dominion · Food Effects] "));
        }
        verify(plugin, never()).reloadPluginConfig();
    }
    @Test void versionPermissionDenialDoesNotLeakMetadata() {
        command.onCommand(sender, null, "foodfx", new String[]{"version"});
        assertTrue(text().contains("permission"));
        assertFalse(text().contains("9.8.7"));
    }
    @Test void permissionAuthorizedConsoleDoesNotNeedOpFlag() {
        when(sender.hasPermission("abyssaldominion.foodeffects.reload")).thenReturn(true);
        when(plugin.reloadPluginConfig()).thenReturn(true);
        command.onCommand(sender, null, "foodfx", new String[]{"reload"});
        verify(plugin).reloadPluginConfig();
        assertTrue(text().contains("successfully"));
    }
    @Test void deniedReloadCannotTouchConfiguration() {
        command.onCommand(sender, null, "foodfx", new String[]{"reload"});
        verify(plugin, never()).reloadPluginConfig();
        assertTrue(text().contains("permission"));
    }
    @Test void failedReloadReportsPreviousValidConfigurationRetained() {
        when(sender.hasPermission("abyssaldominion.foodeffects.reload")).thenReturn(true);
        command.onCommand(sender, null, "foodfx", new String[]{"reload"});
        assertTrue(text().contains("previous valid configuration"));
    }
    @Test void invalidArgumentsDoNotReloadAndHelpIsPermissionFiltered() {
        when(sender.hasPermission("abyssaldominion.foodeffects.version")).thenReturn(true);
        command.onCommand(sender, null, "foodfx", new String[]{"reload", "unexpected"});
        verify(plugin, never()).reloadPluginConfig();
        assertTrue(text().contains("/foodfx version"));
        assertFalse(text().contains("/foodfx reload"));
        assertEquals(List.of("version"), command.onTabComplete(sender, null, "foodfx", new String[]{""}));
    }
}
