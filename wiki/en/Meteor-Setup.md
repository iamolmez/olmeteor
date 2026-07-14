# Meteor Setup Guide

The setup system saves schematic, mob points, loot points, holograms and the anchor block together. The player's normal inventory is backed up during setup and restored on exit.

---

## Starting Setup

```
/olmeteor setup <type>
```

The command shows two options:

| Option | Description |
|---|---|
| Create from scratch | Opens a blank setup session |
| Use existing schematic | Assigns an existing schematic to the type |

### Editing an existing structure:
```
/olmeteor editschematic <type> <schematic>
```

This command temporarily pastes the structure near the player. Previously saved mob, loot and hologram points are restored.

---

## Setup Tools

When entering setup mode, special tools appear in your inventory:

| Tool | Task | Usage Detail |
|---|---|---|
| Blaze Rod (Orange) | Region selection | Left click: sets position 1. Right click: sets position 2. |
| Recovery Compass (Green) | Anchor (origin) | Selects the anchor block where the schematic's meteor center aligns. |
| End Rod (Purple) | Mob point | Right click: adds mob spawn point. Clicking the same point removes it. Opens mob chance editor. |
| Chest (Brown) | Loot point | Right click in air: opens loot GUI. Right click on block: adds/removes adjacent loot point. Shift + container click: imports contents to loot table. |
| Name Tag (Blue) | Hologram | Adds hologram/text point 2 blocks above the clicked block. |
| Barrier (Red) | Safe exit | Exits without saving, clears temporary visuals and restores inventory. |
| Bedrock (Black) | Save | Saves settings/schematic and shows cleanup choice. |

### Setup Actionbar Display:
```
P1 saved | P2 saved | Mob: 5 | Chest: 3 | Hologram: 2
```

---

## What is MythicMobs Weight?

Weight determines the spawn probability of a mob relative to other selected mobs.

- **0** --> This mob does not spawn
- **Higher value** --> More likely to be chosen
- Weights do not need to add up to 100; they are proportional to each other

Example:
```
Zombie : 75  -- ~75% of spawns are Zombie
Skeleton: 25  -- ~25% of spawns are Skeleton
```

In the editor, values change in steps: `0 -> 25 -> 50 -> 75 -> 100`.

---

## Multiple Points

Multiple mob, loot and hologram points can be added to the same schematic. All points are saved as offsets relative to the anchor block.

Example layout:
```
    Hologram
    (anchor + 0, +2, 0)

  Chest           Chest
  (anchor -2)    (anchor +2)

    Anchor Block
  (selected with Recovery Compass)

  Mob             Mob
  (anchor -3)    (anchor +3)
```

This way, when the structure is pasted at another coordinate, the points move to the correct positions.

---

## Saving and Cleanup

After clicking the Bedrock tool:

| Option | What Happens |
|---|---|
| Delete | The temporary setup terrain is restored from snapshot |
| Keep | The structure stays in the world; setup tools and session are cleaned up |

Note: Use only safe file characters in schematic names (letters, numbers, _, -). FAWE or compatible WorldEdit is required for schematic operations.

---

## Step by Step Setup

```
1. /olmeteor setup small
   -> Click "Create from scratch"

2. Select corners with Blaze Rod
   -> Left click: position 1
   -> Right click: position 2

3. Select anchor with Recovery Compass
   -> Click the block at the center of your structure

4. Add mob points with End Rod
   -> Right click at desired locations

5. Add loot points with Chest
   -> Right click next to blocks

6. Set hologram text with Name Tag
   -> /olmeteor settext "Welcome!"

7. Click Bedrock -> SAVE
   -> Choose "Delete" or "Keep"

8. Edit loot
   -> /olmeteor loot small
```

---

## Common Issues

| Issue | Solution |
|---|---|
| Schematic not saving | Check FAWE/WorldEdit hook with /olmeteor debug |
| Points in wrong place | Re-select the anchor block with Recovery Compass |
| Mobs not spawning | Check MythicMobs ID, verify weight is not 0 |
| Can't exit setup | Run /olmeteor setup <type> again to reopen exit menu |
