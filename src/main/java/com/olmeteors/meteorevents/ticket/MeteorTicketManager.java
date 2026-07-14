package com.olmeteors.meteorevents.ticket;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.olmeteors.meteorevents.event.MeteorType;
import com.olmeteors.meteorevents.event.MeteorStartResult;
import com.olmeteors.meteorevents.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import java.util.Map;
import java.util.Set;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Secure, PDC-backed consumable meteor summon tickets. */
public final class MeteorTicketManager implements Listener {
    private final MeteorPlugin plugin; private final NamespacedKey key;
    private final Map<UUID,Long> cooldowns=new ConcurrentHashMap<>();
    private final Set<UUID> pending=ConcurrentHashMap.newKeySet();
    private final Map<UUID, Queue<MeteorType>> pendingRefunds=new ConcurrentHashMap<>();
    public MeteorTicketManager(MeteorPlugin plugin) { this.plugin=plugin; this.key=new NamespacedKey(plugin,"meteor_ticket"); }
    public ItemStack create(@NotNull MeteorType type, int amount) {
        Material material=Material.matchMaterial(plugin.getConfig().getString("event.tickets.material","FIRE_CHARGE"));
        if(material==null||material.isAir()) material=Material.FIRE_CHARGE;
        final ItemStack item=new ItemStack(material,Math.max(1,Math.min(64,amount))); final var meta=item.getItemMeta();
        meta.displayName(MessageUtil.parse("&6&lMeteor Bileti &8• &e"+plugin.getConfigManager().getMeteorTypeName(type)));
        meta.lore(java.util.List.of(MessageUtil.parse("&7Sağ tıklayarak meteor çağırır.")));
        meta.getPersistentDataContainer().set(key,PersistentDataType.STRING,type.name()); item.setItemMeta(meta); return item;
    }
    @EventHandler public void onUse(PlayerInteractEvent event) {
        if(event.getHand()!=EquipmentSlot.HAND || (event.getAction()!=Action.RIGHT_CLICK_AIR
                && event.getAction()!=Action.RIGHT_CLICK_BLOCK)) return;
        final ItemStack item=event.getItem(); if(item==null||!item.hasItemMeta()) return;
        final String raw=item.getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.STRING);
        if(raw==null) return; event.setCancelled(true); final Player player=event.getPlayer();
        final long now=System.currentTimeMillis(); final int seconds=Math.max(0,
                plugin.getConfig().getInt("event.tickets.cooldown-seconds",300));
        final long remaining=cooldowns.getOrDefault(player.getUniqueId(),0L)-now;
        if(remaining>0) { MessageUtil.sendMessage(player,"&cBileti tekrar kullanmak için &e"+
                ((remaining+999)/1000)+" saniye &cbeklemelisin."); return; }
        final MeteorType type; try { type=MeteorType.fromString(raw); } catch(Exception ignored) { return; }
        if (!pending.add(player.getUniqueId())) {
            MessageUtil.sendMessage(player,"&eÖnceki meteor bileti için hâlâ güvenli konum aranıyor.");
            return;
        }
        item.setAmount(item.getAmount()-1);
        MessageUtil.sendMessage(player,"&eMeteor bileti doğrulandı; güvenli konum aranıyor.");
        plugin.getMeteorEventManager().startEvent(type,player.getWorld().getName(),player)
                .whenComplete((result,error) -> plugin.getFoliaScheduler().callGlobal(() -> {
                    pending.remove(player.getUniqueId());
                    final boolean started=error==null && result==MeteorStartResult.STARTED;
                    if (started) {
                        cooldowns.put(player.getUniqueId(),System.currentTimeMillis()+seconds*1000L);
                        if(player.isOnline()) plugin.getFoliaScheduler().runForEntity(player, () ->
                                MessageUtil.sendMessage(player,"&aMeteor bileti kullanıldı."));
                        return;
                    }
                    if(player.isOnline()) plugin.getFoliaScheduler().runForEntity(player,
                            () -> refund(player,type));
                    else pendingRefunds.computeIfAbsent(player.getUniqueId(),ignored->new ConcurrentLinkedQueue<>())
                            .add(type);
                }));
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        final Queue<MeteorType> refunds=pendingRefunds.remove(event.getPlayer().getUniqueId());
        if(refunds==null) return;
        MeteorType type;
        while((type=refunds.poll())!=null) refund(event.getPlayer(),type);
    }

    private void refund(Player player,MeteorType type) {
        final Map<Integer,ItemStack> remaining=player.getInventory().addItem(create(type,1));
        remaining.values().forEach(item->player.getWorld().dropItemNaturally(player.getLocation(),item));
        MessageUtil.sendMessage(player,"&cMeteor başlatılamadı; biletin iade edildi.");
    }
}
