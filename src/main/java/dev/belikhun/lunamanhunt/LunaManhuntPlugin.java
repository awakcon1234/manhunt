package dev.belikhun.lunamanhunt;

import dev.belikhun.lunamanhunt.command.LmAdminCommand;
import dev.belikhun.lunamanhunt.command.MatchCommand;
import dev.belikhun.lunamanhunt.command.CardsCommand;
import dev.belikhun.lunamanhunt.command.SpawnCommand;
import dev.belikhun.lunamanhunt.listener.CardListener;
import dev.belikhun.lunamanhunt.listener.CompassListener;
import dev.belikhun.lunamanhunt.listener.EndPortalListener;
import dev.belikhun.lunamanhunt.listener.GuiInputListener;
import dev.belikhun.lunamanhunt.listener.GuiListener;
import dev.belikhun.lunamanhunt.listener.MatchWinListener;
import dev.belikhun.lunamanhunt.listener.RunStartListener;
import dev.belikhun.lunamanhunt.listener.SpawnConfirmListener;
import dev.belikhun.lunamanhunt.service.card.CardService;
import dev.belikhun.lunamanhunt.service.compass.CompassService;
import dev.belikhun.lunamanhunt.service.gui.GuiService;
import dev.belikhun.lunamanhunt.service.integration.MultiverseService;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.match.MatchManager;
import dev.belikhun.lunamanhunt.service.match.MatchEndService;
import dev.belikhun.lunamanhunt.service.match.MatchLifecycleService;
import dev.belikhun.lunamanhunt.service.match.MatchSettingsService;
import dev.belikhun.lunamanhunt.service.match.WinConditionService;
import dev.belikhun.lunamanhunt.service.persistence.PersistenceManager;
import dev.belikhun.lunamanhunt.service.point.PointService;
import dev.belikhun.lunamanhunt.service.spawn.LobbyTeleportService;
import dev.belikhun.lunamanhunt.service.spawn.SpawnService;
import org.bukkit.plugin.java.JavaPlugin;

public final class LunaManhuntPlugin extends JavaPlugin {
    private PersistenceManager persistenceManager;
    private MultiverseService multiverseService;
    private MatchManager matchManager;
    private MatchSettingsService matchSettingsService;
    private LobbyManager lobbyManager;
    private WinConditionService winConditionService;
    private CompassService compassService;
    private SpawnService spawnService;
    private GuiService guiService;
    private CardService cardService;
    private PointService pointService;
    private LobbyTeleportService lobbyTeleportService;
    private MatchLifecycleService matchLifecycleService;
    private MatchEndService matchEndService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.matchSettingsService = new MatchSettingsService(this);
        this.persistenceManager = new PersistenceManager(this, matchSettingsService);
        this.multiverseService = new MultiverseService();
        this.matchManager = new MatchManager(this, multiverseService);
        this.lobbyManager = new LobbyManager(matchManager, matchSettingsService);
        this.lobbyTeleportService = new LobbyTeleportService(this);
        this.matchEndService = new MatchEndService(this, lobbyManager, multiverseService, lobbyTeleportService);
        this.winConditionService = new WinConditionService(lobbyManager, persistenceManager.getProfileStore(),
            matchEndService);
        this.compassService = new CompassService(this, lobbyManager);
        this.matchLifecycleService = new MatchLifecycleService(lobbyManager, matchManager, compassService);
        this.matchManager.setLifecycleService(matchLifecycleService);
        this.spawnService = new SpawnService(this, lobbyManager, matchManager, winConditionService, lobbyTeleportService);
        this.pointService = new PointService();
        this.cardService = new CardService(this, lobbyManager, compassService, pointService);
        this.guiService = new GuiService(this, lobbyManager, matchManager, cardService, pointService);

        getServer().getPluginManager().registerEvents(new MatchWinListener(winConditionService), this);
        getServer().getPluginManager().registerEvents(new CompassListener(compassService), this);
        getServer().getPluginManager().registerEvents(new SpawnConfirmListener(spawnService), this);
        getServer().getPluginManager().registerEvents(new GuiListener(guiService), this);
        getServer().getPluginManager().registerEvents(new GuiInputListener(this, guiService), this);
        getServer().getPluginManager().registerEvents(new CardListener(cardService), this);
        getServer().getPluginManager().registerEvents(new RunStartListener(matchLifecycleService), this);
        getServer().getPluginManager().registerEvents(new EndPortalListener(matchEndService), this);
        MatchCommand matchCommand = new MatchCommand(lobbyManager, matchManager, guiService);
        LmAdminCommand adminCommand = new LmAdminCommand(this, lobbyManager, matchManager);
        SpawnCommand spawnCommand = new SpawnCommand(spawnService);
        CardsCommand cardsCommand = new CardsCommand(lobbyManager, cardService, guiService);
        if (getCommand("match") != null) {
            getCommand("match").setExecutor(matchCommand);
            getCommand("match").setTabCompleter(matchCommand);
        }
        if (getCommand("lm") != null) {
            getCommand("lm").setExecutor(adminCommand);
            getCommand("lm").setTabCompleter(adminCommand);
        }
        if (getCommand("spawn") != null) {
            getCommand("spawn").setExecutor(spawnCommand);
            getCommand("spawn").setTabCompleter(spawnCommand);
        }
        if (getCommand("cards") != null) {
            getCommand("cards").setExecutor(cardsCommand);
            getCommand("cards").setTabCompleter(cardsCommand);
        }
        getLogger().info("LunaManhunt enabled");
    }

    @Override
    public void onDisable() {
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
