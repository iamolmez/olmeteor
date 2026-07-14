package com.olmeteors.meteorevents.config;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.olmeteors.meteorevents.event.MeteorType;
import com.olmeteors.meteorevents.event.MeteorFallMode;
import com.olmeteors.meteorevents.event.RadiusShape;
import com.olmeteors.meteorevents.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages all configuration files for the MeteorEvents plugin.
 * Supports main config.yml and per-meteor-type configuration sections.
 * <p>
 * Provides centralized message lookup with placeholder replacement
 * for full internationalisation support.
 */
public final class ConfigManager {

    private final MeteorPlugin plugin;
    private final File dataFolder;
    private final File schematicsFolder;
    private final File inventoriesFolder;

    private FileConfiguration config;
    private FileConfiguration commandCategories;
    private FileConfiguration setupData;
    private LocaleManager localeManager;
    private final Map<String, MeteorTypeConfig> typeConfigCache;

    public ConfigManager(MeteorPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        this.schematicsFolder = new File(dataFolder, "schematics");
        this.inventoriesFolder = new File(dataFolder, "inventories");
        this.typeConfigCache = new ConcurrentHashMap<>();
    }

    // ────────────────────────────────────────────────────────────────
    //  Configuration Loading
    // ────────────────────────────────────────────────────────────────

