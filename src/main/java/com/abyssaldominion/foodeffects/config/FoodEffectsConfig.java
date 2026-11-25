package com.abyssaldominion.foodeffects.config;

import com.abyssaldominion.foodeffects.matcher.ItemMatcher;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class FoodEffectsConfig {

    private static final Map<String, String> LEGACY_EFFECT_KEYS = Map.ofEntries(
        Map.entry("slow", "slowness"),
        Map.entry("fast_digging", "haste"),
        Map.entry("slow_digging", "mining_fatigue"),
        Map.entry("increase_damage", "strength"),
        Map.entry("heal", "instant_health"),
        Map.entry("harm", "instant_damage"),
        Map.entry("jump", "jump_boost"),
        Map.entry("confusion", "nausea"),
        Map.entry("damage_resistance", "resistance")
    );

    private final File configFile;
    private final java.util.logging.Logger logger;
    private volatile boolean enabled = true;
    private volatile int maxDurationSeconds = 7200;
    private volatile int maxAmplifier = 10;
    private volatile boolean customItemMatching = true;

    // Material rules are generic; PDC rules are checked first for more-specific custom items.
    private volatile Map<Material, FoodRule> materialRulesSnapshot = Map.of();
    private volatile Map<Material, List<FoodRule>> pdcMaterialRulesSnapshot = Map.of();
    private volatile Map<String, FoodRule> customIdRulesSnapshot = Map.of();

    private record ItemEntry(String id, ConfigurationSection section, boolean custom) { }

    public FoodEffectsConfig(File dataFolder, java.util.logging.Logger logger) {
        this.configFile = new File(dataFolder, "config.yml");
        this.logger = logger;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxDurationSeconds() {
        return maxDurationSeconds;
    }

    public int getMaxAmplifier() {
        return maxAmplifier;
    }

    public boolean isCustomItemMatching() {
        return customItemMatching;
    }

    /**
     * Publishes immutable rule maps after successful validation.
     */
    private void applySnapshot(Map<Material, FoodRule> newMaterialRules,
                               Map<Material, List<FoodRule>> newPdcMaterialRules,
                               Map<String, FoodRule> newCustomIdRules) {
        Map<Material, List<FoodRule>> immutablePdcRules = new HashMap<>();
        newPdcMaterialRules.forEach((material, rules) -> immutablePdcRules.put(material, List.copyOf(rules)));
        this.materialRulesSnapshot = Map.copyOf(newMaterialRules);
        this.pdcMaterialRulesSnapshot = Map.copyOf(immutablePdcRules);
        this.customIdRulesSnapshot = Map.copyOf(newCustomIdRules);
    }

    /**
     * Returns the rule matching the given item, or null if none matches.
     */
    public FoodRule getRuleForItem(ItemStack item) {
        if (item == null || ItemMatcher.isAirMaterial(item.getType())) {
            return null;
        }

        if (customItemMatching) {
            String customId = ItemMatcher.detectCustomId(item);
            if (!customId.isBlank()) {
                FoodRule customRule = customIdRulesSnapshot.get(customId);
                if (customRule != null && (customRule.material() == Material.AIR
                    || customRule.material() == item.getType())
                    && (!customRule.hasPdcRequirements()
                    || ItemMatcher.matchesPdc(item, customRule.pdcRequirements()))) {
                    return customRule;
                }
            }

            for (FoodRule rule : pdcMaterialRulesSnapshot.getOrDefault(item.getType(), List.of())) {
                if (ItemMatcher.matchesPdc(item, rule.pdcRequirements())) {
                    return rule;
                }
            }
        }

        return materialRulesSnapshot.get(item.getType());
    }

    /**
     * Loads configuration from disk, validates it, and applies it atomically.
     * Returns true on success; on failure, returns false and keeps the old config.
     */
    public synchronized boolean reload() {
        if (!configFile.exists()) {
            logger.warning("Config file missing: " + configFile.getPath());
            return false;
        }

        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException failure) {
            logger.warning("Cannot load config.yml; previous valid configuration retained: " + failure.getMessage());
            return false;
        }

        // Parse settings
        ConfigurationSection settings = config.getConfigurationSection("settings");
        if (settings == null) {
            logger.warning("Missing 'settings' section in config.yml");
            return false;
        }

        if (!validBoolean(settings, "enabled") || !validBoolean(settings, "custom-item-matching")
            || !validInteger(settings, "max-duration-seconds", 1, 7200)
            || !validInteger(settings, "max-amplifier", 0, 20)) {
            logger.warning("Invalid settings types/ranges: enabled/custom-item-matching must be booleans; max-duration-seconds 1-7200 and max-amplifier 0-20 must be integers.");
            return false;
        }
        boolean newEnabled = settings.getBoolean("enabled", true);
        int newMaxDuration = settings.getInt("max-duration-seconds", 7200);
        int newMaxAmplifier = settings.getInt("max-amplifier", 10);
        boolean newCustomMatching = settings.getBoolean("custom-item-matching", true);

        if (newMaxDuration <= 0 || newMaxDuration > 7200) {
            logger.warning("Invalid max-duration-seconds: " + newMaxDuration + ". Must be 1-7200.");
            return false;
        }
        if (newMaxAmplifier < 0 || newMaxAmplifier > 20) {
            logger.warning("Invalid max-amplifier: " + newMaxAmplifier + ". Must be 0-20.");
            return false;
        }

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            logger.warning("Missing 'items' section in config.yml");
            return false;
        }

        List<String> errors = new ArrayList<>();
        ConfigurationSection customItemsSection = config.getConfigurationSection("custom-items");
        if (config.contains("custom-items") && customItemsSection == null) {
            errors.add("'custom-items' must be a section.");
        }

        List<ItemEntry> entries = new ArrayList<>();
        appendEntries(itemsSection, false, entries, errors);
        if (customItemsSection != null) {
            appendEntries(customItemsSection, true, entries, errors);
        }

        Map<Material, FoodRule> newMaterialRules = new HashMap<>();
        Map<Material, List<FoodRule>> newPdcMaterialRules = new HashMap<>();
        Map<String, FoodRule> newCustomIdRules = new HashMap<>();

        for (ItemEntry entry : entries) {
            String key = entry.id();
            ConfigurationSection itemSection = entry.section();
            int errorsBeforeItem = errors.size();

            String materialName = itemSection.getString("material", entry.custom() ? null : key);
            Material material = Material.AIR;
            if (materialName != null) {
                try {
                    material = Material.valueOf(materialName.toUpperCase(Locale.ROOT));
                    if (material == Material.AIR) {
                        errors.add("Item '" + key + "' cannot use AIR as its material.");
                    }
                } catch (IllegalArgumentException ex) {
                    errors.add("Item '" + key + "' has unknown material '" + materialName + "'.");
                }
            }

            List<PdcRequirement> pdcRequirements = parsePdcRequirements(
                itemSection.getConfigurationSection("pdc"), key, errors);
            if (itemSection.contains("pdc") && itemSection.getConfigurationSection("pdc") == null) {
                errors.add("Item '" + key + "' has an invalid 'pdc' section.");
            }
            if (entry.custom()) {
                if (material == Material.AIR) {
                    errors.add("Custom item '" + key + "' must declare a valid material.");
                }
                if (pdcRequirements.isEmpty()) {
                    errors.add("Custom item '" + key + "' must declare at least one PDC key/value requirement.");
                }
            }

            List<NamespacedKey> removeEffects = parseRemovalEffects(
                itemSection.get("remove-effects"), key, errors);
            for (String container : List.of("replace-effects", "add-effects")) {
                Object raw = itemSection.get(container);
                if (raw != null && !(raw instanceof ConfigurationSection)
                    && !(raw instanceof List<?> list && list.isEmpty())) {
                    errors.add("Item '" + key + "' " + container + " must be a map (or an empty list).");
                }
            }
            Map<PotionEffectType, EffectRule> replaceEffects = parseEffectMap(itemSection.getConfigurationSection("replace-effects"), newMaxDuration, newMaxAmplifier, key, "replace-effects", errors);
            List<EffectRule> addEffects = parseEffectList(itemSection.getConfigurationSection("add-effects"), newMaxDuration, newMaxAmplifier, key, "add-effects", errors);

            if (errors.size() > errorsBeforeItem) {
                continue;
            }

            FoodRule rule = new FoodRule(material, key, pdcRequirements, removeEffects, replaceEffects, addEffects);
            if (material == Material.AIR) {
                newCustomIdRules.put(key, rule);
            } else if (!pdcRequirements.isEmpty()) {
                newPdcMaterialRules.computeIfAbsent(material, ignored -> new ArrayList<>()).add(rule);
            } else {
                FoodRule existing = newMaterialRules.put(material, rule);
                if (existing != null) {
                    logger.warning("Duplicate material rule for " + material + " at item '" + key + "'. Overwriting previous entry.");
                }
            }
        }

        if (!errors.isEmpty()) {
            logger.warning("Config validation failed with " + errors.size() + " error(s):");
            for (String error : errors) {
                logger.warning("  - " + error);
            }
            return false;
        }

        // All validations passed; apply atomically
        this.enabled = newEnabled;
        this.maxDurationSeconds = newMaxDuration;
        this.maxAmplifier = newMaxAmplifier;
        this.customItemMatching = newCustomMatching;
        newPdcMaterialRules.values().forEach(rules -> rules.sort(
            Comparator.comparingInt((FoodRule rule) -> rule.pdcRequirements().size()).reversed()));
        applySnapshot(newMaterialRules, newPdcMaterialRules, newCustomIdRules);

        int pdcRuleCount = newPdcMaterialRules.values().stream().mapToInt(List::size).sum();
        logger.info("Configuration reloaded successfully. Loaded " + newMaterialRules.size()
            + " material rules, " + pdcRuleCount + " PDC rules, and "
            + newCustomIdRules.size() + " custom-id rules.");
        return true;
    }

    private static boolean validBoolean(ConfigurationSection section, String key) {
        return !section.contains(key) || section.get(key) instanceof Boolean;
    }

    private static boolean validInteger(ConfigurationSection section, String key, long min, long max) {
        if (!section.contains(key)) return true;
        Object value = section.get(key);
        return (value instanceof Integer || value instanceof Long)
            && ((Number) value).longValue() >= min && ((Number) value).longValue() <= max;
    }

    private void appendEntries(ConfigurationSection section,
                               boolean custom,
                               List<ItemEntry> entries,
                               List<String> errors) {
        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(key);
            if (itemSection == null) {
                errors.add("Item '" + key + "' is not a valid section.");
                continue;
            }
            entries.add(new ItemEntry(key, itemSection, custom));
        }
    }

    static List<String> parseRemovalEffectNames(Object rawValue,
                                                String itemKey,
                                                List<String> errors) {
        if (rawValue == null) {
            return List.of();
        }
        if (rawValue instanceof ConfigurationSection section) {
            return List.copyOf(section.getKeys(false));
        }
        if (!(rawValue instanceof List<?> configuredNames)) {
            errors.add("Item '" + itemKey + "' remove-effects must be a list of effect names or a map.");
            return List.of();
        }

        List<String> names = new ArrayList<>();
        for (Object configuredName : configuredNames) {
            if (configuredName instanceof String name && !name.isBlank()) {
                names.add(name);
            } else {
                errors.add("Item '" + itemKey + "' remove-effects entries must be non-empty strings.");
            }
        }
        return List.copyOf(names);
    }

    private List<NamespacedKey> parseRemovalEffects(Object rawValue,
                                                    String itemKey,
                                                    List<String> errors) {
        List<NamespacedKey> keys = new ArrayList<>();
        for (String name : parseRemovalEffectNames(rawValue, itemKey, errors)) {
            PotionEffectType type = resolveEffectType(name);
            if (type == null) {
                errors.add("Item '" + itemKey + "' has unknown effect type '" + name + "' in remove-effects.");
            } else {
                keys.add(type.getKey());
            }
        }
        return keys;
    }

    static List<PdcRequirement> parsePdcRequirements(ConfigurationSection section,
                                                    String itemKey,
                                                    List<String> errors) {
        if (section == null) {
            return List.of();
        }

        List<PdcRequirement> requirements = new ArrayList<>();
        for (String configuredKey : section.getKeys(false)) {
            Object rawValue = section.get(configuredKey);
            if (!(rawValue instanceof String value)) {
                errors.add("Item '" + itemKey + "' PDC value for key '" + configuredKey + "' must be a string.");
                continue;
            }

            String[] parts = configuredKey.split(":", -1);
            if (parts.length > 2 || parts[0].isBlank()
                || (parts.length == 2 && parts[1].isBlank())) {
                errors.add("Item '" + itemKey + "' has invalid PDC key '" + configuredKey + "'.");
                continue;
            }
            String namespace = parts.length == 2 ? parts[0] : "minecraft";
            String keyPart = parts.length == 2 ? parts[1] : parts[0];
            try {
                requirements.add(new PdcRequirement(new NamespacedKey(namespace, keyPart), value));
            } catch (IllegalArgumentException ex) {
                errors.add("Item '" + itemKey + "' has invalid PDC key '" + configuredKey + "'.");
            }
        }
        return requirements;
    }

    private List<EffectRule> parseEffectList(ConfigurationSection section,
                                              int maxDuration,
                                              int maxAmplifier,
                                              String itemKey,
                                              String listName,
                                              List<String> errors) {
        if (section == null || section.getKeys(false).isEmpty()) {
            return List.of();
        }
        List<EffectRule> rules = new ArrayList<>();
        for (String effectName : section.getKeys(false)) {
            ConfigurationSection effectSection = section.getConfigurationSection(effectName);
            if (effectSection == null) {
                errors.add("Item '" + itemKey + "' effect '" + effectName + "' in " + listName + " is not a valid section.");
                continue;
            }
            EffectRule rule = parseEffectRule(effectName, effectSection, maxDuration, maxAmplifier, itemKey, listName, errors);
            if (rule != null) {
                rules.add(rule);
            }
        }
        return rules;
    }

    private Map<PotionEffectType, EffectRule> parseEffectMap(ConfigurationSection section,
                                                              int maxDuration,
                                                              int maxAmplifier,
                                                              String itemKey,
                                                              String mapName,
                                                              List<String> errors) {
        if (section == null || section.getKeys(false).isEmpty()) {
            return Map.of();
        }
        Map<PotionEffectType, EffectRule> map = new LinkedHashMap<>();
        for (String effectName : section.getKeys(false)) {
            ConfigurationSection effectSection = section.getConfigurationSection(effectName);
            if (effectSection == null) {
                errors.add("Item '" + itemKey + "' effect '" + effectName + "' in " + mapName + " is not a valid section.");
                continue;
            }
            EffectRule rule = parseEffectRule(effectName, effectSection, maxDuration, maxAmplifier, itemKey, mapName, errors);
            if (rule != null) {
                map.put(rule.type(), rule);
            }
        }
        return map;
    }

    private EffectRule parseEffectRule(String effectName,
                                       ConfigurationSection section,
                                       int maxDuration,
                                       int maxAmplifier,
                                       String itemKey,
                                       String context,
                                       List<String> errors) {
        // Resolve effect type
        PotionEffectType type = resolveEffectType(effectName);
        if (type == null) {
            errors.add("Item '" + itemKey + "' has unknown effect type '" + effectName + "' in " + context + ".");
            return null;
        }

        int durationSeconds = section.getInt("duration", -1);
        if (durationSeconds <= 0) {
            errors.add("Item '" + itemKey + "' effect '" + effectName + "' has invalid duration: " + durationSeconds + ". Must be > 0.");
            return null;
        }
        int durationTicks;
        try {
            durationTicks = toDurationTicks(durationSeconds, maxDuration);
        } catch (IllegalArgumentException ex) {
            errors.add("Item '" + itemKey + "' effect '" + effectName + "' duration "
                + durationSeconds + "s exceeds max " + maxDuration + "s.");
            return null;
        }

        int amplifier = section.getInt("amplifier", 0);
        if (amplifier < 0 || amplifier > maxAmplifier) {
            errors.add("Item '" + itemKey + "' effect '" + effectName + "' amplifier " + amplifier + " exceeds allowed range 0-" + maxAmplifier + ".");
            return null;
        }

        boolean ambient = section.getBoolean("ambient", false);
        boolean particles = section.getBoolean("particles", true);
        boolean icon = section.getBoolean("icon", true);

        return new EffectRule(type, durationTicks, amplifier, ambient, particles, icon);
    }

    static int toDurationTicks(int durationSeconds, int maxDurationSeconds) {
        if (durationSeconds <= 0 || maxDurationSeconds <= 0
            || durationSeconds > maxDurationSeconds
            || durationSeconds > Integer.MAX_VALUE / 20) {
            throw new IllegalArgumentException("Duration is outside the allowed range.");
        }
        return durationSeconds * 20;
    }

    private PotionEffectType resolveEffectType(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        String normalized = name.toLowerCase(Locale.ROOT);
        PotionEffectType effect = resolveEffectKey(normalized);
        if (effect != null) {
            return effect;
        }

        String legacyKey = LEGACY_EFFECT_KEYS.get(normalized);
        return legacyKey == null ? null : resolveEffectKey(legacyKey);
    }

    private PotionEffectType resolveEffectKey(String keyText) {
        try {
            NamespacedKey key = NamespacedKey.fromString(keyText);
            return key == null ? null : Registry.MOB_EFFECT.get(key);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return null;
        }
    }

    /**
     * Creates the default config file from the packaged resource if missing.
     */
    public void saveDefaultConfig(File dataFolder) {
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            // Copy from jar resource
            try (var in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (in == null) {
                    logger.warning("Default config.yml resource not found in plugin JAR.");
                    return;
                }
                try (var out = new java.io.FileOutputStream(configFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to write default config.yml", e);
            }
        }
    }

    /**
     * Reloads configuration from disk. Exposed for external callers.
     */
    public boolean reloadConfig() {
        return reload();
    }
}
