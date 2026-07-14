package com.olmeteors.meteorevents.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for formatting and sending messages to players.
 * Supports both legacy color codes (&a, &c, etc.) and MiniMessage format.
 * <p>
 * Prefixes for convenience methods ({@link #sendWarning}, {@link #sendError},
 * {@link #sendSuccess}, {@link #sendInfo}, {@link #broadcast}) can be
 * customised via {@link #loadPrefixes} which is called by
 * {@link com.olmeteors.meteorevents.config.ConfigManager} on reload.
 */
public final class MessageUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("&([0-9a-fklmnor])");

    // Configurable prefixes (loaded from messages.message_prefix.* via ConfigManager)
    private static String chatPrefix = "&8[&6Ol&eMeteor&8] &r";
    private static String broadcastPrefix = "&8[&6Ol&eMeteor&8] &r";
    private static String warningPrefix = "&e&l⚠ &6Warning: &e";
    private static String errorPrefix = "&4&l✘ &cError: &4";
    private static String successPrefix = "&a&l✔ &2";
    private static String infoPrefix = "&8[&bℹ&8] &7";

    private MessageUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Loads custom prefixes from configuration.
     * Called by {@link ConfigManager#loadConfiguration()}.
     *
     * @param prefixes map of prefix names to their formatted strings
     */
    public static void loadPrefixes(@NotNull Map<@NotNull String, @NotNull String> prefixes) {
        if (prefixes.containsKey("chat"))       chatPrefix       = prefixes.get("chat");
        if (prefixes.containsKey("broadcast"))  broadcastPrefix  = prefixes.get("broadcast");
        if (prefixes.containsKey("warning"))    warningPrefix    = prefixes.get("warning");
        if (prefixes.containsKey("error"))      errorPrefix      = prefixes.get("error");
        if (prefixes.containsKey("success"))    successPrefix    = prefixes.get("success");
        if (prefixes.containsKey("info"))       infoPrefix       = prefixes.get("info");
    }

    /**
     * Converts a legacy color code string (with &) to a MiniMessage string.
     *
     * @param legacy the legacy string
     * @return the MiniMessage string
     */
    public static @NotNull String legacyToMiniMessage(@NotNull String legacy) {
        String result = legacy;

        // Convert hex colors &#RRGGBB to MiniMessage <color:#RRGGBB>
        final Matcher hexMatcher = HEX_PATTERN.matcher(result);
        result = hexMatcher.replaceAll("<color:#$1>");

        // Convert legacy color codes
        result = result
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&k", "<obf>")
                .replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");

        return result;
    }

    /**
     * Parses a string with legacy color codes (&) into a Component.
     *
     * @param message the message with & color codes
     * @return the formatted Component
     */
    public static @NotNull Component parse(@NotNull String message) {
        // Try to detect if this is already MiniMessage format
        if (message.contains("<") && (message.contains("color") || message.contains("bold") || message.contains("gradient"))) {
            return MINI_MESSAGE.deserialize(message);
        }
        // Otherwise treat as legacy
        return LEGACY_SERIALIZER.deserialize(message);
    }

    /**
     * Sends a formatted message to a CommandSender.
     *
     * @param sender  the recipient
     * @param message the message with & color codes
     */
    public static void sendMessage(@NotNull CommandSender sender, @NotNull String message) {
        sender.sendMessage(parse(chatPrefix + message));
    }

    /**
     * Sends a formatted message to a Player as an action bar message.
     *
     * @param player  the recipient
     * @param message the message with & color codes
     */
    public static void sendActionBar(@NotNull Player player, @NotNull String message) {
        player.sendActionBar(parse(message));
    }

    /**
     * Sends a title and subtitle to a player.
     *
     * @param player   the recipient
     * @param title    the title text
     * @param subtitle the subtitle text
     * @param fadeIn   fade-in time in ticks
     * @param stay     stay time in ticks
     * @param fadeOut  fade-out time in ticks
     */
    public static void sendTitle(@NotNull Player player, @NotNull String title,
                                 @NotNull String subtitle, int fadeIn, int stay, int fadeOut) {
        player.showTitle(Title.title(
                parse(title),
                parse(subtitle),
                Title.Times.times(
                        Duration.ofMillis(fadeIn * 50L),
                        Duration.ofMillis(stay * 50L),
                        Duration.ofMillis(fadeOut * 50L)
                )
        ));
    }

    /**
     * Broadcasts a formatted message to all online players with a prefix.
     * The prefix is customisable via {@code messages.message_prefix.broadcast} in config.
     *
     * @param message the message with & color codes
     */
    public static void broadcast(@NotNull String message) {
        final Component component = parse(broadcastPrefix + message);
        org.bukkit.Bukkit.broadcast(component);
    }

    /**
     * Sends a formatted warning message.
     * The prefix is customisable via {@code messages.message_prefix.warning} in config.
     *
     * @param sender  the recipient
     * @param message the warning message
     */
    public static void sendWarning(@NotNull CommandSender sender, @NotNull String message) {
        sendMessage(sender, warningPrefix + message);
    }

    /**
     * Sends a formatted error message.
     * The prefix is customisable via {@code messages.message_prefix.error} in config.
     *
     * @param sender  the recipient
     * @param message the error message
     */
    public static void sendError(@NotNull CommandSender sender, @NotNull String message) {
        sendMessage(sender, errorPrefix + message);
    }

    /**
     * Sends a formatted success message.
     * The prefix is customisable via {@code messages.message_prefix.success} in config.
     *
     * @param sender  the recipient
     * @param message the success message
     */
    public static void sendSuccess(@NotNull CommandSender sender, @NotNull String message) {
        sendMessage(sender, successPrefix + message);
    }

    /**
     * Sends a formatted info message.
     * The prefix is customisable via {@code messages.message_prefix.info} in config.
     *
     * @param sender  the recipient
     * @param message the info message
     */
    public static void sendInfo(@NotNull CommandSender sender, @NotNull String message) {
        sendMessage(sender, infoPrefix + message);
    }

    /**
     * Formats a simple progress bar.
     *
     * @param current current value
     * @param max     maximum value
     * @param length  bar length in characters
     * @return the progress bar string
     */
    public static @NotNull String progressBar(double current, double max, int length) {
        final double progress = Math.min(1.0, Math.max(0.0, current / max));
        final int filledBars = (int) (progress * length);
        final StringBuilder bar = new StringBuilder();

        bar.append("&a");
        for (int i = 0; i < filledBars; i++) {
            bar.append("▌");
        }
        bar.append("&7");
        for (int i = filledBars; i < length; i++) {
            bar.append("▌");
        }

        return bar.toString();
    }
}
