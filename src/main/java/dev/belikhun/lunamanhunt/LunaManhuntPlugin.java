package dev.belikhun.lunamanhunt;

import dev.belikhun.lunamanhunt.command.CardsCommand;
import dev.belikhun.lunamanhunt.command.CompassCommand;
import dev.belikhun.lunamanhunt.command.BasicCommandAdapter;
import dev.belikhun.lunamanhunt.command.BackCommand;
import dev.belikhun.lunamanhunt.command.HoloCommand;
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
import dev.belikhun.lunamanhunt.listener.MatchConnectionListener;
import dev.belikhun.lunamanhunt.listener.MatchChatListener;
import dev.belikhun.lunamanhunt.listener.MatchPortalLinkListener;
import dev.belikhun.lunamanhunt.listener.MatchScopedAnnouncementListener;
import dev.belikhun.lunamanhunt.listener.MatchStatsListener;
import dev.belikhun.lunamanhunt.listener.MatchWinListener;
import dev.belikhun.lunamanhunt.listener.PlaceholderApiHookListener;
import dev.belikhun.lunamanhunt.listener.PlayerDeathRoutingListener;
import dev.belikhun.lunamanhunt.listener.RunStartListener;
import dev.belikhun.lunamanhunt.listener.SpawnConfirmListener;
import dev.belikhun.lunamanhunt.service.card.CardService;
import dev.belikhun.lunamanhunt.service.compass.CompassService;
import dev.belikhun.lunamanhunt.service.gui.GuiService;
import dev.belikhun.lunamanhunt.service.integration.MultiverseService;
import dev.belikhun.lunamanhunt.service.integration.NetherPortalsService;
import dev.belikhun.lunamanhunt.service.integration.LunaPlaceholderExpansion;
import dev.belikhun.lunamanhunt.service.integration.TimeRecordsHologramService;
import dev.belikhun.lunamanhunt.service.kit.SpeedrunnerKitService;
import dev.belikhun.lunamanhunt.service.kit.SpeedrunnerKitStore;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.match.MatchManager;
import dev.belikhun.lunamanhunt.service.match.MatchEndService;
import dev.belikhun.lunamanhunt.service.match.MatchDisconnectService;
import dev.belikhun.lunamanhunt.service.match.MatchLifecycleService;
import dev.belikhun.lunamanhunt.service.match.MatchSettingsService;
import dev.belikhun.lunamanhunt.service.persistence.ConfigMigrationService;
import dev.belikhun.lunamanhunt.service.match.WinConditionService;
import dev.belikhun.lunamanhunt.service.persistence.PersistenceManager;
import dev.belikhun.lunamanhunt.service.point.PointService;
import dev.belikhun.lunamanhunt.service.scoreboard.ScoreboardService;
import dev.belikhun.lunamanhunt.service.spawn.LobbyItemService;
import dev.belikhun.lunamanhunt.service.spawn.LobbyTeleportService;
import dev.belikhun.lunamanhunt.service.spawn.SpawnService;
import dev.belikhun.lunamanhunt.service.worldgen.WorldGenerationService;
import dev.belikhun.lunamanhunt.util.Debug;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

public final class LunaManhuntPlugin extends JavaPlugin {
	private PersistenceManager persistenceManager;
	private MultiverseService multiverseService;
	private MatchManager matchManager;
	private WorldGenerationService worldGenerationService;
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
	private MatchDisconnectService matchDisconnectService;
	private NetherPortalsService netherPortalsService;
	private ScoreboardService scoreboardService;
	private LunaPlaceholderExpansion placeholderExpansion;
	private TimeRecordsHologramService timeRecordsHologramService;

