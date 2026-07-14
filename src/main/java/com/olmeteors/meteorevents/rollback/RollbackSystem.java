package com.olmeteors.meteorevents.rollback;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.olmeteors.meteorevents.hook.FAWEHook;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Handles asynchronous rollback operations to restore terrain after meteor events.
 * Uses FAWE's high-performance async editing for zero-lag restoration.
 */
public final class RollbackSystem {

    private final MeteorPlugin plugin;
    private final FAWEHook faweHook;
    private final Map<String, TerrainSnapshot> snapshots = new ConcurrentHashMap<>();
    private final File recoveryDirectory;

    public RollbackSystem(MeteorPlugin plugin, FAWEHook faweHook) {
        this.plugin = plugin;
        this.faweHook = faweHook;
        this.recoveryDirectory = new File(plugin.getDataFolder(), "recovery");
    }

    public boolean captureArea(@NotNull String eventId, @NotNull Location center, int radius) {
        final World world = center.getWorld();
        if (world == null || !faweHook.isAvailable()) {
            return false;
        }
        final int minY = Math.max(world.getMinHeight(), center.getBlockY() - 32);
        final int maxY = Math.min(world.getMaxHeight() - 1, center.getBlockY() + 96);
        final Location min = new Location(world, center.getBlockX() - radius, minY,
                center.getBlockZ() - radius);
        final Location max = new Location(world, center.getBlockX() + radius, maxY,
                center.getBlockZ() + radius);
        return captureBounds(eventId, world, min, max);
    }

    /**
     * Captures an event structure whose schematic origin is its minimum selection corner.
     * Plugin-created schematics use that convention, so the structure extends mostly in
     * the positive X/Z directions instead of being centred around the paste location.
     */
    public boolean captureFromPasteOrigin(@NotNull String eventId, @NotNull Location origin,
                                          int configuredRadius) {
        final World world = origin.getWorld();
        if (world == null || !faweHook.isAvailable()) {
            return false;
        }
        final int buffer = 12;
        final int diameter = Math.max(1, configuredRadius * 2);
        final int minY = Math.max(world.getMinHeight(), origin.getBlockY() - 32);
        final int maxY = Math.min(world.getMaxHeight() - 1, origin.getBlockY() + 96);
        final Location min = new Location(world, origin.getBlockX() - buffer, minY,
                origin.getBlockZ() - buffer);
        final Location max = new Location(world, origin.getBlockX() + diameter + buffer, maxY,
                origin.getBlockZ() + diameter + buffer);
        return captureBounds(eventId, world, min, max);
    }

    /** Captures the exact schematic footprint plus a safety border. */
    public boolean captureSchematicArea(@NotNull String eventId, @NotNull Location center,
                                        File schematicFile, int fallbackRadius) {
        if (schematicFile == null) return captureArea(eventId, center, fallbackRadius);
        final FAWEHook.SchematicPasteBounds bounds =
                faweHook.getSchematicPasteBounds(schematicFile, center);
        final World world = center.getWorld();
        if (bounds == null || world == null) return captureArea(eventId, center, fallbackRadius);

        final int horizontalPadding = 12;
        final Location min = bounds.minimum().clone().add(-horizontalPadding, -32, -horizontalPadding);
        final Location max = bounds.maximum().clone().add(horizontalPadding, 64, horizontalPadding);
        min.setY(Math.max(world.getMinHeight(), min.getY()));
        max.setY(Math.min(world.getMaxHeight() - 1, max.getY()));
        return captureBounds(eventId, world, min, max);
    }

    private boolean captureBounds(@NotNull String eventId, @NotNull World world,
                                  @NotNull Location min, @NotNull Location max) {
        final Object clipboard = faweHook.captureTerrain(world, min, max);
        if (clipboard == null) {
            return false;
        }
        snapshots.put(eventId, new TerrainSnapshot(clipboard, world, min));
        if (plugin.getConfigManager().isCrashRecoveryEnabled()
                && !persistSnapshot(eventId, clipboard, world, min)) {
            snapshots.remove(eventId);
            return false;
        }
        return true;
    }

