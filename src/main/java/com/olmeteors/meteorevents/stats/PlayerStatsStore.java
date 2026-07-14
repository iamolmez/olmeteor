package com.olmeteors.meteorevents.stats;

import com.olmeteors.meteorevents.MeteorPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

/** Small persistent store for player meteor progression. */
public final class PlayerStatsStore {
    private final MeteorPlugin plugin; private final File file; private final YamlConfiguration data;
    public PlayerStatsStore(MeteorPlugin plugin) {
        this.plugin=plugin; this.file=new File(plugin.getDataFolder(), "player-stats.yml");
        this.data=YamlConfiguration.loadConfiguration(file);
    }
    public synchronized void addDamage(UUID id, double amount) { add(id,"damage",amount); }
    public synchronized void addKill(UUID id) { add(id,"kills",1); save(); }
    public synchronized void addLoot(UUID id) { add(id,"loot-claims",1); save(); }
    public synchronized void addRankingReward(UUID id) { add(id,"ranking-rewards",1); save(); }
    public synchronized Stats get(UUID id) { final String b="players."+id+"."; return new Stats(
            data.getDouble(b+"damage"),data.getLong(b+"kills"),data.getLong(b+"loot-claims"),
            data.getLong(b+"ranking-rewards")); }
    private void add(UUID id,String key,double value) { final String p="players."+id+"."+key; data.set(p,data.getDouble(p)+value); }
    public synchronized void save() { try { data.save(file); } catch(IOException e) {
        plugin.getLogger().log(Level.WARNING,"Could not save player statistics",e); } }
    public record Stats(double damage,long kills,long lootClaims,long rankingRewards) {}
}
