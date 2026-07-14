package com.olmeteors.meteorevents.hook;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.olmeteors.meteorevents.event.MeteorType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.List;
import java.util.Objects;

/** Classloader-safe Bukkit-only facade for the optional MythicMobs API. */
public final class MythicMobsHook {
    private static final String[] REQUIRED_CLASSES = {
            "io.lumine.mythic.bukkit.MythicBukkit",
            "io.lumine.mythic.api.mobs.MythicMob"
    };
    private final Access access;

    private MythicMobsHook(Access access) {
        this.access = Objects.requireNonNull(access, "access");
    }

    public static @NotNull MythicMobsHook create(@NotNull MeteorPlugin plugin,
                                                  @Nullable Plugin mythicPlugin) {
        if (mythicPlugin == null || !mythicPlugin.isEnabled()) {
            plugin.getLogger().info("MythicMobs not installed or enabled - custom mobs disabled");
            return new MythicMobsHook(DisabledAccess.INSTANCE);
        }
        try {
            final ClassLoader loader = mythicPlugin.getClass().getClassLoader();
            for (final String className : REQUIRED_CLASSES) {
                Class.forName(className, false, loader);
            }
            final MythicMobsHook hook = new MythicMobsHook(new MythicMobsAccess(plugin));
            plugin.getLogger().info("MythicMobs hook initialized successfully");
            return hook;
        } catch (ClassNotFoundException | LinkageError | RuntimeException error) {
            plugin.getLogger().log(Level.WARNING,
                    "MythicMobs API is missing or incompatible - custom mobs disabled safely", error);
            return new MythicMobsHook(DisabledAccess.INSTANCE);
        }
    }

    public boolean isAvailable() { return access.isAvailable(); }
    public @NotNull List<String> getMobIds() { return access.getMobIds(); }
    public @Nullable Entity spawnMob(String id, Location location) {
        return access.spawnMob(id, location);
    }
    public @Nullable Entity spawnBoss(String id, Location location, double multiplier) {
        return access.spawnBoss(id, location, multiplier);
    }
    public @Nullable Entity spawnMinion(Location location, MeteorType type) {
        return access.spawnMinion(location, type);
    }

    interface Access {
        boolean isAvailable();
        List<String> getMobIds();
        Entity spawnMob(String id, Location location);
        Entity spawnBoss(String id, Location location, double multiplier);
        Entity spawnMinion(Location location, MeteorType type);
    }

    private enum DisabledAccess implements Access {
        INSTANCE;
        @Override public boolean isAvailable() { return false; }
        @Override public List<String> getMobIds() { return List.of(); }
        @Override public Entity spawnMob(String id, Location location) { return null; }
        @Override public Entity spawnBoss(String id, Location location, double multiplier) { return null; }
        @Override public Entity spawnMinion(Location location, MeteorType type) { return null; }
    }
}

/**
 * Hook for MythicMobs integration providing custom boss and minion spawning
 * mechanics for meteor events.
 */
final class MythicMobsAccess implements MythicMobsHook.Access {

    private final MeteorPlugin plugin;
    private boolean available;

    MythicMobsAccess(MeteorPlugin plugin) {
        this.plugin = plugin;
        this.available = true;
    }

    public boolean isAvailable() {
        return available;
    }

    public @NotNull List<String> getMobIds() {
        if (!available) return List.of();
        try {
            return io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager()
                    .getMobNames().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        } catch (Exception | LinkageError error) {
            plugin.getLogger().log(Level.WARNING, "Could not read MythicMobs mob list", error);
            return List.of();
        }
    }

    public @Nullable Entity spawnMob(@NotNull String mobId, @NotNull Location location) {
        if (!available || mobId.isBlank()) return null;
        try {
            final var activeMob = io.lumine.mythic.bukkit.MythicBukkit.inst()
                    .getMobManager().spawnMob(mobId, location);
            if (activeMob == null) return null;
            final Entity entity = activeMob.getEntity().getBukkitEntity();
            entity.addScoreboardTag("meteor_minion");
            return entity;
        } catch (Exception | LinkageError error) {
            plugin.getLogger().log(Level.WARNING, "Could not spawn MythicMob: " + mobId, error);
            return null;
        }
    }

