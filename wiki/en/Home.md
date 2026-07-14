# OlMeteor Wiki

OlMeteor is a fully customizable meteor event plugin for Paper and Folia servers. It combines schematic-based impact zones, animated falling, wave-based mob systems, personal loot, damage ranking, automatic events, and safe terrain restoration in a single package.

> This documentation is prepared for **OlMeteor 1.4.0** and **Paper/Folia 1.21.1+**. The server must use **Java 21**.

---

## Features

### Meteor Classes

| Class | Difficulty | Impact Radius |
|---|---|---|
| Small | Easy | 15 blocks |
| Medium | Normal | 25 blocks |
| Large | Hard | 40 blocks |
| Epic | Epic | 60 blocks |
| Legendary | Legendary | 80 blocks |

### Setup and Schematic System
- In-game visual schematic creation and editing
- Multiple mob, hologram, and loot points in a single schematic
- FAWE/WorldEdit instant snapshot and rollback

### Combat and Wave System
- MythicMobs mob and boss support
- Wave system with progressive mob waves
- Type-specific fall animations (instant, normal, slow)
- Damage/kill ActionBar notification and ranked leaderboard

### Loot and Reward System
- Personal or shared loot chests
- Any block type supported (chest, barrel, ancient debris, etc.)
- ItemsAdder, PDC, NBT and 1.21 Data Components preservation
- Rank-based rewards (items + commands)

### Automatic System
- World, surface, Y-level, interval and radius controlled auto meteors
- WorldGuard and Towny claim avoidance
- TPS protection with low-performance delay

### Safety and Rollback
- Full terrain restoration including player modifications
- Crash recovery from disk snapshots
- PDC-signed meteor tickets for secure summoning

### Integrations

| Plugin | Type |
|---|---|
| CommandAPI | Required |
| FAWE / WorldEdit | Recommended |
| WorldGuard | Optional |
| Towny | Optional |
| MythicMobs | Optional |
| PlaceholderAPI | Optional |
| ItemsAdder | Optional |
| FancyHolograms | Optional |
| NBTAPI | Optional |

---

## Quick Start

1. Install dependencies from the **Setup** page.
2. Create your first meteor structure with `/olmeteor setup small`.
3. Edit the loot table with `/olmeteor loot small`.
4. Test it with `/olmeteor spawnat small ~ ~ ~ world normal`.
5. Enable the automatic meteor system when ready.

---

## Wiki Pages

### Getting Started
- **Setup** - Requirements, installation steps and updates
- **Quick Start** - Creating and testing your first meteor

### Management
- **Commands and Permissions** - All commands, permissions and usage examples
- **Meteor Setup Guide** - Visual setup system and tools
- **Auto Meteors** - Automatic scheduling and location finding
- **Loot and Rewards** - Loot table, ranking and rewards
- **Configuration** - Config.yml detailed explanations

### Integration and API
- **Integrations** - Supported plugins and hook system
- **PlaceholderAPI** - All placeholders and usage examples
- **Developer API** - API documentation for plugin developers

### Updates
- **Changelog** - Version history and changes

### Support
- **FAQ** - Frequently asked questions
- **Troubleshooting** - Common issues and solutions

---

## Extra Features

### Adjustable Difficulty System
Each meteor type has a code-level difficulty (Easy, Normal, Hard, Epic, Legendary) and reward slot counts (Small: 10-25, Legendary: 75-150).

### Fall Mode Aliases
Multiple name aliases for fall modes:
- `instant` = fast
- `normal` = default
- `slow` = cinematic

### BossBar Tracking
Active meteors automatically show a BossBar to players: distance, direction, remaining mob count and time remaining.

### Setup Command Blocker
In setup mode, dangerous commands like /clear, /tp, /spawn, /home are blocked. /olmeteor and /msg are allowed.

### Inventory Backup
At setup start, inventory (armor, offhand, XP, health, food) is backed up to disk. Automatically restored on exit and deleted.

### Crash Recovery
Incomplete snapshots from server crashes are automatically detected and restored on next startup.

### Persistent Statistics
4 statistics are stored permanently: total damage, mob kills, loot count, ranking reward count.

### Default Meteor Schematic
If FAWE is installed, a 13x6x13 default crater schematic is automatically created on first startup.

---

> **IMPORTANT:** Do not use `/reload` after changing the plugin or its dependencies. Fully restart the server. CommandAPI, WorldGuard, FAWE and Folia schedulers are not safe during live reload.
