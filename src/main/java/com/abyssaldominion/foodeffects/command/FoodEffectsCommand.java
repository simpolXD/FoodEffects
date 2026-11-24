package com.abyssaldominion.foodeffects.command;

import com.abyssaldominion.foodeffects.AbyssalDominionFoodEffects;
import com.abyssaldominion.suite.RpgMessages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FoodEffectsCommand implements CommandExecutor, TabCompleter {
    private static final String MODULE = "Food Effects";
    private static final String RELOAD = "abyssaldominion.foodeffects.reload";
    private static final String VERSION = "abyssaldominion.foodeffects.version";
    private final AbyssalDominionFoodEffects plugin;

    public FoodEffectsCommand(AbyssalDominionFoodEffects plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) { usage(sender, label); return true; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "version" -> {
                if (allowed(sender, VERSION)) RpgMessages.version(sender, plugin, MODULE);
            }
            case "reload" -> {
                if (!allowed(sender, RELOAD)) return true;
                if (plugin.reloadPluginConfig()) RpgMessages.success(sender, MODULE, "Configuration reloaded successfully.");
                else RpgMessages.error(sender, MODULE, "Configuration reload failed. The previous valid configuration remains active. Check server logs for details.");
            }
            default -> usage(sender, label);
        }
        return true;
    }

    private boolean allowed(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) return true;
        RpgMessages.error(sender, MODULE, "You do not have permission to use this command.");
        return false;
    }

    private void usage(CommandSender sender, String label) {
        RpgMessages.heading(sender, MODULE, "AbyssalDominionFoodEffects Usage:");
        if (sender.hasPermission(VERSION)) RpgMessages.send(sender, MODULE, "/" + label + " version - Show plugin version");
        if (sender.hasPermission(RELOAD)) RpgMessages.send(sender, MODULE, "/" + label + " reload - Reload configuration");
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            if ("version".startsWith(partial) && sender.hasPermission(VERSION)) completions.add("version");
            if ("reload".startsWith(partial) && sender.hasPermission(RELOAD)) completions.add("reload");
        }
        return completions;
    }
}
