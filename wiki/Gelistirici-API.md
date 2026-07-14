# Gelistirici API'si

OlMeteor API, Bukkit ServicesManager uzerinden alinir. Baska bir plugin OlMeteor siniflarini kendi JAR'ina gommemeli; bagimlilik compileOnly olmalidir.

---

## Gradle Bagimliligi

OlMeteor henuz bir Maven deposunda yayinlanmiyorsa JAR'i projenizin libs/ klasorune ekleyin:

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

## Servisi Alma

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
            getLogger().warning("OlMeteor API kullanilamiyor.");
            return;
        }

        getLogger().info("OlMeteor API basariyla baglandi!");
    }
}
```

---

## Meteor Baslatma

### Belirli Konumda Baslatma:
```java
Location location = new Location(Bukkit.getWorld("world"), 100, 80, -200);
api.startAt("epic", location, commandSender);
```

### Rastgele Konumda Baslatma:
```java
api.startRandom("medium", "world", commandSender);
```

| Parametre | Tip | Aciklama |
|---|---|---|
| type | String | Meteor tipi: small, medium, large, epic, legendary |
| location | Location | Baslatilacak kesin konum |
| worldName | String | Dunya adi |
| commandSender | CommandSender | Islemi baslatan (konsol veya oyuncu) |

---

## Aktif Eventler ve Durdurma

### Aktif Eventleri Listeleme:
```java
Map<String, Location> activeEvents = api.activeEvents();

for (Map.Entry<String, Location> entry : activeEvents.entrySet()) {
    String eventId = entry.getKey();
    Location loc = entry.getValue();
    getLogger().info("Aktif meteor: " + eventId);
}
```

### Event Durdurma:
```java
for (String eventId : activeEvents.keySet()) {
    api.stop(eventId, commandSender);
}
```

---

## Lifecycle Eventleri

OlMeteor, meteor etkinliginin farkli asamalarinda dinleyebileceginiz Bukkit eventleri saglar.

### Event Listener Ornegi:
```java
import com.olmeteors.meteorevents.api.events.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MeteorListener implements Listener {

    @EventHandler
    public void onPreStart(MeteorPreStartEvent event) {
        // Meteor olusturulmadan once - iptal edilebilir
        String eventId = event.getEventId();
        Location location = event.getLocation();
        String type = event.getMeteorType();

        if (shouldBlock(location)) {
            event.setCancelled(true);
            event.setCancelReason("Bu bolge koruma altinda!");
        }
    }

    @EventHandler
    public void onImpact(MeteorImpactEvent event) {
        // Meteor carpma asamasina gectiginde
        getLogger().info("Meteor carpti: " + event.getEventId());

        Location loc = event.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 10, 1);
    }

    @EventHandler
    public void onFinish(MeteorFinishEvent event) {
        // Temizlik/rollback tamamlandiginda
        getLogger().info("Meteor alani geri yuklendi: " + event.isRestored());
    }
}
```

### Event Kaydi:
```java
@Override
public void onEnable() {
    Bukkit.getPluginManager().registerEvents(new MeteorListener(), this);
}
```

### Event Tablosu

| Event | Zamani | Iptal Edilebilir mi? |
|---|---|---|
| MeteorPreStartEvent | Meteor olusturulmadan once | Evet |
| MeteorImpactEvent | Meteor carpma asamasina gectiginde | Hayir |
| MeteorFinishEvent | Temizlik/rollback tamamlandiginda | Hayir |

---

## Folia Uyumlulugu

API cagrilarini Bukkit/Paper zamanlayici kurallarina uygun thread'den yapin. Folia sunucularinda dunya veya entity islemlerini kendi region scheduler'inizla yurutun.

### Folia Ornegi:
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

## Tam Entegrasyon Ornegi

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
            getLogger().warning("OlMeteor bulunamadi!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("MyAddon basariyla yuklendi!");
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
            Bukkit.broadcastMessage("Meteor alani temizlendi!");
        }
    }

    private boolean isProtectedZone(Location loc) {
        return false;
    }
}
```

---

## Onemli Uyarilar

| Uyari | Aciklama |
|---|---|
| Thread guvenligi | API cagrilarini ana sunucu thread'inden yapin |
| Folia uyumu | Dunya/entity islemlerini region scheduler ile yapin |
| Null kontrolu | api == null kontrolunu her zaman yapin |
| Softdepend | OlMeteor olmadan da plugininiz calisabiliyorsa softdepend kullanin |
