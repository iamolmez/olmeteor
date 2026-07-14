package com.olmeteors.meteorevents.loot;

import com.olmeteors.meteorevents.MeteorPlugin;
import com.olmeteors.meteorevents.display.DisplayEntityManager;
import com.olmeteors.meteorevents.event.ActiveMeteorEvent;
import com.olmeteors.meteorevents.event.EventPhase;
import com.olmeteors.meteorevents.event.MeteorType;
import com.olmeteors.meteorevents.hook.MythicMobsHook;
import com.olmeteors.meteorevents.util.DataComponentUtil;
import com.olmeteors.meteorevents.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.TrialSpawner;
import org.bukkit.block.Vault;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Manages the Trial Vault loot system for meteor events.
 * Implements boss-locked vaults with personal key distribution
 * based on boss damage contribution.
 */
public final class VaultManager implements Listener {
    private static final UUID SHARED_LOOT_SCOPE = new UUID(0L, 0L);

    private final MeteorPlugin plugin;
    private final DisplayEntityManager displayEntityManager;
    private final MythicMobsHook mythicMobsHook;

    private final Map<String, List<VaultData>> eventVaults;
    private final Map<String, Set<UUID>> rankingRewardsGranted = new ConcurrentHashMap<>();
    private final Map<LootSessionKey, Inventory> personalLoot = new ConcurrentHashMap<>();

    // ── Config-based message helper ────────────────────────────
    private void tell(Player player, String path, String... placeholders) {
        plugin.getConfigManager().sendMessage(player, path, placeholders);
    }

    public VaultManager(MeteorPlugin plugin, DisplayEntityManager displayEntityManager,
                        MythicMobsHook mythicMobsHook) {
        this.plugin = plugin;
        this.displayEntityManager = displayEntityManager;
        this.mythicMobsHook = mythicMobsHook;
        this.eventVaults = new ConcurrentHashMap<>();
    }

