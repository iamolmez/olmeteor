package com.olmeteors.meteorevents.event;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.olmeteors.meteorevents.config.ConfigManager;
import com.olmeteors.meteorevents.display.DisplayEntityManager;
import com.olmeteors.meteorevents.hazard.HazardManager;
import com.olmeteors.meteorevents.hook.MythicMobsHook;
import com.olmeteors.meteorevents.hook.WGHook;
import com.olmeteors.meteorevents.location.LocationFinder;
import com.olmeteors.meteorevents.loot.VaultManager;
import com.olmeteors.meteorevents.rollback.RollbackSystem;
import com.olmeteors.meteorevents.scheduler.FoliaScheduler;
import com.olmeteors.meteorevents.schematic.SchematicManager;
import com.olmeteors.meteorevents.util.MessageUtil;
import com.olmeteors.meteorevents.util.ParticleUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Manages the complete lifecycle of meteor events including scheduling,
 * pre-impact effects, impact, active phase, and rollback.
 */
public final class MeteorEventManager implements Listener {

    private final MeteorPlugin plugin;
    private final ConfigManager configManager;
    private final FoliaScheduler scheduler;
    private final LocationFinder locationFinder;
    private final SchematicManager schematicManager;
    private final RollbackSystem rollbackSystem;
    private final DisplayEntityManager displayEntityManager;
    private final HazardManager hazardManager;
    private final VaultManager vaultManager;
    private final WGHook wgHook;
    private final MythicMobsHook mythicMobsHook;
    private final ParticleUtil particleUtil;
    private final NamespacedKey meteorEventKey;
    private final MeteorHistoryStore historyStore;
    private final Set<String> automaticEventIds = ConcurrentHashMap.newKeySet();

    private final ConcurrentHashMap<String, ActiveMeteorEvent> activeEvents;
    private final Map<String, Set<UUID>> eventMobIds = new ConcurrentHashMap<>();
    private final Set<String> lootClaimedEvents = ConcurrentHashMap.newKeySet();
    private final Set<String> completionScheduled = ConcurrentHashMap.newKeySet();
    private final Set<String> combatCompletedEvents = ConcurrentHashMap.newKeySet();
    private final Set<String> rollbackStartedEvents = ConcurrentHashMap.newKeySet();
    private final Set<String> cancelRequestedEvents = ConcurrentHashMap.newKeySet();
    private final Set<String> impactPastePending = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingWaves = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<UUID>> eventParticipants = new ConcurrentHashMap<>();
    private final Map<String, FoliaScheduler.ScheduledTask> trackingTasks = new ConcurrentHashMap<>();
    private final Map<String, Map<UUID, net.kyori.adventure.bossbar.BossBar>> trackingBars = new ConcurrentHashMap<>();
    private volatile FoliaScheduler.ScheduledTask automaticTask;
    private volatile long nextAutomaticAtMillis;

    private static volatile int eventIdCounter = 0;

    // ── Config-based message helpers ────────────────────────────
    private void tell(org.bukkit.command.CommandSender sender, String path, String... placeholders) {
        configManager.sendMessage(sender, path, placeholders);
    }

    private void tellSafely(CommandSender sender, String path, String... placeholders) {
        if (sender instanceof Player player) {
            scheduler.runForEntity(player, () -> tell(player, path, placeholders));
        } else {
            scheduler.callGlobal(() -> tell(sender, path, placeholders));
        }
    }

    private void tellRawSafely(CommandSender sender, String message) {
        if (sender instanceof Player player) {
            scheduler.runForEntity(player, () -> MessageUtil.sendMessage(player, message));
        } else {
            scheduler.callGlobal(() -> MessageUtil.sendMessage(sender, message));
        }
    }

    private void broadcastMsg(String path, String... placeholders) {
        final String msg = configManager.getMessage(path, placeholders);
        scheduler.callGlobal(() -> MessageUtil.broadcast(msg));
    }

