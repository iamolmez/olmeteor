package com.olmeteors.meteorevents.setup;

import com.olmeteors.meteorevents.MeteorPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Provides safe inventory backup and restoration for setup mode.
 * Saves player inventory to disk asynchronously to prevent data loss
 * in case of server crashes or unexpected disconnections.
 * <p>
 * Backup files are stored in data/inventories/<UUID>.yml and are
 * automatically deleted after successful restoration.
 */
public final class SetupInventoryBackup {

    private final MeteorPlugin plugin;
    private final Path inventoriesPath;
    private final Set<UUID> pendingRestorations;

    public SetupInventoryBackup(MeteorPlugin plugin, Path inventoriesPath) {
        this.plugin = plugin;
        this.inventoriesPath = inventoriesPath;
        this.pendingRestorations = ConcurrentHashMap.newKeySet();
    }

    /**
     * Saves a player's complete inventory (including armor and off-hand)
     * to a YAML file asynchronously.
     *
     * @param player the player whose inventory to save
     * @return CompletableFuture that completes when the save is done
     */
    public @NotNull CompletableFuture<Boolean> saveInventoryAsync(@NotNull Player player) {
        // Bukkit inventory/entity state must be captured on the player's thread.
        final ItemStack[] contents = cloneItems(player.getInventory().getContents());
        final ItemStack[] armorContents = cloneItems(player.getInventory().getArmorContents());
        final ItemStack[] extraContents = cloneItems(player.getInventory().getExtraContents());
        final float exp = player.getExp();
        final int level = player.getLevel();
        final double health = player.getHealth();
        final int food = player.getFoodLevel();
        final UUID playerId = player.getUniqueId();
        final String playerName = player.getName();

        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.createDirectories(inventoriesPath);
                final File backupFile = getBackupFile(playerId);
                final YamlConfiguration config = new YamlConfiguration();

                // Save main inventory contents
                for (int i = 0; i < contents.length; i++) {
                    if (contents[i] != null && !contents[i].isEmpty()) {
                        config.set("inventory." + i, contents[i]);
                    }
                }

                // Save armor contents
                for (int i = 0; i < armorContents.length; i++) {
                    if (armorContents[i] != null && !armorContents[i].isEmpty()) {
                        config.set("armor." + i, armorContents[i]);
                    }
                }

                // Save extra contents (off-hand)
                for (int i = 0; i < extraContents.length; i++) {
                    if (extraContents[i] != null && !extraContents[i].isEmpty()) {
                        config.set("extra." + i, extraContents[i]);
                    }
                }

                // Save experience and level
                config.set("exp", exp);
                config.set("level", level);

                // Save health and food
                config.set("health", health);
                config.set("food", food);

                // Write to disk
                config.save(backupFile);

                pendingRestorations.add(playerId);
                plugin.getLogger().info("Inventory backed up for player: "
                        + playerName + " (" + playerId + ")");
                return true;

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE,
                        "Failed to save inventory for player: " + playerName, e);
                return false;
            }
        });
    }

    /**
     * Restores a player's inventory from their backup file.
     * Called on plugin start or on demand.
     *
     * @param player the player to restore
     * @return true if restoration was successful
     */
    public boolean restoreInventory(@NotNull Player player) {
        final File backupFile = getBackupFile(player.getUniqueId());

        if (!backupFile.exists()) {
            return false;
        }

        try {
            final YamlConfiguration config = YamlConfiguration.loadConfiguration(backupFile);

            // Remove every setup item, including slots that were empty in the backup.
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.getInventory().setExtraContents(new ItemStack[player.getInventory().getExtraContents().length]);

            // Restore main inventory
            if (config.contains("inventory")) {
                final var section = config.getConfigurationSection("inventory");
                if (section != null) {
                    for (final String key : section.getKeys(false)) {
                        final int slot = Integer.parseInt(key);
                        final ItemStack item = config.getItemStack("inventory." + key);
                        if (item != null && slot >= 0
                                && slot < player.getInventory().getSize()) {
                            player.getInventory().setItem(slot, item);
                        }
                    }
                }
            }

            // Restore armor contents
            if (config.contains("armor")) {
                final var section = config.getConfigurationSection("armor");
                if (section != null) {
                    final ItemStack[] armor = new ItemStack[4];
                    for (final String key : section.getKeys(false)) {
                        final int slot = Integer.parseInt(key);
                        if (slot >= 0 && slot < armor.length) {
                            armor[slot] = config.getItemStack("armor." + key);
                        }
                    }
                    player.getInventory().setArmorContents(armor);
                }
            }

            // Restore extra contents (off-hand)
            if (config.contains("extra")) {
                final var section = config.getConfigurationSection("extra");
                if (section != null) {
                    final ItemStack[] extra = new ItemStack[
                            player.getInventory().getExtraContents().length];
                    for (final String key : section.getKeys(false)) {
                        final int slot = Integer.parseInt(key);
                        if (slot >= 0 && slot < extra.length) {
                            extra[slot] = config.getItemStack("extra." + key);
                        }
                    }
                    player.getInventory().setExtraContents(extra);
                }
            }

            // Restore experience
            player.setExp((float) config.getDouble("exp", 0.0));
            player.setLevel(config.getInt("level", 0));

            // Restore health and food
            player.setHealth(Math.min(player.getMaxHealth(),
                    config.getDouble("health", 20.0)));
            player.setFoodLevel(config.getInt("food", 20));

            // Delete the backup file after successful restoration
            Files.deleteIfExists(backupFile.toPath());
            pendingRestorations.remove(player.getUniqueId());

            plugin.getLogger().info("Inventory restored for player: "
                    + player.getName());
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to restore inventory for player: " + player.getName(), e);
            return false;
        }
    }

    /**
     * Checks if a player has a pending backup.
     *
     * @param player the player to check
     * @return true if a backup exists
     */
    public boolean hasBackup(@NotNull Player player) {
        return getBackupFile(player.getUniqueId()).exists();
    }

    /**
     * Deletes a player's backup file without restoring.
     *
     * @param player the player
     */
    public void deleteBackup(@NotNull Player player) {
        try {
            Files.deleteIfExists(getBackupFile(player.getUniqueId()).toPath());
            pendingRestorations.remove(player.getUniqueId());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to delete backup for player: " + player.getName(), e);
        }
    }

    /**
     * Gets all players who have pending inventory backups.
     *
     * @return set of UUIDs
     */
    public @NotNull Set<UUID> getPendingRestorations() {
        return Collections.unmodifiableSet(pendingRestorations);
    }

    private @NotNull File getBackupFile(@NotNull UUID playerUUID) {
        return new File(inventoriesPath.toFile(), playerUUID.toString() + ".yml");
    }

    private static ItemStack[] cloneItems(ItemStack[] source) {
        final ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] == null ? null : source[i].clone();
        }
        return copy;
    }
}
