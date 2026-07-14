package com.olmeteors.meteorevents.hook;

import com.olmeteors.meteorevents.MeteorPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.logging.Level;

/**
 * Classloader-safe facade for the optional WorldGuard integration.
 *
 * <p>This class deliberately exposes and stores Bukkit types only. Consequently,
 * the JVM can load the plugin and this facade even when WorldGuard is absent or
 * incompatible. The API-backed implementation is loaded only after all required
 * WorldGuard/WorldEdit classes have been verified in WorldGuard's classloader.</p>
 */
public final class WGHook {

    private static final String[] REQUIRED_CLASSES = {
            "com.sk89q.worldguard.WorldGuard",
            "com.sk89q.worldguard.protection.flags.Flag",
            "com.sk89q.worldguard.protection.flags.Flags",
            "com.sk89q.worldguard.protection.flags.StateFlag",
            "com.sk89q.worldguard.protection.managers.RegionManager",
            "com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion",
            "com.sk89q.worldedit.bukkit.BukkitAdapter",
            "com.sk89q.worldedit.math.BlockVector3"
    };

    private final Access access;

    private WGHook(@NotNull Access access) {
        this.access = Objects.requireNonNull(access, "access");
    }

    /**
     * Creates a safe WorldGuard hook. A missing, disabled, or API-incompatible
     * WorldGuard installation produces a no-op hook instead of an enable failure.
     */
    public static @NotNull WGHook create(@NotNull MeteorPlugin plugin,
                                          @Nullable Plugin worldGuardPlugin) {
        Objects.requireNonNull(plugin, "plugin");

        if (worldGuardPlugin == null || !worldGuardPlugin.isEnabled()) {
            plugin.getLogger().info("WorldGuard is not installed/enabled; region protection is disabled");
            return new WGHook(DisabledAccess.INSTANCE);
        }

        try {
            final ClassLoader classLoader = worldGuardPlugin.getClass().getClassLoader();
            for (String className : REQUIRED_CLASSES) {
                Class.forName(className, false, classLoader);
            }

            final WGHook hook = new WGHook(new WorldGuardAccess(plugin));
            plugin.getLogger().info("WorldGuard hook initialized successfully");
            return hook;
        } catch (ClassNotFoundException | LinkageError | RuntimeException error) {
            plugin.getLogger().log(Level.WARNING,
                    "WorldGuard was found, but its API is missing or incompatible; "
                            + "region protection has been disabled safely", error);
            return new WGHook(DisabledAccess.INSTANCE);
        }
    }

    public boolean isAvailable() {
        return access.isAvailable();
    }

    public void createTemporaryRegion(@NotNull String regionName, @NotNull World world,
                                      @NotNull BoundingBox bounds) {
        Objects.requireNonNull(regionName, "regionName");
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(bounds, "bounds");
        if (!regionName.isBlank()) {
            access.createTemporaryRegion(regionName, world, bounds);
        }
    }

    public void removeTemporaryRegion(@NotNull String regionName, @NotNull World world) {
        Objects.requireNonNull(regionName, "regionName");
        Objects.requireNonNull(world, "world");
        if (!regionName.isBlank()) {
            access.removeTemporaryRegion(regionName, world);
        }
    }

    public boolean isInRegion(@NotNull Location location, int radius) {
        Objects.requireNonNull(location, "location");
        if (location.getWorld() == null) {
            return false;
        }
        return access.isInRegion(location, Math.max(0, radius));
    }

    /** Bukkit-only boundary implemented by the lazily loaded API adapter. */
    interface Access {
        boolean isAvailable();

        void createTemporaryRegion(String regionName, World world, BoundingBox bounds);

        void removeTemporaryRegion(String regionName, World world);

        boolean isInRegion(Location location, int radius);
    }

    private enum DisabledAccess implements Access {
        INSTANCE;

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void createTemporaryRegion(String regionName, World world, BoundingBox bounds) {
            // Optional integration is unavailable.
        }

        @Override
        public void removeTemporaryRegion(String regionName, World world) {
            // Optional integration is unavailable.
        }

        @Override
        public boolean isInRegion(Location location, int radius) {
            return false;
        }
    }
}