    /**
     * Loads (or reloads) the main configuration and all type-specific configurations.
     */
    public void loadConfiguration() {
        createDirectories();
        saveDefaultConfig();

        this.config = YamlConfiguration.loadConfiguration(new File(dataFolder, "config.yml"));

        final File categoriesFile = new File(dataFolder, "command-categories.yml");
        if (!categoriesFile.exists()) {
            plugin.saveResource("command-categories.yml", false);
        }
        this.commandCategories = YamlConfiguration.loadConfiguration(categoriesFile);
        this.setupData = YamlConfiguration.loadConfiguration(new File(dataFolder, "setups.yml"));

        final InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)));
            config.options().copyDefaults(true);
            try {
                config.save(new File(dataFolder, "config.yml"));
            } catch (IOException error) {
                plugin.getLogger().log(Level.WARNING,
                        "Could not write newly added default settings to config.yml", error);
            }
        }

        typeConfigCache.clear();
        validateSchematicReferences(config);

        // Initialise the locale system after loading config
        final String localeSetting = config.getString("locale", "auto");
        if (this.localeManager == null) {
            this.localeManager = new LocaleManager(plugin, dataFolder.toPath());
        }
        this.localeManager.init(localeSetting);

        // Load message prefixes for MessageUtil convenience methods
        loadMessagePrefixes();

        plugin.getLogger().info("Configuration loaded successfully");
    }

    private void createDirectories() {
        try {
            Files.createDirectories(dataFolder.toPath());
            Files.createDirectories(schematicsFolder.toPath());
            Files.createDirectories(inventoriesFolder.toPath());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create plugin data directories", e);
        }
    }

    private void saveDefaultConfig() {
        final File configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  Message System (Multi-Locale)
    // ────────────────────────────────────────────────────────────────

    /**
     * Retrieves a formatted message with locale cascade support.
     * <p>
     * Lookup order:
     * <ol>
     *   <li>{@code config.yml → messages.<path>} (admin overrides)</li>
     *   <li>Active locale file ({@code lang/messages_XX.yml})</li>
     *   <li>English fallback ({@code lang/messages_en.yml})</li>
     *   <li>Missing-translation indicator</li>
     * </ol>
     *
     * @param path         dot-separated path under {@code messages} (e.g. {@code "command.help.title"})
     * @param placeholders optional vararg of {@code "key", "value"} pairs
     * @return the formatted message string
     * @throws IllegalArgumentException if placeholders are not in key-value pairs
     */
    public @NotNull String getMessage(@NotNull String path, @NotNull String @NotNull ... placeholders) {
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders must be provided as key-value pairs");
        }

        final String fullPath = "messages." + path;

        // Locale files win by default. Older installations contain a complete
        // English messages section in config.yml, which previously shadowed
        // messages_tr.yml even when the active locale was Turkish.
        final boolean useConfigOverrides = config.getBoolean("locale-config-overrides", false);
        String message = useConfigOverrides ? config.getString(fullPath) : null;

        // 2. Check LocaleManager (locale-specific file) — get RAW message without placeholders
        //    to avoid double placeholder application (ConfigManager applies them at step 4)
        if (localeManager != null && (message == null || message.isEmpty())) {
            // Use empty varargs array to disambiguate the overloaded getMessage()
            // We want the 3-param version: getMessage(path, fallback, placeholders...)
            // with null fallback so ConfigManager handles its own fallback logic.
            final String localeMsg = localeManager.getMessage(path, (String) null, new String[0]);
            if (localeMsg != null && !localeMsg.isEmpty()) {
                message = localeMsg;
            }
        }


        // Optional legacy/admin fallback. Set locale-config-overrides: true to
        // deliberately make config.yml messages override the selected language.
        if ((message == null || message.isEmpty()) && !useConfigOverrides) {
            message = config.getString(fullPath);
        }

        // 3. Missing translation fallback
        if (message == null || message.isEmpty()) {
            return "&c[Missing translation: " + fullPath
                    + " | locale: " + (localeManager != null ? localeManager.getActiveLocale() : "?") + "]";
        }

        // 4. Apply placeholders (single application, safe from double-replacement)
        return applyPlaceholders(message, placeholders);
    }

    /**
     * Convenience: retrieves a message and sends it via {@code MessageUtil.sendMessage}.
     *
     * @param sender       the recipient
     * @param path         dot-separated path under {@code messages}
     * @param placeholders optional placeholder pairs
     */
    public void sendMessage(@NotNull org.bukkit.command.CommandSender sender,
                            @NotNull String path,
                            @NotNull String @NotNull ... placeholders) {
        final String msg = getMessage(path, placeholders);
        com.olmeteors.meteorevents.util.MessageUtil.sendMessage(sender, msg);
    }

    /**
     * Retrieves a multi-line message and returns it as a list of lines.
     * Uses the same locale cascade as {@link #getMessage(String, String...)}.
     * Useful for item lore or multi-line display texts.
     *
     * @param path         dot-separated path under {@code messages}
     * @param placeholders optional vararg of {@code "key", "value"} pairs
     * @return list of non-empty lines
     */
    public @NotNull List<@NotNull String> getMessageLines(@NotNull String path,
                                                           @NotNull String @NotNull ... placeholders) {
        final String raw = getMessage(path, placeholders);
        if (raw.contains("Missing translation")) {
            return List.of(raw);
        }
        // Split on newlines, keep empty lines for lore spacing,
        // but trim trailing empty lines
        final String[] lines = raw.split("\n", -1);
        final List<String> result = new ArrayList<>();
        int lastNonEmpty = -1;
        for (int i = 0; i < lines.length; i++) {
            result.add(lines[i]);
            if (!lines[i].isEmpty()) {
                lastNonEmpty = i;
            }
        }
        // Trim trailing empty lines
        if (lastNonEmpty >= 0 && lastNonEmpty < result.size() - 1) {
            result.subList(lastNonEmpty + 1, result.size()).clear();
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Applies global and caller-supplied placeholders to a message string.
     */
    private @NotNull String applyPlaceholders(@NotNull String message,
                                               @NotNull String @NotNull ... placeholders) {
        String result = message;

        // Global placeholders
        // Handle null pluginMeta defensively (e.g., in unit tests without full Paper environment)
        final var pm = plugin.getPluginMeta();
        result = result.replace("%version%", pm != null ? pm.getVersion() : "?");
        result = result.replace("%prefix%", "&8[&6Ol&eMeteor&8]");

        // Caller-supplied
        for (int i = 0; i < placeholders.length; i += 2) {
            result = result.replace("%" + placeholders[i] + "%",
                    placeholders[i + 1] != null ? placeholders[i + 1] : "");
        }

        return result;
    }

    /**
     * Loads custom message prefixes into {@link MessageUtil}.
     * Reads from {@code messages.message_prefix.*} in the loaded config.
     */
    private void loadMessagePrefixes() {
        final Map<String, String> prefixes = new HashMap<>();
        final String base = "messages.message_prefix.";
        for (final String key : List.of("chat", "broadcast", "warning", "error", "success", "info")) {
            final String value = config.getString(base + key);
            if (value != null && !value.isEmpty()) {
                prefixes.put(key, value
                        .replace("MeteorEvents", "OlMeteor")
                        .replace("Meteor Events", "OlMeteor"));
            }
        }
        MessageUtil.loadPrefixes(prefixes);
    }

    /** Config-driven grouping used by /olmeteor and /olmeteor help. */
    public @NotNull List<CommandHelpCategory> getCommandHelpCategories() {
        ConfigurationSection categories = commandCategories == null ? null
                : commandCategories.getConfigurationSection("categories");
        if (categories == null) {
            categories = config.getConfigurationSection("command-help.categories");
        }
        if (categories == null) return List.of();
        final List<CommandHelpCategory> result = new ArrayList<>();
        for (final String key : categories.getKeys(false)) {
            final ConfigurationSection section = categories.getConfigurationSection(key);
            if (section == null || !section.getBoolean("enabled", true)) continue;
            final String title = section.getString("title", key);
            final List<String> commands = section.getStringList("commands").stream()
                    .filter(line -> line != null && !line.isBlank()).toList();
            result.add(new CommandHelpCategory(title, commands));
        }
        return List.copyOf(result);
    }

    public record CommandHelpCategory(@NotNull String title,
                                      @NotNull List<String> commands) {}

    // ────────────────────────────────────────────────────────────────
    //  Meteor Type Display Names (locale-aware)
    // ────────────────────────────────────────────────────────────────

    /**
     * Returns a locale-aware display name for a meteor type.
     * Falls back to {@link MeteorType#displayName()} if not configured.
     */
    public @NotNull String getMeteorTypeName(@NotNull MeteorType type) {
        return getMessage("meteor_type." + type.name().toLowerCase(Locale.ROOT) + ".name");
    }

    /**
     * Returns a locale-aware color code for a meteor type.
     * Falls back to {@link MeteorType#colorCode()} if not configured.
     */
    public @NotNull String getMeteorTypeColor(@NotNull MeteorType type) {
        final String value = getMessage("meteor_type." + type.name().toLowerCase(Locale.ROOT) + ".color");
        return value.contains("Missing translation") ? type.colorCode() : value;
    }

    /**
     * Returns a locale-aware display name for a difficulty level.
     * Falls back to {@link MeteorType.Difficulty#displayName()} if not configured.
     */
    public @NotNull String getDifficultyName(@NotNull MeteorType.Difficulty difficulty) {
        final String value = getMessage("meteor_type.difficulty."
                + difficulty.name().toLowerCase(Locale.ROOT) + ".name");
        return value.contains("Missing translation") ? difficulty.displayName() : value;
    }

    /**
     * Returns a locale-aware color code for a difficulty level.
     * Falls back to {@link MeteorType.Difficulty#colorCode()} if not configured.
     */
    public @NotNull String getDifficultyColor(@NotNull MeteorType.Difficulty difficulty) {
        final String value = getMessage("meteor_type.difficulty."
                + difficulty.name().toLowerCase(Locale.ROOT) + ".color");
        return value.contains("Missing translation") ? difficulty.colorCode() : value;
    }

    /**
     * Returns the {@link LocaleManager} used by this configuration.
     */
    public @NotNull LocaleManager getLocaleManager() {
        if (localeManager == null) {
            localeManager = new LocaleManager(plugin, dataFolder.toPath());
            localeManager.init(config != null ? config.getString("locale", "auto") : "auto");
        }
        return localeManager;
    }

    // ---- Config Getters (unchanged) ----

    public int getMaxLocationAttempts() {
        return config.getInt("location-finder.max-attempts", 30);
    }

    public int getBufferZone() {
        return config.getInt("location-finder.buffer-zone-from-claims", 50);
    }

    public int getTerrainVarianceTolerance() {
        return config.getInt("location-finder.terrain-variance-tolerance", 5);
    }

    public int getMinEventDistance() {
        return config.getInt("location-finder.min-distance-from-spawn", 100);
    }

    public int getMaxEventDistance() {
        return config.getInt("location-finder.max-distance-from-spawn", 5000);
    }

    public boolean isTownyRequireWilderness() {
        return config.getBoolean("location-finder.towny-require-wilderness", true);
    }

    public boolean isWorldGuardCheckClaims() {
        return config.getBoolean("location-finder.worldguard-check-claims", true);
    }

    public @NotNull LocationPreset getLocationPreset() {
        return getLocationPreset(config.getString("location-finder.active-preset", "flat_surface"));
    }

    public @NotNull LocationPreset getLocationPreset(@Nullable String presetName) {
        String name = presetName == null ? "flat_surface"
                : presetName.toLowerCase(Locale.ROOT).replace('-', '_');
        if (!config.isConfigurationSection("location-finder.presets." + name)) {
            name = "flat_surface";
        }
        final String base = "location-finder.presets." + name + ".";
        return new LocationPreset(name,
                config.getString(base + "mode", "surface").toLowerCase(Locale.ROOT),
                config.getBoolean(base + "require-flat", true),
                config.getBoolean(base + "allow-water", false),
                config.getBoolean(base + "allow-lava", false),
                config.getInt(base + "min-y", -64),
                config.getInt(base + "max-y", 320),
                config.getStringList(base + "allowed-floor-blocks"));
    }

    public boolean setLocationPreset(@NotNull String preset, @Nullable Integer minY,
                                     @Nullable Integer maxY) {
        final String normalized = preset.toLowerCase(Locale.ROOT).replace('-', '_');
        if (!config.isConfigurationSection("location-finder.presets." + normalized)) return false;
        if (minY != null && maxY != null && minY > maxY) return false;
        config.set("location-finder.active-preset", normalized);
        if (minY != null) config.set("location-finder.presets." + normalized + ".min-y", minY);
        if (maxY != null) config.set("location-finder.presets." + normalized + ".max-y", maxY);
        return saveConfigFile("location preset");
    }

    public @NotNull List<String> getLocationPresetNames() {
        final ConfigurationSection section = config.getConfigurationSection("location-finder.presets");
        return section == null ? List.of() : section.getKeys(false).stream().sorted().toList();
    }

    public int getChunkForceLoadRadius() {
        return config.getInt("event.chunk-force-load-radius", 3);
    }

    public int getRadiationDamagePerSecond() {
        return config.getInt("event.hazards.radiation-damage-per-second", 2);
    }

    public double getWindChargeKnockbackMultiplier() {
        return config.getDouble("event.hazards.wind-charge-knockback-multiplier", 1.5);
    }

    public int getWindChargeInterval() {
        return config.getInt("event.hazards.wind-charge-interval-ticks", 100);
    }

    public boolean isElytraDisabled() {
        return config.getBoolean("event.hazards.disable-elytra", true);
    }

    public boolean isEnderPearlDisabled() {
        return config.getBoolean("event.hazards.disable-ender-pearl", true);
    }

    public int getBossDamageThreshold() {
        return config.getInt("event.vault.boss-damage-threshold-percent", 10);
    }

    public int getVaultOpenDelayTicks() {
        return config.getInt("event.vault.open-delay-ticks", 100);
    }

    public int getCompletionCleanupDelaySeconds() {
        return Math.max(0, config.getInt("event.completion.cleanup-delay-seconds", 60));
    }

    public int getUnattendedTimeoutMinutes() {
        return Math.max(1, config.getInt("event.completion.unattended-timeout-minutes", 30));
    }

    public boolean isCrashRecoveryEnabled() {
        return config.getBoolean("event.recovery.enabled", true);
    }

    public boolean isTrackingBossBarEnabled() { return config.getBoolean("event.tracking.enabled", true); }
    public int getTrackingDistance() { return Math.max(32, config.getInt("event.tracking.max-distance", 2000)); }
    public boolean isTpsGuardEnabled() { return config.getBoolean("automatic-events.tps-guard.enabled", true); }
    public double getMinimumAutoTps() { return config.getDouble("automatic-events.tps-guard.minimum-tps", 18.0); }
    public int getTpsRetryMinutes() { return Math.max(1, config.getInt("automatic-events.tps-guard.retry-minutes", 5)); }
    public boolean isLocationCooldownEnabled() { return config.getBoolean("automatic-events.location-cooldown.enabled", true); }
    public int getLocationCooldownRadius() { return Math.max(0, config.getInt("automatic-events.location-cooldown.radius", 1000)); }
    public int getLocationCooldownHours() { return Math.max(1, config.getInt("automatic-events.location-cooldown.hours", 24)); }
    public int getWaveCount(@NotNull MeteorType type) { return Math.max(1, config.getInt("meteor-types."
            + type.name().toLowerCase(Locale.ROOT) + ".waves.count", config.getInt("event.waves.count", 3))); }
    public int getWaveIntervalSeconds(@NotNull MeteorType type) { return Math.max(1, config.getInt("meteor-types."
            + type.name().toLowerCase(Locale.ROOT) + ".waves.interval-seconds",
            config.getInt("event.waves.interval-seconds", 15))); }

    public @NotNull com.olmeteors.meteorevents.event.RadiusShape getRadiusShape(
            @NotNull MeteorType type) {
        return com.olmeteors.meteorevents.event.RadiusShape.parse(config.getString("meteor-types."
                + type.name().toLowerCase(Locale.ROOT) + ".radius-shape", "CIRCLE"));
    }

    public boolean isImpactCoreEnabled() {
        return config.getBoolean("event.fall.show-impact-core", false);
    }

    public @NotNull Material getLootBlockMaterial(@NotNull MeteorType type) {
        final String base = "meteor-types." + type.name().toLowerCase(Locale.ROOT) + ".loot.";
        final Material material = Material.matchMaterial(config.getString(base + "block",
                config.getString("event.vault.loot-block", "ANCIENT_DEBRIS")));
        return material == null || material.isAir() ? Material.ANCIENT_DEBRIS : material;
    }

    public @NotNull String getLootAccessMode(@NotNull MeteorType type) {
        return config.getString("meteor-types." + type.name().toLowerCase(Locale.ROOT)
                + ".loot.access-mode", "AUTO").toUpperCase(Locale.ROOT);
    }

    public boolean isPersonalLoot(@NotNull MeteorType type) {
        return config.getBoolean("meteor-types." + type.name().toLowerCase(Locale.ROOT)
                + ".loot.personal", true);
    }

    public boolean isStructureRestoreEnabled(@NotNull MeteorType type) {
        return config.getBoolean("meteor-types." + type.name().toLowerCase(Locale.ROOT)
                + ".restore-structure-on-finish", true);
    }

    public boolean isDamageActionBarEnabled(@NotNull MeteorType type) {
        return typeBoolean(type, "combat-feedback.damage-actionbar",
                config.getBoolean("event.combat-feedback.damage-actionbar", true));
    }

    public boolean isKillActionBarEnabled(@NotNull MeteorType type) {
        return typeBoolean(type, "combat-feedback.kill-actionbar",
                config.getBoolean("event.combat-feedback.kill-actionbar", true));
    }

    public boolean isLeaderboardBroadcastEnabled(@NotNull MeteorType type) {
        return typeBoolean(type, "combat-feedback.broadcast-leaderboard",
                config.getBoolean("event.combat-feedback.broadcast-leaderboard", true));
    }

    public int getLeaderboardSize(@NotNull MeteorType type) {
        final String path = "meteor-types." + type.name().toLowerCase(Locale.ROOT)
                + ".combat-feedback.leaderboard-size";
        return Math.max(1, config.getInt(path,
                config.getInt("event.combat-feedback.leaderboard-size", 5)));
    }

    public @NotNull String getCombatFeedbackText(@NotNull MeteorType type,
                                                  @NotNull String key,
                                                  @NotNull String fallback) {
        final String typePath = "meteor-types." + type.name().toLowerCase(Locale.ROOT)
                + ".combat-feedback." + key;
        return config.getString(typePath,
                config.getString("event.combat-feedback." + key, fallback));
    }

    public @NotNull String getLootInventoryTitle(@NotNull MeteorType type) {
        return config.getString("meteor-types." + type.name().toLowerCase(Locale.ROOT)
                + ".loot.inventory-title", "Meteor Ödülü");
    }

    private boolean typeBoolean(@NotNull MeteorType type, @NotNull String suffix,
                                boolean fallback) {
        return config.getBoolean("meteor-types." + type.name().toLowerCase(Locale.ROOT)
                + "." + suffix, fallback);
    }

    public int getRewardTopCount() {
        return Math.max(1, config.getInt("event.vault.reward-top-count", 3));
    }

    public @NotNull MeteorFallMode getFallMode(@NotNull MeteorType type) {
        return MeteorFallMode.parse(config.getString("meteor-types."
                + type.name().toLowerCase(Locale.ROOT) + ".fall-mode", "normal"));
    }

    public int getFallDurationSeconds(@NotNull MeteorType type, @NotNull MeteorFallMode mode) {
        final String base = "meteor-types." + type.name().toLowerCase(Locale.ROOT) + ".";
        return switch (mode) {
            case INSTANT -> 0;
            case NORMAL -> Math.max(2, config.getInt(base + "normal-fall-duration-seconds", 8));
            case SLOW -> Math.max(5, config.getInt(base + "slow-fall-duration-seconds", 18));
        };
    }

    public int getFallHeight(@NotNull MeteorFallMode mode) {
        return config.getInt("event.fall." + mode.name().toLowerCase(Locale.ROOT)
                + "-height", mode == MeteorFallMode.SLOW ? 120 : 80);
    }

    public boolean isAutomaticEventsEnabled() {
        return config.getBoolean("automatic-events.enabled", true);
    }

    public boolean setAutomaticEventsEnabled(boolean enabled) {
        config.set("automatic-events.enabled", enabled);
        return saveConfigFile("automatic event status");
    }

    public int getAutomaticMinMinutes() {
        return Math.max(1, config.getInt("automatic-events.min-interval-minutes", 30));
    }

    public int getAutomaticMaxMinutes() {
        return Math.max(getAutomaticMinMinutes(),
                config.getInt("automatic-events.max-interval-minutes", 60));
    }

    public int getAutomaticMaxActiveEvents() {
        return Math.max(1, config.getInt("automatic-events.max-active-events", 1));
    }

    public @NotNull List<String> getAutomaticWorlds() {
        return config.getStringList("automatic-events.worlds");
    }

    public boolean isAutomaticWorldEnabled(@NotNull String worldName) {
        return config.getBoolean("automatic-events.world-settings." + worldName + ".enabled", true);
    }

    public int getAutomaticWorldWeight(@NotNull String worldName) {
        return Math.max(1, config.getInt(
                "automatic-events.world-settings." + worldName + ".weight", 1));
    }

    public @NotNull String getAutomaticPresetName(@NotNull String worldName) {
        return config.getString("automatic-events.world-settings." + worldName + ".preset",
                config.getString("automatic-events.default-location-preset", "flat_surface"))
                .toLowerCase(Locale.ROOT).replace('-', '_');
    }

    public @NotNull LocationPreset getAutomaticLocationPreset(@NotNull String worldName) {
        final LocationPreset preset = getLocationPreset(getAutomaticPresetName(worldName));
        final String base = "automatic-events.world-settings." + worldName + ".";
        return new LocationPreset(preset.name(), preset.mode(), preset.requireFlat(),
                preset.allowWater(), preset.allowLava(),
                config.getInt(base + "min-y", preset.minY()),
                config.getInt(base + "max-y", preset.maxY()), preset.allowedFloorBlocks());
    }

    public boolean setAutomaticDefaultPreset(@NotNull String preset) {
        final String normalized = preset.toLowerCase(Locale.ROOT).replace('-', '_');
        if (!config.isConfigurationSection("location-finder.presets." + normalized)) return false;
        config.set("automatic-events.default-location-preset", normalized);
        return saveConfigFile("automatic location preset");
    }

    public boolean setAutomaticWorldRule(@NotNull String worldName, @NotNull String preset,
                                         int weight) {
        final String normalized = preset.toLowerCase(Locale.ROOT).replace('-', '_');
        if (!config.isConfigurationSection("location-finder.presets." + normalized)
                || weight < 1) return false;
        final String base = "automatic-events.world-settings." + worldName + ".";
        config.set(base + "enabled", true);
        config.set(base + "weight", weight);
        config.set(base + "preset", normalized);
        final List<String> worlds = new ArrayList<>(getAutomaticWorlds());
        if (worlds.stream().noneMatch(worldName::equalsIgnoreCase)) {
            worlds.add(worldName);
            config.set("automatic-events.worlds", worlds);
        }
        return saveConfigFile("automatic world rule");
    }

    /** Atomically configures the complete automatic-event rule used by the one-command setup. */
    public boolean setAutomaticRule(@NotNull List<MeteorType> types, @NotNull String worldName,
                                    @NotNull String preset, @NotNull List<Material> lootBlocks,
                                    int minMinutes, int maxMinutes,
                                    int minDistance, int maxDistance,
                                    @Nullable Integer minY, @Nullable Integer maxY) {
        return setAutomaticRule(types, worldName, preset, lootBlocks, minMinutes, maxMinutes,
                minDistance, maxDistance, minY, maxY, getAutomaticSearchShape(worldName));
    }

    public boolean setAutomaticRule(@NotNull List<MeteorType> types, @NotNull String worldName,
                                    @NotNull String preset, @NotNull List<Material> lootBlocks,
                                    int minMinutes, int maxMinutes,
                                    int minDistance, int maxDistance,
                                    @Nullable Integer minY, @Nullable Integer maxY,
                                    @NotNull RadiusShape searchShape) {
        final String normalized = preset.toLowerCase(Locale.ROOT).replace('-', '_');
        if (!config.isConfigurationSection("location-finder.presets." + normalized)
                || minMinutes < 1 || maxMinutes < minMinutes
                || minDistance < 0 || maxDistance <= minDistance
                || lootBlocks.isEmpty() || lootBlocks.stream().anyMatch(material ->
                material == null || material.isAir() || !material.isBlock() || !material.isItem())
                || (minY == null) != (maxY == null)
                || (minY != null && minY >= maxY)) return false;
        final Collection<MeteorType> lootTypes = types.isEmpty()
                ? List.of(MeteorType.values()) : types;
        if (lootBlocks.size() != 1 && lootBlocks.size() != lootTypes.size()) return false;
        config.set("automatic-events.enabled", true);
        config.set("automatic-events.min-interval-minutes", minMinutes);
        config.set("automatic-events.max-interval-minutes", maxMinutes);
        config.set("automatic-events.min-distance-from-spawn", minDistance);
        config.set("automatic-events.max-distance-from-spawn", maxDistance);
        config.set("automatic-events.worlds", List.of(worldName));
        config.set("automatic-events.meteor-types", types.stream()
                .map(type -> type.name().toLowerCase(Locale.ROOT)).distinct().toList());
        int lootIndex = 0;
        for (final MeteorType type : lootTypes) {
            final Material lootBlock = lootBlocks.size() == 1 ? lootBlocks.getFirst()
                    : lootBlocks.get(lootIndex++);
            config.set("meteor-types." + type.name().toLowerCase(Locale.ROOT)
                    + ".loot.block", lootBlock.name());
        }
        final String base = "automatic-events.world-settings." + worldName + ".";
        config.set(base + "enabled", true);
        config.set(base + "weight", 1);
        config.set(base + "preset", normalized);
        config.set(base + "search-shape", searchShape.name());
        config.set(base + "min-y", minY);
        config.set(base + "max-y", maxY);
        return saveConfigFile("complete automatic event rule");
    }

    public boolean setPresetFloorBlocks(@NotNull String preset, @NotNull List<String> blocks) {
        final String normalized = preset.toLowerCase(Locale.ROOT).replace('-', '_');
        final String base = "location-finder.presets." + normalized;
        if (!config.isConfigurationSection(base) || blocks.isEmpty()) return false;
        final List<String> materials = blocks.stream()
                .map(value -> value.toUpperCase(Locale.ROOT))
                .map(Material::matchMaterial)
                .filter(Objects::nonNull)
                .filter(Material::isBlock)
                .map(Material::name)
                .distinct().toList();
        if (materials.size() != blocks.size()) return false;
        config.set(base + ".allowed-floor-blocks", materials);
        return saveConfigFile("location preset floor blocks");
    }

    public @NotNull List<String> getAutomaticTypes() {
        return config.getStringList("automatic-events.meteor-types");
    }

    public int getAutomaticMinDistance() {
        return Math.max(0, config.getInt("automatic-events.min-distance-from-spawn",
                getMinEventDistance()));
    }

    public int getAutomaticMaxDistance() {
        return Math.max(getAutomaticMinDistance() + 1,
                config.getInt("automatic-events.max-distance-from-spawn", getMaxEventDistance()));
    }

    public @NotNull RadiusShape getAutomaticSearchShape(@NotNull String worldName) {
        return RadiusShape.parse(config.getString(
                "automatic-events.world-settings." + worldName + ".search-shape",
                config.getString("automatic-events.search-shape", "CIRCLE")));
    }

    public @NotNull List<String> getRankingRewardItems(@NotNull MeteorType type, int rank) {
        final String base = "meteor-types." + type.name().toLowerCase(Locale.ROOT) + ".";
        final List<String> result = new ArrayList<>(config.getStringList(base + "rewards-items"));
        result.addAll(config.getStringList(base + "ranking-rewards." + rank + ".items"));
        return result;
    }

    public @NotNull List<String> getRankingRewardCommands(@NotNull MeteorType type, int rank) {
        final String base = "meteor-types." + type.name().toLowerCase(Locale.ROOT) + ".";
        final List<String> result = new ArrayList<>(config.getStringList(base + "rewards-commands"));
        result.addAll(config.getStringList(base + "ranking-rewards." + rank + ".commands"));
        return result;
    }

    public @NotNull String getSchematicForType(@NotNull MeteorType type) {
        final String path = "meteor-types." + type.name().toLowerCase() + ".schematic";
        final String schematic = config.getString(path);
        if (schematic != null && !schematic.isEmpty()) {
            return schematic;
        }
        return config.getString("schematic.default", "meteor_crater.schem");
    }

    // ---------------------------------------------------------------
    //  Schematic Validation
    // ---------------------------------------------------------------

    /**
     * Validates all schematic references in meteor type configurations.
     * Logs warnings for missing schematics.
     */
    private void validateSchematicReferences(@NotNull FileConfiguration config) {
        for (final MeteorType type : MeteorType.values()) {
            final String customSchematic = config.getString(
                    "meteor-types." + type.name().toLowerCase() + ".schematic", "");
            if (!customSchematic.isEmpty() && !schematicExists(customSchematic)) {
                plugin.getLogger().warning("Schematic '" + customSchematic
                        + "' for " + type.name() + " not found in schematics folder.");
            }
        }
        final String defaultSchematic = config.getString("schematic.default", "meteor_crater.schem");
        if (!schematicExists(defaultSchematic)) {
            plugin.getLogger().warning("Default schematic '" + defaultSchematic
                    + "' not found in schematics folder.");
        }
    }

    // ---- File helpers ----

    public boolean schematicExists(@NotNull String schematicName) {
        if (schematicName.isEmpty()) return false;
        return new File(schematicsFolder, schematicName).exists();
    }

    public @Nullable File getSchematicFile(@NotNull String schematicName) {
        final File file = new File(schematicsFolder, schematicName);
        return file.exists() && file.isFile() ? file : null;
    }

    public Path getInventoriesPath() {
        return inventoriesFolder.toPath();
    }

    public Path getSchematicsPath() {
        return schematicsFolder.toPath();
    }

    public boolean setSchematicForType(@NotNull MeteorType type, @NotNull String schematicName) {
        final String path = "meteor-types." + type.name().toLowerCase(Locale.ROOT) + ".schematic";
        config.set(path, schematicName);
        try {
            config.save(new File(dataFolder, "config.yml"));
            typeConfigCache.remove(type.name());
            return true;
        } catch (IOException error) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to save schematic setting for " + type.name(), error);
            return false;
        }
    }

    public boolean setMythicMobsForType(@NotNull MeteorType type, @NotNull List<String> mobIds) {
        final String path = "meteor-types." + type.name().toLowerCase(Locale.ROOT) + ".mythicmobs";
        config.set(path, new ArrayList<>(new LinkedHashSet<>(mobIds)));
        try {
            config.save(new File(dataFolder, "config.yml"));
            typeConfigCache.remove(type.name());
            return true;
        } catch (IOException error) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save MythicMobs for " + type.name(), error);
            return false;
        }
    }

    public boolean setSetupOffsets(@NotNull MeteorType type,
                                   @NotNull List<org.bukkit.util.Vector> mobOffsets,
                                   @NotNull List<org.bukkit.util.Vector> hologramOffsets,
                                   @NotNull List<org.bukkit.util.Vector> chestOffsets) {
        final String base = "meteor-types." + type.name().toLowerCase(Locale.ROOT) + ".";
        config.set(base + "mob-spawn-offsets", serializeVectors(mobOffsets));
        config.set(base + "hologram-offsets", serializeVectors(hologramOffsets));
        config.set(base + "chest-offsets", serializeVectors(chestOffsets));
        try {
            config.save(new File(dataFolder, "config.yml"));
            return true;
        } catch (IOException error) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save setup points for " + type.name(), error);
            return false;
        }
    }

    public boolean saveSchematicSetup(@NotNull String schematicName,
                                      @NotNull List<org.bukkit.util.Vector> mobOffsets,
                                      @NotNull List<org.bukkit.util.Vector> hologramOffsets,
                                      @NotNull List<org.bukkit.util.Vector> chestOffsets,
                                      @NotNull org.bukkit.util.Vector rootOffset) {
        final String base = "schematics." + schematicSetupKey(schematicName) + ".";
        setupData.set(base + "file", schematicName);
        setupData.set(base + "mob-spawn-offsets", serializeVectors(mobOffsets));
        setupData.set(base + "hologram-offsets", serializeVectors(hologramOffsets));
        setupData.set(base + "chest-offsets", serializeVectors(chestOffsets));
        setupData.set(base + "root-offset", serializeVectors(List.of(rootOffset)));
        try {
            setupData.save(new File(dataFolder, "setups.yml"));
            return true;
        } catch (IOException error) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to save schematic setup points for " + schematicName, error);
            return false;
        }
    }

    public @NotNull List<org.bukkit.util.Vector> getSchematicMobOffsets(@NotNull String name) {
        return readVectors(setupData,
                "schematics." + schematicSetupKey(name) + ".mob-spawn-offsets");
    }

    public @NotNull List<org.bukkit.util.Vector> getSchematicHologramOffsets(@NotNull String name) {
        return readVectors(setupData,
                "schematics." + schematicSetupKey(name) + ".hologram-offsets");
    }

    public @NotNull List<org.bukkit.util.Vector> getSchematicChestOffsets(@NotNull String name) {
        return readVectors(setupData,
                "schematics." + schematicSetupKey(name) + ".chest-offsets");
    }

    public @NotNull org.bukkit.util.Vector getSchematicRootOffset(@NotNull String name) {
        final List<org.bukkit.util.Vector> values = readVectors(setupData,
                "schematics." + schematicSetupKey(name) + ".root-offset");
        return values.isEmpty() ? new org.bukkit.util.Vector() : values.getFirst();
    }

    public boolean applySchematicSetupToType(@NotNull String name, @NotNull MeteorType type) {
        final List<org.bukkit.util.Vector> mobs = getSchematicMobOffsets(name);
        final List<org.bukkit.util.Vector> holograms = getSchematicHologramOffsets(name);
        final List<org.bukkit.util.Vector> chests = getSchematicChestOffsets(name);
        if (mobs.isEmpty() && holograms.isEmpty() && chests.isEmpty()) return false;
        return setSetupOffsets(type, mobs, holograms, chests);
    }

    private @NotNull String schematicSetupKey(@NotNull String name) {
        return name.toLowerCase(Locale.ROOT)
                .replace(".schematic", "").replace(".schem", "")
                .replaceAll("[^a-z0-9_-]", "_");
    }

    public @NotNull List<org.bukkit.util.Vector> getMobSpawnOffsets(@NotNull MeteorType type) {
        return readVectors("meteor-types." + type.name().toLowerCase(Locale.ROOT) + ".mob-spawn-offsets");
    }

    public @NotNull List<org.bukkit.util.Vector> getHologramOffsets(@NotNull MeteorType type) {
        return readVectors("meteor-types." + type.name().toLowerCase(Locale.ROOT) + ".hologram-offsets");
    }

    public @NotNull List<org.bukkit.util.Vector> getChestOffsets(@NotNull MeteorType type) {
        return readVectors("meteor-types." + type.name().toLowerCase(Locale.ROOT) + ".chest-offsets");
    }

    public @NotNull String getHologramText(@NotNull MeteorType type) {
        return config.getString("meteor-types." + type.name().toLowerCase(Locale.ROOT)
                + ".hologram-text", getMeteorTypeName(type));
    }

    public boolean setHologramText(@NotNull MeteorType type, @NotNull String text) {
        config.set("meteor-types." + type.name().toLowerCase(Locale.ROOT) + ".hologram-text", text);
        return saveConfigFile("hologram text for " + type.name());
    }

    public @NotNull Map<String, Double> getMythicMobChances(@NotNull MeteorType type) {
        final String path = "meteor-types." + type.name().toLowerCase(Locale.ROOT) + ".mythicmob-chances";
        final ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) return new LinkedHashMap<>();
        final Map<String, Double> result = new LinkedHashMap<>();
        for (final String key : section.getKeys(false)) {
            final double chance = section.getDouble(key, 0.0);
            if (chance > 0) result.put(key, chance);
        }
        return result;
    }

    public boolean setMythicMobChances(@NotNull MeteorType type, @NotNull Map<String, Double> chances) {
        final String path = "meteor-types." + type.name().toLowerCase(Locale.ROOT) + ".mythicmob-chances";
        config.set(path, null);
        chances.forEach((id, chance) -> config.set(path + "." + id, chance));
        config.set("meteor-types." + type.name().toLowerCase(Locale.ROOT) + ".mythicmobs",
                chances.entrySet().stream().filter(e -> e.getValue() > 0).map(Map.Entry::getKey).toList());
        typeConfigCache.remove(type.name());
        return saveConfigFile("MythicMob chances for " + type.name());
    }

    private boolean saveConfigFile(String description) {
        try {
            config.save(new File(dataFolder, "config.yml"));
            return true;
        } catch (IOException error) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save " + description, error);
            return false;
        }
    }

    private List<String> serializeVectors(List<org.bukkit.util.Vector> vectors) {
        return vectors.stream().map(v -> v.getX() + "," + v.getY() + "," + v.getZ()).toList();
    }

    private List<org.bukkit.util.Vector> readVectors(String path) {
        return readVectors(config, path);
    }

    private List<org.bukkit.util.Vector> readVectors(FileConfiguration source, String path) {
        final List<org.bukkit.util.Vector> result = new ArrayList<>();
        if (source == null) return result;
        for (final String raw : source.getStringList(path)) {
            final String[] parts = raw.split(",");
            if (parts.length != 3) continue;
            try {
                result.add(new org.bukkit.util.Vector(Double.parseDouble(parts[0]),
                        Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
            } catch (NumberFormatException ignored) {
                plugin.getLogger().warning("Invalid setup offset: " + raw);
            }
        }
        return result;
    }

    public @NotNull FileConfiguration getConfig() {
        if (config == null) loadConfiguration();
        return config;
    }

    // ---- Type-specific config ----

    public @NotNull MeteorTypeConfig getTypeConfig(@NotNull MeteorType type) {
        return typeConfigCache.computeIfAbsent(type.name(), k -> {
            final String p = "meteor-types." + type.name().toLowerCase() + ".";
            return new MeteorTypeConfig(
                    config.getInt(p + "pre-impact-duration-seconds", 30),
                    config.getInt(p + "event-duration-seconds", 300),
                    config.getInt(p + "rollback-duration-seconds", 30),
                    config.getInt(p + "impact-radius", type.impactRadius()),
                    config.getDouble(p + "boss-health-multiplier", 1.0),
                    config.getStringList(p + "rewards-commands"),
                    config.getString(p + "boss-mythicmob", ""),
                    config.getStringList(p + "mythicmobs")
            );
        });
    }

    public record MeteorTypeConfig(
            int preImpactDurationSeconds,
            int eventDurationSeconds,
            int rollbackDurationSeconds,
            int impactRadius,
            double bossHealthMultiplier,
            List<String> rewardsCommands,
            String bossMythicMob,
            List<String> mythicMobs
    ) {
        public long preImpactDurationTicks() { return preImpactDurationSeconds * 20L; }
        public long eventDurationTicks() { return eventDurationSeconds * 20L; }
        public long rollbackDurationTicks() { return rollbackDurationSeconds * 20L; }
    }

    public record LocationPreset(@NotNull String name, @NotNull String mode,
                                 boolean requireFlat, boolean allowWater, boolean allowLava,
                                 int minY, int maxY, @NotNull List<String> allowedFloorBlocks) {}
}
