package com.olmeteors.meteorevents.hook;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.olmeteors.meteorevents.event.ActiveMeteorEvent;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** Classloader-safe facade for the optional PlaceholderAPI expansion. */
public final class PlaceholderAPIHook {
    private final Access access;

    private PlaceholderAPIHook(Access access) {
        this.access = Objects.requireNonNull(access, "access");
    }

    public static @NotNull PlaceholderAPIHook create(@NotNull MeteorPlugin plugin,
                                                       @Nullable Plugin placeholderPlugin) {
        if (placeholderPlugin == null || !placeholderPlugin.isEnabled()) {
            plugin.getLogger().info("PlaceholderAPI not installed or enabled - placeholders disabled");
            return new PlaceholderAPIHook(DisabledAccess.INSTANCE);
        }
        try {
            final ClassLoader loader = placeholderPlugin.getClass().getClassLoader();
            Class.forName("me.clip.placeholderapi.PlaceholderAPI", false, loader);
            Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion", false, loader);
            return new PlaceholderAPIHook(new PlaceholderAPIAccess(plugin));
        } catch (ClassNotFoundException | LinkageError | RuntimeException error) {
            plugin.getLogger().log(Level.WARNING,
                    "PlaceholderAPI is missing or incompatible - placeholders disabled safely", error);
            return new PlaceholderAPIHook(DisabledAccess.INSTANCE);
        }
    }

    public boolean isAvailable() { return access.isAvailable(); }
    public void shutdown() { access.shutdown(); }

    interface Access {
        boolean isAvailable();
        void shutdown();
    }

    private enum DisabledAccess implements Access {
        INSTANCE;
        @Override public boolean isAvailable() { return false; }
        @Override public void shutdown() { }
    }
}

/**
 * Hook for PlaceholderAPI integration.
 * Provides placeholders for scoreboard, chat, and action bar usage:
 * - %olmeteor_active% - Number of active events
 * - %olmeteor_eventid% - ID of nearest event to player
 */
final class PlaceholderAPIAccess implements PlaceholderAPIHook.Access {

    private final MeteorPlugin plugin;
    private boolean available;
    private MeteorExpansion expansion;

    PlaceholderAPIAccess(MeteorPlugin plugin) {
        this.plugin = plugin;
        this.available = registerExpansion();
        if (this.available) {
            plugin.getLogger().info("PlaceholderAPI hook initialized successfully");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    private boolean registerExpansion() {
        this.expansion = new MeteorExpansion(plugin);
        if (expansion.register()) {
            plugin.getLogger().info("PlaceholderAPI expansion registered: olmeteor");
            return true;
        } else {
            plugin.getLogger().warning("Failed to register PlaceholderAPI expansion");
            return false;
        }
    }

    @Override
    public void shutdown() {
        if (expansion != null) {
            try {
                expansion.unregister();
            } catch (RuntimeException | LinkageError error) {
                plugin.getLogger().log(Level.FINE,
                        "Could not unregister PlaceholderAPI expansion", error);
            }
        }
        available = false;
    }

    /**
     * Placeholder expansion class for meteor events.
     */
    private static final class MeteorExpansion extends PlaceholderExpansion {

        private final MeteorPlugin plugin;
        private final Map<String, String> cache;

        MeteorExpansion(MeteorPlugin plugin) {
            this.plugin = plugin;
            this.cache = new ConcurrentHashMap<>();
        }

        @Override
        public @NotNull String getIdentifier() {
            return "olmeteor";
        }

        @Override
        public @NotNull String getAuthor() {
            return "OlMeteor";
        }

        @Override
        public @NotNull String getVersion() {
            return plugin.getPluginMeta().getVersion();
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
            if (player == null) return "";

            try {
                final var eventManager = plugin.getMeteorEventManager();
                final var activeEvents = eventManager.getActiveEvents();

                return switch (params.toLowerCase()) {
                    case "active" -> String.valueOf(activeEvents.size());
                    case "eventid" -> getNearestEventId(player, activeEvents);
                    case "phase" -> getNearestEventPhase(player, activeEvents);
                    case "distance" -> getNearestEventDistance(player, activeEvents);
                    case "type" -> getNearestEventType(player, activeEvents);
                    case "active_type" -> getNearestEventType(player, activeEvents);
                    case "boss_alive" -> isBossAlive(player, activeEvents);
                    case "next_time" -> nextTime();
                    case "player_damage" -> String.format(java.util.Locale.ROOT, "%.1f",
                            plugin.getPlayerStatsStore().get(player.getUniqueId()).damage());
                    case "player_kills" -> String.valueOf(plugin.getPlayerStatsStore()
                            .get(player.getUniqueId()).kills());
                    case "player_loot" -> String.valueOf(plugin.getPlayerStatsStore()
                            .get(player.getUniqueId()).lootClaims());
                    case "player_rank" -> playerRank(player, activeEvents);
                    default -> null;
                };

            } catch (Exception e) {
                plugin.getLogger().log(Level.FINE, "Error processing placeholder: " + params, e);
                return "error";
            }
        }

        private String nextTime() {
            final long next = plugin.getMeteorEventManager().getNextAutomaticAtMillis();
            if (next <= System.currentTimeMillis()) return "none";
            final long seconds = (next - System.currentTimeMillis()) / 1000L;
            return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
        }

        private String playerRank(Player player, Map<String, ActiveMeteorEvent> events) {
            final ActiveMeteorEvent event = findNearestEvent(player, events);
            if (event == null || !event.playerDamageMap().containsKey(player.getUniqueId())) return "0";
            final double own = event.playerDamageMap().get(player.getUniqueId());
            return String.valueOf(1 + event.playerDamageMap().values().stream()
                    .filter(damage -> damage > own).count());
        }

        private @Nullable String getNearestEventId(Player player,
                                                     Map<String, ActiveMeteorEvent> events) {
            final var nearest = findNearestEvent(player, events);
            return nearest != null ? nearest.eventId() : "none";
        }

        private @Nullable String getNearestEventPhase(Player player,
                                                       Map<String, ActiveMeteorEvent> events) {
            final var nearest = findNearestEvent(player, events);
            return nearest != null ? nearest.phase().name() : "none";
        }

        private @Nullable String getNearestEventDistance(Player player,
                                                          Map<String, ActiveMeteorEvent> events) {
            final var nearest = findNearestEvent(player, events);
            if (nearest == null) return "—";
            final double distance = player.getLocation().distance(nearest.center());
            return String.format("%.0f", distance);
        }

        private @Nullable String getNearestEventType(Player player,
                                                      Map<String, ActiveMeteorEvent> events) {
            final var nearest = findNearestEvent(player, events);
            return nearest != null ? plugin.getConfigManager().getMeteorTypeName(nearest.meteorType()) : "none";
        }

        private @Nullable String isBossAlive(Player player,
                                              Map<String, ActiveMeteorEvent> events) {
            final var nearest = findNearestEvent(player, events);
            if (nearest == null) return "false";
            return nearest.bossDefeated() ? "false" : "true";
        }

        private @Nullable ActiveMeteorEvent findNearestEvent(Player player,
                                                              Map<String, ActiveMeteorEvent> events) {
            ActiveMeteorEvent nearest = null;
            double minDistance = Double.MAX_VALUE;

            for (final var event : events.values()) {
                if (!event.isActive()) continue;
                if (!event.world().equals(player.getWorld())) continue;

                final double distance = player.getLocation().distance(event.center());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = event;
                }
            }

            return nearest;
        }
    }
}