    /**
     * Spawns a vault block at the center of the meteor event.
     *
     * @param center the center location
     * @param event  the meteor event
     */
    public void spawnVault(@NotNull Location center, @NotNull ActiveMeteorEvent event) {
        try {
            final List<org.bukkit.util.Vector> chestOffsets = plugin.getConfigManager()
                    .getChestOffsets(event.meteorType());
            final List<org.bukkit.util.Vector> actualOffsets = chestOffsets.isEmpty()
                    ? List.of(new org.bukkit.util.Vector(0, 1, 0)) : chestOffsets;
            final List<VaultData> vaults = new CopyOnWriteArrayList<>();
            eventVaults.put(event.eventId(), vaults);
            for (final org.bukkit.util.Vector offset : actualOffsets) {
                final Location vaultLoc = center.clone().add(offset).toBlockLocation();
                plugin.getFoliaScheduler().runAtLocation(vaultLoc, () -> {
                    try {
                        // The event may have finished while this region task was queued.
                        // Never recreate a reward block after removeVault() has run.
                        if (eventVaults.get(event.eventId()) != vaults) return;
                        final Material lootBlock = plugin.getConfigManager()
                                .getLootBlockMaterial(event.meteorType());
                        vaultLoc.getBlock().setType(lootBlock, false);
                        if (eventVaults.get(event.eventId()) != vaults) {
                            vaultLoc.getBlock().setType(Material.AIR, false);
                            return;
                        }
                        vaults.add(new VaultData(vaultLoc, event.eventId(),
                                event.meteorType(),
                                false, ConcurrentHashMap.newKeySet(), new AtomicBoolean(false)));
                        plugin.getLogger().info("Reward loot block spawned for event: " + event.eventId()
                                + " at " + formatLocation(vaultLoc));
                        plugin.getFoliaScheduler().runLaterAtLocation(vaultLoc, () -> {
                            if (eventVaults.containsKey(event.eventId())
                                    && vaultLoc.getBlock().getType() != lootBlock) {
                                vaultLoc.getBlock().setType(lootBlock, false);
                                plugin.getLogger().warning("Reward block was overwritten and restored at "
                                        + formatLocation(vaultLoc));
                            }
                        }, 20L);
                    } catch (Exception error) {
                        plugin.getLogger().log(Level.SEVERE,
                                "Failed to create reward chest at " + formatLocation(vaultLoc), error);
                    }
                });
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to spawn vault", e);
        }
    }

    /**
     * Removes a vault block for a specific event.
     *
     * @param eventId the event ID
     */
    public void removeVault(@NotNull String eventId) {
        final List<VaultData> vaults = eventVaults.remove(eventId);
        rankingRewardsGranted.remove(eventId);
        personalLoot.entrySet().stream().filter(entry -> entry.getKey().eventId().equals(eventId))
                .map(Map.Entry::getValue).distinct().forEach(inventory ->
                        List.copyOf(inventory.getViewers()).forEach(viewer -> {
                            if (viewer instanceof Player player) {
                                plugin.getFoliaScheduler().runForEntity(player, player::closeInventory);
                            }
                        }));
        personalLoot.keySet().removeIf(key -> key.eventId().equals(eventId));
        if (vaults != null) {
            vaults.forEach(vault -> plugin.getFoliaScheduler().runAtLocation(
                    vault.location(), () -> vault.location().getBlock().setType(Material.AIR)));
        }
    }

    /**
     * Distributes vault keys to players who contributed significant damage to the boss.
     * Called when the boss is defeated.
     *
     * @param event the meteor event
     */
    public void distributeKeys(@NotNull ActiveMeteorEvent event) {
        final double damageThreshold = plugin.getConfigManager().getBossDamageThreshold();

        // The boss is dead, so the vault must never remain permanently locked.
        final List<VaultData> currentVaults = eventVaults.get(event.eventId());
        if (currentVaults != null) {
            currentVaults.replaceAll(VaultData::unlock);
        }

        // Calculate total damage
        final double totalDamage = event.playerDamageMap().values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        if (totalDamage <= 0) {
            // No keys to distribute if no damage was dealt
            return;
        }

        // Distribute keys based on damage contribution (min 10% threshold)
        final double threshold = damageThreshold;
        int keysDistributed = 0;

        for (final var entry : event.playerDamageMap().entrySet()) {
            final double contribution = (entry.getValue() / totalDamage) * 100;

            if (contribution >= threshold) {
                final Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    final ItemStack key = DataComponentUtil.createMeteorKey(
                            plugin.getConfigManager(),
                            plugin.getConfigManager().getMeteorTypeName(event.meteorType()));
                    player.getInventory().addItem(key);

                    tell(player, "vault.key_received",
                            "damage", String.format("%.1f", contribution));
                    keysDistributed++;
                }
            }
        }

        // Unlock the vault
        final List<VaultData> vaults = eventVaults.get(event.eventId());
        if (vaults != null) {
            vaults.replaceAll(VaultData::unlock);
        }

        plugin.getLogger().info("Distributed " + keysDistributed
                + " vault keys for event: " + event.eventId());
    }

    public void unlockEventChests(@NotNull String eventId) {
        final List<VaultData> vaults = eventVaults.get(eventId);
        if (vaults != null) vaults.replaceAll(VaultData::unlock);
    }

    // ---- Event Handlers ----

