# Loot and Rewards

OlMeteor supports three reward layers together: loot GUI, ranking rewards and console commands.

---

## Loot GUI

```
/olmeteor loot <type>
```

### Usage:
| Action | How to Do It |
|---|---|
| Add item | Drag normal or custom items into the GUI |
| Edit chance | Left click on item |
| Edit amount | Right click on item |
| Toggle lock | Shift + click on item |
| Remove item | Press Q on item |
| Save | Green Save button |
| Cancel | Red Cancel button |
| Clear | Clear button |
| Reset | Restore Defaults button |

### Quick Transfer:
During setup, Shift + right clicking a container with the Chest tool imports its contents into the loot table.

### Data Preservation:
ItemsAdder data, PDC, vanilla/custom NBT and 1.21 Data Components are preserved. If NBTAPI is installed, extra integration is used but basic data preservation works without it.

---

## Personal and Shared Loot

Loot settings for each meteor type:

```yaml
meteor-types:
  small:
    loot:
      block: "CHEST"
      access-mode: "AUTO"
      personal: true
      inventory-title: "Small Meteor Reward"
```

### Access Modes

| Mode | Behavior |
|---|---|
| AUTO | Chest/barrel -> right click | Normal block -> mine |
| INTERACT | Always opens with right click |
| BREAK | Opens when block is mined |
| BOTH | Opens with both right click and mining |

### Personal vs Shared:

| Feature | personal: true | personal: false |
|---|---|---|
| Each player sees their own inventory | Yes | No |
| All players see the same chest | No | Yes |
| First come first served | Personal | Shared |

### Usable Blocks:
```yaml
block: "CHEST"
block: "BARREL"
block: "ANCIENT_DEBRIS"
block: "CRYING_OBSIDIAN"
block: "RESPAWN_ANCHOR"
block: "ENDER_CHEST"
block: "SHULKER_BOX"
```

Any block can be used. If the block doesn't have a native inventory, it opens when mined in BREAK or BOTH mode.

---

## Who Can Claim Rewards?

```yaml
event:
  vault:
    boss-damage-threshold-percent: 10
    reward-top-count: 3
```

| Setting | Description |
|---|---|
| boss-damage-threshold-percent | Minimum boss damage percentage required |
| reward-top-count | How many players can open the reward block |

Example: 10 players attacked the boss. reward-top-count=3 means only the top 3 in damage ranking can open the reward chest. Players who dealt less than 10% boss damage are excluded from the ranking.

---

## Ranking Rewards

```yaml
meteor-types:
  legendary:
    ranking-rewards:
      "1":
        items:
          - "NETHERITE_INGOT:2"
        commands:
          - "eco give %player% 5000"
      "2":
        items:
          - "DIAMOND:8"
        commands: []
      "3":
        items:
          - "DIAMOND:4"
        commands: []
```

### Example Reward Configurations:

First Place:
```yaml
"1":
  items:
    - "NETHERITE_INGOT:3"
    - "DIAMOND:16"
  commands:
    - "eco give %player% 10000"
    - "minecraft:xp add %player% 500 points"
```

Second Place:
```yaml
"2":
  items:
    - "DIAMOND:8"
  commands:
    - "eco give %player% 5000"
```

### Reward Command Tips:

Recommended - Use namespace:
```yaml
commands:
  - "minecraft:xp add %player% 100 points"
```

Avoid - EssentialsX conflicts:
```yaml
commands:
  - "xp give %player% 100"
```

`%player%` is replaced with the winner's name. For XP, use the vanilla namespace instead of Essentials' `xp` command. This prevents `MissingResourceException` errors from EssentialsX language pack conflicts.

---

## Disabling Feedback

The following can be disabled globally or per meteor type:

```yaml
combat-feedback:
  damage-actionbar: true
  kill-actionbar: true
  broadcast-leaderboard: true
  leaderboard-size: 5
```

### ActionBar Formats:
```yaml
damage-actionbar-text: "Damage: %damage% | Rank: #%rank%"
kill-actionbar-text: "Mob killed! Remaining: %remaining% | Rank: #%rank%"
```

### Leaderboard Formats:
```yaml
leaderboard-title: "Meteor Damage Leaderboard"
leaderboard-entry: "#%rank% %player% - %damage% damage"
leaderboard-empty: "No player damage recorded for this meteor."
```

---

## Persistent Player Statistics

Player achievements in meteor events are permanently stored in `player-stats.yml`:

| Statistic | Description | Placeholder |
|---|---|---|
| damage | Total damage dealt | %olmeteor_player_damage% |
| kills | Meteor mobs killed | %olmeteor_player_kills% |
| loot-claims | Times loot was claimed | %olmeteor_player_loot% |
| ranking-rewards | Ranking rewards received | (no plugin placeholder) |

Note: `/olmeteor stats [player]` shows all 4 statistics.

---

## Summary: Reward Layers

| Layer | Source | Trigger |
|---|---|---|
| 1 | Loot GUI | Configured via /olmeteor loot <type>, given when chest is opened |
| 2 | Ranking Rewards | Defined in ranking-rewards, given automatically on boss death |
| 3 | rewards-commands | Defined in rewards-commands, executed when chest is opened |
