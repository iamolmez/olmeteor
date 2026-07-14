package com.olmeteors.meteorevents.display;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.olmeteors.meteorevents.scheduler.FoliaScheduler;
import com.olmeteors.meteorevents.util.DataComponentUtil;
import com.olmeteors.meteorevents.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Manages Minecraft 1.21+ Display Entities (ItemDisplay and TextDisplay)
 * for visual effects during meteor events including:
 * - 3D rotating meteor core (ItemDisplay)
 * - Event information holograms (TextDisplay)
 * - Warning text displays
 */
public final class DisplayEntityManager {

    private final MeteorPlugin plugin;
    private final Map<String, Set<UUID>> eventEntities;

    // Display entity size constants
    private static final float METEOR_CORE_SCALE = 4.0f;
    private static final float HOLOGRAM_SIZE = 1.0f;
    private static final float FLOATING_AMPLITUDE = 0.3f;
    private static final long ROTATION_PERIOD_TICKS = 80L; // 4 seconds for full rotation

    public DisplayEntityManager(MeteorPlugin plugin) {
        this.plugin = plugin;
        this.eventEntities = new ConcurrentHashMap<>();
    }

    /**
     * Spawns a 3D rotating meteor core ItemDisplay entity above the impact point.
     * The entity slowly rotates and floats up and down for dramatic effect.
     *
     * @param center the center location of the event
     * @return the spawned ItemDisplay entity
     */
    public @NotNull ItemDisplay spawnMeteorCore(@NotNull Location center) {
        return spawnMeteorCore("global", center);
    }

    public @NotNull ItemDisplay spawnMeteorCore(@NotNull String eventId, @NotNull Location center) {
        final World world = center.getWorld();
        final Location spawnLoc = center.clone().add(0, 3.0, 0);

        final ItemDisplay display = (ItemDisplay) world.spawnEntity(spawnLoc, EntityType.ITEM_DISPLAY);

        // Set the meteor core item (fire charge with custom model data)
        display.setItemStack(DataComponentUtil.createMeteorCoreItem(plugin.getConfigManager()));
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);

        // Configure display properties
        display.setDisplayWidth(3.0f);
        display.setDisplayHeight(3.0f);
        display.setViewRange(128.0f);
        display.setShadowRadius(2.0f);
        display.setShadowStrength(1.0f);
        display.setBrightness(new Display.Brightness(15, 15)); // Max brightness (glow)

        // Set transformation for scaling
        final Transformation transformation = new Transformation(
                new Vector3f(0, 0, 0),                      // Translation
                new Quaternionf(0, 0, 0, 1),                // Left rotation
                new Vector3f(METEOR_CORE_SCALE, METEOR_CORE_SCALE, METEOR_CORE_SCALE), // Scale
                new Quaternionf(0, 0, 0, 1)                 // Right rotation
        );
        display.setTransformation(transformation);

        // Start rotation and floating animation
        startCoreAnimation(display, spawnLoc);

        // For non-Folia, add to entity tracking
        addEntityToEvent(eventId, display.getUniqueId());

