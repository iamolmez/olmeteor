package com.olmeteors.meteorevents.event;

import com.olmeteors.meteorevents.MeteorPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

/** Persistent, lightweight audit history for meteor impacts. */
public final class MeteorHistoryStore {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.of("Europe/Istanbul"));

    private final MeteorPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    public MeteorHistoryStore(@NotNull MeteorPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "meteor-history.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void recordImpact(@NotNull ActiveMeteorEvent event, boolean automatic) {
        final Location location = event.center();
        final String base = "events." + event.eventId() + ".";
        data.set(base + "time", System.currentTimeMillis());
        data.set(base + "type", event.meteorType().name());
        data.set(base + "world", event.world().getName());
        data.set(base + "x", location.getBlockX());
        data.set(base + "y", location.getBlockY());
        data.set(base + "z", location.getBlockZ());
        data.set(base + "automatic", automatic);
        data.set(base + "result", "ACTIVE");
        save();
    }

    public synchronized void markResult(@NotNull String eventId, @NotNull String result) {
        if (!data.contains("events." + eventId)) return;
        data.set("events." + eventId + ".result", result);
        data.set("events." + eventId + ".finished-time", System.currentTimeMillis());
        save();
    }

    public synchronized @NotNull List<HistoryEntry> recent(int requestedLimit) {
        final ConfigurationSection events = data.getConfigurationSection("events");
        if (events == null) return List.of();
        final List<HistoryEntry> result = new ArrayList<>();
        for (final String id : events.getKeys(false)) {
            final String base = "events." + id + ".";
            final long time = data.getLong(base + "time");
            result.add(new HistoryEntry(id, time, TIME_FORMAT.format(Instant.ofEpochMilli(time)),
                    data.getString(base + "type", "?"),
                    data.getString(base + "world", "?"),
                    data.getInt(base + "x"), data.getInt(base + "y"), data.getInt(base + "z"),
                    data.getBoolean(base + "automatic"),
                    data.getString(base + "result", "?")));
        }
        return result.stream().sorted(Comparator.comparingLong(HistoryEntry::epochMillis).reversed())
                .limit(Math.max(1, Math.min(50, requestedLimit))).toList();
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException error) {
            plugin.getLogger().log(Level.SEVERE, "Could not save meteor-history.yml", error);
        }
    }

    public record HistoryEntry(@NotNull String eventId, long epochMillis,
                               @NotNull String formattedTime, @NotNull String type,
                               @NotNull String world, int x, int y, int z,
                               boolean automatic, @NotNull String result) {}
}
