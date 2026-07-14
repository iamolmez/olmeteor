package com.olmeteors.meteorevents.api.event;
import com.olmeteors.meteorevents.event.MeteorType; import org.bukkit.Location; import org.bukkit.event.*; import org.jetbrains.annotations.NotNull;
public final class MeteorImpactEvent extends Event { private static final HandlerList HANDLERS=new HandlerList(); private final String id; private final MeteorType type; private final Location location;
 public MeteorImpactEvent(String id,MeteorType type,Location location){this.id=id;this.type=type;this.location=location.clone();}
 public String getEventId(){return id;} public MeteorType getMeteorType(){return type;} public Location getLocation(){return location.clone();}
 public @NotNull HandlerList getHandlers(){return HANDLERS;} public static HandlerList getHandlerList(){return HANDLERS;} }