        plugin.getLogger().info("Meteor core display entity spawned at: "
                + formatLocation(center));
        return display;
    }

    /** Creates a visible meteor that descends from the sky with an accelerating spiral. */
    public void spawnFallingMeteor(@NotNull String eventId, @NotNull Location impact,
                                   long durationTicks, int height, boolean cinematic) {
        final World world = impact.getWorld();
        if (world == null || durationTicks <= 0) return;
        final Location start = impact.clone().add(0, height, 0);
        final ItemDisplay display = (ItemDisplay) world.spawnEntity(start, EntityType.ITEM_DISPLAY);
        display.setItemStack(DataComponentUtil.createMeteorCoreItem(plugin.getConfigManager()));
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
        display.setBrightness(new Display.Brightness(15, 15));
        display.setGlowing(true);
        final float scale = cinematic ? 7.0f : 5.0f;
        display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
                new Vector3f(scale, scale, scale), new Quaternionf()));
        addEntityToEvent(eventId, display.getUniqueId());

        final AtomicLong tick = new AtomicLong();
        final AtomicReference<FoliaScheduler.ScheduledTask> taskRef = new AtomicReference<>();
        taskRef.set(plugin.getFoliaScheduler().runRepeatingForEntity(display, () -> {
            if (!display.isValid()) {
                plugin.getFoliaScheduler().cancelTask(taskRef.get());
                return;
            }
            final long current = tick.incrementAndGet();
            final double progress = Math.min(1.0, current / (double) durationTicks);
            final double eased = progress * progress;
            final double remaining = 1.0 - eased;
            final double spiralRadius = (cinematic ? 8.0 : 4.0) * remaining;
            final double angle = progress * Math.PI * (cinematic ? 10.0 : 6.0);
            final Location location = impact.clone().add(
                    Math.cos(angle) * spiralRadius,
                    height * remaining + 2.0,
                    Math.sin(angle) * spiralRadius);
            display.teleportAsync(location);
            world.spawnParticle(org.bukkit.Particle.FLAME, location, cinematic ? 18 : 10,
                    0.7, 1.0, 0.7, 0.03);
            world.spawnParticle(org.bukkit.Particle.LARGE_SMOKE, location, cinematic ? 8 : 4,
                    0.5, 0.8, 0.5, 0.02);
            if (current % 20 == 0) {
                world.playSound(location, org.bukkit.Sound.ENTITY_BLAZE_SHOOT,
                        cinematic ? 2.5f : 1.5f, (float) (0.55 + progress * 0.6));
            }
            if (current >= durationTicks) {
                display.remove();
                plugin.getFoliaScheduler().cancelTask(taskRef.get());
            }
        }, 0L, 1L));
    }

    /**
     * Starts the animation loop for the meteor core display entity.
     * Handles rotation and floating movement.
     *
     * @param display  the ItemDisplay entity
     * @param original the original spawn location
     */
    private void startCoreAnimation(@NotNull ItemDisplay display, @NotNull Location original) {
        final long startTick = plugin.getServer().getCurrentTick();

        // Schedule periodic updates for animation using FoliaScheduler
        final AtomicReference<FoliaScheduler.ScheduledTask> taskRef = new AtomicReference<>();
        taskRef.set(plugin.getFoliaScheduler().runRepeatingForEntity(display, () -> {
            if (!display.isValid()) {
                plugin.getFoliaScheduler().cancelTask(taskRef.get());
                return;
            }

            final long tick = plugin.getServer().getCurrentTick() - startTick;

            // Calculate rotation (full rotation every ROTATION_PERIOD_TICKS)
            final float angle = (float) (2 * Math.PI * (tick % ROTATION_PERIOD_TICKS)
                    / ROTATION_PERIOD_TICKS);
            final Quaternionf rotation = new Quaternionf().rotationY(angle);

            // Calculate floating motion (up and down)
            final float floatOffset = (float) (Math.sin(tick * 0.1) * FLOATING_AMPLITUDE);
            final Vector3f translation = new Vector3f(0, floatOffset, 0);

            // Apply transformation
            display.setTransformation(new Transformation(
                    translation,
                    new Quaternionf(0, 0, 0, 1),
                    new Vector3f(METEOR_CORE_SCALE, METEOR_CORE_SCALE, METEOR_CORE_SCALE),
                    rotation
            ));

        }, 0L, 1L)); // Update every tick for smooth animation
    }

    /**
     * Spawns an informational hologram (TextDisplay) at the given location.
     *
     * @param location the location for the hologram
     * @param text     the text to display (supports MiniMessage format)
     * @return the spawned TextDisplay entity
     */
    public @NotNull TextDisplay spawnHologram(@NotNull Location location, @NotNull String text) {
        final World world = location.getWorld();

        final TextDisplay display = (TextDisplay) world.spawnEntity(location, EntityType.TEXT_DISPLAY);

        // Set text content
        display.text(MessageUtil.parse(text));

        // Configure display properties
        display.setDisplayWidth(5.0f);
        display.setDisplayHeight(2.0f);
        display.setViewRange(64.0f);
        display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0)); // Transparent
        display.setShadowed(true);
        display.setSeeThrough(false);

        // Make text always face the player
        display.setBillboard(Display.Billboard.CENTER);

        return display;
    }

    /**
     * Creates a warning hologram above the impact zone.
     *
     * @param center  the center location
     * @param message the warning message
     */
    public void spawnWarningHologram(@NotNull Location center, @NotNull String message) {
        final Location hologramLoc = center.clone().add(0, 8, 0);
        final TextDisplay display = spawnHologram(hologramLoc, message);

        // Pulse animation using scheduler
        final AtomicReference<FoliaScheduler.ScheduledTask> taskRef = new AtomicReference<>();
        taskRef.set(plugin.getFoliaScheduler().runRepeatingForEntity(display, () -> {
            if (!display.isValid()) {
                plugin.getFoliaScheduler().cancelTask(taskRef.get());
                return;
            }
            // Alternate opacity for pulsing effect
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(1.0f, 1.0f, 1.0f),
                    new Quaternionf()
            ));
        }, 1L, 10L));
    }

    /**
     * Creates the key count display above the vault.
     * Text content is loaded from locale (config.yml → messages.display.vault_hologram).
     *
     * @param location the vault location
     * @param keyCount the number of keys
     */
    public void spawnKeyCountDisplay(@NotNull Location location, int keyCount) {
        spawnKeyCountDisplay("global", location, keyCount);
    }

    public void spawnKeyCountDisplay(@NotNull String eventId, @NotNull Location location, int keyCount) {
        final Location displayLoc = location.clone().add(0, 2.5, 0);
        final String text = plugin.getConfigManager()
                .getMessage("display.vault_hologram",
                        "count", String.valueOf(keyCount));
        final TextDisplay display = spawnHologram(displayLoc, text);

        display.setBillboard(Display.Billboard.CENTER);
        display.setDisplayWidth(2.0f);
        display.setDisplayHeight(1.0f);

        addEntityToEvent(eventId, display.getUniqueId());
    }

    /**
     * Updates the text of an existing hologram.
     *
     * @param display the TextDisplay entity
     * @param text    the new text
     */
    public void updateHologram(@NotNull TextDisplay display, @NotNull String text) {
        if (display.isValid()) {
            display.text(MessageUtil.parse(text));
        }
    }

    /**
     * Tracks an entity as part of a specific event for cleanup.
     *
     * @param eventId  the event ID
     * @param entityId the entity UUID
     */
    public void addEntityToEvent(@NotNull String eventId, @NotNull UUID entityId) {
        eventEntities.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet()).add(entityId);
    }

    /**
     * Removes all display entities associated with a specific event.
     *
     * @param eventId the event ID
     */
    public void removeEventEntities(@NotNull String eventId) {
        final Set<UUID> entities = eventEntities.remove(eventId);
        if (entities != null) {
            for (final UUID entityId : entities) {
                final Entity entity = plugin.getServer().getEntity(entityId);
                if (entity != null) plugin.getFoliaScheduler().runForEntity(entity, () -> {
                    if (entity.isValid()) entity.remove();
                });
            }
            plugin.getLogger().info("Removed " + entities.size()
                    + " display entities for event: " + eventId);
        }
    }

    /**
     * Removes all tracked display entities (called on plugin disable).
     */
    public void removeAllEntities() {
        for (final var entry : eventEntities.entrySet()) {
            removeEventEntities(entry.getKey());
        }
        eventEntities.clear();
    }

    private String formatLocation(Location loc) {
        return String.format("%.0f, %.0f, %.0f",
                loc.getX(), loc.getY(), loc.getZ());
    }
}
