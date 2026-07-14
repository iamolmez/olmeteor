package com.olmeteors.meteorevents.loot;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.olmeteors.meteorevents.config.ConfigManager;
import com.olmeteors.meteorevents.hook.NBTIntegration;
import com.olmeteors.meteorevents.event.MeteorType;
import com.olmeteors.meteorevents.util.DataComponentUtil;
import com.olmeteors.meteorevents.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * GUI editor for configuring loot tables for meteor events.
 * Allows administrators to:
 * - Add items to the loot table
 * - Configure drop chances (0.1% - 100%)
 * - Set minimum and maximum amounts
 * - Lock/unlock items
 * - Remove items from the table
 * <p>
 * Values are edited by closing the GUI, typing the desired value in chat,
 * and having the GUI automatically reopen with the updated entry.
 * <p>
 * All user-facing messages are read from {@code config.yml → messages.loot_editor.*}
 * via {@link ConfigManager#getMessage(String, String...)}.
 */
public final class LootGUIEditor implements Listener {

    private final MeteorPlugin plugin;
    private final ConfigManager config;

    private final Map<UUID, LootEditorSession> activeEditors;
    private final Map<MeteorType, LootTable> lootTables;
    private final File lootFile;
    private final NBTIntegration nbtIntegration;

    // Map of players currently in a chat-based edit flow (UUID -> session info)
    // These players have closed the GUI and are expected to type a value in chat.
    private final Map<UUID, ChatEditContext> chatEditContexts;

    private static final int GUI_SIZE = 54; // 6 rows

    // GUI layout
    private static final int ITEMS_START = 0;
    private static final int ITEMS_END = 44;
    private static final int INFO_SLOT = 45;
    private static final int CHANCE_EDIT_SLOT = 46;
    private static final int AMOUNT_EDIT_SLOT = 47;
    private static final int LOCK_TOGGLE_SLOT = 48;
    private static final int RESET_SLOT = 49;
    private static final int CLEAR_SLOT = 50;
    private static final int CANCEL_SLOT = 52;
    private static final int SAVE_SLOT = 53;

    // Regex patterns for input validation
    // Chance: 1-3 digits before decimal, optionally followed by . and 1-2 decimal digits
    private static final String CHANCE_REGEX = "^\\d{1,3}(\\.\\d{1,2})?$";
    private static final String AMOUNT_REGEX = "^\\d+\\s+\\d+$";

    // ── Config message helpers ──────────────────────────────────
    private void tell(@NotNull Player player, @NotNull String path,
                      @NotNull String @NotNull ... placeholders) {
        config.sendMessage(player, "loot_editor." + path, placeholders);
    }

    /** Parses a GUI label from {@code loot_editor.gui.<path>}. */
    private @NotNull Component guiMsg(@NotNull String path,
                                       @NotNull String @NotNull ... placeholders) {
        return MessageUtil.parse(config.getMessage("loot_editor.gui." + path, placeholders));
    }

    /** Gets a raw GUI string from {@code loot_editor.gui.<path>}. */
    private @NotNull String guiStr(@NotNull String path,
                                    @NotNull String @NotNull ... placeholders) {
        return config.getMessage("loot_editor.gui." + path, placeholders);
    }

    /** Returns GUI lore lines as Components from {@code loot_editor.gui.<path>}. */
    private @NotNull List<@NotNull Component> guiLore(@NotNull String path,
                                                       @NotNull String @NotNull ... placeholders) {
        return config.getMessageLines("loot_editor.gui." + path, placeholders).stream()
                .map(MessageUtil::parse)
                .toList();
    }

    public LootGUIEditor(MeteorPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.config = configManager;
        this.activeEditors = new ConcurrentHashMap<>();
        this.lootTables = new ConcurrentHashMap<>();
        this.chatEditContexts = new ConcurrentHashMap<>();
        this.lootFile = new File(plugin.getDataFolder(), "loot-tables.yml");
        this.nbtIntegration = new NBTIntegration(plugin);

        // Initialize default loot tables
        initializeDefaultLootTables();
        loadLootTables();
    }

    /**
     * Initializes default loot tables for each meteor type.
     */
    private void initializeDefaultLootTables() {
        for (final MeteorType type : MeteorType.values()) {
            final LootTable table = createDefaultLootTable(type);
            lootTables.put(type, table);
        }
    }

    /** Imports chest contents as guaranteed loot while preserving all ItemStack/PDC data. */
    public int importFromInventory(@NotNull MeteorType type, @NotNull Inventory source) {
        final LootTable table = new LootTable(config.getMeteorTypeName(type) + " Loot");
        int imported = 0;
        for (final ItemStack item : source.getStorageContents()) {
            if (item == null || item.isEmpty()) continue;
            final ItemStack copy = item.clone();
            final int amount = Math.max(1, copy.getAmount());
            copy.setAmount(1);
            table.addEntry(new LootTable.LootEntry(copy, 100.0, amount, amount, false));
            imported++;
        }
        lootTables.put(type, table);
        saveLootTables();
        return imported;
    }

    private void loadLootTables() {
        if (!lootFile.isFile()) return;
        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(lootFile);
        for (final MeteorType type : MeteorType.values()) {
            final ConfigurationSection section = yaml.getConfigurationSection(type.name());
            if (section == null) continue;
            final LootTable table = new LootTable(section.getString("name",
                    config.getMeteorTypeName(type) + " Loot"));
            for (final Map<?, ?> raw : section.getMapList("entries")) {
                final ItemStack byteItem = nbtIntegration.decode(
                        raw.get("nbt-base64") instanceof String encoded ? encoded : null);
                final Object itemValue = raw.get("item");
                final ItemStack item = byteItem != null ? byteItem
                        : itemValue instanceof ItemStack fallback ? fallback : null;
                if (item == null || item.isEmpty()) continue;
                final double chance = Math.max(0.1, Math.min(100.0,
                        number(raw.get("chance"), 100.0).doubleValue()));
                final int min = Math.max(1, number(raw.get("min"), 1).intValue());
                final int max = Math.max(min, number(raw.get("max"), min).intValue());
                final boolean locked = Boolean.TRUE.equals(raw.get("locked"));
                table.addEntry(new LootTable.LootEntry(item, chance, min, max, locked));
            }
            lootTables.put(type, table);
        }
    }

    private Number number(Object value, Number fallback) {
        return value instanceof Number number ? number : fallback;
    }

    private void saveLootTables() {
        final YamlConfiguration yaml = new YamlConfiguration();
        lootTables.forEach((type, table) -> {
            yaml.set(type.name() + ".name", table.name());
            final List<Map<String, Object>> entries = new ArrayList<>();
            for (final LootTable.LootEntry entry : table.entries()) {
                final Map<String, Object> data = new LinkedHashMap<>();
                data.put("item", entry.item().clone());
                data.put("nbt-base64", nbtIntegration.encode(entry.item()));
                data.put("chance", entry.chance());
                data.put("min", entry.minAmount());
                data.put("max", entry.maxAmount());
                data.put("locked", entry.locked());
                entries.add(data);
            }
            yaml.set(type.name() + ".entries", entries);
        });
        try {
            yaml.save(lootFile);
        } catch (IOException error) {
            plugin.getLogger().log(Level.SEVERE, "Could not save loot-tables.yml", error);
        }
    }

    /**
     * Creates a default loot table for a meteor type based on difficulty.
     */
    private @NotNull LootTable createDefaultLootTable(@NotNull MeteorType type) {
        final LootTable table = new LootTable(config.getMeteorTypeName(type) + " Loot");

        // Common items (available for all types)
        table.addEntry(new LootTable.LootEntry(
                new ItemStack(Material.DIAMOND, 1), 50.0, 1, 3, false));
        table.addEntry(new LootTable.LootEntry(
                new ItemStack(Material.IRON_INGOT, 1), 70.0, 3, 8, false));
        table.addEntry(new LootTable.LootEntry(
                new ItemStack(Material.GOLD_INGOT, 1), 60.0, 2, 5, false));
        table.addEntry(new LootTable.LootEntry(
                new ItemStack(Material.EXPERIENCE_BOTTLE, 1), 80.0, 3, 10, false));

        // Rare items based on difficulty
        switch (type.difficulty()) {
            case EASY -> {
                table.addEntry(new LootTable.LootEntry(
                        new ItemStack(Material.EMERALD, 1), 30.0, 1, 2, false));
            }
            case NORMAL -> {
                table.addEntry(new LootTable.LootEntry(
                        new ItemStack(Material.EMERALD, 1), 40.0, 2, 4, false));
                table.addEntry(new LootTable.LootEntry(
                        DataComponentUtil.createWindChargeItem(config, 1), 25.0, 1, 3, false));
            }
            case HARD -> {
                table.addEntry(new LootTable.LootEntry(
                        new ItemStack(Material.EMERALD, 1), 50.0, 3, 6, false));
                table.addEntry(new LootTable.LootEntry(
                        DataComponentUtil.createWindChargeItem(config, 1), 35.0, 2, 4, false));
                table.addEntry(new LootTable.LootEntry(
                        new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1), 15.0, 1, 1, false));
            }
            case EPIC -> {
                table.addEntry(new LootTable.LootEntry(
                        new ItemStack(Material.EMERALD, 1), 60.0, 4, 8, false));
                table.addEntry(new LootTable.LootEntry(
                        DataComponentUtil.createWindChargeItem(config, 1), 45.0, 3, 6, false));
                table.addEntry(new LootTable.LootEntry(
                        new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1), 25.0, 1, 2, false));
                table.addEntry(new LootTable.LootEntry(
                        DataComponentUtil.createMace(config, 3, 2, 1), 10.0, 1, 1, true));
            }
            case LEGENDARY -> {
                table.addEntry(new LootTable.LootEntry(
                        new ItemStack(Material.NETHERITE_INGOT, 1), 30.0, 1, 2, false));
                table.addEntry(new LootTable.LootEntry(
                        new ItemStack(Material.EMERALD, 1), 70.0, 5, 12, false));
                table.addEntry(new LootTable.LootEntry(
                        DataComponentUtil.createWindChargeItem(config, 1), 55.0, 4, 8, false));
                table.addEntry(new LootTable.LootEntry(
                        new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1), 35.0, 2, 3, false));
                table.addEntry(new LootTable.LootEntry(
                        DataComponentUtil.createMace(config, 5, 4, 3), 20.0, 1, 1, true));
                table.addEntry(new LootTable.LootEntry(
                        new ItemStack(Material.HEAVY_CORE, 1), 5.0, 1, 1, true));
            }
        }

        return table;
    }

    /**
     * Opens the loot editor GUI for a player.
     *
     * @param player the player
     * @param type   the meteor type to edit
     */
    public void openEditor(@NotNull Player player, @NotNull MeteorType type) {
        final LootTable table = lootTables.getOrDefault(type, createDefaultLootTable(type));
        final LootEditorSession session = new LootEditorSession(type, table);

        // Create inventory with locale-aware title
        final String typeName = config.getMeteorTypeName(type);
        final Inventory inv = Bukkit.createInventory(null, GUI_SIZE,
                MessageUtil.parse(guiStr("title_prefix") + typeName));

        // Fill with loot items
        refreshGUI(inv, session);

        // Open inventory
        activeEditors.put(player.getUniqueId(), session);
        player.openInventory(inv);

        tell(player, "opened", "type", typeName);
        tell(player, "drag_hint");
        tell(player, "click_hint");
    }

    /**
     * Refreshes the GUI display with current loot table data.
     */
    private void refreshGUI(@NotNull Inventory inv, @NotNull LootEditorSession session) {
        inv.clear();

        final LootTable table = session.table();
        final String typeName = config.getMeteorTypeName(session.type());

        // Place loot items in the grid
        int slot = ITEMS_START;
        for (final var entry : table.entries()) {
            if (slot > ITEMS_END) break;

            final ItemStack displayItem = entry.item().clone();
            final ItemMeta meta = displayItem.getItemMeta();

            // Add lore showing configuration (locale-aware via guiMsg/guiStr)
            final var lore = new ArrayList<Component>();
            if (meta.hasLore()) {
                lore.addAll(meta.lore());
                lore.add(Component.text(""));
            }

            lore.add(guiMsg("item_separator"));
            lore.add(guiMsg("drop_chance", "chance", String.format("%.1f", entry.chance())));
            lore.add(guiMsg("amount_range",
                    "min", String.valueOf(entry.minAmount()),
                    "max", String.valueOf(entry.maxAmount())));
            lore.add(guiMsg("locked_status", "status",
                    guiStr(entry.locked() ? "locked_yes" : "locked_no")));
            if (slot == session.selectedSlot()) {
                lore.add(guiMsg("selected_status"));
            }
            lore.add(Component.text(""));
            lore.add(guiMsg("left_click"));
            lore.add(guiMsg("right_click"));
            lore.add(guiMsg("shift_click"));
            lore.add(guiMsg("q_drop"));

            meta.lore(lore);
            displayItem.setItemMeta(meta);

            inv.setItem(slot, displayItem);
            slot++;
        }

        // Fill remaining slots with glass panes
        final ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        final var fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(""));
        filler.setItemMeta(fillerMeta);

        for (int i = slot; i <= ITEMS_END; i++) {
            inv.setItem(i, filler);
        }

        // Bottom row - Control panel (all locale-aware)
        // Info slot
        final ItemStack info = new ItemStack(Material.WRITABLE_BOOK);
        final var infoMeta = info.getItemMeta();
        infoMeta.displayName(guiMsg("info_name"));
        infoMeta.lore(List.of(
                guiMsg("info_type", "type", typeName),
                guiMsg("info_items", "count", String.valueOf(table.entries().size())),
                guiMsg("info_difficulty",
                        "color", config.getDifficultyColor(session.type().difficulty()),
                        "difficulty", config.getDifficultyName(session.type().difficulty()))
        ));
        info.setItemMeta(infoMeta);
        inv.setItem(INFO_SLOT, info);

        // Chance edit helper
        final ItemStack chanceHelper = new ItemStack(Material.REPEATER);
        final var chanceMeta = chanceHelper.getItemMeta();
        chanceMeta.displayName(guiMsg("chance_name"));
        chanceMeta.lore(guiLore("chance_lore"));
        chanceHelper.setItemMeta(chanceMeta);
        inv.setItem(CHANCE_EDIT_SLOT, chanceHelper);

        // Amount edit helper
        final ItemStack amountHelper = new ItemStack(Material.COMPARATOR);
        final var amountMeta = amountHelper.getItemMeta();
        amountMeta.displayName(guiMsg("amount_name"));
        amountMeta.lore(guiLore("amount_lore"));
        amountHelper.setItemMeta(amountMeta);
        inv.setItem(AMOUNT_EDIT_SLOT, amountHelper);

        // Lock toggle helper
        final ItemStack lockHelper = new ItemStack(Material.IRON_BARS);
        final var lockMeta = lockHelper.getItemMeta();
        lockMeta.displayName(guiMsg("lock_name"));
        lockMeta.lore(guiLore("lock_lore"));
        lockHelper.setItemMeta(lockMeta);
        inv.setItem(LOCK_TOGGLE_SLOT, lockHelper);

        // Reset to the built-in defaults for this meteor type
        final ItemStack reset = new ItemStack(Material.RECOVERY_COMPASS);
        final var resetMeta = reset.getItemMeta();
        resetMeta.displayName(guiMsg("reset_name"));
        resetMeta.lore(guiLore("reset_lore"));
        reset.setItemMeta(resetMeta);
        inv.setItem(RESET_SLOT, reset);

        // Clear all
        final ItemStack clear = new ItemStack(Material.BARRIER);
        final var clearMeta = clear.getItemMeta();
        clearMeta.displayName(guiMsg("clear_name"));
        clearMeta.lore(guiLore("clear_lore"));
        clear.setItemMeta(clearMeta);
        inv.setItem(CLEAR_SLOT, clear);

        // Close without saving the current session
        final ItemStack cancel = new ItemStack(Material.REDSTONE_BLOCK);
        final var cancelMeta = cancel.getItemMeta();
        cancelMeta.displayName(guiMsg("cancel_name"));
        cancelMeta.lore(guiLore("cancel_lore"));
        cancel.setItemMeta(cancelMeta);
        inv.setItem(CANCEL_SLOT, cancel);

        // Save
        final ItemStack save = new ItemStack(Material.EMERALD_BLOCK);
        final var saveMeta = save.getItemMeta();
        saveMeta.displayName(guiMsg("save_name"));
        saveMeta.lore(guiLore("save_lore"));
        save.setItemMeta(saveMeta);
        inv.setItem(SAVE_SLOT, save);
    }

    // ---- Event Handlers ----

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        final LootEditorSession session = activeEditors.get(player.getUniqueId());
        if (session == null) return;

        final Inventory inv = event.getInventory();
        final int slot = event.getRawSlot();
        final ItemStack current = event.getCurrentItem();
        final ItemStack cursor = event.getCursor();

        // Prevent taking items from control panel
        if (slot >= 45 && slot < GUI_SIZE) {
            event.setCancelled(true);

            if (current != null && !current.isEmpty()) {
                handleControlClick(player, session, inv, slot, current);
            }
            return;
        }

        // Handle item placement in the grid (allow adding items)
        if (slot >= ITEMS_START && slot <= ITEMS_END) {
            if (cursor != null && !cursor.isEmpty()) {
                // Adding a new item
                event.setCancelled(true);
                addItemToTable(player, session, inv, slot, cursor);
            } else if (current != null && !current.isEmpty()) {
                // Clicking on an existing item
                event.setCancelled(true);
                handleItemClick(player, session, inv, slot, current, event.getClick());
            } else {
                event.setCancelled(true);
            }
            return;
        }

        // Block items being moved into the GUI from player inventory
        if (slot >= GUI_SIZE) {
            if (event.isShiftClick() && current != null && !current.isEmpty()
                    && session.table().entries().size() < 45) {
                event.setCancelled(true);
                addItemToTable(player, session, inv, session.table().entries().size(), current);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        final LootEditorSession session = activeEditors.get(player.getUniqueId());
        if (session == null) return;

        // Block dragging into the GUI
        for (final int slot : event.getRawSlots()) {
            if (slot < GUI_SIZE) {
                event.setCancelled(true);
                final ItemStack dragged = event.getOldCursor();
                if (dragged != null && !dragged.isEmpty() && session.table().entries().size() < 45) {
                    addItemToTable(player, session, event.getInventory(),
                            session.table().entries().size(), dragged);
                }
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // Only remove the editor session if the player is NOT in a chat edit flow
        // (closing due to chance/amount edit prompt)
        if (!chatEditContexts.containsKey(player.getUniqueId())) {
            activeEditors.remove(player.getUniqueId());
        }
    }

    /**
     * Cleans up chat edit contexts when a player disconnects,
     * preventing memory leaks from abandoned edit flows.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final UUID playerUUID = event.getPlayer().getUniqueId();
        chatEditContexts.remove(playerUUID);
        activeEditors.remove(playerUUID);
    }

    /**
     * Handles chat input from players who are in the middle of editing
     * a loot entry's chance or amount value via the chat prompt flow.
     * <p>
     * The player closes the GUI, types a value in chat, and the GUI
     * automatically reopens with the updated entry.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final ChatEditContext context = chatEditContexts.get(player.getUniqueId());

        if (context == null) return;

        // Cancel the chat event so the message isn't broadcast
        event.setCancelled(true);

        final String input = event.getMessage().trim();

        // Process the chat input on the main server thread
        plugin.getFoliaScheduler().callGlobal(() ->
                processChatInput(player, context, input)
        );
    }

    // ---- Chat Input Processing ----

    /**
     * Processes the player's chat input for a pending loot entry edit.
     * Validates, updates the entry, and reopens the GUI.
     */
    private void processChatInput(@NotNull Player player, @NotNull ChatEditContext context, @NotNull String input) {
        try {
            // Remove the context immediately to prevent double-processing
            chatEditContexts.remove(player.getUniqueId());

            // Handle cancellation
            if (input.equals("0") || input.equalsIgnoreCase("cancel")
                    || input.equalsIgnoreCase("exit")) {
                tell(player, "cancelled");
                reopenEditor(player, context);
                return;
            }

            final LootEditorSession session = context.session();
            final int slot = context.slot();
            final var entries = session.table().entries();

            if (slot < 0 || slot >= entries.size()) {
                tell(player, "no_longer_exists");
                reopenEditor(player, context);
                return;
            }

            final var currentEntry = entries.get(slot);
            final var editType = context.editType();

            switch (editType) {
                case CHANCE -> {
                    final Optional<Double> chance = parseChanceInput(input);
                    if (chance.isEmpty()) {
                        tell(player, "chance_error");
                        tell(player, "chance_retry");
                        // Re-register the context so the player can try again
                        chatEditContexts.put(player.getUniqueId(), context);
                        return;
                    }

                    final double newChance = chance.get();
                    final var updatedEntry = new LootTable.LootEntry(
                            currentEntry.item(), newChance,
                            currentEntry.minAmount(), currentEntry.maxAmount(),
                            currentEntry.locked()
                    );
                    session.updateEntry(slot, updatedEntry);

                    tell(player, "chance_changed",
                            "old", String.format("%.1f", currentEntry.chance()),
                            "new", String.format("%.1f", newChance));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                }

                case AMOUNT -> {
                    final Optional<int[]> amount = parseAmountInput(input);
                    if (amount.isEmpty()) {
                        tell(player, "amount_error");
                        tell(player, "amount_retry");
                        chatEditContexts.put(player.getUniqueId(), context);
                        return;
                    }

                    final int[] range = amount.get();
                    final int newMin = range[0];
                    final int newMax = range[1];
                    final var updatedEntry = new LootTable.LootEntry(
                            currentEntry.item(), currentEntry.chance(),
                            newMin, newMax,
                            currentEntry.locked()
                    );
                    session.updateEntry(slot, updatedEntry);

                    tell(player, "amount_changed",
                            "old", currentEntry.minAmount() + "-" + currentEntry.maxAmount(),
                            "new", newMin + "-" + newMax);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                }
            }

            // Save changes to the persistent loot table before reopening
            saveLootTable(session);

            // Reopen the editor GUI with updated values
            reopenEditor(player, context);

        } catch (Exception e) {
            plugin.getLogger().warning("Error processing loot editor chat input: " + e.getMessage());
            tell(player, "unexpected_error");
            chatEditContexts.remove(player.getUniqueId());
        }
    }

    /**
     * Parses a chance value from chat input.
     * Accepts values like "50", "12.5", "100", "0.1"
     *
     * @param input the raw chat input
     * @return Optional containing the parsed chance (0.1-100.0), or empty if invalid
     */
    private @NotNull Optional<Double> parseChanceInput(@NotNull String input) {
        try {
            // Remove trailing % if present
            final String clean = input.endsWith("%") ? input.substring(0, input.length() - 1).trim() : input.trim();

            if (!clean.matches(CHANCE_REGEX)) {
                return Optional.empty();
            }

            final double value = Double.parseDouble(clean);

            if (value < 0.1 || value > 100.0) {
                return Optional.empty();
            }

            // Round to one decimal place
            return Optional.of(Math.round(value * 10.0) / 10.0);

        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Parses an amount range from chat input.
     * Accepts values like "1 5", "3 10"
     *
     * @param input the raw chat input
     * @return Optional containing [min, max] array, or empty if invalid
     */
    private @NotNull Optional<int[]> parseAmountInput(@NotNull String input) {
        try {
            if (!input.matches(AMOUNT_REGEX)) {
                return Optional.empty();
            }

            final String[] parts = input.split("\\s+");
            final int min = Integer.parseInt(parts[0]);
            final int max = Integer.parseInt(parts[1]);

            if (min < 1 || max < min) {
                return Optional.empty();
            }

            // Cap at a reasonable maximum (e.g. 9999)
            if (max > 9999) {
                return Optional.empty();
            }

            return Optional.of(new int[]{min, max});

        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Reopens the loot editor GUI for a player who was in a chat edit flow.
     */
    private void reopenEditor(@NotNull Player player, @NotNull ChatEditContext context) {
        final LootEditorSession session = context.session();
        final String typeName = config.getMeteorTypeName(session.type());
        final Inventory inventory = Bukkit.createInventory(null, GUI_SIZE,
                MessageUtil.parse(guiStr("title_prefix") + typeName));
        refreshGUI(inventory, session);
        activeEditors.put(player.getUniqueId(), session);
        player.openInventory(inventory);
    }

    // ---- GUI Interaction Handlers ----

    private void handleControlClick(Player player, LootEditorSession session,
                                     Inventory inv, int slot, ItemStack clicked) {
        switch (slot) {
            case CLEAR_SLOT -> {
                session.clearTable();
                refreshGUI(inv, session);
                tell(player, "cleared");
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            }
            case RESET_SLOT -> {
                session.replaceTable(createDefaultLootTable(session.type()));
                refreshGUI(inv, session);
                tell(player, "reset");
                player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 1.2f);
            }
            case CANCEL_SLOT -> {
                activeEditors.remove(player.getUniqueId());
                player.closeInventory();
                tell(player, "discarded");
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
            }
            case SAVE_SLOT -> {
                saveLootTable(session);
                player.closeInventory();
                tell(player, "saved", "type", config.getMeteorTypeName(session.type()));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            }
            case CHANCE_EDIT_SLOT -> {
                if (session.selectedSlot() >= 0) {
                    promptChanceEdit(player, session, inv);
                } else {
                    tell(player, "select_first");
                }
            }
            case AMOUNT_EDIT_SLOT -> {
                if (session.selectedSlot() >= 0) {
                    promptAmountEdit(player, session, inv);
                } else {
                    tell(player, "select_first");
                }
            }
            case LOCK_TOGGLE_SLOT -> {
                if (session.selectedSlot() >= 0) {
                    toggleLock(player, session, inv);
                } else {
                    tell(player, "select_first");
                }
            }
        }
    }

    private void handleItemClick(Player player, LootEditorSession session,
                                  Inventory inv, int slot, ItemStack clicked, ClickType click) {
        if (click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
            // Remove item
            session.removeEntry(slot);
            refreshGUI(inv, session);
            tell(player, "removed");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        } else if (click.isShiftClick()) {
            // Toggle locked
            toggleSlotLock(player, session, inv, slot);
        } else if (click.isLeftClick()) {
            session.selectSlot(slot);
            promptChanceEdit(player, session, inv);
        } else if (click.isRightClick()) {
            session.selectSlot(slot);
            promptAmountEdit(player, session, inv);
        } else {
            session.selectSlot(slot);
            refreshGUI(inv, session);
            tell(player, "selected", "slot", String.valueOf(slot + 1));
        }
    }

    private void addItemToTable(Player player, LootEditorSession session,
                                 Inventory inv, int slot, ItemStack cursor) {
        final ItemStack item = cursor.clone();
        final int amount = Math.max(1, cursor.getAmount());
        item.setAmount(1);

        // Preserve the dragged stack size as the initial reward amount.
        final var entry = new LootTable.LootEntry(item, 50.0, amount, amount, false);
        session.addEntry(slot, entry);
        session.selectSlot(Math.min(slot, session.table().entries().size() - 1));

        refreshGUI(inv, session);
        tell(player, "added");
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    private void promptChanceEdit(Player player, LootEditorSession session, Inventory inv) {
        final int slot = session.selectedSlot();
        final var entries = session.table().entries();

        if (slot >= 0 && slot < entries.size()) {
            final var entry = entries.get(slot);
            final ChatEditContext context = new ChatEditContext(session, slot, LootEditorSession.EditType.CHANCE);
            chatEditContexts.put(player.getUniqueId(), context);
            player.closeInventory();

            // Send chat instructions (multi-line from config)
            config.sendMessage(player, "loot_editor.chance_prompt",
                    "current", String.format("%.1f", entry.chance()));
        }
    }

    private void promptAmountEdit(Player player, LootEditorSession session, Inventory inv) {
        final int slot = session.selectedSlot();
        final var entries = session.table().entries();

        if (slot >= 0 && slot < entries.size()) {
            final var entry = entries.get(slot);
            final ChatEditContext context = new ChatEditContext(session, slot, LootEditorSession.EditType.AMOUNT);
            chatEditContexts.put(player.getUniqueId(), context);
            player.closeInventory();

            // Send chat instructions (multi-line from config)
            config.sendMessage(player, "loot_editor.amount_prompt",
                    "current", entry.minAmount() + " - " + entry.maxAmount());
        }
    }

    private void toggleLock(Player player, LootEditorSession session, Inventory inv) {
        final int slot = session.selectedSlot();
        toggleSlotLock(player, session, inv, slot);
    }

    private void toggleSlotLock(Player player, LootEditorSession session, Inventory inv, int slot) {
        final var entries = session.table().entries();
        if (slot >= 0 && slot < entries.size()) {
            final var entry = entries.get(slot);
            final var newEntry = new LootTable.LootEntry(
                    entry.item(), entry.chance(), entry.minAmount(), entry.maxAmount(),
                    !entry.locked()
            );
            session.updateEntry(slot, newEntry);
            refreshGUI(inv, session);

            tell(player, newEntry.locked() ? "item_locked" : "item_unlocked");
            player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 1.0f);
        }
    }

    /**
     * Saves the loot table for the current session.
     */
    private void saveLootTable(@NotNull LootEditorSession session) {
        lootTables.put(session.type(), session.table());
        saveLootTables();
    }

    /**
     * Gets the loot table for a specific meteor type.
     *
     * @param type the meteor type
     * @return the loot table
     */
    public @NotNull LootTable getLootTable(@NotNull MeteorType type) {
        return lootTables.getOrDefault(type, createDefaultLootTable(type));
    }

    // ---- Chat Edit Context ----

    /**
     * Stores the context of a pending chat-based edit for a loot entry.
     * This is created when the player clicks "Edit Chance" or "Edit Amount"
     * in the GUI and is removed once the player types their response in chat.
     */
    private record ChatEditContext(
            @NotNull LootEditorSession session,
            int slot,
            @NotNull LootEditorSession.EditType editType
    ) {}

    // ---- Editor Session ----

    static final class LootEditorSession {
        private final MeteorType type;
        private LootTable table;
        private int selectedSlot;

        LootEditorSession(MeteorType type, LootTable table) {
            this.type = type;
            this.table = table;
            this.selectedSlot = -1;
        }

        MeteorType type() { return type; }
        LootTable table() { return table; }
        int selectedSlot() { return selectedSlot; }

        void selectSlot(int slot) { this.selectedSlot = slot; }
        void clearTable() {
            this.table = new LootTable(table.name());
            this.selectedSlot = -1;
        }
        void replaceTable(LootTable replacement) {
            this.table = replacement;
            this.selectedSlot = -1;
        }

        void addEntry(int slot, LootTable.LootEntry entry) {
            final var entries = new ArrayList<>(table.entries());
            if (slot < entries.size()) {
                entries.set(slot, entry);
            } else {
                entries.add(entry);
            }
            this.table = new LootTable(table.name(), entries);
        }

        void updateEntry(int index, LootTable.LootEntry entry) {
            final var entries = new ArrayList<>(table.entries());
            if (index >= 0 && index < entries.size()) {
                entries.set(index, entry);
                this.table = new LootTable(table.name(), entries);
            }
        }

        void removeEntry(int index) {
            final var entries = new ArrayList<>(table.entries());
            if (index >= 0 && index < entries.size()) {
                entries.remove(index);
                this.table = new LootTable(table.name(), entries);
                if (entries.isEmpty()) {
                    this.selectedSlot = -1;
                } else if (selectedSlot >= entries.size()) {
                    this.selectedSlot = entries.size() - 1;
                }
            }
        }

        enum EditType { CHANCE, AMOUNT }
    }
}
