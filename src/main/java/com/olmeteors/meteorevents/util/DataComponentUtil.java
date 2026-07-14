package com.olmeteors.meteorevents.util;

import com.olmeteors.meteorevents.config.ConfigManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for working with Minecraft 1.21+ Data Component API.
 * <p>
 * All item display names and lore are loaded from the locale system
 * ({@code config.yml → messages.item.*}) via {@link ConfigManager}.
 */
public final class DataComponentUtil {

    private DataComponentUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Creates an ItemStack with a custom model data component.
     */
    public static @NotNull ItemStack createCustomItem(@NotNull Material material, int amount, int customModelData) {
        final ItemStack item = new ItemStack(material, amount);
        final ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(customModelData);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates an ItemStack with custom display name, lore, rarity, and model data.
     */
    public static @NotNull ItemStack createStyledItem(
            @NotNull Material material,
            int amount,
            @NotNull String displayName,
            @NotNull List<String> loreLines,
            @NotNull ItemRarity rarity,
            int customModelData) {

        final ItemStack item = new ItemStack(material, amount);
        final ItemMeta meta = item.getItemMeta();

        meta.displayName(MessageUtil.parse(displayName));

        if (!loreLines.isEmpty()) {
            final List<Component> loreComponents = new ArrayList<>();
            for (final String line : loreLines) {
                loreComponents.add(MessageUtil.parse(line));
            }
            meta.lore(loreComponents);
        }

        meta.setRarity(rarity);
        meta.setCustomModelData(customModelData);

        if (rarity == ItemRarity.EPIC || rarity == ItemRarity.RARE) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    // ─────────────────────────────────────────────────────────────
    //  Locale-aware item factories
    // ─────────────────────────────────────────────────────────────

    /** Config path prefix for items. */
    private static final String ITEM = "item.";

    /**
     * Helper: resolves an item's display name and lore from the locale system,
     * then builds the styled item.
     */
    private static @NotNull ItemStack buildFromConfig(
            @NotNull ConfigManager config,
            @NotNull Material material,
            @NotNull String configKey,
            @NotNull ItemRarity rarity,
            int customModelData,
            @NotNull String @NotNull ... placeholders) {

        final String name = config.getMessage(ITEM + configKey + ".name");
        final List<String> lore = config.getMessageLines(ITEM + configKey + ".lore", placeholders);
        return createStyledItem(material, 1, name, lore, rarity, customModelData);
    }

    /**
     * Creates a Meteor Key from locale config.
     * Placeholders: {@code %meteor_type%}
     */
    public static @NotNull ItemStack createMeteorKey(@NotNull ConfigManager config,
                                                      @NotNull String meteorTypeName) {
        return buildFromConfig(config, Material.TRIAL_KEY, "meteor_key", ItemRarity.RARE, 1001,
                "meteor_type", meteorTypeName);
    }

    /** Creates the Setup Wand from locale config. */
    public static @NotNull ItemStack createSetupWand(@NotNull ConfigManager config) {
        return buildFromConfig(config, Material.BLAZE_ROD, "setup_wand", ItemRarity.UNCOMMON, 1002);
    }

    /** Creates the Mob Spawn Tool from locale config. */
    public static @NotNull ItemStack createMobSpawnTool(@NotNull ConfigManager config) {
        return buildFromConfig(config, Material.END_ROD, "mob_spawn_tool", ItemRarity.UNCOMMON, 1003);
    }

    /** Creates the Loot Editor item from locale config. */
    public static @NotNull ItemStack createLootEditor(@NotNull ConfigManager config) {
        return buildFromConfig(config, Material.CHEST, "loot_editor_item", ItemRarity.UNCOMMON, 1004);
    }

    /** Creates the Safe Exit item from locale config. */
    public static @NotNull ItemStack createSafeExit(@NotNull ConfigManager config) {
        return buildFromConfig(config, Material.BEDROCK, "safe_exit", ItemRarity.EPIC, 1005);
    }

    /** Leaves setup without writing any setup changes. */
    public static @NotNull ItemStack createCancelExit(@NotNull ConfigManager config) {
        final ItemStack item = new ItemStack(Material.BARRIER);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(MessageUtil.parse("&c&lKaydetmeden Çık"));
        meta.lore(List.of(MessageUtil.parse("&7Değişiklikleri iptal eder ve envanterini geri verir.")));
        meta.setRarity(ItemRarity.RARE);
        meta.setCustomModelData(1006);
        item.setItemMeta(meta);
        return item;
    }

    public static @NotNull ItemStack createHologramTool(@NotNull ConfigManager config) {
        final ItemStack item = new ItemStack(Material.NAME_TAG);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(MessageUtil.parse("&b&lYazı / Hologram Konumu"));
        meta.lore(List.of(MessageUtil.parse("&7Bir bloğa sağ tıklayarak yazı konumu ekle.")));
        meta.setRarity(ItemRarity.UNCOMMON);
        meta.setCustomModelData(1007);
        item.setItemMeta(meta);
        return item;
    }

    /** Creates the Meteor Core display item from locale config. */
    public static @NotNull ItemStack createMeteorCoreItem(@NotNull ConfigManager config) {
        return buildFromConfig(config, Material.FIRE_CHARGE, "meteor_core", ItemRarity.EPIC, 2001);
    }

    /**
     * Creates a Wind Charge item from locale config.
     */
    public static @NotNull ItemStack createWindChargeItem(@NotNull ConfigManager config, int amount) {
        final ItemStack item = new ItemStack(Material.WIND_CHARGE, amount);
        final ItemMeta meta = item.getItemMeta();

        meta.displayName(MessageUtil.parse(
                config.getMessage("item.wind_charge.name")));
        meta.lore(config.getMessageLines("item.wind_charge.lore").stream()
                .map(MessageUtil::parse)
                .toList());
        meta.setRarity(ItemRarity.RARE);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a Mace from locale config.
     * Placeholders: {@code %density_level%}, {@code %breach_level%}, {@code %wind_burst_level%}
     */
    public static @NotNull ItemStack createMace(@NotNull ConfigManager config,
                                                  int densityLevel, int breachLevel, int windBurstLevel) {
        final ItemStack mace = new ItemStack(Material.MACE, 1);
        final ItemMeta meta = mace.getItemMeta();

        meta.displayName(MessageUtil.parse(
                config.getMessage("item.mace.name")));
        meta.lore(config.getMessageLines("item.mace.lore",
                "density_level", String.valueOf(densityLevel),
                "breach_level", String.valueOf(breachLevel),
                "wind_burst_level", String.valueOf(windBurstLevel)).stream()
                .map(MessageUtil::parse)
                .toList());
        meta.setRarity(ItemRarity.EPIC);
        meta.setCustomModelData(2002);

        if (densityLevel > 0) meta.addEnchant(Enchantment.DENSITY, densityLevel, true);
        if (breachLevel > 0) meta.addEnchant(Enchantment.BREACH, breachLevel, true);
        if (windBurstLevel > 0) meta.addEnchant(Enchantment.WIND_BURST, windBurstLevel, true);

        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        mace.setItemMeta(meta);
        return mace;
    }
}
