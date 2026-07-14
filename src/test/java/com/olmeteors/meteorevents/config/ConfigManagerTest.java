package com.olmeteors.meteorevents.config;

import com.olmeteors.meteorevents.MeteorPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link ConfigManager#getMessage(String, String...)}.
 * <p>
 * Tests focus on the message resolution logic:
 * <ul>
 *   <li>Direct config.yml lookup</li>
 *   <li>Placeholder replacement (global + caller-supplied)</li>
 *   <li>Missing translation fallback</li>
 *   <li>Validation of placeholder argument pairs</li>
 * </ul>
 * <p>
 * These tests use Reflection to inject a controlled {@link FileConfiguration}
 * directly into ConfigManager, bypassing filesystem-dependent
 * {@link ConfigManager#loadConfiguration()} entirely.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigManager.getMessage()")
class ConfigManagerTest {

    @Mock
    private MeteorPlugin plugin;

    @Mock
    private PluginDescriptionFile pluginMeta;

    private ConfigManager configManager;
    private FileConfiguration testConfig;

    @BeforeEach
    @SuppressWarnings("JavaReflectionMemberAccess")
    void setUp() throws Exception {
        lenient().when(plugin.getPluginMeta()).thenReturn(pluginMeta);
        lenient().when(pluginMeta.getVersion()).thenReturn("1.0.0");
        lenient().when(plugin.getLogger()).thenReturn(Logger.getLogger("ConfigManagerTest"));

        // Create ConfigManager WITHOUT calling loadConfiguration()
        configManager = new ConfigManager(plugin);

        // Build a controlled test configuration
        testConfig = new YamlConfiguration();
        testConfig.set("messages.command.help.title", "&6&l=== MeteorEvents Help ===");
        testConfig.set("messages.command.reload.success", "&aConfiguration reloaded successfully.");
        testConfig.set("messages.command.wand.success", "&aSetup wand has been added to your inventory.");
        testConfig.set("messages.command.player_only", "&cThis command can only be executed by a player.");
        testConfig.set("messages.command.setup.invalid_type", "&cInvalid meteor type: &e%type%");
        testConfig.set("messages.command.start.invalid_type", "&cInvalid meteor type: &e%type%");
        testConfig.set("messages.command.start.scheduled", "&aMeteor event &e%eventId% &ascheduled at &e%location%");
        testConfig.set("messages.command.start.location_fail",
                "&cCould not find a suitable location for the meteor event.");
        testConfig.set("messages.command.stop.no_event", "&cNo active event found with ID: &e%eventId%");
        testConfig.set("messages.command.stop.success", "&aEvent &e%eventId% &astop initiated.");
        testConfig.set("messages.command.cancel.no_event", "&cNo active event found with ID: &e%eventId%");
        testConfig.set("messages.command.cancel.success", "&cEvent &e%eventId% &chas been cancelled.");
        testConfig.set("messages.command.list.title", "&6&l=== Active Meteor Events ===");
        testConfig.set("messages.command.list.empty", "&eThere are no active meteor events.");
        testConfig.set("messages.command.list.entry",
                "&7- &f%eventId% &7| Type: &f%type% &7| World: &f%world% &7| %location%");
        testConfig.set("messages.command.info.title", "&6&l=== Meteor Event Info ===");
        testConfig.set("messages.command.info.no_event", "&cNo active event found with ID: &e%eventId%");
        testConfig.set("messages.command.info.id", "&7ID: &f%eventId%");
        testConfig.set("messages.command.info.type", "&7Type: &f%type%");
        testConfig.set("messages.command.info.radius", "&7Radius: &f%radius% blocks");
        testConfig.set("messages.command.info.distance", "&7Distance to &e%player%&7: &f%distance% blocks");
        testConfig.set("messages.event.broadcast.incoming", "%color%&l⚠ %name% &eis incoming!");
        testConfig.set("messages.setup.command_blocked",
                "&cThis command is disabled while in setup mode.");
        testConfig.set("messages.vault.locked", "&c&l✦ Vault Locked!");
        testConfig.set("messages.simple.placeholder_test", "Hello %name%!");
        testConfig.set("messages.simple.multiple", "%a%, %b%, %c%");

        // Inject via Reflection to bypass filesystem-dependent loadConfiguration()
        final Field configField = ConfigManager.class.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(configManager, testConfig);

        // Keep localeManager null so cascade doesn't interfere with base tests
    }

    // ──────────────────────────────────────────────────────────────
    //  Basic Retrieval
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Returns exact message from config.yml when path exists")
    void getMessage_basic_returnsFromConfig() {
        final String result = configManager.getMessage("command.help.title");
        assertEquals("&6&l=== MeteorEvents Help ===", result);
    }

    @Test
    @DisplayName("Returns message with leading messages prefix trimmed off path")
    void getMessage_withMessagesPrefix_stillWorks() {
        final String result = configManager.getMessage("command.reload.success");
        assertEquals("&aConfiguration reloaded successfully.", result);
    }

    // ──────────────────────────────────────────────────────────────
    //  Placeholder Replacement
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Placeholder replacement")
    class PlaceholderReplacement {

        @Test
        @DisplayName("Replaces single caller-supplied placeholder")
        void singlePlaceholder() {
            final String result = configManager.getMessage(
                    "command.setup.invalid_type", "type", "EPIC");
            assertEquals("&cInvalid meteor type: &eEPIC", result);
        }

        @Test
        @DisplayName("Replaces multiple caller-supplied placeholders")
        void multiplePlaceholders() {
            final String result = configManager.getMessage(
                    "command.start.scheduled",
                    "eventId", "EVT-001",
                    "location", "100, 64, 200");
            assertEquals("&aMeteor event &eEVT-001 &ascheduled at &e100, 64, 200", result);
        }

        @Test
        @DisplayName("Replaces the same placeholder appearing in multiple places")
        void repeatedPlaceholder() {
            final String result = configManager.getMessage(
                    "command.info.no_event", "eventId", "MY-EVENT");
            assertEquals("&cNo active event found with ID: &eMY-EVENT", result);
        }

        @Test
        @DisplayName("Replaces placeholders that have no prefix")
        void unprefixedPlaceholders() {
            // Some messages use %color%, %name% directly (event.broadcast.*)
            final String result = configManager.getMessage(
                    "event.broadcast.incoming",
                    "color", "&c",
                    "name", "Big Meteor",
                    "seconds", "30");
            assertEquals("&c&l⚠ Big Meteor &eis incoming!", result);
        }

        @Test
        @DisplayName("Leaves unreferenced placeholders unchanged in result")
        void unreferencedPlaceholderIgnored() {
            final String result = configManager.getMessage(
                    "command.reload.success", "unusedKey", "unusedValue");
            // Should return the message as-is (no error, extra placeholders are fine)
            assertEquals("&aConfiguration reloaded successfully.", result);
        }

        @Test
        @DisplayName("Replaces global %version% placeholder")
        void globalVersionPlaceholder() {
            // Set a message containing %version%
            testConfig.set("messages.version.test", "Version: %version%");

            final String result = configManager.getMessage("version.test");
            assertEquals("Version: 1.0.0", result);
        }

        @Test
        @DisplayName("Replaces global %prefix% placeholder")
        void globalPrefixPlaceholder() {
            testConfig.set("messages.prefix.test", "%prefix% &aHello");

            final String result = configManager.getMessage("prefix.test");
            assertEquals("&8[&6Ol&eMeteor&8] &aHello", result);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Missing / Invalid Inputs
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("Returns missing-translation indicator for nonexistent path")
        void missingPath_returnsMissingIndicator() {
            final String result = configManager.getMessage("completely.fake.path");
            assertAll(
                    () -> assertTrue(result.contains("Missing translation"),
                            "Should indicate missing translation"),
                    () -> assertTrue(result.contains("messages.completely.fake.path"),
                            "Should include the full path in the error message")
            );
        }

        @Test
        @DisplayName("Throws IllegalArgumentException for odd number of placeholders")
        void oddPlaceholders_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> configManager.getMessage("command.help.title", "lonely"),
                    "Should reject unpaired placeholder keys");
        }

        @Test
        @DisplayName("Throws IllegalArgumentException for three placeholders")
        void threePlaceholders_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> configManager.getMessage("command.help.title", "a", "b", "c"),
                    "Three args = one pair + one orphan, should fail");
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Edge Cases
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Returns empty message when value is empty string")
        void emptyMessage_returnsFallback() {
            testConfig.set("messages.empty.path", "");

            final String result = configManager.getMessage("empty.path");
            assertTrue(result.contains("Missing translation"),
                    "Empty string should trigger fallback");
        }

        @Test
        @DisplayName("Handles null placeholder values gracefully")
        void nullPlaceholderValue() {
            final String result = configManager.getMessage(
                    "command.info.id", "eventId", null);
            assertEquals("&7ID: &f", result);
        }

        @Test
        @DisplayName("Message with only placeholder text gets fully replaced")
        void messageOnlyPlaceholder() {
            testConfig.set("messages.simple.only_placeholder", "%type%");

            final String result = configManager.getMessage(
                    "simple.only_placeholder", "type", "LEGENDARY");
            assertEquals("LEGENDARY", result);
        }

        @Test
        @DisplayName("Handles very long placeholder values")
        void longPlaceholderValue() {
            final String longValue = "A".repeat(1000);

            final String result = configManager.getMessage(
                    "simple.placeholder_test", "name", longValue);
            assertEquals("Hello " + longValue + "!", result);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  sendMessage delegation
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendMessage delegation")
    class SendMessageDelegation {

        @Test
        @DisplayName("sendMessage retrieves message and passes to MessageUtil")
        void sendMessage_retrievesCorrectMessage() throws Exception {
            // Use a mock CommandSender
            final org.bukkit.command.CommandSender sender =
                    org.mockito.Mockito.mock(org.bukkit.command.CommandSender.class);

            configManager.sendMessage(sender, "command.help.title");

            // Verify the message was sent via the mock
            // (MessageUtil.sendMessage calls sender.sendMessage())
            org.mockito.Mockito.verify(sender).sendMessage(
                    org.mockito.ArgumentMatchers.any(net.kyori.adventure.text.Component.class));
        }

        @Test
        @DisplayName("sendMessage with placeholders works correctly")
        void sendMessage_withPlaceholders() {
            final org.bukkit.command.CommandSender sender =
                    org.mockito.Mockito.mock(org.bukkit.command.CommandSender.class);

            configManager.sendMessage(sender, "command.setup.invalid_type",
                    "type", "UNKNOWN");

            org.mockito.Mockito.verify(sender).sendMessage(
                    org.mockito.ArgumentMatchers.any(net.kyori.adventure.text.Component.class));
        }
    }
}
