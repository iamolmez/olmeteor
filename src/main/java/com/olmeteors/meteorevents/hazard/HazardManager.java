package com.olmeteors.meteorevents.hazard;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.olmeteors.meteorevents.config.ConfigManager;
import com.olmeteors.meteorevents.event.ActiveMeteorEvent;
import com.olmeteors.meteorevents.event.EventPhase;
import com.olmeteors.meteorevents.hook.WGHook;
import com.olmeteors.meteorevents.scheduler.FoliaScheduler;
import com.olmeteors.meteorevents.util.MessageUtil;
import com.olmeteors.meteorevents.util.ParticleUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages environmental hazards within active meteor event zones including:
 * - Radiation damage (Wither/poison effect)
 * - Wind Charge knockback waves
 * - EMP field (Elytra disable, Ender Pearl block)
 */
public final class HazardManager implements Listener {

    private final MeteorPlugin plugin;
    private final ConfigManager configManager;
    private final WGHook wgHook;
    private final ParticleUtil particleUtil;

    private final Map<String, ActiveMeteorEvent> hazardEvents;

    private static final int RADIATION_INTERVAL_TICKS = 20; // Every second

    // ── Config-based message helper ────────────────────────────
    private String msg(String path, String... placeholders) {
        return plugin.getConfigManager().getMessage(path, placeholders);
    }
    private static final int FIELD_CHECK_INTERVAL_TICKS = 10; // Every 0.5 seconds

