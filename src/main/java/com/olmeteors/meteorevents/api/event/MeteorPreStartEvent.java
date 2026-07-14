package com.olmeteors.meteorevents.api.event;
import com.olmeteors.meteorevents.event.MeteorType;
import org.bukkit.Location; import org.bukkit.event.*; import org.jetbrains.annotations.NotNull;
public final class MeteorPreStartEvent extends Event implements Cancellable {
 private static final HandlerList HANDLERS=new HandlerList(); private final MeteorType type; private final Location location; private boolean cancelled;
 public MeteorPreStartEvent(MeteorType type,Location location){this.type=type;this.location=location.clone();}
 public MeteorType getMeteorType(){return type;} public Location getLocation(){return location.clone();}
 public boolean isCancelled(){return cancelled;} public void setCancelled(boolean value){cancelled=value;}
 public @NotNull HandlerList getHandlers(){return HANDLERS;} public static HandlerList getHandlerList(){return HANDLERS;}
}
