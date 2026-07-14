package com.olmeteors.meteorevents.hook;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.palmergames.bukkit.towny.TownyAPI;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.logging.Level;

/** Classloader-safe Bukkit-only facade for the optional Towny integration. */
public final class TownyHook {
    private final Access access;

    private TownyHook(Access access) {
        this.access = Objects.requireNonNull(access, "access");
    }

    public static @NotNull TownyHook create(@NotNull MeteorPlugin plugin,
                                             @Nullable Plugin townyPlugin) {
        if (townyPlugin == null || !townyPlugin.isEnabled()) {
            plugin.getLogger().info("Towny not installed or enabled - claim conflict detection disabled");
            return new TownyHook(DisabledAccess.INSTANCE);
        }
        try {
            Class.forName("com.palmergames.bukkit.towny.TownyAPI", false,
                    townyPlugin.getClass().getClassLoader());
            final TownyHook hook = new TownyHook(new TownyAccess(plugin));
            plugin.getLogger().info("Towny hook initialized successfully");
            return hook;
        } catch (ClassNotFoundException | LinkageError | RuntimeException error) {
            plugin.getLogger().log(Level.WARNING,
                    "Towny API is missing or incompatible - claim detection disabled safely", error);
            return new TownyHook(DisabledAccess.INSTANCE);
        }
    }

    public boolean isAvailable() { return access.isAvailable(); }
    public boolean isInClaim(@NotNull Location center, int radius) {
        return access.isInClaim(center, Math.max(0, radius));
    }

    interface Access {
        boolean isAvailable();
        boolean isInClaim(Location center, int radius);
    }

    private enum DisabledAccess implements Access {
        INSTANCE;
        @Override public boolean isAvailable() { return false; }
        @Override public boolean isInClaim(Location center, int radius) { return false; }
    }
}

/**
 * Hook for Towny integration providing claim conflict detection.
 * Ensures meteor events only spawn in wilderness areas.
 */
final class TownyAccess implements TownyHook.Access {

    private final MeteorPlugin plugin;
    private boolean available;

    TownyAccess(MeteorPlugin plugin) {
        this.plugin = plugin;
        this.available = true;
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Checks if a location area is within any Towny claim.
     * Checks multiple points within the given radius.
     *
     * @param center the center location
     * @param radius the radius to check
     * @return true if any part of the area is in a claim
     */
    public boolean isInClaim(@NotNull Location center, int radius) {
        if (!available) return false;

        try {
            final TownyAPI townyAPI = TownyAPI.getInstance();

            // Check center first
            if (isLocationInClaim(townyAPI, center)) return true;

            // Towny claims are chunk-sized TownBlocks. Check the centre of every
            // intersecting chunk so narrow/border claims cannot fall between samples.
            final int minChunkX = Math.floorDiv(center.getBlockX() - radius, 16);
            final int maxChunkX = Math.floorDiv(center.getBlockX() + radius, 16);
            final int minChunkZ = Math.floorDiv(center.getBlockZ() - radius, 16);
            final int maxChunkZ = Math.floorDiv(center.getBlockZ() + radius, 16);
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    final Location checkLoc = new Location(center.getWorld(),
                            chunkX * 16 + 8, center.getY(), chunkZ * 16 + 8);
                    if (isLocationInClaim(townyAPI, checkLoc)) {
                        return true;
                    }
                }
            }

        } catch (Exception | LinkageError error) {
            plugin.getLogger().log(Level.WARNING,
                    "Towny claim check failed for one candidate; rejecting that location", error);
            return true;
        }

        return false;
    }

    /**
     * Checks if a specific location is in a Towny claim (non-wilderness).
     */
    private boolean isLocationInClaim(@NotNull TownyAPI townyAPI, @NotNull Location location) {
        try {
            final var townBlock = townyAPI.getTownBlock(location);
            return townBlock != null && townBlock.hasTown();
        } catch (Exception e) {
            // Towny throws exceptions for wilderness locations
            return false;
        }
    }
}
