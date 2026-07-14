# Setup

## Requirements

| Component | Status | Description |
|---|---:|---|
| Paper or Folia 1.21.1+ | Required | Spigot/CraftBukkit are not supported. |
| Java 21 | Required | The Java version the server runs on. |
| CommandAPI | Required | Required for command registration. |
| FAWE or WorldEdit | Recommended | For schematic save/paste and terrain snapshot operations. |
| WorldGuard | Optional | Prevents auto meteors from falling in protected regions. |
| Towny | Optional | Prevents auto meteors from falling in town claims. |
| MythicMobs | Optional | Custom mob and boss spawning. |
| PlaceholderAPI | Optional | Scoreboard and hologram placeholders. |
| ItemsAdder | Optional | Preserves custom item data in loot GUI. |
| FancyHolograms | Optional | Detected as integration if installed. |
| NBTAPI | Optional | Extra NBT integration; basic NBT/PDC preservation works without it. |

---

## Installation Steps

### 1. Preparation
```
1. Fully shut down the server.
2. Prepare the required JAR files.
3. Check the plugins/ folder.
```

### 2. Place Files
```
plugins/
  OlMeteor-1.4.0.jar       -- Main plugin
  CommandAPI-*.jar           -- Required dependency
  FastAsyncWorldEdit-*.jar   -- Recommended (schematic operations)
  WorldGuard-*.jar           -- Optional
  Towny-*.jar               -- Optional
  MythicMobs-*.jar          -- Optional
  ...other optional plugins
```

### 3. First Start
```
1. Start the server.
2. Wait for the plugins/OlMeteor/ folder to be created.
3. Run /olmeteor debug in console to check integration status.
4. Set locale: en in plugins/OlMeteor/config.yml for English.
```

### 4. Verification
```
/olmeteor debug       -- Hook and system status check
/olmeteor help        -- Help menu
/olmeteor setup small -- First meteor setup
```

---

## Updating

```
1. Shut down the server.
2. Replace the old OlMeteor JAR with the new one.
3. Keep backups of config.yml, lang/, schematics/ and data files.
4. Restart the server.
```

Warning: Do not use `/reload`, PlugMan or similar live reload tools. CommandAPI, WorldGuard, FAWE and Folia schedulers are not safe during live reload.

---

## Common Mistakes

| Issue | Solution |
|---|---|
| Plugin won't enable | Check Java 21, CommandAPI presence, Paper/Folia 1.21.1+ |
| NoClassDefFoundError | Old JAR, broken WorldGuard, or after /reload |
| Schematic not working | Check FAWE/WorldEdit hook, corner selection in setup |
| Auto meteor not starting | Activate with /olmeteor auto on, check TPS |

---

## First Check

```
/olmeteor debug
/olmeteor help
/olmeteor setup small
```

If CommandAPI is not found, OlMeteor safely disables itself. If an optional integration is missing, only that integration's features are disabled.
