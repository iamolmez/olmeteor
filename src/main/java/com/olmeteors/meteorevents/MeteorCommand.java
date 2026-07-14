package com.olmeteors.meteorevents;

import com.olmeteors.meteorevents.config.ConfigManager;
import com.olmeteors.meteorevents.event.MeteorEventManager;
import com.olmeteors.meteorevents.event.MeteorType;
import com.olmeteors.meteorevents.event.MeteorFallMode;
import com.olmeteors.meteorevents.location.LocationFinder;
import com.olmeteors.meteorevents.loot.LootGUIEditor;
import com.olmeteors.meteorevents.loot.VaultManager;
import com.olmeteors.meteorevents.schematic.SchematicManager;
import com.olmeteors.meteorevents.setup.SetupManager;
import com.olmeteors.meteorevents.util.MessageUtil;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.WorldArgument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

/**
 * Single command tree for /olmeteor using the CommandAPI framework.
 * <p>
 * All user-facing messages are read from {@code config.yml → messages.command.*}
 * via {@link ConfigManager#getMessage(String, String...)}. Placeholders such as
 * {@code %type%}, {@code %eventId%}, {@code %world%} are replaced at runtime.
 */
public final class MeteorCommand {

    /**
     * Explicit ASCII namespace. CommandAPI's external-plugin default is derived
     * from "CommandAPI" and may become "commandapı" under a Turkish JVM locale.
     */
    static final String COMMAND_NAMESPACE = "olmeteor";

    private final MeteorPlugin plugin;
    private final ConfigManager config;
    private final SetupManager setupManager;
    private final MeteorEventManager eventManager;
    private final LocationFinder locationFinder;
    private final SchematicManager schematicManager;
    private final VaultManager vaultManager;
    private final LootGUIEditor lootGUIEditor;

