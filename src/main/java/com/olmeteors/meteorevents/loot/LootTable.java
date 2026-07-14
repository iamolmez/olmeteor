package com.olmeteors.meteorevents.loot;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a configurable loot table for meteor event rewards.
 * Each entry has configurable drop chance, min/max amounts, and locked status.
 */
public final class LootTable {

    private final String name;
    private final List<LootEntry> entries;

    public LootTable(@NotNull String name) {
        this.name = name;
        this.entries = new ArrayList<>();
    }

    public LootTable(@NotNull String name, @NotNull List<LootEntry> entries) {
        this.name = name;
        this.entries = new ArrayList<>(entries);
    }

    public @NotNull String name() {
        return name;
    }

    public @NotNull List<LootEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Adds a loot entry to the table.
     *
     * @param entry the loot entry
     */
    public void addEntry(@NotNull LootEntry entry) {
        this.entries.add(entry);
    }

    /**
     * Removes a loot entry at the given index.
     *
     * @param index the index
     */
    public void removeEntry(int index) {
        if (index >= 0 && index < entries.size()) {
            this.entries.remove(index);
        }
    }

    /**
     * Generates loot based on configured drop chances.
     * Each entry is rolled independently.
     *
     * @return a list of ItemStacks to give to the player
     */
    public @NotNull List<ItemStack> generateLoot() {
        final List<ItemStack> loot = new ArrayList<>();

        for (final LootEntry entry : entries) {
            try {
                // Roll for this entry
                final double roll = ThreadLocalRandom.current().nextDouble(0, 100);

                if (roll <= entry.chance()) {
                    // Determine random amount within range
                    final int amount;
                    if (entry.minAmount() >= entry.maxAmount()) {
                        amount = entry.minAmount();
                    } else {
                        amount = ThreadLocalRandom.current()
                                .nextInt(entry.minAmount(), entry.maxAmount() + 1);
                    }

                    // Create the item stack
                    final ItemStack item = entry.item().clone();
                    item.setAmount(amount);
                    loot.add(item);
                }
            } catch (Exception e) {
                // Skip invalid entries silently
            }
        }

        return loot;
    }

    /**
     * Generates loot for a specific number of reward slots.
     *
     * @param slotCount the number of slots to fill
     * @return a list of ItemStacks
     */
    public @NotNull List<ItemStack> generateLoot(int slotCount) {
        final List<ItemStack> loot = generateLoot();

        // If we have fewer items than slots, add some default items
        while (loot.size() < slotCount) {
            loot.add(new ItemStack(Material.AIR));
        }

        // If we have more items than slots, truncate
        if (loot.size() > slotCount) {
            return loot.subList(0, slotCount);
        }

        return loot;
    }

    /**
     * Represents a single entry in a loot table.
     *
     * @param item       the item stack
     * @param chance     the drop chance (0.0 - 100.0)
     * @param minAmount  minimum amount
     * @param maxAmount  maximum amount
     * @param locked     whether this entry is locked (requires special key/event)
     */
    public record LootEntry(
            @NotNull ItemStack item,
            double chance,
            int minAmount,
            int maxAmount,
            boolean locked
    ) {
        public LootEntry {
            // Validate
            if (chance < 0 || chance > 100) {
                throw new IllegalArgumentException("Chance must be between 0 and 100");
            }
            if (minAmount < 1) minAmount = 1;
            if (maxAmount < minAmount) maxAmount = minAmount;
        }

        /**
         * Creates a simple loot entry with default settings.
         *
         * @param item   the item
         * @param chance the chance (0-100)
         * @return the loot entry
         */
        public static @NotNull LootEntry simple(@NotNull ItemStack item, double chance) {
            return new LootEntry(item, chance, 1, 1, false);
        }

        /**
         * Creates a loot entry with variable amount.
         *
         * @param item      the item
         * @param chance    the chance (0-100)
         * @param minAmount minimum amount
         * @param maxAmount maximum amount
         * @return the loot entry
         */
        public static @NotNull LootEntry ranged(@NotNull ItemStack item, double chance,
                                                  int minAmount, int maxAmount) {
            return new LootEntry(item, chance, minAmount, maxAmount, false);
        }

        /**
         * Creates a locked loot entry.
         *
         * @param item   the item
         * @param chance the chance (0-100)
         * @return the loot entry
         */
        public static @NotNull LootEntry locked(@NotNull ItemStack item, double chance) {
            return new LootEntry(item, chance, 1, 1, true);
        }
    }
}
