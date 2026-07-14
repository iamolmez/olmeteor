package com.olmeteors.meteorevents.api.event;
import com.olmeteors.meteorevents.event.MeteorType; import org.bukkit.Location; import org.bukkit.event.*; import org.jetbrains.annotations.NotNull;
public final class MeteorFinishEvent extends Event { private static final HandlerList HANDLERS=new HandlerList(); private final String id; private final MeteorType type; private final Location location; private final boolean restored;
 public MeteorFinishEvent(String id,MeteorType type,Location location,boolean restored){this.id=id;this.type=type;this.location=location.clone();this.restored=restored;}
 public String getEventId(){return id;} public MeteorType getMeteorType(){return type;} public Location getLocation(){return location.clone();} public boolean isRestored(){return restored;}
 public @NotNull HandlerList getHandlers(){return HANDLERS;} public static HandlerList getHandlerList(){return HANDLERS;} }
