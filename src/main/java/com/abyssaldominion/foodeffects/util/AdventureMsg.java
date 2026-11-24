package com.abyssaldominion.foodeffects.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

public final class AdventureMsg {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private AdventureMsg() {
    }

    public static Component parse(String input) {
        if (input == null || input.isBlank()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(input);
    }

    public static void send(CommandSender sender, String message) {
        if (sender == null || message == null || message.isBlank()) {
            return;
        }
        sender.sendMessage(parse(message));
    }

    public static void send(CommandSender sender, Component component) {
        if (sender == null || component == null) {
            return;
        }
        sender.sendMessage(component);
    }
}
