package com.olmeteors.meteorevents.gui;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.olmeteors.meteorevents.config.ConfigManager;
import com.olmeteors.meteorevents.event.MeteorFallMode;
import com.olmeteors.meteorevents.event.MeteorType;
import com.olmeteors.meteorevents.event.RadiusShape;
import com.olmeteors.meteorevents.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ConfigGUI implements Listener {

    // Page constants
    private static final int PAGE_MAIN = 0;
    private static final int PAGE_LOCATION = 1;
    private static final int PAGE_HAZARDS = 2;
    private static final int PAGE_COMBAT = 3;
    private static final int PAGE_FALL_VAULT = 4;
    private static final int PAGE_AUTO = 5;
    private static final int PAGE_TYPES_MENU = 6;
    private static final int PAGE_TYPE_EDIT = 7;
    private static final int PAGE_LOOT_MENU = 8;
    private static final int PAGE_LOOT_EDIT = 9;
    private static final int PAGE_REWARDS_MENU = 10;
    private static final int PAGE_REWARDS_EDIT = 11;
    private static final int PAGE_TICKET = 12;
    private static final int PAGE_MOBS_MENU = 13;
    private static final int PAGE_MOBS_EDIT = 14;
    private static final int PAGE_SCHEMATIC = 15;

    private static final int[] BORDER_SLOTS = {0,1,2,3,4,5,6,7,8,45,46,47,48,49,50,51,52,53};
    private static final int BACK_SLOT = 45;
    private static final int SAVE_SLOT = 49;
    private static final int CLOSE_SLOT = 53;

    private final MeteorPlugin plugin;
    private final ConfigManager config;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, RewardsChatContext> rewardsChatContexts = new ConcurrentHashMap<>();
    private final Map<UUID, MobsChatContext> mobsChatContexts = new ConcurrentHashMap<>();

    public ConfigGUI(MeteorPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    public void open(@NotNull Player player) {
        final Session session = new Session();
        loadConfigToSession(session);
        session.inventory = Bukkit.createInventory(null, 54,
                MessageUtil.parse("&6&lOlMeteor &8• &eConfig Ayarları"));
        sessions.put(player.getUniqueId(), session);
        drawMainMenu(session);
        player.openInventory(session.inventory);
    }

    private void loadConfigToSession(Session s) {
        s.locationPreset = config.getLocationPreset().name();
        s.minDistance = config.getMinEventDistance();
        s.maxDistance = config.getMaxEventDistance();
        s.terrainVariance = config.getTerrainVarianceTolerance();
        s.bufferZone = config.getBufferZone();
        s.wgCheck = config.isWorldGuardCheckClaims();
        s.townyCheck = config.isTownyRequireWilderness();
        s.radiation = config.getRadiationDamagePerSecond();
        s.windKnockback = config.getWindChargeKnockbackMultiplier();
        s.windInterval = config.getWindChargeInterval();
        s.disableElytra = config.isElytraDisabled();
        s.disablePearl = config.isEnderPearlDisabled();
        s.damageBar = config.isDamageActionBarEnabled(MeteorType.SMALL);
        s.killBar = config.isKillActionBarEnabled(MeteorType.SMALL);
        s.leaderboard = config.isLeaderboardBroadcastEnabled(MeteorType.SMALL);
        s.leaderboardSize = config.getLeaderboardSize(MeteorType.SMALL);
        s.rewardTopCount = config.getRewardTopCount();
        s.fallNormal = config.getFallHeight(MeteorFallMode.NORMAL);
        s.fallSlow = config.getFallHeight(MeteorFallMode.SLOW);
        s.impactCore = config.isImpactCoreEnabled();
        s.bossThreshold = config.getBossDamageThreshold();
        s.vaultDelay = config.getVaultOpenDelayTicks();
        s.autoEnabled = config.isAutomaticEventsEnabled();
        s.autoMin = config.getAutomaticMinMinutes();
        s.autoMax = config.getAutomaticMaxMinutes();
        s.tpsGuard = config.isTpsGuardEnabled();
        s.minTps = config.getMinimumAutoTps();
        s.cooldownEnabled = config.isLocationCooldownEnabled();
        s.cooldownRadius = config.getLocationCooldownRadius();
        s.cooldownHours = config.getLocationCooldownHours();
        s.lootBlock = Material.matchMaterial(
                config.getConfig().getString("event.vault.loot-block", "ANCIENT_DEBRIS"));
        s.ticketMaterial = config.getConfig().getString("event.tickets.material", "FIRE_CHARGE");
        s.ticketCooldown = config.getConfig().getInt("event.tickets.cooldown-seconds", 300);
        s.recoveryEnabled = config.isCrashRecoveryEnabled();
        s.cleanupDelay = config.getCompletionCleanupDelaySeconds();
        s.unattendedTimeout = config.getUnattendedTimeoutMinutes();
        s.chunkRadius = config.getChunkForceLoadRadius();
    }

    // ────────────────────────────────────────────────────────────────
    //  MAIN MENU
    // ────────────────────────────────────────────────────────────────

    private void drawMainMenu(Session s) {
        s.page = PAGE_MAIN;
        s.inventory.clear();
        fillBorder(s);
        s.inventory.setItem(SAVE_SLOT, item(Material.EMERALD_BLOCK, "&a&lKaydet ve Kapat",
                "&7Tüm değişiklikleri config.yml'ye yazar"));
        s.inventory.setItem(CLOSE_SLOT, item(Material.BARRIER, "&cKapat", "&7Kaydetmeden kapatır"));
        s.inventory.setItem(10, item(Material.COMPASS, "&bLokasyon Ayarları",
                "&7Preset, mesafe, tampon bölge, WorldGuard, Towny"));
        s.inventory.setItem(11, item(Material.TNT, "&cTehlike Ayarları",
                "&7Radyasyon hasarı, yel itme şiddeti, EMP"));
        s.inventory.setItem(12, item(Material.DIAMOND_SWORD, "&6Savaş Ayarları",
                "&7Hasar sıralaması, actionbar mesajları"));
        s.inventory.setItem(14, item(Material.ANVIL, "&eDüşüş & Sandık",
                "&7Düşüş yüksekliği, sandık gecikmesi, eşik"));
        s.inventory.setItem(13, item(Material.FIRE_CHARGE, "&6Bilet Ayarları",
                "&7Bilet materyali ve bekleme süresi"));
        s.inventory.setItem(15, item(Material.CLOCK, "&5Otomatik Meteor",
                "&7Aralık, TPS koruması, konum soğuması"));
        s.inventory.setItem(16, item(Material.FIRE_CHARGE, "&dMeteor Türleri",
                "&7Her tür için yarıçap, boss, süre"));
        s.inventory.setItem(20, item(Material.ZOMBIE_HEAD, "&aMob Ayarları",
                "&7MythicMobs listesi ve spawn offsetleri"));
        s.inventory.setItem(22, item(Material.CHEST, "&6Loot Ayarları",
                "&7Her tür için loot bloğu, erişim modu, kişisel/ortak"));
        s.inventory.setItem(23, item(Material.BOOK, "&6Ödül Ayarları",
                "&7Ödül komutları ve sıralama ödülleri"));
        s.inventory.setItem(24, item(Material.FILLED_MAP, "&8Şematik & Geri Yükleme",
                "&7Varsayılan şematik, geri yükleme, zaman aşımı"));
    }

    // ────────────────────────────────────────────────────────────────
    //  LOCATION PAGE
    // ────────────────────────────────────────────────────────────────

    private void drawLocationPage(Session s) {
        s.page = PAGE_LOCATION;
        s.inventory.clear();
        fillBorder(s);
        s.inventory.setItem(BACK_SLOT, item(Material.ARROW, "&7← Ana Menü"));
        s.inventory.setItem(SAVE_SLOT, item(Material.EMERALD_BLOCK, "&a&lKaydet"));
        final var preset = config.getLocationPreset();
        s.inventory.setItem(10, item(Material.COMPASS, "&eAktif Preset: &f" + s.locationPreset,
                "&7Sol/sağ tıkla değiştir"));
        s.inventory.setItem(11, item(Material.LEATHER_BOOTS, "&bMin Uzaklık: &f" + s.minDistance,
                "&7Spawn'a min mesafe", "&7Sol/sağ: ±100 &7Shift: ±500"));
        s.inventory.setItem(12, item(Material.DIAMOND_BOOTS, "&bMax Uzaklık: &f" + s.maxDistance,
                "&7Spawn'dan max mesafe", "&7Sol/sağ: ±100 &7Shift: ±500"));
        s.inventory.setItem(13, item(Material.STONE, "&7Arazi Toleransı: &f" + s.terrainVariance,
                "&7Engebeli arazi toleransı", "&7Sol/sağ: ±1 &7Shift: ±5"));
        s.inventory.setItem(14, item(Material.IRON_BARS, "&7Tampon Bölge: &f" + s.bufferZone,
                "&7Claim'lere min mesafe", "&7Sol/sağ: ±10 &7Shift: ±50"));
        s.inventory.setItem(15, item(s.wgCheck ? Material.LIME_DYE : Material.GRAY_DYE,
                "&eWorldGuard Koruması: " + bool(s.wgCheck),
                "&7Tıkla aç/kapat"));
        s.inventory.setItem(16, item(s.townyCheck ? Material.LIME_DYE : Material.GRAY_DYE,
                "&eTowny Vahşi Doğa: " + bool(s.townyCheck),
                "&7Tıkla aç/kapat"));
    }

    // ────────────────────────────────────────────────────────────────
    //  HAZARDS PAGE
    // ────────────────────────────────────────────────────────────────

    private void drawHazardsPage(Session s) {
        s.page = PAGE_HAZARDS;
        s.inventory.clear();
        fillBorder(s);
        s.inventory.setItem(BACK_SLOT, item(Material.ARROW, "&7← Ana Menü"));
        s.inventory.setItem(SAVE_SLOT, item(Material.EMERALD_BLOCK, "&a&lKaydet"));
        s.inventory.setItem(10, item(Material.SKELETON_SKULL, "&cRadyasyon Hasarı: &f" + s.radiation + "/sn",
                "&7Sol/sağ: ±1"));
        s.inventory.setItem(11, item(Material.FEATHER, "&bYel İtme Çarpanı: &f" + s.windKnockback + "x",
                "&7Sol/sağ: ±0.1 &7Shift: ±0.5"));
        s.inventory.setItem(12, item(Material.CLOCK, "&bYel Aralığı: &f" + s.windInterval + " tick",
                "&7Sol/sağ: ±20"));
        s.inventory.setItem(14, item(s.disableElytra ? Material.LIME_DYE : Material.GRAY_DYE,
                "&eElytra Engelle: " + bool(s.disableElytra),
                "&7Tıkla aç/kapat"));
        s.inventory.setItem(15, item(s.disablePearl ? Material.LIME_DYE : Material.GRAY_DYE,
                "&eEnder İncisi Engelle: " + bool(s.disablePearl),
                "&7Tıkla aç/kapat"));
    }

    // ────────────────────────────────────────────────────────────────
    //  COMBAT PAGE
    // ────────────────────────────────────────────────────────────────

    private void drawCombatPage(Session s) {
        s.page = PAGE_COMBAT;
        s.inventory.clear();
        fillBorder(s);
        s.inventory.setItem(BACK_SLOT, item(Material.ARROW, "&7← Ana Menü"));
        s.inventory.setItem(SAVE_SLOT, item(Material.EMERALD_BLOCK, "&a&lKaydet"));
        s.inventory.setItem(10, item(s.damageBar ? Material.LIME_DYE : Material.GRAY_DYE,
                "&cHasar ActionBar: " + bool(s.damageBar), "&7Tıkla aç/kapat"));
        s.inventory.setItem(11, item(s.killBar ? Material.LIME_DYE : Material.GRAY_DYE,
                "&cÖldürme ActionBar: " + bool(s.killBar), "&7Tıkla aç/kapat"));
        s.inventory.setItem(12, item(s.leaderboard ? Material.LIME_DYE : Material.GRAY_DYE,
                "&6Liderlik Tablosu: " + bool(s.leaderboard), "&7Tıkla aç/kapat"));
        s.inventory.setItem(13, item(Material.PLAYER_HEAD, "&6Liderlik Boyutu: &f" + s.leaderboardSize,
                "&7Sol/sağ: ±1"));
        s.inventory.setItem(14, item(Material.EMERALD, "&aÖdül Üst Sıra: &f" + s.rewardTopCount,
                "&7Kaç kişi sandık açabilir", "&7Sol/sağ: ±1"));
    }

    // ────────────────────────────────────────────────────────────────
    //  FALL & VAULT PAGE
    // ────────────────────────────────────────────────────────────────

    private void drawFallVaultPage(Session s) {
        s.page = PAGE_FALL_VAULT;
        s.inventory.clear();
        fillBorder(s);
        s.inventory.setItem(BACK_SLOT, item(Material.ARROW, "&7← Ana Menü"));
        s.inventory.setItem(SAVE_SLOT, item(Material.EMERALD_BLOCK, "&a&lKaydet"));
        s.inventory.setItem(10, item(Material.FEATHER, "&eNormal Düşüş: &f" + s.fallNormal + " blok",
                "&7Sol/sağ: ±10"));
        s.inventory.setItem(11, item(Material.ELYTRA, "&eYavaş Düşüş: &f" + s.fallSlow + " blok",
                "&7Sol/sağ: ±10"));
        s.inventory.setItem(12, item(s.impactCore ? Material.LIME_DYE : Material.GRAY_DYE,
                "&eÇarpışma Çekirdeği: " + bool(s.impactCore), "&7Tıkla aç/kapat"));
        s.inventory.setItem(14, item(Material.ANCIENT_DEBRIS, "&dSandık Bloğu: &f" + s.lootBlock,
                "&7İmleçte blokla tıkla değiştir"));
        s.inventory.setItem(15, item(Material.REDSTONE, "&dBoss Eşiği: &f" + s.bossThreshold + "%",
                "&7Anahtar için min hasar", "&7Sol/sağ: ±5"));
        s.inventory.setItem(16, item(Material.REPEATER, "&dSandık Gecikmesi: &f" + s.vaultDelay + " tick",
                "&7Boss ölümü sonrası", "&7Sol/sağ: ±20"));
    }

    // ────────────────────────────────────────────────────────────────
    //  AUTO EVENTS PAGE
    // ────────────────────────────────────────────────────────────────

    private void drawAutoPage(Session s) {
        s.page = PAGE_AUTO;
        s.inventory.clear();
        fillBorder(s);
        s.inventory.setItem(BACK_SLOT, item(Material.ARROW, "&7← Ana Menü"));
        s.inventory.setItem(SAVE_SLOT, item(Material.EMERALD_BLOCK, "&a&lKaydet"));
        s.inventory.setItem(10, item(s.autoEnabled ? Material.LIME_DYE : Material.GRAY_DYE,
                "&5Otomatik Aktif: " + bool(s.autoEnabled), "&7Tıkla aç/kapat"));
        s.inventory.setItem(11, item(Material.CLOCK, "&5Min Aralık: &f" + s.autoMin + " dk",
                "&7Sol/sağ: ±5"));
        s.inventory.setItem(12, item(Material.CLOCK, "&5Max Aralık: &f" + s.autoMax + " dk",
                "&7Sol/sağ: ±5"));
        s.inventory.setItem(14, item(s.tpsGuard ? Material.LIME_DYE : Material.GRAY_DYE,
                "&eTPS Koruması: " + bool(s.tpsGuard), "&7Tıkla aç/kapat"));
        s.inventory.setItem(15, item(Material.SHIELD, "&eMin TPS: &f" + s.minTps,
                "&7Sol/sağ: ±0.5"));
        s.inventory.setItem(20, item(s.cooldownEnabled ? Material.LIME_DYE : Material.GRAY_DYE,
                "&eKonum Soğuması: " + bool(s.cooldownEnabled), "&7Tıkla aç/kapat"));
        s.inventory.setItem(21, item(Material.MAP, "&eSoğuma Yarıçapı: &f" + s.cooldownRadius,
                "&7Sol/sağ: ±100 &7Shift: ±500"));
        s.inventory.setItem(22, item(Material.CLOCK, "&eSoğuma Süresi: &f" + s.cooldownHours + " saat",
                "&7Sol/sağ: ±6"));
    }

    // ────────────────────────────────────────────────────────────────
    //  METEOR TYPES MENU
    // ────────────────────────────────────────────────────────────────

    private void drawTypesMenu(Session s) {
        s.page = PAGE_TYPES_MENU;
        s.inventory.clear();
        fillBorder(s);
        s.inventory.setItem(BACK_SLOT, item(Material.ARROW, "&7← Ana Menü"));
        s.inventory.setItem(SAVE_SLOT, item(Material.EMERALD_BLOCK, "&a&lKaydet ve Kapat"));
        int slot = 10;
        final MeteorType[] types = MeteorType.values();
        for (int i = 0; i < types.length && slot < 35; i++) {
            final MeteorType type = types[i];
            final var tc = config.getTypeConfig(type);
            s.inventory.setItem(slot++, item(Material.FIRE_CHARGE,
                    config.getMeteorTypeColor(type) + config.getMeteorTypeName(type),
                    "&7Yarıçap: " + tc.impactRadius(),
                    "&7Boss: " + (tc.bossMythicMob().isEmpty() ? "&7Yok" : "&c" + tc.bossMythicMob()),
                    "&7&lTıkla düzenle"));
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  METEOR TYPE EDIT PAGE
    // ────────────────────────────────────────────────────────────────

    private void drawTypeEditPage(Session s) {
        s.page = PAGE_TYPE_EDIT;
        s.inventory.clear();
        fillBorder(s);
        s.inventory.setItem(BACK_SLOT, item(Material.ARROW, "&7← Türler Menüsü"));
        s.inventory.setItem(SAVE_SLOT, item(Material.EMERALD_BLOCK, "&a&lKaydet ve Kapat"));
        final MeteorType type = s.editingType;
        if (type == null) return;
        s.inventory.setItem(10, item(Material.STONE, "&7Yarıçap: &f" + s.editRadius,
                "&7Çarpma alanı yarıçapı", "&7Sol/sağ: ±5 &7Shift: ±10"));
        s.inventory.setItem(11, item(Material.COMPASS, "&7Yarıçap Şekli: &f" + s.editShape.name(),
                "&7Sol/sağ tıkla değiştir"));
        s.inventory.setItem(12, item(Material.IRON_SWORD, "&7Boss Çarpanı: &f" + s.editBossMult + "x",
                "&7Sol/sağ: ±0.1 &7Shift: ±0.5"));
        s.inventory.setItem(13, item(Material.ZOMBIE_HEAD, "&7Boss Mob: &f" + (s.editBossMobId.isEmpty() ? "&7Yok" : s.editBossMobId),
                "&7MythicMobs boss ID", "&7Sol/sağ tıkla değiştir"));
        s.inventory.setItem(14, item(Material.ANVIL, "&7Ön Süre: &f" + s.editPreImpact + " sn",
                "&7Çarpma öncesi uyarı süresi", "&7Sol/sağ: ±10"));
        s.inventory.setItem(15, item(Material.FIRE_CHARGE, "&7Etkinlik Süresi: &f" + s.editDuration + " sn",
                "&7Sol/sağ: ±30"));
        s.inventory.setItem(16, item(Material.WATER_BUCKET, "&7Geri Yükleme: &f" + s.editRollback + " sn",
                "&7Sol/sağ: ±10"));
        s.inventory.setItem(20, item(Material.FEATHER, "&7Düşüş Modu: &f" + s.editFallMode.name(),
                "&7Sol/sağ tıkla değiştir"));
        s.inventory.setItem(21, item(Material.CLOCK, "&7Normal Düşüş: &f" + s.editNormalFall + " sn",
                "&7Sol/sağ: ±2"));
        s.inventory.setItem(22, item(Material.ELYTRA, "&7Yavaş Düşüş: &f" + s.editSlowFall + " sn",
                "&7Sol/sağ: ±2"));
        s.inventory.setItem(23, item(Material.ZOMBIE_HEAD, "&aDalga Sayısı: &f" + s.editWaveCount,
                "&7Bu tür için dalga sayısı", "&7Sol/sağ: ±1 &7Shift: ±5"));
        s.inventory.setItem(24, item(Material.CLOCK, "&aDalga Aralığı: &f" + s.editWaveInterval + " sn",
                "&7Sol/sağ: ±5"));
    }

    // ────────────────────────────────────────────────────────────────
    //  CLICK HANDLER
    // ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        final Session s = sessions.get(player.getUniqueId());
        if (s == null || event.getInventory() != s.inventory) return;
        if (event.getRawSlot() >= 54) {
            if (event.isShiftClick()) event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        final int slot = event.getRawSlot();

        switch (s.page) {
            case PAGE_MAIN -> handleMainClick(s, slot, event);
            case PAGE_LOCATION -> handleLocationClick(s, slot, event);
            case PAGE_HAZARDS -> handleHazardsClick(s, slot, event);
            case PAGE_COMBAT -> handleCombatClick(s, slot, event);
            case PAGE_FALL_VAULT -> handleFallVaultClick(s, slot, event);
            case PAGE_AUTO -> handleAutoClick(s, slot, event);
            case PAGE_TYPES_MENU -> handleTypesMenuClick(s, slot, event);
            case PAGE_TYPE_EDIT -> handleTypeEditClick(s, slot, event);
            case PAGE_LOOT_MENU -> handleLootMenuClick(s, slot, event);
            case PAGE_LOOT_EDIT -> handleLootEditClick(s, slot, event);
            case PAGE_REWARDS_MENU -> handleRewardsMenuClick(s, slot, event);
            case PAGE_REWARDS_EDIT -> handleRewardsEditClick(s, slot, event);
            case PAGE_TICKET -> handleTicketClick(s, slot, event);
            case PAGE_MOBS_MENU -> handleMobsMenuClick(s, slot, event);
            case PAGE_MOBS_EDIT -> handleMobsEditClick(s, slot, event);
            case PAGE_SCHEMATIC -> handleSchematicClick(s, slot, event);
        }
    }

    private void handleMainClick(Session s, int slot, InventoryClickEvent event) {
        if (slot == SAVE_SLOT) { saveAll(s); close(s, event); }
        else if (slot == CLOSE_SLOT) close(s, event);
        else if (slot == 10) drawLocationPage(s);
        else if (slot == 11) drawHazardsPage(s);
        else if (slot == 12) drawCombatPage(s);
        else if (slot == 13) drawTicketPage(s);
        else if (slot == 14) drawFallVaultPage(s);
        else if (slot == 15) drawAutoPage(s);
        else if (slot == 16) drawTypesMenu(s);
        else if (slot == 20) drawMobsMenuPage(s);
        else if (slot == 22) drawLootMenuPage(s);
        else if (slot == 23) drawRewardsMenuPage(s);
        else if (slot == 24) drawSchematicPage(s);
    }

    private void handleLocationClick(Session s, int slot, InventoryClickEvent event) {
        if (slot == BACK_SLOT) { drawMainMenu(s); return; }
        if (slot == SAVE_SLOT) { saveAll(s); drawMainMenu(s); return; }
        final boolean right = event.isRightClick();
        final boolean shift = event.isShiftClick();
        if (slot == 10) cyclePreset(s, right ? -1 : 1);
        else if (slot == 11) s.minDistance = clamp(s.minDistance + (shift ? 500 : 100) * (right ? -1 : 1), 0, 100000);
        else if (slot == 12) s.maxDistance = clamp(s.maxDistance + (shift ? 500 : 100) * (right ? -1 : 1), 1, 100000);
        else if (slot == 13) s.terrainVariance = clamp(s.terrainVariance + (shift ? 5 : 1) * (right ? -1 : 1), 0, 100);
        else if (slot == 14) s.bufferZone = clamp(s.bufferZone + (shift ? 50 : 10) * (right ? -1 : 1), 0, 1000);
        else if (slot == 15) s.wgCheck = !s.wgCheck;
        else if (slot == 16) s.townyCheck = !s.townyCheck;
        drawLocationPage(s);
    }

    private void handleHazardsClick(Session s, int slot, InventoryClickEvent event) {
        if (slot == BACK_SLOT) { drawMainMenu(s); return; }
        if (slot == SAVE_SLOT) { saveAll(s); drawMainMenu(s); return; }
        final boolean right = event.isRightClick();
        final boolean shift = event.isShiftClick();
        if (slot == 10) s.radiation = clamp(s.radiation + (right ? -1 : 1), 0, 100);
        else if (slot == 11) s.windKnockback = Math.round(clamp(s.windKnockback + (shift ? 0.5 : 0.1) * (right ? -1 : 1), 0, 20) * 10.0) / 10.0;
        else if (slot == 12) s.windInterval = clamp(s.windInterval + (right ? -20 : 20), 20, 600);
        else if (slot == 14) s.disableElytra = !s.disableElytra;
        else if (slot == 15) s.disablePearl = !s.disablePearl;
        drawHazardsPage(s);
    }

    private void handleCombatClick(Session s, int slot, InventoryClickEvent event) {
        if (slot == BACK_SLOT) { drawMainMenu(s); return; }
        if (slot == SAVE_SLOT) { saveAll(s); drawMainMenu(s); return; }
        final boolean right = event.isRightClick();
        if (slot == 10) s.damageBar = !s.damageBar;
        else if (slot == 11) s.killBar = !s.killBar;
        else if (slot == 12) s.leaderboard = !s.leaderboard;
        else if (slot == 13) s.leaderboardSize = clamp(s.leaderboardSize + (right ? -1 : 1), 1, 50);
        else if (slot == 14) s.rewardTopCount = clamp(s.rewardTopCount + (right ? -1 : 1), 1, 50);
        drawCombatPage(s);
    }

    private void handleFallVaultClick(Session s, int slot, InventoryClickEvent event) {
        if (slot == BACK_SLOT) { drawMainMenu(s); return; }
        if (slot == SAVE_SLOT) { saveAll(s); drawMainMenu(s); return; }
        final boolean right = event.isRightClick();
        if (slot == 10) s.fallNormal = clamp(s.fallNormal + (right ? -10 : 10), 5, 500);
        else if (slot == 11) s.fallSlow = clamp(s.fallSlow + (right ? -10 : 10), 5, 500);
        else if (slot == 12) s.impactCore = !s.impactCore;
        else if (slot == 14) changeLootBlock(s, event.getCursor(), right ? -1 : 1);
        else if (slot == 15) s.bossThreshold = clamp(s.bossThreshold + (right ? -5 : 5), 0, 100);
        else if (slot == 16) s.vaultDelay = clamp(s.vaultDelay + (right ? -20 : 20), 0, 600);
        drawFallVaultPage(s);
    }

    private void handleAutoClick(Session s, int slot, InventoryClickEvent event) {
        if (slot == BACK_SLOT) { drawMainMenu(s); return; }
        if (slot == SAVE_SLOT) { saveAll(s); drawMainMenu(s); return; }
        final boolean right = event.isRightClick();
        final boolean shift = event.isShiftClick();
        if (slot == 10) s.autoEnabled = !s.autoEnabled;
        else if (slot == 11) s.autoMin = clamp(s.autoMin + (right ? -5 : 5), 1, s.autoMax);
        else if (slot == 12) s.autoMax = clamp(s.autoMax + (right ? -5 : 5), s.autoMin, 10080);
        else if (slot == 14) s.tpsGuard = !s.tpsGuard;
        else if (slot == 15) s.minTps = Math.round(clamp(s.minTps + (right ? -0.5 : 0.5), 5, 20) * 10.0) / 10.0;
        else if (slot == 20) s.cooldownEnabled = !s.cooldownEnabled;
        else if (slot == 21) s.cooldownRadius = clamp(s.cooldownRadius + (shift ? 500 : 100) * (right ? -1 : 1), 0, 50000);
        else if (slot == 22) s.cooldownHours = clamp(s.cooldownHours + (right ? -6 : 6), 1, 720);
        drawAutoPage(s);
    }

    private void handleTypesMenuClick(Session s, int slot, InventoryClickEvent event) {
        if (slot == BACK_SLOT) { drawMainMenu(s); return; }
        if (slot == SAVE_SLOT) { saveAll(s); close(s, event); return; }
        final MeteorType[] types = MeteorType.values();
        final int typeIndex = slot - 10;
        if (typeIndex >= 0 && typeIndex < types.length) {
            s.editingType = types[typeIndex];
            final var tc = config.getTypeConfig(s.editingType);
            s.editRadius = tc.impactRadius();
            s.editShape = config.getRadiusShape(s.editingType);
            s.editBossMult = tc.bossHealthMultiplier();
            s.editBossMobId = tc.bossMythicMob();
            s.editPreImpact = tc.preImpactDurationSeconds();
            s.editDuration = tc.eventDurationSeconds();
            s.editRollback = tc.rollbackDurationSeconds();
            s.editFallMode = config.getFallMode(s.editingType);
            s.editNormalFall = config.getFallDurationSeconds(s.editingType, MeteorFallMode.NORMAL);
            s.editSlowFall = config.getFallDurationSeconds(s.editingType, MeteorFallMode.SLOW);
            s.editWaveCount = config.getWaveCount(s.editingType);
            s.editWaveInterval = config.getWaveIntervalSeconds(s.editingType);
            drawTypeEditPage(s);
        }
    }

    private void handleTypeEditClick(Session s, int slot, InventoryClickEvent event) {
        if (slot == BACK_SLOT) { drawTypesMenu(s); return; }
        if (slot == SAVE_SLOT) { saveAll(s); close(s, event); return; }
        final boolean right = event.isRightClick();
        final boolean shift = event.isShiftClick();
        if (slot == 10) s.editRadius = clamp(s.editRadius + (shift ? 10 : 5) * (right ? -1 : 1), 5, 500);
        else if (slot == 11) cycleShape(s, right ? -1 : 1);
        else if (slot == 12) s.editBossMult = Math.round(clamp(s.editBossMult + (shift ? 0.5 : 0.1) * (right ? -1 : 1), 0, 50) * 10.0) / 10.0;
        else if (slot == 13) cycleBossMob(s, right ? -1 : 1);
        else if (slot == 14) s.editPreImpact = clamp(s.editPreImpact + (right ? -10 : 10), 5, 600);
        else if (slot == 15) s.editDuration = clamp(s.editDuration + (right ? -30 : 30), 30, 3600);
        else if (slot == 16) s.editRollback = clamp(s.editRollback + (right ? -10 : 10), 5, 600);
        else if (slot == 20) cycleFallMode(s, right ? -1 : 1);
        else if (slot == 21) s.editNormalFall = clamp(s.editNormalFall + (right ? -2 : 2), 2, 120);
        else if (slot == 22) s.editSlowFall = clamp(s.editSlowFall + (right ? -2 : 2), 5, 120);
        else if (slot == 23) s.editWaveCount = clamp(s.editWaveCount + (shift ? 5 : 1) * (right ? -1 : 1), 1, 50);
        else if (slot == 24) s.editWaveInterval = clamp(s.editWaveInterval + (right ? -5 : 5), 1, 300);
        drawTypeEditPage(s);
    }

    // ────────────────────────────────────────────────────────────────
    //  LOOT PAGES
    // ────────────────────────────────────────────────────────────────

    private void drawLootMenuPage(Session s) {
        s.page = PAGE_LOOT_MENU;
        s.inventory.clear();
        fillBorder(s);
        s.inventory.setItem(BACK_SLOT, item(Material.ARROW, "&7← Ana Menü"));
        s.inventory.setItem(SAVE_SLOT, item(Material.EMERALD_BLOCK, "&a&lKaydet ve Kapat"));
        int slot = 10;
        for (MeteorType type : MeteorType.values()) {
            if (slot >= 35) break;
            s.inventory.setItem(slot++, item(Material.CHEST,
                    config.getMeteorTypeColor(type) + config.getMeteorTypeName(type),
                    "&7Bloğu: " + config.getLootBlockMaterial(type).name(),
                    "&7Mod: " + config.getLootAccessMode(type),
                    "&7Kişisel: " + (config.isPersonalLoot(type) ? "&aEvet" : "&cHayır"),
                    "&7&lTıkla düzenle"));
        }
    }

    private void drawLootEditPage(Session s) {
        s.page = PAGE_LOOT_EDIT;
        s.inventory.clear();
        fillBorder(s);
        s.inventory.setItem(BACK_SLOT, item(Material.ARROW, "&7← Loot Menüsü"));
        s.inventory.setItem(SAVE_SLOT, item(Material.EMERALD_BLOCK, "&a&lKaydet ve Kapat"));
        final MeteorType type = s.editingType;
        if (type == null) return;
        s.inventory.setItem(10, item(s.editPersonalLoot ? Material.LIME_DYE : Material.GRAY_DYE,
                "&eKişisel Loot: " + (s.editPersonalLoot ? "&a&lAÇIK" : "&c&lKAPALI"),
                "&7Açık: her oyuncuya ayrı ganimet",
                "&7Kapalı: herkes için ortak envanter",
                "&7Tıkla değiştir"));
        s.inventory.setItem(11, item(s.editLootBlock != null ? s.editLootBlock : Material.CHEST,
                "&dLoot Bloğu: &f" + (s.editLootBlock != null ? s.editLootBlock.name() : "CHEST"),
                "&7İmleçte blokla tıkla değiştir",
                "&7Sol/sağ: önceden tanımlı bloklar"));
        s.inventory.setItem(12, item(Material.COMPASS, "&6Erişim Modu: &f" + s.editAccessMode,
                "&7AUTO: etkileşimli blok sağ tık, diğer kazma",
                "&7INTERACT: sadece sağ tık",
                "&7BREAK: sadece kazma",
                "&7BOTH: hem sağ tık hem kazma",
                "&7Sol/sağ tıkla değiştir"));
    }

    private void handleLootMenuClick(Session s, int slot, InventoryClickEvent event) {
        if (slot == BACK_SLOT) { drawMainMenu(s); return; }
        if (slot == SAVE_SLOT) { saveAll(s); close(s, event); return; }
        final int typeIndex = slot - 10;
        final MeteorType[] types = MeteorType.values();
        if (typeIndex >= 0 && typeIndex < types.length) {
            s.editingType = types[typeIndex];
            s.editLootBlock = config.getLootBlockMaterial(s.editingType);
            s.editAccessMode = config.getLootAccessMode(s.editingType);
            s.editPersonalLoot = config.isPersonalLoot(s.editingType);
            drawLootEditPage(s);
        }
    }

    private void handleLootEditClick(Session s, int slot, InventoryClickEvent event) {
        if (slot == BACK_SLOT) { drawLootMenuPage(s); return; }
        if (slot == SAVE_SLOT) { saveAll(s); close(s, event); return; }
        final boolean right = event.isRightClick();
        if (slot == 10) { s.editPersonalLoot = !s.editPersonalLoot; drawLootEditPage(s); }
        else if (slot == 11) changeEditLootBlock(s, event.getCursor(), right ? -1 : 1);
        else if (slot == 12) cycleAccessMode(s, right ? -1 : 1);
        drawLootEditPage(s);
    }

    private void changeEditLootBlock(Session s, ItemStack cursor, int delta) {
        if (cursor != null && !cursor.isEmpty() && cursor.getType().isBlock()
                && cursor.getType().isItem()) {
            s.editLootBlock = cursor.getType();
            return;
        }
        if (s.editLootBlock == null) { s.editLootBlock = LOOT_BLOCKS[0]; return; }
        int idx = 0;
        for (int i = 0; i < LOOT_BLOCKS.length; i++) if (LOOT_BLOCKS[i] == s.editLootBlock) idx = i;
        s.editLootBlock = LOOT_BLOCKS[Math.floorMod(idx + delta, LOOT_BLOCKS.length)];
    }

    private void cycleAccessMode(Session s, int delta) {
        final String[] modes = {"AUTO", "INTERACT", "BREAK", "BOTH"};
        int idx = 0;
        for (int i = 0; i < modes.length; i++) if (modes[i].equals(s.editAccessMode)) idx = i;
        s.editAccessMode = modes[Math.floorMod(idx + delta, modes.length)];
    }

    // ────────────────────────────────────────────────────────────────
    //  REWARDS PAGES
    // ────────────────────────────────────────────────────────────────

    private void drawRewardsMenuPage(Session s) {
        s.page = PAGE_REWARDS_MENU;
        s.inventory.clear();
        fillBorder(s);
        s.inventory.setItem(BACK_SLOT, item(Material.ARROW, "&7← Ana Menü"));
        s.inventory.setItem(SAVE_SLOT, item(Material.EMERALD_BLOCK, "&a&lKaydet ve Kapat"));
        int slot = 10;
        for (MeteorType type : MeteorType.values()) {
            if (slot >= 35) break;
            s.inventory.setItem(slot++, item(Material.BOOK,
                    config.getMeteorTypeColor(type) + config.getMeteorTypeName(type),
                    "&7Ödül komutlarını ve sıralama ödüllerini düzenle",
                    "&7&lTıkla düzenle"));
        }
    }

    private void drawRewardsEditPage(Session s) {
        s.page = PAGE_REWARDS_EDIT;
        s.inventory.clear();
        fillBorder(s);
        s.inventory.setItem(BACK_SLOT, item(Material.ARROW, "&7← Ödül Menüsü"));
        s.inventory.setItem(SAVE_SLOT, item(Material.EMERALD_BLOCK, "&a&lKaydet ve Kapat"));
        final MeteorType type = s.editingType;
        if (type == null) return;
        // Use edited lists if modified, otherwise read from config
        final List<String> generalCmds = s.rewardsEdited
                ? s.editGeneralCommands : config.getTypeConfig(type).rewardsCommands();
        final List<String> generalItems = s.rewardsEdited
                ? s.editGeneralItems : config.getRankingRewardItems(type, 0);
        s.inventory.setItem(10, item(Material.BOOK, "&eGenel Ödül Komutları",
                "&7Bu meteor türü için kasa açılış komutları",
                "&7&lSayı: &f" + generalCmds.size(),
                "&7&lTıkla düzenle (sohbet)"));
        s.inventory.setItem(11, item(Material.CHEST, "&eGenel Ödül Eşyaları",
                "&7Bu meteor türü için kasa açılış eşyaları",
                "&7&lSayı: &f" + generalItems.size(),
                "&7&lTıkla düzenle (sohbet)"));
        final String rankLabel = s.editRank == 0 ? "&6Genel Ödüller" : "&6Sıralama Ödülleri &7(#" + s.editRank + ")";
        final String slotLabel = s.editRank == 0 ? "&bGenel" : "&bSıra #" + s.editRank;
        s.inventory.setItem(14, item(Material.EMERALD, rankLabel,
                "&7Sol/sağ: sırayı değiştir",
                "&7Düzenlemek için tıkla"));
        // Show rank-specific info
        final List<String> rankItems = s.rewardsEdited
                ? s.editRankItems : config.getRankingRewardItems(type, s.editRank);
        final List<String> rankCmds = s.rewardsEdited
                ? s.editRankCmds : config.getRankingRewardCommands(type, s.editRank);
        s.inventory.setItem(15, item(Material.DIAMOND,
                slotLabel + " &7- Eşyalar",
                rankItems.isEmpty()
                        ? new String[]{"&7Henüz eşya eklenmemiş"}
                        : rankItems.stream().limit(5).map(i -> "&7- " + i).toArray(String[]::new)));
        s.inventory.setItem(16, item(Material.PAPER,
                slotLabel + " &7- Komutlar",
                rankCmds.isEmpty()
                        ? new String[]{"&7Henüz komut eklenmemiş"}
                        : rankCmds.stream().limit(5).map(c -> "&7- " + c).toArray(String[]::new)));
    }

    private void handleRewardsMenuClick(Session s, int slot, InventoryClickEvent event) {
        if (slot == BACK_SLOT) { drawMainMenu(s); return; }
        if (slot == SAVE_SLOT) { saveAll(s); close(s, event); return; }
        final int typeIndex = slot - 10;
        final MeteorType[] types = MeteorType.values();
        if (typeIndex >= 0 && typeIndex < types.length) {
            s.editingType = types[typeIndex];
            s.editRank = 0;
            // Load current config values into editable lists
            s.rewardsEdited = false;
            s.editGeneralCommands.clear();
            s.editGeneralCommands.addAll(config.getTypeConfig(s.editingType).rewardsCommands());
            s.editGeneralItems.clear();
            s.editGeneralItems.addAll(config.getRankingRewardItems(s.editingType, 0));
            s.editRankCmds.clear();
            s.editRankCmds.addAll(config.getRankingRewardCommands(s.editingType, 0));
            s.editRankItems.clear();
            s.editRankItems.addAll(config.getRankingRewardItems(s.editingType, 0));
            drawRewardsEditPage(s);
        }
    }

    private void handleRewardsEditClick(Session s, int slot, InventoryClickEvent event) {
        if (slot == BACK_SLOT) { drawRewardsMenuPage(s); return; }
        if (slot == SAVE_SLOT) { saveAll(s); close(s, event); return; }
        final boolean right = event.isRightClick();
        if (slot == 14) {
            // 0=genel ödüller, 1-3=sıralama ödülleri
            s.editRank = Math.floorMod(s.editRank + (right ? -1 : 1), 4);
            // Rank değişince rank listelerini config'den yeniden yükle (genel listeler etkilenmez)
            s.editRankCmds.clear();
            s.editRankCmds.addAll(config.getRankingRewardCommands(s.editingType, s.editRank));
            s.editRankItems.clear();
            s.editRankItems.addAll(config.getRankingRewardItems(s.editingType, s.editRank));
            drawRewardsEditPage(s);
        } else if (slot == 10 || slot == 11 || slot == 15 || slot == 16) {
            // Chat-based editing
            final Player player = (Player) event.getWhoClicked();
            promptRewardsChatEdit(player, s, slot, event);
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  TICKET PAGE
    // ────────────────────────────────────────────────────────────────

    private void drawTicketPage(Session s) {
        s.page = PAGE_TICKET;
        s.inventory.clear();
        fillBorder(s);
        s.inventory.setItem(BACK_SLOT, item(Material.ARROW, "&7← Ana Menü"));
        s.inventory.setItem(SAVE_SLOT, item(Material.EMERALD_BLOCK, "&a&lKaydet"));
        final Material mat = Material.matchMaterial(s.ticketMaterial);
        s.inventory.setItem(10, item(mat != null ? mat : Material.FIRE_CHARGE,
                "&6Bilet Materyali: &f" + s.ticketMaterial,
                "&7İmleçte eşyayla tıkla değiştir",
                "&7Sol/sağ: önceden tanımlı eşyalar"));
        s.inventory.setItem(11, item(Material.CLOCK,
                "&6Bekleme Süresi: &f" + s.ticketCooldown + " sn",
                "&7Sol/sağ: ±10 &7Shift: ±60"));
    }

    private void handleTicketClick(Session s, int slot, InventoryClickEvent event) {
        if (slot == BACK_SLOT) { drawMainMenu(s); return; }
        if (slot == SAVE_SLOT) { saveAll(s); drawMainMenu(s); return; }
        final boolean right = event.isRightClick();
        final boolean shift = event.isShiftClick();
        if (slot == 10) changeTicketMaterial(s, event.getCursor(), right ? -1 : 1);
        else if (slot == 11) s.ticketCooldown = clamp(s.ticketCooldown + (shift ? 60 : 10) * (right ? -1 : 1), 0, 86400);
        drawTicketPage(s);
    }

    private static final String[] TICKET_MATERIALS = {"FIRE_CHARGE", "PAPER", "MAP",
            "NETHER_STAR", "ENDER_PEARL", "ENDER_EYE", "BLAZE_ROD", "BLAZE_POWDER",
            "MAGMA_CREAM", "GHAST_TEAR", "PHANTOM_MEMBRANE", "ECHO_SHARD", "AMETHYST_SHARD",
            "EXPERIENCE_BOTTLE", "BOOK", "ENCHANTED_BOOK"};

    // ────────────────────────────────────────────────────────────────
    //  MOBS PAGES
    // ────────────────────────────────────────────────────────────────

    private void drawMobsMenuPage(Session s) {
        s.page = PAGE_MOBS_MENU;
        s.inventory.clear();
        fillBorder(s);
        s.inventory.setItem(BACK_SLOT, item(Material.ARROW, "&7← Ana Menü"));
        s.inventory.setItem(SAVE_SLOT, item(Material.EMERALD_BLOCK, "&a&lKaydet ve Kapat"));
        int slot = 10;
        for (MeteorType type : MeteorType.values()) {
            if (slot >= 35) break;
            final int mobCount = config.getTypeConfig(type).mythicMobs().size();
            final int offsetCount = config.getMobSpawnOffsets(type).size();
            s.inventory.setItem(slot++, item(Material.ZOMBIE_HEAD,
                    config.getMeteorTypeColor(type) + config.getMeteorTypeName(type),
                    "&7Mob: &f" + mobCount + " adet",
                    "&7Spawn offset: &f" + offsetCount + " adet",
                    "&7&lTıkla düzenle"));
        }
    }

    private void drawMobsEditPage(Session s) {
        s.page = PAGE_MOBS_EDIT;
        s.inventory.clear();
        fillBorder(s);
        s.inventory.setItem(BACK_SLOT, item(Material.ARROW, "&7← Mob Menüsü"));
        s.inventory.setItem(SAVE_SLOT, item(Material.EMERALD_BLOCK, "&a&lKaydet ve Kapat"));
        final MeteorType type = s.editingType;
        if (type == null) return;
        final List<String> mobs = s.mobsEdited
                ? s.editMobs : config.getTypeConfig(type).mythicMobs();
        final int offsetCount = config.getMobSpawnOffsets(type).size();
        final int availableMobs = plugin.getMythicMobsHook().getMobIds().size();
        final var bossId = config.getTypeConfig(type).bossMythicMob();
        final List<String> mobLore = new ArrayList<>();
        mobLore.add("&7&lSayı: &f" + mobs.size());
        if (mobs.isEmpty()) {
            mobLore.add("&7Henüz mob eklenmemiş");
        } else {
            mobs.stream().limit(6).forEach(m -> mobLore.add("&7- " + m));
        }
        s.inventory.setItem(10, item(Material.SPAWNER,
                "&aYapılandırılmış Moblar",
                mobLore.toArray(new String[0])));
        s.inventory.setItem(11, item(Material.ENDER_PEARL,
                "&5Spawn Offsetleri: &f" + offsetCount + " adet",
                "&7Kurulumdan gelen mob spawn noktaları",
                "&7Düzenlemek için oyun içi kurulumu kullan"));
        s.inventory.setItem(12, item(Material.BOOK,
                "&7Sunucuda Mevcut MythicMobs: &f" + availableMobs,
                "&7Mob eklemek için MythicMobs ID'sini kullan",
                "&7Sol/sağ tıkla düzenle (sohbet)"));
        s.inventory.setItem(14, item(Material.BLAZE_ROD,
                "&cBoss Mob: &f" + (bossId.isEmpty() ? "&7Yok" : bossId),
                "&7Meteor Türü sayfasında düzenlenebilir"));
    }

    private void handleMobsMenuClick(Session s, int slot, InventoryClickEvent event) {
        if (slot == BACK_SLOT) { drawMainMenu(s); return; }
        if (slot == SAVE_SLOT) { saveAll(s); close(s, event); return; }
        final int typeIndex = slot - 10;
        final MeteorType[] types = MeteorType.values();
        if (typeIndex >= 0 && typeIndex < types.length) {
            s.editingType = types[typeIndex];
            s.mobsEdited = false;
            s.editMobs.clear();
            s.editMobs.addAll(config.getTypeConfig(s.editingType).mythicMobs());
            drawMobsEditPage(s);
        }
    }

    private void handleMobsEditClick(Session s, int slot, InventoryClickEvent event) {
        if (slot == BACK_SLOT) { drawMobsMenuPage(s); return; }
        if (slot == SAVE_SLOT) { saveAll(s); close(s, event); return; }
        if (slot == 10 || slot == 12) {
            final Player player = (Player) event.getWhoClicked();
            final String label = "MythicMobs";
            final MobsChatContext ctx = new MobsChatContext(s, s.editingType);
            mobsChatContexts.put(player.getUniqueId(), ctx);
            player.closeInventory();
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "&6&l✦ " + label + " düzenle");
            MessageUtil.sendMessage(player, "&7Eklemek için: &fmobId");
            MessageUtil.sendMessage(player, "&7Çıkarmak için: &f-mobId");
            MessageUtil.sendMessage(player, "&7Mevcut mobları görmek için: &flist");
            MessageUtil.sendMessage(player, "&7Bitince: &fdone");
            MessageUtil.sendMessage(player, "&7Sunucudaki tüm moblar: &f/olmeteor debug");
            MessageUtil.sendMessage(player, "");
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  MOBS CHAT EDIT
    // ────────────────────────────────────────────────────────────────

    private record MobsChatContext(@NotNull Session session, @NotNull MeteorType type) {}

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMobsChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final MobsChatContext ctx = mobsChatContexts.get(player.getUniqueId());
        if (ctx == null) return;
        event.setCancelled(true);
        final String input = event.getMessage().trim();
        plugin.getFoliaScheduler().callGlobal(() ->
                processMobsChatInput(player, ctx, input));
    }

    private void processMobsChatInput(Player player, MobsChatContext ctx, String input) {
        try {
            mobsChatContexts.remove(player.getUniqueId());
            if (input.equalsIgnoreCase("done") || input.equalsIgnoreCase("cancel")
                    || input.equalsIgnoreCase("exit") || input.equals("0")) {
                reopenMobsEditor(player, ctx);
                return;
            }
            if (input.equalsIgnoreCase("list")) {
                final List<String> currentMobs = ctx.session.editMobs;
                if (currentMobs.isEmpty()) {
                    MessageUtil.sendMessage(player, "&7Henüz mob eklenmemiş.");
                } else {
                    MessageUtil.sendMessage(player, "&6Mevcut moblar:");
                    currentMobs.forEach(m -> MessageUtil.sendMessage(player, "&7- &f" + m));
                }
                mobsChatContexts.put(player.getUniqueId(), ctx);
                return;
            }
            final boolean isRemove = input.startsWith("-");
            final String value = (isRemove || input.startsWith("+"))
                    ? input.substring(1).trim() : input.trim();
            if (value.isEmpty()) {
                MessageUtil.sendMessage(player, "&cGeçersiz giriş.");
                reopenMobsEditor(player, ctx);
                return;
            }
            if (isRemove) {
                final boolean removed = ctx.session.editMobs.remove(value);
                if (removed) {
                    MessageUtil.sendMessage(player, "&aKaldırıldı: &f" + value);
                } else {
                    MessageUtil.sendMessage(player, "&eBulunamadı: &f" + value);
                }
            } else {
                ctx.session.editMobs.add(value);
                MessageUtil.sendMessage(player, "&aEklendi: &f" + value);
            }
            ctx.session.mobsEdited = true;
            reopenMobsEditor(player, ctx);
        } catch (Exception e) {
            plugin.getLogger().warning("Mobs chat input error: " + e.getMessage());
            mobsChatContexts.remove(player.getUniqueId());
        }
    }

    private void reopenMobsEditor(Player player, MobsChatContext ctx) {
        final Session s = ctx.session;
        s.editingType = ctx.type;
        s.inventory = Bukkit.createInventory(null, 54,
                MessageUtil.parse("&6&lOlMeteor &8• &eConfig Ayarları"));
        sessions.put(player.getUniqueId(), s);
        drawMobsEditPage(s);
        player.openInventory(s.inventory);
    }

    // ────────────────────────────────────────────────────────────────
    //  SCHEMATIC & RECOVERY PAGE
    // ────────────────────────────────────────────────────────────────

    private void drawSchematicPage(Session s) {
        s.page = PAGE_SCHEMATIC;
        s.inventory.clear();
        fillBorder(s);
        s.inventory.setItem(BACK_SLOT, item(Material.ARROW, "&7← Ana Menü"));
        s.inventory.setItem(SAVE_SLOT, item(Material.EMERALD_BLOCK, "&a&lKaydet"));
        final String defaultSchem = config.getConfig().getString("schematic.default", "meteor_crater.schem");
        final boolean schemExists = config.schematicExists(defaultSchem);
        s.inventory.setItem(10, item(Material.FILLED_MAP,
                "&7Varsayılan Şematik: &f" + defaultSchem,
                schemExists ? "&aDosya mevcut" : "&cDosya bulunamadı!",
                "&7Her tür için özelleştirme: Meteor Türleri sayfası"));
        s.inventory.setItem(11, item(s.recoveryEnabled ? Material.LIME_DYE : Material.GRAY_DYE,
                "&eÇökme Kurtarma: " + bool(s.recoveryEnabled),
                "&7Sunucu çökmesi sonrası yarım kalan meteorları geri yükler",
                "&7Tıkla aç/kapat"));
        s.inventory.setItem(12, item(Material.REPEATER,
                "&eTemizlik Gecikmesi: &f" + s.cleanupDelay + " sn",
                "&7Tüm moblar öldükten sonra alanın silinme süresi",
                "&7Sol/sağ: ±10"));
        s.inventory.setItem(13, item(Material.CLOCK,
                "&eSahipsiz Zaman Aşımı: &f" + s.unattendedTimeout + " dk",
                "&7Hiç oyuncu gelmezse eventin iptal edilme süresi",
                "&7Sol/sağ: ±5"));
        s.inventory.setItem(14, item(Material.CHORUS_FRUIT,
                "&eChunk Yükleme Yarıçapı: &f" + s.chunkRadius + " chunk",
                "&7Event alanındaki force-load chunk sayısı",
                "&7Sol/sağ: ±1"));
    }

    private void handleSchematicClick(Session s, int slot, InventoryClickEvent event) {
        if (slot == BACK_SLOT) { drawMainMenu(s); return; }
        if (slot == SAVE_SLOT) { saveAll(s); drawMainMenu(s); return; }
        final boolean right = event.isRightClick();
        if (slot == 11) s.recoveryEnabled = !s.recoveryEnabled;
        else if (slot == 12) s.cleanupDelay = clamp(s.cleanupDelay + (right ? -10 : 10), 10, 600);
        else if (slot == 13) s.unattendedTimeout = clamp(s.unattendedTimeout + (right ? -5 : 5), 1, 120);
        else if (slot == 14) s.chunkRadius = clamp(s.chunkRadius + (right ? -1 : 1), 1, 10);
        drawSchematicPage(s);
    }

    private void changeTicketMaterial(Session s, ItemStack cursor, int delta) {
        if (cursor != null && !cursor.isEmpty() && cursor.getType().isItem()) {
            s.ticketMaterial = cursor.getType().name();
            return;
        }
        int idx = 0;
        for (int i = 0; i < TICKET_MATERIALS.length; i++) {
            if (TICKET_MATERIALS[i].equalsIgnoreCase(s.ticketMaterial)) { idx = i; break; }
        }
        s.ticketMaterial = TICKET_MATERIALS[Math.floorMod(idx + delta, TICKET_MATERIALS.length)];
    }

    // ────────────────────────────────────────────────────────────────
    //  HELPERS
    // ────────────────────────────────────────────────────────────────

    private void cyclePreset(Session s, int delta) {
        final List<String> names = config.getLocationPresetNames();
        if (names.isEmpty()) return;
        int idx = names.indexOf(s.locationPreset);
        if (idx < 0) idx = 0;
        s.locationPreset = names.get(Math.floorMod(idx + delta, names.size()));
    }

    private void cycleShape(Session s, int delta) {
        final RadiusShape[] shapes = RadiusShape.values();
        s.editShape = shapes[Math.floorMod(s.editShape.ordinal() + delta, shapes.length)];
    }

    private void cycleFallMode(Session s, int delta) {
        final MeteorFallMode[] modes = MeteorFallMode.values();
        s.editFallMode = modes[Math.floorMod(s.editFallMode.ordinal() + delta, modes.length)];
    }

    private void cycleBossMob(Session s, int delta) {
        final List<String> mobIds = plugin.getMythicMobsHook().getMobIds();
        if (mobIds.isEmpty()) { s.editBossMobId = ""; return; }
        if (s.editBossMobId == null || s.editBossMobId.isEmpty()) {
            s.editBossMobId = delta > 0 ? mobIds.getFirst() : "";
            return;
        }
        int idx = mobIds.indexOf(s.editBossMobId);
        if (idx < 0) { s.editBossMobId = mobIds.getFirst(); return; }
        final int next = Math.floorMod(idx + delta, mobIds.size() + 1);
        s.editBossMobId = next == mobIds.size() ? "" : mobIds.get(next);
    }

    private static final Material[] LOOT_BLOCKS = {Material.CHEST, Material.BARREL,
            Material.ANCIENT_DEBRIS, Material.CRYING_OBSIDIAN, Material.RESPAWN_ANCHOR,
            Material.ENDER_CHEST, Material.SHULKER_BOX};

    private void changeLootBlock(Session s, ItemStack cursor, int delta) {
        if (cursor != null && !cursor.isEmpty() && cursor.getType().isBlock()
                && cursor.getType().isItem()) {
            s.lootBlock = cursor.getType();
            return;
        }
        int idx = 0;
        for (int i = 0; i < LOOT_BLOCKS.length; i++) if (LOOT_BLOCKS[i] == s.lootBlock) idx = i;
        s.lootBlock = LOOT_BLOCKS[Math.floorMod(idx + delta, LOOT_BLOCKS.length)];
    }

    private void fillBorder(Session s) {
        final ItemStack border = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot : BORDER_SLOTS) s.inventory.setItem(slot, border);
    }

    private void close(Session s, InventoryClickEvent event) {
        sessions.remove(event.getWhoClicked().getUniqueId());
        event.getWhoClicked().closeInventory();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String bool(boolean value) {
        return value ? "&a&lAÇIK" : "&c&lKAPALI";
    }

    // ────────────────────────────────────────────────────────────────
    //  SAVE
    // ────────────────────────────────────────────────────────────────

    private void saveAll(Session s) {
        final var fileConfig = config.getConfig();
        // Location
        config.setLocationPreset(s.locationPreset, null, null);
        fileConfig.set("location-finder.min-distance-from-spawn", s.minDistance);
        fileConfig.set("location-finder.max-distance-from-spawn", s.maxDistance);
        fileConfig.set("location-finder.terrain-variance-tolerance", s.terrainVariance);
        fileConfig.set("location-finder.buffer-zone-from-claims", s.bufferZone);
        fileConfig.set("location-finder.worldguard-check-claims", s.wgCheck);
        fileConfig.set("location-finder.towny-require-wilderness", s.townyCheck);
        // Hazards
        fileConfig.set("event.hazards.radiation-damage-per-second", s.radiation);
        fileConfig.set("event.hazards.wind-charge-knockback-multiplier", s.windKnockback);
        fileConfig.set("event.hazards.wind-charge-interval-ticks", s.windInterval);
        fileConfig.set("event.hazards.disable-elytra", s.disableElytra);
        fileConfig.set("event.hazards.disable-ender-pearl", s.disablePearl);
        // Combat
        fileConfig.set("event.combat-feedback.damage-actionbar", s.damageBar);
        fileConfig.set("event.combat-feedback.kill-actionbar", s.killBar);
        fileConfig.set("event.combat-feedback.broadcast-leaderboard", s.leaderboard);
        fileConfig.set("event.combat-feedback.leaderboard-size", s.leaderboardSize);
        fileConfig.set("event.vault.reward-top-count", s.rewardTopCount);
        // Fall & Vault
        fileConfig.set("event.fall.normal-height", s.fallNormal);
        fileConfig.set("event.fall.slow-height", s.fallSlow);
        fileConfig.set("event.fall.show-impact-core", s.impactCore);
        fileConfig.set("event.vault.boss-damage-threshold-percent", s.bossThreshold);
        fileConfig.set("event.vault.open-delay-ticks", s.vaultDelay);
        // Auto
        fileConfig.set("automatic-events.enabled", s.autoEnabled);
        fileConfig.set("automatic-events.min-interval-minutes", s.autoMin);
        fileConfig.set("automatic-events.max-interval-minutes", s.autoMax);
        fileConfig.set("automatic-events.tps-guard.enabled", s.tpsGuard);
        fileConfig.set("automatic-events.tps-guard.minimum-tps", s.minTps);
        fileConfig.set("automatic-events.location-cooldown.enabled", s.cooldownEnabled);
        fileConfig.set("automatic-events.location-cooldown.radius", s.cooldownRadius);
        fileConfig.set("automatic-events.location-cooldown.hours", s.cooldownHours);
        // Meteor Type edits
        if (s.editingType != null) {
            saveTypeEdits(s);
        }
        // Loot edits (yalnızca loot sayfası ziyaret edildiyse)
        if (s.editLootBlock != null && s.editAccessMode != null) {
            saveLootEdits(s);
        }
        // Ticket
        if (s.ticketMaterial != null) {
            fileConfig.set("event.tickets.material", s.ticketMaterial);
            fileConfig.set("event.tickets.cooldown-seconds", s.ticketCooldown);
        }
        // Rewards
        if (s.rewardsEdited && s.editingType != null) {
            final String base = "meteor-types." + s.editingType.name().toLowerCase(Locale.ROOT) + ".";
            fileConfig.set(base + "rewards-commands", new ArrayList<>(s.editGeneralCommands));
            fileConfig.set(base + "rewards-items", new ArrayList<>(s.editGeneralItems));
            fileConfig.set(base + "ranking-rewards." + s.editRank + ".commands",
                    new ArrayList<>(s.editRankCmds));
            fileConfig.set(base + "ranking-rewards." + s.editRank + ".items",
                    new ArrayList<>(s.editRankItems));
        }
        // Mobs
        if (s.mobsEdited && s.editingType != null) {
            final String base = "meteor-types." + s.editingType.name().toLowerCase(Locale.ROOT) + ".";
            fileConfig.set(base + "mythicmobs", new ArrayList<>(s.editMobs));
            // Also update mythicmob-chances (set default 50% for new, remove for deleted)
            final Map<String, Double> existingChances = config.getMythicMobChances(s.editingType);
            final Map<String, Double> newChances = new LinkedHashMap<>();
            for (final String mobId : s.editMobs) {
                newChances.put(mobId, existingChances.getOrDefault(mobId, 50.0));
            }
            config.setMythicMobChances(s.editingType, newChances);
        }
        // Schematic & Recovery
        fileConfig.set("event.recovery.enabled", s.recoveryEnabled);
        fileConfig.set("event.completion.cleanup-delay-seconds", s.cleanupDelay);
        fileConfig.set("event.completion.unattended-timeout-minutes", s.unattendedTimeout);
        fileConfig.set("event.chunk-force-load-radius", s.chunkRadius);
        // Save to disk
        try {
            fileConfig.save(new java.io.File(plugin.getDataFolder(), "config.yml"));
            config.loadConfiguration();
            MessageUtil.sendMessage(Bukkit.getConsoleSender(), "&aConfig GUI değişiklikleri kaydedildi.");
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("Config GUI kaydedilemedi: " + e.getMessage());
        }
    }

    private void saveTypeEdits(Session s) {
        if (s.editingType == null) return;
        final MeteorType type = s.editingType;
        final String base = "meteor-types." + type.name().toLowerCase(Locale.ROOT) + ".";
        final var fileConfig = config.getConfig();
        fileConfig.set(base + "impact-radius", s.editRadius);
        fileConfig.set(base + "radius-shape", s.editShape.name());
        fileConfig.set(base + "boss-health-multiplier", s.editBossMult);
        fileConfig.set(base + "boss-mythicmob", s.editBossMobId);
        fileConfig.set(base + "pre-impact-duration-seconds", s.editPreImpact);
        fileConfig.set(base + "event-duration-seconds", s.editDuration);
        fileConfig.set(base + "rollback-duration-seconds", s.editRollback);
        fileConfig.set(base + "fall-mode", s.editFallMode.name().toLowerCase(Locale.ROOT));
        fileConfig.set(base + "normal-fall-duration-seconds", s.editNormalFall);
        fileConfig.set(base + "slow-fall-duration-seconds", s.editSlowFall);
        fileConfig.set(base + "waves.count", s.editWaveCount);
        fileConfig.set(base + "waves.interval-seconds", s.editWaveInterval);
    }

    private void saveLootEdits(Session s) {
        if (s.editingType == null) return;
        final MeteorType type = s.editingType;
        final String base = "meteor-types." + type.name().toLowerCase(Locale.ROOT) + ".loot.";
        final var fileConfig = config.getConfig();
        fileConfig.set(base + "block", s.editLootBlock != null ? s.editLootBlock.name() : "CHEST");
        fileConfig.set(base + "access-mode", s.editAccessMode);
        fileConfig.set(base + "personal", s.editPersonalLoot);
    }

    // ────────────────────────────────────────────────────────────────
    //  REWARDS CHAT EDIT
    // ────────────────────────────────────────────────────────────────

    private record RewardsChatContext(
            @NotNull Session session,
            @NotNull MeteorType type,
            int rank,
            @NotNull RewardField field
    ) {
        enum RewardField { GENERAL_COMMANDS, GENERAL_ITEMS, RANK_COMMANDS, RANK_ITEMS }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRewardsChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final RewardsChatContext ctx = rewardsChatContexts.get(player.getUniqueId());
        if (ctx == null) return;
        event.setCancelled(true);
        final String input = event.getMessage().trim();
        plugin.getFoliaScheduler().callGlobal(() ->
                processRewardsChatInput(player, ctx, input));
    }

    private void processRewardsChatInput(Player player, RewardsChatContext ctx, String input) {
        try {
            rewardsChatContexts.remove(player.getUniqueId());
            if (input.equalsIgnoreCase("done") || input.equalsIgnoreCase("cancel")
                    || input.equalsIgnoreCase("exit") || input.equals("0")) {
                reopenRewardsEditor(player, ctx);
                return;
            }
            final List<String> targetList = getRewardTargetList(ctx.session, ctx.field);
            final boolean isRemove = input.startsWith("-");
            final String value = (isRemove || input.startsWith("+"))
                    ? input.substring(1).trim() : input.trim();
            if (value.isEmpty()) {
                MessageUtil.sendMessage(player, "&cGeçersiz giriş. Boş değer eklenemez.");
                reopenRewardsEditor(player, ctx);
                return;
            }
            if (isRemove) {
                final boolean removed = targetList.remove(value);
                if (removed) {
                    MessageUtil.sendMessage(player, "&aKaldırıldı: &f" + value);
                } else {
                    MessageUtil.sendMessage(player, "&eBulunamadı: &f" + value);
                }
            } else {
                targetList.add(value);
                MessageUtil.sendMessage(player, "&aEklendi: &f" + value);
            }
            ctx.session.rewardsEdited = true;
            reopenRewardsEditor(player, ctx);
        } catch (Exception e) {
            plugin.getLogger().warning("Rewards chat input error: " + e.getMessage());
            rewardsChatContexts.remove(player.getUniqueId());
        }
    }

    private @NotNull List<String> getRewardTargetList(Session s, RewardsChatContext.RewardField field) {
        return switch (field) {
            case GENERAL_COMMANDS -> s.editGeneralCommands;
            case GENERAL_ITEMS -> s.editGeneralItems;
            case RANK_COMMANDS -> s.editRankCmds;
            case RANK_ITEMS -> s.editRankItems;
        };
    }

    private void reopenRewardsEditor(Player player, RewardsChatContext ctx) {
        final Session s = ctx.session;
        s.editingType = ctx.type;
        s.editRank = ctx.rank;
        // Re-create inventory
        s.inventory = Bukkit.createInventory(null, 54,
                MessageUtil.parse("&6&lOlMeteor &8• &eConfig Ayarları"));
        sessions.put(player.getUniqueId(), s);
        drawRewardsEditPage(s);
        player.openInventory(s.inventory);
    }

    private void promptRewardsChatEdit(Player player, Session s, int slot,
                                        InventoryClickEvent event) {
        final MeteorType type = s.editingType;
        if (type == null) return;
        final int rank = s.editRank;
        final RewardsChatContext.RewardField field;
        final String label;
        if (slot == 10) {
            field = RewardsChatContext.RewardField.GENERAL_COMMANDS;
            label = "genel komut";
        } else if (slot == 11) {
            field = RewardsChatContext.RewardField.GENERAL_ITEMS;
            label = "genel eşya";
        } else if (slot == 15) {
            field = RewardsChatContext.RewardField.RANK_ITEMS;
            label = "sıralama eşyası";
        } else if (slot == 16) {
            field = RewardsChatContext.RewardField.RANK_COMMANDS;
            label = "sıralama komutu";
        } else return;
        final RewardsChatContext ctx = new RewardsChatContext(s, type, rank, field);
        rewardsChatContexts.put(player.getUniqueId(), ctx);
        player.closeInventory();
        MessageUtil.sendMessage(player, "");
        MessageUtil.sendMessage(player, "&6&l✦ " + label + " düzenle");
        MessageUtil.sendMessage(player, "&7Eklemek için: &fdeğer");
        MessageUtil.sendMessage(player, "&7Çıkarmak için: &f-değer");
        MessageUtil.sendMessage(player, "&7Bitince: &fdone");
        MessageUtil.sendMessage(player, "");
    }

    // ────────────────────────────────────────────────────────────────
    //  CLOSE / DRAG
    // ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        final Session s = sessions.get(event.getWhoClicked().getUniqueId());
        if (s != null && event.getInventory() == s.inventory
                && event.getRawSlots().stream().anyMatch(slot -> slot < 54)) {
            event.setCancelled(true);
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  ITEM BUILDER
    // ────────────────────────────────────────────────────────────────

    private ItemStack item(Material material, String name, String... lore) {
        final ItemStack item = new ItemStack(material);
        final var meta = item.getItemMeta();
        meta.displayName(MessageUtil.parse(name));
        meta.lore(Arrays.stream(lore).map(MessageUtil::parse).toList());
        item.setItemMeta(meta);
        return item;
    }

    // ────────────────────────────────────────────────────────────────
    //  SESSION
    // ────────────────────────────────────────────────────────────────

    private static final class Session {
        Inventory inventory;
        int page = PAGE_MAIN;
        // Location
        String locationPreset;
        int minDistance, maxDistance, terrainVariance, bufferZone;
        boolean wgCheck, townyCheck;
        // Hazards
        int radiation, windInterval;
        double windKnockback;
        boolean disableElytra, disablePearl;
        // Combat
        boolean damageBar, killBar, leaderboard;
        int leaderboardSize, rewardTopCount;
        // Fall & Vault
        int fallNormal, fallSlow, bossThreshold, vaultDelay;
        boolean impactCore;
        Material lootBlock;
        // Auto
        boolean autoEnabled, tpsGuard, cooldownEnabled;
        int autoMin, autoMax, cooldownRadius, cooldownHours;
        double minTps;
        // Type edit
        MeteorType editingType;
        int editRadius, editPreImpact, editDuration, editRollback, editNormalFall, editSlowFall;
        int editWaveCount, editWaveInterval;
        double editBossMult;
        String editBossMobId;
        RadiusShape editShape;
        MeteorFallMode editFallMode;
        // Loot edit
        Material editLootBlock;
        String editAccessMode;
        boolean editPersonalLoot;
        // Rewards edit
        int editRank;
        boolean rewardsEdited;
        List<String> editGeneralCommands = new ArrayList<>();
        List<String> editGeneralItems = new ArrayList<>();
        List<String> editRankCmds = new ArrayList<>();
        List<String> editRankItems = new ArrayList<>();
        // Mobs edit
        boolean mobsEdited;
        List<String> editMobs = new ArrayList<>();
        // Ticket
        String ticketMaterial;
        int ticketCooldown;
        // Schematic & Recovery
        boolean recoveryEnabled;
        int cleanupDelay, unattendedTimeout, chunkRadius;
    }
}
