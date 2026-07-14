# Frequently Asked Questions (FAQ)

---

## General Questions

### Which server software does OlMeteor support?
Paper and Folia 1.21.1+ are supported. Spigot and CraftBukkit are not supported.

### What Java version is required?
Java 21 is required.

### Does it work without CommandAPI?
No. CommandAPI is a required dependency. OlMeteor safely disables itself if CommandAPI is not found.

### Is FAWE/WorldEdit required?
FAWE or WorldEdit is strongly recommended for schematic creation and terrain rollback. Without FAWE/WorldEdit, setup and rollback features will not work.

---

## Setup Questions

### How do I install the plugin?
1. Shut down the server completely
2. Place the OlMeteor JAR and CommandAPI JAR in the plugins/ folder
3. Optionally install FAWE/WorldEdit, WorldGuard, Towny, MythicMobs, etc.
4. Start the server
5. Run `/olmeteor debug` to check integration status

### How do I update the plugin?
1. Shut down the server
2. Replace the old OlMeteor JAR with the new one
3. Keep backups of config.yml, lang/, schematics/ and data files
4. Restart the server
Do NOT use `/reload`.

### Can I use `/reload`?
No. CommandAPI, WorldGuard, FAWE and Folia schedulers are not safe during live reload. Always perform a full restart.

### The plugin won't enable, what should I do?
- Verify you are using Java 21
- Check that you are using Paper/Folia 1.21.1+
- Make sure CommandAPI is installed and loads first
- Clean up old JAR files
- Fully restart the server

---

## Setup Mode Questions

### How do I exit setup mode?
There are two ways:
- Click the **Barrier** tool to exit without saving
- Click the **Bedrock** tool to save and exit
You can also run `/olmeteor setup <type>` again to reopen the exit menu.

### My inventory disappeared during setup, where is it?
Your inventory is backed up to `data/inventories/<UUID>.yml`. It is automatically restored when you successfully exit setup. If the server crashes, the file remains and can be restored manually.

### Which commands are blocked during setup?
/tp, /spawn, /home, /fly, /gamemode, /clear, /kill, /stop and similar dangerous commands are blocked. /olmeteor, /msg, /tell are allowed.

### Can I add multiple mob spawn points?
Yes. Multiple mob, loot and hologram points can be added to the same schematic. Each point is saved as an offset relative to the anchor block.

---

## Meteor Questions

### What are the meteor types?
Small (Easy, 15 blocks), Medium (Normal, 25 blocks), Large (Hard, 40 blocks), Epic (Epic, 60 blocks), Legendary (Legendary, 80 blocks).

### What is the difference between fall modes?
- **instant**: Direct impact, no animation
- **normal**: Normal animated fall (8-12 seconds)
- **slow**: Long cinematic fall (18-25 seconds)

### Why didn't the meteor fall at the specified location?
The manual `/olmeteor spawnat` command uses the exact location. Auto meteors use the location finder which searches for a safe location.

### How many meteors can run at the same time?
Auto meteors are limited by `max-active-events` (default: 1). Manual meteors can be started without limit.

---

## Loot and Reward Questions

### How do I add items to the loot GUI?
Drag items from your inventory into the GUI. During setup, you can also Shift + right click a container with the Chest tool to import its contents.

### What's the difference between personal and shared loot?
- **personal: true**: Each player sees their own separate inventory
- **personal: false**: All players share the same chest

### How do I customize rewards?
There are three reward layers:
1. **Loot GUI**: Configured with `/olmeteor loot <type>`
2. **Ranking Rewards**: Defined in `ranking-rewards` in config
3. **rewards-commands**: Commands executed when chest is opened

### Will my ItemsAdder items be lost?
No. ItemsAdder data, PDC, NBT and 1.21 Data Components are preserved.

---

## Auto Meteor Questions

### Why aren't auto meteors starting?
- Make sure the system is enabled with `/olmeteor auto on`
- Check min/max interval values
- For `air` preset, make sure minY and maxY are provided
- Check the TPS guard threshold
- Use `/olmeteor auto now` for diagnosis

### Which worlds do auto meteors work in?
They work in worlds listed in the `automatic-events.worlds` config. If the list is empty, all loaded worlds are used.

### Why do meteors keep falling in the same place?
The location cooldown system prevents recently used areas from being selected. If it's still happening, check the cooldown radius and duration settings.

### How do I prevent meteors from falling in WorldGuard/Towny areas?
Enable `location-finder.worldguard-check-claims: true` and `towny-require-wilderness: true`. Verify hooks are active with `/olmeteor debug`.

---

## Error Troubleshooting

### I get a NoClassDefFoundError
This is usually caused by an old JAR, broken WorldGuard installation, or after `/reload`. Solution:
1. Shut down the server
2. Clean up old JARs
3. Check WorldGuard and WorldEdit versions
4. Fully restart the server

### Schematic is not appearing or restoring
- Check FAWE/WorldEdit hook with `/olmeteor debug`
- Verify the schematic file name
- Make sure both corners and anchor block were selected in setup
- Check `restore-structure-on-finish: true` setting

### Mobs are spawning in wrong places
- Re-select the anchor block with Recovery Compass
- Re-save mob points with End Rod
- Verify the MythicMobs ID is correct

### Loot block won't open
- Add a loot point with the Chest tool during setup
- Verify `loot.block` is a valid material
- Check that mobs/boss are defeated
- Check `reward-top-count` and `boss-damage-threshold-percent` values

---

## Integration Questions

### How do I use PlaceholderAPI?
If PlaceholderAPI is installed, OlMeteor automatically registers the `olmeteor` expansion. No separate download is needed. Check with `/papi list`.

### How do I integrate MythicMobs?
If MythicMobs is installed, use `/olmeteor selectmob <id>` to select creatures. Weights can be adjusted with the End Rod editor in setup.

### How do I use the OlMeteor API from my plugin?
Obtain the OlMeteorAPI service through Bukkit's ServicesManager. Add the dependency as `compileOnly`. See the Developer API page for details.

---

## Performance Questions

### Does OlMeteor slow down my server?
OlMeteor uses async FAWE operations and Folia-compatible scheduling. TPS protection auto-delays events during low performance. Chunk force-load radius is limited to 3 chunks.

### Does a large loot table affect performance?
No. The loot table is managed efficiently. Each item is calculated independently.

### Can running too many meteors cause issues?
Yes. Increasing `max-active-events` can affect server performance. Keeping the default value of 1 is recommended.

---

> For more details, see the relevant wiki page or run `/olmeteor debug` to check system status.