    private boolean persistSnapshot(String eventId, Object clipboard, World world, Location minimum) {
        final File schematic = new File(recoveryDirectory, eventId + ".schem");
        if (!faweHook.writeRecoveryClipboard(clipboard, schematic)) return false;
        final YamlConfiguration metadata = new YamlConfiguration();
        metadata.set("event-id", eventId);
        metadata.set("world", world.getName());
        metadata.set("x", minimum.getBlockX());
        metadata.set("y", minimum.getBlockY());
        metadata.set("z", minimum.getBlockZ());
        try {
            metadata.save(new File(recoveryDirectory, eventId + ".yml"));
            return true;
        } catch (java.io.IOException error) {
            plugin.getLogger().log(Level.WARNING, "Could not save recovery metadata", error);
            schematic.delete();
            return false;
        }
    }

    public @NotNull CompletableFuture<Boolean> rollbackEvent(@NotNull String eventId) {
        final TerrainSnapshot snapshot = snapshots.remove(eventId);
        if (snapshot == null) {
            plugin.getLogger().warning("No pre-impact terrain snapshot for event " + eventId);
            return CompletableFuture.completedFuture(false);
        }
        return faweHook.restoreTerrainAsync(snapshot.clipboard(), snapshot.world(),
                        snapshot.minimumPoint())
                .thenCompose(success -> {
                    if (Boolean.TRUE.equals(success)) return CompletableFuture.completedFuture(true);
                    plugin.getLogger().warning("Retrying terrain restore for event " + eventId);
                    return faweHook.restoreTerrainAsync(snapshot.clipboard(), snapshot.world(),
                            snapshot.minimumPoint());
                }).whenComplete((success, error) -> {
                    if (error == null && Boolean.TRUE.equals(success)) deleteRecovery(eventId);
                });
    }

    public void discardSnapshot(@NotNull String eventId) {
        snapshots.remove(eventId);
        deleteRecovery(eventId);
    }

    /** Restores every snapshot left behind by a crash/restart. */
    public int recoverPendingSnapshots() {
        if (!plugin.getConfigManager().isCrashRecoveryEnabled()
                || !recoveryDirectory.isDirectory() || !faweHook.isAvailable()) return 0;
        final File[] metadataFiles = recoveryDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (metadataFiles == null) return 0;
        int queued = 0;
        for (final File metadataFile : metadataFiles) {
            try {
                final YamlConfiguration metadata = YamlConfiguration.loadConfiguration(metadataFile);
                final String eventId = metadata.getString("event-id");
                final World world = plugin.getServer().getWorld(metadata.getString("world", ""));
                if (eventId == null || world == null) continue;
                final File schematic = new File(recoveryDirectory, eventId + ".schem");
                final Object clipboard = faweHook.readRecoveryClipboard(schematic);
                if (clipboard == null) continue;
                final Location minimum = new Location(world, metadata.getInt("x"),
                        metadata.getInt("y"), metadata.getInt("z"));
                queued++;
                faweHook.restoreTerrainAsync(clipboard, world, minimum).whenComplete((success, error) -> {
                    if (error == null && Boolean.TRUE.equals(success)) {
                        deleteRecovery(eventId);
                        plugin.getLogger().info("Recovered terrain left by interrupted event " + eventId);
                    } else {
                        plugin.getLogger().warning("Recovery failed and was retained for retry: " + eventId);
                    }
                });
            } catch (RuntimeException error) {
                plugin.getLogger().log(Level.WARNING,
                        "Invalid recovery metadata: " + metadataFile.getName(), error);
            }
        }
        return queued;
    }

    public int getPendingRecoveryCount() {
        final File[] files = recoveryDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        return files == null ? 0 : files.length;
    }

    private void deleteRecovery(String eventId) {
        new File(recoveryDirectory, eventId + ".schem").delete();
        new File(recoveryDirectory, eventId + ".yml").delete();
    }

    private record TerrainSnapshot(Object clipboard, World world, Location minimumPoint) {}
}