    public HazardManager(MeteorPlugin plugin, WGHook wgHook) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.wgHook = wgHook;
        this.particleUtil = new ParticleUtil(plugin);
        this.hazardEvents = new ConcurrentHashMap<>();
    }

    /**
     * Starts all hazard effects for a meteor event.
     *
     * @param event the active meteor event
     * @return a ScheduledTask handle for the hazard loop
     */
    public @Nullable FoliaScheduler.ScheduledTask startHazards(@NotNull ActiveMeteorEvent event) {
        hazardEvents.put(event.eventId(), event);

        // Start the main hazard loop
        return plugin.getFoliaScheduler().runRepeatingGlobal(
                () -> tickHazards(event),
                20L, // Start after 1 second
                20L  // Tick every second
        );
    }

    /**
     * Stops all hazards for a specific event.
     *
     * @param eventId the event ID
     */
    public void stopHazards(@NotNull String eventId) {
        hazardEvents.remove(eventId);
    }

    /**
     * Disables all hazard effects (called on plugin disable).
     */
    public void disableAll() {
        hazardEvents.clear();
    }

    /**
     * Main hazard tick method - runs every second for each active event.
     */
    private void tickHazards(@NotNull ActiveMeteorEvent event) {
        if (event.phase() != EventPhase.ACTIVE || !hazardEvents.containsKey(event.eventId())) {
            return;
        }
        final boolean windWave = ThreadLocalRandom.current().nextInt(
                Math.max(1, configManager.getWindChargeInterval() / 20)) == 0;
        for (final Player player : plugin.getServer().getOnlinePlayers()) {
            plugin.getFoliaScheduler().runForEntity(player,
                    () -> tickPlayerHazards(player, event, windWave));
        }
    }

    private void tickPlayerHazards(@NotNull Player player, @NotNull ActiveMeteorEvent event,
                                   boolean windWave) {
        if (!hazardEvents.containsKey(event.eventId()) || !player.isValid() || player.isDead()
                || !player.getWorld().equals(event.world())) return;
        final Location playerLocation = player.getLocation();
        final int radius = event.meteorType().impactRadius();
        if (playerLocation.distanceSquared(event.center()) > (double) radius * radius) return;

        if (!player.hasPermission("olmeteor.bypass.hazards")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0,
                    true, true, true));
            final int damage = configManager.getRadiationDamagePerSecond();
            if (damage > 1) player.damage(damage);
            player.spawnParticle(Particle.DUST, playerLocation.clone().add(0, 1, 0),
                    2, 0.8, 1.0, 0.8, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(76, 255, 25), 0.8f));
            MessageUtil.sendActionBar(player, msg("hazard.radiation.actionbar",
                    "health", String.format("%.1f", player.getHealth())));
        }

        if (windWave) {
            final Vector direction = playerLocation.toVector().subtract(event.center().toVector());
            if (direction.lengthSquared() > 0.0001) direction.normalize();
            final double multiplier = configManager.getWindChargeKnockbackMultiplier();
            player.setVelocity(direction.multiply(1.5 * multiplier).setY(0.5 * multiplier));
            player.spawnParticle(Particle.SONIC_BOOM, playerLocation.clone().add(0, 1, 0),
                    1, 0, 0, 0, 0);
            MessageUtil.sendActionBar(player, msg("hazard.wind.actionbar"));
        }

        if (configManager.isElytraDisabled() && player.isGliding()) {
            player.setGliding(false);
            MessageUtil.sendMessage(player, msg("hazard.emp.elytra"));
        }
        if (configManager.isElytraDisabled() || configManager.isEnderPearlDisabled()) {
            player.spawnParticle(Particle.DUST, playerLocation.clone().add(0, 1.5, 0),
                    3, 1.5, 1.5, 1.5, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(51, 128, 255), 1.5f));
        }
    }

    /**
     * Applies radiation (Wither) damage to players within the hazard zone.
     */
    private void applyRadiationDamage(@NotNull World world, @NotNull Location center, int radius) {
        final int damage = configManager.getRadiationDamagePerSecond();

        for (final Player player : world.getNearbyPlayers(center, radius)) {
            if (!player.isValid() || player.isDead()) continue;
            if (player.hasPermission("olmeteor.bypass.hazards")) continue;

            // Apply Wither effect (visual and damage)
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.WITHER,
                    40, // 2 seconds
                    0,
                    true,
                    true,
                    true
            ));

            // Additional direct damage if Wither effect isn't enough
            if (damage > 1) {
                player.damage(damage);
            }

            // Visual radiation particles
            final var playerLoc = player.getLocation();
            particleUtil.spawnDustParticle(world,
                    playerLoc.getX() + ThreadLocalRandom.current().nextDouble(-1, 1),
                    playerLoc.getY() + ThreadLocalRandom.current().nextDouble(0, 2),
                    playerLoc.getZ() + ThreadLocalRandom.current().nextDouble(-1, 1),
                    0.3f, 1.0f, 0.1f, 0.8f
            );

            // Action bar warning (every second)
            if (player.getTicksLived() % 20 == 0) {
                MessageUtil.sendActionBar(player,
                        msg("hazard.radiation.actionbar",
                                "health", String.format("%.1f", player.getHealth())));
            }
        }
    }

    /**
     * Triggers a wind charge wave that pushes players away from the center.
     * Creates visual wind particle effects.
     */
    private void triggerWindChargeWave(@NotNull World world, @NotNull Location center, int radius) {
        final double knockbackMultiplier = configManager.getWindChargeKnockbackMultiplier();

        for (final Player player : world.getNearbyPlayers(center, radius)) {
            if (!player.isValid() || player.isDead()) continue;

            // Calculate knockback direction (away from center)
            final Vector direction = player.getLocation().toVector()
                    .subtract(center.toVector()).normalize();

            // Apply vertical and horizontal knockback
            final Vector velocity = direction.multiply(1.5 * knockbackMultiplier);
            velocity.setY(0.5 * knockbackMultiplier);
            player.setVelocity(velocity);

            // Visual wind burst effect at player location
            final var playerLoc = player.getLocation();
            particleUtil.spawnExplosionEffect(world, playerLoc, 1.0f);

            MessageUtil.sendActionBar(player,
                    msg("hazard.wind.actionbar"));
        }

        // Visual ring expanding from center
        particleUtil.spawnShockwaveRing(world, center, 1, radius, 1.0);
    }

    /**
     * Checks and enforces EMP field effects (Elytra disable, Ender Pearl block).
     */
    private void checkEMPEffects(@NotNull World world, @NotNull Location center, int radius) {
        if (!configManager.isElytraDisabled() && !configManager.isEnderPearlDisabled()) return;

        for (final Player player : world.getNearbyPlayers(center, radius)) {
            if (!player.isValid() || player.isDead()) continue;

            // Disable Elytra flight
            if (configManager.isElytraDisabled() && player.isGliding()) {
                player.setGliding(false);
                MessageUtil.sendMessage(player,
                        msg("hazard.emp.elytra"));
            }

            // Visual EMP particles around player
            final var playerLoc = player.getLocation();
            for (int i = 0; i < 3; i++) {
                particleUtil.spawnDustParticle(world,
                        playerLoc.getX() + ThreadLocalRandom.current().nextDouble(-2, 2),
                        playerLoc.getY() + ThreadLocalRandom.current().nextDouble(0, 3),
                        playerLoc.getZ() + ThreadLocalRandom.current().nextDouble(-2, 2),
                        0.2f, 0.5f, 1.0f, 1.5f
                );
            }
        }
    }

    // ---- Event Handlers ----

    @EventHandler
    public void onElytraToggle(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!configManager.isElytraDisabled()) return;

        // Check if player is in an active hazard zone
        for (final var hazardEvent : hazardEvents.values()) {
            if (!hazardEvent.world().equals(player.getWorld())) continue;
            if (!hazardEvent.phase().hazardsEnabled()) continue;

            final double distance = player.getLocation().distance(hazardEvent.center());
            if (distance <= hazardEvent.meteorType().impactRadius()) {
                // Block Elytra usage within zone
                event.setCancelled(true);
                MessageUtil.sendMessage(player,
                        msg("hazard.emp.elytra_block"));
                return;
            }
        }
    }

    @EventHandler
    public void onEnderPearl(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        if (!configManager.isEnderPearlDisabled()) return;

        final Player player = event.getPlayer();

        // Check if player is in an active hazard zone
        for (final var hazardEvent : hazardEvents.values()) {
            if (!hazardEvent.world().equals(player.getWorld())) continue;
            if (!hazardEvent.phase().hazardsEnabled()) continue;

            final Location to = event.getTo();
            if (to == null) continue;

            final double distanceTo = to.distance(hazardEvent.center());
            if (distanceTo <= hazardEvent.meteorType().impactRadius()) {
                // Block Ender Pearl into the zone
                event.setCancelled(true);
                MessageUtil.sendMessage(player,
                        msg("hazard.emp.pearl_block"));
                return;
            }
        }
    }

    /**
     * Checks if a location is within an active hazard zone.
     *
     * @param location the location to check
     * @return true if hazards are active at the location
     */
    public boolean isInHazardZone(@NotNull Location location) {
        for (final var hazardEvent : hazardEvents.values()) {
            if (!hazardEvent.world().equals(location.getWorld())) continue;
            if (!hazardEvent.phase().hazardsEnabled()) continue;

            final double distance = location.distance(hazardEvent.center());
            if (distance <= hazardEvent.meteorType().impactRadius()) {
                return true;
            }
        }
        return false;
    }
}
