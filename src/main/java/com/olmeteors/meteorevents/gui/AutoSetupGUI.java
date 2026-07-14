package com.olmeteors.meteorevents.gui;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.olmeteors.meteorevents.event.MeteorType;
import com.olmeteors.meteorevents.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Compact in-game editor for the complete automatic meteor rule. */
public final class AutoSetupGUI implements Listener {
    private static final Material[] LOOT_BLOCKS = {Material.CHEST, Material.BARREL,
            Material.ANCIENT_DEBRIS, Material.CRYING_OBSIDIAN, Material.RESPAWN_ANCHOR};
    private final MeteorPlugin plugin;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public AutoSetupGUI(MeteorPlugin plugin) { this.plugin = plugin; }

    public void open(@NotNull Player player) {
        final var config = plugin.getConfigManager();
        final List<String> configured = config.getAutomaticTypes();
        final EnumSet<MeteorType> types = configured.isEmpty() ? EnumSet.allOf(MeteorType.class)
                : configured.stream().map(raw -> {
                    try { return MeteorType.fromString(raw); } catch (Exception ignored) { return null; }
                }).filter(Objects::nonNull).collect(() -> EnumSet.noneOf(MeteorType.class),
                        EnumSet::add, EnumSet::addAll);
        final String world = config.getAutomaticWorlds().stream().findFirst()
                .orElse(player.getWorld().getName());
        final var preset = config.getAutomaticLocationPreset(world);
        final Session session = new Session(types, world, preset.name(),
                config.getLootBlockMaterial(types.isEmpty() ? MeteorType.SMALL : types.iterator().next()),
                config.getAutomaticMinMinutes(), config.getAutomaticMaxMinutes(),
                config.getAutomaticMinDistance(), config.getAutomaticMaxDistance(),
                preset.minY(), preset.maxY());
        session.inventory = Bukkit.createInventory(null, 54,
                MessageUtil.parse("&6&lOlMeteor &8• &eAuto Ayar"));
        sessions.put(player.getUniqueId(), session);
        redraw(session);
        player.openInventory(session.inventory);
    }

    private void redraw(Session s) {
        s.inventory.clear();
        int slot = 10;
        for (MeteorType type : MeteorType.values()) {
            final boolean selected = s.types.contains(type);
            s.inventory.setItem(slot++, item(selected ? Material.LIME_DYE : Material.GRAY_DYE,
                    (selected ? "&a✔ " : "&7") + plugin.getConfigManager().getMeteorTypeName(type),
                    "&7Tıklayarak aç/kapat"));
        }
        s.inventory.setItem(20, item(Material.GRASS_BLOCK, "&bDünya: &f" + s.world,
                "&7Sol/sağ tıkla dünyayı değiştir"));
        s.inventory.setItem(22, item(Material.COMPASS, "&eYüzey: &f" + s.preset,
                "&7Sol/sağ tıkla preset değiştir"));
        s.inventory.setItem(24, item(s.lootBlock, "&dGanimet bloğu: &f" + s.lootBlock,
                "&7İmleçte blokla tıkla veya boş elle değiştir"));
        s.inventory.setItem(29, item(Material.CLOCK, "&6Süre: &f" + s.minMinutes + "-" + s.maxMinutes + " dk",
                "&7Sol/sağ: maksimum ±5", "&7Shift+sol/sağ: minimum ±5"));
        s.inventory.setItem(31, item(Material.RECOVERY_COMPASS,
                "&6Uzaklık: &f" + s.minDistance + "-" + s.maxDistance,
                "&7Sol/sağ: maksimum ±250", "&7Shift+sol/sağ: minimum ±250"));
        s.inventory.setItem(33, item(Material.FEATHER, "&bY aralığı: &f" + s.minY + "-" + s.maxY,
                "&7Air için kullanılır", "&7Sol/sağ: max ±10", "&7Shift+sol/sağ: min ±10"));
        s.inventory.setItem(49, item(Material.EMERALD_BLOCK, "&a&lKaydet ve Aç",
                "&7Bütün auto ayarlarını tek seferde kaydeder"));
        s.inventory.setItem(53, item(Material.BARRIER, "&cKapat", "&7Kaydetmeden kapatır"));
    }

    @EventHandler public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        final Session s = sessions.get(player.getUniqueId());
        if (s == null || event.getInventory() != s.inventory) return;
        if (event.getRawSlot() >= 54) {
            if (event.isShiftClick()) event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        final int slot = event.getRawSlot();
        if (slot >= 10 && slot < 10 + MeteorType.values().length) {
            final MeteorType type = MeteorType.values()[slot - 10];
            if (s.types.contains(type) && s.types.size() > 1) s.types.remove(type); else s.types.add(type);
        } else if (slot == 20) cycleWorld(s, event.isRightClick() ? -1 : 1);
        else if (slot == 22) cyclePreset(s, event.isRightClick() ? -1 : 1);
        else if (slot == 24) changeLootBlock(s, event.getCursor(), event.isRightClick() ? -1 : 1);
        else if (slot == 29) adjustTime(s, event.isShiftClick(), event.isRightClick() ? -5 : 5);
        else if (slot == 31) adjustDistance(s, event.isShiftClick(), event.isRightClick() ? -250 : 250);
        else if (slot == 33) adjustY(s, event.isShiftClick(), event.isRightClick() ? -10 : 10);
        else if (slot == 49) { save(player, s); return; }
        else if (slot == 53) { sessions.remove(player.getUniqueId()); player.closeInventory(); return; }
        redraw(s);
    }

