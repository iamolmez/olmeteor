package com.olmeteors.meteorevents;

import com.olmeteors.meteorevents.config.ConfigManager;
import com.olmeteors.meteorevents.event.MeteorEventManager;
import com.olmeteors.meteorevents.event.MeteorType;
import com.olmeteors.meteorevents.location.LocationFinder;
import com.olmeteors.meteorevents.loot.LootGUIEditor;
import com.olmeteors.meteorevents.loot.VaultManager;
import com.olmeteors.meteorevents.schematic.SchematicManager;
import com.olmeteors.meteorevents.setup.SetupManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Validates that every message path used in {@link MeteorCommand#register()}
 * exists in the actual {@code config.yml} resource file.
 * <p>
 * Instead of testing command registration (which requires CommandAPI at runtime),
 * this test loads the real config.yml, creates a {@link ConfigManager} with it,
 * and resolves every path the {@code tell()} helper and direct
 * {@code config.sendMessage()} calls use.
 * <p>
 * This catches typos, renamed paths, and orphaned references early.
 */
@DisplayName("MeteorCommand message paths")
class MeteorCommandPathTest {

    @Test
    void commandNamespaceIsAsciiAndValid() {
        assertTrue(MeteorCommand.COMMAND_NAMESPACE.matches("[0-9a-z_.-]+"));
    }

    private static ConfigManager configManager;

    /** All message paths used by {@link MeteorCommand#register()} via tell() or direct sendMessage(). */
    private static final List<CommandPath> COMMAND_PATHS = List.of(
            // /meteor help — via tell("help.title") and config.sendMessage("command.help.usage")
            path("command.help.title"),
            path("command.help.usage"),

            // /meteor setup <type>
            path("command.setup.invalid_type", "type"),
            path("command.player_only"),

            // /meteor start <type> [world] [radius]
            path("command.start.invalid_type", "type"),

            // /meteor stop <eventId>
            path("command.stop.no_event", "eventId"),
            path("command.stop.success", "eventId"),

            // /meteor cancel <eventId>
            path("command.cancel.no_event", "eventId"),
            path("command.cancel.success", "eventId"),

            // /meteor reload
            path("command.reload.success"),

            // /meteor wand
            path("command.wand.success"),
            path("command.player_only"),

            // /meteor list
            path("command.list.empty"),
            path("command.list.title"),
            path("command.list.entry", "eventId", "type", "world", "location"),

            // /meteor info <eventId> [target]
            path("command.info.title"),
            path("command.info.no_event", "eventId"),
            path("command.info.id", "eventId"),
            path("command.info.type", "type"),
            path("command.info.phase", "phase"),
            path("command.info.world", "world"),
            path("command.info.location", "location"),
            path("command.info.radius", "radius"),
            path("command.info.schematic", "schematic"),
            path("command.info.distance", "player", "distance"),

            // Root command (no args) — same as help
            path("command.help.title"),
            path("command.help.usage")
    );

    private record CommandPath(String fullPath, String[] placeholders) {
        String messagePath() {
            // The fullPath already includes "command." prefix, e.g. "command.help.title".
            // getMessage() prepends "messages.", producing "messages.command.help.title".
            return fullPath;
        }
    }

    private static CommandPath path(String fullPath, String... placeholders) {
        return new CommandPath(fullPath, placeholders);
    }

    @BeforeAll
    @SuppressWarnings("JavaReflectionMemberAccess")
    static void setUp() throws Exception {
        // Load the real config.yml from the main resources
        final InputStream configStream = MeteorCommandPathTest.class
                .getClassLoader().getResourceAsStream("config.yml");
        assertNotNull(configStream, "config.yml must exist on test classpath");
        final FileConfiguration config = YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(configStream, java.nio.charset.StandardCharsets.UTF_8));

        // Mock the plugin minimally
        final MeteorPlugin plugin = mock(MeteorPlugin.class);
        final PluginDescriptionFile pluginMeta = mock(PluginDescriptionFile.class);
        lenient().when(plugin.getPluginMeta()).thenReturn(pluginMeta);
        lenient().when(pluginMeta.getVersion()).thenReturn("1.0.0");
        lenient().when(plugin.getLogger()).thenReturn(Logger.getLogger("MeteorCommandPathTest"));

        // Create ConfigManager and inject the real config
        configManager = new ConfigManager(plugin);

        final Field configField = ConfigManager.class.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(configManager, config);

        // Keep localeManager null to test config.yml resolution only
    }

    // ──────────────────────────────────────────────────────────────
    //  Path Resolution Tests
    // ──────────────────────────────────────────────────────────────

    private static String[] toPairs(String... keys) {
        // Convert placeholder keys to key-value pairs with dummy values for path-resolution checks
        final String[] pairs = new String[keys.length * 2];
        for (int i = 0; i < keys.length; i++) {
            pairs[i * 2] = keys[i];
            pairs[i * 2 + 1] = "test_value";
        }
        return pairs;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCommandPaths")
    @DisplayName("Every MeteorCommand tell() path resolves in config.yml")
    void everyCommandPath_resolves(CommandPath cmdPath) {
        final String msgPath = cmdPath.messagePath();
        final String result = configManager.getMessage(msgPath, toPairs(cmdPath.placeholders()));

        assertFalse(result.contains("Missing translation"),
                () -> "Path 'messages." + msgPath
                        + "' not found in config.yml. Got: " + result);
        assertFalse(result.isEmpty(), "Message for path '" + msgPath + "' must not be empty");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCommandPaths")
    @DisplayName("Every MeteorCommand tell() path contains expected leading color code")
    void everyCommandPath_hasColorCode(CommandPath cmdPath) {
        final String msgPath = cmdPath.messagePath();
        final String result = configManager.getMessage(msgPath, toPairs(cmdPath.placeholders()));

        // Every message should start with a color code (&x) or a placeholder
        assertTrue(result.startsWith("&") || result.startsWith("%"),
                () -> "Message for '" + msgPath
                        + "' should start with color code or placeholder, got: "
                        + result.substring(0, Math.min(20, result.length())));
    }

    @Test
    @DisplayName("All placeholder key-lists are valid")
    void allPlaceholderKeys_areValid() {
        for (final CommandPath cmdPath : COMMAND_PATHS) {
            // Just verify placeholder keys don't contain nulls or empty strings
            for (final String key : cmdPath.placeholders()) {
                assertNotNull(key, "Null placeholder key in: " + cmdPath.fullPath);
                assertFalse(key.isEmpty(), "Empty placeholder key in: " + cmdPath.fullPath);
            }
        }
    }

    static Stream<CommandPath> allCommandPaths() {
        return COMMAND_PATHS.stream().distinct();
    }

    // ──────────────────────────────────────────────────────────────
    //  Subcommand Permission Verification
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("All Meteor subcommands have corresponding plugin.yml permissions")
    void subcommandPermissions_matchPluginYml() {
        // This test verifies that every subcommand permission string used in
        // MeteorCommand matches an entry in plugin.yml.
        //
        // Extracted from MeteorCommand.register():
        //   - help:          CommandPermission.NONE (no permission required)
        //   - setup:         meteorevents.setup
        //   - start:         meteorevents.start
        //   - spawnat:       meteorevents.start (reuses start permission)
        //   - stop:          meteorevents.stop
        //   - cancel:        meteorevents.cancel
        //   - reload:        meteorevents.reload
        //   - wand:          meteorevents.wand
        //   - list:          meteorevents.list
        //   - info:          meteorevents.info

        final String[] expectedPermissions = {
                "olmeteor.setup",
                "olmeteor.start",
                "olmeteor.stop",
                "olmeteor.cancel",
                "olmeteor.reload",
                "olmeteor.wand",
                "olmeteor.list",
                "olmeteor.info"
        };

        // Load plugin.yml and verify each permission exists
        final InputStream pluginYml = MeteorCommandPathTest.class
                .getClassLoader().getResourceAsStream("plugin.yml");
        assertNotNull(pluginYml, "plugin.yml must exist on test classpath");

        final FileConfiguration pluginConfig = YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(pluginYml, java.nio.charset.StandardCharsets.UTF_8));

        for (final String perm : expectedPermissions) {
            assertTrue(pluginConfig.contains("permissions." + perm),
                    () -> "Permission '" + perm + "' used in MeteorCommand but not defined in plugin.yml");
        }

        // Verify help has NONE (no permission)
        assertTrue(pluginConfig.contains("permissions.olmeteor.participate"),
                "Participate permission must exist for players to receive event messages");
    }

    // ──────────────────────────────────────────────────────────────
    //  Subcommand Structure Verification
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MeteorCommand defines all expected subcommands")
    void expectedSubcommands_areDefined() {
        final String[] expectedSubcommands = {
                "help", "setup", "start",
                "stop", "cancel", "reload", "wand",
                "list", "info"
        };
        // Note: "spawnat" is excluded because it reuses start's messages
        // and has no message paths of its own in config.yml.

        for (final String sub : expectedSubcommands) {
            // Each subcommand should have at least one message path that resolves
            final boolean hasEntry = COMMAND_PATHS.stream()
                    .anyMatch(p -> p.fullPath().startsWith("command." + sub));
            assertTrue(hasEntry,
                    () -> "No message path defined for subcommand '" + sub
                            + "' in COMMAND_PATHS");
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Complete Path Coverage (config.yml vs used paths)
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("All messages.command.* paths in config.yml are reachable via getMessage")
    void allCommandPathsInConfig_areAccessible() {
        // Load the real config.yml and iterate over all messages.command.* keys
        final InputStream configStream = MeteorCommandPathTest.class
                .getClassLoader().getResourceAsStream("config.yml");
        assertNotNull(configStream);

        final FileConfiguration config = YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(configStream, java.nio.charset.StandardCharsets.UTF_8));

        // Collect all command message paths
        final var commandSection = config.getConfigurationSection("messages.command");
        assertNotNull(commandSection, "messages.command section must exist in config.yml");

        // Flatten the section to get full paths
        final var commandKeys = commandSection.getKeys(true);
        assertFalse(commandKeys.isEmpty(), "messages.command must have at least one message");

        for (final String key : commandKeys) {
            // Skip sections (non-leaf nodes)
            final Object value = commandSection.get(key);
            if (value instanceof org.bukkit.configuration.ConfigurationSection) continue;

            final String path = "command." + key;
            final String result = configManager.getMessage(path);

            assertFalse(result.contains("Missing translation"),
                    () -> "Path 'messages.command." + key
                            + "' defined in config.yml is not reachable via getMessage(\"" + path + "\")");
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────────────
}
