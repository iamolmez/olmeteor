package com.olmeteors.meteorevents.config;

import com.olmeteors.meteorevents.MeteorPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages locale detection and locale-specific message loading.
 * <p>
 * Locale resolution order:
 * <ol>
 *   <li>Explicit {@code locale} setting in {@code config.yml} (e.g. {@code "tr"}, {@code "de"}, {@code "en"})</li>
 *   <li>Value {@code "auto"} → JVM system locale ({@link Locale#getDefault()})</li>
 *   <li>Fallback to {@code "en"} (English) if neither matches an available file</li>
 * </ol>
 * <p>
 * Locale files are loaded from the plugin's {@code lang/} data-folder directory.
 * A built-in set of locale files is shipped inside the plugin JAR under {@code lang/}.
 * On first run the shipped files are copied to the data folder automatically.
 */
public final class LocaleManager {

    private final MeteorPlugin plugin;
    private final Path langFolder;

    /** Resolved locale code, e.g. {@code "tr"}, {@code "en"}, {@code "de"}. */
    private String activeLocale;

    /** Messages loaded from the active locale file (may be partial). */
    private YamlConfiguration localeMessages;

    /** Global placeholder mappings. */
    private final Map<String, String> globalPlaceholders;

    /** Default English fallback loaded from the plugin JAR. */
    private YamlConfiguration fallbackMessages;

    // ────────────────────────────────────────────────────────────
    //  Known locale codes shipped with the plugin
    // ────────────────────────────────────────────────────────────

    private static final String[] SHIPPED_LOCALES = {"en", "tr"};
    private static final String FALLBACK_LOCALE = "en";

    public LocaleManager(MeteorPlugin plugin, Path dataFolder) {
        this.plugin = plugin;
        this.langFolder = dataFolder.resolve("lang");
        this.globalPlaceholders = new LinkedHashMap<>();
        this.activeLocale = FALLBACK_LOCALE;

        registerGlobalPlaceholders();
    }

    private void registerGlobalPlaceholders() {
        globalPlaceholders.put("%version%", plugin.getPluginMeta().getVersion());
        globalPlaceholders.put("%prefix%", "&8[&6Ol&eMeteor&8]");
    }

    // ────────────────────────────────────────────────────────────
    //  Initialisation
    // ────────────────────────────────────────────────────────────

    /**
     * Initialises the locale system:
     * <ul>
     *   <li>Creates the {@code lang/} data directory</li>
     *   <li>Ships built-in locale files from the JAR to the data folder</li>
     *   <li>Resolves the active locale and loads its message file</li>
     *   <li>Loads the English fallback file from the JAR</li>
     * </ul>
     *
     * @param configLocale the {@code locale} value from {@code config.yml}
     */
    public void init(@NotNull String configLocale) {
        try {
            Files.createDirectories(langFolder);

            // Ship built-in locale files to data folder
            for (final String locale : SHIPPED_LOCALES) {
                shipLocaleFile(locale);
            }

            // Resolve the active locale
            this.activeLocale = resolveLocale(configLocale);

            // Load locale-specific messages (from data folder, so admins can edit them)
            final File localeFile = langFolder.resolve("messages_" + activeLocale + ".yml").toFile();
            if (localeFile.exists()) {
                this.localeMessages = YamlConfiguration.loadConfiguration(localeFile);
            } else {
                // Fall back to JAR resource
                final InputStream in = plugin.getResource("lang/messages_" + activeLocale + ".yml");
                if (in != null) {
                    this.localeMessages = YamlConfiguration.loadConfiguration(
                            new InputStreamReader(in, StandardCharsets.UTF_8));
                }
            }

            // Keep administrator edits, but supply newly added translation keys
            // from the bundled file after plugin updates.
            if (this.localeMessages != null) {
                try (InputStream bundledIn = plugin.getResource(
                        "lang/messages_" + activeLocale + ".yml")) {
                    if (bundledIn != null) {
                        this.localeMessages.setDefaults(YamlConfiguration.loadConfiguration(
                                new InputStreamReader(bundledIn, StandardCharsets.UTF_8)));
                    }
                }
            }

            // Always load English fallback from JAR
            final InputStream enIn = plugin.getResource("lang/messages_en.yml");
            if (enIn != null) {
                this.fallbackMessages = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(enIn, StandardCharsets.UTF_8));
            }

            plugin.getLogger().info("Locale activated: " + activeLocale);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to initialise locale system", e);
            this.activeLocale = FALLBACK_LOCALE;
            this.localeMessages = null;
            this.fallbackMessages = null;
        }
    }

    /**
     * Copies a built-in locale resource from the JAR to the data folder
     * if it does not already exist there.
     */
    private void shipLocaleFile(@NotNull String locale) {
        final String resourcePath = "lang/messages_" + locale + ".yml";
        final File target = langFolder.resolve("messages_" + locale + ".yml").toFile();

        if (target.exists()) return; // Admin may have customised it

        try (final InputStream in = plugin.getResource(resourcePath)) {
            if (in != null) {
                Files.copy(in, target.toPath());
                plugin.getLogger().fine("Shipped locale file: " + resourcePath);
            } else {
                plugin.getLogger().warning("Built-in locale not found in JAR: " + resourcePath);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to ship locale file: " + resourcePath, e);
        }
    }

    // ────────────────────────────────────────────────────────────
    //  Locale resolution
    // ────────────────────────────────────────────────────────────

    /**
     * Resolves the effective locale code.
     */
    private @NotNull String resolveLocale(@NotNull String configLocale) {
        final String trimmed = configLocale.trim().toLowerCase(Locale.ROOT);

        // Explicit non-auto value
        if (!trimmed.equals("auto") && !trimmed.isEmpty()) {
            if (isLocaleAvailable(trimmed)) {
                return trimmed;
            }
            plugin.getLogger().warning("Configured locale '" + trimmed
                    + "' not available, falling back to auto-detection");
        }

        // Auto-detect from JVM
        final Locale jvmLocale = Locale.getDefault();
        final String lang = jvmLocale.getLanguage(); // e.g. "tr", "de", "en"

        if (isLocaleAvailable(lang)) {
            return lang;
        }

        // Try country-specific (e.g. "pt_BR")
        final String full = jvmLocale.toString();
        if (isLocaleAvailable(full)) {
            return full;
        }

        return FALLBACK_LOCALE;
    }

    private boolean isLocaleAvailable(@NotNull String code) {
        // Check both JAR resources and data folder
        if (Files.exists(langFolder.resolve("messages_" + code + ".yml"))) {
            return true;
        }
        return plugin.getResource("lang/messages_" + code + ".yml") != null;
    }

    // ────────────────────────────────────────────────────────────
    //  Message lookup
    // ────────────────────────────────────────────────────────────

    /**
     * Retrieves a message using the locale cascade:
     * <ol>
     *   <li>Active locale file ({@code lang/messages_XX.yml})</li>
     *   <li>English fallback ({@code lang/messages_en.yml} in JAR)</li>
     *   <li>Fallback string</li>
     * </ol>
     *
     * @param path       dot-separated path under {@code messages} (e.g. {@code "command.help.title"})
     * @param fallback   fallback string if not found in any locale file
     * @param placeholders key-value pairs for {@code %key%} replacement
     * @return the resolved message
     */
    public @NotNull String getMessage(@NotNull String path,
                                       @NotNull String fallback,
                                       @NotNull String @NotNull ... placeholders) {
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders must be provided as key-value pairs");
        }

        final String fullPath = "messages." + path;
        String message = null;

        // 1. Active locale
        if (localeMessages != null) {
            message = localeMessages.getString(fullPath);
        }

        // 2. English fallback
        if ((message == null || message.isEmpty()) && fallbackMessages != null) {
            message = fallbackMessages.getString(fullPath);
        }

        // 3. Provided fallback string
        if (message == null || message.isEmpty()) {
            return fallback; // may be null — caller handles that
        }

        // Apply placeholders
        return applyPlaceholders(message, placeholders);
    }

    /**
     * Same as {@link #getMessage(String, String, String...)}, but with
     * a built-in fallback that shows the missing translation path.
     */
    public @NotNull String getMessage(@NotNull String path,
                                       @NotNull String @NotNull ... placeholders) {
        return getMessage(path,
                "&c[Missing translation: messages." + path + " for locale " + activeLocale + "]",
                placeholders);
    }

    /**
     * Applies global and caller-supplied placeholders to a message.
     */
    private @NotNull String applyPlaceholders(@NotNull String message,
                                               @NotNull String @NotNull ... placeholders) {
        String result = message;

        // Global placeholders
        for (final var entry : globalPlaceholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        // Caller-supplied
        for (int i = 0; i < placeholders.length; i += 2) {
            result = result.replace("%" + placeholders[i] + "%",
                    placeholders[i + 1] != null ? placeholders[i + 1] : "");
        }

        return result;
    }

    // ────────────────────────────────────────────────────────────
    //  Accessors
    // ────────────────────────────────────────────────────────────

    /**
     * Returns the currently active locale code (e.g. {@code "tr"}, {@code "en"}).
     */
    public @NotNull String getActiveLocale() {
        return activeLocale;
    }

    /**
     * Returns an array of all locale codes shipped with the plugin.
     */
    public static @NotNull String[] getShippedLocales() {
        return SHIPPED_LOCALES.clone();
    }
}
