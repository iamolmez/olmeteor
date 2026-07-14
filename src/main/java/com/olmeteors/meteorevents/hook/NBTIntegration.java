package com.olmeteors.meteorevents.hook;

import com.olmeteors.meteorevents.MeteorPlugin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.logging.Level;

/**
 * Full-fidelity item codec for vanilla/custom NBT and modern Data Components.
 * NBT-API is detected through its own class loader, but is never a hard dependency.
 */
public final class NBTIntegration {
    private final MeteorPlugin plugin;
    private final boolean nbtApiAvailable;

    public NBTIntegration(@NotNull MeteorPlugin plugin) {
        this.plugin = plugin;
        this.nbtApiAvailable = detectNbtApi();
        plugin.getLogger().info("NBT item integration: native byte codec"
                + (nbtApiAvailable ? " + NBT-API detected" : " (NBT-API optional)"));
    }

    public boolean isNbtApiAvailable() {
        return nbtApiAvailable;
    }

    public @NotNull String encode(@NotNull ItemStack item) {
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    public @Nullable ItemStack decode(@Nullable String encoded) {
        if (encoded == null || encoded.isBlank()) return null;
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
        } catch (RuntimeException error) {
            plugin.getLogger().log(Level.WARNING,
                    "Could not decode NBT/Data Components loot item; using YAML fallback", error);
            return null;
        }
    }

    private boolean detectNbtApi() {
        if (!plugin.getConfigManager().getConfig().getBoolean("integrations.nbt-api.enabled", true)) {
            return false;
        }
        final Plugin nbtPlugin = plugin.getServer().getPluginManager().getPlugin("NBTAPI");
        if (nbtPlugin == null || !nbtPlugin.isEnabled()) return false;
        try {
            nbtPlugin.getClass().getClassLoader().loadClass("de.tr7zw.changeme.nbtapi.NBT");
            return true;
        } catch (ClassNotFoundException | LinkageError error) {
            plugin.getLogger().warning("NBTAPI plugin exists but its API class could not be loaded safely.");
            return false;
        }
    }
}
