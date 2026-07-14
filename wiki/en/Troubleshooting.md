# Troubleshooting

## Quick Diagnosis

When experiencing issues, first run these commands:

```
/olmeteor debug       -- System and integration status
/olmeteor list        -- Active meteors
/olmeteor auto status -- Auto meteor schedule
```

---

## Plugin Won't Enable

Checklist:
- Is Java 21 being used?
- Is Paper/Folia 1.21.1+ being used?
- Is CommandAPI installed and loading first?
- Did this happen after /reload? (Restart the server fully)
- Are old JAR files cleaned up?
- Have you checked the console logs?

---

## NoClassDefFoundError

Error:
```
java.lang.NoClassDefFoundError: com.sk89q.worldguard...
```

Causes:
1. Old JAR - Outdated OlMeteor version
2. Broken WorldGuard installation - Corrupted or incompatible WorldGuard JAR
3. Live reload - Class loading issue after /reload

Solution:
1. Shut down the server
2. Remove old OlMeteor JARs (keep only the latest version)
3. Verify WorldGuard and WorldEdit/FAWE versions are compatible
4. Fully restart the server (don't use PlugMan)

---

## Schematic Not Appearing or Not Restoring

Checklist:
- Is the FAWE/WorldEdit hook active? Check with /olmeteor debug
- Is the schematic file name correct for the meteor type?
- Were both corners and the anchor block selected during setup?
- Is restore-structure-on-finish: true enabled?
- Is there write permission for the snapshot/recovery folder?

Test the normal event completion or `/olmeteor stop <id>` flow instead of force-stopping. Place schematic files in the `plugins/OlMeteor/schematics/` folder. Avoid Turkish characters and spaces in file names.

---

## Mobs Spawning in Wrong Places

1. Open the structure with `/olmeteor editschematic <type> <schematic>`
2. Re-select the anchor block with Recovery Compass
3. Re-save mob points with End Rod
4. Remember that clicking the same point again removes it
5. Verify the MythicMobs ID is exact and correct

| Issue | Solution |
|---|---|
| Wrong anchor position | Re-select with Recovery Compass |
| Mob point removed | Add it again with End Rod |
| Wrong MythicMobs ID | Use /olmeteor selectmob to choose correct ID |
| Weight is 0 | Set weight above 0 in the editor |

---

## Loot Block Not Visible or Not Opening

Step by step check:
1. Add a loot point with the Chest tool during setup
2. Verify loot.block is a valid Bukkit material
3. Check that mobs/boss are dead
4. Check that the player qualifies in the damage ranking
5. Check reward-top-count and boss-damage-threshold-percent values

| Issue | Solution |
|---|---|
| No loot point added | Add one with the Chest tool in setup |
| Wrong block name | Use a valid Bukkit ID like CHEST |
| Boss not defeated yet | Kill the boss first |
| Player not in ranking | Deal more damage |
| Personal/shared confusion | Check personal: true/false setting |

---

## Meteor Falling on Town or WorldGuard Region

These checks are for the auto location finder; the manual spawnat command uses the exact admin-specified location.

Solution:
1. Enable towny-require-wilderness: true
2. Enable worldguard-check-claims: true
3. Verify both hooks are active with /olmeteor debug
4. Check the auto event is using the current rules with /olmeteor auto status

---

## EssentialsX XP Error

The `xp` command in reward commands can be hijacked by another plugin.

Correct usage:
```yaml
commands:
  - "minecraft:xp add %player% 100 points"
```

Incorrect usage:
```yaml
commands:
  - "xp give %player% 100"
```

---

## Auto Meteor Not Starting

Checklist:
- Is the system activated with /olmeteor auto on?
- Are min/max interval values in the correct order?
- Is air preset provided with minY and maxY values?
- Is the TPS guard threshold above current TPS?
- Are claim, surface and cooldown rules allowing any location?
- Test with /olmeteor auto now for diagnosis

---

## When Reporting an Issue

Include the following information:

1. Server version (Paper 1.21.3, etc.)
2. OlMeteor version
3. Installed integration versions
4. FULL log from the first error line to the last 'Caused by' section
5. /olmeteor debug output
