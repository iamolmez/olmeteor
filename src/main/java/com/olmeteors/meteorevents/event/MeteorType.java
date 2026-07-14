package com.olmeteors.meteorevents.event;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents the different types of meteor events available in the plugin.
 * Each type has predefined properties for impact radius, damage, and difficulty.
 */
public enum MeteorType {

    SMALL("Small Meteor", "&a", 15, 10, 25, 1.0, Difficulty.EASY),
    MEDIUM("Medium Meteor", "&e", 25, 20, 50, 1.5, Difficulty.NORMAL),
    LARGE("Large Meteor", "&c", 40, 35, 75, 2.0, Difficulty.HARD),
    EPIC("Epic Meteor", "&5", 60, 50, 100, 3.0, Difficulty.EPIC),
    LEGENDARY("Legendary Meteor", "&6", 80, 75, 150, 4.0, Difficulty.LEGENDARY);

    private final String displayName;
    private final String colorCode;
    private final int impactRadius;
    private final int minRewardSlots;
    private final int maxRewardSlots;
    private final double bossHealthMultiplier;
    private final Difficulty difficulty;

    MeteorType(String displayName, String colorCode, int impactRadius,
               int minRewardSlots, int maxRewardSlots,
               double bossHealthMultiplier, Difficulty difficulty) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.impactRadius = impactRadius;
        this.minRewardSlots = minRewardSlots;
        this.maxRewardSlots = maxRewardSlots;
        this.bossHealthMultiplier = bossHealthMultiplier;
        this.difficulty = difficulty;
    }

    public @NotNull String displayName() {
        return displayName;
    }

    public @NotNull String colorCode() {
        return colorCode;
    }

    public int impactRadius() {
        return impactRadius;
    }

    public int minRewardSlots() {
        return minRewardSlots;
    }

    public int maxRewardSlots() {
        return maxRewardSlots;
    }

    public double bossHealthMultiplier() {
        return bossHealthMultiplier;
    }

    public @NotNull Difficulty difficulty() {
        return difficulty;
    }

    /**
     * Returns a random number of reward slots within this type's range.
     *
     * @return random slot count
     */
    public int randomRewardSlotCount() {
        return ThreadLocalRandom.current().nextInt(minRewardSlots, maxRewardSlots + 1);
    }

    /**
     * Gets a meteor type by name, case-insensitive.
     *
     * @param name the type name (e.g. "small", "EPIC", "Legendary")
     * @return the MeteorType
     * @throws IllegalArgumentException if the name does not match any MeteorType
     */
    public static @NotNull MeteorType fromString(@NotNull String name)
            throws IllegalArgumentException {
        return valueOf(name.toUpperCase());
    }

    /**
     * Difficulty levels for meteor events.
     */
    public enum Difficulty {
        EASY("Easy", "&a"),
        NORMAL("Normal", "&e"),
        HARD("Hard", "&c"),
        EPIC("Epic", "&5"),
        LEGENDARY("Legendary", "&6");

        private final String displayName;
        private final String colorCode;

        Difficulty(String displayName, String colorCode) {
            this.displayName = displayName;
            this.colorCode = colorCode;
        }

        public @NotNull String displayName() {
            return displayName;
        }

        public @NotNull String colorCode() {
            return colorCode;
        }
    }
}