    private void cycleWorld(Session s, int delta) {
        final List<World> worlds = plugin.getServer().getWorlds();
        if (worlds.isEmpty()) return;
        int index = 0;
        for (int i = 0; i < worlds.size(); i++) if (worlds.get(i).getName().equals(s.world)) index = i;
        s.world = worlds.get(Math.floorMod(index + delta, worlds.size())).getName();
    }
    private void cyclePreset(Session s, int delta) {
        final List<String> values = plugin.getConfigManager().getLocationPresetNames();
        if (values.isEmpty()) return;
        final int index = Math.max(0, values.indexOf(s.preset));
        s.preset = values.get(Math.floorMod(index + delta, values.size()));
        final var preset = plugin.getConfigManager().getLocationPreset(s.preset);
        s.minY = preset.minY(); s.maxY = preset.maxY();
    }
    private void changeLootBlock(Session s, ItemStack cursor, int delta) {
        if (cursor != null && !cursor.isEmpty() && cursor.getType().isBlock()
                && cursor.getType().isItem()) { s.lootBlock = cursor.getType(); return; }
        int index = 0;
        for (int i = 0; i < LOOT_BLOCKS.length; i++) if (LOOT_BLOCKS[i] == s.lootBlock) index = i;
        s.lootBlock = LOOT_BLOCKS[Math.floorMod(index + delta, LOOT_BLOCKS.length)];
    }
    private void adjustTime(Session s, boolean minimum, int delta) {
        if (minimum) s.minMinutes = Math.max(1, Math.min(s.maxMinutes, s.minMinutes + delta));
        else s.maxMinutes = Math.max(s.minMinutes, s.maxMinutes + delta);
    }
    private void adjustDistance(Session s, boolean minimum, int delta) {
        if (minimum) s.minDistance = Math.max(0, Math.min(s.maxDistance - 1, s.minDistance + delta));
        else s.maxDistance = Math.max(s.minDistance + 1, s.maxDistance + delta);
    }
    private void adjustY(Session s, boolean minimum, int delta) {
        final World world = plugin.getServer().getWorld(s.world); if (world == null) return;
        if (minimum) s.minY = Math.max(world.getMinHeight(), Math.min(s.maxY - 1, s.minY + delta));
        else s.maxY = Math.min(world.getMaxHeight() - 1, Math.max(s.minY + 1, s.maxY + delta));
    }
    private void save(Player player, Session s) {
        final boolean success = plugin.getConfigManager().setAutomaticRule(new ArrayList<>(s.types),
                s.world, s.preset, List.of(s.lootBlock), s.minMinutes, s.maxMinutes,
                s.minDistance, s.maxDistance, s.minY, s.maxY);
        if (success) {
            plugin.getMeteorEventManager().startAutomaticScheduler();
            sessions.remove(player.getUniqueId()); player.closeInventory();
            MessageUtil.sendMessage(player, "&aAuto ayarları kaydedildi ve yeni zaman planlandı.");
        } else MessageUtil.sendMessage(player, "&cAuto ayarları kaydedilemedi.");
    }
    @EventHandler public void onClose(InventoryCloseEvent event) {
        final Session s = sessions.get(event.getPlayer().getUniqueId());
        if (s != null && event.getInventory() == s.inventory) sessions.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler public void onDrag(InventoryDragEvent event) {
        final Session s = sessions.get(event.getWhoClicked().getUniqueId());
        if (s != null && event.getInventory() == s.inventory
                && event.getRawSlots().stream().anyMatch(slot -> slot < 54)) event.setCancelled(true);
    }
    private ItemStack item(Material material, String name, String... lore) {
        final ItemStack item = new ItemStack(material); final var meta = item.getItemMeta();
        meta.displayName(MessageUtil.parse(name));
        meta.lore(Arrays.stream(lore).map(MessageUtil::parse).toList()); item.setItemMeta(meta); return item;
    }
    private static final class Session {
        final EnumSet<MeteorType> types; String world, preset; Material lootBlock;
        int minMinutes, maxMinutes, minDistance, maxDistance, minY, maxY; Inventory inventory;
        Session(EnumSet<MeteorType> types, String world, String preset, Material lootBlock,
                int minMinutes, int maxMinutes, int minDistance, int maxDistance, int minY, int maxY) {
            this.types=types; this.world=world; this.preset=preset; this.lootBlock=lootBlock;
            this.minMinutes=minMinutes; this.maxMinutes=maxMinutes; this.minDistance=minDistance;
            this.maxDistance=maxDistance; this.minY=minY; this.maxY=maxY;
        }
    }
}
