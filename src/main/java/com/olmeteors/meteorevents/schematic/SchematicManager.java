package com.olmeteors.meteorevents.schematic;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.olmeteors.meteorevents.config.ConfigManager;
import com.olmeteors.meteorevents.hook.FAWEHook;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Locale;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manages schematic loading, validation, and asynchronous pasting operations.
 * Integrates with FAWE for high-performance schematic handling.
 */
public final class SchematicManager {

    private static final String VALID_NAME_PATTERN = "[a-z0-9_-]{1,64}";

    private final MeteorPlugin plugin;
    private final ConfigManager configManager;
    private final FAWEHook faweHook;

    public SchematicManager(MeteorPlugin plugin, FAWEHook faweHook) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.faweHook = faweHook;
    }

    /**
     * Pastes a schematic at the given location asynchronously.
     * Performs sanity checks before attempting the paste.
     *
     * @param schematicName the name of the schematic file
     * @param center        the center location for pasting
     * @param world         the world
     * @return true if the schematic was pasted successfully or if no FAWE is available
     */
    public boolean pasteSchematic(@Nullable String schematicName, @NotNull Location center,
                                   @NotNull World world) {
        final CompletableFuture<Boolean> future = pasteSchematicAsync(schematicName, center, world);
        return !future.isDone() || future.getNow(false);
    }

    /** Completes only after FAWE has finished applying every schematic block. */
    public @NotNull CompletableFuture<Boolean> pasteSchematicAsync(
            @Nullable String schematicName, @NotNull Location center, @NotNull World world) {
        if (schematicName == null || schematicName.isEmpty()) {
            plugin.getLogger().info("No schematic configured for this event type.");
            return CompletableFuture.completedFuture(false);
        }

        // Validate schematic file existence
        final File schematicFile = configManager.getSchematicFile(schematicName);
        if (schematicFile == null) {
            plugin.getLogger().warning("Schematic file not found: " + schematicName
                    + ". Skipping schematic paste.");
            return CompletableFuture.completedFuture(false);
        }

        // Check FAWE availability
        if (!faweHook.isAvailable()) {
            plugin.getLogger().warning("FAWE not available. Cannot paste schematic: " + schematicName);
            return CompletableFuture.completedFuture(false);
        }

        // Perform asynchronous paste
        final Location adjustedCenter = adjustedPasteCenter(schematicName, center);
        final CompletableFuture<Boolean> future = faweHook.pasteSchematicAsync(schematicFile, adjustedCenter)
                .exceptionally(throwable -> {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to paste schematic: " + schematicName, throwable);
            return false;
        });

        plugin.getLogger().info("Schematic paste initiated: " + schematicName
                + " at " + formatLocation(center));
        return future;
    }

    /**
     * Validates the existence and format of a schematic file.
     *
     * @param schematicName the schematic file name
     * @return true if the schematic exists and has a valid format
     */
    public boolean validateSchematic(@NotNull String schematicName) {
        final File schematicFile = configManager.getSchematicFile(schematicName);
        if (schematicFile == null) return false;

        // Check file extension
        final String name = schematicFile.getName().toLowerCase();
        return name.endsWith(".schem") || name.endsWith(".schematic")
                || name.endsWith(".nbt");
    }

    /**
     * Gets the schematic file for a given name.
     *
     * @param schematicName the schematic file name
     * @return the File, or null if it doesn't exist
     */
    public @Nullable File getSchematicFile(@NotNull String schematicName) {
        return configManager.getSchematicFile(schematicName);
    }

    public boolean isValidSchematicName(@NotNull String name) {
        final String baseName = stripExtension(name.trim().toLowerCase(Locale.ROOT));
        return baseName.matches(VALID_NAME_PATTERN);
    }

    /** Saves a setup selection into the plugin's schematics folder. */
    public @NotNull CompletableFuture<Boolean> saveSelection(@NotNull String name,
                                                              @NotNull Location first,
                                                              @NotNull Location second) {
        if (!isValidSchematicName(name) || first.getWorld() == null
                || !first.getWorld().equals(second.getWorld()) || !faweHook.isAvailable()) {
            return CompletableFuture.completedFuture(false);
        }

        final String fileName = stripExtension(name.trim().toLowerCase(Locale.ROOT)) + ".schem";
        final File output = configManager.getSchematicsPath().resolve(fileName).toFile();
        return faweHook.saveSchematicAsync(output, first.getWorld(), first, second);
    }

    public @NotNull CompletableFuture<Boolean> clearSelection(@NotNull Location first,
                                                               @NotNull Location second) {
        if (first.getWorld() == null || !first.getWorld().equals(second.getWorld())) {
            return CompletableFuture.completedFuture(false);
        }
        return faweHook.clearAreaAsync(first.getWorld(), first, second);
    }

    /** Returns FAWE's centre after applying the administrator-selected root offset. */
    public @NotNull Location adjustedPasteCenter(@Nullable String schematicName,
                                                  @NotNull Location requestedRoot) {
        if (schematicName == null || schematicName.isBlank()) return requestedRoot.clone();
        return requestedRoot.clone().subtract(configManager.getSchematicRootOffset(schematicName));
    }

    public @NotNull String normalizeFileName(@NotNull String name) {
        return stripExtension(name.trim().toLowerCase(Locale.ROOT)) + ".schem";
    }

    public @NotNull List<String> listSchematics() {
        final File[] files = configManager.getSchematicsPath().toFile().listFiles(file ->
                file.isFile() && (file.getName().toLowerCase(Locale.ROOT).endsWith(".schem")
                        || file.getName().toLowerCase(Locale.ROOT).endsWith(".schematic")));
        if (files == null) {
            return List.of();
        }
        return java.util.Arrays.stream(files)
                .map(File::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public void ensureDefaultSchematic() {
        final File defaultFile = configManager.getSchematicsPath().resolve("meteor_crater.schem").toFile();
        faweHook.createDefaultMeteorSchematicAsync(defaultFile);
    }

    private String stripExtension(String name) {
        if (name.endsWith(".schematic")) {
            return name.substring(0, name.length() - ".schematic".length());
        }
        if (name.endsWith(".schem")) {
            return name.substring(0, name.length() - ".schem".length());
        }
        return name;
    }

    private String formatLocation(Location loc) {
        return String.format("%.0f, %.0f, %.0f (%s)",
                loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
    }
}
