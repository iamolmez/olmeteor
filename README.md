OlEncounters

**Build cinematic world encounters that arrive, fight, reward, and disappear without leaving the world damaged.**

OlEncounters is a configurable encounter and world-event plugin for Paper and Folia. Create meteor impacts, portal invasions, underground structures, boss arenas, temporary resource sites, treasure events, and other server-wide encounters from one system.

Every encounter can define its own schematic, arrival animation, mobs, waves, loot, leaderboard, messages, effects, location rules, and cleanup behavior.


## More than a meteor plugin

OlEncounters is designed as a general encounter engine. A preset can represent a meteor crashing into a forest, a dungeon entrance emerging from underground, a boss arena descending from the sky, a structure arriving through a portal, or a timed resource site with personal loot.

Each preset remains independent, allowing encounters in the same server to use different structures, combat rules, rewards, messages, and visual styles.


## Full-schematic arrival animations

OlEncounters can animate the schematic itself instead of moving only a decorative core. Schematic blocks are represented with display entities during arrival, then the real structure is placed safely when the animation finishes.

Available arrival modes:

- Instant placement
- Normal sky fall
- Slow cinematic descent
- Underground rise
- Portal arrival
- Materialization
- Horizontal fly-in

Duration, distance, particles, sounds, display limits, batch size, and layer timing can be adjusted per encounter. Large structures use configurable safety limits and fallback behavior.


## In-game setup and editing

The guided setup system lets administrators create a new encounter or edit an existing schematic directly in game.

Setup tools support:

- Schematic root selection
- Selection boundaries
- Multiple mob spawn points
- Multiple loot locations
- Custom loot blocks
- Hologram positions
- Saving or exiting without saving
- Optional removal of the temporary setup structure

Selecting an existing point again removes it. Player inventories are backed up and temporary setup tools are removed when the session ends.


## MythicMobs and wave combat

Build small creature encounters, multi-wave invasions, or boss fights. Each meteor type can define multiple MythicMobs entries, selection weights, spawn locations, and wave behavior.

Mob arrivals support instant spawning, sky descent, underground emergence, portals, and materialization. Encounter mobs can be protected from natural distance-based despawning and are removed by the event lifecycle.



## Flexible loot and rewards

Loot does not have to come from one chest. Encounters may contain multiple loot points, and nearly any configured block can act as a reward source.

Supported reward features include:

- Personal or shared loot
- Drag-and-drop loot editing
- Per-item chance, weight, and amount
- Multiple loot blocks
- Reopenable containers until emptied
- Locked reward vaults
- Optional keys and custom key items
- Damage-ranking rewards
- Console-command rewards
- ItemsAdder items
- NBT, PDC, and Data Component preservation

With personal loot enabled, every player's reward state is tracked independently.


## Weighted schematic variants

Version 1.5.5 allows every meteor type to contain multiple schematic variants. The automatic system selects the meteor type first, then chooses one enabled schematic from that type's pool using configurable weights.

For example, the Small pool can contain a forest ruin, abandoned camp, and small crater while all three continue using Small encounter settings. Each schematic keeps its own mob, loot, hologram, and root positions.

Open `/olencounters auto` and select **Category Meteors** to manage variants in game. Left click enables or disables a structure, right click increases its weight, and Shift+right click decreases it.

## Advanced automatic encounters

Automatic encounters support:

- Per-world configuration
- Minimum and maximum intervals
- Custom center positions and distance ranges
- Circle, square, triangle, diamond, and hexagon search shapes
- Land, air, underground, and any-surface placement
- Configurable Y ranges
- Rough terrain and tree-top placement
- Allowed and blocked surface materials
- World-border validation
- WorldGuard region avoidance
- Towny town avoidance
- TPS protection
- Inactivity cleanup

Location searches run in controlled batches. Search results and rejection reasons are reported for easier troubleshooting.


## Messages, leaderboards, and player feedback

Every encounter can define its own start, arrival, combat, reward, and completion messages. Messages can target all players, nearby players, distant players, participants, administrators, or the console.

Optional combat feedback includes action-bar damage updates, per-encounter leaderboards, rank rewards, and clickable administrator location messages. Turkish and English locale files are included.

## Safe cleanup and terrain restoration

OlEncounters records the affected area before changing the world. Cleanup can remove the encounter structure, player changes, mobs, loot blocks, display entities, holograms, and unfinished animation tasks before restoring the original terrain.

The plugin supports cleanup countdowns, inactivity timeouts, crash recovery, and efficient large-area restoration through FAWE.


## Paper and Folia support

Region-sensitive and entity-sensitive work is dispatched through the appropriate scheduler. Expensive operations are batched, display counts are limited, and automatic encounters can pause when server performance falls below configured thresholds.

## Integrations

**Required:** CommandAPI

**Supported integrations:**

- FastAsyncWorldEdit
- WorldEdit
- WorldGuard
- Towny
- MythicMobs
- ItemsAdder
- PlaceholderAPI
- FancyHolograms
- NBT API

Optional APIs are isolated behind safe hooks. A missing optional dependency disables only its related features instead of crashing the entire plugin.

## Requirements

- OlEncounters 1.5.5
- Paper or Folia 1.21.1+
- Java 21
- CommandAPI

WorldEdit or FastAsyncWorldEdit is required for schematic-based encounters.

## Main command

```text
/olencounters
```

Setup, event management, automatic scheduling, previews, history, configuration, and diagnostic tools are organized under this command.

## Updating from OlMeteor

OlEncounters can detect a legacy OlMeteor data directory and migrate existing data. Compatibility aliases remain available for legacy commands, permissions, PlaceholderAPI identifiers, and persistent data tags. Only one plugin JAR should be installed at a time.

Developed by **OlPlugins**.
