# Auto Meteors

The automatic system finds a new location at each trigger based on interval, world, distance, surface and claim rules. Multiple meteor types can be added to a single rule.

---

## Overview

Auto Meteor Cycle:
1. Timer triggers (min-max interval)
2. Suitable world is selected
3. Location finder activates
4. Surface/Y-level/claim checks are performed
5. If suitable location found, meteor starts
6. Location is added to cooldown
7. Next time is scheduled

---

## In-Game Menu

```
/olmeteor auto
```

This command opens the auto settings GUI for players. When run from console, it shows the current status.

---

## One-Command Setup

```
/olmeteor auto setup <meteors> <world> <preset> <lootBlock> <minMinutes> <maxMinutes> <minDistance> <maxDistance> [minY] [maxY]
```

### Parameters

| Parameter | Description | Example |
|---|---|---|
| meteors | Single type, comma-separated multiple types, or random | small,medium,large |
| world | The loaded world to search in | world |
| preset | Surface and height rule | flat_surface |
| lootBlock | Loot block (can be comma-separated to match types) | CHEST,BARREL,ANCIENT_DEBRIS |
| minMinutes | Minimum interval between meteors (minutes) | 30 |
| maxMinutes | Maximum interval between meteors (minutes) | 60 |
| minDistance | Minimum distance from world spawn (blocks) | 100 |
| maxDistance | Maximum distance from world spawn (blocks) | 5000 |
| minY | Optional minimum Y level | 120 |
| maxY | Optional maximum Y level | 220 |

---

### Example Usage

#### Example 1: Basic Setup
```
/olmeteor auto setup small,medium,large world grass_surface CHEST,BARREL,ANCIENT_DEBRIS 30 60 500 5000
```

- Meteor types: Small, Medium, Large (randomly selected)
- World: world
- Preset: Grass surface
- Loot blocks: Match in order: small->CHEST, medium->BARREL, large->ANCIENT_DEBRIS
- Interval: 30-60 minutes random
- Distance: 500-5000 blocks from spawn

#### Example 2: Random Types
```
/olmeteor auto setup random world any_surface CHEST 20 45 300 4000
```

#### Example 3: Air Meteor
```
/olmeteor auto setup epic world air END_PORTAL_FRAME 45 90 1000 8000 120 220
```

---

## Location Presets

| Preset | Description |
|---|---|
| flat_surface | Flat and dry surface |
| water_surface | Water surface |
| underground | Underground |
| air | In the air (Y-level required) |
| any_surface | Any surface (no flatness requirement) |
| grass_surface | Grass/dirt-like blocks |
| desert_surface | Desert surface |
| nether_surface | Nether surface |
| end_surface | End surface |

New presets can be created under `location-finder.presets`. Add Bukkit material names like `GRASS_BLOCK`, `SAND` to the `allowed-floor-blocks` list.

---

## Security Checks

| Setting | Description |
|---|---|
| towny-require-wilderness: true | Rejects Towny town claims |
| worldguard-check-claims: true | Rejects WorldGuard regions |
| tps-guard.enabled: true | Delays events on low TPS |
| location-cooldown.enabled: true | Avoids recently used areas |
| max-active-events: 1 | Maximum concurrent auto meteors |

If WorldGuard or Towny is not installed, the related check is safely skipped. Use `/olmeteor debug` to verify the integration is active if claim protection is needed.

---

## Management Commands

```
/olmeteor auto on          -- Enable auto system
/olmeteor auto off         -- Disable auto system
/olmeteor auto status      -- Show schedule and next time
/olmeteor auto now         -- Start meteor at a random location immediately
```

### Status Output Example:
```
=== Auto Meteor Status ===
Active: Yes
Meteors: small, medium, large
World: world
Preset: flat_surface
Interval: 30 - 60 min
Next: in 24 minutes
```

---

## Common Issues

| Issue | Solution |
|---|---|
| Auto meteor not starting | Make sure it's enabled with /olmeteor auto on |
| Location not found | Check min/max distance values, test preset suitability |
| Same type always appears | Check that multiple types are in the list |
| Falling in town/WG area | Verify towny-require-wilderness and worldguard-check-claims are enabled |
