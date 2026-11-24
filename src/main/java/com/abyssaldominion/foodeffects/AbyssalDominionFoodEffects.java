package com.abyssaldominion.foodeffects;

import com.abyssaldominion.foodeffects.command.FoodEffectsCommand;
import com.abyssaldominion.foodeffects.config.FoodEffectsConfig;
import com.abyssaldominion.foodeffects.listener.FoodEffectsListener;
import com.abyssaldominion.foodeffects.tracker.ConsumptionTracker;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class AbyssalDominionFoodEffects extends JavaPlugin {

    private FoodEffectsConfig config;
    private ConsumptionTracker tracker;
    private FoodEffectsListener listener;

    @Override
    public void onEnable() {
        try {
            this.tracker = new ConsumptionTracker();
            this.config = new FoodEffectsConfig(getDataFolder(), getLogger());
            config.saveDefaultConfig(getDataFolder());

            boolean loaded = config.reloadConfig();
            if (!loaded) {
                getLogger().severe("Failed to load configuration. Plugin disabled.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            this.listener = new FoodEffectsListener(this, config, tracker, getLogger());
            getServer().getPluginManager().registerEvents(listener, this);

            // Register command
            FoodEffectsCommand command = new FoodEffectsCommand(this);
            String cmdName = "foodfx";
            if (getCommand(cmdName) == null) {
                getLogger().severe("Command '" + cmdName + "' is not declared in plugin.yml. Plugin disabled.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getCommand(cmdName).setExecutor(command);
            getCommand(cmdName).setTabCompleter(command);

            getLogger().info("AbyssalDominionFoodEffects v" + getPluginMeta().getVersion() + " enabled.");
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Unexpected error during plugin enable", t);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (tracker != null) {
            tracker.clearAll();
        }
        getLogger().info("AbyssalDominionFoodEffects disabled.");
    }

    /**
     * Reloads configuration and returns true on success.
     */
    public boolean reloadPluginConfig() {
        if (config == null) {
            return false;
        }
        return config.reloadConfig();
    }
}
