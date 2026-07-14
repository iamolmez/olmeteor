package com.olmeteors.meteorevents.setup;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.olmeteors.meteorevents.config.ConfigManager;
import com.olmeteors.meteorevents.display.DisplayEntityManager;
import com.olmeteors.meteorevents.event.MeteorType;
import com.olmeteors.meteorevents.scheduler.FoliaScheduler;
import com.olmeteors.meteorevents.util.DataComponentUtil;
import com.olmeteors.meteorevents.util.MessageUtil;
import com.olmeteors.meteorevents.util.ParticleUtil;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages the visual setup mode for administrators to configure meteor events.
 * Provides a setup kit with specialized tools for region selection,
 * mob spawn point marking, loot editing, and safe exit.
 */
public final class SetupManager implements Listener {

    private final MeteorPlugin plugin;
    private final ConfigManager configManager;
    private final DisplayEntityManager displayEntityManager;
    private final ParticleUtil particleUtil;
    private final SetupInventoryBackup inventoryBackup;

    private final Map<UUID, SetupSession> activeSessions;
    private final Map<UUID, SetupVisual> activeVisuals;
    private final Map<UUID, MobChanceSession> mobChanceEditors = new ConcurrentHashMap<>();
    private final Map<UUID, String> editPreviewSnapshots = new ConcurrentHashMap<>();

    private static final int SETUP_WAND_SLOT = 0;
    private static final int MOB_SPAWN_TOOL_SLOT = 1;
    private static final int LOOT_EDITOR_SLOT = 2;
    private static final int HOLOGRAM_TOOL_SLOT = 3;
    private static final int ROOT_TOOL_SLOT = 4;
    private static final int CANCEL_EXIT_SLOT = 7;
    private static final int SAFE_EXIT_SLOT = 8;

    public SetupManager(MeteorPlugin plugin, ConfigManager configManager,
                        DisplayEntityManager displayEntityManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.displayEntityManager = displayEntityManager;
        this.particleUtil = new ParticleUtil(plugin);
        this.inventoryBackup = new SetupInventoryBackup(plugin,
                configManager.getInventoriesPath());
        this.activeSessions = new ConcurrentHashMap<>();
        this.activeVisuals = new ConcurrentHashMap<>();
    }