    public MeteorEventManager(MeteorPlugin plugin, ConfigManager configManager,
                              FoliaScheduler scheduler, LocationFinder locationFinder,
                              SchematicManager schematicManager, RollbackSystem rollbackSystem,
                              DisplayEntityManager displayEntityManager, HazardManager hazardManager,
                              VaultManager vaultManager, WGHook wgHook,
                              MythicMobsHook mythicMobsHook) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.scheduler = scheduler;
        this.locationFinder = locationFinder;
        this.schematicManager = schematicManager;
        this.rollbackSystem = rollbackSystem;
        this.displayEntityManager = displayEntityManager;
        this.hazardManager = hazardManager;
        this.vaultManager = vaultManager;
        this.wgHook = wgHook;
        this.mythicMobsHook = mythicMobsHook;
        this.particleUtil = new ParticleUtil(plugin);
        this.meteorEventKey = new NamespacedKey(plugin, "meteor_event_id");
        this.historyStore = new MeteorHistoryStore(plugin);
        this.activeEvents = new ConcurrentHashMap<>();
    }

    /**
     * Starts a new meteor event of the given type.
     *
     * @param type      the meteor type
     * @param worldName optional world name (uses default if null)
     * @param sender    the command sender for feedback
     */
    public @NotNull CompletableFuture<MeteorStartResult> startEvent(
            @NotNull MeteorType type, @Nullable String worldName, @NotNull CommandSender sender) {
        final CompletableFuture<MeteorStartResult> result = new CompletableFuture<>();
        if (worldName == null && sender instanceof Player player) {
            scheduler.runForEntity(player,
                    () -> searchAndStart(type, player.getWorld(), sender, result));
            return result;
        }
        scheduler.callGlobal(() -> {
            try {
                final World world = resolveWorld(worldName, sender);
                if (world == null) {
                    result.complete(MeteorStartResult.WORLD_NOT_FOUND);
                    return;
                }
                searchAndStart(type, world, sender, result);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error starting meteor event", e);
                tellSafely(sender, "command.start.error", "error", e.getMessage());
                result.complete(MeteorStartResult.ERROR);
            }
        });
        return result;
    }

    private void searchAndStart(MeteorType type, World world, CommandSender sender,
                                CompletableFuture<MeteorStartResult> result) {
        locationFinder.findSuitableLocationAsync(world, type).whenComplete((location, error) -> {
            if (error != null) {
                plugin.getLogger().log(Level.SEVERE, "Error finding meteor location", error);
                tellSafely(sender, "command.start.error", "error", error.getMessage());
                result.complete(MeteorStartResult.ERROR);
                return;
            }
            if (location == null || location.isEmpty()) {
                tellSafely(sender, "command.start.location_fail");
                result.complete(MeteorStartResult.LOCATION_NOT_FOUND);
                return;
            }
            final Location center = location.get();
            scheduler.callAtLocation(center, () -> result.complete(createAndStartEvent(
                    type, center, sender, configManager.getFallMode(type))));
        });
    }

    /** Starts an event exactly at the supplied location without the warning countdown. */
    public @NotNull CompletableFuture<MeteorStartResult> startEventAt(
                             @NotNull MeteorType type, @NotNull Location location,
                             @NotNull CommandSender sender) {
        return startEventAt(type, location, sender, MeteorFallMode.INSTANT);
    }

    public @NotNull CompletableFuture<MeteorStartResult> startEventAt(
                             @NotNull MeteorType type, @NotNull Location location,
                             @NotNull CommandSender sender, @NotNull MeteorFallMode mode) {
        final CompletableFuture<MeteorStartResult> result = new CompletableFuture<>();
        if (location.getWorld() == null) {
            tellSafely(sender, "command.start.world_not_found", "world", "?");
            result.complete(MeteorStartResult.WORLD_NOT_FOUND);
            return result;
        }
        final Location center = location.toCenterLocation();
        scheduler.callAtLocation(center,
                () -> result.complete(createAndStartEvent(type, center, sender, mode)));
        return result;
    }

    private MeteorStartResult createAndStartEvent(MeteorType type, Location center,
                                                   CommandSender sender, MeteorFallMode fallMode) {
        if (!plugin.getFAWEHook().isAvailable()) {
            tellSafely(sender, "command.start.fawe_required");
            return MeteorStartResult.ERROR;
        }
        final var preStart = new com.olmeteors.meteorevents.api.event.MeteorPreStartEvent(type, center);
        plugin.getServer().getPluginManager().callEvent(preStart);
        if (preStart.isCancelled()) {
            tellRawSafely(sender, "&cMeteor başlangıcı başka bir eklenti tarafından iptal edildi.");
            return MeteorStartResult.CANCELLED;
        }
        final ActiveMeteorEvent event = createEvent(type, center);
        activeEvents.put(event.eventId(), event);
        tellSafely(sender, "command.start.scheduled",
                "eventId", event.eventId(), "location", formatLocation(center));
        if (fallMode == MeteorFallMode.INSTANT) {
            beginImpactPhase(event);
        } else {
            beginAnimatedFall(event, fallMode);
        }
        return MeteorStartResult.STARTED;
    }

    private @NotNull ActiveMeteorEvent createEvent(@NotNull MeteorType type,
                                                    @NotNull Location center) {
        final String eventId = generateEventId(type);
        final int radius = configManager.getTypeConfig(type).impactRadius();
        final var region = org.bukkit.util.BoundingBox.of(
                center.clone().add(-radius, -10, -radius),
                center.clone().add(radius, 20, radius));
        return ActiveMeteorEvent.createNew(
                eventId, type, center, region, chunkRadius(),
                configManager.getSchematicForType(type),
                configManager.getTypeConfig(type).bossMythicMob(),
                configuredMobSpawnPoints(type, center, radius));
    }

    private void beginAnimatedFall(@NotNull ActiveMeteorEvent event,
                                   @NotNull MeteorFallMode mode) {
        final ActiveMeteorEvent updated = event.withPhase(EventPhase.PRE_IMPACT);
        activeEvents.put(event.eventId(), updated);
        final int seconds = configManager.getFallDurationSeconds(event.meteorType(), mode);
        final long durationTicks = seconds * 20L;
        final Location center = event.center();
        scheduler.forceLoadChunks(event.world(), center.getBlockX() >> 4,
                center.getBlockZ() >> 4, configManager.getChunkForceLoadRadius());
        broadcastMsg("event.broadcast.incoming",
                "color", configManager.getMeteorTypeColor(event.meteorType()),
                "name", configManager.getMeteorTypeName(event.meteorType()),
                "seconds", String.valueOf(seconds),
                "world", center.getWorld().getName(),
                "location", formatLocation(center),
                "x", String.valueOf(center.getBlockX()),
                "y", String.valueOf(center.getBlockY()),
                "z", String.valueOf(center.getBlockZ()));
        scheduler.runAtLocation(center, () -> displayEntityManager.spawnFallingMeteor(
                event.eventId(), center, durationTicks, configManager.getFallHeight(mode),
                mode == MeteorFallMode.SLOW));
        final FoliaScheduler.ScheduledTask particleTask = scheduler.runRepeatingAtLocation(
                center, () -> runPreImpactParticles(updated), 0L, mode == MeteorFallMode.SLOW ? 8L : 12L);
        final FoliaScheduler.ScheduledTask phaseTask = scheduler.runLaterAtLocation(center,
                () -> {
                    final ActiveMeteorEvent latest = activeEvents.get(event.eventId());
                    if (latest != null && latest.phase() == EventPhase.PRE_IMPACT) {
                        beginImpactPhase(latest);
                    }
                }, durationTicks);
        activeEvents.put(event.eventId(), updated.withParticleTask(particleTask).withPhaseTask(phaseTask));
    }

    /**
     * Begins the pre-impact phase with visual and audio warnings.
     */
    private void beginPreImpactPhase(@NotNull ActiveMeteorEvent event) {
        final ActiveMeteorEvent updatedEvent = event.withPhase(EventPhase.PRE_IMPACT);
        activeEvents.put(event.eventId(), updatedEvent);

        final Location center = updatedEvent.center();
        final World world = center.getWorld();

        // Broadcast warning
        final long preImpactSecs = configManager.getTypeConfig(updatedEvent.meteorType())
                .preImpactDurationSeconds();
        broadcastMsg("event.broadcast.incoming",
                "color", configManager.getMeteorTypeColor(updatedEvent.meteorType()),
                "name", configManager.getMeteorTypeName(updatedEvent.meteorType()),
                "seconds", String.valueOf(preImpactSecs),
                "world", world.getName(),
                "location", formatLocation(center),
                "x", String.valueOf(center.getBlockX()),
                "y", String.valueOf(center.getBlockY()),
                "z", String.valueOf(center.getBlockZ()));

        // Force-load chunks for the area
        scheduler.forceLoadChunks(world, center.getBlockX() >> 4, center.getBlockZ() >> 4,
                configManager.getChunkForceLoadRadius());

        // Start the periodic particle effect task
        final FoliaScheduler.ScheduledTask particleTask = scheduler.runRepeatingAtLocation(
                center, () -> runPreImpactParticles(updatedEvent), 0L, 10L);

        // Schedule impact after pre-impact duration
        final long preImpactTicks = configManager.getTypeConfig(updatedEvent.meteorType())
                .preImpactDurationTicks();
        final FoliaScheduler.ScheduledTask phaseTask = scheduler.runLaterAtLocation(center,
                () -> {
                    final ActiveMeteorEvent latest = activeEvents.get(event.eventId());
                    if (latest != null && latest.phase() == EventPhase.PRE_IMPACT) {
                        beginImpactPhase(latest);
                    }
                },
                preImpactTicks
        );

        activeEvents.put(event.eventId(), updatedEvent
                .withParticleTask(particleTask)
                .withPhaseTask(phaseTask)
        );
    }

    /**
     * Runs visual effects during the pre-impact phase.
     */
    private void runPreImpactParticles(@NotNull ActiveMeteorEvent event) {
        final Location center = event.center();
        final World world = center.getWorld();

        if (world == null) return;

        // Sky flash effects
        final int radius = configManager.getTypeConfig(event.meteorType()).impactRadius();
        final double skyY = center.getY() + 50 + ThreadLocalRandom.current().nextDouble(30);

        // Create firework-like effects in the sky
        for (int i = 0; i < 3; i++) {
            final double offsetX = ThreadLocalRandom.current().nextDouble(-radius, radius);
            final double offsetZ = ThreadLocalRandom.current().nextDouble(-radius, radius);
            final Location skyLoc = center.clone().add(offsetX, 50 + ThreadLocalRandom.current().nextDouble(20), offsetZ);
            scheduler.runAtLocation(skyLoc,
                    () -> particleUtil.spawnExplosionEffect(world, skyLoc, 1.5f));
        }

        // Ground tremor indicators
        world.spawnParticle(org.bukkit.Particle.FLAME, center.clone().add(0, 1, 0),
                Math.max(8, radius * 2), radius * 0.4, 0.5, radius * 0.4, 0.02);
        particleUtil.spawnDustParticle(world, center.getX(), center.getY(), center.getZ(),
                1.0f, 0.5f, 0.0f, 2.0f);

        // Sonic boom warning sounds at random positions
        if (ThreadLocalRandom.current().nextInt(4) == 0) {
            final double sx = center.getX() + ThreadLocalRandom.current().nextDouble(-radius * 2, radius * 2);
            final double sz = center.getZ() + ThreadLocalRandom.current().nextDouble(-radius * 2, radius * 2);
            final Location sonic = new Location(world, sx, center.getY() + 3, sz);
            scheduler.runAtLocation(sonic, () -> world.spawnParticle(
                    org.bukkit.Particle.SONIC_BOOM, sonic, 1, 0, 0, 0, 0));
        }

        // Send screen shake to nearby players
        for (final Player player : plugin.getServer().getOnlinePlayers()) {
            scheduler.runForEntity(player, () -> {
                if (!player.getWorld().equals(world)) return;
                if (isInsideEvent(event, player.getLocation(), 2.0)) {
                    player.damage(0.001);
                }
            });
        }
    }

    /**
     * Executes the impact phase - schematic pasting and initial explosion effects.
     */
    private void beginImpactPhase(@NotNull ActiveMeteorEvent event) {
        final ActiveMeteorEvent liveEvent = activeEvents.get(event.eventId());
        if (liveEvent == null || liveEvent.phase() == EventPhase.IMPACT
                || liveEvent.phase() == EventPhase.ACTIVE
                || liveEvent.phase() == EventPhase.ROLLBACK
                || liveEvent.phase() == EventPhase.CANCELLED
                || liveEvent.phase() == EventPhase.COMPLETED) return;
        final ActiveMeteorEvent updatedEvent = liveEvent.withPhase(EventPhase.IMPACT);
        activeEvents.put(event.eventId(), updatedEvent);
        historyStore.recordImpact(updatedEvent, automaticEventIds.contains(event.eventId()));
        locationFinder.recordImpact(updatedEvent.center());
        plugin.getServer().getPluginManager().callEvent(
                new com.olmeteors.meteorevents.api.event.MeteorImpactEvent(updatedEvent.eventId(),
                        updatedEvent.meteorType(), updatedEvent.center()));

        final Location center = updatedEvent.center();
        final World world = center.getWorld();
        final int radius = configManager.getTypeConfig(updatedEvent.meteorType()).impactRadius();

        // Immediate /spawnat events do not pass through pre-impact, so chunks
        // must also be loaded here.
        scheduler.forceLoadChunks(world, center.getBlockX() >> 4, center.getBlockZ() >> 4,
                configManager.getChunkForceLoadRadius());

        // Cancel particle task
        if (event.particleTask() != null) {
            scheduler.cancelTask(event.particleTask());
        }

        // Dramatic broadcast
        broadcastMsg("event.broadcast.impacting",
                "color", configManager.getMeteorTypeColor(event.meteorType()),
                "name", configManager.getMeteorTypeName(event.meteorType()),
                "world", world.getName(), "location", formatLocation(center),
                "x", String.valueOf(center.getBlockX()),
                "y", String.valueOf(center.getBlockY()),
                "z", String.valueOf(center.getBlockZ()));

        // Create meteor core display entity (3D rotating item) on the owning
        // region thread so this remains safe on Folia.
        if (configManager.isImpactCoreEnabled()) {
            scheduler.runAtLocation(center,
                    () -> displayEntityManager.spawnMeteorCore(updatedEvent.eventId(), center));
        }
        for (final org.bukkit.util.Vector offset : configManager.getHologramOffsets(event.meteorType())) {
            final Location hologramLocation = center.clone().add(offset);
            scheduler.runAtLocation(hologramLocation, () -> {
                final var hologram = displayEntityManager.spawnHologram(hologramLocation,
                        configManager.getHologramText(event.meteorType()));
                displayEntityManager.addEntityToEvent(updatedEvent.eventId(), hologram.getUniqueId());
            });
        }

        // Snapshot the original terrain before changing a single block. Saved
        // schematics use their minimum selection corner as the paste origin.
        // FAWE centres the schematic around the supplied impact location.
        // Capture both sides of that centre so rollback contains every block.
        final java.io.File eventSchematic = updatedEvent.schematicName() == null ? null
                : schematicManager.getSchematicFile(updatedEvent.schematicName());
        final Location adjustedPasteCenter = schematicManager.adjustedPasteCenter(
                updatedEvent.schematicName(), center);
        final boolean snapshotCaptured = rollbackSystem.captureSchematicArea(
                updatedEvent.eventId(), adjustedPasteCenter, eventSchematic, radius * 2);
        if (!snapshotCaptured) {
            plugin.getLogger().severe("ROLLBACK SNAPSHOT FAILED for event "
                    + updatedEvent.eventId() + "; impact cancelled before changing terrain");
            displayEntityManager.removeEventEntities(updatedEvent.eventId());
            activeEvents.remove(updatedEvent.eventId());
            clearCompletionTracking(updatedEvent.eventId());
            automaticEventIds.remove(updatedEvent.eventId());
            historyStore.markResult(updatedEvent.eventId(), "SNAPSHOT_FAILED");
            scheduler.releaseChunks(updatedEvent.world(),
                    updatedEvent.center().getBlockX() >> 4,
                    updatedEvent.center().getBlockZ() >> 4,
                    configManager.getChunkForceLoadRadius());
            return;
        }

        // Do not place chests or spawn mobs until FAWE has actually finished.
        // Otherwise the late schematic paste can overwrite freshly placed chests.
        impactPastePending.add(updatedEvent.eventId());
        schematicManager.pasteSchematicAsync(updatedEvent.schematicName(), center, world)
                .whenComplete((success, error) -> scheduler.runAtLocation(center,
                        () -> finishImpactAfterPaste(updatedEvent, radius, success, error)));

        // Capture original terrain for rollback
        final var terrainBounds = org.bukkit.util.BoundingBox.of(
                center.clone().add(-radius, -10, -radius),
                center.clone().add(radius, 20, radius)
        );

        // Create dynamic WorldGuard region
        if (wgHook.isAvailable()) {
            final String regionName = "meteor_" + updatedEvent.eventId();
            wgHook.createTemporaryRegion(regionName, world, terrainBounds);
            activeEvents.put(event.eventId(), updatedEvent.withWGRegionName(regionName));
        }

    }

    private void finishImpactAfterPaste(@NotNull ActiveMeteorEvent event, int radius,
                                        @Nullable Boolean success, @Nullable Throwable error) {
        impactPastePending.remove(event.eventId());
        if (error != null || !Boolean.TRUE.equals(success)) {
            plugin.getLogger().log(Level.WARNING,
                    "Schematic paste failed; continuing with guaranteed reward chest for "
                            + event.eventId(), error);
        }

        final ActiveMeteorEvent current = activeEvents.get(event.eventId());
        if (current == null || current.phase() == EventPhase.ROLLBACK
                || current.phase() == EventPhase.COMPLETED) return;
        if (current.phase() == EventPhase.CANCELLED) {
            beginRollbackPhase(current);
            return;
        }
        if (current.phase() != EventPhase.IMPACT) return;

        vaultManager.spawnVault(current.center(), current);
        spawnEventWaves(current);
        showImpactEffect(current, radius);

        final FoliaScheduler.ScheduledTask phaseTask = scheduler.runLaterAtLocation(current.center(),
                () -> {
                    final ActiveMeteorEvent latest = activeEvents.get(current.eventId());
                    if (latest != null && latest.phase() == EventPhase.IMPACT) {
                        beginActivePhase(latest);
                    }
                }, 40L);
        activeEvents.put(current.eventId(), current.withPhaseTask(phaseTask));
    }

    /** Sends the impact outline as player packets, avoiding cross-region world access on Folia. */
    private void showImpactEffect(@NotNull ActiveMeteorEvent event, int radius) {
        final RadiusShape shape = configManager.getRadiusShape(event.meteorType());
        final Location center = event.center();
        final int points = Math.max(36, radius * 8);
        final double viewDistance = Math.max(96.0, radius * 4.0);
        for (final Player player : plugin.getServer().getOnlinePlayers()) {
            scheduler.runForEntity(player, () -> {
                final Location playerLocation = player.getLocation();
                if (!playerLocation.getWorld().equals(center.getWorld())
                        || playerLocation.distanceSquared(center) > viewDistance * viewDistance) return;

                player.spawnParticle(Particle.EXPLOSION, center, 1, 0, 0, 0, 0);
                player.spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0);
                for (int i = 0; i < points; i++) {
                    final double angle = Math.PI * 2.0 * i / points;
                    final double edge = shape.boundaryDistance(angle, radius);
                    player.spawnParticle(Particle.SONIC_BOOM,
                            center.getX() + Math.cos(angle) * edge,
                            center.getY() + 1.0,
                            center.getZ() + Math.sin(angle) * edge,
                            1, 0, 0, 0, 0);
                }
            });
        }
    }

    /**
     * Begins the active phase where hazards are enabled and players can fight.
     */
    private void beginActivePhase(@NotNull ActiveMeteorEvent event) {
        final ActiveMeteorEvent liveEvent = activeEvents.get(event.eventId());
        if (liveEvent == null || liveEvent.phase() != EventPhase.IMPACT) return;
        final ActiveMeteorEvent updatedEvent = liveEvent.withPhase(EventPhase.ACTIVE);
        activeEvents.put(event.eventId(), updatedEvent);
        startTracking(updatedEvent);
        for (final Player player : plugin.getServer().getOnlinePlayers()) {
            scheduler.runForEntity(player, () -> {
                if (isInsideEvent(event, player.getLocation(), 2.0)) {
                    eventParticipants.computeIfAbsent(event.eventId(),
                            ignored -> ConcurrentHashMap.newKeySet()).add(player.getUniqueId());
                }
            });
        }

        // Combat may finish during the two-second impact window. Do not start
        // another boss/hazard cycle after it has already been completed.
        if (combatCompletedEvents.contains(event.eventId())) {
            broadcastMsg("event.broadcast.active",
                    "color", configManager.getMeteorTypeColor(event.meteorType()),
                    "name", configManager.getMeteorTypeName(event.meteorType()));
            if (lootClaimedEvents.contains(event.eventId())) {
                markLootClaimed(event.eventId());
            } else {
                checkEarlyCompletion(event.eventId());
            }
            return;
        }

        // Start hazard manager
        final FoliaScheduler.ScheduledTask hazardTask = hazardManager.startHazards(updatedEvent);
        final ActiveMeteorEvent runningEvent = updatedEvent.withHazardTask(hazardTask);
        activeEvents.put(event.eventId(), runningEvent);

        // Chance configuration may legitimately spawn zero mobs, and a missing
        // MythicMob must not leave the event without a completion trigger.
        scheduler.runLaterAtLocation(updatedEvent.center(), () -> {
            final ActiveMeteorEvent latest = activeEvents.get(updatedEvent.eventId());
            if (latest != null && latest.phase() == EventPhase.ACTIVE
                    && !hasLivingMeteorMobs(latest.eventId())
                    && !completionScheduled.contains(latest.eventId())) {
                handleCombatCompletion(latest);
            }
        }, 20L);

        broadcastMsg("event.broadcast.active",
                "color", configManager.getMeteorTypeColor(event.meteorType()),
                "name", configManager.getMeteorTypeName(event.meteorType()));

        // Schedule rollback
        final long activeDuration = configManager.getTypeConfig(updatedEvent.meteorType())
                .eventDurationTicks();
        final FoliaScheduler.ScheduledTask phaseTask = scheduler.runLaterAtLocation(updatedEvent.center(),
                () -> handleActiveDurationTimeout(updatedEvent, activeDuration),
                activeDuration
        );

        activeEvents.put(event.eventId(), runningEvent.withPhaseTask(phaseTask));

        // MythicMobs can remove/transform entities without a normal Bukkit death
        // event. This watchdog makes combat completion deterministic.
        final java.util.concurrent.atomic.AtomicReference<FoliaScheduler.ScheduledTask> monitorRef =
                new java.util.concurrent.atomic.AtomicReference<>();
        monitorRef.set(scheduler.runRepeatingGlobal(() -> {
            final ActiveMeteorEvent latest = activeEvents.get(event.eventId());
            if (latest == null || latest.phase() != EventPhase.ACTIVE) {
                scheduler.cancelTask(monitorRef.get());
                return;
            }
            handleCombatCompletion(latest);
            if (combatCompletedEvents.contains(latest.eventId())) {
                scheduler.cancelTask(monitorRef.get());
            }
        }, 20L, 20L));
    }

    private void handleActiveDurationTimeout(@NotNull ActiveMeteorEvent event, long elapsedTicks) {
        final ActiveMeteorEvent latest = activeEvents.get(event.eventId());
        if (latest == null || latest.phase() == EventPhase.ROLLBACK) return;
        final boolean unattended = eventParticipants.getOrDefault(event.eventId(), Set.of()).isEmpty()
                && latest.playerDamageMap().isEmpty();
        final long unattendedTicks = configManager.getUnattendedTimeoutMinutes() * 60L * 20L;
        if (unattended && elapsedTicks < unattendedTicks) {
            final long remaining = unattendedTicks - elapsedTicks;
            final FoliaScheduler.ScheduledTask task = scheduler.runLaterAtLocation(latest.center(),
                    () -> beginRollbackPhase(activeEvents.getOrDefault(event.eventId(), latest)), remaining);
            activeEvents.put(event.eventId(), latest.withPhaseTask(task));
            return;
        }
        beginRollbackPhase(latest);
    }

    /**
     * Begins the rollback phase to restore the area.
     */
    private void beginRollbackPhase(@NotNull ActiveMeteorEvent event) {
        final ActiveMeteorEvent liveEvent = activeEvents.get(event.eventId());
        if (liveEvent == null || liveEvent.phase() == EventPhase.COMPLETED) return;
        if (!rollbackStartedEvents.add(event.eventId())) return;
        plugin.getLogger().info("FORCED ROLLBACK STARTED for event " + event.eventId());
        final ActiveMeteorEvent updatedEvent = liveEvent.withPhase(EventPhase.ROLLBACK);
        activeEvents.put(event.eventId(), updatedEvent);

        // Every cleanup operation is isolated: one optional integration must
        // never prevent chest removal or terrain restoration.
        safely("stop hazards", () -> {
            hazardManager.stopHazards(updatedEvent.eventId());
            if (updatedEvent.hazardTask() != null) scheduler.cancelTask(updatedEvent.hazardTask());
        });

        broadcastMsg("event.broadcast.rollback",
                "color", configManager.getMeteorTypeColor(event.meteorType()),
                "name", configManager.getMeteorTypeName(event.meteorType()));

        // Remove display entities
        safely("remove display entities",
                () -> displayEntityManager.removeEventEntities(updatedEvent.eventId()));

        // Remove vault
        safely("remove reward chests", () -> vaultManager.removeVault(updatedEvent.eventId()));

        // Remove any event mobs still alive (timeout/admin stop/despawn edge cases).
        safely("remove meteor mobs", () -> removeTrackedMobs(updatedEvent.eventId()));

        // Remove WorldGuard region
        if (updatedEvent.wgRegionName() != null && wgHook.isAvailable()) {
            safely("remove WorldGuard region", () ->
                    wgHook.removeTemporaryRegion(updatedEvent.wgRegionName(), updatedEvent.world()));
        }

        // Restore is independently configurable for every meteor type. When disabled,
        // the snapshot is discarded and the changed structure is intentionally kept.
        final boolean restoreStructure = configManager.isStructureRestoreEnabled(event.meteorType());
        final CompletableFuture<Boolean> rollbackFuture;
        if (restoreStructure) {
            rollbackFuture = rollbackSystem.rollbackEvent(updatedEvent.eventId());
        } else {
            rollbackSystem.discardSnapshot(updatedEvent.eventId());
            rollbackFuture = CompletableFuture.completedFuture(true);
        }

        rollbackFuture.whenComplete((success, error) -> {
            final boolean cancelled = cancelRequestedEvents.remove(updatedEvent.eventId());
            if (error != null) {
                plugin.getLogger().log(Level.SEVERE,
                        "Terrain rollback future failed for " + updatedEvent.eventId(), error);
                historyStore.markResult(updatedEvent.eventId(), "ROLLBACK_ERROR");
            } else if (Boolean.TRUE.equals(success)) {
                historyStore.markResult(updatedEvent.eventId(), cancelled
                        ? (restoreStructure ? "CANCELLED_RESTORED" : "CANCELLED_STRUCTURE_KEPT")
                        : (restoreStructure ? "RESTORED" : "STRUCTURE_KEPT"));
                if (restoreStructure) {
                    broadcastMsg("event.broadcast.restored",
                            "color", configManager.getMeteorTypeColor(event.meteorType()),
                            "name", configManager.getMeteorTypeName(event.meteorType()));
                }
            } else {
                historyStore.markResult(updatedEvent.eventId(), "ROLLBACK_FAILED");
                plugin.getLogger().warning("Rollback may not have completed fully for event "
                        + updatedEvent.eventId());
            }

            // Release chunks
            scheduler.releaseChunks(updatedEvent.world(),
                    updatedEvent.center().getBlockX() >> 4,
                    updatedEvent.center().getBlockZ() >> 4,
                    configManager.getChunkForceLoadRadius());

            // Mark as completed
            activeEvents.remove(event.eventId());
            clearCompletionTracking(event.eventId());
            automaticEventIds.remove(event.eventId());
            eventParticipants.remove(event.eventId());
            scheduler.callGlobal(() -> plugin.getServer().getPluginManager().callEvent(
                    new com.olmeteors.meteorevents.api.event.MeteorFinishEvent(event.eventId(),
                            event.meteorType(), event.center(), Boolean.TRUE.equals(success) && restoreStructure)));
        });
    }

    private void safely(@NotNull String operation, @NotNull Runnable action) {
        try {
            action.run();
        } catch (Exception | LinkageError error) {
            plugin.getLogger().log(Level.WARNING,
                    "Event cleanup step failed (continuing): " + operation, error);
        }
    }

    /**
     * Cancels an active event immediately.
     */
    public void cancelEvent(@NotNull String eventId, @NotNull CommandSender sender) {
        final ActiveMeteorEvent event = activeEvents.get(eventId);
        if (event == null) {
            tell(sender, "command.cancel.no_event", "eventId", eventId);
            return;
        }

        cancelRequestedEvents.add(eventId);

        // Cancel all tasks
        cancelEventTasks(event);

        // A paste may still be running during IMPACT. Mark the cancellation and
        // let its completion callback start rollback, so paste and restore never race.
        if (event.phase() == EventPhase.IMPACT && impactPastePending.contains(eventId)) {
            activeEvents.put(eventId, event.withPhase(EventPhase.CANCELLED));
            hazardManager.stopHazards(eventId);
            displayEntityManager.removeEventEntities(eventId);
            vaultManager.removeVault(eventId);
            removeTrackedMobs(eventId);
            historyStore.markResult(eventId, "CANCEL_REQUESTED");
            tell(sender, "command.cancel.success", "eventId", eventId);
            broadcastMsg("event.broadcast.cancelled",
                    "color", configManager.getMeteorTypeColor(event.meteorType()),
                    "name", configManager.getMeteorTypeName(event.meteorType()));
            return;
        }

        if (event.phase() == EventPhase.IMPACT) {
            beginRollbackPhase(event);
            tell(sender, "command.cancel.success", "eventId", eventId);
            return;
        }

        // Once blocks may have changed, cancellation is completed through the
        // same deterministic terrain-restore path as a graceful stop.
        if (event.phase() == EventPhase.ACTIVE || event.phase() == EventPhase.ROLLBACK) {
            beginRollbackPhase(event);
            tell(sender, "command.cancel.success", "eventId", eventId);
            return;
        }

        // Remove entities
        displayEntityManager.removeEventEntities(eventId);
        vaultManager.removeVault(eventId);
        hazardManager.stopHazards(eventId);
        removeTrackedMobs(eventId);

        // Remove WG region
        if (event.wgRegionName() != null && wgHook.isAvailable()) {
            wgHook.removeTemporaryRegion(event.wgRegionName(), event.world());
        }

        // Release chunks
        scheduler.releaseChunks(event.world(),
                event.center().getBlockX() >> 4,
                event.center().getBlockZ() >> 4,
                configManager.getChunkForceLoadRadius());

        // Remove from active events
        activeEvents.remove(eventId);
        rollbackSystem.discardSnapshot(eventId);
        clearCompletionTracking(eventId);
        cancelRequestedEvents.remove(eventId);
        automaticEventIds.remove(eventId);
        historyStore.markResult(eventId, "CANCELLED");

        tell(sender, "command.cancel.success", "eventId", eventId);
        broadcastMsg("event.broadcast.cancelled",
                "color", configManager.getMeteorTypeColor(event.meteorType()),
                "name", configManager.getMeteorTypeName(event.meteorType()));
    }

    /**
     * Stops an event gracefully (immediate rollback).
     */
    public void stopEvent(@NotNull String eventId, @NotNull CommandSender sender) {
        final ActiveMeteorEvent event = activeEvents.get(eventId);
        if (event == null) {
            tell(sender, "command.stop.no_event", "eventId", eventId);
            return;
        }

        // Cancel tasks
        cancelEventTasks(event);

        // Do not restore while FAWE is still pasting. The paste completion
        // callback observes CANCELLED and starts rollback in the correct order.
        if (event.phase() == EventPhase.IMPACT && impactPastePending.contains(eventId)) {
            activeEvents.put(eventId, event.withPhase(EventPhase.CANCELLED));
            displayEntityManager.removeEventEntities(eventId);
            vaultManager.removeVault(eventId);
            hazardManager.stopHazards(eventId);
            removeTrackedMobs(eventId);
            tell(sender, "command.stop.success", "eventId", eventId);
            return;
        }

        // Begin immediate rollback
        beginRollbackPhase(event);
        tell(sender, "command.stop.success", "eventId", eventId);
    }

    /**
     * Cancels all scheduled tasks for an event.
     */
    private void cancelEventTasks(@NotNull ActiveMeteorEvent event) {
        if (event.phaseTask() != null) scheduler.cancelTask(event.phaseTask());
        if (event.hazardTask() != null) scheduler.cancelTask(event.hazardTask());
        if (event.particleTask() != null) scheduler.cancelTask(event.particleTask());
    }

    /**
     * Shuts down all active events (called on plugin disable).
     */
    public void shutdownAll() {
        if (automaticTask != null) scheduler.cancelTask(automaticTask);
        for (final var entry : activeEvents.entrySet()) {
            cancelEventTasks(entry.getValue());
            stopTracking(entry.getKey());
            displayEntityManager.removeEventEntities(entry.getKey());
            vaultManager.removeVault(entry.getKey());
            hazardManager.stopHazards(entry.getKey());

            final var event = entry.getValue();
            if (event.wgRegionName() != null && wgHook.isAvailable()) {
                wgHook.removeTemporaryRegion(event.wgRegionName(), event.world());
            }
            scheduler.releaseChunks(event.world(),
                    event.center().getBlockX() >> 4,
                    event.center().getBlockZ() >> 4,
                    configManager.getChunkForceLoadRadius());
        }
        activeEvents.clear();
        eventMobIds.clear();
        lootClaimedEvents.clear();
        completionScheduled.clear();
        combatCompletedEvents.clear();
        rollbackStartedEvents.clear();
        cancelRequestedEvents.clear();
        impactPastePending.clear();
        pendingWaves.clear();
        eventParticipants.clear();
    }

    public void startAutomaticScheduler() {
        if (automaticTask != null) scheduler.cancelTask(automaticTask);
        automaticTask = null;
        nextAutomaticAtMillis = 0L;
        if (!configManager.isAutomaticEventsEnabled()) {
            plugin.getLogger().info("Automatic meteor events are disabled.");
            return;
        }
        scheduleNextAutomaticEvent();
    }

    public boolean setAutomaticEventsEnabled(boolean enabled) {
        if (!configManager.setAutomaticEventsEnabled(enabled)) return false;
        startAutomaticScheduler();
        return true;
    }

    public boolean isAutomaticEventsEnabled() {
        return configManager.isAutomaticEventsEnabled();
    }

    public long getNextAutomaticAtMillis() {
        return nextAutomaticAtMillis;
    }

    public void triggerAutomaticEventNow() {
        if (automaticTask != null) scheduler.cancelTask(automaticTask);
        automaticTask = null;
        nextAutomaticAtMillis = 0L;
        scheduler.callGlobal(this::runAutomaticEvent);
    }

    public boolean scheduleAutomaticEventInMinutes(int minutes) {
        if (minutes < 1) return false;
        if (!configManager.setAutomaticEventsEnabled(true)) return false;
        if (automaticTask != null) scheduler.cancelTask(automaticTask);
        nextAutomaticAtMillis = System.currentTimeMillis() + minutes * 60_000L;
        automaticTask = scheduler.runLaterGlobal(this::runAutomaticEvent, minutes * 60L * 20L);
        return true;
    }

    public @NotNull List<MeteorHistoryStore.HistoryEntry> getRecentHistory(int limit) {
        return historyStore.recent(limit);
    }

    private void scheduleNextAutomaticEvent() {
        final int min = configManager.getAutomaticMinMinutes();
        final int max = configManager.getAutomaticMaxMinutes();
        final int minutes = ThreadLocalRandom.current().nextInt(min, max + 1);
        nextAutomaticAtMillis = System.currentTimeMillis() + minutes * 60_000L;
        automaticTask = scheduler.runLaterGlobal(this::runAutomaticEvent, minutes * 60L * 20L);
        plugin.getLogger().info("Next automatic meteor scheduled in " + minutes + " minutes.");
    }

    private void runAutomaticEvent() {
        if (configManager.isTpsGuardEnabled()
                && plugin.getServer().getTPS()[0] < configManager.getMinimumAutoTps()) {
            final int retry = configManager.getTpsRetryMinutes();
            nextAutomaticAtMillis = System.currentTimeMillis() + retry * 60_000L;
            automaticTask = scheduler.runLaterGlobal(this::runAutomaticEvent, retry * 60L * 20L);
            plugin.getLogger().warning("Automatic meteor delayed " + retry
                    + " minutes because TPS is " + String.format(Locale.ROOT, "%.2f", plugin.getServer().getTPS()[0]));
            return;
        }
        try {
            final long activeCount = activeEvents.values().stream()
                    .filter(event -> event.phase().isActive()).count();
            if (activeCount >= configManager.getAutomaticMaxActiveEvents()) return;

            List<World> worlds = configManager.getAutomaticWorlds().stream()
                    .map(plugin.getServer()::getWorld).filter(Objects::nonNull).toList();
            if (worlds.isEmpty()) worlds = plugin.getServer().getWorlds();
            worlds = worlds.stream().filter(world ->
                    configManager.isAutomaticWorldEnabled(world.getName())).toList();
            if (worlds.isEmpty()) return;

            final List<MeteorType> types = configManager.getAutomaticTypes().stream()
                    .map(raw -> {
                        try { return MeteorType.fromString(raw); }
                        catch (IllegalArgumentException ignored) { return null; }
                    }).filter(Objects::nonNull).toList();
            final List<MeteorType> pool = types.isEmpty() ? List.of(MeteorType.values()) : types;
            final World world = selectWeightedAutomaticWorld(worlds);
            final MeteorType type = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
            startAutomaticEvent(type, world);
        } finally {
            if (configManager.isAutomaticEventsEnabled()) scheduleNextAutomaticEvent();
        }
    }

    private void startAutomaticEvent(@NotNull MeteorType type, @NotNull World world) {
        if (!plugin.getFAWEHook().isAvailable()) {
            plugin.getLogger().warning("Automatic meteor skipped because FAWE is unavailable.");
            return;
        }
        final ConfigManager.LocationPreset preset =
                configManager.getAutomaticLocationPreset(world.getName());
        locationFinder.findSuitableLocationAsync(world, type, preset,
                configManager.getAutomaticMinDistance(), configManager.getAutomaticMaxDistance(),
                configManager.getAutomaticSearchShape(world.getName()))
                .whenComplete((location, error) -> {
            if (error != null) {
                plugin.getLogger().log(Level.WARNING, "Automatic location search failed", error);
                return;
            }
            if (location == null || location.isEmpty()) return;
            scheduler.callAtLocation(location.get(), () -> {
                final var preStart = new com.olmeteors.meteorevents.api.event.MeteorPreStartEvent(type,
                        location.get().toCenterLocation());
                plugin.getServer().getPluginManager().callEvent(preStart);
                if (preStart.isCancelled()) return;
                final ActiveMeteorEvent event = createEvent(type, location.get().toCenterLocation());
                automaticEventIds.add(event.eventId());
                activeEvents.put(event.eventId(), event);
                beginAnimatedFall(event, configManager.getFallMode(type));
            });
        });
    }

    private @NotNull World selectWeightedAutomaticWorld(@NotNull List<World> worlds) {
        final int total = worlds.stream().mapToInt(world ->
                configManager.getAutomaticWorldWeight(world.getName())).sum();
        int roll = ThreadLocalRandom.current().nextInt(Math.max(1, total));
        for (final World world : worlds) {
            roll -= configManager.getAutomaticWorldWeight(world.getName());
            if (roll < 0) return world;
        }
        return worlds.getFirst();
    }

    /**
     * Loads active events from persistent storage (if any).
     * <p>
     * Future: This will read from {@code events.yml} (or similar persistent storage)
     * so that events that were active during an unexpected shutdown can be
     * resumed or properly rolled back on next startup.
     */
    public void loadActiveEvents() {
        historyStore.recent(50).forEach(entry -> locationFinder.recordImpact(
                entry.world(), entry.x(), entry.z(), entry.epochMillis()));
        final int queued = rollbackSystem.recoverPendingSnapshots();
        if (queued > 0) {
            plugin.getLogger().warning("Queued " + queued
                    + " interrupted meteor terrain snapshot(s) for automatic recovery.");
        } else {
            plugin.getLogger().info("No interrupted meteor terrain snapshots found.");
        }
    }

    // ---- Boss Mechanics ----

    private void spawnBoss(@NotNull ActiveMeteorEvent event) {
        final String bossMobId = event.bossMobId();
        if (bossMobId != null && !bossMobId.isEmpty() && mythicMobsHook.isAvailable()) {
            final Location bossLocation = event.center().clone().add(0, 3, 0);
            scheduler.runAtLocation(bossLocation, () -> trackMob(event.eventId(),
                    mythicMobsHook.spawnBoss(bossMobId, bossLocation,
                            configManager.getTypeConfig(event.meteorType()).bossHealthMultiplier())));
        }
    }

    private void spawnEventMobs(@NotNull ActiveMeteorEvent event) {
        final List<String> configured = configManager.getTypeConfig(event.meteorType()).mythicMobs();
        final Map<String, Double> weighted = configManager.getMythicMobChances(event.meteorType());
        int index = 0;
        for (final Location spawnPoint : event.mobSpawnPoints()) {
            if (mythicMobsHook.isAvailable()) {
                final int mobIndex = index++;
                scheduler.runAtLocation(spawnPoint, () -> {
                final String selectedMob = configured.isEmpty() ? null : selectWeightedMob(weighted,
                        configured.get(mobIndex % configured.size()));
                final Entity spawned = configured.isEmpty()
                        ? mythicMobsHook.spawnMinion(spawnPoint, event.meteorType())
                        : selectedMob == null ? null : mythicMobsHook.spawnMob(selectedMob, spawnPoint);
                trackMob(event.eventId(), spawned);
                });
            }
        }
    }

    private void spawnEventWaves(@NotNull ActiveMeteorEvent event) {
        final int count = configManager.getWaveCount(event.meteorType());
        final long interval = configManager.getWaveIntervalSeconds(event.meteorType()) * 20L;
        pendingWaves.add(event.eventId());
        for (int wave = 1; wave <= count; wave++) {
            final int currentWave = wave;
            scheduler.runLaterGlobal(() -> {
                final ActiveMeteorEvent latest = activeEvents.get(event.eventId());
                if (latest == null || latest.phase() == EventPhase.ROLLBACK) return;
                spawnEventMobs(latest);
                broadcastMsg("event.broadcast.wave", "wave", String.valueOf(currentWave),
                        "total", String.valueOf(count), "name",
                        configManager.getMeteorTypeName(latest.meteorType()));
                if (currentWave == count) {
                    pendingWaves.remove(event.eventId());
                    spawnBoss(latest);
                }
            }, (wave - 1L) * interval);
        }
    }

    private @Nullable String selectWeightedMob(@NotNull Map<String, Double> weights,
                                                @NotNull String fallback) {
        final double total = weights.values().stream().filter(value -> value > 0)
                .mapToDouble(Double::doubleValue).sum();
        if (total <= 0) return fallback;
        final double range = Math.max(100.0, total);
        double roll = ThreadLocalRandom.current().nextDouble(range);
        if (total < 100.0 && roll >= total) return null;
        for (final var entry : weights.entrySet()) {
            if (entry.getValue() <= 0) continue;
            roll -= entry.getValue();
            if (roll <= 0) return entry.getKey();
        }
        return fallback;
    }

    private void trackMob(@NotNull String eventId, @Nullable Entity entity) {
        if (entity != null) {
            entity.getPersistentDataContainer().set(meteorEventKey,
                    PersistentDataType.STRING, eventId);
            entity.addScoreboardTag("olmeteor_mob");
            eventMobIds.computeIfAbsent(eventId, ignored -> ConcurrentHashMap.newKeySet())
                    .add(entity.getUniqueId());
            plugin.getLogger().info("Tracking meteor mob " + entity.getUniqueId()
                    + " for event " + eventId);
        }
    }

    /** Called by the vault/chest system after the event loot has been claimed. */
    public void markLootClaimed(@NotNull String eventId) {
        if (!lootClaimedEvents.add(eventId)) return;
        final ActiveMeteorEvent event = activeEvents.get(eventId);
        if (event == null || event.phase() == EventPhase.ROLLBACK
                || event.phase() == EventPhase.COMPLETED
                || event.phase() == EventPhase.CANCELLED) return;

        // Opening every physical chest is an unconditional completion signal.
        // It must work during IMPACT as well as ACTIVE and must not be blocked
        // by a stale MythicMobs UUID.
        if (event.phaseTask() != null) scheduler.cancelTask(event.phaseTask());
        completionScheduled.add(eventId);
        broadcastMsg("event.broadcast.all_chests_opened");
        plugin.getLogger().info("All reward chests claimed; forced rollback in 10 seconds: " + eventId);
        final FoliaScheduler.ScheduledTask cleanupTask = scheduler.runLaterAtLocation(event.center(),
                () -> {
                    final ActiveMeteorEvent latest = activeEvents.get(eventId);
                    if (latest != null) beginRollbackPhase(latest);
                }, 200L);
        activeEvents.put(eventId, event.withPhaseTask(cleanupTask));
    }

    /** Called by chest interaction as an additional deterministic completion check. */
    public void completeCombatIfReady(@NotNull String eventId) {
        final ActiveMeteorEvent event = activeEvents.get(eventId);
        if (event != null) handleCombatCompletion(event);
    }

    public boolean hasLivingMeteorMobs(@NotNull String eventId) {
        if (pendingWaves.contains(eventId)) return true;
        final Set<UUID> ids = eventMobIds.get(eventId);
        if (ids == null) return false;
        // EntityDeathEvent removes normal deaths. A null lookup safely catches
        // MythicMobs transformations/despawns without touching a foreign region.
        ids.removeIf(id -> plugin.getServer().getEntity(id) == null);
        return !ids.isEmpty();
    }

    private void checkEarlyCompletion(@NotNull String eventId) {
        final ActiveMeteorEvent event = activeEvents.get(eventId);
        if (event == null || event.phase() != EventPhase.ACTIVE) return;
        if (hasLivingMeteorMobs(eventId)) return;
        // Once every tracked mob is gone, the configured delay becomes the
        // reward-claim window. Rollback must not depend on every chest being
        // opened, otherwise abandoned chests keep the terrain forever.
        if (!completionScheduled.add(eventId)) return;

        if (event.phaseTask() != null) scheduler.cancelTask(event.phaseTask());
        final boolean unattended = eventParticipants.getOrDefault(eventId, Set.of()).isEmpty()
                && event.playerDamageMap().isEmpty();
        final long delay = unattended
                ? configManager.getUnattendedTimeoutMinutes() * 60L * 20L
                : configManager.getCompletionCleanupDelaySeconds() * 20L;
        final FoliaScheduler.ScheduledTask cleanupTask = scheduler.runLaterAtLocation(event.center(),
                () -> {
                    final ActiveMeteorEvent latest = activeEvents.get(eventId);
                    if (latest != null) beginRollbackPhase(latest);
                }, delay);
        activeEvents.put(eventId, event.withPhaseTask(cleanupTask));
    }

    private void clearCompletionTracking(@NotNull String eventId) {
        stopTracking(eventId);
        eventMobIds.remove(eventId);
        lootClaimedEvents.remove(eventId);
        completionScheduled.remove(eventId);
        combatCompletedEvents.remove(eventId);
        rollbackStartedEvents.remove(eventId);
        pendingWaves.remove(eventId);
        impactPastePending.remove(eventId);
        eventParticipants.remove(eventId);
    }

    private void startTracking(@NotNull ActiveMeteorEvent event) {
        if (!configManager.isTrackingBossBarEnabled()) return;
        stopTracking(event.eventId());
        trackingTasks.put(event.eventId(), scheduler.runRepeatingGlobal(
                () -> updateTracking(event.eventId()), 1L, 20L));
    }

    private void updateTracking(@NotNull String eventId) {
        final ActiveMeteorEvent event = activeEvents.get(eventId);
        if (event == null || !event.phase().isActive()) { stopTracking(eventId); return; }
        final Map<UUID, net.kyori.adventure.bossbar.BossBar> bars = trackingBars
                .computeIfAbsent(eventId, ignored -> new ConcurrentHashMap<>());
        for (final Player player : plugin.getServer().getOnlinePlayers()) {
            scheduler.runForEntity(player, () -> {
                final boolean visible = player.getWorld().equals(event.world())
                        && player.getLocation().distanceSquared(event.center())
                        <= (double) configManager.getTrackingDistance() * configManager.getTrackingDistance();
                final var old = bars.get(player.getUniqueId());
                if (!visible) { if (old != null) player.hideBossBar(old); bars.remove(player.getUniqueId()); return; }
                final double distance = player.getLocation().distance(event.center());
                final String direction = cardinal(player.getLocation(), event.center());
                final int mobs = eventMobIds.getOrDefault(eventId, Set.of()).size();
                final Component title = MessageUtil.parse(configManager.getMeteorTypeColor(event.meteorType())
                        + configManager.getMeteorTypeName(event.meteorType()) + " &8• &f"
                        + Math.round(distance) + "m " + direction + " &8• &cMob: " + mobs);
                final float progress = (float) Math.max(0.01, Math.min(1.0,
                        1.0 - event.phaseElapsedTimeMs() / (configManager.getTypeConfig(event.meteorType())
                                .eventDurationSeconds() * 1000.0)));
                final var bar = old == null ? net.kyori.adventure.bossbar.BossBar.bossBar(title,
                        progress, net.kyori.adventure.bossbar.BossBar.Color.YELLOW,
                        net.kyori.adventure.bossbar.BossBar.Overlay.PROGRESS) : old;
                bar.name(title); bar.progress(progress);
                if (old == null) { bars.put(player.getUniqueId(), bar); player.showBossBar(bar); }
            });
        }
    }

    private String cardinal(Location from, Location to) {
        final double dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? "Doğu →" : "← Batı";
        return dz > 0 ? "Güney ↓" : "↑ Kuzey";
    }

    private void stopTracking(@NotNull String eventId) {
        final var task = trackingTasks.remove(eventId); if (task != null) scheduler.cancelTask(task);
        final var bars = trackingBars.remove(eventId); if (bars == null) return;
        bars.forEach((uuid, bar) -> {
            final Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) scheduler.runForEntity(player, () -> player.hideBossBar(bar));
        });
    }

    private void removeTrackedMobs(@NotNull String eventId) {
        final Set<UUID> ids = eventMobIds.get(eventId);
        if (ids == null) return;
        for (final UUID id : new HashSet<>(ids)) {
            final Entity entity = plugin.getServer().getEntity(id);
            if (entity != null) scheduler.runForEntity(entity, () -> {
                if (entity.isValid()) entity.remove();
            });
        }
        ids.clear();
    }

    // ---- Event Handlers ----

    @EventHandler
    public void onBossDamage(EntityDamageByEntityEvent event) {
        final Player player;
        if (event.getDamager() instanceof Player directPlayer) {
            player = directPlayer;
        } else if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            player = shooter;
        } else {
            return;
        }
        final String eventId = resolveMeteorEventId(event.getEntity());
        if (eventId == null) return;
        final ActiveMeteorEvent meteorEvent = activeEvents.get(eventId);
        if (meteorEvent == null) return;
        meteorEvent.recordPlayerDamage(player.getUniqueId(), event.getFinalDamage());
        plugin.getPlayerStatsStore().addDamage(player.getUniqueId(), event.getFinalDamage());
        eventParticipants.computeIfAbsent(eventId, ignored -> ConcurrentHashMap.newKeySet())
                .add(player.getUniqueId());
        if (configManager.isDamageActionBarEnabled(meteorEvent.meteorType())) {
            final int rank = damageRank(meteorEvent, player.getUniqueId());
            MessageUtil.sendActionBar(player, combatText(meteorEvent.meteorType(),
                    "damage-actionbar-text", "&cHasar: &f%damage% &8| &eSıra: #%rank%",
                    "damage", String.format(Locale.ROOT, "%.1f", meteorEvent.playerDamageMap()
                            .getOrDefault(player.getUniqueId(), 0.0)),
                    "rank", String.valueOf(rank)));
        }
    }

    @EventHandler
    public void onMeteorMobDeath(EntityDeathEvent deathEvent) {
        final UUID deadId = deathEvent.getEntity().getUniqueId();
        final String taggedEventId = resolveMeteorEventId(deathEvent.getEntity());
        if (taggedEventId != null && deathEvent.getEntity().getKiller() != null) {
            plugin.getPlayerStatsStore().addKill(deathEvent.getEntity().getKiller().getUniqueId());
        }
        for (final var entry : eventMobIds.entrySet()) {
            if (!entry.getValue().remove(deadId)
                    && !entry.getKey().equals(taggedEventId)) continue;

            final ActiveMeteorEvent meteorEvent = activeEvents.get(entry.getKey());
            final Player killer = deathEvent.getEntity().getKiller();
            if (killer != null && meteorEvent != null
                    && configManager.isKillActionBarEnabled(meteorEvent.meteorType())) {
                MessageUtil.sendActionBar(killer, combatText(meteorEvent.meteorType(),
                        "kill-actionbar-text", "&aMob öldürüldü! &7Kalan: &e%remaining% &8| &6Sıra: #%rank%",
                        "remaining", String.valueOf(entry.getValue().size()),
                        "rank", String.valueOf(damageRank(meteorEvent, killer.getUniqueId()))));
            }
            if (meteorEvent != null
                    && deathEvent.getEntity().getScoreboardTags().contains("meteor_boss")) {
                vaultManager.distributeKeys(meteorEvent);
            }
            if (meteorEvent != null) handleCombatCompletion(meteorEvent);
            break;
        }
    }

    private @Nullable String resolveMeteorEventId(@NotNull Entity entity) {
        final String tagged = entity.getPersistentDataContainer().get(
                meteorEventKey, PersistentDataType.STRING);
        if (tagged != null && activeEvents.containsKey(tagged)) return tagged;
        final UUID entityId = entity.getUniqueId();
        for (final var entry : eventMobIds.entrySet()) {
            if (entry.getValue().contains(entityId)) return entry.getKey();
        }
        return null;
    }

    private void handleCombatCompletion(@NotNull ActiveMeteorEvent event) {
        if ((event.phase() != EventPhase.ACTIVE && event.phase() != EventPhase.IMPACT)
                || hasLivingMeteorMobs(event.eventId())) return;
        if (!combatCompletedEvents.add(event.eventId())) return;
        plugin.getLogger().info("Meteor combat completed; showing ranking: " + event.eventId());
        safely("unlock reward chests", () -> vaultManager.unlockEventChests(event.eventId()));
        safely("distribute meteor keys", () -> vaultManager.distributeKeys(event));
        safely("grant leaderboard rewards", () -> vaultManager.grantLeaderboardRewards(event));
        safely("show damage leaderboard", () -> showDamageLeaderboard(event));
        checkEarlyCompletion(event.eventId());
    }

    private int damageRank(@NotNull ActiveMeteorEvent event, @NotNull UUID playerId) {
        final double damage = event.playerDamageMap().getOrDefault(playerId, 0.0);
        return 1 + (int) event.playerDamageMap().values().stream()
                .filter(other -> other > damage).count();
    }

    private void showDamageLeaderboard(@NotNull ActiveMeteorEvent event) {
        if (!configManager.isLeaderboardBroadcastEnabled(event.meteorType())) return;
        final List<Map.Entry<UUID, Double>> ranking = event.playerDamageMap().entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(configManager.getLeaderboardSize(event.meteorType())).toList();
        scheduler.callGlobal(() -> {
            MessageUtil.broadcast(combatText(event.meteorType(), "leaderboard-title",
                    "&6&lMeteor Hasar Sıralaması"));
            if (ranking.isEmpty()) MessageUtil.broadcast(combatText(event.meteorType(),
                    "leaderboard-empty", "&7Bu meteor için oyuncu hasarı kaydedilmedi."));
            for (int index = 0; index < ranking.size(); index++) {
                final var row = ranking.get(index);
                final String name = Optional.ofNullable(plugin.getServer()
                                .getOfflinePlayer(row.getKey()).getName())
                        .orElse(row.getKey().toString().substring(0, 8));
                MessageUtil.broadcast(combatText(event.meteorType(), "leaderboard-entry",
                        "&e#%rank% &f%player% &7- &c%damage% hasar",
                        "rank", String.valueOf(index + 1), "player", name,
                        "damage", String.format(Locale.ROOT, "%.1f", row.getValue())));
            }
        });
    }

    private @NotNull String combatText(@NotNull MeteorType type, @NotNull String key,
                                       @NotNull String fallback,
                                       @NotNull String... placeholders) {
        String text = configManager.getCombatFeedbackText(type, key, fallback);
        for (int index = 0; index + 1 < placeholders.length; index += 2) {
            text = text.replace("%" + placeholders[index] + "%", placeholders[index + 1]);
        }
        return text;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Check if player died inside an active event area
        final Player player = event.getPlayer();
        for (final var entry : activeEvents.entrySet()) {
            final var meteorEvent = entry.getValue();
            if (meteorEvent.isActive()
                    && meteorEvent.playerDamageMap().containsKey(player.getUniqueId())) {
                // Player still qualifies for rewards if they dealt damage before dying
                // unless they died outside the event area
                if (isInsideEvent(meteorEvent, player.getLocation(), 2.0)) {
                    event.setKeepInventory(true);
                    event.setKeepLevel(true);
                }
                break;
            }
        }
    }

    // ---- Utility Methods ----

    private boolean isInsideEvent(@NotNull ActiveMeteorEvent event,
                                  @NotNull Location location, double multiplier) {
        if (!location.getWorld().equals(event.world())) return false;
        final double radius = configManager.getTypeConfig(event.meteorType()).impactRadius() * multiplier;
        return configManager.getRadiusShape(event.meteorType()).contains(
                location.getX() - event.center().getX(),
                location.getZ() - event.center().getZ(), radius);
    }

    private @Nullable World resolveWorld(@Nullable String worldName, CommandSender sender) {
        if (worldName != null) {
            final World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                tellSafely(sender, "command.start.world_not_found", "world", worldName);
            }
            return world;
        }

        // Default to sender's world if player, otherwise overworld
        if (sender instanceof Player player) {
            return player.getWorld();
        }
        return plugin.getServer().getWorlds().get(0);
    }

    private int chunkRadius() {
        return configManager.getChunkForceLoadRadius();
    }

    private List<Location> generateMobSpawnPoints(Location center, int radius) {
        final List<Location> points = new ArrayList<>();
        final int count = Math.max(3, radius / 10);

        for (int i = 0; i < count; i++) {
            final double angle = 2 * Math.PI * i / count;
            final double distance = radius * 0.6 + ThreadLocalRandom.current().nextDouble(radius * 0.3);
            final double x = center.getX() + distance * Math.cos(angle);
            final double z = center.getZ() + distance * Math.sin(angle);
            points.add(new Location(center.getWorld(), x, center.getY(), z));
        }

        return points;
    }

    private List<Location> configuredMobSpawnPoints(@NotNull MeteorType type,
                                                     @NotNull Location center, int radius) {
        final List<org.bukkit.util.Vector> offsets = configManager.getMobSpawnOffsets(type);
        if (offsets.isEmpty()) return generateMobSpawnPoints(center, radius);
        return offsets.stream().map(offset -> center.clone().add(offset)).toList();
    }

    private String generateEventId(MeteorType type) {
        final int id = ++eventIdCounter;
        return type.name().toLowerCase() + "_" + System.currentTimeMillis() + "_" + id;
    }

    private String formatLocation(Location loc) {
        return String.format("%s  •  X: %d  Y: %d  Z: %d",
                loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    // ---- Accessors ----

    public Map<String, ActiveMeteorEvent> getActiveEvents() {
        return Collections.unmodifiableMap(activeEvents);
    }

    public @Nullable ActiveMeteorEvent getEvent(String eventId) {
        return activeEvents.get(eventId);
    }
}
