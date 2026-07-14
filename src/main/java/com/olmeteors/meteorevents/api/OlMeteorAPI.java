package com.olmeteors.meteorevents.api;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import java.util.Map;
/** Public service API obtainable from Bukkit's ServicesManager. */
public interface OlMeteorAPI {
    void startAt(@NotNull String meteorType,@NotNull Location location,@NotNull CommandSender sender);
    void startRandom(@NotNull String meteorType,@NotNull String world,@NotNull CommandSender sender);
    boolean stop(@NotNull String eventId,@NotNull CommandSender sender);
    @NotNull Map<String,Location> activeEvents();
}