    /**
     * Puts a player into setup mode for a specific meteor type.
     * Backs up their inventory, clears it, and gives them the setup kit.
     *
     * @param player the player
     * @param type   the meteor type to configure
     */
    public void enterSetupMode(@NotNull Player player, @NotNull MeteorType type) {
        final UUID playerUUID = player.getUniqueId();

        // Exit any existing setup mode first
        if (activeSessions.containsKey(playerUUID)) {
            exitSetupMode(player);
        }

        // Backup current inventory asynchronously
        inventoryBackup.saveInventoryAsync(player).whenComplete((success, error) ->
                plugin.getFoliaScheduler().runForEntity(player, () -> {
            if (error != null || !Boolean.TRUE.equals(success)) {
                configManager.sendMessage(player, "setup.backup_failed");
                return;
            }

            // Clear inventory and give setup kit
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);

            // Give setup items
            giveSetupKit(player);

            // Create setup session
            final SetupSession session = new SetupSession(type, player.getLocation());
            activeSessions.put(playerUUID, session);

            // Register command blocker
            plugin.getSetupCommandBlocker().addBlockedPlayer(player);

            // Start visual effects
            startSetupVisuals(player, session);

            configManager.sendMessage(player, "setup.entered", "type", configManager.getMeteorTypeName(type));

            updateActionBar(player, session);
        }));
    }

    /** Pastes a saved schematic in front of the player and opens it for editing. */
    public void editExistingSchematic(@NotNull Player player, @NotNull MeteorType type,
                                      @NotNull String requestedName) {
        final Optional<String> existing = plugin.getSchematicManager().listSchematics().stream()
                .filter(name -> name.equalsIgnoreCase(requestedName)).findFirst();
        if (existing.isEmpty()) {
            configManager.sendMessage(player, "setup.choice.not_found", "name", requestedName);
            return;
        }

        enterSetupMode(player, type);
        plugin.getFoliaScheduler().runLaterAtLocation(player.getLocation(), () -> {
            final SetupSession current = activeSessions.get(player.getUniqueId());
            if (current == null) return;
            final Location pasteAt = player.getLocation().clone()
                    .add(player.getLocation().getDirection().setY(0).normalize().multiply(8))
                    .toBlockLocation();
            final String snapshotId = "setup-preview-" + player.getUniqueId();
            final int previewRadius = configManager.getTypeConfig(type).impactRadius();
            final Location previewPasteCenter = plugin.getSchematicManager()
                    .adjustedPasteCenter(existing.get(), pasteAt);
            plugin.getRollbackSystem().captureSchematicArea(snapshotId, previewPasteCenter,
                    plugin.getSchematicManager().getSchematicFile(existing.get()), previewRadius * 2);
            editPreviewSnapshots.put(player.getUniqueId(), snapshotId);
            final boolean pasted = plugin.getSchematicManager()
                    .pasteSchematic(existing.get(), pasteAt, player.getWorld());
            if (pasted) {
                final List<org.bukkit.util.Vector> storedMobs =
                        configManager.getSchematicMobOffsets(existing.get());
                final List<org.bukkit.util.Vector> storedHolograms =
                        configManager.getSchematicHologramOffsets(existing.get());
                final List<org.bukkit.util.Vector> storedChests =
                        configManager.getSchematicChestOffsets(existing.get());
                final SetupSession loaded = current.withSchematicName(existing.get())
                        .withPasteAnchor(pasteAt)
                        .withSavedPoints(
                                locationsFromOffsets(pasteAt, storedMobs.isEmpty()
                                        ? configManager.getMobSpawnOffsets(type) : storedMobs),
                                locationsFromOffsets(pasteAt, storedHolograms.isEmpty()
                                        ? configManager.getHologramOffsets(type) : storedHolograms),
                                locationsFromOffsets(pasteAt, storedChests.isEmpty()
                                        ? configManager.getChestOffsets(type) : storedChests));
                activeSessions.put(player.getUniqueId(), loaded);
                loaded.chestPoints().forEach(chest -> {
                    if (chest.getWorld() != null) {
                        chest.getBlock().setType(configManager.getLootBlockMaterial(type), false);
                    }
                });
                final SetupVisual oldVisual = activeVisuals.remove(player.getUniqueId());
                if (oldVisual != null) oldVisual.stop();
                startSetupVisuals(player, loaded);
                updateActionBar(player, loaded);
                MessageUtil.sendMessage(player, "&aSchematic ve kayıtlı noktalar yüklendi. &eMob: "
                        + loaded.mobSpawnPoints().size() + " &7| &eSandık: "
                        + loaded.chestPoints().size());
            } else {
                plugin.getRollbackSystem().rollbackEvent(snapshotId);
                editPreviewSnapshots.remove(player.getUniqueId());
                MessageUtil.sendMessage(player, "&cSchematic düzenleme alanına yerleştirilemedi.");
            }
        }, 10L);
    }

    /** Shows clickable choices for creating a setup or selecting an existing schematic. */
    public void showSetupChoice(@NotNull Player player, @NotNull MeteorType type) {
        configManager.sendMessage(player, "setup.choice.title",
                "type", configManager.getMeteorTypeName(type));

        final String typeName = type.name().toLowerCase(Locale.ROOT);
        final Component createButton = MessageUtil.parse(configManager.getMessage("setup.choice.create_new"))
                .clickEvent(ClickEvent.runCommand("/olmeteor setupnew " + typeName))
                .hoverEvent(HoverEvent.showText(MessageUtil.parse(
                        configManager.getMessage("setup.choice.create_new_hover"))));
        player.sendMessage(createButton);

        final List<String> schematics = plugin.getSchematicManager().listSchematics();
        if (schematics.isEmpty()) {
            configManager.sendMessage(player, "setup.choice.no_schematics");
            return;
        }

        configManager.sendMessage(player, "setup.choice.existing_title");
        for (String schematic : schematics) {
            final Component useButton = MessageUtil.parse(configManager.getMessage(
                            "setup.choice.existing_entry", "name", schematic))
                    .clickEvent(ClickEvent.runCommand(
                            "/olmeteor useschematic " + typeName + " " + schematic))
                    .hoverEvent(HoverEvent.showText(MessageUtil.parse(configManager.getMessage(
                            "setup.choice.existing_hover", "name", schematic))));
            final Component editButton = MessageUtil.parse(" &8[&eDÜZENLE&8]")
                    .clickEvent(ClickEvent.runCommand(
                            "/olmeteor editschematic " + typeName + " " + schematic))
                    .hoverEvent(HoverEvent.showText(MessageUtil.parse(
                            "&eYanına yapıştır ve düzenleme modunu aç")));
            player.sendMessage(useButton.append(editButton));
        }
    }

    /** Assigns an existing schematic to a meteor type without entering edit mode. */
    public void useExistingSchematic(@NotNull Player player, @NotNull MeteorType type,
                                     @NotNull String requestedName) {
        final Optional<String> existing = plugin.getSchematicManager().listSchematics().stream()
                .filter(name -> name.equalsIgnoreCase(requestedName))
                .findFirst();
        if (existing.isEmpty()) {
            configManager.sendMessage(player, "setup.choice.not_found", "name", requestedName);
            return;
        }

        if (configManager.setSchematicForType(type, existing.get())) {
            configManager.applySchematicSetupToType(existing.get(), type);
            configManager.sendMessage(player, "setup.choice.assigned",
                    "name", existing.get(),
                    "type", configManager.getMeteorTypeName(type));
        } else {
            configManager.sendMessage(player, "setup.schematic.config_failed", "name", existing.get());
        }
    }

    public void selectMythicMob(@NotNull Player player, @NotNull String requestedId) {
        final SetupSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            MessageUtil.sendMessage(player, "&cÖnce /olmeteor setup <tür> ile setup moduna gir.");
            return;
        }
        final Optional<String> actual = plugin.getMythicMobsHook().getMobIds().stream()
                .filter(id -> id.equalsIgnoreCase(requestedId)).findFirst();
        if (actual.isEmpty()) {
            MessageUtil.sendMessage(player, "&cMythicMobs mobu bulunamadı: &e" + requestedId);
            return;
        }
        if (!session.selectedMobIds().contains(actual.get())) session.selectedMobIds().add(actual.get());
        MessageUtil.sendMessage(player, "&aMeteor mob listesine eklendi: &e" + actual.get());
    }

    public void setHologramText(@NotNull Player player, @NotNull String text) {
        final SetupSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            MessageUtil.sendMessage(player, "&cÖnce setup moduna gir.");
            return;
        }
        configManager.setHologramText(session.meteorType(), text.replace("\\n", "\n"));
        MessageUtil.sendMessage(player, "&aMeteor yazısı ayarlandı: &f" + text);
    }

    /**
     * Exits setup mode, restores inventory, and cleans up visuals.
     */
    public void exitSetupMode(@NotNull Player player) {
        exitSetupMode(player, true);
    }

    private void exitSetupMode(@NotNull Player player, boolean restorePreview) {
        final UUID playerUUID = player.getUniqueId();
        final SetupSession session = activeSessions.remove(playerUUID);

        final String previewSnapshot = editPreviewSnapshots.remove(playerUUID);
        if (previewSnapshot != null) {
            if (restorePreview) plugin.getRollbackSystem().rollbackEvent(previewSnapshot);
            else plugin.getRollbackSystem().discardSnapshot(previewSnapshot);
        }

        // Remove visual effects
        final SetupVisual visual = activeVisuals.remove(playerUUID);
        if (visual != null) {
            visual.stop();
        }

        // Remove command blocking
        plugin.getSetupCommandBlocker().removeBlockedPlayer(player);

        // Restore inventory
        if (inventoryBackup.restoreInventory(player)) {
            player.updateInventory();
            configManager.sendMessage(player, "setup.exited");
        } else {
            // If backup failed, just clear the inventory
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
            configManager.sendMessage(player, "setup.exit_failed");
        }
    }

    public void finishSetup(@NotNull Player player, boolean deleteArea) {
        final SetupSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            MessageUtil.sendMessage(player, "&cAktif setup oturumun yok.");
            return;
        }
        final String preview = editPreviewSnapshots.get(player.getUniqueId());
        if (deleteArea && preview != null) {
            editPreviewSnapshots.remove(player.getUniqueId());
            plugin.getRollbackSystem().rollbackEvent(preview);
        } else if (deleteArea && session.pos1() != null && session.pos2() != null) {
            plugin.getSchematicManager().clearSelection(session.pos1(), session.pos2());
        }
        exitSetupMode(player, false);
        MessageUtil.sendMessage(player, deleteArea
                ? "&aSetup tamamlandı; çalışma alanı temizleniyor."
                : "&aSetup tamamlandı; yapı arazide bırakıldı.");
    }

    private void askSetupCleanup(@NotNull Player player) {
        MessageUtil.sendMessage(player, "&6&lSetup kaydedildi. &eYapı araziden silinsin mi?");
        final Component remove = MessageUtil.parse("&8[&cSİL VE ÇIK&8]")
                .clickEvent(ClickEvent.runCommand("/olmeteor setupfinish sil"));
        final Component keep = MessageUtil.parse(" &8[&aBIRAK VE ÇIK&8]")
                .clickEvent(ClickEvent.runCommand("/olmeteor setupfinish birak"));
        player.sendMessage(remove.append(keep));
    }

    /**
     * Forces all players to exit setup mode (called on plugin disable).
     */
    public void exitAllSetupModes() {
        for (final Player player : plugin.getServer().getOnlinePlayers()) {
            if (activeSessions.containsKey(player.getUniqueId())) {
                exitSetupMode(player);
            }
        }
    }

    /**
     * Gives the setup kit to a player.
     * Uses 1.21 Data Components for styled items.
     */
    private void giveSetupKit(@NotNull Player player) {
        final Inventory inv = player.getInventory();

        inv.setItem(SETUP_WAND_SLOT, DataComponentUtil.createSetupWand(configManager));
        inv.setItem(MOB_SPAWN_TOOL_SLOT, DataComponentUtil.createMobSpawnTool(configManager));
        inv.setItem(LOOT_EDITOR_SLOT, DataComponentUtil.createLootEditor(configManager));
        inv.setItem(HOLOGRAM_TOOL_SLOT, DataComponentUtil.createHologramTool(configManager));
        final ItemStack rootTool = new ItemStack(Material.RECOVERY_COMPASS);
        final var rootMeta = rootTool.getItemMeta();
        rootMeta.displayName(MessageUtil.parse("&b&lSchematic Ana Kök Bloğu"));
        rootMeta.lore(List.of(MessageUtil.parse("&7Sağ tık: meteorun merkezine gelecek bloğu seç")));
        rootTool.setItemMeta(rootMeta);
        inv.setItem(ROOT_TOOL_SLOT, rootTool);
        inv.setItem(CANCEL_EXIT_SLOT, DataComponentUtil.createCancelExit(configManager));
        inv.setItem(SAFE_EXIT_SLOT, DataComponentUtil.createSafeExit(configManager));

        // Fill remaining slots with glass panes as separators
        final ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        final var fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(MessageUtil.parse(
                configManager.getMessage("setup.tool_slot_filler")));
        filler.setItemMeta(fillerMeta);

        for (int i = 9; i < 36; i++) {
            inv.setItem(i, filler);
        }
    }

    /**
     * Gives just the setup wand to a player who already has items.
     *
     * @param player the player
     */
    public void giveWandToPlayer(@NotNull Player player) {
        final HashMap<Integer, ItemStack> remaining = player.getInventory()
                .addItem(DataComponentUtil.createSetupWand(configManager));
        if (!remaining.isEmpty()) {
            player.getWorld().dropItem(player.getLocation(),
                    DataComponentUtil.createSetupWand(configManager));
        }
        configManager.sendMessage(player, "command.wand.success");
    }

    /** Saves the current setup cuboid as a schematic and assigns it to its meteor type. */
    public void saveSelectedSchematic(@NotNull Player player, @NotNull String requestedName) {
        saveSelectedSchematic(player, requestedName, null);
    }

    private void saveSelectedSchematic(@NotNull Player player, @NotNull String requestedName,
                                       @Nullable Runnable afterSave) {
        final SetupSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            configManager.sendMessage(player, "setup.schematic.not_in_setup");
            if (afterSave != null) afterSave.run();
            return;
        }
        if (session.pos1() == null || session.pos2() == null) {
            configManager.sendMessage(player, "setup.schematic.select_region_first");
            if (afterSave != null) afterSave.run();
            return;
        }
        if (!Objects.equals(session.pos1().getWorld(), session.pos2().getWorld())) {
            configManager.sendMessage(player, "setup.schematic.same_world_required");
            if (afterSave != null) afterSave.run();
            return;
        }

        final var schematicManager = plugin.getSchematicManager();
        if (!schematicManager.isValidSchematicName(requestedName)) {
            configManager.sendMessage(player, "setup.schematic.invalid_name");
            if (afterSave != null) afterSave.run();
            return;
        }
        if (!plugin.getFAWEHook().isAvailable()) {
            configManager.sendMessage(player, "setup.schematic.fawe_required");
            if (afterSave != null) afterSave.run();
            return;
        }

        final String fileName = schematicManager.normalizeFileName(requestedName);
        configManager.sendMessage(player, "setup.schematic.saving", "name", fileName);
        schematicManager.saveSelection(requestedName, session.pos1(), session.pos2())
                .whenComplete((success, error) -> plugin.getFoliaScheduler().runForEntity(player, () -> {
                    if (error != null || !Boolean.TRUE.equals(success)) {
                        configManager.sendMessage(player, "setup.schematic.failed", "name", fileName);
                        if (afterSave != null) afterSave.run();
                        return;
                    }
                    if (!configManager.setSchematicForType(session.meteorType(), fileName)) {
                        configManager.sendMessage(player, "setup.schematic.config_failed", "name", fileName);
                        if (afterSave != null) afterSave.run();
                        return;
                    }
                    saveSetupOffsets(session, fileName);
                    configManager.sendMessage(player, "setup.schematic.saved",
                            "name", fileName,
                            "type", configManager.getMeteorTypeName(session.meteorType()));
                    if (afterSave != null) afterSave.run();
                }));
    }

    // ---- Event Handlers ----

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final UUID playerUUID = player.getUniqueId();
        final SetupSession session = activeSessions.get(playerUUID);

        if (session == null) return;

        final ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        event.setCancelled(true);

        final Material type = item.getType();
        final boolean leftClick = event.getAction() == Action.LEFT_CLICK_BLOCK
                || event.getAction() == Action.LEFT_CLICK_AIR;
        final boolean rightClick = event.getAction() == Action.RIGHT_CLICK_BLOCK
                || event.getAction() == Action.RIGHT_CLICK_AIR;

        if (type == Material.BLAZE_ROD) {
            // Region selection tool
            handleWandClick(player, session, event, leftClick, rightClick);
        } else if (type == Material.END_ROD) {
            // Mob spawn point tool
            handleMobSpawnClick(player, session, event, rightClick);
        } else if (type == Material.CHEST) {
            // Loot editor
            handleLootEditorClick(player, session, event);
        } else if (type == Material.NAME_TAG && rightClick && event.getClickedBlock() != null) {
            final Location hologram = event.getClickedBlock().getLocation().add(0.5, 2.0, 0.5);
            session.hologramPoints().add(hologram);
            MessageUtil.sendMessage(player, "&aYazı konumu eklendi: &e" + formatLocation(hologram));
        } else if (type == Material.RECOVERY_COMPASS && rightClick
                && event.getClickedBlock() != null) {
            final Location root = event.getClickedBlock().getLocation();
            activeSessions.put(playerUUID, session.withPasteAnchor(root));
            MessageUtil.sendMessage(player, "&aSchematic ana kök bloğu seçildi: &e"
                    + formatLocation(root));
        } else if (type == Material.BARRIER) {
            exitSetupMode(player);
        } else if (type == Material.BEDROCK) {
            // Save selection/configuration and exit.
            if (!session.selectedMobIds().isEmpty()) {
                configManager.setMythicMobsForType(session.meteorType(), session.selectedMobIds());
            }
            saveSetupOffsets(session);
            if (session.pos1() != null && session.pos2() != null) {
                final String saveName = session.schematicName() != null
                        ? session.schematicName()
                        : session.meteorType().name().toLowerCase(Locale.ROOT) + "_meteor.schem";
                saveSelectedSchematic(player, saveName, () -> askSetupCleanup(player));
                return;
            }
            askSetupCleanup(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        final MobChanceSession chanceSession = mobChanceEditors.get(player.getUniqueId());
        if (chanceSession != null && event.getView().getTopInventory().equals(chanceSession.inventory())) {
            event.setCancelled(true);
            final int slot = event.getRawSlot();
            if (slot >= 0 && slot < chanceSession.mobIds().size() && slot < 45) {
                final String mobId = chanceSession.mobIds().get(slot);
                final double current = chanceSession.chances().getOrDefault(mobId, 0.0);
                final double next = current >= 100.0 ? 0.0 : current + 25.0;
                if (next <= 0) chanceSession.chances().remove(mobId);
                else chanceSession.chances().put(mobId, next);
                refreshMobChanceItem(chanceSession, slot);
            } else if (slot == 53) {
                configManager.setMythicMobChances(chanceSession.type(), chanceSession.chances());
                mobChanceEditors.remove(player.getUniqueId());
                player.closeInventory();
                MessageUtil.sendMessage(player, "&aMob çıkma şansları kaydedildi.");
            }
            return;
        }
        final UUID playerUUID = player.getUniqueId();
        final SetupSession session = activeSessions.get(playerUUID);

        if (session == null) return;

        // Prevent modification of setup kit items in hotbar slots
        if (event.getClickedInventory() != null
                && event.getClickedInventory().equals(player.getInventory())
                && event.getSlot() >= 0 && event.getSlot() <= 8) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        final MobChanceSession session = mobChanceEditors.remove(player.getUniqueId());
        if (session != null && event.getInventory().equals(session.inventory())) {
            configManager.setMythicMobChances(session.type(), session.chances());
        }
    }

    // ---- Tool Handlers ----

    private void handleWandClick(Player player, SetupSession session,
                                  PlayerInteractEvent event, boolean leftClick, boolean rightClick) {
        final Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        final Location loc = clickedBlock.getLocation();
        final UUID playerUUID = player.getUniqueId();
        SetupSession current = session; // use local variable instead of reassigning parameter

        if (leftClick) {
            final SetupSession updated = current.withPos1(loc);
            activeSessions.put(playerUUID, updated);
            configManager.sendMessage(player, "setup.pos1_set",
                    "location", formatLocation(loc));
            current = updated;
        } else if (rightClick) {
            final SetupSession updated = current.withPos2(loc);
            activeSessions.put(playerUUID, updated);
            configManager.sendMessage(player, "setup.pos2_set",
                    "location", formatLocation(loc));
            current = updated;
        }

        // Update bounding box if both positions are set
        if (current.pos1() != null && current.pos2() != null) {
            final SetupSession withBb = current.withBoundingBox(
                    BoundingBox.of(current.pos1(), current.pos2()));
            activeSessions.put(playerUUID, withBb);
            current = withBb;
            configManager.sendMessage(player, "setup.region_selected",
                    "size", getRegionSize(current.boundingBox()));
        }

        updateActionBar(player, current);
    }

    private void handleMobSpawnClick(Player player, SetupSession session,
                                      PlayerInteractEvent event, boolean rightClick) {
        if (!rightClick) return;

        final Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        final Location spawnLoc = clickedBlock.getLocation().add(0.5, 1, 0.5);
        final Optional<Location> existing = session.mobSpawnPoints().stream()
                .filter(point -> point.getWorld().equals(spawnLoc.getWorld())
                        && point.distanceSquared(spawnLoc) < 0.1).findFirst();
        if (existing.isPresent()) {
            session.mobSpawnPoints().remove(existing.get());
            MessageUtil.sendMessage(player, "&cMob doğma noktası silindi: &e" + formatLocation(spawnLoc));
            updateActionBar(player, session);
            return;
        }
        session.mobSpawnPoints().add(spawnLoc);

        configManager.sendMessage(player, "setup.mob_spawn_added",
                "location", formatLocation(spawnLoc));
        configManager.sendMessage(player, "setup.mob_spawn_total",
                "count", String.valueOf(session.mobSpawnPoints().size()));

        // Visual feedback - vertical beam at spawn point
        final World world = spawnLoc.getWorld();
        particleUtil.spawnVerticalBeam(world, spawnLoc.getX(), spawnLoc.getZ(),
                spawnLoc.getY() - 1, spawnLoc.getY() + 5,
                new Particle.DustOptions(
                        org.bukkit.Color.fromRGB(255, 85, 255), 1.5f
                ));

        updateActionBar(player, session);
        openMobChanceEditor(player, session.meteorType());
    }

    private void handleLootEditorClick(Player player, SetupSession session,
                                        PlayerInteractEvent event) {
        if (player.isSneaking() && event.getClickedBlock() != null
                && event.getClickedBlock().getState() instanceof Container container) {
            final int imported = plugin.getLootGUIEditor().importFromInventory(
                    session.meteorType(), container.getInventory());
            MessageUtil.sendMessage(player, "&aSandıktan &e" + imported
                    + " &aadet loot içeriği aktarıldı. ItemsAdder verileri korunur.");
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            final Location chestLocation = event.getClickedBlock().getRelative(event.getBlockFace())
                    .getLocation();
            final Optional<Location> existing = session.chestPoints().stream()
                    .filter(point -> point.getWorld().equals(chestLocation.getWorld())
                            && point.getBlockX() == chestLocation.getBlockX()
                            && point.getBlockY() == chestLocation.getBlockY()
                            && point.getBlockZ() == chestLocation.getBlockZ()).findFirst();
            if (existing.isPresent()) {
                session.chestPoints().remove(existing.get());
                chestLocation.getBlock().setType(Material.AIR, false);
                MessageUtil.sendMessage(player, "&cÖdül sandığı konumu silindi: &e"
                        + formatLocation(chestLocation));
                return;
            }
            chestLocation.getBlock().setType(
                    configManager.getLootBlockMaterial(session.meteorType()), false);
            session.chestPoints().add(chestLocation);
            MessageUtil.sendMessage(player, "&aÖdül sandığı konumu eklendi: &e"
                    + formatLocation(chestLocation));
            return;
        }
        plugin.getLootGUIEditor().openEditor(player, session.meteorType());
    }

    private void openMobChanceEditor(Player player, MeteorType type) {
        final List<String> ids = plugin.getMythicMobsHook().getMobIds().stream().limit(45).toList();
        final Inventory inventory = Bukkit.createInventory(null, 54, "MythicMob Çıkma Şansları");
        final MobChanceSession session = new MobChanceSession(type, inventory, ids,
                new LinkedHashMap<>(configManager.getMythicMobChances(type)));
        mobChanceEditors.put(player.getUniqueId(), session);
        for (int slot = 0; slot < ids.size(); slot++) refreshMobChanceItem(session, slot);
        final ItemStack save = new ItemStack(Material.EMERALD_BLOCK);
        final var saveMeta = save.getItemMeta();
        saveMeta.displayName(MessageUtil.parse("&a&lKaydet"));
        save.setItemMeta(saveMeta);
        inventory.setItem(53, save);
        player.openInventory(inventory);
    }

    private @NotNull List<Location> locationsFromOffsets(
            @NotNull Location anchor, @NotNull List<org.bukkit.util.Vector> offsets) {
        return offsets.stream().map(offset -> anchor.clone().add(offset)).collect(
                java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private void refreshMobChanceItem(MobChanceSession session, int slot) {
        final String id = session.mobIds().get(slot);
        final double chance = session.chances().getOrDefault(id, 0.0);
        final ItemStack item = new ItemStack(chance > 0 ? Material.ZOMBIE_SPAWN_EGG : Material.PAPER);
        final var meta = item.getItemMeta();
        meta.displayName(MessageUtil.parse("&e" + id));
        meta.lore(List.of(MessageUtil.parse("&7Çıkma ağırlığı: &a%" + (int) chance),
                MessageUtil.parse("&8Tıklayarak: 0 → 25 → 50 → 75 → 100")));
        item.setItemMeta(meta);
        session.inventory().setItem(slot, item);
    }

    private void saveSetupOffsets(@NotNull SetupSession session) {
        saveSetupOffsets(session, session.schematicName());
    }

    private void saveSetupOffsets(@NotNull SetupSession session, @Nullable String schematicName) {
        final Location geometricCenter;
        if (session.boundingBox() != null && session.pos1() != null
                && session.pos1().getWorld() != null) {
            final int minX = Math.min(session.pos1().getBlockX(), session.pos2().getBlockX());
            final int minY = Math.min(session.pos1().getBlockY(), session.pos2().getBlockY());
            final int minZ = Math.min(session.pos1().getBlockZ(), session.pos2().getBlockZ());
            final int sizeX = Math.abs(session.pos1().getBlockX() - session.pos2().getBlockX()) + 1;
            final int sizeZ = Math.abs(session.pos1().getBlockZ() - session.pos2().getBlockZ()) + 1;
            geometricCenter = new Location(session.pos1().getWorld(),
                    minX + sizeX / 2, minY, minZ + sizeZ / 2);
        } else if (session.pasteAnchor() != null && schematicName != null) {
            geometricCenter = session.pasteAnchor().clone().subtract(
                    configManager.getSchematicRootOffset(schematicName));
        } else {
            geometricCenter = session.origin().toBlockLocation();
        }
        final Location center = session.pasteAnchor() != null
                ? session.pasteAnchor().clone() : geometricCenter.clone();
        final List<org.bukkit.util.Vector> mobOffsets = session.mobSpawnPoints().stream()
                .map(location -> location.toVector().subtract(center.toVector())).toList();
        final List<org.bukkit.util.Vector> hologramOffsets = session.hologramPoints().stream()
                .map(location -> location.toVector().subtract(center.toVector())).toList();
        final List<org.bukkit.util.Vector> chestOffsets = session.chestPoints().stream()
                .map(location -> location.toVector().subtract(center.toVector())).toList();
        configManager.setSetupOffsets(session.meteorType(), mobOffsets, hologramOffsets, chestOffsets);
        if (schematicName != null && !schematicName.isBlank()) {
            configManager.saveSchematicSetup(schematicName,
                    mobOffsets, hologramOffsets, chestOffsets,
                    center.toVector().subtract(geometricCenter.toVector()));
        }
    }

    // ---- Visual Effects ----

    private void startSetupVisuals(Player player, SetupSession session) {
        final SetupVisual visual = new SetupVisual(player, session);
        activeVisuals.put(player.getUniqueId(), visual);
        visual.start();
    }

    /**
     * Updates the player's action bar with live setup session status.
     * All text content is loaded from the locale system (config.yml → messages.setup.actionbar.*)
     * so server admins can customize colours and wording.
     */
    private void updateActionBar(Player player, SetupSession session) {
        final String pos1Status = session.pos1() != null
                ? configManager.getMessage("setup.actionbar.pos1_saved")
                : configManager.getMessage("setup.actionbar.pos1_pending");
        final String pos2Status = session.pos2() != null
                ? configManager.getMessage("setup.actionbar.pos2_saved")
                : configManager.getMessage("setup.actionbar.pos2_pending");            final String msg = configManager.getMessage("setup.actionbar.template",
                    "pos1_status", pos1Status,
                    "pos2_status", pos2Status,
                    "mob_count", String.valueOf(session.mobSpawnPoints().size()),
                    "type", configManager.getMeteorTypeName(session.meteorType()));

        MessageUtil.sendActionBar(player, msg);
    }

    // ---- Setup Session Record ----

    public record SetupSession(
            @NotNull MeteorType meteorType,
            @NotNull Location origin,
            @Nullable Location pos1,
            @Nullable Location pos2,
            @Nullable BoundingBox boundingBox,
            @NotNull List<Location> mobSpawnPoints,
            @Nullable Inventory lootInventory,
            @NotNull List<String> selectedMobIds,
            @NotNull List<Location> hologramPoints,
            @NotNull List<Location> chestPoints,
            @Nullable Location pasteAnchor,
            @Nullable String schematicName
    ) {
        public SetupSession(@NotNull MeteorType type, @NotNull Location origin) {
            this(type, origin, null, null, null, new ArrayList<>(), null,
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null, null);
        }

        public SetupSession withPos1(@Nullable Location pos1) {
            return new SetupSession(meteorType, origin, pos1, pos2, boundingBox, mobSpawnPoints, lootInventory, selectedMobIds, hologramPoints, chestPoints, pasteAnchor, schematicName);
        }

        public SetupSession withPos2(@Nullable Location pos2) {
            return new SetupSession(meteorType, origin, pos1, pos2, boundingBox, mobSpawnPoints, lootInventory, selectedMobIds, hologramPoints, chestPoints, pasteAnchor, schematicName);
        }

        public SetupSession withBoundingBox(@Nullable BoundingBox bb) {
            return new SetupSession(meteorType, origin, pos1, pos2, bb, mobSpawnPoints, lootInventory, selectedMobIds, hologramPoints, chestPoints, pasteAnchor, schematicName);
        }

        public SetupSession withLootInventory(@Nullable Inventory inv) {
            return new SetupSession(meteorType, origin, pos1, pos2, boundingBox, mobSpawnPoints, inv, selectedMobIds, hologramPoints, chestPoints, pasteAnchor, schematicName);
        }

        public SetupSession withSchematicName(@Nullable String name) {
            return new SetupSession(meteorType, origin, pos1, pos2, boundingBox, mobSpawnPoints, lootInventory, selectedMobIds, hologramPoints, chestPoints, pasteAnchor, name);
        }

        public SetupSession withPasteAnchor(@Nullable Location anchor) {
            return new SetupSession(meteorType, origin, pos1, pos2, boundingBox, mobSpawnPoints,
                    lootInventory, selectedMobIds, hologramPoints, chestPoints, anchor, schematicName);
        }

        public SetupSession withSavedPoints(@NotNull List<Location> mobs,
                                            @NotNull List<Location> holograms,
                                            @NotNull List<Location> chests) {
            return new SetupSession(meteorType, origin, pos1, pos2, boundingBox, mobs,
                    lootInventory, selectedMobIds, holograms, chests, pasteAnchor, schematicName);
        }
    }

    private record MobChanceSession(MeteorType type, Inventory inventory,
                                    List<String> mobIds, Map<String, Double> chances) {}

    /**
     * Handles periodic visual effects during setup mode.
     */
    private final class SetupVisual {
        private final Player player;
        private final SetupSession session;
        private boolean running;
        private FoliaScheduler.ScheduledTask scheduledTask;

        SetupVisual(Player player, SetupSession session) {
            this.player = player;
            this.session = session;
            this.scheduledTask = null;
        }

        void start() {
            this.running = true;
            this.scheduledTask = plugin.getFoliaScheduler().runRepeatingForEntity(player,
                    this::tick, 0L, 10L);
        }

        void stop() {
            this.running = false;
            if (scheduledTask != null) {
                plugin.getFoliaScheduler().cancelTask(scheduledTask);
                scheduledTask = null;
            }
        }

        private void tick() {
            if (!running || !player.isOnline()) {
                stop();
                return;
            }

            final SetupSession currentSession = activeSessions.get(player.getUniqueId());
            if (currentSession == null) {
                stop();
                return;
            }

            // Draw region selection box if both positions are set
            if (currentSession.pos1() != null && currentSession.pos2() != null) {
                final double minX = Math.min(currentSession.pos1().getX(), currentSession.pos2().getX());
                final double minY = Math.min(currentSession.pos1().getY(), currentSession.pos2().getY());
                final double minZ = Math.min(currentSession.pos1().getZ(), currentSession.pos2().getZ());
                final double maxX = Math.max(currentSession.pos1().getX(), currentSession.pos2().getX());
                final double maxY = Math.max(currentSession.pos1().getY(), currentSession.pos2().getY());
                final double maxZ = Math.max(currentSession.pos1().getZ(), currentSession.pos2().getZ());

                particleUtil.drawBoxOutline(player, minX, minY, minZ, maxX, maxY, maxZ,
                        new Particle.DustOptions(
                                org.bukkit.Color.fromRGB(255, 170, 0), 1.2f
                        ), 1.5);
            }

            // Draw vertical beams at mob spawn points
            for (final Location spawnLoc : currentSession.mobSpawnPoints()) {
                particleUtil.spawnVerticalBeam(player,
                        spawnLoc.getX(), spawnLoc.getZ(),
                        spawnLoc.getY() - 1, spawnLoc.getY() + 5,
                        new Particle.DustOptions(
                                org.bukkit.Color.fromRGB(255, 85, 255), 1.5f
                        ));
            }

            // Update action bar
            updateActionBar(player, currentSession);
        }
    }

    // ---- Utility ----

    private String formatLocation(Location loc) {
        return String.format("%.0f, %.0f, %.0f",
                loc.getX(), loc.getY(), loc.getZ());
    }

    private String getRegionSize(BoundingBox bb) {
        final int xSize = (int) (bb.getMaxX() - bb.getMinX());
        final int ySize = (int) (bb.getMaxY() - bb.getMinY());
        final int zSize = (int) (bb.getMaxZ() - bb.getMinZ());
        return xSize + "x" + ySize + "x" + zSize + " (" + (xSize * ySize * zSize) + ")";
    }
}
