package com.olmeteors.meteorevents;

import com.olmeteors.meteorevents.config.ConfigManager;
import com.olmeteors.meteorevents.display.DisplayEntityManager;
import com.olmeteors.meteorevents.event.MeteorEventManager;
import com.olmeteors.meteorevents.hazard.HazardManager;
import com.olmeteors.meteorevents.gui.AutoSetupGUI;
import com.olmeteors.meteorevents.gui.ConfigGUI;
import com.olmeteors.meteorevents.hook.FAWEHook;
import com.olmeteors.meteorevents.hook.MythicMobsHook;
import com.olmeteors.meteorevents.hook.PlaceholderAPIHook;
import com.olmeteors.meteorevents.hook.TownyHook;
import com.olmeteors.meteorevents.hook.WGHook;
import com.olmeteors.meteorevents.location.LocationFinder;
import com.olmeteors.meteorevents.loot.LootGUIEditor;
import com.olmeteors.meteorevents.loot.VaultManager;
import com.olmeteors.meteorevents.rollback.RollbackSystem;
import com.olmeteors.meteorevents.scheduler.FoliaScheduler;
import com.olmeteors.meteorevents.schematic.SchematicManager;
import com.olmeteors.meteorevents.setup.SetupCommandBlocker;
import com.olmeteors.meteorevents.setup.SetupManager;
import com.olmeteors.meteorevents.stats.PlayerStatsStore;
import com.olmeteors.meteorevents.ticket.MeteorTicketManager;
import com.olmeteors.meteorevents.api.OlMeteorAPI;
import com.olmeteors.meteorevents.api.OlMeteorAPIImpl;
import org.bukkit.plugin.ServicePriority;
import com.olmeteors.meteorevents.util.MessageUtil;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ultimate Meteor Event Plugin for Paper 1.21.1+
 * <p>
 * A comprehensive, modular plugin providing dynamic meteor impact events
 * with full Folia multi-threading support, WorldGuard integration,
 * Vault loot systems, and async schematic rollback capabilities.
 */
public final class MeteorPlugin extends JavaPlugin {

    private static MeteorPlugin instance;
    private static Logger pluginLogger;

    private ConfigManager configManager;
    private FoliaScheduler foliaScheduler;
    private SetupManager setupManager;
    private SetupCommandBlocker setupCommandBlocker;
    private LocationFinder locationFinder;
    private MeteorEventManager meteorEventManager;
    private SchematicManager schematicManager;
    private RollbackSystem rollbackSystem;
    private DisplayEntityManager displayEntityManager;
    private HazardManager hazardManager;
    private VaultManager vaultManager;
    private LootGUIEditor lootGUIEditor;
    private WGHook wgHook;
    private TownyHook townyHook;
    private FAWEHook faweHook;
    private MythicMobsHook mythicMobsHook;
    private PlaceholderAPIHook placeholderAPIHook;
    private AutoSetupGUI autoSetupGUI;
    private ConfigGUI configGUI;
    private PlayerStatsStore playerStatsStore;
    private MeteorTicketManager meteorTicketManager;

    @Override
    public void onLoad() {
        instance = this;
        pluginLogger = getLogger();
        pluginLogger.info("OlMeteor v" + getPluginMeta().getVersion() + " başlatılıyor...");
    }