    private static final String[] METEOR_TYPE_SUGGESTIONS =
            Arrays.stream(MeteorType.values()).map(Enum::name).toArray(String[]::new);
    private static final String[] AUTO_METEOR_SUGGESTIONS;
    private static final String[] LOOT_BLOCK_SUGGESTIONS = Arrays.stream(Material.values())
            .filter(Material::isBlock).filter(Material::isItem).filter(material -> !material.isAir())
            .map(Material::name).toArray(String[]::new);
    static {
        AUTO_METEOR_SUGGESTIONS = Arrays.copyOf(METEOR_TYPE_SUGGESTIONS,
                METEOR_TYPE_SUGGESTIONS.length + 3);
        AUTO_METEOR_SUGGESTIONS[METEOR_TYPE_SUGGESTIONS.length] = "rastgele";
        AUTO_METEOR_SUGGESTIONS[METEOR_TYPE_SUGGESTIONS.length + 1] = "small,medium,large";
        AUTO_METEOR_SUGGESTIONS[METEOR_TYPE_SUGGESTIONS.length + 2] = "epic,legendary";
    }
    private static final DateTimeFormatter AUTO_TIME_FORMAT = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.of("Europe/Istanbul"));

    @SuppressWarnings("unchecked")
    private static final ArgumentSuggestions<org.bukkit.command.CommandSender> TYPE_SUGGESTIONS =
            (ArgumentSuggestions<org.bukkit.command.CommandSender>) (Object) ArgumentSuggestions.strings(METEOR_TYPE_SUGGESTIONS);

    public MeteorCommand(MeteorPlugin plugin, ConfigManager configManager,
                         SetupManager setupManager, MeteorEventManager eventManager,
                         LocationFinder locationFinder, SchematicManager schematicManager,
                         VaultManager vaultManager, LootGUIEditor lootGUIEditor) {
        this.plugin = plugin;
        this.config = configManager;
        this.setupManager = setupManager;
        this.eventManager = eventManager;
        this.locationFinder = locationFinder;
        this.schematicManager = schematicManager;
        this.vaultManager = vaultManager;
        this.lootGUIEditor = lootGUIEditor;
    }

    // ── Shorthand for config message send ──────────────────────
    private void tell(@NotNull org.bukkit.command.CommandSender sender,
                      @NotNull String path,
                      @NotNull String @NotNull ... placeholders) {
        config.sendMessage(sender, "command." + path, placeholders);
    }

    /**
     * Registers the full command tree with the CommandAPI.
     */
    public void register() {

        // ── /meteor help ────────────────────────────────────────────────
        final var helpCmd = new CommandAPICommand("help")
                .withPermission(CommandPermission.NONE)
                .executes((sender, args) -> {
                    showConfiguredHelp(sender);
                });

        final dev.jorel.commandapi.executors.CommandExecutor setupFallback = (sender, args) ->
                tell(sender, "player_only");
        // ── /meteor setup <type> ────────────────────────────────────────
        final var setupCmd = new CommandAPICommand("setup")
                .withPermission(CommandPermission.fromString("olmeteor.setup"))
                .withArguments(new StringArgument("type")
                        .replaceSuggestions(TYPE_SUGGESTIONS))
                .executesPlayer((player, args) -> {
                    final String raw = (String) args.get("type");
                    try {
                        final MeteorType type = MeteorType.fromString(raw);
                        setupManager.showSetupChoice(player, type);
                    } catch (IllegalArgumentException e) {
                        tell(player, "setup.invalid_type", "type", raw);
                    }
                })
                .executes(setupFallback);

        final var setupNewCmd = new CommandAPICommand("setupnew")
                .withPermission(CommandPermission.fromString("olmeteor.setup"))
                .withArguments(new StringArgument("type").replaceSuggestions(TYPE_SUGGESTIONS))
                .executesPlayer((dev.jorel.commandapi.executors.PlayerCommandExecutor) (player, args) -> {
                    final String raw = (String) args.get("type");
                    try {
                        setupManager.enterSetupMode(player, MeteorType.fromString(raw));
                    } catch (IllegalArgumentException error) {
                        tell(player, "setup.invalid_type", "type", raw);
                    }
                })
                .executes(setupFallback);

        final var setupFinishCmd = new CommandAPICommand("setupfinish")
                .withPermission(CommandPermission.fromString("olmeteor.setup"))
                .withArguments(new StringArgument("secim").replaceSuggestions(
                        ArgumentSuggestions.strings("sil", "birak")))
                .executesPlayer((dev.jorel.commandapi.executors.PlayerCommandExecutor) (player, args) -> {
                    final String choice = ((String) args.get("secim")).toLowerCase(java.util.Locale.ROOT);
                    if (!choice.equals("sil") && !choice.equals("birak")) {
                        MessageUtil.sendMessage(player, "&cSeçim: sil veya birak olmalı.");
                        return;
                    }
                    setupManager.finishSetup(player, choice.equals("sil"));
                })
                .executes(setupFallback);

        final var presetCmd = new CommandAPICommand("preset")
                .withPermission(CommandPermission.fromString("olmeteor.auto"))
                .withArguments(new StringArgument("preset").replaceSuggestions(
                        ArgumentSuggestions.strings(info -> config.getLocationPresetNames()
                                .toArray(String[]::new))))
                .withOptionalArguments(new IntegerArgument("minY", -64, 320),
                        new IntegerArgument("maxY", -64, 320))
                .executes((dev.jorel.commandapi.executors.CommandExecutor) (sender, args) ->
                        executeSafely(sender, () -> {
                            final String name = (String) args.get("preset");
                            final Integer minY = (Integer) args.getOptional("minY").orElse(null);
                            final Integer maxY = (Integer) args.getOptional("maxY").orElse(null);
                            if ((minY == null) != (maxY == null)) {
                                MessageUtil.sendMessage(sender, "&cY aralığı için minY ve maxY birlikte girilmeli.");
                            } else if (config.setLocationPreset(name, minY, maxY)) {
                                MessageUtil.sendMessage(sender, "&aKonum preseti ayarlandı: &e" + name
                                        + (minY == null ? "" : " &7Y: " + minY + ".." + maxY));
                            } else {
                                MessageUtil.sendMessage(sender, "&cPreset bulunamadı veya Y aralığı geçersiz.");
                            }
                        }));

        final var useSchematicCmd = new CommandAPICommand("useschematic")
                .withPermission(CommandPermission.fromString("olmeteor.setup"))
                .withArguments(new StringArgument("type").replaceSuggestions(TYPE_SUGGESTIONS))
                .withArguments(new StringArgument("name").replaceSuggestions(ArgumentSuggestions.strings(
                        info -> schematicManager.listSchematics().toArray(String[]::new))))
                .executesPlayer((dev.jorel.commandapi.executors.PlayerCommandExecutor) (player, args) -> {
                    final String raw = (String) args.get("type");
                    try {
                        setupManager.useExistingSchematic(player, MeteorType.fromString(raw),
                                (String) args.get("name"));
                    } catch (IllegalArgumentException error) {
                        tell(player, "setup.invalid_type", "type", raw);
                    }
                })
                .executes(setupFallback);

        final var editSchematicCmd = new CommandAPICommand("editschematic")
                .withPermission(CommandPermission.fromString("olmeteor.setup"))
                .withArguments(new StringArgument("type").replaceSuggestions(TYPE_SUGGESTIONS))
                .withArguments(new StringArgument("name").replaceSuggestions(ArgumentSuggestions.strings(
                        info -> schematicManager.listSchematics().toArray(String[]::new))))
                .executesPlayer((dev.jorel.commandapi.executors.PlayerCommandExecutor) (player, args) -> {
                    final String raw = (String) args.get("type");
                    try {
                        setupManager.editExistingSchematic(player, MeteorType.fromString(raw),
                                (String) args.get("name"));
                    } catch (IllegalArgumentException error) {
                        tell(player, "setup.invalid_type", "type", raw);
                    }
                })
                .executes(setupFallback);

        final var selectMobCmd = new CommandAPICommand("selectmob")
                .withPermission(CommandPermission.fromString("olmeteor.setup"))
                .withArguments(new StringArgument("mob").replaceSuggestions(ArgumentSuggestions.strings(
                        info -> plugin.getMythicMobsHook().getMobIds().toArray(String[]::new))))
                .executesPlayer((dev.jorel.commandapi.executors.PlayerCommandExecutor) (player, args) ->
                        setupManager.selectMythicMob(player, (String) args.get("mob")))
                .executes(setupFallback);

        final var setTextCmd = new CommandAPICommand("settext")
                .withPermission(CommandPermission.fromString("olmeteor.setup"))
                .withArguments(new GreedyStringArgument("text"))
                .executesPlayer((dev.jorel.commandapi.executors.PlayerCommandExecutor) (player, args) ->
                        setupManager.setHologramText(player, (String) args.get("text")))
                .executes(setupFallback);

        final var lootCmd = new CommandAPICommand("loot")
                .withPermission(CommandPermission.fromString("olmeteor.setup"))
                .withArguments(new StringArgument("type").replaceSuggestions(TYPE_SUGGESTIONS))
                .executesPlayer((dev.jorel.commandapi.executors.PlayerCommandExecutor) (player, args) -> {
                    try {
                        lootGUIEditor.openEditor(player,
                                MeteorType.fromString((String) args.get("type")));
                    } catch (IllegalArgumentException error) {
                        tell(player, "setup.invalid_type", "type", (String) args.get("type"));
                    }
                })
                .executes(setupFallback);

        // ── /meteor schematic <name> ──────────────────────────────────
        final var schematicCmd = new CommandAPICommand("schematic")
                .withPermission(CommandPermission.fromString("olmeteor.setup"))
                .withArguments(new StringArgument("name"))
                .executesPlayer((dev.jorel.commandapi.executors.PlayerCommandExecutor) (player, args) ->
                        setupManager.saveSelectedSchematic(player, (String) args.get("name")))
                .executes(setupFallback);

        // ── /meteor start <type> [world] [radius] ───────────────────────
        final var activeEventIdSuggestions = ArgumentSuggestions.strings(
                (dev.jorel.commandapi.SuggestionInfo<org.bukkit.command.CommandSender> info) -> MeteorPlugin.getInstance()
                        .getMeteorEventManager().getActiveEvents().keySet()
                        .toArray(new String[0]));

        final dev.jorel.commandapi.executors.CommandExecutor startExecutor = this::executeStart;
        final var startCmd = new CommandAPICommand("start")
                .withPermission(CommandPermission.fromString("olmeteor.start"))
                .withArguments(new StringArgument("type")
                        .replaceSuggestions(TYPE_SUGGESTIONS))
                .withOptionalArguments(
                        new WorldArgument("world"),
                        new IntegerArgument("radius", 10, 200))
                .executes(startExecutor);

        // ── /meteor spawnat <type> <location> [world] ──────────────────
        final var spawnatCmd = new CommandAPICommand("spawnat")
                .withPermission(CommandPermission.fromString("olmeteor.start"))
                .withArguments(new StringArgument("type")
                        .replaceSuggestions(TYPE_SUGGESTIONS))
                .withArguments(new LocationArgument("location", LocationType.BLOCK_POSITION, false))
                .withOptionalArguments(
                        new WorldArgument("world"),
                        new StringArgument("mode").replaceSuggestions(
                                ArgumentSuggestions.strings("instant", "normal", "slow")))
                .executes((dev.jorel.commandapi.executors.CommandExecutor) (sender, args) ->
                        executeSafely(sender, () -> {
                    final String raw = (String) args.get("type");
                    try {
                        final MeteorType type = MeteorType.fromString(raw);
                        final Location loc = (Location) args.get("location");
                        final World world = (World) args.getOptional("world")
                                .orElseGet(() -> sender instanceof Player p ? p.getWorld()
                                        : plugin.getServer().getWorlds().getFirst());
                        loc.setWorld(world);
                        final String modeName = (String) args.getOptional("mode").orElse("instant");
                        eventManager.startEventAt(type, loc, sender,
                                MeteorFallMode.parse(modeName));
                    } catch (IllegalArgumentException e) {
                        tell(sender, "start.invalid_type", "type", raw);
                    }
                }));

        // ── /meteor stop <eventId> ─────────────────────────────────────
        final var stopCmd = new CommandAPICommand("stop")
                .withPermission(CommandPermission.fromString("olmeteor.stop"))
                .withArguments(new StringArgument("eventId")
                        .replaceSuggestions(activeEventIdSuggestions))
                .executes((sender, args) -> {
                    final String eventId = (String) args.get("eventId");
                    eventManager.stopEvent(eventId, sender);
                });

        // ── /meteor cancel <eventId> ───────────────────────────────────
        final var cancelCmd = new CommandAPICommand("cancel")
                .withPermission(CommandPermission.fromString("olmeteor.cancel"))
                .withArguments(new StringArgument("eventId")
                        .replaceSuggestions(activeEventIdSuggestions))
                .executes((dev.jorel.commandapi.executors.CommandExecutor) (sender, args) -> {
                    final String eventId = (String) args.get("eventId");
                    eventManager.cancelEvent(eventId, sender);
                });

        // ── /meteor reload ─────────────────────────────────────────────
        final var reloadCmd = new CommandAPICommand("reload")
                .withPermission(CommandPermission.fromString("olmeteor.reload"))
                .executes((dev.jorel.commandapi.executors.CommandExecutor) (sender, args) -> {
                    executeSafely(sender, () -> {
                        config.loadConfiguration();
                        eventManager.startAutomaticScheduler();
                        tell(sender, "reload.success");
                    });
                });

        final dev.jorel.commandapi.executors.CommandExecutor wandFallback = (sender, args) ->
                tell(sender, "player_only");
        // ── /meteor wand ───────────────────────────────────────────────
        final var wandCmd = new CommandAPICommand("wand")
                .withPermission(CommandPermission.fromString("olmeteor.wand"))
                .executesPlayer((player, args) -> {
                    setupManager.giveWandToPlayer(player);
                    tell(player, "wand.success");
                })
                .executes(wandFallback);

        // ── /meteor list ───────────────────────────────────────────────
        final var listCmd = new CommandAPICommand("list")
                .withPermission(CommandPermission.fromString("olmeteor.list"))
                .executes((sender, args) -> {
                    final var events = eventManager.getActiveEvents();
                    if (events.isEmpty()) {
                        tell(sender, "list.empty");
                        return;
                    }
                    tell(sender, "list.title");
                    events.forEach((id, evt) -> {
                        final var loc = evt.center();
                        config.sendMessage(sender, "command.list.entry",
                                "eventId", id,
                                "type", evt.meteorType().name(),
                                "world", loc.getWorld().getName(),
                                "location", formatLocation(loc));
                    });
                });

        // ── /meteor info <eventId> [target] ────────────────────────────
        final var infoCmd = new CommandAPICommand("info")
                .withPermission(CommandPermission.fromString("olmeteor.info"))
                .withArguments(new StringArgument("eventId")
                        .replaceSuggestions(activeEventIdSuggestions))
                .withOptionalArguments(new EntitySelectorArgument.OnePlayer("target"))
                .executes((sender, args) -> {
                    final String eventId = (String) args.get("eventId");
                    final var evt = eventManager.getEvent(eventId);
                    if (evt == null) {
                        tell(sender, "info.no_event", "eventId", eventId);
                        return;
                    }

                    tell(sender, "info.title");
                    tell(sender, "info.id", "eventId", evt.eventId());
                    tell(sender, "info.type", "type", evt.meteorType().name());
                    tell(sender, "info.phase", "phase", evt.phase().name());
                    tell(sender, "info.world", "world", evt.center().getWorld().getName());
                    tell(sender, "info.location", "location", formatLocation(evt.center()));
                    tell(sender, "info.radius", "radius", String.valueOf(
                            config.getTypeConfig(evt.meteorType()).impactRadius()));
                    tell(sender, "info.radius_shape", "shape",
                            config.getRadiusShape(evt.meteorType()).name());
                    tell(sender, "info.schematic", "schematic",
                            evt.schematicName() != null ? evt.schematicName() : "None");

                    final Player target = (Player) args.getOptional("target").orElse(null);
                    if (target != null && target.isOnline()) {
                        final double dist = target.getLocation().distance(evt.center());
                        tell(sender, "info.distance",
                                "player", target.getName(),
                                "distance", String.format("%.1f", dist));
                    }
                });

        final var autoCmd = new CommandAPICommand("auto")
                .withPermission(CommandPermission.fromString("olmeteor.auto"))
                .withSubcommand(new CommandAPICommand("ayarla").withAliases("setup")
                        .withArguments(new StringArgument("meteor").replaceSuggestions(
                                ArgumentSuggestions.strings(AUTO_METEOR_SUGGESTIONS)))
                        .withArguments(new WorldArgument("world"))
                        .withArguments(new StringArgument("preset").replaceSuggestions(
                                ArgumentSuggestions.strings(info -> config.getLocationPresetNames()
                                        .toArray(String[]::new))))
                        .withArguments(new StringArgument("ganimetBloku").replaceSuggestions(
                                ArgumentSuggestions.strings(LOOT_BLOCK_SUGGESTIONS)))
                        .withArguments(
                                new IntegerArgument("minDakika", 1, 10080),
                                new IntegerArgument("maxDakika", 1, 10080),
                                new IntegerArgument("minUzaklik", 0, 30000000),
                                new IntegerArgument("maxUzaklik", 1, 30000000))
                        .withOptionalArguments(
                                new IntegerArgument("minY", -2048, 2048),
                                new IntegerArgument("maxY", -2048, 2048))
                        .executes((dev.jorel.commandapi.executors.CommandExecutor) (sender, args) ->
                                executeSafely(sender, () -> {
                                    final String rawType = (String) args.get("meteor");
                                    final World world = (World) args.get("world");
                                    final String preset = (String) args.get("preset");
                                    final String rawLootBlock = (String) args.get("ganimetBloku");
                                    final List<Material> lootBlocks = Arrays.stream(rawLootBlock.split("[,;+]"))
                                            .map(String::trim).map(Material::matchMaterial).toList();
                                    if (lootBlocks.isEmpty() || lootBlocks.stream().anyMatch(material ->
                                            material == null || material.isAir()
                                                    || !material.isBlock() || !material.isItem())) {
                                        MessageUtil.sendMessage(sender, "&cGeçersiz ganimet bloğu: &e" + rawLootBlock);
                                        return;
                                    }
                                    final int minMinutes = (int) args.get("minDakika");
                                    final int maxMinutes = (int) args.get("maxDakika");
                                    final int minDistance = (int) args.get("minUzaklik");
                                    final int maxDistance = (int) args.get("maxUzaklik");
                                    final Integer minY = (Integer) args.getOptional("minY").orElse(null);
                                    final Integer maxY = (Integer) args.getOptional("maxY").orElse(null);
                                    final boolean airPreset = preset.equalsIgnoreCase("air");
                                    if (airPreset && (minY == null || maxY == null)) {
                                        MessageUtil.sendMessage(sender, "&eAir yüzeyi için yükseklik gerekli: "
                                                + "&f... <minY> <maxY> &7(örnek: &f100 220&7)");
                                        return;
                                    }
                                    if ((minY == null) != (maxY == null)
                                            || (minY != null && minY >= maxY)
                                            || (minY != null && (minY < world.getMinHeight()
                                            || maxY >= world.getMaxHeight()))) {
                                        MessageUtil.sendMessage(sender, "&cY aralığı geçersiz. Dünya aralığı: &e"
                                                + world.getMinHeight() + " - " + (world.getMaxHeight() - 1));
                                        return;
                                    }
                                    final boolean random = rawType.equalsIgnoreCase("rastgele")
                                            || rawType.equalsIgnoreCase("random");
                                    final List<MeteorType> selectedTypes = new java.util.ArrayList<>();
                                    try {
                                        if (!random) {
                                            for (final String value : rawType.split("[,;+]")) {
                                                final MeteorType parsed = MeteorType.fromString(value.trim());
                                                if (!selectedTypes.contains(parsed)) selectedTypes.add(parsed);
                                            }
                                        }
                                    } catch (IllegalArgumentException error) {
                                        MessageUtil.sendMessage(sender, "&cGeçersiz meteor tipi: &e" + rawType);
                                        return;
                                    }
                                    if (!random && selectedTypes.isEmpty()) {
                                        MessageUtil.sendMessage(sender, "&cEn az bir meteor tipi seçmelisiniz.");
                                        return;
                                    }
                                    final int targetCount = random ? MeteorType.values().length : selectedTypes.size();
                                    if (lootBlocks.size() != 1 && lootBlocks.size() != targetCount) {
                                        MessageUtil.sendMessage(sender, "&cBir ganimet bloğu yazın veya her meteor için "
                                                + "sırayla bir blok girin. Örnek: &eCHEST,BARREL");
                                        return;
                                    }
                                    if (config.setAutomaticRule(selectedTypes, world.getName(), preset,
                                            lootBlocks, minMinutes, maxMinutes, minDistance, maxDistance,
                                            minY, maxY)) {
                                        eventManager.startAutomaticScheduler();
                                        MessageUtil.sendMessage(sender, "&aAuto tamamen ayarlandı: &6"
                                                + (random ? "Tüm meteorlardan rastgele"
                                                : selectedTypes.stream().map(config::getMeteorTypeName)
                                                .collect(java.util.stream.Collectors.joining(", ")))
                                                + " &8• &b" + world.getName() + " &8• &e" + preset
                                                + " &8• &d" + lootBlocks.stream().map(Material::name)
                                                .collect(java.util.stream.Collectors.joining(","))
                                                + "\n&7Süre: &f" + minMinutes + "-" + maxMinutes
                                                + " dk &8• &7Uzaklık: &f" + minDistance + "-"
                                                + maxDistance + " blok"
                                                + (minY == null ? "" : " &8• &7Y: &f" + minY + "-" + maxY));
                                    } else {
                                        MessageUtil.sendMessage(sender, "&cAuto ayarlanamadı. Minimum değerler maksimumdan büyük olamaz.");
                                    }
                                })))
                .withSubcommand(new CommandAPICommand("ac").withAliases("on")
                        .executes((dev.jorel.commandapi.executors.CommandExecutor) (sender, args) -> executeSafely(sender, () -> {
                            if (eventManager.setAutomaticEventsEnabled(true)) {
                                MessageUtil.sendMessage(sender, "&aOtomatik meteorlar açıldı. Yeni zaman planlandı.");
                            } else {
                                MessageUtil.sendMessage(sender, "&cOtomatik meteor ayarı kaydedilemedi.");
                            }
                        })))
                .withSubcommand(new CommandAPICommand("kapat").withAliases("off")
                        .executes((dev.jorel.commandapi.executors.CommandExecutor) (sender, args) -> executeSafely(sender, () -> {
                            if (eventManager.setAutomaticEventsEnabled(false)) {
                                MessageUtil.sendMessage(sender, "&eOtomatik meteorlar kapatıldı.");
                            } else {
                                MessageUtil.sendMessage(sender, "&cOtomatik meteor ayarı kaydedilemedi.");
                            }
                        })))
                .withSubcommand(new CommandAPICommand("durum").withAliases("status")
                        .executes((dev.jorel.commandapi.executors.CommandExecutor) (sender, args) -> showAutomaticStatus(sender)))
                .withSubcommand(new CommandAPICommand("simdi").withAliases("now")
                        .executes((dev.jorel.commandapi.executors.CommandExecutor) (sender, args) -> executeSafely(sender, () -> {
                            eventManager.triggerAutomaticEventNow();
                            MessageUtil.sendMessage(sender, "&aOtomatik meteor konumu şimdi aranıyor.");
                        })))
                .executes((dev.jorel.commandapi.executors.CommandExecutor) (sender, args) -> {
                    if (sender instanceof Player player) plugin.getAutoSetupGUI().open(player);
                    else showAutomaticStatus(sender);
                });

        final var debugCmd = new CommandAPICommand("debug").withAliases("kontrol")
                .withPermission(CommandPermission.fromString("olmeteor.admin"))
                .executes((dev.jorel.commandapi.executors.CommandExecutor) (sender, args) -> {
                    MessageUtil.sendMessage(sender, "&6&lOlMeteor Tanılama &7v"
                            + plugin.getPluginMeta().getVersion());
                    MessageUtil.sendMessage(sender, "&7Sunucu: &f"
                            + (MeteorPlugin.isFolia() ? "Folia" : "Paper")
                            + " &8• &7Aktif event: &f" + eventManager.getActiveEvents().size());
                    MessageUtil.sendMessage(sender, "&7FAWE/WorldEdit: "
                            + state(plugin.getFAWEHook().isAvailable()) + " &8• &7WorldGuard: "
                            + state(plugin.getWGHook().isAvailable()) + " &8• &7Towny: "
                            + state(plugin.getTownyHook().isAvailable()));
                    MessageUtil.sendMessage(sender, "&7MythicMobs: "
                            + state(plugin.getMythicMobsHook().isAvailable())
                            + " &8• &7PlaceholderAPI: "
                            + state(plugin.getPlaceholderAPIHook().isAvailable()));
                    MessageUtil.sendMessage(sender, "&7Bekleyen crash kurtarma: &f"
                            + plugin.getRollbackSystem().getPendingRecoveryCount()
                            + " &8(" + (config.isCrashRecoveryEnabled() ? "&aAçık" : "&cKapalı") + "&8)"
                            + " &8• &7Şematik: &f" + schematicManager.listSchematics().size());
                    MessageUtil.sendMessage(sender, "&7Claim koruması: Towny=&f"
                            + config.isTownyRequireWilderness() + " &7WG=&f"
                            + config.isWorldGuardCheckClaims());
                });

        final var previewCmd = new CommandAPICommand("preview").withAliases("onizleme")
                .withPermission(CommandPermission.fromString("olmeteor.setup"))
                .withArguments(new StringArgument("type").replaceSuggestions(TYPE_SUGGESTIONS))
                .executesPlayer((dev.jorel.commandapi.executors.PlayerCommandExecutor) (player, args) -> executeSafely(player, () -> {
                    try { previewMeteor(player, MeteorType.fromString((String) args.get("type"))); }
                    catch (IllegalArgumentException error) {
                        MessageUtil.sendMessage(player, "&cGeçersiz meteor tipi.");
                    }
                }));

        final var statsCmd = new CommandAPICommand("stats").withAliases("istatistik")
                .withPermission(CommandPermission.fromString("olmeteor.info"))
                .withOptionalArguments(new EntitySelectorArgument.OnePlayer("target"))
                .executes((dev.jorel.commandapi.executors.CommandExecutor) (sender, args) -> {
                    final Player target = (Player) args.getOptional("target")
                            .orElse(sender instanceof Player player ? player : null);
                    if (target == null) { MessageUtil.sendMessage(sender, "&cKonsolda oyuncu seçmelisiniz."); return; }
                    final var stats = plugin.getPlayerStatsStore().get(target.getUniqueId());
                    MessageUtil.sendMessage(sender, "&6&l" + target.getName() + " &eMeteor İstatistikleri");
                    MessageUtil.sendMessage(sender, "&7Toplam hasar: &f" + String.format(java.util.Locale.ROOT,"%.1f",stats.damage())
                            + " &8• &7Mob: &f" + stats.kills() + " &8• &7Ganimet: &f" + stats.lootClaims()
                            + " &8• &7Sıralama ödülü: &f" + stats.rankingRewards());
                });

        final var ticketCmd = new CommandAPICommand("ticket").withAliases("bilet")
                .withPermission(CommandPermission.fromString("olmeteor.admin"))
                .withArguments(new EntitySelectorArgument.OnePlayer("target"))
                .withArguments(new StringArgument("type").replaceSuggestions(TYPE_SUGGESTIONS))
                .withOptionalArguments(new IntegerArgument("amount",1,64))
                .executes((dev.jorel.commandapi.executors.CommandExecutor) (sender,args) -> executeSafely(sender,()->{
                    final Player target=(Player)args.get("target"); final int amount=(int)args.getOptional("amount").orElse(1);
                    try {
                        final MeteorType type=MeteorType.fromString((String)args.get("type"));
                        final var remains=target.getInventory().addItem(plugin.getMeteorTicketManager().create(type,amount));
                        remains.values().forEach(item->target.getWorld().dropItemNaturally(target.getLocation(),item));
                        MessageUtil.sendMessage(sender,"&a"+target.getName()+" oyuncusuna &e"+amount+" &ameteor bileti verildi.");
                    } catch(IllegalArgumentException error) { MessageUtil.sendMessage(sender,"&cGeçersiz meteor tipi."); }
                }));

        final var historyCmd = new CommandAPICommand("history").withAliases("gecmis")
                .withPermission(CommandPermission.fromString("olmeteor.history"))
                .withOptionalArguments(new IntegerArgument("limit", 1, 50))
                .executes((dev.jorel.commandapi.executors.CommandExecutor) (sender, args) -> executeSafely(sender, () -> {
                    final int limit = (int) args.getOptional("limit").orElse(10);
                    final var history = eventManager.getRecentHistory(limit);
                    MessageUtil.sendMessage(sender, "&6&lMeteor Geçmişi &7(Son " + history.size() + ")");
                    if (history.isEmpty()) {
                        MessageUtil.sendMessage(sender, "&7Henüz kaydedilmiş meteor çarpması yok.");
                        return;
                    }
                    history.forEach(entry -> MessageUtil.sendMessage(sender,
                            "&e" + entry.formattedTime() + " &8| &f" + entry.type()
                                    + " &8| &b" + entry.world() + " &7" + entry.x() + ", "
                                    + entry.y() + ", " + entry.z() + " &8| "
                                    + (entry.automatic() ? "&dOtomatik" : "&aManuel")
                                    + " &8| &7" + entry.result()));
                }));

        // ── Root command tree ──────────────────────────────────────────
        final dev.jorel.commandapi.executors.CommandExecutor rootExecutor = this::executeRoot;
        new CommandAPICommand("olmeteor")
                .withSubcommand(helpCmd)
                .withSubcommand(setupCmd)
                .withSubcommand(setupNewCmd)
                .withSubcommand(setupFinishCmd)
                .withSubcommand(useSchematicCmd)
                .withSubcommand(editSchematicCmd)
                .withSubcommand(selectMobCmd)
                .withSubcommand(setTextCmd)
                .withSubcommand(lootCmd)
                .withSubcommand(schematicCmd)
                .withSubcommand(startCmd)
                .withSubcommand(spawnatCmd)
                .withSubcommand(stopCmd)
                .withSubcommand(cancelCmd)
                .withSubcommand(reloadCmd)
                .withSubcommand(wandCmd)
                .withSubcommand(listCmd)
                .withSubcommand(infoCmd)
                .withSubcommand(autoCmd)
                .withSubcommand(debugCmd)
                .withSubcommand(previewCmd)
                .withSubcommand(statsCmd)
                .withSubcommand(ticketCmd)
                .withSubcommand(historyCmd)
                .withSubcommand(presetCmd)
                .withSubcommand(new CommandAPICommand("ayarlar").withAliases("config","settings")
                        .withPermission(CommandPermission.fromString("olmeteor.admin"))
                        .executes((dev.jorel.commandapi.executors.CommandExecutor) (sender, args) -> {
                            if (sender instanceof Player player) {
                                plugin.getConfigGUI().open(player);
                            } else {
                                MessageUtil.sendMessage(sender, "&cConfig ayarları yalnızca oyun içinden açılabilir.");
                            }
                        }))
                .executes(rootExecutor)
                .register(COMMAND_NAMESPACE);

        plugin.getLogger().info("CommandAPI command tree registered with config-based messages");
    }

    // ─────────────────────────────────────────────────────────────────
    //  Utility
    // ─────────────────────────────────────────────────────────────────

    private static @NotNull String formatLocation(@NotNull Location loc) {
        return String.format("X: %d  Y: %d  Z: %d",
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private static String state(boolean available) {
        return available ? "&aÇalışıyor" : "&cYok/Kapalı";
    }

    private void previewMeteor(@NotNull Player player, @NotNull MeteorType type) {
        final Location center = player.getLocation().toCenterLocation();
        final int radius = config.getTypeConfig(type).impactRadius();
        final var radiusShape = config.getRadiusShape(type);
        final java.util.concurrent.atomic.AtomicReference<com.olmeteors.meteorevents.scheduler.FoliaScheduler.ScheduledTask>
                reference = new java.util.concurrent.atomic.AtomicReference<>();
        reference.set(plugin.getFoliaScheduler().runRepeatingForEntity(player, () -> {
            final World world = center.getWorld(); if (world == null) return;
            for (int angle=0; angle<360; angle+=10) {
                final double rad=Math.toRadians(angle);
                final double edge=radiusShape.boundaryDistance(rad,radius);
                player.spawnParticle(org.bukkit.Particle.END_ROD, center.getX()+Math.cos(rad)*edge,
                        center.getY()+1, center.getZ()+Math.sin(rad)*edge, 1,0,0,0,0);
            }
            config.getMobSpawnOffsets(type).forEach(offset -> player.spawnParticle(
                    org.bukkit.Particle.FLAME, center.clone().add(offset).add(0,1,0), 4, .15,.4,.15,0));
            config.getChestOffsets(type).forEach(offset -> player.spawnParticle(
                    org.bukkit.Particle.HAPPY_VILLAGER, center.clone().add(offset).add(0,1,0), 6,.2,.3,.2,0));
        }, 1L, 10L));
        plugin.getFoliaScheduler().runLaterAtLocation(center, () -> {
            final var task=reference.get(); if(task!=null) plugin.getFoliaScheduler().cancelTask(task);
        }, 300L);
        MessageUtil.sendMessage(player, "&a15 saniyelik önizleme başladı. &7Şekil: &e"
                + radiusShape.name() + " &8• &fEnd Rod=sınır, Alev=mob, Yeşil=ganimet.");
    }

    private void showAutomaticStatus(@NotNull org.bukkit.command.CommandSender sender) {
        final boolean enabled = eventManager.isAutomaticEventsEnabled();
        MessageUtil.sendMessage(sender, enabled
                ? "&aOtomatik meteor sistemi açık." : "&eOtomatik meteor sistemi kapalı.");
        final long next = eventManager.getNextAutomaticAtMillis();
        if (next > System.currentTimeMillis()) {
            final long minutes = Math.max(1L, (next - System.currentTimeMillis() + 59_999L) / 60_000L);
            MessageUtil.sendMessage(sender, "&7Sonraki: &e" + AUTO_TIME_FORMAT.format(Instant.ofEpochMilli(next))
                    + " &7(yaklaşık " + minutes + " dakika)");
        } else {
            MessageUtil.sendMessage(sender, "&7Planlanmış otomatik meteor yok.");
        }
        final var worlds = config.getAutomaticWorlds();
        final var types = config.getAutomaticTypes();
        MessageUtil.sendMessage(sender, "&7Meteor seçimi: &6"
                + (types.isEmpty() ? "Rastgele" : String.join(", ", types)));
        final var statusTypes = types.isEmpty() ? List.of(MeteorType.values()) : types.stream()
                .map(raw -> {
                    try { return MeteorType.fromString(raw); }
                    catch (IllegalArgumentException ignored) { return null; }
                }).filter(java.util.Objects::nonNull).toList();
        MessageUtil.sendMessage(sender, "&7Ganimet blokları: &d" + statusTypes.stream()
                .map(type -> type.name().toLowerCase() + "=" + config.getLootBlockMaterial(type).name())
                .collect(java.util.stream.Collectors.joining(", ")));
        MessageUtil.sendMessage(sender, "&7Rastgele süre: &e" + config.getAutomaticMinMinutes()
                + "-" + config.getAutomaticMaxMinutes() + " dakika"
                + " &8• &7Spawn uzaklığı: &e" + config.getAutomaticMinDistance()
                + "-" + config.getAutomaticMaxDistance() + " blok"
                + " &8• &7Arama şekli: &e" + config.getAutomaticSearchShape(
                        worlds.isEmpty() ? "__default__" : worlds.getFirst()));
        if (worlds.isEmpty()) {
            MessageUtil.sendMessage(sender, "&7Dünyalar: &fTüm yüklü dünyalar &8• &7Yüzey: &e"
                    + config.getAutomaticLocationPreset("__default__").name());
        } else {
            MessageUtil.sendMessage(sender, "&6Auto dünya kuralları:");
            worlds.forEach(world -> MessageUtil.sendMessage(sender,
                    "&8• &b" + world + " &8→ &e" + config.getAutomaticPresetName(world)
                            + " &7(" + config.getAutomaticSearchShape(world)
                            + ", ağırlık " + config.getAutomaticWorldWeight(world) + ")"));
        }
    }

    private void executeStart(@NotNull org.bukkit.command.CommandSender sender, @NotNull CommandArguments args) {
        executeSafely(sender, () -> {
            final String raw = (String) args.get("type");
            try {
                final MeteorType type = MeteorType.fromString(raw);
                final World world = (World) args.getOptional("world")
                        .orElseGet(() -> sender instanceof Player p ? p.getWorld()
                                : plugin.getServer().getWorlds().getFirst());
                eventManager.startEvent(type, world.getName(), sender);
            } catch (IllegalArgumentException e) {
                tell(sender, "start.invalid_type", "type", raw);
            }
        });
    }

    private void executeRoot(@NotNull org.bukkit.command.CommandSender sender, @NotNull CommandArguments args) {
        showConfiguredHelp(sender);
    }

    private void showConfiguredHelp(@NotNull org.bukkit.command.CommandSender sender) {
        tell(sender, "help.title");
        final var categories = config.getCommandHelpCategories();
        if (categories.isEmpty()) {
            config.sendMessage(sender, "command.help.usage");
            return;
        }
        for (final ConfigManager.CommandHelpCategory category : categories) {
            MessageUtil.sendMessage(sender, category.title());
            category.commands().forEach(line -> MessageUtil.sendMessage(sender, line));
        }
    }

    private void executeSafely(@NotNull org.bukkit.command.CommandSender sender,
                               @NotNull Runnable action) {
        try {
            action.run();
        } catch (Exception | LinkageError error) {
            plugin.getLogger().log(Level.SEVERE,
                    "OlMeteor command failed for " + sender.getName(), error);
            MessageUtil.sendMessage(sender,
                    "&cKomut çalıştırılırken beklenmeyen bir hata oluştu. Konsola ayrıntılı kayıt yazıldı.");
        }
    }
}
