# Developer API

The OlMeteor API is obtained through Bukkit's ServicesManager. Other plugins should not bundle OlMeteor classes in their JAR; the dependency should be `compileOnly`.

---

## Gradle Dependency

If OlMeteor is not published to a Maven repository, add the JAR to your project's `libs/` folder:

### build.gradle.kts:
```kotlin
dependencies {
    compileOnly(files("libs/OlMeteor-1.3.0.jar"))
}
```

### plugin.yml:
```yaml
name: MyAddon
version: 1.0.0
main: com.example.myaddon.MyAddon
api-version: '1.21'
folia-supported: true

softdepend:
  - OlMeteor
```

---

## Getting the Service

```java
import com.olmeteors.meteorevents.api.OlMeteorAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicesManager;

public class MyAddon extends JavaPlugin {

    private OlMeteorAPI olMeteorAPI;

    @Override
    public void onEnable() {
        ServicesManager manager = Bukkit.getServicesManager();
        olMeteorAPI = manager.load(OlMeteorAPI.class);

        if (olMeteorAPI == null) {
            getLogger().warning("OlMeteor API is not available.");
            return;
        }

        getLogger().info("Successfully connected to OlMeteor API!");
    }
}
```

---

## Starting a Meteor

### At a Specific Location:
```java
Location location = new Location(Bukkit.getWorld("world"), 100, 80, -200);
api.startAt("epic", location, commandSender);
```

### At a Random Location:
```java
api.startRandom("medium", "world", commandSender);
```

| Parameter | Type | Description |
|---|---|---|
| type | String | Meteor type: small, medium, large, epic, legendary |
| location | Location | Exact location to start at |
| worldName | String | World name |
| commandSender | CommandSender | Who initiated the action (console or player) |

---

## Active Events and Stopping

### Listing Active Events:
```java
Map<String, Location> activeEvents = api.activeEvents();

for (Map.Entry<String, Location> entry : activeEvents.entrySet()) {
    String eventId = entry.getKey();
    Location loc = entry.getValue();
    getLogger().info("Active meteor: " + eventId);
}
```

### Stopping an Event:
```java
for (String eventId : activeEvents.keySet()) {
    api.stop(eventId, commandSender);
}
```

---

## Lifecycle Events

OlMeteor provides Bukkit events that you can listen to at different stages of a meteor event.

### Event Listener Example:
```java
import com.olmeteors.meteorevents.api.events.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MeteorListener implements Listener {

    @EventHandler
    public void onPreStart(MeteorPreStartEvent event) {
        // Called before meteor creation - can be cancelled
        String eventId = event.getEventId();
        Location location = event.getLocation();
        String type = event.getMeteorType();

        if (shouldBlock(location)) {
            event.setCancelled(true);
            event.setCancelReason("This area is protected!");
        }
    }

    @EventHandler
    public void onImpact(MeteorImpactEvent event) {
        // Called when meteor enters impact phase
        getLogger().info("Meteor impacted: " + event.getEventId());

        Location loc = event.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 10, 1);
    }

    @EventHandler
    public void onFinish(MeteorFinishEvent event) {
        // Called when cleanup/rollback is complete
        getLogger().info("Meteor area restored: " + event.isRestored());
    }
}
```

### Event Registration:
```java
@Override
public void onEnable() {
    Bukkit.getPluginManager().registerEvents(new MeteorListener(), this);
}
```

### Event Table

| Event | Timing | Cancellable? |
|---|---|---|
| MeteorPreStartEvent | Before meteor creation | Yes |
| MeteorImpactEvent | When meteor enters impact phase | No |
| MeteorFinishEvent | When cleanup/rollback completes | No |

---

## Folia Compatibility

Always make API calls from the correct thread according to Bukkit/Paper scheduler rules. On Folia servers, execute world or entity operations with your own region scheduler.

### Folia Example:
```java
if (FoliaScheduler.isFolia()) {
    location.getWorld().getRegionScheduler()
        .run(plugin, location, (task) -> {
            api.startAt("small", location, Bukkit.getConsoleSender());
        });
} else {
    Bukkit.getScheduler().runTask(plugin, () -> {
        api.startAt("small", location, Bukkit.getConsoleSender());
    });
}
```

---

## Full Integration Example

```java
package com.example.myaddon;

import com.olmeteors.meteorevents.api.OlMeteorAPI;
import com.olmeteors.meteorevents.api.events.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class MyAddon extends JavaPlugin implements Listener {

    private OlMeteorAPI api;

    @Override
    public void onEnable() {
        api = Bukkit.getServicesManager().load(OlMeteorAPI.class);
        if (api == null) {
            getLogger().warning("OlMeteor not found!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("MyAddon successfully loaded!");
    }

    public void startCustomMeteor(String type, Location loc) {
        if (api != null) {
            api.startAt(type, loc, Bukkit.getConsoleSender());
        }
    }

    @EventHandler
    public void onMeteorPreStart(MeteorPreStartEvent event) {
        Location loc = event.getLocation();
        if (isProtectedZone(loc)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMeteorImpact(MeteorImpactEvent event) {
        Location loc = event.getLocation();
        loc.getWorld().strikeLightningEffect(loc);
    }

    @EventHandler
    public void onMeteorFinish(MeteorFinishEvent event) {
        if (event.isRestored()) {
            Bukkit.broadcastMessage("Meteor area cleaned!");
        }
    }

    private boolean isProtectedZone(Location loc) {
        return false;
    }
}
```

---

## Important Notes

| Warning | Description |
|---|---|
| Thread safety | Make API calls from the main server thread |
| Folia compatibility | Use region scheduler for world/entity operations |
| Null check | Always check for api == null |
| Softdepend | Use softdepend if your plugin can work without OlMeteor |
