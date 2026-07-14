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
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
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
    private static final int PAGE_WAVES = 3;
    private static final int PAGE_COMBAT = 4;
    private static final int PAGE_FALL_VAULT = 5;
    private static final int PAGE_AUTO = 6;
    private static final int PAGE_TYPES_MENU = 7;
    private static final int PAGE_TYPE_EDIT = 8;

    private static final int[] BORDER_SLOTS = {0,1,2,3,4,5,6,7,8,45,46,47,48,49,50,51,52,53};
    private static final int BACK_SLOT = 45;
    private static final int SAVE_SLOT = 49;
    private static final int CLOSE_SLOT = 53;

    private final MeteorPlugin plugin;
    private final ConfigManager config;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

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
        s.waveCount = config.getWaveCount(MeteorType.SMALL);
        s.waveInterval = config.getWaveIntervalSeconds(MeteorType.SMALL);
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
        s.inventory.setItem(12, item(Material.WATER_BUCKET, "&3Dalga Ayarları",
                "&7Dalga sayısı ve aralık süresi"));
        s.inventory.setItem(13, item(Material.DIAMOND_SWORD, "&6Savaş Ayarları",
                "&7Hasar sıralaması, actionbar mesajları"));
        s.inventory.setItem(14, item(Material.ANVIL, "&eDüşüş & Sandık",
                "&7Düşüş yüksekliği, sandık gecikmesi, eşik"));
        s.inventory.setItem(15, item(Material.CLOCK, "&5Otomatik Meteor",
                "&7Aralık, TPS koruması, konum soğuması"));
        s.inventory.setItem(16, item(Material.FIRE_CHARGE, "&dMeteor Türleri",
                "&7Her tür için yarıçap, boss, süre"));
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
    //  WAVES PAGE
    // ────────────────────────────────────────────────────────────────

    private void drawWavesPage(Session s) {
        s.page = PAGE_WAVES;
        s.inventory.clear();
        fillBorder(s);
        s.inventory.setItem(BACK_SLOT, item(Material.ARROW, "&7← Ana Menü"));
        s.inventory.setItem(SAVE_SLOT, item(Material.EMERALD_BLOCK, "&a&lKaydet"));
        s.inventory.setItem(10, item(Material.ZOMBIE_HEAD, "&aDalga Sayısı: &f" + s.waveCount,
                "&7Sol/sağ: ±1 &7Shift: ±5"));
        s.inventory.setItem(11, item(Material.CLOCK, "&aDalga Aralığı: &f" + s.waveInterval + " sn",
                "&7Sol/sağ: ±5"));
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
        final var tc = config.getTypeConfig(type);
        s.inventory.setItem(10, item(Material.STONE, "&7Yarıçap: &f" + s.editRadius,
                "&7Çarpma alanı yarıçapı", "&7Sol/sağ: ±5 &7Shift: ±10"));
        s.inventory.setItem(11, item(Material.COMPASS, "&7Yarıçap Şekli: &f" + s.editShape.name(),
                "&7Sol/sağ tıkla değiştir"));
        s.inventory.setItem(12, item(Material.IRON_SWORD, "&7Boss Çarpanı: &f" + s.editBossMult + "x",
                "&7Sol/sağ: ±0.1 &7Shift: ±0.5"));
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
            case PAGE_WAVES -> handleWavesClick(s, slot, event);
            case PAGE_COMBAT -> handleCombatClick(s, slot, event);
            case PAGE_FALL_VAULT -> handleFallVaultClick(s, slot, event);
            case PAGE_AUTO -> handleAutoClick(s, slot, event);
            case PAGE_TYPES_MENU -> handleTypesMenuClick(s, slot, event);
            case PAGE_TYPE_EDIT -> handleTypeEditClick(s, slot, event);
        }
    }

    private void handleMainClick(Session s, int slot, InventoryClickEvent event) {
        if (slot == SAVE_SLOT) { saveAll(s); close(s, event); }
        else if (slot == CLOSE_SLOT) close(s, event);
        else if (slot == 10) drawLocationPage(s);
        else if (slot == 11) drawHazardsPage(s);
        else if (slot == 12) drawWavesPage(s);
        else if (slot == 13) drawCombatPage(s);
        else if (slot == 14) drawFallVaultPage(s);
        else if (slot == 15) drawAutoPage(s);
        else if (slot == 16) drawTypesMenu(s);
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

    private void handleWavesClick(Session s, int slot, InventoryClickEvent event) {
        if (slot == BACK_SLOT) { drawMainMenu(s); return; }
        if (slot == SAVE_SLOT) { saveAll(s); drawMainMenu(s); return; }
        final boolean right = event.isRightClick();
        final boolean shift = event.isShiftClick();
        if (slot == 10) s.waveCount = clamp(s.waveCount + (shift ? 5 : 1) * (right ? -1 : 1), 1, 50);
        else if (slot == 11) s.waveInterval = clamp(s.waveInterval + (right ? -5 : 5), 1, 300);
        drawWavesPage(s);
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
            s.editPreImpact = tc.preImpactDurationSeconds();
            s.editDuration = tc.eventDurationSeconds();
            s.editRollback = tc.rollbackDurationSeconds();
            s.editFallMode = config.getFallMode(s.editingType);
            s.editNormalFall = config.getFallDurationSeconds(s.editingType, MeteorFallMode.NORMAL);
            s.editSlowFall = config.getFallDurationSeconds(s.editingType, MeteorFallMode.SLOW);
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
        else if (slot == 14) s.editPreImpact = clamp(s.editPreImpact + (right ? -10 : 10), 5, 600);
        else if (slot == 15) s.editDuration = clamp(s.editDuration + (right ? -30 : 30), 30, 3600);
        else if (slot == 16) s.editRollback = clamp(s.editRollback + (right ? -10 : 10), 5, 600);
        else if (slot == 20) cycleFallMode(s, right ? -1 : 1);
        else if (slot == 21) s.editNormalFall = clamp(s.editNormalFall + (right ? -2 : 2), 2, 120);
        else if (slot == 22) s.editSlowFall = clamp(s.editSlowFall + (right ? -2 : 2), 5, 120);
        drawTypeEditPage(s);
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
        // Waves
        fileConfig.set("event.waves.count", s.waveCount);
        fileConfig.set("event.waves.interval-seconds", s.waveInterval);
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
        fileConfig.set(base + "pre-impact-duration-seconds", s.editPreImpact);
        fileConfig.set(base + "event-duration-seconds", s.editDuration);
        fileConfig.set(base + "rollback-duration-seconds", s.editRollback);
        fileConfig.set(base + "fall-mode", s.editFallMode.name().toLowerCase(Locale.ROOT));
        fileConfig.set(base + "normal-fall-duration-seconds", s.editNormalFall);
        fileConfig.set(base + "slow-fall-duration-seconds", s.editSlowFall);
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
        // Waves
        int waveCount, waveInterval;
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
        double editBossMult;
        RadiusShape editShape;
        MeteorFallMode editFallMode;
    }
}
