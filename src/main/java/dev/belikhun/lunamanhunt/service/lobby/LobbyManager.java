package dev.belikhun.lunamanhunt.service.lobby;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchSettings;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.model.match.MatchVisibility;
import dev.belikhun.lunamanhunt.service.match.MatchSettingsService;
import dev.belikhun.lunamanhunt.service.match.MatchManager;
import dev.belikhun.lunamanhunt.util.Text;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyManager {
	private final MatchManager matchManager;
	private final MatchSettingsService matchSettingsService;
	private final Map<UUID, String> playerMatchIndex = new ConcurrentHashMap<>();

	public LobbyManager(MatchManager matchManager, MatchSettingsService matchSettingsService) {
		this.matchManager = Objects.requireNonNull(matchManager, "matchManager");
		this.matchSettingsService = Objects.requireNonNull(matchSettingsService, "matchSettingsService");
	}

	public Match createMatch(UUID ownerId) {
		return createMatch(ownerId, matchSettingsService.createDefaults());
	}

	public Match createMatch(UUID ownerId, MatchSettings settings) {
		Match match = matchManager.createMatch(ownerId, settings);
		playerMatchIndex.put(ownerId, match.getId());
		return match;
	}

	public LobbyActionResult invite(UUID inviterId, UUID targetId, Match match) {
		if (match == null) {
			return LobbyActionResult.fail("<red>❌</red> Không tìm thấy phòng đấu.");
		}
		if (match.getState() != MatchState.LOBBY) {
			return LobbyActionResult.fail("<red>❌</red> Phòng đã bắt đầu, không thể mời thêm.");
		}
		if (!match.getOwnerId().equals(inviterId)) {
			return LobbyActionResult.fail("<red>❌</red> Chỉ chủ phòng mới được mời người chơi.");
		}
		if (targetId == null) {
			return LobbyActionResult.fail("<red>❌</red> Người chơi không hợp lệ.");
		}

		match.getInvitedPlayers().add(targetId);
		return LobbyActionResult.ok("<green>✔</green> Đã gửi lời mời tham gia phòng.", match);
	}

	public LobbyActionResult acceptInvite(UUID playerId) {
		Match match = findInvitedMatch(playerId);
		if (match == null) {
			return LobbyActionResult.fail("<red>❌</red> Bạn không có lời mời nào.");
		}
		return join(playerId, match.getId());
	}

	public LobbyActionResult declineInvite(UUID playerId) {
		Match match = findInvitedMatch(playerId);
		if (match == null) {
			return LobbyActionResult.fail("<red>❌</red> Bạn không có lời mời nào.");
		}
		match.getInvitedPlayers().remove(playerId);
		return LobbyActionResult.ok("<green>✔</green> Đã từ chối lời mời vào phòng <yellow>" + match.getId() + "</yellow>.", match);
	}

	public LobbyActionResult join(UUID playerId, String matchId) {
		if (playerId == null || matchId == null || matchId.isBlank()) {
			return LobbyActionResult.fail("<red>❌</red> Mã phòng không hợp lệ.");
		}
		if (playerMatchIndex.containsKey(playerId)) {
			return LobbyActionResult.fail("<red>❌</red> Bạn đang ở trong một phòng khác.");
		}

		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			return LobbyActionResult.fail("<red>❌</red> Không tìm thấy phòng đấu.");
		}
		if (match.getState() != MatchState.LOBBY) {
			return LobbyActionResult.fail("<red>❌</red> Phòng đã bắt đầu, không thể tham gia.");
		}
		if (!canJoin(match, playerId)) {
			return LobbyActionResult.fail("<red>❌</red> Bạn không có quyền vào phòng này.");
		}

		match.getLobbyPlayers().add(playerId);
		playerMatchIndex.put(playerId, match.getId());
		announceMemberJoined(match, playerId);
		return LobbyActionResult.ok("<green>✔</green> Đã tham gia phòng <yellow>" + match.getId() + "</yellow>.", match);
	}

	public LobbyActionResult leave(UUID playerId) {
		return leave(playerId, true);
	}

	public LobbyActionResult leave(UUID playerId, boolean allowDisband) {
		if (playerId == null) {
			return LobbyActionResult.fail("<red>❌</red> Người chơi không hợp lệ.");
		}
		String matchId = playerMatchIndex.remove(playerId);
		if (matchId == null) {
			return LobbyActionResult.fail("<red>❌</red> Bạn chưa ở trong phòng nào.");
		}

		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			return LobbyActionResult.ok("<green>✔</green> Đã rời phòng.", null);
		}

		match.getLobbyPlayers().remove(playerId);
		match.getInvitedPlayers().remove(playerId);
		match.getHunters().remove(playerId);
		announceMemberLeft(match, playerId);

		if (allowDisband && match.getOwnerId().equals(playerId)) {
			disbandMatch(match);
			return LobbyActionResult.ok("<green>✔</green> Phòng đã bị giải tán.", null);
		}

		if (allowDisband && match.getLobbyPlayers().isEmpty()) {
			disbandMatch(match);
		}

		return LobbyActionResult.ok("<green>✔</green> Đã rời phòng.", match);
	}

	private void announceMemberJoined(Match match, UUID playerId) {
		if (match == null || playerId == null) {
			return;
		}
		String name = resolvePlayerName(playerId);
		int count = match.getLobbyPlayers().size();
		broadcastLobby(match, "<yellow>ℹ</yellow> <yellow>" + name
			+ "</yellow> đã vào phòng. <gray>(" + count + " người)</gray>");
	}

	private void announceMemberLeft(Match match, UUID playerId) {
		if (match == null || playerId == null) {
			return;
		}
		String name = resolvePlayerName(playerId);
		int count = match.getLobbyPlayers().size();
		broadcastLobby(match, "<yellow>ℹ</yellow> <yellow>" + name
			+ "</yellow> đã rời phòng. <gray>(" + count + " người)</gray>");
	}

	private void broadcastLobby(Match match, String message) {
		if (match == null || message == null) {
			return;
		}
		Set<UUID> members = new java.util.HashSet<>(match.getLobbyPlayers());
		members.addAll(match.getHunters());
		if (match.getSpeedrunnerId() != null) {
			members.add(match.getSpeedrunnerId());
		}
		for (UUID memberId : Set.copyOf(members)) {
			Player member = Bukkit.getPlayer(memberId);
			if (member != null) {
				Text.msg(member, message);
			}
		}
	}

	private String resolvePlayerName(UUID playerId) {
		Player online = Bukkit.getPlayer(playerId);
		if (online != null && online.getName() != null && !online.getName().isBlank()) {
			return online.getName();
		}
		OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
		String name = offline == null ? null : offline.getName();
		return name == null || name.isBlank() ? "Người chơi" : name;
	}

	public LobbyActionResult disbandMatch(Match match) {
		if (match == null) {
			return LobbyActionResult.fail("<red>❌</red> Không tìm thấy phòng đấu.");
		}

		Set<UUID> members = Set.copyOf(match.getLobbyPlayers());
		for (UUID member : members) {
			playerMatchIndex.remove(member);
		}
		matchManager.removeMatch(match.getId());
		return LobbyActionResult.ok("<green>✔</green> Phòng đã bị giải tán.", null);
	}

	public String getMatchIdForPlayer(UUID playerId) {
		return playerMatchIndex.get(playerId);
	}

	public Match getMatchForPlayer(UUID playerId) {
		String matchId = getMatchIdForPlayer(playerId);
		if (matchId == null) {
			return null;
		}
		return matchManager.getMatch(matchId);
	}

	private Match findInvitedMatch(UUID playerId) {
		if (playerId == null) {
			return null;
		}
		return matchManager.getMatches().stream()
			.filter(match -> match.getState() == MatchState.LOBBY)
			.filter(match -> match.getInvitedPlayers().contains(playerId))
			.sorted(java.util.Comparator.comparing(Match::getId))
			.findFirst()
			.orElse(null);
	}

	private boolean canJoin(Match match, UUID playerId) {
		if (match.getOwnerId().equals(playerId)) {
			return true;
		}
		MatchVisibility visibility = match.getSettings().getVisibility();
		if (visibility == MatchVisibility.PUBLIC) {
			return true;
		}
		return match.getInvitedPlayers().contains(playerId);
	}
}
