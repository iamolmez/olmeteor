# Configuration

The main file is located at `plugins/OlMeteor/config.yml`. After changes, use `/olmeteor reload`; for JAR or dependency updates, a full restart is required.

---

## Main Sections

| Section | Purpose |
|---|---|
| locale | Language selection: auto, tr, en |
| integrations | NBT and other optional integration toggles |
| command-help | Help menu categories, order and texts |
| automatic-events | Auto scheduling, TPS guard, worlds and location cooldown |
| schematic | Default schematic file |
| location-finder | Surface/Y-level/claim rules and presets |
| event | Waves, tickets, recovery, tracking, cleanup, hazards and vault settings |
| meteor-types | Independent settings for each meteor type: visuals, duration, loot, mobs and rewards |
| messages | Legacy config messages; normally lang/messages_en.yml is used |

---

## Language

```yaml
locale: en
locale-config-overrides: false
```

- `locale: auto` --> Auto-detect from JVM system locale
- `locale: en` --> English
- `locale: tr` --> Turkish

When `locale-config-overrides: false`, the `lang/messages_en.yml` file is used. You can add your own language by creating `lang/messages_XX.yml`.

---

## Wave System

```yaml
event:
  waves:
    count: 3
    interval-seconds: 15
```

Normal mobs arrive in configured waves. The boss spawns after the last wave.

---

## Meteor Ticket

```yaml
event:
  tickets:
    material: "FIRE_CHARGE"
    cooldown-seconds: 300
```

Tickets are PDC-signed; renamed fake items cannot start meteors. This system prevents ticket duplication and cheating.

---

## Cleanup and Recovery

```yaml
event:
  recovery:
    enabled: true
  completion:
    cleanup-delay-seconds: 60
    unattended-timeout-minutes: 30
```

| Setting | Description |
|---|---|
| cleanup-delay-seconds | Delay after mobs die and completion conditions are met |
| unattended-timeout-minutes | Maximum time an event stays open if nobody participates |
| recovery.enabled | Restores incomplete snapshots after server crash/restart |

When `restore-structure-on-finish: true` is set for a meteor type, the schematic area is restored including blocks broken or added by players.

---

## Fall Animation

```yaml
event:
  fall:
    normal-height: 80
    slow-height: 120
    show-impact-core: false

meteor-types:
  small:
    fall-mode: "normal"
    normal-fall-duration-seconds: 8
    slow-fall-duration-seconds: 18
```

---

## Type-Based Settings

Each meteor type (small, medium, large, epic, legendary) has the following configurable properties:

### Reward Slot Counts (Code-level)

| Type | Min - Max Slots | Difficulty |
|---|---|---|
| Small | 10 - 25 | Easy |
| Medium | 20 - 50 | Normal |
| Large | 35 - 75 | Hard |
| Epic | 50 - 100 | Epic |
| Legendary | 75 - 150 | Legendary |

These values determine how many different items can appear in each type's loot chest. A random number is selected.

### Base Settings
```yaml
meteor-types:
  small:
    restore-structure-on-finish: true
    fall-mode: "normal"
    impact-radius: 15
    pre-impact-duration-seconds: 30
    event-duration-seconds: 300
    rollback-duration-seconds: 30
    boss-health-multiplier: 1.0
```

### Loot Settings
```yaml
    loot:
      block: "CHEST"
      access-mode: "AUTO"
      personal: true
      inventory-title: "Small Meteor Reward"
```

### MythicMobs Settings
```yaml
    boss-mythicmob: ""
    mythicmobs: []
    mythicmob-chances: {}
```

### Ranking Rewards
```yaml
    ranking-rewards:
      "1":
        items: ["NETHERITE_INGOT:2"]
        commands: ["eco give %player% 5000"]
      "2":
        items: ["DIAMOND:8"]
        commands: []
```

### Point Offsets
```yaml
    mob-spawn-offsets: []
    hologram-offsets: []
    chest-offsets: []
```

---

## Event Lifecycle (EventPhase)

Each meteor progresses through phases:

| Phase | Description | Duration |
|---|---|---|
| SCHEDULED | Meteor scheduled, not yet started | Short |
| PRE_IMPACT | Pre-impact warning, visual effects, screen shake | 30 seconds (configurable) |
| IMPACT | Meteor impacted, schematic pasting, waves starting | ~2 seconds |
| ACTIVE | Active event - hazards on, boss spawned, vault accessible | 300 seconds (configurable) |
| ROLLBACK | Area being cleaned and restored | 30 seconds (configurable) |
| COMPLETED | Event completed successfully | - |
| CANCELLED | Event cancelled or failed | - |

Each phase determines its own behaviors:
- **Hazards**: Only active during the ACTIVE phase
- **Vault**: Only accessible during the ACTIVE phase
- **Boss**: Only spawns during the ACTIVE phase

---

## BossBar Tracking System

During active meteor events, players see an automatic BossBar:

```yaml
event:
  tracking:
    enabled: true
    max-distance: 2000
```

The BossBar displays:
- Meteor type and color
- Distance to the player (in meters)
- Direction (East/West/North/South)
- Remaining mob count
- Remaining time (progress bar)

---

## Command Help Categories (command-categories.yml)

Help categories are read from a separate `command-categories.yml` file. Each category can:
- Be hidden with `enabled: false`
- Have a custom title
- List specific commands to display

---

## Important Notes

- Do not use tabs in YAML indentation; use 2 spaces per level
- Backup your config file before making changes
- If the file becomes too large, you can remove comment lines
