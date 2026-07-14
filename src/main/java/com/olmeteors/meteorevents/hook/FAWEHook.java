package com.olmeteors.meteorevents.hook;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Hook for FastAsyncWorldEdit (FAWE) integration.
 * Provides asynchronous schematic pasting and terrain capture operations
 * using FAWE's high-performance API.
 */
public final class FAWEHook {

    private final MeteorPlugin plugin;
    private boolean available;

    public FAWEHook(MeteorPlugin plugin) {
        this.plugin = plugin;
        checkAvailability();
    }

    private void checkAvailability() {
        final var pluginManager = plugin.getServer().getPluginManager();
        final var fawe = pluginManager.getPlugin("FastAsyncWorldEdit");
        final var worldEdit = pluginManager.getPlugin("WorldEdit");
        if ((fawe == null || !fawe.isEnabled()) && (worldEdit == null || !worldEdit.isEnabled())) {
            this.available = false;
            plugin.getLogger().info("FAWE/WorldEdit not installed or enabled - schematic features disabled");
            return;
        }

        try {
            Class.forName("com.sk89q.worldedit.WorldEdit");
            Class.forName("com.sk89q.worldedit.EditSession");
            this.available = true;
            plugin.getLogger().info("FAWE/WorldEdit hook initialized successfully");
        } catch (ClassNotFoundException | LinkageError error) {
            this.available = false;
            plugin.getLogger().log(Level.WARNING,
                    "FAWE/WorldEdit API is missing or incompatible - schematic features disabled", error);
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /** Resolves the exact world bounds affected by the same centred paste logic. */
    public @Nullable SchematicPasteBounds getSchematicPasteBounds(
            @NotNull File schematicFile, @NotNull Location center) {
        if (!available || center.getWorld() == null) return null;
        try {
            final ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
            if (format == null) return null;
            final Clipboard clipboard;
            try (final ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                clipboard = reader.read();
            }
            final BlockVector3 pasteAt = BlockVector3.at(
                    center.getBlockX() - clipboard.getDimensions().x() / 2,
                    center.getBlockY(),
                    center.getBlockZ() - clipboard.getDimensions().z() / 2);
            final BlockVector3 origin = clipboard.getOrigin();
            final BlockVector3 sourceMin = clipboard.getMinimumPoint();
            final BlockVector3 sourceMax = clipboard.getMaximumPoint();
            final Location minimum = new Location(center.getWorld(),
                    pasteAt.x() + sourceMin.x() - origin.x(),
                    pasteAt.y() + sourceMin.y() - origin.y(),
                    pasteAt.z() + sourceMin.z() - origin.z());
            final Location maximum = new Location(center.getWorld(),
                    pasteAt.x() + sourceMax.x() - origin.x(),
                    pasteAt.y() + sourceMax.y() - origin.y(),
                    pasteAt.z() + sourceMax.z() - origin.z());
            return new SchematicPasteBounds(minimum, maximum);
        } catch (IOException | RuntimeException | LinkageError error) {
            plugin.getLogger().log(Level.WARNING,
                    "Could not read exact schematic bounds for " + schematicFile.getName(), error);
            return null;
        }
    }

    public record SchematicPasteBounds(@NotNull Location minimum,
                                       @NotNull Location maximum) {}

    /**
     * Pastes a schematic file at the given location asynchronously.
     *
     * @param schematicFile the schematic file to paste
     * @param center        the center location for the paste
     * @return CompletableFuture that completes when the paste finishes
     */
    public @NotNull CompletableFuture<Boolean> pasteSchematicAsync(@NotNull File schematicFile,
                                                                    @NotNull Location center) {
        if (!available) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                final World adaptedWorld = BukkitAdapter.adapt(center.getWorld());
                final ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);

                if (format == null) {
                    plugin.getLogger().warning("Unsupported schematic format: " + schematicFile.getName());
                    return false;
                }

                // Read the schematic
                final Clipboard clipboard;
                try (final ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                    clipboard = reader.read();
                }

                // Calculate paste position (center the schematic on the impact point)
                final BlockVector3 pasteAt = BlockVector3.at(
                        center.getBlockX() - clipboard.getDimensions().x() / 2,
                        center.getBlockY(),
                        center.getBlockZ() - clipboard.getDimensions().z() / 2
                );

                // Paste using FAWE for async operation
                try (final EditSession editSession = WorldEdit.getInstance()
                        .newEditSessionBuilder()
                        .world(adaptedWorld)
                        .fastMode(true)
                        .build()) {

                    final ClipboardHolder holder = new ClipboardHolder(clipboard);
                    final Operation operation = holder
                            .createPaste(editSession)
                            .to(pasteAt)
                            .ignoreAirBlocks(true)
                            .copyEntities(false)
                            .copyBiomes(false)
                            .build();

                    Operations.complete(operation);
                }

                plugin.getLogger().info("Schematic pasted successfully: " + schematicFile.getName()
                        + " at " + formatLocation(center));
                return true;

            } catch (IOException | WorldEditException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to paste schematic: "
                        + schematicFile.getName(), e);
                return false;
            } catch (Exception | LinkageError error) {
                this.available = false;
                plugin.getLogger().log(Level.SEVERE,
                        "Unexpected FAWE/WorldEdit error; integration disabled for this session", error);
                return false;
            }
        });
    }

    /**
     * Captures the current terrain state within the given bounds for rollback purposes.
     *
     * @param world  the world
     * @param min    minimum corner
     * @param max    maximum corner
     * @return a Clipboard containing the captured terrain, or null on failure
     */
    public @Nullable Clipboard captureTerrain(@NotNull org.bukkit.World world,
                                               @NotNull Location min, @NotNull Location max) {
        if (!available) return null;

        try {
            final World adaptedWorld = BukkitAdapter.adapt(world);
            final BlockVector3 minVec = BlockVector3.at(min.getBlockX(), min.getBlockY(), min.getBlockZ());
            final BlockVector3 maxVec = BlockVector3.at(max.getBlockX(), max.getBlockY(), max.getBlockZ());
            final CuboidRegion region = new CuboidRegion(adaptedWorld, minVec, maxVec);

            final BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            clipboard.setOrigin(minVec);

            try (final EditSession editSession = WorldEdit.getInstance()
                    .newEditSessionBuilder()
                    .world(adaptedWorld)
                    .fastMode(true)
                    .build()) {

                final ForwardExtentCopy copy = new ForwardExtentCopy(
                        editSession, region, clipboard, minVec);
                Operations.complete(copy);
            }

            plugin.getLogger().info("Terrain captured for rollback: "
                    + region.getArea() + " blocks");
            return clipboard;

        } catch (Exception | LinkageError error) {
            this.available = false;
            plugin.getLogger().log(Level.WARNING,
                    "Failed to capture terrain; FAWE/WorldEdit disabled for this session", error);
            return null;
        }
    }

    /**
     * Restores terrain from a previously captured clipboard asynchronously.
     *
     * @param clipboard the clipboard containing the original terrain
     * @param world     the world
     * @param origin    the origin location for the paste
     * @return CompletableFuture that completes with true on success
     */
    public @NotNull CompletableFuture<Boolean> restoreTerrainAsync(@NotNull Clipboard clipboard,
                                                                    @NotNull org.bukkit.World world,
                                                                    @NotNull Location origin) {
        if (!available) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                final World adaptedWorld = BukkitAdapter.adapt(world);
                final BlockVector3 pasteAt = BlockVector3.at(
                        origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());

                try (final EditSession editSession = WorldEdit.getInstance()
                        .newEditSessionBuilder()
                        .world(adaptedWorld)
                        .fastMode(true)
                        .build()) {

                    final ClipboardHolder holder = new ClipboardHolder(clipboard);
                    final Operation operation = holder
                            .createPaste(editSession)
                            .to(pasteAt)
                            .ignoreAirBlocks(false)
                            .copyEntities(false)
                            .copyBiomes(false)
                            .build();

                    Operations.complete(operation);
                }

                plugin.getLogger().info("Terrain restored successfully");
                return true;

            } catch (Exception | LinkageError error) {
                this.available = false;
                plugin.getLogger().log(Level.WARNING,
                        "Failed to restore terrain; FAWE/WorldEdit disabled for this session", error);
                return false;
            }
        });
    }

    /** Writes an already captured terrain clipboard for crash recovery. */
    public boolean writeRecoveryClipboard(@NotNull Clipboard clipboard, @NotNull File outputFile) {
        if (!available) return false;
        try {
            final File parent = outputFile.getParentFile();
            if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) return false;
            try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC
                    .getWriter(new FileOutputStream(outputFile))) {
                writer.write(clipboard);
            }
            return true;
        } catch (IOException | RuntimeException | LinkageError error) {
            plugin.getLogger().log(Level.WARNING, "Could not persist recovery snapshot", error);
            return false;
        }
    }

    /** Reads a terrain clipboard previously written for crash recovery. */
    public @Nullable Clipboard readRecoveryClipboard(@NotNull File inputFile) {
        if (!available || !inputFile.isFile()) return null;
        try (ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC
                .getReader(new FileInputStream(inputFile))) {
            return reader.read();
        } catch (IOException | RuntimeException | LinkageError error) {
            plugin.getLogger().log(Level.WARNING, "Could not read recovery snapshot", error);
            return null;
        }
    }

    /**
     * Captures a cuboid selection and writes it as a modern Sponge v3 .schem file.
     */
    public @NotNull CompletableFuture<Boolean> saveSchematicAsync(@NotNull File outputFile,
                                                                   @NotNull org.bukkit.World world,
                                                                   @NotNull Location first,
                                                                   @NotNull Location second) {
        if (!available) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                final File parent = outputFile.getParentFile();
                if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
                    plugin.getLogger().warning("Could not create schematic output directory");
                    return false;
                }

                final Location min = new Location(world,
                        Math.min(first.getBlockX(), second.getBlockX()),
                        Math.min(first.getBlockY(), second.getBlockY()),
                        Math.min(first.getBlockZ(), second.getBlockZ()));
                final Location max = new Location(world,
                        Math.max(first.getBlockX(), second.getBlockX()),
                        Math.max(first.getBlockY(), second.getBlockY()),
                        Math.max(first.getBlockZ(), second.getBlockZ()));
                final Clipboard clipboard = captureTerrain(world, min, max);
                if (clipboard == null) {
                    return false;
                }

                try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC
                        .getWriter(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
                    writer.write(clipboard);
                }

                plugin.getLogger().info("Schematic saved: " + outputFile.getName());
                return true;
            } catch (IOException | RuntimeException | LinkageError error) {
                plugin.getLogger().log(Level.SEVERE,
                        "Failed to save schematic " + outputFile.getName(), error);
                return false;
            }
        });
    }

    /** Clears a setup selection asynchronously after an administrator confirms deletion. */
    public @NotNull CompletableFuture<Boolean> clearAreaAsync(@NotNull org.bukkit.World world,
                                                               @NotNull Location first,
                                                               @NotNull Location second) {
        if (!available) return CompletableFuture.completedFuture(false);
        return CompletableFuture.supplyAsync(() -> {
            try {
                final World adaptedWorld = BukkitAdapter.adapt(world);
                final CuboidRegion region = new CuboidRegion(adaptedWorld,
                        BlockVector3.at(Math.min(first.getBlockX(), second.getBlockX()),
                                Math.min(first.getBlockY(), second.getBlockY()),
                                Math.min(first.getBlockZ(), second.getBlockZ())),
                        BlockVector3.at(Math.max(first.getBlockX(), second.getBlockX()),
                                Math.max(first.getBlockY(), second.getBlockY()),
                                Math.max(first.getBlockZ(), second.getBlockZ())));
                try (final EditSession editSession = WorldEdit.getInstance().newEditSession(adaptedWorld)) {
                    editSession.setBlocks((com.sk89q.worldedit.regions.Region) region,
                            java.util.Objects.requireNonNull(BlockTypes.AIR).getDefaultState());
                    editSession.flushQueue();
                }
                return true;
            } catch (Exception | LinkageError error) {
                plugin.getLogger().log(Level.WARNING, "Could not clear setup selection", error);
                return false;
            }
        });
    }

    /** Creates the free built-in meteor schematic on first startup. */
    public @NotNull CompletableFuture<Boolean> createDefaultMeteorSchematicAsync(@NotNull File outputFile) {
        if (!available || outputFile.exists()) {
            return CompletableFuture.completedFuture(outputFile.exists());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                final File parent = outputFile.getParentFile();
                if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
                    return false;
                }

                final int size = 13;
                final int center = size / 2;
                final CuboidRegion region = new CuboidRegion(
                        BlockVector3.at(0, 0, 0), BlockVector3.at(size - 1, 6, size - 1));
                final BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
                clipboard.setOrigin(BlockVector3.at(0, 0, 0));

                final BlockState blackstone = java.util.Objects.requireNonNull(BlockTypes.BLACKSTONE).getDefaultState();
                final BlockState magma = java.util.Objects.requireNonNull(BlockTypes.MAGMA_BLOCK).getDefaultState();
                final BlockState obsidian = java.util.Objects.requireNonNull(BlockTypes.OBSIDIAN).getDefaultState();
                final BlockState cryingObsidian = java.util.Objects.requireNonNull(BlockTypes.CRYING_OBSIDIAN).getDefaultState();

                for (int x = 0; x < size; x++) {
                    for (int z = 0; z < size; z++) {
                        final int dx = x - center;
                        final int dz = z - center;
                        final double distance = Math.sqrt(dx * dx + dz * dz);
                        if (distance > 6.2) {
                            continue;
                        }
                        final int height = Math.max(0, (int) Math.round(4.8 - distance * 0.7));
                        for (int y = 0; y <= height; y++) {
                            final int selector = Math.floorMod(x * 31 + y * 17 + z * 13, 11);
                            final BlockState state = selector == 0 ? cryingObsidian
                                    : selector <= 2 ? magma
                                    : distance < 2.2 ? obsidian : blackstone;
                            clipboard.setBlock(BlockVector3.at(x, y, z), state);
                        }
                    }
                }

                try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC
                        .getWriter(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
                    writer.write(clipboard);
                }
                plugin.getLogger().info("Free default meteor schematic created: " + outputFile.getName());
                return true;
            } catch (IOException | RuntimeException | LinkageError error) {
                plugin.getLogger().log(Level.WARNING, "Could not create the default meteor schematic", error);
                return false;
            }
        });
    }

    private String formatLocation(Location loc) {
        return String.format("%.0f, %.0f, %.0f (%s)",
                loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
    }
}
