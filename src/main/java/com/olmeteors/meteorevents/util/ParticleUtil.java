package com.olmeteors.meteorevents.util;

import com.olmeteors.meteorevents.MeteorPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for creating particle effects and visual displays.
 * Uses async scheduling to prevent lag from heavy particle operations.
 */
public final class ParticleUtil {

    private final MeteorPlugin plugin;

    public ParticleUtil(MeteorPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawns a colored dust particle at the given location.
     *
     * @param world   the world
     * @param x       x coordinate
     * @param y       y coordinate
     * @param z       z coordinate
     * @param red     red component (0-1)
     * @param green   green component (0-1)
     * @param blue    blue component (0-1)
     * @param size    particle size
     */
    public void spawnDustParticle(@NotNull World world, double x, double y, double z,
                                   float red, float green, float blue, float size) {
        world.spawnParticle(
                Particle.DUST,
                x, y, z,
                1, 0, 0, 0, 0,
                new Particle.DustOptions(
                        org.bukkit.Color.fromRGB(
                                (int) (red * 255),
                                (int) (green * 255),
                                (int) (blue * 255)
                        ),
                        size
                )
        );
    }

    /**
     * Draws a 3D box outline using colored dust particles around a region.
     *
     * @param world   the world
     * @param minX    minimum X
     * @param minY    minimum Y
     * @param minZ    minimum Z
     * @param maxX    maximum X
     * @param maxY    maximum Y
     * @param maxZ    maximum Z
     * @param color   the color to use
     * @param density particles per block (recommended: 1.0)
     */
    public void drawBoxOutline(@NotNull World world,
                                double minX, double minY, double minZ,
                                double maxX, double maxY, double maxZ,
                                @NotNull Particle.DustOptions color, double density) {
        final double step = 1.0 / Math.max(1.0, density);

        // Draw 12 edges of the box
        for (double t = 0; t <= 1.0; t += step) {
            final double x = minX + (maxX - minX) * t;
            final double y = minY + (maxY - minY) * t;
            final double z = minZ + (maxZ - minZ) * t;

            // X-axis edges (bottom)
            world.spawnParticle(Particle.DUST, x, minY, minZ, 1, 0, 0, 0, 0, color);
            world.spawnParticle(Particle.DUST, x, minY, maxZ, 1, 0, 0, 0, 0, color);
            // X-axis edges (top)
            world.spawnParticle(Particle.DUST, x, maxY, minZ, 1, 0, 0, 0, 0, color);
            world.spawnParticle(Particle.DUST, x, maxY, maxZ, 1, 0, 0, 0, 0, color);

            // Z-axis edges (bottom)
            world.spawnParticle(Particle.DUST, minX, minY, z, 1, 0, 0, 0, 0, color);
            world.spawnParticle(Particle.DUST, maxX, minY, z, 1, 0, 0, 0, 0, color);
            // Z-axis edges (top)
            world.spawnParticle(Particle.DUST, minX, maxY, z, 1, 0, 0, 0, 0, color);
            world.spawnParticle(Particle.DUST, maxX, maxY, z, 1, 0, 0, 0, 0, color);

            // Y-axis edges
            world.spawnParticle(Particle.DUST, minX, y, minZ, 1, 0, 0, 0, 0, color);
            world.spawnParticle(Particle.DUST, minX, y, maxZ, 1, 0, 0, 0, 0, color);
            world.spawnParticle(Particle.DUST, maxX, y, minZ, 1, 0, 0, 0, 0, color);
            world.spawnParticle(Particle.DUST, maxX, y, maxZ, 1, 0, 0, 0, 0, color);
        }
    }

    /** Draws a setup selection only for its owner; safe from the player's Folia scheduler. */
    public void drawBoxOutline(@NotNull Player player,
                               double minX, double minY, double minZ,
                               double maxX, double maxY, double maxZ,
                               @NotNull Particle.DustOptions color, double density) {
        final double step = 1.0 / Math.max(1.0, density);
        for (double t = 0; t <= 1.0; t += step) {
            final double x = minX + (maxX - minX) * t;
            final double y = minY + (maxY - minY) * t;
            final double z = minZ + (maxZ - minZ) * t;
            player.spawnParticle(Particle.DUST, x, minY, minZ, 1, 0, 0, 0, 0, color);
            player.spawnParticle(Particle.DUST, x, minY, maxZ, 1, 0, 0, 0, 0, color);
            player.spawnParticle(Particle.DUST, x, maxY, minZ, 1, 0, 0, 0, 0, color);
            player.spawnParticle(Particle.DUST, x, maxY, maxZ, 1, 0, 0, 0, 0, color);
            player.spawnParticle(Particle.DUST, minX, minY, z, 1, 0, 0, 0, 0, color);
            player.spawnParticle(Particle.DUST, maxX, minY, z, 1, 0, 0, 0, 0, color);
            player.spawnParticle(Particle.DUST, minX, maxY, z, 1, 0, 0, 0, 0, color);
            player.spawnParticle(Particle.DUST, maxX, maxY, z, 1, 0, 0, 0, 0, color);
            player.spawnParticle(Particle.DUST, minX, y, minZ, 1, 0, 0, 0, 0, color);
            player.spawnParticle(Particle.DUST, minX, y, maxZ, 1, 0, 0, 0, 0, color);
            player.spawnParticle(Particle.DUST, maxX, y, minZ, 1, 0, 0, 0, 0, color);
            player.spawnParticle(Particle.DUST, maxX, y, maxZ, 1, 0, 0, 0, 0, color);
        }
    }

    /**
     * Creates a vertical beam of particles at the given location.
     *
     * @param world  the world
     * @param x      x coordinate
     * @param z      z coordinate
     * @param minY   bottom Y
     * @param maxY   top Y
     * @param color  the particle color
     */
    public void spawnVerticalBeam(@NotNull World world, double x, double z,
                                   double minY, double maxY,
                                   @NotNull Particle.DustOptions color) {
        final double step = 0.5;
        for (double y = minY; y <= maxY; y += step) {
            world.spawnParticle(Particle.DUST, x, y, z, 1, 0.3, 0.1, 0.3, 0, color);
        }
    }

    /** Sends a setup marker beam only to its owner. */
    public void spawnVerticalBeam(@NotNull Player player, double x, double z,
                                  double minY, double maxY,
                                  @NotNull Particle.DustOptions color) {
        for (double y = minY; y <= maxY; y += 0.5) {
            player.spawnParticle(Particle.DUST, x, y, z, 1, 0.3, 0.1, 0.3, 0, color);
        }
    }

    /**
     * Creates a circle of particles at the given Y level.
     *
     * @param world  the world
     * @param center the center location
     * @param radius the circle radius
     * @param y      the Y level
     * @param color  the particle color
     * @param count  number of particles in the circle
     */
    public void spawnParticleCircle(@NotNull World world, @NotNull Location center,
                                     double radius, double y,
                                     @NotNull Particle.DustOptions color, int count) {
        for (int i = 0; i < count; i++) {
            final double angle = 2 * Math.PI * i / count;
            final double x = center.getX() + radius * Math.cos(angle);
            final double z = center.getZ() + radius * Math.sin(angle);
            world.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, color);
        }
    }

