package com.olmeteors.meteorevents.api;
import com.olmeteors.meteorevents.MeteorPlugin;
import com.olmeteors.meteorevents.event.MeteorType;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import java.util.Map;
import java.util.stream.Collectors;
public final class OlMeteorAPIImpl implements OlMeteorAPI {
    private final MeteorPlugin plugin; public OlMeteorAPIImpl(MeteorPlugin plugin){this.plugin=plugin;}
    public void startAt(String type,Location location,CommandSender sender){plugin.getMeteorEventManager().startEventAt(MeteorType.fromString(type),location,sender);}
    public void startRandom(String type,String world,CommandSender sender){plugin.getMeteorEventManager().startEvent(MeteorType.fromString(type),world,sender);}
    public boolean stop(String id,CommandSender sender){if(plugin.getMeteorEventManager().getEvent(id)==null)return false;plugin.getMeteorEventManager().stopEvent(id,sender);return true;}
    public @NotNull Map<String,Location> activeEvents(){return plugin.getMeteorEventManager().getActiveEvents().entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,e->e.getValue().center().clone()));}
}
