package com.olmeteors.meteorevents.hook;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.olmeteors.meteorevents.event.ActiveMeteorEvent;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Hook for PlaceholderAPI integration.
 * Provides placeholders for scoreboard, chat, and action bar usage:
 * - %olmeteor_active% - Number of active events
 * - %olmeteor_eventid% - ID of nearest event to player
 */
public final class PlaceholderAPIHook {

    private final MeteorPlugin plugin;
    private boolean available;
    private MeteorExpansion expansion;

    public PlaceholderAPIHook(MeteorPlugin plugin) {
        this.plugin = plugin;
        checkAvailability();
    }

    private void checkAvailability() {
        final var placeholderApi = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderApi == null || !placeholderApi.isEnabled()) {
            this.available = false;
            plugin.getLogger().info("PlaceholderAPI not installed or enabled - placeholders disabled");
            return;
        }

        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            this.available = true;
            registerExpansion();
            plugin.getLogger().info("PlaceholderAPI hook initialized successfully");
        } catch (ClassNotFoundException | LinkageError | RuntimeException error) {
            this.available = false;
            plugin.getLogger().log(Level.WARNING,
                    "PlaceholderAPI is missing or incompatible - placeholders disabled", error);
        }
    }

    public boolean isAvailable() {
        return available;
    }

    private void registerExpansion() {
        this.expansion = new MeteorExpansion(plugin);
        if (expansion.register()) {
            plugin.getLogger().info("PlaceholderAPI expansion registered: olmeteor");
        } else {
            plugin.getLogger().warning("Failed to register PlaceholderAPI expansion");
        }
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
