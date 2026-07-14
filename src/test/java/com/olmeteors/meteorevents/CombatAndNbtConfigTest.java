package com.olmeteors.meteorevents;

import com.olmeteors.meteorevents.event.MeteorType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

final class CombatAndNbtConfigTest {

    @Test
    void everyMeteorTypeHasIndependentLootAndCombatFeedbackSettings() {
        final var stream = getClass().getClassLoader().getResourceAsStream("config.yml");
        assertNotNull(stream);
        final var config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));

        for (final MeteorType type : MeteorType.values()) {
            final String base = "meteor-types." + type.name().toLowerCase(Locale.ROOT) + ".";
            assertTrue(config.contains(base + "restore-structure-on-finish"), type.name());
            assertTrue(config.getString(base + "loot.block", "")
                    .matches("[A-Z0-9_]+"), type.name());
            assertTrue(config.getString(base + "loot.access-mode", "")
                    .matches("AUTO|INTERACT|BREAK|BOTH"), type.name());
            assertTrue(config.contains(base + "loot.personal"), type.name());
            assertTrue(config.contains(base + "loot.inventory-title"), type.name());
            assertTrue(config.contains(base + "combat-feedback.damage-actionbar"), type.name());
            assertTrue(config.contains(base + "combat-feedback.kill-actionbar"), type.name());
            assertTrue(config.contains(base + "combat-feedback.broadcast-leaderboard"), type.name());
            assertTrue(config.getString(base + "radius-shape", "")
                    .matches("CIRCLE|SQUARE|TRIANGLE|DIAMOND|HEXAGON"), type.name());
        }
    }

    @Test
    void nbtAndCombatDefaultsArePresent() {
        final var stream = getClass().getClassLoader().getResourceAsStream("config.yml");
        assertNotNull(stream);
        final var config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        assertTrue(config.contains("integrations.nbt-api.enabled"));
        assertTrue(config.contains("event.combat-feedback.damage-actionbar-text"));
        assertTrue(config.contains("event.combat-feedback.leaderboard-entry"));
        assertTrue(config.getString("automatic-events.search-shape", "")
                .matches("CIRCLE|SQUARE|TRIANGLE|DIAMOND|HEXAGON"));
    }
}
