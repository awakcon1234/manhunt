package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import dev.belikhun.lunamanhunt.util.UiColors;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CurseAdventureModeCard extends BaseCard {
	private final Map<String, Map<UUID, GameMode>> snapshotsByMatch = new ConcurrentHashMap<>();

	public CurseAdventureModeCard() {
		super("adventure_lock", "Khóa phiêu lưu", CardType.CURSE, CardRarity.RARE, 2,
			"Chuyển tất cả " + UiColors.roleHunterLower() + " sang chế độ Phiêu lưu trong 20 giây.", Material.IRON_BARS, 20);
	}

	@Override
	public boolean apply(CardContext context) {
		if (context == null || context.getMatch() == null) {
			return false;
		}
		Match match = context.getMatch();
		String matchId = match.getId();
		if (matchId == null || matchId.isBlank()) {
			return false;
		}
		Set<UUID> hunterIds = match.getHunters();
		if (hunterIds.isEmpty()) {
			hunterIds = match.getLobbyPlayers();
		}
		Map<UUID, GameMode> snapshot = new ConcurrentHashMap<>();
		boolean changed = false;
		for (UUID hunterId : Set.copyOf(hunterIds)) {
			if (context.getSpeedrunner() != null && hunterId.equals(context.getSpeedrunner().getUniqueId())) {
				continue;
			}
			Player hunter = Bukkit.getPlayer(hunterId);
			if (hunter == null) {
				continue;
			}
			GameMode current = hunter.getGameMode();
			if (current == GameMode.SPECTATOR) {
				continue;
			}
			snapshot.put(hunterId, current);
			if (current != GameMode.ADVENTURE) {
				hunter.setGameMode(GameMode.ADVENTURE);
				changed = true;
			}
		}
		if (snapshot.isEmpty()) {
			return false;
		}
		snapshotsByMatch.put(matchId, snapshot);
		return changed;
	}

	@Override
	public boolean clear(CardContext context) {
		if (context == null || context.getMatch() == null) {
			return false;
		}
		String matchId = context.getMatch().getId();
		if (matchId == null || matchId.isBlank()) {
			return false;
		}
		Map<UUID, GameMode> snapshot = snapshotsByMatch.remove(matchId);
		if (snapshot == null || snapshot.isEmpty()) {
			return false;
		}
		boolean restored = false;
		for (Map.Entry<UUID, GameMode> entry : snapshot.entrySet()) {
			Player hunter = Bukkit.getPlayer(entry.getKey());
			if (hunter == null) {
				continue;
			}
			if (hunter.getGameMode() == GameMode.SPECTATOR) {
				continue;
			}
			hunter.setGameMode(entry.getValue());
			restored = true;
		}
		return restored;
	}
}