    @Override
    public void onEnable() {
        final long startTime = System.currentTimeMillis();

        try {
            final PluginManager pluginManager = getServer().getPluginManager();
            final Plugin commandApi = pluginManager.getPlugin("CommandAPI");
            if (commandApi == null || !commandApi.isEnabled()) {
                getLogger().severe("Required dependency CommandAPI is not installed or enabled. "
                        + "OlMeteor will now disable safely.");
                pluginManager.disablePlugin(this);
                return;
            }

            // Initialize configuration first
            this.configManager = new ConfigManager(this);
            this.configManager.loadConfiguration();

            // Initialize scheduler wrapper
            this.foliaScheduler = new FoliaScheduler(this);

            // Initialize utility hooks
            this.faweHook = FAWEHook.create(this,
                    pluginManager.getPlugin("FastAsyncWorldEdit"));
            this.wgHook = WGHook.create(this, pluginManager.getPlugin("WorldGuard"));
            this.townyHook = TownyHook.create(this, pluginManager.getPlugin("Towny"));
            this.mythicMobsHook = MythicMobsHook.create(this,
                    pluginManager.getPlugin("MythicMobs"));
            this.placeholderAPIHook = PlaceholderAPIHook.create(this,
                    pluginManager.getPlugin("PlaceholderAPI"));

            // Initialize core systems
            this.schematicManager = new SchematicManager(this, faweHook);
            this.schematicManager.ensureDefaultSchematic();
            this.rollbackSystem = new RollbackSystem(this, faweHook);
            this.displayEntityManager = new DisplayEntityManager(this);
            this.locationFinder = new LocationFinder(this, wgHook, townyHook);
            this.hazardManager = new HazardManager(this, wgHook);
            this.vaultManager = new VaultManager(this, displayEntityManager, mythicMobsHook);
            this.lootGUIEditor = new LootGUIEditor(this, configManager);
            this.autoSetupGUI = new AutoSetupGUI(this);
            this.configGUI = new ConfigGUI(this);
            this.playerStatsStore = new PlayerStatsStore(this);
            this.meteorTicketManager = new MeteorTicketManager(this);

            // Initialize setup and command blocking
            this.setupCommandBlocker = new SetupCommandBlocker(this);
            this.setupManager = new SetupManager(this, configManager, displayEntityManager);

            // Initialize event manager (depends on all systems)
            this.meteorEventManager = new MeteorEventManager(
                    this, configManager, foliaScheduler, locationFinder,
                    schematicManager, rollbackSystem, displayEntityManager,
                    hazardManager, vaultManager, wgHook, mythicMobsHook
            );
            getServer().getServicesManager().register(OlMeteorAPI.class,
                    new OlMeteorAPIImpl(this), this, ServicePriority.Normal);

            // Register commands
            registerCommands();

            // Register listeners
            registerListeners();

            // Load active events from persistent storage
            this.meteorEventManager.loadActiveEvents();
            this.meteorEventManager.startAutomaticScheduler();

            // Check if FAWE is available and log status
            if (faweHook.isAvailable()) {
                pluginLogger.info("FastAsyncWorldEdit hook enabled - Async schematic operations available");
            } else {
                pluginLogger.warning("FastAsyncWorldEdit not found - Schematic features will be disabled");
            }

            // Log hook statuses
            logHookStatuses();

            final long elapsed = System.currentTimeMillis() - startTime;
            pluginLogger.info("OlMeteor v" + getPluginMeta().getVersion()
                    + " enabled successfully in " + elapsed + "ms");
            pluginLogger.info("Folia multi-threading support: " + isFolia());

        } catch (Exception | LinkageError error) {
            getLogger().log(Level.SEVERE,
                    "Failed to enable OlMeteor. A dependency may be missing or API-incompatible; "
                            + "the plugin will now disable safely.", error);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        safelyShutdown("active meteor events",
                this.meteorEventManager == null ? null : this.meteorEventManager::shutdownAll);
        safelyShutdown("hazards", this.hazardManager == null ? null : this.hazardManager::disableAll);
        safelyShutdown("display entities",
                this.displayEntityManager == null ? null : this.displayEntityManager::removeAllEntities);
        safelyShutdown("scheduled tasks",
                this.foliaScheduler == null ? null : this.foliaScheduler::cancelAllTasks);
        safelyShutdown("setup sessions",
                this.setupManager == null ? null : this.setupManager::exitAllSetupModes);
        safelyShutdown("PlaceholderAPI expansion",
                this.placeholderAPIHook == null ? null : this.placeholderAPIHook::shutdown);
        safelyShutdown("player statistics", this.playerStatsStore == null ? null : this.playerStatsStore::save);
        getLogger().info("OlMeteor disabled successfully");
    }

    private void safelyShutdown(String component, Runnable cleanup) {
        if (cleanup == null) {
            return;
        }
        try {
            cleanup.run();
        } catch (Exception | LinkageError error) {
            getLogger().log(Level.WARNING, "Failed to clean up " + component, error);
        }
    }

    private void registerCommands() {
        final MeteorCommand command = new MeteorCommand(this, configManager, setupManager,
                meteorEventManager, locationFinder, schematicManager, vaultManager, lootGUIEditor);
        command.register();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(this.setupCommandBlocker, this);
        getServer().getPluginManager().registerEvents(this.setupManager, this);
        getServer().getPluginManager().registerEvents(this.vaultManager, this);
        getServer().getPluginManager().registerEvents(this.lootGUIEditor, this);
        getServer().getPluginManager().registerEvents(this.meteorEventManager, this);
        getServer().getPluginManager().registerEvents(this.hazardManager, this);
        getServer().getPluginManager().registerEvents(this.autoSetupGUI, this);
        getServer().getPluginManager().registerEvents(this.configGUI, this);
        getServer().getPluginManager().registerEvents(this.meteorTicketManager, this);
    }

    private void logHookStatuses() {
        if (wgHook.isAvailable()) {
            pluginLogger.info("WorldGuard hook enabled - Dynamic region protection available");
        } else {
            pluginLogger.info("WorldGuard not found - Region protection disabled");
        }
        if (townyHook.isAvailable()) {
            pluginLogger.info("Towny hook enabled - Claim conflict detection available");
        } else {
            pluginLogger.info("Towny not found - Claim conflict detection disabled");
        }
        if (mythicMobsHook.isAvailable()) {
            pluginLogger.info("MythicMobs hook enabled - Custom boss mechanics available");
        } else {
            pluginLogger.info("MythicMobs not found - Standard mob spawning will be used");
        }
        if (placeholderAPIHook.isAvailable()) {
            pluginLogger.info("PlaceholderAPI hook enabled - Placeholder expansion registered");
        } else {
            pluginLogger.info("PlaceholderAPI not found - Placeholder features disabled");
        }
        final Plugin fancyHolograms = getServer().getPluginManager().getPlugin("FancyHolograms");
        if (fancyHolograms != null && fancyHolograms.isEnabled()) {
            pluginLogger.info("FancyHolograms detected - OlMeteor display entities use the same Paper/Folia display layer and coexist safely");
        }
    }

    /**
     * Checks if the server is running Folia.
     *
     * @return true if Folia is detected
     */
    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // ---- Static Accessors ----

    public static MeteorPlugin getInstance() {
        return instance;
    }

    public static Logger getPluginLogger() {
        return pluginLogger;
    }

    // ---- Instance Accessors ----

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public FoliaScheduler getFoliaScheduler() {
        return foliaScheduler;
    }

    public SetupManager getSetupManager() {
        return setupManager;
    }

    public SetupCommandBlocker getSetupCommandBlocker() {
        return setupCommandBlocker;
    }

    public LocationFinder getLocationFinder() {
        return locationFinder;
    }

    public MeteorEventManager getMeteorEventManager() {
        return meteorEventManager;
    }

    public SchematicManager getSchematicManager() {
        return schematicManager;
    }

    public RollbackSystem getRollbackSystem() {
        return rollbackSystem;
    }

    public DisplayEntityManager getDisplayEntityManager() {
        return displayEntityManager;
    }

    public HazardManager getHazardManager() {
        return hazardManager;
    }

    public VaultManager getVaultManager() {
        return vaultManager;
    }

    public LootGUIEditor getLootGUIEditor() {
        return lootGUIEditor;
    }

    public WGHook getWGHook() {
        return wgHook;
    }

    public TownyHook getTownyHook() {
        return townyHook;
    }

    public FAWEHook getFAWEHook() {
        return faweHook;
    }

    public MythicMobsHook getMythicMobsHook() {
        return mythicMobsHook;
    }

    public PlaceholderAPIHook getPlaceholderAPIHook() {
        return placeholderAPIHook;
    }

    public AutoSetupGUI getAutoSetupGUI() { return autoSetupGUI; }
    public ConfigGUI getConfigGUI() { return configGUI; }
    public PlayerStatsStore getPlayerStatsStore() { return playerStatsStore; }
    public MeteorTicketManager getMeteorTicketManager() { return meteorTicketManager; }
}