	@Override
	public void onEnable() {
		saveDefaultConfig();
		new ConfigMigrationService(this).migrateIfNeeded();
		registerCardPermissions();
		Debug.setEnabled(getConfig().getBoolean("debug.enabled", false));
		if (Debug.isEnabled()) {
			getLogger().info("[Debug] Global debug logging enabled.");
		}
		this.kitService = new SpeedrunnerKitService(new SpeedrunnerKitStore());
		this.matchSettingsService = new MatchSettingsService(this, kitService);
		this.persistenceManager = new PersistenceManager(this, matchSettingsService);
		this.multiverseService = new MultiverseService();
		this.netherPortalsService = new NetherPortalsService();
		this.worldGenerationService = new WorldGenerationService(this);
		this.matchManager = new MatchManager(this, multiverseService, worldGenerationService);
		int maxHunters = Math.max(16, Math.min(32, getConfig().getInt("balance.maxHunters", 24)));
		this.lobbyManager = new LobbyManager(matchManager, matchSettingsService, maxHunters);
		this.compassService = new CompassService(this, lobbyManager);
		this.pointService = new PointService(this);
		this.cardService = new CardService(this, lobbyManager, compassService, pointService);
		this.guiService = new GuiService(this, lobbyManager, matchManager, cardService, pointService,
			persistenceManager.getProfileStore(), matchSettingsService, kitService);
		this.lobbyItemService = new LobbyItemService(this, lobbyManager, guiService);
		this.guiService.setLobbyItemService(lobbyItemService);
		this.lobbyTeleportService = new LobbyTeleportService(this, lobbyItemService);
		this.matchEndService = new MatchEndService(this, lobbyManager, matchManager, multiverseService, netherPortalsService,
			lobbyTeleportService, cardService, pointService);
		this.matchEndService.processQueuedWorlds();
		this.matchEndService.startCompletedMatchCleanupMonitor();
		this.winConditionService = new WinConditionService(lobbyManager, persistenceManager.getProfileStore(),
			matchEndService);
		this.matchDisconnectService = new MatchDisconnectService(this, lobbyManager, winConditionService);
		this.matchLifecycleService = new MatchLifecycleService(this, lobbyManager, matchManager, compassService,
			kitService, cardService, netherPortalsService, pointService);
		this.matchManager.setLifecycleService(matchLifecycleService);
		this.spawnService = new SpawnService(this, lobbyManager, matchManager, winConditionService, lobbyTeleportService);
		this.scoreboardService = new ScoreboardService(this, lobbyManager, cardService, matchLifecycleService,
			pointService, persistenceManager.getProfileStore());
		this.timeRecordsHologramService = new TimeRecordsHologramService(this, persistenceManager.getProfileStore());
		registerPlaceholderExpansion();

		getServer().getPluginManager().registerEvents(new PlaceholderApiHookListener(this), this);
		getServer().getPluginManager().registerEvents(new MatchWinListener(winConditionService, lobbyManager), this);
		getServer().getPluginManager().registerEvents(
			new PlayerDeathRoutingListener(this, lobbyManager, lobbyTeleportService, pointService), this);
		getServer().getPluginManager().registerEvents(new MatchConnectionListener(matchDisconnectService), this);
		getServer().getPluginManager().registerEvents(new CompassListener(compassService), this);
		getServer().getPluginManager().registerEvents(new SpawnConfirmListener(spawnService), this);
		getServer().getPluginManager().registerEvents(new GuiListener(guiService), this);
		getServer().getPluginManager().registerEvents(new GuiInputListener(this, guiService), this);
		getServer().getPluginManager().registerEvents(new MatchChatListener(this, lobbyManager, guiService), this);
		getServer().getPluginManager().registerEvents(new MatchScopedAnnouncementListener(this, lobbyManager), this);
		getServer().getPluginManager().registerEvents(new LobbyHotbarListener(lobbyItemService), this);
		getServer().getPluginManager().registerEvents(new LobbyItemGuardListener(lobbyItemService), this);
		getServer().getPluginManager().registerEvents(
			new LobbyStateListener(this, lobbyItemService, lobbyTeleportService, lobbyManager), this);
		getServer().getPluginManager().registerEvents(new CardListener(cardService, lobbyManager, pointService), this);
		getServer().getPluginManager().registerEvents(new CardEffectListener(cardService), this);
		getServer().getPluginManager().registerEvents(new RunStartListener(matchLifecycleService), this);
		getServer().getPluginManager().registerEvents(new MatchStatsListener(lobbyManager), this);
		getServer().getPluginManager().registerEvents(new MatchPortalLinkListener(lobbyManager, matchManager, getLogger()), this);
		getServer().getPluginManager().registerEvents(new EndPortalListener(matchEndService, getLogger()), this);
		compassService.startAutoUpdate();
		scoreboardService.start();
		MatchCommand matchCommand = new MatchCommand(lobbyManager, matchManager, guiService, lobbyItemService);
		LmAdminCommand adminCommand = new LmAdminCommand(this, lobbyManager, matchManager,
			matchLifecycleService, matchEndService);
		SpawnCommand spawnCommand = new SpawnCommand(spawnService);
		CardsCommand cardsCommand = new CardsCommand(lobbyManager, cardService, guiService);
		CompassCommand compassCommand = new CompassCommand(lobbyManager, compassService);
		BackCommand backCommand = new BackCommand(lobbyManager, pointService);
		HoloCommand holoCommand = new HoloCommand(timeRecordsHologramService);
		BasicCommandAdapter matchAdapter = new BasicCommandAdapter("match", matchCommand);
		BasicCommandAdapter matchAliasAdapter = new BasicCommandAdapter("m", matchCommand);
		BasicCommandAdapter adminAdapter = new BasicCommandAdapter("lm", adminCommand);
		BasicCommandAdapter adminManhuntAdapter = new BasicCommandAdapter("manhunt", adminCommand);
		BasicCommandAdapter adminHuntAdapter = new BasicCommandAdapter("hunt", adminCommand);
		BasicCommandAdapter adminMhAdapter = new BasicCommandAdapter("mh", adminCommand);
		BasicCommandAdapter spawnAdapter = new BasicCommandAdapter("spawn", spawnCommand);
		BasicCommandAdapter cardsAdapter = new BasicCommandAdapter("cards", cardsCommand);
		BasicCommandAdapter cardsAliasAdapter = new BasicCommandAdapter("c", cardsCommand);
		BasicCommandAdapter compassAdapter = new BasicCommandAdapter("compass", compassCommand);
		BasicCommandAdapter trackerAdapter = new BasicCommandAdapter("tracker", compassCommand);
		BasicCommandAdapter backAdapter = new BasicCommandAdapter("back", backCommand);
		BasicCommandAdapter holoAdapter = new BasicCommandAdapter("holo", holoCommand);
		getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
			event.registrar().register("match", matchAdapter);
			event.registrar().register("m", matchAliasAdapter);
			event.registrar().register("lm", adminAdapter);
			event.registrar().register("manhunt", adminManhuntAdapter);
			event.registrar().register("hunt", adminHuntAdapter);
			event.registrar().register("mh", adminMhAdapter);
			event.registrar().register("spawn", spawnAdapter);
			event.registrar().register("cards", cardsAdapter);
			event.registrar().register("c", cardsAliasAdapter);
			event.registrar().register("compass", compassAdapter);
			event.registrar().register("tracker", trackerAdapter);
			event.registrar().register("back", backAdapter);
			event.registrar().register("holo", holoAdapter);
		});
		getLogger().info("LunaManhunt enabled");
	}

	private void registerCardPermissions() {
		registerPermission("lunamanhunt.cards", "Xem thẻ bài", PermissionDefault.TRUE);
		registerPermission("lunamanhunt.cards.use", "Dùng thẻ bài", PermissionDefault.TRUE);
		registerPermission("lunamanhunt.spawn", "Dịch chuyển về sảnh", PermissionDefault.TRUE);
		registerPermission("lunamanhunt.compass", "Lấy lại la bàn truy tìm", PermissionDefault.TRUE);
		registerPermission("lunamanhunt.back", "Dùng /back về điểm chết", PermissionDefault.TRUE);
		registerPermission("lunamanhunt.admin.holo", "Quản lý hologram kỷ lục", PermissionDefault.OP);
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
		if (placeholderExpansion != null) {
			placeholderExpansion.unregister();
			placeholderExpansion = null;
		}
		if (scoreboardService != null) {
			scoreboardService.stop();
		}
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

	private synchronized void registerPlaceholderExpansion() {
		if (placeholderExpansion != null) {
			try {
				placeholderExpansion.unregister();
			} catch (Throwable ignored) {
			}
			placeholderExpansion = null;
		}

		if (!getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			getLogger().info("PlaceholderAPI chưa sẵn sàng, sẽ chờ hook để đăng ký %mh_*%.");
			return;
		}

		try {
			placeholderExpansion = new LunaPlaceholderExpansion(this, lobbyManager,
				persistenceManager.getProfileStore());
			if (placeholderExpansion.register()) {
				getLogger().info("PlaceholderAPI expansion đã đăng ký: %mh_*%.");
				return;
			}
			getLogger().warning("Không thể đăng ký PlaceholderAPI expansion cho LunaManhunt (%mh_*%).");
			placeholderExpansion = null;
		} catch (Throwable throwable) {
			getLogger().warning("Lỗi đăng ký PlaceholderAPI expansion: " + throwable.getMessage());
			placeholderExpansion = null;
		}
	}

	public synchronized void onPlaceholderApiEnabled() {
		registerPlaceholderExpansion();
	}

	public synchronized void onPlaceholderApiDisabled() {
		if (placeholderExpansion != null) {
			try {
				placeholderExpansion.unregister();
			} catch (Throwable ignored) {
			}
			placeholderExpansion = null;
		}
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

	public PointService getPointService() {
		return pointService;
	}

}