    @EventHandler
    public void onVaultInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        final Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        final VaultData vault = findVault(clickedBlock.getLocation());
        if (vault == null) return;
        event.setCancelled(true);
        if (allowsInteraction(vault)) accessVault(event.getPlayer(), clickedBlock.getLocation());
        else tell(event.getPlayer(), "vault.mine_to_open");
    }

    @EventHandler(ignoreCancelled = true)
    public void onLootBlockBreak(BlockBreakEvent event) {
        final VaultData vault = findVault(event.getBlock().getLocation());
        if (vault == null) return;
        event.setCancelled(true);
        if (allowsBreaking(vault)) accessVault(event.getPlayer(), event.getBlock().getLocation());
        else tell(event.getPlayer(), "vault.interact_to_open");
    }

    private boolean accessVault(@NotNull Player player, @NotNull Location blockLoc) {
        for (final var entry : eventVaults.entrySet()) {
          for (final VaultData vaultData : entry.getValue()) {

            if (!vaultData.location().equals(blockLoc)) continue;

            final ActiveMeteorEvent meteorEvent = plugin.getMeteorEventManager()
                    .getEvent(vaultData.eventId());

            if (meteorEvent == null || (meteorEvent.phase() != EventPhase.IMPACT
                    && meteorEvent.phase() != EventPhase.ACTIVE)) {
                tell(player, "vault.inactive");
                return true;
            }

            // All tracked mobs are dead: unlock even when there is no boss or a
            // configured MythicMob failed to spawn. This avoids permanent locks.
            if (!vaultData.unlocked()) unlockEventChests(vaultData.eventId());

            if (plugin.getMeteorEventManager().hasLivingMeteorMobs(vaultData.eventId())) {
                tell(player, "vault.mobs_alive");
                return true;
            }

            // If the chest is allowed to open, combat is objectively complete.
            // This also covers MythicMobs transformations that skip EntityDeathEvent.
            plugin.getMeteorEventManager().completeCombatIfReady(vaultData.eventId());

            if (!meteorEvent.playerDamageMap().isEmpty()
                    && damageRank(meteorEvent, player.getUniqueId())
                    > plugin.getConfigManager().getRewardTopCount()) {
                tell(player, "vault.top_only", "count",
                        String.valueOf(plugin.getConfigManager().getRewardTopCount()));
                return true;
            }


            // Check if player already opened this vault
            if (!plugin.getConfigManager().isPersonalLoot(meteorEvent.meteorType())
                    && vaultData.claimed().get()) {
                tell(player, "vault.already_opened");
                return true;
            }
            if (vaultData.openedBy().contains(player.getUniqueId())) {
                tell(player, "vault.already_opened");
                return true;
            }

            // Open vault (grant loot)
            openVault(player, vaultData);
            grantRankingRewards(player, meteorEvent);

            return true;
          }
        }
        return false;
    }

    private @Nullable VaultData findVault(@NotNull Location location) {
        return eventVaults.values().stream().flatMap(List::stream)
                .filter(vault -> vault.location().equals(location)).findFirst().orElse(null);
    }

    private boolean allowsInteraction(@NotNull VaultData vault) {
        final String mode = plugin.getConfigManager().getLootAccessMode(vault.meteorType());
        return mode.equals("INTERACT") || mode.equals("BOTH")
                || (mode.equals("AUTO") && plugin.getConfigManager()
                .getLootBlockMaterial(vault.meteorType()).isInteractable());
    }

    private boolean allowsBreaking(@NotNull VaultData vault) {
        final String mode = plugin.getConfigManager().getLootAccessMode(vault.meteorType());
        return mode.equals("BREAK") || mode.equals("BOTH")
                || (mode.equals("AUTO") && !plugin.getConfigManager()
                .getLootBlockMaterial(vault.meteorType()).isInteractable());
    }

    /**
     * Opens the vault for a player and grants them loot.
     *
     * @param player    the player
     * @param vaultData the vault data
     */
    private void openVault(@NotNull Player player, @NotNull VaultData vaultData) {
        // Generate personal loot based on the meteor type
        final ActiveMeteorEvent meteorEvent = plugin.getMeteorEventManager()
                .getEvent(vaultData.eventId());

        if (meteorEvent == null) return;
        final boolean personal = plugin.getConfigManager().isPersonalLoot(meteorEvent.meteorType());

        // Generate and give loot items
        final var lootTable = plugin.getLootGUIEditor().getLootTable(meteorEvent.meteorType());
        if (lootTable != null) {
            final var rewards = lootTable.generateLoot();
            final int size = Math.min(54, Math.max(9,
                    ((Math.min(rewards.size(), 54) + 8) / 9) * 9));
            final LootSessionKey key = LootSessionKey.of(vaultData,
                    personal ? player.getUniqueId() : SHARED_LOOT_SCOPE);
            final Inventory rewardChest = personalLoot.computeIfAbsent(key, ignored -> {
                final Inventory created = Bukkit.createInventory(null, size,
                        plugin.getConfigManager().getLootInventoryTitle(meteorEvent.meteorType()));
                for (int slot = 0; slot < rewards.size() && slot < size; slot++) {
                    created.setItem(slot, rewards.get(slot));
                }
                return created;
            });
            if (isEmpty(rewardChest)) {
                personalLoot.remove(key);
                completeClaim(vaultData, player, meteorEvent, personal);
                return;
            }
            player.openInventory(rewardChest);
        }

        tell(player, personal ? "vault.opened" : "vault.opened_shared");
    }

    @EventHandler
    public void onLootInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        final var match = personalLoot.entrySet().stream()
                .filter(entry -> entry.getValue() == event.getInventory()
                        && (entry.getKey().scopeId().equals(player.getUniqueId())
                        || entry.getKey().scopeId().equals(SHARED_LOOT_SCOPE))).findFirst();
        if (match.isEmpty() || !isEmpty(event.getInventory())) return;
        final LootSessionKey key = match.get().getKey();
        personalLoot.remove(key);
        final List<VaultData> vaults = eventVaults.get(key.eventId());
        if (vaults == null) return;
        final ActiveMeteorEvent meteorEvent = plugin.getMeteorEventManager().getEvent(key.eventId());
        if (meteorEvent == null) return;
        vaults.stream().filter(vault -> vault.location().getBlockX() == key.x()
                        && vault.location().getBlockY() == key.y()
                        && vault.location().getBlockZ() == key.z()).findFirst().ifPresent(vault ->
                completeClaim(vault, player, meteorEvent,
                        plugin.getConfigManager().isPersonalLoot(meteorEvent.meteorType())));
    }

    @EventHandler public void onLootClick(InventoryClickEvent event) {
        if (!personalLoot.containsValue(event.getInventory())) return;
        final int topSize=event.getInventory().getSize();
        if (event.getRawSlot()>=topSize) { if(event.isShiftClick()) event.setCancelled(true); return; }
        if ((event.getCursor()!=null && !event.getCursor().isEmpty())
                || event.getClick()==org.bukkit.event.inventory.ClickType.NUMBER_KEY
                || event.getClick()==org.bukkit.event.inventory.ClickType.SWAP_OFFHAND) event.setCancelled(true);
    }

    @EventHandler public void onLootDrag(InventoryDragEvent event) {
        if (personalLoot.containsValue(event.getInventory())
                && event.getRawSlots().stream().anyMatch(slot->slot<event.getInventory().getSize()))
            event.setCancelled(true);
    }

    @EventHandler public void onLootQuit(PlayerQuitEvent event) { closeRewardInventory(event.getPlayer()); }
    @EventHandler public void onLootDeath(PlayerDeathEvent event) { closeRewardInventory(event.getEntity()); }
    @EventHandler(ignoreCancelled=true) public void onLootTeleport(PlayerTeleportEvent event) {
        closeRewardInventory(event.getPlayer());
    }
    private void closeRewardInventory(Player player) {
        if(personalLoot.containsValue(player.getOpenInventory().getTopInventory())) player.closeInventory();
    }

    private boolean isEmpty(@NotNull Inventory inventory) {
        return java.util.Arrays.stream(inventory.getStorageContents())
                .allMatch(item -> item == null || item.getType().isAir());
    }

    private void completeClaim(@NotNull VaultData vault, @NotNull Player player,
                               @NotNull ActiveMeteorEvent event, boolean personal) {
        plugin.getPlayerStatsStore().addLoot(player.getUniqueId());
        vault.openedBy().add(player.getUniqueId());
        final int requiredClaims = !personal || event.playerDamageMap().isEmpty()
                ? 1 : Math.min(plugin.getConfigManager().getRewardTopCount(),
                event.playerDamageMap().size());
        if (vault.openedBy().size() >= requiredClaims) vault.claimed().set(true);
        final List<VaultData> vaults = eventVaults.get(vault.eventId());
        if (vaults != null && vaults.stream().allMatch(data -> data.claimed().get())) {
            plugin.getMeteorEventManager().markLootClaimed(vault.eventId());
        }
    }

    private void grantRankingRewards(@NotNull Player player,
                                     @NotNull ActiveMeteorEvent meteorEvent) {
        final Set<UUID> granted = rankingRewardsGranted.computeIfAbsent(
                meteorEvent.eventId(), ignored -> ConcurrentHashMap.newKeySet());
        if (!granted.add(player.getUniqueId())) return;

        final int rank = damageRank(meteorEvent, player.getUniqueId());
        if (rank > plugin.getConfigManager().getRewardTopCount()) return;
        final double damage = meteorEvent.playerDamageMap()
                .getOrDefault(player.getUniqueId(), 0.0);

        for (final String raw : plugin.getConfigManager()
                .getRankingRewardItems(meteorEvent.meteorType(), rank)) {
            if (raw == null || raw.isBlank()) continue;
            final String[] parts = raw.trim().split(":", 2);
            final Material material = Material.matchMaterial(parts[0].trim());
            if (material == null || material.isAir()) {
                plugin.getLogger().warning("Invalid ranking reward item: " + raw);
                continue;
            }
            int amount = 1;
            if (parts.length == 2) {
                try {
                    amount = Math.max(1, Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException error) {
                    plugin.getLogger().warning("Invalid ranking reward amount: " + raw);
                }
            }
            final Map<Integer, ItemStack> remaining = player.getInventory()
                    .addItem(new ItemStack(material, amount));
            remaining.values().forEach(item -> player.getWorld()
                    .dropItemNaturally(player.getLocation(), item));
        }

        for (final String command : plugin.getConfigManager()
                .getRankingRewardCommands(meteorEvent.meteorType(), rank)) {
            String parsed = command
                    .replace("%player%", player.getName())
                    .replace("%rank%", String.valueOf(rank))
                    .replace("%damage%", String.format(Locale.ROOT, "%.1f", damage));
            parsed = normalizeRewardCommand(parsed);
            try {
                if (!plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), parsed)) {
                    plugin.getLogger().warning("Ranking reward command was not recognized: " + parsed);
                }
            } catch (RuntimeException error) {
                plugin.getLogger().log(Level.WARNING,
                        "Ranking reward command failed without blocking other rewards: " + parsed, error);
            }
        }
        tell(player, "vault.ranking_reward", "rank", String.valueOf(rank));
        plugin.getPlayerStatsStore().addRankingReward(player.getUniqueId());
    }

    private @NotNull String normalizeRewardCommand(@NotNull String raw) {
        String command = raw.trim();
        if (command.startsWith("/")) command = command.substring(1).trim();
        // Essentials may claim the unqualified xp command and currently throws
        // MissingResourceException in some Turkish bundles. Always invoke the
        // vanilla command namespace for deterministic reward delivery.
        if (command.equalsIgnoreCase("xp")) return "minecraft:xp";
        if (command.regionMatches(true, 0, "xp ", 0, 3)) {
            return "minecraft:xp " + command.substring(3);
        }
        if (command.equalsIgnoreCase("experience")) return "minecraft:experience";
        if (command.regionMatches(true, 0, "experience ", 0, 11)) {
            return "minecraft:experience " + command.substring(11);
        }
        return command;
    }

    /** Automatically grants configured rank rewards when combat finishes. */
    public void grantLeaderboardRewards(@NotNull ActiveMeteorEvent meteorEvent) {
        meteorEvent.playerDamageMap().entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(plugin.getConfigManager().getRewardTopCount())
                .map(Map.Entry::getKey)
                .map(plugin.getServer()::getPlayer)
                .filter(java.util.Objects::nonNull)
                .filter(Player::isOnline)
                .forEach(player -> grantRankingRewards(player, meteorEvent));
    }

    /**
     * Checks if an item is a Meteor Key.
     */
    private boolean isMeteorKey(@NotNull ItemStack item) {
        if (item.getType() != Material.TRIAL_KEY) return false;
        if (!item.hasItemMeta()) return false;
        final var meta = item.getItemMeta();
        return meta.hasCustomModelData() && meta.getCustomModelData() == 1001;
    }

    private int damageRank(@NotNull ActiveMeteorEvent event, @NotNull UUID playerId) {
        final Double ownDamage = event.playerDamageMap().get(playerId);
        if (ownDamage == null) return Integer.MAX_VALUE;
        return 1 + (int) event.playerDamageMap().values().stream()
                .filter(damage -> damage > ownDamage).count();
    }

    /**
     * Gets the vault location for a specific event.
     */
    public @Nullable Location getVaultLocation(@NotNull String eventId) {
        final List<VaultData> vaults = eventVaults.get(eventId);
        return vaults != null && !vaults.isEmpty() ? vaults.getFirst().location() : null;
    }

    // ---- Vault Data ----

    private record VaultData(
            @NotNull Location location,
            @NotNull String eventId,
            @NotNull MeteorType meteorType,
            boolean unlocked,
            @NotNull Set<UUID> openedBy,
            @NotNull AtomicBoolean claimed
    ) {
        VaultData unlock() {
            return new VaultData(location, eventId, meteorType, true, openedBy, claimed);
        }
    }

    private record LootSessionKey(@NotNull String eventId, int x, int y, int z,
                                  @NotNull UUID scopeId) {
        static LootSessionKey of(VaultData vault, UUID scopeId) {
            return new LootSessionKey(vault.eventId(), vault.location().getBlockX(),
                    vault.location().getBlockY(), vault.location().getBlockZ(), scopeId);
        }
    }

    private String formatLocation(Location loc) {
        return String.format("%.0f, %.0f, %.0f",
                loc.getX(), loc.getY(), loc.getZ());
    }
}
