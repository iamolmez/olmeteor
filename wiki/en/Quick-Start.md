# Quick Start

In this example we will create a small meteor structure, add loot, and test it at a specific location.

---

## Step 1: Open Setup Mode

```
/olmeteor setup small
```

Two options will appear in chat:

| Option | Description |
|---|---|
| Create from scratch | Starts a blank setup session |
| Use existing schematic | Assigns an existing schematic to the type |

Click **Create from scratch**. Your inventory will be temporarily backed up and setup tools will be given to you.

---

## Step 2: Prepare the Structure

The following tools are used during setup:

| Tool | Task | Usage |
|---|---|---|
| Blaze Rod | Region selection | Left click: pos 1 - Right click: pos 2 |
| Recovery Compass | Anchor block | Selects the center block for alignment |
| End Rod | Mob point | Right click: add/remove mob spawn point |
| Chest | Loot point | Right click on block: add/remove loot point |
| Name Tag | Hologram | Adds a text point 2 blocks above the clicked block |
| Barrier | Safe exit | Exits without saving, clears temporary structures |
| Bedrock | Save | Saves all settings and shows cleanup options |

### Step by Step:

```
1. Left click with Blaze Rod to set position 1.
2. Right click with Blaze Rod to set position 2.
3. Use Recovery Compass to select the anchor block (center).
4. Use End Rod to add mob spawn points.
5. Use Chest to add one or more loot points.
6. Use Name Tag to add hologram points.
```

Note: Clicking the same mob or loot point again removes it.

---

## Step 3: Save

Click the **Bedrock** tool. If you selected a region, the structure is saved as a `.schem` file. Then you can choose to delete or keep the temporary structure in the world.

---

## Step 4: Edit Loot

```
/olmeteor loot small
```

In the loot GUI:

| Action | How to Do It |
|---|---|
| Add item | Drag from inventory to GUI |
| Edit chance | Left click on item |
| Edit amount | Right click on item |
| Toggle lock | Shift + click on item |
| Remove item | Press Q on item |
| Save | Green Save button at bottom |
| Reset | Restore Defaults button |

ItemsAdder custom item data (PDC, NBT, Data Components) is preserved.

---

## Step 5: Test the Meteor

### At your current location:
```
/olmeteor spawnat small ~ ~ ~ normal
```

### At specific coordinates:
```
/olmeteor spawnat small 125 80 -340 world slow
```

### Fall Modes:

| Mode | Description |
|---|---|
| instant | Direct impact, no animation |
| normal | Normal animated fall (8-12 seconds) |
| slow | Long cinematic fall (18-25 seconds) |

---

## Step 6: Verify the Result

```
/olmeteor list              -- List active meteors
/olmeteor info <eventId>    -- Show meteor details
/olmeteor history 10        -- Last 10 meteor history
```

### Event Flow:

1. Meteor incoming! (warning phase)
2. Meteor impacted! (impact moment)
3. Wave 1/3 started! (mob waves)
4. Boss spawned! (after last wave)
5. Boss defeated! Loot opened!
6. Area cleaning... (rollback)
7. Area restored!

After mobs and boss are defeated, eligible players can open loot blocks. When the cleanup timer expires, the schematic area is restored from snapshot, including blocks broken or added by players.

---

## Extra: Preview Command

You can preview the meteor area without placing blocks:

```
/olmeteor preview <type>
```

Starts a 15-second preview. Color codes:
- **End Rod** particles: Meteor border
- **Flame** particles: Mob spawn points
- **Happy Villager** particles: Loot points

---

## Extra: Setup Command Blocker

While in setup mode, dangerous or disruptive commands are automatically blocked:

**Blocked commands:** /clear, /tp, /spawn, /home, /fly, /gamemode, /gm, /kill, /stop, /reload, /pl
**Allowed commands:** /olmeteor, /msg, /tell, /r, /reply

This prevents accidental inventory loss, teleportation or server shutdown during setup.

**Inventory Backup:**
When setup starts, your inventory (including armor, offhand, XP level, health and food) is saved to `data/inventories/<UUID>.yml`. After successfully exiting setup, the backup is automatically deleted. If the server crashes, the backup file remains and can be restored.
