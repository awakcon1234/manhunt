package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.card.CardService;
import dev.belikhun.lunamanhunt.service.point.PointService;
import dev.belikhun.lunamanhunt.util.Text;
import dev.belikhun.lunamanhunt.util.UiColors;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.Objects;

public class CardListener implements Listener {
	private final CardService cardService;
	private final LobbyManager lobbyManager;
	private final PointService pointService;

	public CardListener(CardService cardService, LobbyManager lobbyManager, PointService pointService) {
		this.cardService = Objects.requireNonNull(cardService, "cardService");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.pointService = Objects.requireNonNull(pointService, "pointService");
	}

	@EventHandler
	public void onAdvancement(PlayerAdvancementDoneEvent event) {
		Player player = event.getPlayer();
		if (player == null) {
			return;
		}

		String key = event.getAdvancement().getKey().getKey();
		if (key != null && key.startsWith("recipes/")) {
			return;
		}

		cardService.handleSpeedrunnerAdvancement(player);

		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null || match.getState() != MatchState.RUNNING) {
			return;
		}
		if (!match.getHunters().contains(player.getUniqueId())) {
			return;
		}

		int gainedPoints = pointService.resolveAdvancementPoints(match);
		int currentPoints = pointService.addHunterPoints(match, player.getUniqueId(), gainedPoints);
		Text.msg(player, "<green>✔</green> Bạn nhận được " + pointService.formatPoints(gainedPoints)
			+ " từ thành tựu. Hiện có: " + pointService.formatPoints(currentPoints)
			+ ". <gray>(Dùng " + UiColors.command("/back") + " để quay lại điểm chết)</gray>");
	}
}
