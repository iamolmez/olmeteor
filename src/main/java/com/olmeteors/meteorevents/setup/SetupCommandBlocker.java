package com.olmeteors.meteorevents.setup;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.olmeteors.meteorevents.config.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Blocks dangerous or disruptive commands while a player is in setup mode.
 * Prevents accidental inventory loss, teleportation, or state corruption.
 */
public final class SetupCommandBlocker implements Listener {

    private final MeteorPlugin plugin;
    private final ConfigManager config;
    private final Set<UUID> blockedPlayers;

    private static final Set<String> BLOCKED_COMMANDS = new HashSet<>(Arrays.asList(
            "/clear", "/invsee", "/enderchest", "/ec",
            "/tp", "/teleport", "/tpa", "/tpo",
            "/spawn", "/home", "/warp", "/back",
            "/fly", "/gamemode", "/gm",
            "/kill", "/suicide",
            "/me", "/say", "/msg",
            "/pl", "/plugins", "/version",
            "/stop", "/restart", "/reload"
    ));

    private static final Set<String> ALLOWED_COMMANDS = new HashSet<>(Arrays.asList(
            "/olmeteor",
            "/msg", "/r", "/reply",
            "/tell", "/w", "/whisper"
    ));

    public SetupCommandBlocker(MeteorPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.blockedPlayers = new HashSet<>();
    }

    /**
     * Marks a player as being in setup mode with command blocking.
     *
     * @param player the player to block
     */
    public void addBlockedPlayer(@NotNull Player player) {
        blockedPlayers.add(player.getUniqueId());
    }

    /**
     * Removes command blocking for a player.
     *
     * @param player the player to unblock
     */
    public void removeBlockedPlayer(@NotNull Player player) {
        blockedPlayers.remove(player.getUniqueId());
    }

    /**
     * Checks if a player is currently being blocked.
     *
     * @param player the player
     * @return true if the player is blocked
     */
    public boolean isBlocked(@NotNull Player player) {
        return blockedPlayers.contains(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();

        if (!blockedPlayers.contains(player.getUniqueId())) {
            return;
        }

        final String message = event.getMessage().toLowerCase().trim();

        // Always allow OlMeteor commands
        for (final String allowed : ALLOWED_COMMANDS) {
            if (message.startsWith(allowed + " ") || message.equals(allowed)) {
                return;
            }
        }

        // Block matching commands
        for (final String blocked : BLOCKED_COMMANDS) {
            if (message.equals(blocked) || message.startsWith(blocked + " ")) {
                event.setCancelled(true);
                config.sendMessage(player, "setup.command_blocked");
                return;
            }
        }

        // Block any command that could modify inventory or teleport
        if (message.startsWith("/") && containsBlockedKeyword(message)) {
            event.setCancelled(true);
            config.sendMessage(player, "setup.command_not_allowed");
        }
    }

    /**
     * Checks if a command contains keywords related to blocked functionality.
     */
    private boolean containsBlockedKeyword(String command) {
        final String[] keywords = {
                "inventory", "inv", "ender", "clear",
                "teleport", "tp", "warp", "spawn",
                "gamemode", "gm", "fly", "speed",
                "kill", "heal", "feed", "god"
        };

        for (final String keyword : keywords) {
            if (command.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}
