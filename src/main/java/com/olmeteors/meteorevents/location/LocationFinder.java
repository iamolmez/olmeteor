package com.olmeteors.meteorevents.location;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.olmeteors.meteorevents.config.ConfigManager;
import com.olmeteors.meteorevents.event.MeteorType;
import com.olmeteors.meteorevents.event.RadiusShape;
import com.olmeteors.meteorevents.hook.TownyHook;
import com.olmeteors.meteorevents.hook.WGHook;
import com.olmeteors.meteorevents.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.ChunkSnapshot;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Handles intelligent location selection for meteor events.
 * Performs terrain variance checks, claim conflict detection,
 * and ensures suitable landing zones.
 */
public final class LocationFinder {

    private final MeteorPlugin plugin;
    private final ConfigManager configManager;
    private final WGHook wgHook;
    private final TownyHook townyHook;
    private final java.util.Deque<RecentImpact> recentImpacts = new ConcurrentLinkedDeque<>();

    private static final Material[] BLOCKED_MATERIALS = {
            Material.WATER, Material.LAVA, Material.SEAGRASS,
            Material.TALL_SEAGRASS, Material.KELP, Material.KELP_PLANT,
            Material.MAGMA_BLOCK, Material.CACTUS, Material.SWEET_BERRY_BUSH,
            Material.POWDER_SNOW
    };

    private static final Material[] ACCEPTABLE_FLOOR_MATERIALS = {
            Material.GRASS_BLOCK, Material.STONE, Material.DIRT, Material.COARSE_DIRT,
            Material.PODZOL, Material.SAND, Material.SANDSTONE, Material.RED_SAND,
            Material.RED_SANDSTONE, Material.GRAVEL, Material.TERRACOTTA,
            Material.WHITE_TERRACOTTA, Material.ORANGE_TERRACOTTA,
            Material.MAGENTA_TERRACOTTA, Material.LIGHT_BLUE_TERRACOTTA,
            Material.YELLOW_TERRACOTTA, Material.LIME_TERRACOTTA,
            Material.PINK_TERRACOTTA, Material.GRAY_TERRACOTTA,
            Material.LIGHT_GRAY_TERRACOTTA, Material.CYAN_TERRACOTTA,
            Material.PURPLE_TERRACOTTA, Material.BLUE_TERRACOTTA,
            Material.BROWN_TERRACOTTA, Material.GREEN_TERRACOTTA,
            Material.RED_TERRACOTTA, Material.BLACK_TERRACOTTA,
            Material.SNOW_BLOCK, Material.PACKED_ICE, Material.BLUE_ICE,
            Material.NETHERRACK, Material.SOUL_SAND, Material.SOUL_SOIL,
            Material.END_STONE, Material.MOSS_BLOCK
    };