    /**
     * Spawns a MythicMobs boss at the given location with health scaling.
     *
     * @param bossMobId     the MythicMobs mob identifier
     * @param location      the spawn location
     * @param healthMultiplier the health multiplier based on meteor type
     * @return the spawned entity, or null on failure
     */
    public @Nullable Entity spawnBoss(@NotNull String bossMobId, @NotNull Location location,
                                       double healthMultiplier) {
        if (!available) return null;

        try {
            final var mythicBukkit = io.lumine.mythic.bukkit.MythicBukkit.inst();
            final var mobManager = mythicBukkit.getMobManager();

            final var mythicMobOptional = mobManager.getMythicMob(bossMobId);
            if (mythicMobOptional.isEmpty()) {
                plugin.getLogger().warning("MythicMob not found: " + bossMobId);
                return null;
            }

            final var mythicMob = mythicMobOptional.get();
            final var activeMob = mobManager.spawnMob(bossMobId, location);

            if (activeMob == null) {
                plugin.getLogger().warning("Failed to spawn MythicMob: " + bossMobId);
                return null;
            }

            final var entity = activeMob.getEntity().getBukkitEntity();
            if (entity instanceof LivingEntity livingEntity) {
                // Scale health based on meteor type difficulty
                final double baseHealth = livingEntity.getMaxHealth();
                final double scaledHealth = baseHealth * healthMultiplier;
                livingEntity.setMaxHealth(scaledHealth);
                livingEntity.setHealth(scaledHealth);

                // Add identifying tag
                livingEntity.addScoreboardTag("meteor_boss");

                // Apply locale-aware nameplate (Adventure Component API)
                final String bossName = plugin.getConfigManager()
                        .getMessage("boss.custom_name");
                livingEntity.customName(com.olmeteors.meteorevents.util.MessageUtil
                        .parse(bossName));
                livingEntity.setCustomNameVisible(true);
            }

            plugin.getLogger().info("MythicMobs boss spawned: " + bossMobId
                    + " at " + formatLocation(location));
            return entity;

        } catch (Exception | LinkageError error) {
            this.available = false;
            plugin.getLogger().log(Level.WARNING,
                    "Failed to spawn MythicMobs boss; integration disabled for this session", error);
            return null;
        }
    }

    /**
     * Spawns a minion mob at the given location.
     *
     * @param location  the spawn location
     * @param meteorType the meteor type for difficulty scaling
     * @return the spawned entity, or null on failure
     */
    public @Nullable Entity spawnMinion(@NotNull Location location, @NotNull MeteorType meteorType) {
        if (!available) return null;

        try {
            final var mythicBukkit = io.lumine.mythic.bukkit.MythicBukkit.inst();
            final var mobManager = mythicBukkit.getMobManager();

            // Determine minion type based on meteor type difficulty
            final String minionMobId = getMinionMobForType(meteorType);
            if (minionMobId == null) {
                return null;
            }

            final var activeMob = mobManager.spawnMob(minionMobId, location);

            if (activeMob == null) {
                return null;
            }

            final var entity = activeMob.getEntity().getBukkitEntity();
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.addScoreboardTag("meteor_minion");
            }

            return entity;

        } catch (Exception | LinkageError error) {
            this.available = false;
            plugin.getLogger().log(Level.FINE,
                    "Failed to spawn MythicMobs minion; integration disabled for this session", error);
            return null;
        }
    }

    /**
     * Gets the appropriate minion mob ID for a given meteor type difficulty.
     * Falls back to standard mobs if no MythicMobs minions are configured.
     */
    private @Nullable String getMinionMobForType(@NotNull MeteorType type) {
        return switch (type.difficulty()) {
            case EASY -> "MeteorSpider";
            case NORMAL -> "MeteorZombie";
            case HARD -> "MeteorSkeleton";
            case EPIC -> "MeteorBlaze";
            case LEGENDARY -> "MeteorWitherSkeleton";
        };
    }

    private String formatLocation(Location loc) {
        return String.format("%.0f, %.0f, %.0f",
                loc.getX(), loc.getY(), loc.getZ());
    }
}
