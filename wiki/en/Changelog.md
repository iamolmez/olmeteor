# Changelog

This page contains changes from current and past versions of OlMeteor.

---

## Version 1.3.0 (Current)

**Release:** 2024 | **Java:** 21 | **Server:** Paper/Folia 1.21.1+

### New Features

#### Meteor Event System
- 5 meteor classes: Small, Medium, Large, Epic, Legendary
- Each class has its own impact radius, reward slots, boss health multiplier and difficulty
- 7-phase event lifecycle: SCHEDULED, PRE_IMPACT, IMPACT, ACTIVE, ROLLBACK, COMPLETED, CANCELLED
- Each phase has distinct behaviors (hazard state, vault access, boss state)

#### Fall Animation System
- 3 fall modes: instant, normal, slow
- Extended alias support: fast, cinematic
- Configurable fall height and duration
- Pre-impact visual effects (sky flash, particles, sound)
- Screen shake effect

#### Setup System
- In-game visual schematic creation
- 7 special setup tools (Blaze Rod, Recovery Compass, End Rod, Chest, Name Tag, Barrier, Bedrock)
- Mob, loot and hologram point placement
- Anchor block offset system
- MythicMobs weight editor
- Setup command blocker (/tp, /spawn, /fly, etc. blocked)
- Inventory backup (armor, offhand, XP, health, food included)

#### Loot System
- Visual loot GUI editor
- Chance, amount range and lock settings
- ItemsAdder, PDC, NBT and 1.21 Data Components preservation
- Quick import from containers (Shift + right click)
- Personal and shared loot modes
- 4 access modes: AUTO, INTERACT, BREAK, BOTH

#### Auto Meteors
- Scheduled automatic meteor system
- In-game settings GUI (AutoSetupGUI)
- World-based weighting system
- 9 location presets (flat_surface, water_surface, underground, air, etc.)
- TPS protection for low performance delay
- Location cooldown system
- Maximum active event limit

#### Hazard System
- Radiation damage (Wither effect + direct damage, green particles)
- Wind Charge waves (Sonic Boom particles, knockback)
- EMP field (Elytra disable, Ender Pearl blocking)
- Blue dust particles for EMP visual indicator
- olmeteor.bypass.hazards permission

#### Ticket System
- PDC-signed secure meteor summoning tickets
- Cooldown between uses
- Automatic refund on failed summon
- Refund on disconnect (delivered on rejoin)

#### Reward System
- 3 reward layers: loot table, ranking rewards, rewards-commands
- BossBar tracking system (distance, direction, remaining mobs, time)
- Damage leaderboard and ranking
- ActionBar notifications (damage, kills)
- Inventory preservation on death in event area

#### Rollback and Recovery
- FAWE/WorldEdit async terrain restoration
- Snapshot on impact (terrain capture)
- Crash recovery on server failure
- Configurable restoration (restore-structure-on-finish)
- Pending snapshot recovery queue

#### Persistent Data Storage
- Player statistics (player-stats.yml): damage, kills, loot, ranking rewards
- Meteor history (meteor-history.yml): type, world, location, result
- Each record stored with timestamp

### Integrations

- **CommandAPI** - Full command tree
- **FAWE / WorldEdit** - Schematic operations, rollback, default schematic creation
- **WorldGuard** - Region protection and temporary region creation
- **Towny** - Town claim avoidance
- **MythicMobs** - Custom mob and boss support
- **PlaceholderAPI** - 14+ placeholders
- **ItemsAdder** - Custom item data preservation
- **FancyHolograms** - Hologram support (coexists safely)
- **NBTAPI** - Extra NBT integration

### API

- OlMeteorAPI service (via ServicesManager)
- 4 API methods: startAt, startRandom, stop, activeEvents
- 3 Bukkit events: MeteorPreStartEvent (cancellable), MeteorImpactEvent, MeteorFinishEvent
- Folia-compatible scheduler

### Commands

| Command | Permission |
|---|---|
| /olmeteor start <type> [world] [radius] | olmeteor.start |
| /olmeteor spawnat <type> <location> [world] [mode] | olmeteor.start |
| /olmeteor stop <eventId> | olmeteor.stop |
| /olmeteor cancel <eventId> | olmeteor.cancel |
| /olmeteor setup <type> | olmeteor.setup |
| /olmeteor setupnew <type> | olmeteor.setup |
| /olmeteor editschematic <type> <name> | olmeteor.setup |
| /olmeteor schematic <name> | olmeteor.setup |
| /olmeteor setupfinish (delete / keep) | olmeteor.setup |
| /olmeteor useschematic <type> <name> | olmeteor.setup |
| /olmeteor selectmob <MythicMobId> | olmeteor.setup |
| /olmeteor settext <text> | olmeteor.setup |
| /olmeteor loot <type> | olmeteor.setup |
| /olmeteor wand | olmeteor.wand |
| /olmeteor list | olmeteor.list |
| /olmeteor info <eventId> [player] | olmeteor.info |
| /olmeteor stats [player] | olmeteor.info |
| /olmeteor history [1-50] | olmeteor.history |
| /olmeteor preview <type> | olmeteor.setup |
| /olmeteor reload | olmeteor.reload |
| /olmeteor debug | olmeteor.admin |
| /olmeteor ticket <player> <type> [1-64] | olmeteor.admin |
| /olmeteor auto | olmeteor.auto |
| /olmeteor auto on | olmeteor.auto |
| /olmeteor auto off | olmeteor.auto |
| /olmeteor auto status | olmeteor.auto |
| /olmeteor auto now | olmeteor.auto |
| /olmeteor auto setup ... | olmeteor.auto |
| /olmeteor preset <name> [minY] [maxY] | olmeteor.auto |

### Permissions

| Permission | Default |
|---|---|
| olmeteor.* | OP |
| olmeteor.admin | OP |
| olmeteor.setup | OP |
| olmeteor.start | OP |
| olmeteor.stop | OP |
| olmeteor.cancel | OP |
| olmeteor.reload | OP |
| olmeteor.list | OP |
| olmeteor.info | OP |
| olmeteor.history | OP |
| olmeteor.wand | OP |
| olmeteor.auto | OP |
| olmeteor.preset | OP |
| olmeteor.participate | Everyone |
| olmeteor.bypass.hazards | OP |

### Configuration

- `config.yml` with 9 main sections
- `command-categories.yml` for help menu customization
- `lang/messages_en.yml` for language file (locale: en)
- `player-stats.yml` for persistent player statistics
- `meteor-history.yml` for meteor history

---

## Planned Features

- [ ] Database support (MySQL/SQLite)
- [ ] In-game guide (Guide GUI)
- [ ] Customizable meteor types (custom class)
- [ ] Party system (team combat)
- [ ] Web panel
- [ ] Discord integration
