# Commands and Permissions

The main command is `/olmeteor`. Command suggestions are shown in-game via CommandAPI.

---

## Player Commands

| Command | Permission | Description |
|---|---|---|
| /olmeteor help | None | Shows the configurable help menu |
| /olmeteor list | olmeteor.list | Lists active meteors |
| /olmeteor info <eventId> [player] | olmeteor.info | Shows type, phase, world, location and distance |
| /olmeteor history [1-50] | olmeteor.history | Shows past meteor times, locations and results |
| /olmeteor stats [player] | olmeteor.info | Shows persistent damage, kills and loot statistics |

---

## Admin Commands

### Starting and Stopping Meteors

| Command | Permission | Description |
|---|---|---|
| /olmeteor start <type> [world] [radius] | olmeteor.start | Starts a meteor at a safe random location |
| /olmeteor spawnat <type> <x y z> [world] [mode] | olmeteor.start | Starts a meteor at the exact given location |
| /olmeteor stop <eventId> | olmeteor.stop | Puts the event into safe finish/rollback flow |
| /olmeteor cancel <eventId> | olmeteor.cancel | Cancels the event immediately |

Meteor types: small, medium, large, epic, legendary

Fall modes (for spawnat): instant, normal, slow

### Example Usage:
```
/olmeteor start small
/olmeteor start epic world_nether 500
/olmeteor spawnat legendary ~ ~ ~ world normal
/olmeteor stop meteor_2024_01_01_12_00
/olmeteor cancel meteor_2024_01_01_12_00
```

### Management and Settings

| Command | Permission | Description |
|---|---|---|
| /olmeteor reload | olmeteor.reload | Reloads configuration (does not reload the plugin) |
| /olmeteor debug | olmeteor.admin | Shows hook, recovery and system status |
| /olmeteor preview <type> | olmeteor.setup | Previews the meteor area without placing blocks |
| /olmeteor ticket <player> <type> [1-64] | olmeteor.admin | Gives a PDC-protected meteor summoning ticket |

### Example Usage:
```
/olmeteor reload
/olmeteor debug
/olmeteor preview legendary
/olmeteor ticket Ahmet legendary 5
```

---

## Setup Commands

| Command | Permission | Description |
|---|---|---|
| /olmeteor setup <type> | olmeteor.setup | Opens the new/existing schematic selection screen |
| /olmeteor setupnew <type> | olmeteor.setup | Starts setup from scratch directly |
| /olmeteor useschematic <type> <name> | olmeteor.setup | Assigns an existing schematic to a type |
| /olmeteor editschematic <type> <name> | olmeteor.setup | Pastes and edits an existing schematic |
| /olmeteor schematic <name> | olmeteor.setup | Saves the selected region as a schematic |
| /olmeteor setupfinish (sil / birak) | olmeteor.setup | Deletes or keeps the temporary structure after setup |
| /olmeteor selectmob <MythicMobId> | olmeteor.setup | Selects a MythicMobs creature for setup |
| /olmeteor settext <text> | olmeteor.setup | Sets the meteor hologram text |
| /olmeteor loot <type> | olmeteor.setup | Opens the loot GUI |
| /olmeteor wand | olmeteor.wand | Gives the setup selection wand |

### Example Usage:
```
/olmeteor setup large
/olmeteor editschematic epic epic_crater_v2
/olmeteor schematic my_meteor_v1
/olmeteor selectmob SkeletalKing
/olmeteor settext "Meteor Area"
/olmeteor loot medium
```

---

## Auto Meteor Commands

| Command | Description |
|---|---|
| /olmeteor auto | Opens GUI for players, status screen for console |
| /olmeteor auto on | Enables auto meteors and schedules the next one |
| /olmeteor auto off | Disables auto meteors |
| /olmeteor auto status | Shows the schedule and next time |
| /olmeteor auto now | Searches for a location immediately using auto rules |
| /olmeteor auto setup ... | Saves all basic auto settings in one command |
| /olmeteor preset <name> [minY] [maxY] | Changes the active location preset |

Auto command permission is `olmeteor.auto`.

### Example Usage:
```
/olmeteor auto
/olmeteor auto on
/olmeteor auto off
/olmeteor auto status
/olmeteor auto now
/olmeteor preset desert_surface
/olmeteor preset air 120 220
```

---

## Permission Table

| Permission | Default | Description |
|---|---|---|
| olmeteor.* | OP | All OlMeteor permissions (wildcard) |
| olmeteor.admin | OP | All admin commands |
| olmeteor.setup | OP | Setup mode and schematic operations |
| olmeteor.start | OP | Meteor starting (start, spawnat) |
| olmeteor.stop | OP | Meteor stopping |
| olmeteor.cancel | OP | Meteor cancellation |
| olmeteor.reload | OP | Config reloading |
| olmeteor.list | OP | Active meteor listing |
| olmeteor.info | OP | Meteor details and stats viewing |
| olmeteor.history | OP | Meteor history |
| olmeteor.wand | OP | Setup wand receiving |
| olmeteor.auto | OP | Auto meteor management |
| olmeteor.preset | OP | Location preset changing |
| olmeteor.participate | Everyone | Ability to participate in events |
| olmeteor.bypass.hazards | OP | Bypass radiation, EMP and other hazards |

### Example Permission Setup (LuckPerms):
```
/lp group admin permission set olmeteor.admin true
/lp group member permission set olmeteor.participate true
/lp group member permission set olmeteor.list true
```
