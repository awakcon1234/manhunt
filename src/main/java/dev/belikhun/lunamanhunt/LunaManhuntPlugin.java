package dev.belikhun.lunamanhunt;

import dev.belikhun.lunamanhunt.command.CardsCommand;
import dev.belikhun.lunamanhunt.command.BasicCommandAdapter;
import dev.belikhun.lunamanhunt.command.LmAdminCommand;
import dev.belikhun.lunamanhunt.command.MatchCommand;
import dev.belikhun.lunamanhunt.command.SpawnCommand;
import dev.belikhun.lunamanhunt.listener.CardEffectListener;
import dev.belikhun.lunamanhunt.listener.CardListener;
import dev.belikhun.lunamanhunt.listener.CompassListener;
import dev.belikhun.lunamanhunt.listener.EndPortalListener;
import dev.belikhun.lunamanhunt.listener.GuiInputListener;
import dev.belikhun.lunamanhunt.listener.GuiListener;
import dev.belikhun.lunamanhunt.listener.LobbyHotbarListener;
import dev.belikhun.lunamanhunt.listener.LobbyItemGuardListener;
import dev.belikhun.lunamanhunt.listener.LobbyStateListener;
import dev.belikhun.lunamanhunt.listener.MatchWinListener;
import dev.belikhun.lunamanhunt.listener.RunStartListener;
import dev.belikhun.lunamanhunt.listener.SpawnConfirmListener;
import dev.belikhun.lunamanhunt.service.card.CardService;
import dev.belikhun.lunamanhunt.service.compass.CompassService;
import dev.belikhun.lunamanhunt.service.gui.GuiService;
import dev.belikhun.lunamanhunt.service.integration.MultiverseService;
import dev.belikhun.lunamanhunt.service.integration.NetherPortalsService;
import dev.belikhun.lunamanhunt.service.kit.SpeedrunnerKitService;
import dev.belikhun.lunamanhunt.service.kit.SpeedrunnerKitStore;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.match.MatchManager;
import dev.belikhun.lunamanhunt.service.match.MatchEndService;
import dev.belikhun.lunamanhunt.service.match.MatchLifecycleService;
import dev.belikhun.lunamanhunt.service.match.MatchSettingsService;
import dev.belikhun.lunamanhunt.service.match.WinConditionService;
import dev.belikhun.lunamanhunt.service.persistence.PersistenceManager;
import dev.belikhun.lunamanhunt.service.point.PointService;
import dev.belikhun.lunamanhunt.service.spawn.LobbyItemService;
import dev.belikhun.lunamanhunt.service.spawn.LobbyTeleportService;
import dev.belikhun.lunamanhunt.service.spawn.SpawnService;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

public final class LunaManhuntPlugin extends JavaPlugin {
    private PersistenceManager persistenceManager;
    private MultiverseService multiverseService;
    private MatchManager matchManager;
    private MatchSettingsService matchSettingsService;
    private SpeedrunnerKitService kitService;
    private LobbyManager lobbyManager;
    private WinConditionService winConditionService;
    private CompassService compassService;
    private SpawnService spawnService;
    private GuiService guiService;
    private CardService cardService;
    private PointService pointService;
    private LobbyTeleportService lobbyTeleportService;
    private LobbyItemService lobbyItemService;
    private MatchLifecycleService matchLifecycleService;
    private MatchEndService matchEndService;
    private NetherPortalsService netherPortalsService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        registerCardPermissions();
        this.kitService = new SpeedrunnerKitService(new SpeedrunnerKitStore());
        this.matchSettingsService = new MatchSettingsService(this, kitService);
        this.persistenceManager = new PersistenceManager(this, matchSettingsService);
        this.multiverseService = new MultiverseService();
        this.netherPortalsService = new NetherPortalsService();
        this.matchManager = new MatchManager(this, multiverseService);
        this.lobbyManager = new LobbyManager(matchManager, matchSettingsService);
        this.compassService = new CompassService(this, lobbyManager);
        this.pointService = new PointService();
        this.cardService = new CardService(this, lobbyManager, compassService, pointService);
        this.guiService = new GuiService(this, lobbyManager, matchManager, cardService, pointService,
            persistenceManager.getProfileStore(), matchSettingsService, kitService);
        this.lobbyItemService = new LobbyItemService(this, lobbyManager, guiService);
        this.guiService.setLobbyItemService(lobbyItemService);
        this.lobbyTeleportService = new LobbyTeleportService(this, lobbyItemService);
        this.matchEndService = new MatchEndService(this, lobbyManager, multiverseService, netherPortalsService,
            lobbyTeleportService, cardService);
        this.matchEndService.processQueuedWorlds();
        this.winConditionService = new WinConditionService(lobbyManager, persistenceManager.getProfileStore(),
            matchEndService);
        this.matchLifecycleService = new MatchLifecycleService(this, lobbyManager, matchManager, compassService,
            kitService, cardService, netherPortalsService);
        this.matchManager.setLifecycleService(matchLifecycleService);
        this.spawnService = new SpawnService(this, lobbyManager, matchManager, winConditionService, lobbyTeleportService);