    public LocationFinder(MeteorPlugin plugin, WGHook wgHook, TownyHook townyHook) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.wgHook = wgHook;
        this.townyHook = townyHook;
    }

    /**
     * Finds a suitable location for a meteor event of the given type.
     * Uses configurable max attempts to prevent infinite loops.
     *
     * @param world the world to search in
     * @param type  the meteor type (determines radius requirements)
     * @return an Optional containing the center location if found
     */
    public @NotNull Optional<Location> findSuitableLocation(@NotNull World world, @NotNull MeteorType type) {
        return findSuitableLocation(world, type, configManager.getLocationPreset());
    }

    public @NotNull Optional<Location> findSuitableLocation(@NotNull World world,
                                                             @NotNull MeteorType type,
                                                             @NotNull ConfigManager.LocationPreset preset) {
        return findSuitableLocation(world, type, preset,
                configManager.getMinEventDistance(), configManager.getMaxEventDistance());
    }

    public @NotNull Optional<Location> findSuitableLocation(@NotNull World world,
                                                             @NotNull MeteorType type,
                                                             @NotNull ConfigManager.LocationPreset preset,
                                                             int minDistance, int maxDistance) {
        final int maxAttempts = configManager.getMaxLocationAttempts();
        final int bufferZone = configManager.getBufferZone();
        final int impactRadius = configManager.getTypeConfig(type).impactRadius();
        final int varianceTolerance = configManager.getTerrainVarianceTolerance();

        plugin.getLogger().info("Searching for suitable meteor location in world: "
                + world.getName() + " (max " + maxAttempts + " attempts)");

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // Generate random coordinates within bounds
                final int[] coordinates = generateCoordinates(world, minDistance, maxDistance,
                        RadiusShape.CIRCLE);
                if (coordinates == null) return Optional.empty();
                final int x = coordinates[0];
                final int z = coordinates[1];

                final int minY = Math.max(world.getMinHeight() + 1, preset.minY());
                final int maxY = Math.min(world.getMaxHeight() - 2, preset.maxY());
                if (minY > maxY) continue;
                final boolean surfaceMode = preset.mode().equals("surface")
                        || preset.mode().equals("water");
                final int candidateY = surfaceMode
                        ? findSurfaceY(world, x, z, minY, maxY, preset).orElse(Integer.MIN_VALUE)
                        : ThreadLocalRandom.current().nextInt(minY, maxY + 1);
                if (candidateY < minY || candidateY > maxY) {
                    plugin.getLogger().fine("Attempt " + attempt + ": Location (" + x + "," + z
                            + ") is outside preset Y range: " + candidateY);
                    continue;
                }

                final Location candidate = new Location(world, x + 0.5, candidateY, z + 0.5);
                if (isRecentlyUsed(candidate)) continue;

                // Check terrain suitability
                if (surfaceMode && !isTerrainSuitable(world, x, candidateY, z, impactRadius,
                        varianceTolerance, preset)) {
                    continue;
                }

                // Check claim conflicts
                if (isLocationConflicting(candidate, impactRadius, bufferZone)) {
                    continue;
                }

                // Check water/lava presence
                if (surfaceMode && isAreaFlooded(world, x, candidateY, z, impactRadius, preset)) {
                    continue;
                }

                // Location passed all checks
                final Location center = candidate;
                plugin.getLogger().info("Suitable meteor location found at: "
                        + formatLocation(center) + " (after " + attempt + " attempts)");
                return Optional.of(center);

            } catch (Exception e) {
                plugin.getLogger().log(Level.FINE, "Error during location search attempt " + attempt, e);
            }
        }

        // No suitable location found after max attempts
        plugin.getLogger().warning("Could not find suitable meteor location after "
                + maxAttempts + " attempts in world: " + world.getName());
        return Optional.empty();
    }

    /**
     * Folia-safe location search. Chunk data is read from immutable snapshots and
     * optional claim integrations are queried on the candidate's owning region.
     */
    public @NotNull CompletableFuture<Optional<Location>> findSuitableLocationAsync(
            @NotNull World world, @NotNull MeteorType type) {
        return findSuitableLocationAsync(world, type, configManager.getLocationPreset(),
                configManager.getMinEventDistance(), configManager.getMaxEventDistance());
    }

    public @NotNull CompletableFuture<Optional<Location>> findSuitableLocationAsync(
            @NotNull World world, @NotNull MeteorType type,
            @NotNull ConfigManager.LocationPreset preset, int minDistance, int maxDistance) {
        return findSuitableLocationAsync(world, type, preset, minDistance, maxDistance,
                RadiusShape.CIRCLE);
    }

    public @NotNull CompletableFuture<Optional<Location>> findSuitableLocationAsync(
            @NotNull World world, @NotNull MeteorType type,
            @NotNull ConfigManager.LocationPreset preset, int minDistance, int maxDistance,
            @NotNull RadiusShape searchShape) {
        final CompletableFuture<Optional<Location>> result = new CompletableFuture<>();
        plugin.getFoliaScheduler().callGlobal(() -> searchSnapshotAttempt(world, type, preset,
                minDistance, maxDistance, searchShape, 1, result)).exceptionally(error -> {
            result.completeExceptionally(error);
            return null;
        });
        return result;
    }

    private void searchSnapshotAttempt(@NotNull World world, @NotNull MeteorType type,
                                       @NotNull ConfigManager.LocationPreset preset,
                                       int minDistance, int maxDistance,
                                       @NotNull RadiusShape searchShape, int attempt,
                                       @NotNull CompletableFuture<Optional<Location>> result) {
        if (result.isDone()) return;
        final int maxAttempts = configManager.getMaxLocationAttempts();
        if (attempt > maxAttempts) {
            plugin.getLogger().warning("Could not find suitable meteor location after "
                    + maxAttempts + " attempts in world: " + world.getName());
            result.complete(Optional.empty());
            return;
        }

        final int[] coordinates = generateCoordinates(world, minDistance, maxDistance, searchShape);
        if (coordinates == null) {
            plugin.getLogger().warning("Automatic search area does not intersect the world border in "
                    + world.getName() + " (shape=" + searchShape + ", distance="
                    + minDistance + "-" + maxDistance + ")");
            result.complete(Optional.empty());
            return;
        }
        final int x = coordinates[0];
        final int z = coordinates[1];
        final int minY = Math.max(world.getMinHeight() + 1, preset.minY());
        final int maxY = Math.min(world.getMaxHeight() - 2, preset.maxY());
        if (minY > maxY) {
            searchSnapshotAttempt(world, type, preset, minDistance, maxDistance,
                    searchShape, attempt + 1, result);
            return;
        }

        candidateYAsync(world, x, z, minY, maxY, preset).thenCompose(candidateY -> {
            if (candidateY.isEmpty()) return CompletableFuture.completedFuture(Optional.<Location>empty());
            final Location candidate = new Location(world, x + 0.5, candidateY.get(), z + 0.5);
            if (isRecentlyUsed(candidate)) return CompletableFuture.completedFuture(Optional.<Location>empty());
            final int configuredRadius = configManager.getTypeConfig(type).impactRadius();
            return validateSnapshotsAsync(candidate, configuredRadius, preset,
                    configManager.getRadiusShape(type)).thenCompose(valid -> {
                if (!valid) return CompletableFuture.completedFuture(Optional.<Location>empty());
                final AtomicBoolean conflicting = new AtomicBoolean(true);
                return plugin.getFoliaScheduler().callAtLocation(candidate, () -> conflicting.set(
                                isLocationConflicting(candidate, configuredRadius,
                                        configManager.getBufferZone())))
                        .thenApply(ignored -> conflicting.get()
                                ? Optional.<Location>empty() : Optional.of(candidate));
            });
        }).whenComplete((candidate, error) -> {
            if (result.isDone()) return;
            if (error != null || candidate == null || candidate.isEmpty()) {
                plugin.getFoliaScheduler().callGlobal(() -> searchSnapshotAttempt(world, type, preset,
                        minDistance, maxDistance, searchShape, attempt + 1, result));
                return;
            }
            plugin.getLogger().info("Suitable meteor location found at: "
                    + formatLocation(candidate.get()) + " (after " + attempt + " attempts)");
            result.complete(candidate);
        });
    }

    private CompletableFuture<Optional<Integer>> candidateYAsync(
            World world, int x, int z, int minY, int maxY, ConfigManager.LocationPreset preset) {
        final boolean surface = preset.mode().equals("surface") || preset.mode().equals("water");
        if (!surface) return CompletableFuture.completedFuture(Optional.of(
                ThreadLocalRandom.current().nextInt(minY, maxY + 1)));
        return snapshotAsync(world, x >> 4, z >> 4).thenApply(snapshot -> {
            final int highest = Math.min(maxY - 1,
                    snapshot.getHighestBlockYAt(x & 15, z & 15));
            for (int floorY = highest; floorY >= minY - 1; floorY--) {
                final Material floor = snapshot.getBlockType(x & 15, floorY, z & 15);
                if (!isAcceptableFloor(floor, preset)) continue;
                final Material feet = snapshot.getBlockType(x & 15, floorY + 1, z & 15);
                final Material head = snapshot.getBlockType(x & 15, floorY + 2, z & 15);
                if ((feet.isAir() || feet == Material.WATER && preset.allowWater())
                        && (head.isAir() || head == Material.WATER && preset.allowWater())) {
                    return Optional.of(floorY + 1);
                }
            }
            return Optional.empty();
        });
    }

    private CompletableFuture<Boolean> validateSnapshotsAsync(Location candidate, int radius,
                                                                ConfigManager.LocationPreset preset,
                                                                RadiusShape shape) {
        final World world = candidate.getWorld();
        final int centerX = candidate.getBlockX();
        final int centerY = candidate.getBlockY();
        final int centerZ = candidate.getBlockZ();
        final int minChunkX = (centerX - radius) >> 4;
        final int maxChunkX = (centerX + radius) >> 4;
        final int minChunkZ = (centerZ - radius) >> 4;
        final int maxChunkZ = (centerZ + radius) >> 4;
        final Map<Long, CompletableFuture<ChunkSnapshot>> futures = new HashMap<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                futures.put(chunkKey(cx, cz), snapshotAsync(world, cx, cz));
            }
        }
        return CompletableFuture.allOf(futures.values().toArray(CompletableFuture[]::new))
                .thenApply(ignored -> {
                    final Map<Long, ChunkSnapshot> snapshots = new HashMap<>();
                    futures.forEach((key, future) -> snapshots.put(key, future.join()));
                    if (!isAcceptableFloor(snapshotBlock(snapshots, centerX, centerY - 1, centerZ), preset)) {
                        return false;
                    }
                    if (preset.requireFlat()) {
                        int lowest = Integer.MAX_VALUE;
                        int highest = Integer.MIN_VALUE;
                        final int samplesPerAxis = Math.max(5, radius / 3);
                        final int step = Math.max(1, radius / samplesPerAxis);
                        for (int dx = -radius; dx <= radius; dx += step) {
                            for (int dz = -radius; dz <= radius; dz += step) {
                                if (!shape.contains(dx, dz, radius)) continue;
                                final int height = snapshotHeight(snapshots, centerX + dx, centerZ + dz);
                                lowest = Math.min(lowest, height);
                                highest = Math.max(highest, height);
                            }
                        }
                        if (highest - lowest > configManager.getTerrainVarianceTolerance()) return false;
                    }
                    int water = 0;
                    int lava = 0;
                    int checks = 0;
                    for (int dx = -radius; dx <= radius; dx += 2) {
                        for (int dz = -radius; dz <= radius; dz += 2) {
                            if (!shape.contains(dx, dz, radius)) continue;
                            final Material material = snapshotBlock(snapshots,
                                    centerX + dx, centerY, centerZ + dz);
                            if (material == Material.WATER) water++;
                            if (material == Material.LAVA) lava++;
                            checks++;
                        }
                    }
                    return (preset.allowWater() || water / (double) checks <= 0.3)
                            && (preset.allowLava() || lava == 0);
                });
    }

    private CompletableFuture<ChunkSnapshot> snapshotAsync(World world, int chunkX, int chunkZ) {
        return world.getChunkAtAsync(chunkX, chunkZ, true)
                .thenApply(chunk -> chunk.getChunkSnapshot(true, true, false));
    }

    private Material snapshotBlock(Map<Long, ChunkSnapshot> snapshots, int x, int y, int z) {
        return snapshots.get(chunkKey(x >> 4, z >> 4)).getBlockType(x & 15, y, z & 15);
    }

    private int snapshotHeight(Map<Long, ChunkSnapshot> snapshots, int x, int z) {
        return snapshots.get(chunkKey(x >> 4, z >> 4)).getHighestBlockYAt(x & 15, z & 15);
    }

    private long chunkKey(int x, int z) {
        return (x & 0xffffffffL) << 32 | z & 0xffffffffL;
    }

    public void recordImpact(@NotNull Location location) {
        recentImpacts.addFirst(new RecentImpact(location.getWorld().getName(),
                location.getBlockX(), location.getBlockZ(), System.currentTimeMillis()));
        while (recentImpacts.size() > 200) recentImpacts.pollLast();
    }

    public void recordImpact(@NotNull String world, int x, int z, long time) {
        recentImpacts.addLast(new RecentImpact(world, x, z, time));
    }

    private boolean isRecentlyUsed(Location candidate) {
        if (!configManager.isLocationCooldownEnabled()) return false;
        final long cutoff = System.currentTimeMillis()
                - configManager.getLocationCooldownHours() * 3_600_000L;
        recentImpacts.removeIf(impact -> impact.time < cutoff);
        final long radiusSquared = (long) configManager.getLocationCooldownRadius()
                * configManager.getLocationCooldownRadius();
        return recentImpacts.stream().filter(impact -> impact.world.equals(candidate.getWorld().getName()))
                .anyMatch(impact -> {
                    final long dx = impact.x - candidate.getBlockX();
                    final long dz = impact.z - candidate.getBlockZ();
                    return dx * dx + dz * dz < radiusSquared;
                });
    }

    private record RecentImpact(String world, int x, int z, long time) {}

    /** Finds a permitted floor with enough room above it inside the configured Y range. */
    private @NotNull Optional<Integer> findSurfaceY(@NotNull World world, int x, int z,
                                                     int minY, int maxY,
                                                     @NotNull ConfigManager.LocationPreset preset) {
        final int highest = Math.min(maxY - 1, world.getHighestBlockYAt(x, z));
        for (int floorY = highest; floorY >= minY - 1; floorY--) {
            final Material floor = world.getBlockAt(x, floorY, z).getType();
            if (!isAcceptableFloor(floor, preset)) continue;
            final Material feet = world.getBlockAt(x, floorY + 1, z).getType();
            final Material head = world.getBlockAt(x, floorY + 2, z).getType();
            final boolean feetClear = feet.isAir() || (feet == Material.WATER && preset.allowWater());
            final boolean headClear = head.isAir() || (head == Material.WATER && preset.allowWater());
            if (feetClear && headClear) return Optional.of(floorY + 1);
        }
        return Optional.empty();
    }

    /**
     * Validates the terrain variance within the impact radius.
     * Checks that the terrain is relatively flat and not obstructed.
     */
    private boolean isTerrainSuitable(@NotNull World world, int centerX, int centerY,
                                       int centerZ, int radius, int varianceTolerance,
                                       @NotNull ConfigManager.LocationPreset preset) {
        final Block centerBlock = world.getBlockAt(centerX, centerY - 1, centerZ);
        if (!isAcceptableFloor(centerBlock.getType(), preset)) {
            plugin.getLogger().fine("Center block is not acceptable: " + centerBlock.getType().name());
            return false;
        }
        if (!preset.requireFlat()) return true;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int sampleCount = 0;

        // Sample terrain at multiple points within the radius
        final int samplesPerAxis = Math.max(5, radius / 3);
        for (int dx = -radius; dx <= radius; dx += radius / samplesPerAxis) {
            for (int dz = -radius; dz <= radius; dz += radius / samplesPerAxis) {
                final int sampleX = centerX + dx;
                final int sampleZ = centerZ + dz;
                final int sampleY = world.getHighestBlockYAt(sampleX, sampleZ);

                if (sampleY > world.getMinHeight()) {
                    minY = Math.min(minY, sampleY);
                    maxY = Math.max(maxY, sampleY);
                    sampleCount++;
                }
            }
        }

        if (sampleCount == 0) return false;

        // Check terrain variance
        final int variance = maxY - minY;
        if (variance > varianceTolerance) {
            plugin.getLogger().fine("Terrain variance too high: " + variance
                    + " (tolerance: " + varianceTolerance + ")");
            return false;
        }

        return true;
    }

    /**
     * Checks if the location conflicts with WorldGuard regions or Towny claims.
     */
    private boolean isLocationConflicting(@NotNull Location location, int radius, int bufferZone) {
        // Check WorldGuard regions
        if (configManager.isWorldGuardCheckClaims() && wgHook.isAvailable()) {
            if (wgHook.isInRegion(location, radius + bufferZone)) {
                plugin.getLogger().fine("Location conflicts with WorldGuard region");
                return true;
            }
        }

        // Check Towny claims
        if (configManager.isTownyRequireWilderness() && townyHook.isAvailable()) {
            if (townyHook.isInClaim(location, radius + bufferZone)) {
                plugin.getLogger().fine("Location conflicts with Towny claim");
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the area within the radius is flooded with water or lava.
     */
    private boolean isAreaFlooded(@NotNull World world, int centerX, int centerY,
                                   int centerZ, int radius,
                                   @NotNull ConfigManager.LocationPreset preset) {
        int waterCount = 0;
        int lavaCount = 0;
        int totalChecks = 0;

        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                totalChecks++;
                final Block block = world.getBlockAt(centerX + dx, centerY, centerZ + dz);

                if (block.getType() == Material.WATER) {
                    waterCount++;
                } else if (block.getType() == Material.LAVA) {
                    lavaCount++;
                }
            }
        }

        final double waterRatio = (double) waterCount / totalChecks;
        final double lavaRatio = (double) lavaCount / totalChecks;

        // Reject if more than 30% water or any lava
        if (!preset.allowWater() && waterRatio > 0.3) {
            plugin.getLogger().fine("Area too flooded: " + (waterRatio * 100) + "% water");
            return true;
        }
        if (!preset.allowLava() && lavaRatio > 0.0) {
            plugin.getLogger().fine("Area contains lava");
            return true;
        }

        return false;
    }

    private boolean isAcceptableFloor(@NotNull Material material,
                                      @NotNull ConfigManager.LocationPreset preset) {
        if (material == Material.WATER) return preset.allowWater();
        if (material == Material.LAVA) return preset.allowLava();
        // any_surface modu: düzlük gerektirmeyen ve özel blok listesi olmayan presetler,
        // su ve lav hariç tüm blokları kabul eder (taş, toprak, ağaç, maden vs.)
        if (!preset.requireFlat() && preset.allowedFloorBlocks().isEmpty()) {
            return material.isSolid();
        }
        if (!preset.allowedFloorBlocks().isEmpty()) {
            return preset.allowedFloorBlocks().stream()
                    .map(Material::matchMaterial).filter(java.util.Objects::nonNull)
                    .anyMatch(material::equals);
        }
        for (final Material acceptable : ACCEPTABLE_FLOOR_MATERIALS) {
            if (material == acceptable) return true;
        }
        return material.name().endsWith("_TERRACOTTA")
                || material.name().endsWith("_CONCRETE")
                || material.name().endsWith("_POWDER");
    }

    private boolean isBlockedSpawn(@NotNull Material material) {
        for (final Material blocked : BLOCKED_MATERIALS) {
            if (material == blocked) return true;
        }
        return false;
    }

    private int @Nullable [] generateCoordinates(@NotNull World world, int minDistance,
                                                  int maxDistance,
                                                  @NotNull RadiusShape searchShape) {
        final Location spawn = world.getSpawnLocation();
        final org.bukkit.WorldBorder border = world.getWorldBorder();
        final int margin = 16;
        final double half = Math.max(1.0, border.getSize() / 2.0 - margin);
        final double minX = border.getCenter().getX() - half;
        final double maxX = border.getCenter().getX() + half;
        final double minZ = border.getCenter().getZ() - half;
        final double maxZ = border.getCenter().getZ() + half;
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 512; attempt++) {
            final double dx = random.nextDouble(-maxDistance, maxDistance);
            final double dz = random.nextDouble(-maxDistance, maxDistance);
            if (!searchShape.contains(dx, dz, maxDistance)
                    || (minDistance > 0 && searchShape.contains(dx, dz, minDistance))) continue;
            final int x = spawn.getBlockX() + (int) Math.round(dx);
            final int z = spawn.getBlockZ() + (int) Math.round(dz);
            if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) return new int[]{x, z};
        }
        return null;
    }

    private String formatLocation(Location loc) {
        return String.format("%.0f, %.0f, %.0f",
                loc.getX(), loc.getY(), loc.getZ());
    }
}
