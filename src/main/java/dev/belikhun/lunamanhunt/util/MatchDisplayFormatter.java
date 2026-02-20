package dev.belikhun.lunamanhunt.util;

import dev.belikhun.lunamanhunt.model.match.MatchState;

import java.util.Locale;

public final class MatchDisplayFormatter {
	private MatchDisplayFormatter() {
	}

	public static String formatRoleDisplay(String roleKey) {
		String normalized = normalizeRole(roleKey);
		return switch (normalized) {
			case "speedrunner" -> "Speedrunner";
			case "hunter" -> "Thợ săn";
			case "lobby" -> "Sảnh";
			case "player" -> "Người chơi";
			default -> "Không có";
		};
	}

	public static String formatRoleColored(String roleKey) {
		String normalized = normalizeRole(roleKey);
		return switch (normalized) {
			case "speedrunner" -> UiColors.roleSpeedrunner();
			case "hunter" -> UiColors.roleHunter();
			case "lobby" -> "<white>Sảnh</white>";
			case "player" -> "<white>Người chơi</white>";
			default -> "<gray>Không có</gray>";
		};
	}

	public static String formatRoleColored(boolean speedrunner, boolean hunter) {
		if (speedrunner) {
			return UiColors.roleSpeedrunner();
		}
		if (hunter) {
			return UiColors.roleHunter();
		}
		return "<white>Người chơi</white>";
	}

	public static String formatRoleChip(boolean speedrunner, boolean hunter) {
		if (speedrunner) {
			return "<gold>★</gold> " + UiColors.roleSpeedrunner();
		}
		if (hunter) {
			return "<red>⚔</red> " + UiColors.roleHunter();
		}
		return "<yellow>⌛</yellow> <#ffd166>Chờ phân vai</#ffd166>";
	}

	public static String formatMatchStateDisplay(MatchState state) {
		if (state == null) {
			return "Chưa tham gia";
		}
		return switch (state) {
			case LOBBY -> "Phòng chờ";
			case WORLD_CREATING -> "Đang tạo thế giới";
			case WORLD_READY -> "Thế giới sẵn sàng";
			case RUN_PENDING -> "Chờ bắt đầu";
			case RUNNING -> "Đang chạy";
			case ENDING -> "Kết thúc";
			case CLEANUP -> "Đang dọn dẹp";
			case FINISHED -> "Đã xong";
		};
	}

	public static String formatMatchStateColoredScoreboard(MatchState state) {
		if (state == null) {
			return "-";
		}
		return switch (state) {
			case LOBBY -> "<gray>Đang chờ</gray>";
			case WORLD_CREATING -> "<yellow>Đang tạo thế giới</yellow>";
			case WORLD_READY -> "<yellow>Đang chuẩn bị</yellow>";
			case RUN_PENDING -> "<aqua>Chờ bắt đầu</aqua>";
			case RUNNING -> "<green>Đang chạy</green>";
			case ENDING -> "<gold>Kết thúc</gold>";
			case CLEANUP -> "<gold>Thu dọn</gold>";
			case FINISHED -> "<gray>Đã xong</gray>";
		};
	}

	public static String formatMatchStateColoredGui(MatchState state) {
		if (state == null) {
			return "<gray>Không rõ</gray>";
		}
		return switch (state) {
			case LOBBY -> "<#7fd36b>Chờ</#7fd36b>";
			case WORLD_CREATING -> "<#ffd166>Tạo thế giới</#ffd166>";
			case WORLD_READY -> "<#7fd36b>Sẵn sàng</#7fd36b>";
			case RUN_PENDING -> "<#ffd166>Chờ bắt đầu</#ffd166>";
			case RUNNING -> "<#36a8ff>Đang chạy</#36a8ff>";
			case ENDING -> "<#ff6b6b>Kết thúc</#ff6b6b>";
			case CLEANUP -> "<#ffd166>Dọn dẹp</#ffd166>";
			case FINISHED -> "<#9aa0a6>Đã xong</#9aa0a6>";
		};
	}

	private static String normalizeRole(String roleKey) {
		if (roleKey == null) {
			return "";
		}
		String normalized = roleKey.trim().toLowerCase(Locale.ROOT);
		if (normalized.equals("speedrunner")) {
			return "speedrunner";
		}
		if (normalized.equals("hunter") || normalized.equals("hunters")) {
			return "hunter";
		}
		if (normalized.equals("lobby")) {
			return "lobby";
		}
		if (normalized.equals("player")) {
			return "player";
		}
		return normalized;
	}
}
