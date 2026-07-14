# Integrations

OlMeteor loads optional plugins through a safe hook system. If an integration is not found, its API classes are not force-loaded when the main plugin starts, preventing NoClassDefFoundError errors.

---

## Supported Integrations

| Plugin | Feature | Required? |
|---|---|---|
| CommandAPI | /olmeteor command tree and suggestions | Required |
| FAWE / WorldEdit | Schematic save/paste, snapshot and rollback | Strongly recommended |
| WorldGuard | Region conflict check for auto location | No |
| Towny | Wilderness check for auto location | No |
| MythicMobs | Selectable mobs and bosses | No |
| PlaceholderAPI | %olmeteor_*% placeholders | No |
| ItemsAdder | Custom loot item data preservation | No |
| FancyHolograms | Hologram provider detection | No |
| NBTAPI | Extra NBT read/write integration | No |

---

## WorldGuard and Towny

```yaml
location-finder:
  towny-require-wilderness: true
  worldguard-check-claims: true
```

Auto Meteor Location Finder works as follows:
1. WorldGuard check (if active) - Rejects locations inside regions
2. Towny check (if active) - Rejects locations inside town claims
3. If suitable location found, meteor starts

The manual `/olmeteor spawnat` command uses the exact location given by the admin; it is not subject to auto location filtering. Use `/olmeteor debug` to verify the integration is active if claim protection is needed.

---

## MythicMobs

If MythicMobs is installed:
- `/olmeteor selectmob <id>` suggestions come from MythicMobs registrations
- Mob weights can be adjusted with the End Rod editor in setup
- Each meteor type can have its own `boss-mythicmob`

### Example Configuration:
```yaml
meteor-types:
  epic:
    boss-mythicmob: "EpicMeteorBoss"
    mythicmobs:
      - "SkeletalKnight"
      - "FireDemon"
    mythicmob-chances:
      SkeletalKnight: 50
      FireDemon: 30
```

---

## ItemsAdder and NBT

Dragging the actual item into the loot editor is the safest method. OlMeteor preserves PDC, NBT and Data Components when serializing item data.

If ItemsAdder command rewards are needed, add the provider's console command to ranking rewards:
```yaml
ranking-rewards:
  "1":
    items: []
    commands:
      - "iagive %player% namespace:custom_item 1"
```

---

## FancyHolograms

| Status | Behavior |
|---|---|
| Installed | OlMeteor auto-detects and uses FancyHolograms API |
| Not installed | Default hologram system (ArmorStand) is used |

---

## Default Meteor Schematic

If FAWE/WorldEdit is installed, OlMeteor automatically creates a **default meteor schematic** on first startup:
- Stored at `schematics/meteor_crater.schem`
- Made of Blackstone, Magma, Obsidian and Crying Obsidian
- 13x6x13 block crater shape
- Usable by any meteor type

---

## Crash Recovery System

OlMeteor can recover terrain changes from incomplete events after a server crash or unexpected shutdown:

1. At each meteor impact, a **snapshot** of the area is saved to a .schem file on disk
2. On server restart, `MeteorEventManager.loadActiveEvents()` detects pending snapshots
3. Pending snapshots are automatically queued for restoration
4. `/olmeteor debug` shows the pending recovery count

---

## Hook Check

```
/olmeteor debug
```

Example output:
```
=== OlMeteor Debug ===
Plugin: OlMeteor 1.3.0
Server: Paper 1.21.3

Integrations:
[ACTIVE] CommandAPI
[ACTIVE] WorldEdit (FAWE)
[ACTIVE] WorldGuard
[INACTIVE] Towny (not installed)
[ACTIVE] MythicMobs
[ACTIVE] PlaceholderAPI
[INACTIVE] ItemsAdder
[INACTIVE] FancyHolograms
[ACTIVE] NBTAPI

Recovery:
[ACTIVE] Snapshot recovery active
Pending snapshots: 0
```

---

## Troubleshooting

If an integration appears inactive despite being installed on the server:
1. Make sure you are using the correct version of the plugin
2. Verify the dependency plugin loads successfully before OlMeteor
3. Do a full restart instead of /reload
4. Check the full stack trace from the first error line

### Common Causes:

| Issue | Solution |
|---|---|
| Plugin not working after reload | Do a full server restart |
| Using old JAR | Download the latest version |
| Version incompatibility | Use the correct plugin version for your server version |
| Dependency loading order | Ensure plugins OlMeteor depends on are loaded first |
