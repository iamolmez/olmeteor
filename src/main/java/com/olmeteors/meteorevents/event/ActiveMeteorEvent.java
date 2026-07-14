package com.olmeteors.meteorevents.event;

import com.olmeteors.meteorevents.scheduler.FoliaScheduler;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Immutable record representing the state of an active meteor event.
 * Contains all data needed to manage the event lifecycle.
 */
public record ActiveMeteorEvent(
        @NotNull String eventId,
        @NotNull MeteorType meteorType,
        @NotNull Location center,
        @NotNull World world,
        @NotNull BoundingBox region,
        @NotNull EventPhase phase,
        long startTime,
        long phaseStartTime,
        int chunkRadius,
        @Nullable String schematicName,
        @Nullable String bossMobId,
        boolean bossDefeated,
        @NotNull Map<UUID, Double> playerDamageMap,
        @NotNull Set<UUID> vaultOpenedBy,
        @NotNull List<Location> mobSpawnPoints,
        @Nullable FoliaScheduler.ScheduledTask phaseTask,
        @Nullable FoliaScheduler.ScheduledTask hazardTask,
        @Nullable FoliaScheduler.ScheduledTask particleTask,
        @Nullable String wgRegionName,
        @Nullable BoundingBox originalTerrain
) {

    /**
     * Creates a new ActiveMeteorEvent at the SCHEDULED phase.
     */
    public static @NotNull ActiveMeteorEvent createNew(
            @NotNull String eventId,
            @NotNull MeteorType type,
            @NotNull Location center,
            @NotNull BoundingBox region,
            int chunkRadius,
            @Nullable String schematicName,
            @Nullable String bossMobId,
            @NotNull List<Location> mobSpawnPoints) {

        final long now = System.currentTimeMillis();
        return new ActiveMeteorEvent(
                eventId, type, center, center.getWorld(), region,
                EventPhase.SCHEDULED, now, now, chunkRadius,
                schematicName, bossMobId, false,
                new ConcurrentHashMap<>(), ConcurrentHashMap.newKeySet(),
                new ArrayList<>(mobSpawnPoints),
                null, null, null, null, null
        );
    }

    /**
     * Creates a copy of this event with a new phase.
     *
     * @param newPhase the new phase
     * @return the updated event record
     */
    public @NotNull ActiveMeteorEvent withPhase(@NotNull EventPhase newPhase) {
        return new ActiveMeteorEvent(
                eventId, meteorType, center, world, region,
                newPhase, startTime, System.currentTimeMillis(),
                chunkRadius, schematicName, bossMobId, bossDefeated,
                playerDamageMap, vaultOpenedBy, mobSpawnPoints,
                phaseTask, hazardTask, particleTask, wgRegionName, originalTerrain
        );
    }

    /**
     * Creates a copy of this event with an updated task reference.
     *
     * @param newPhaseTask the new phase task
     * @return the updated event record
     */
    public @NotNull ActiveMeteorEvent withPhaseTask(@Nullable FoliaScheduler.ScheduledTask newPhaseTask) {
        return new ActiveMeteorEvent(
                eventId, meteorType, center, world, region,
                phase, startTime, phaseStartTime,
                chunkRadius, schematicName, bossMobId, bossDefeated,
                playerDamageMap, vaultOpenedBy, mobSpawnPoints,
                newPhaseTask, hazardTask, particleTask, wgRegionName, originalTerrain
        );
    }

    /**
     * Creates a copy of this event with an updated hazard task reference.
     */
    public @NotNull ActiveMeteorEvent withHazardTask(@Nullable FoliaScheduler.ScheduledTask newHazardTask) {
        return new ActiveMeteorEvent(
                eventId, meteorType, center, world, region,
                phase, startTime, phaseStartTime,
                chunkRadius, schematicName, bossMobId, bossDefeated,
                playerDamageMap, vaultOpenedBy, mobSpawnPoints,
                phaseTask, newHazardTask, particleTask, wgRegionName, originalTerrain
        );
    }

    /**
     * Creates a copy of this event with an updated particle task reference.
     */
    public @NotNull ActiveMeteorEvent withParticleTask(@Nullable FoliaScheduler.ScheduledTask newParticleTask) {
        return new ActiveMeteorEvent(
                eventId, meteorType, center, world, region,
                phase, startTime, phaseStartTime,
                chunkRadius, schematicName, bossMobId, bossDefeated,
                playerDamageMap, vaultOpenedBy, mobSpawnPoints,
                phaseTask, hazardTask, newParticleTask, wgRegionName, originalTerrain
        );
    }

    /**
     * Creates a copy of this event with an updated boss defeated status.
     */
    public @NotNull ActiveMeteorEvent withBossDefeated(boolean defeated) {
        return new ActiveMeteorEvent(
                eventId, meteorType, center, world, region,
                phase, startTime, phaseStartTime,
                chunkRadius, schematicName, bossMobId, defeated,
                playerDamageMap, vaultOpenedBy, mobSpawnPoints,
                phaseTask, hazardTask, particleTask, wgRegionName, originalTerrain
        );
    }

    /**
     * Creates a copy of this event with an updated WG region name.
     */
    public @NotNull ActiveMeteorEvent withWGRegionName(@Nullable String regionName) {
        return new ActiveMeteorEvent(
                eventId, meteorType, center, world, region,
                phase, startTime, phaseStartTime,
                chunkRadius, schematicName, bossMobId, bossDefeated,
                playerDamageMap, vaultOpenedBy, mobSpawnPoints,
                phaseTask, hazardTask, particleTask, regionName, originalTerrain
        );
    }

    /**
     * Creates a copy of this event with original terrain bounds.
     */
    public @NotNull ActiveMeteorEvent withOriginalTerrain(@Nullable BoundingBox terrain) {
        return new ActiveMeteorEvent(
                eventId, meteorType, center, world, region,
                phase, startTime, phaseStartTime,
                chunkRadius, schematicName, bossMobId, bossDefeated,
                playerDamageMap, vaultOpenedBy, mobSpawnPoints,
                phaseTask, hazardTask, particleTask, wgRegionName, terrain
        );
    }

    /**
     * Records damage dealt by a player to the boss.
     *
     * @param playerUUID the player UUID
     * @param damage     the damage amount
     */
    public void recordPlayerDamage(@NotNull UUID playerUUID, double damage) {
        playerDamageMap.merge(playerUUID, damage, Double::sum);
    }

    /**
     * Marks that a player has opened the vault.
     */
    public boolean markVaultOpened(@NotNull UUID playerUUID) {
        return vaultOpenedBy.add(playerUUID);
    }

    /**
     * Checks if a player has already opened the vault.
     */
    public boolean hasPlayerOpenedVault(@NotNull UUID playerUUID) {
        return vaultOpenedBy.contains(playerUUID);
    }

    /**
     * Gets the elapsed time since the event started.
     */
    public long elapsedTimeMs() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Gets the elapsed time since the current phase started.
     */
    public long phaseElapsedTimeMs() {
        return System.currentTimeMillis() - phaseStartTime;
    }

    /**
     * Checks if this event is still in an active phase.
     */
    public boolean isActive() {
        return phase.isActive();
    }
}
