package com.olmeteors.meteorevents.hook;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * WorldGuard API implementation. Never reference this class outside
 * {@link WGHook}; doing so would break the classloader-safe boundary.
 */
final class WorldGuardAccess implements WGHook.Access {

    private static final String REGION_PREFIX = "meteor_";

    private final MeteorPlugin plugin;
    private final Map<String, String> createdRegions = new ConcurrentHashMap<>();
    private volatile boolean available = true;

    WorldGuardAccess(MeteorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void createTemporaryRegion(String regionName, World world, BoundingBox bounds) {
        if (!available) {
            return;
        }

        try {
            final RegionManager regionManager = regionManager(world);
            if (regionManager == null) {
                plugin.getLogger().warning("Could not get WorldGuard region manager for " + world.getName());
                return;
            }

            final String fullName = prefixed(regionName);
            if (regionManager.hasRegion(fullName)) {
                plugin.getLogger().warning("Region " + fullName + " already exists; replacing it");
                regionManager.removeRegion(fullName);
            }

            final BlockVector3 min = BlockVector3.at(
                    Math.floor(bounds.getMinX()), Math.floor(bounds.getMinY()), Math.floor(bounds.getMinZ()));
            final BlockVector3 max = BlockVector3.at(
                    Math.ceil(bounds.getMaxX()), Math.ceil(bounds.getMaxY()), Math.ceil(bounds.getMaxZ()));
            final ProtectedCuboidRegion region = new ProtectedCuboidRegion(fullName, min, max);

            region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);
            region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
            region.setFlag(Flags.CHEST_ACCESS, StateFlag.State.ALLOW);
            region.setFlag(Flags.PVP, StateFlag.State.ALLOW);
            region.setFlag(Flags.MOB_DAMAGE, StateFlag.State.ALLOW);
            region.setFlag(Flags.ENDER_BUILD, StateFlag.State.DENY);
            region.setFlag(Flags.ENDERPEARL, StateFlag.State.DENY);
            region.setFlag(Flags.FIRE_SPREAD, StateFlag.State.DENY);
            region.setFlag(Flags.LAVA_FIRE, StateFlag.State.DENY);
            region.setFlag(Flags.LIGHTNING, StateFlag.State.DENY);
            region.setFlag(Flags.ENTRY, StateFlag.State.ALLOW);
            region.setFlag(Flags.EXIT, StateFlag.State.ALLOW);

            regionManager.addRegion(region);
            createdRegions.put(regionName, fullName);
            plugin.getLogger().info("Temporary WorldGuard region created: " + fullName);
        } catch (Exception | LinkageError error) {
            disableAfterFailure("create a temporary region", error);
        }
    }

    @Override
    public void removeTemporaryRegion(String regionName, World world) {
        if (!available) {
            return;
        }

        try {
            final RegionManager regionManager = regionManager(world);
            if (regionManager == null) {
                return;
            }

            final String fullName = createdRegions.getOrDefault(regionName, prefixed(regionName));
            regionManager.removeRegion(fullName);
            createdRegions.remove(regionName);
            plugin.getLogger().info("Temporary WorldGuard region removed: " + fullName);
        } catch (Exception | LinkageError error) {
            disableAfterFailure("remove a temporary region", error);
        }
    }

    @Override
    public boolean isInRegion(Location location, int radius) {
        if (!available) {
            return false;
        }

        try {
            final World world = location.getWorld();
            if (world == null) {
                return false;
            }

            final RegionManager regionManager = regionManager(world);
            if (regionManager == null) {
                return false;
            }

            final int checksPerAxis = Math.max(3, radius / 10);
            final int step = Math.max(1, radius == 0 ? 1 : radius / checksPerAxis);
            for (int dx = -radius; dx <= radius; dx += step) {
                for (int dz = -radius; dz <= radius; dz += step) {
                    final Location checkLocation = location.clone().add(dx, 0, dz);
                    for (ProtectedRegion region : regionManager.getRegions().values()) {
                        if (!region.getId().startsWith(REGION_PREFIX)
                                && region.contains(checkLocation.getBlockX(),
                                checkLocation.getBlockY(), checkLocation.getBlockZ())) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception | LinkageError error) {
            disableAfterFailure("query regions", error);
        }
        return false;
    }

    private RegionManager regionManager(World world) {
        return WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
    }

    private String prefixed(String regionName) {
        return regionName.startsWith(REGION_PREFIX) ? regionName : REGION_PREFIX + regionName;
    }

    private void disableAfterFailure(String operation, Throwable error) {
        available = false;
        plugin.getLogger().log(Level.WARNING,
                "WorldGuard failed while attempting to " + operation
                        + "; the integration has been disabled for this server session", error);
    }
}