    /**
     * Creates a flame ring effect expanding outward.
     *
     * @param world   the world
     * @param center  the center location
     * @param radius  the radius
     */
    public void spawnFlameRing(@NotNull World world, @NotNull Location center, double radius) {
        final int count = (int) (radius * 8);
        for (int i = 0; i < count; i++) {
            final double angle = 2 * Math.PI * i / count;
            final double x = center.getX() + radius * Math.cos(angle);
            final double z = center.getZ() + radius * Math.sin(angle);
            world.spawnParticle(Particle.FLAME, x, center.getY() + 1, z, 1, 0, 0.5, 0, 0.02);
        }
    }

    /**
     * Creates an explosion-like particle burst effect.
     *
     * @param world  the world
     * @param center the center location
     * @param power  the power/intensity of the burst
     */
    public void spawnExplosionEffect(@NotNull World world, @NotNull Location center, float power) {
        world.createExplosion(center, 0.0f, false, false);

        // Additional particle effects
        world.spawnParticle(Particle.EXPLOSION, center, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0);

        final int count = (int) (power * 20);
        for (int i = 0; i < count; i++) {
            final double angle1 = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
            final double angle2 = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
            final double radius = ThreadLocalRandom.current().nextDouble(power * 2);

            final double x = center.getX() + radius * Math.sin(angle1) * Math.cos(angle2);
            final double y = center.getY() + radius * Math.sin(angle1) * Math.sin(angle2);
            final double z = center.getZ() + radius * Math.cos(angle1);

            world.spawnParticle(Particle.FLAME, x, y, z, 1, 0, 0, 0, 0.05);
            if (i % 3 == 0) {
                world.spawnParticle(Particle.SMOKE, x, y, z, 1, 0, 0, 0, 0.05);
            }
        }
    }

    /**
     * Creates a shockwave ring that expands outward from the center.
     *
     * @param world      the world
     * @param center     the center location
     * @param startRadius the initial radius
     * @param endRadius  the final radius
     * @param yOffset    Y offset from center
     */
    public void spawnShockwaveRing(@NotNull World world, @NotNull Location center,
                                    double startRadius, double endRadius, double yOffset) {
        for (double radius = startRadius; radius <= endRadius; radius += 0.5) {
            final int count = (int) (radius * 6);
            for (int i = 0; i < count; i++) {
                final double angle = 2 * Math.PI * i / count;
                final double x = center.getX() + radius * Math.cos(angle);
                final double z = center.getZ() + radius * Math.sin(angle);
                world.spawnParticle(Particle.SONIC_BOOM, x, center.getY() + yOffset, z, 1, 0, 0, 0, 0);
            }
        }
    }

    /**
     * Shows alert particles around a player's screen edges.
     *
     * @param player the player
     */
    public void showScreenAlert(@NotNull Player player) {
        final Location loc = player.getLocation();
        final World world = loc.getWorld();

        // Spawn a ring of warning particles around the player
        for (int i = 0; i < 16; i++) {
            final double angle = 2 * Math.PI * i / 16;
            final double x = loc.getX() + 3 * Math.cos(angle);
            final double z = loc.getZ() + 3 * Math.sin(angle);
            world.spawnParticle(Particle.WAX_OFF, x, loc.getY() + 1, z, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Creates a dramatic lightning strike effect at the given location.
     *
     * @param world  the world
     * @param center the center location
     */
    public void strikeLightning(@NotNull World world, @NotNull Location center) {
        world.strikeLightningEffect(center);
        world.spawnParticle(Particle.SONIC_BOOM, center, 3, 1, 1, 1, 0);
        world.spawnParticle(Particle.GLOW, center, 30, 1, 1, 1, 0.5);
    }
}