        getServer().getPluginManager().registerEvents(new MatchWinListener(winConditionService), this);
        getServer().getPluginManager().registerEvents(new CompassListener(compassService), this);
        getServer().getPluginManager().registerEvents(new SpawnConfirmListener(spawnService), this);
        getServer().getPluginManager().registerEvents(new GuiListener(guiService), this);
        getServer().getPluginManager().registerEvents(new GuiInputListener(this, guiService), this);
        getServer().getPluginManager().registerEvents(new LobbyHotbarListener(lobbyItemService), this);
        getServer().getPluginManager().registerEvents(new LobbyItemGuardListener(lobbyItemService), this);
        getServer().getPluginManager().registerEvents(new LobbyStateListener(lobbyItemService, lobbyTeleportService), this);
        getServer().getPluginManager().registerEvents(new CardListener(cardService), this);
        getServer().getPluginManager().registerEvents(new CardEffectListener(cardService), this);
        getServer().getPluginManager().registerEvents(new RunStartListener(matchLifecycleService), this);
        getServer().getPluginManager().registerEvents(new EndPortalListener(matchEndService), this);
        compassService.startAutoUpdate();
        MatchCommand matchCommand = new MatchCommand(lobbyManager, matchManager, guiService, lobbyItemService);
        LmAdminCommand adminCommand = new LmAdminCommand(this, lobbyManager, matchManager);
        SpawnCommand spawnCommand = new SpawnCommand(spawnService);
        CardsCommand cardsCommand = new CardsCommand(lobbyManager, cardService, guiService);
        BasicCommandAdapter matchAdapter = new BasicCommandAdapter("match", matchCommand);
        BasicCommandAdapter matchAliasAdapter = new BasicCommandAdapter("m", matchCommand);
        BasicCommandAdapter adminAdapter = new BasicCommandAdapter("lm", adminCommand);
        BasicCommandAdapter spawnAdapter = new BasicCommandAdapter("spawn", spawnCommand);
        BasicCommandAdapter cardsAdapter = new BasicCommandAdapter("cards", cardsCommand);
        BasicCommandAdapter cardsAliasAdapter = new BasicCommandAdapter("c", cardsCommand);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register("match", matchAdapter);
            event.registrar().register("m", matchAliasAdapter);
            event.registrar().register("lm", adminAdapter);
            event.registrar().register("spawn", spawnAdapter);
            event.registrar().register("cards", cardsAdapter);
            event.registrar().register("c", cardsAliasAdapter);
        });
        getLogger().info("LunaManhunt enabled");
    }

    private void registerCardPermissions() {
        registerPermission("lunamanhunt.cards", "Xem thẻ bài", PermissionDefault.TRUE);
        registerPermission("lunamanhunt.cards.use", "Dùng thẻ bài", PermissionDefault.TRUE);
        registerPermission("lunamanhunt.spawn", "Dịch chuyển về sảnh", PermissionDefault.TRUE);
    }

    private void registerPermission(String node, String description, PermissionDefault defaultValue) {
        var pluginManager = getServer().getPluginManager();
        Permission permission = pluginManager.getPermission(node);
        if (permission == null) {
            permission = new Permission(node, description, defaultValue);
            pluginManager.addPermission(permission);
            return;
        }
        permission.setDefault(defaultValue);
        if ((permission.getDescription() == null || permission.getDescription().isBlank()) && description != null) {
            permission.setDescription(description);
        }
    }

    @Override
    public void onDisable() {
        if (matchEndService != null && matchManager != null) {
            for (var match : java.util.Set.copyOf(matchManager.getMatches())) {
                matchEndService.forceCleanup(match);
            }
        }
        getLogger().info("LunaManhunt disabled");
    }

    public MatchManager getMatchManager() {
        return matchManager;
    }

    public LobbyManager getLobbyManager() {
        return lobbyManager;
    }

    public WinConditionService getWinConditionService() {
        return winConditionService;
    }

    public CompassService getCompassService() {
        return compassService;
    }

    public PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

}
