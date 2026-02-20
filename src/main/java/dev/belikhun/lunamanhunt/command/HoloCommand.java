package dev.belikhun.lunamanhunt.command;

import dev.belikhun.lunamanhunt.service.integration.TimeRecordsHologramService;
import dev.belikhun.lunamanhunt.util.Text;
import dev.belikhun.lunamanhunt.util.UiColors;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class HoloCommand implements TabExecutor {
	private static final String PERM = "lunamanhunt.admin.holo";
	private final TimeRecordsHologramService timeRecords;

	public HoloCommand(TimeRecordsHologramService timeRecords) {
		this.timeRecords = Objects.requireNonNull(timeRecords, "timeRecords");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.hasPermission(PERM) && !sender.hasPermission("lunamanhunt.admin")) {
			reply(sender, "<red>❌</red> Bạn không có quyền dùng lệnh này.");
			return true;
		}

		if (args.length < 1 || !"timerecords".equalsIgnoreCase(args[0])) {
			sendHelp(sender);
			return true;
		}

		if (args.length < 2) {
			sendHelp(sender);
			return true;
		}

		String action = args[1].toLowerCase(Locale.ROOT);
		try {
			switch (action) {
				case "create" -> handleCreate(sender);
				case "setpos", "move", "position" -> handleSetPos(sender);
				case "setfixed" -> handleSetFixed(sender, args);
				case "setbg", "setbackground" -> handleSetBackground(sender, args);
				case "setopacity" -> handleSetOpacity(sender, args);
				case "refresh" -> handleRefresh(sender);
				case "remove", "delete" -> handleRemove(sender);
				case "info" -> handleInfo(sender);
				default -> sendHelp(sender);
			}
		} catch (Exception exception) {
			reply(sender, "<red>❌</red> Lỗi thao tác hologram: <white>" + safe(exception.getMessage()) + "</white>");
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1) {
			return filter(List.of("timerecords"), args[0]);
		}
		if (args.length == 2 && "timerecords".equalsIgnoreCase(args[0])) {
			return filter(List.of("create", "setpos", "setfixed", "setbg", "setopacity", "refresh", "remove", "info"), args[1]);
		}
		if (args.length == 3 && "timerecords".equalsIgnoreCase(args[0])) {
			if ("setfixed".equalsIgnoreCase(args[1])) {
				return filter(List.of("true", "false"), args[2]);
			}
			if ("setbg".equalsIgnoreCase(args[1]) || "setbackground".equalsIgnoreCase(args[1])) {
				return filter(List.of("#000000", "#1f2937", "none"), args[2]);
			}
			if ("setopacity".equalsIgnoreCase(args[1])) {
				return filter(List.of("1.0", "0.85", "0.6", "0.3"), args[2]);
			}
		}
		return List.of();
	}

	private void handleCreate(CommandSender sender) throws Exception {
		Location location = sender instanceof Player player ? player.getLocation() : null;
		if (!timeRecords.createOrRefresh(location)) {
			reply(sender, "<red>❌</red> Chưa có vị trí. Hãy đứng tại vị trí muốn đặt và dùng "
				+ UiColors.command("/holo timerecords setpos") + ".");
			return;
		}
		reply(sender, "<green>✔</green> Đã tạo/cập nhật hologram kỷ lục: <white>" + safe(timeRecords.getHologramName()) + "</white>.");
	}

	private void handleSetPos(CommandSender sender) throws Exception {
		if (!(sender instanceof Player player)) {
			reply(sender, "<red>❌</red> Lệnh này chỉ dùng trong game.");
			return;
		}
		timeRecords.setPosition(player.getLocation());
		timeRecords.createOrRefresh(player.getLocation());
		reply(sender, "<green>✔</green> Đã cập nhật vị trí hologram tại chỗ đứng hiện tại.");
	}

	private void handleSetFixed(CommandSender sender, String[] args) throws Exception {
		if (args.length < 3) {
			reply(sender, "<red>❌</red> Dùng: " + UiColors.command("/holo timerecords setfixed <true|false>") + ".");
			return;
		}
		Boolean value = parseBool(args[2]);
		if (value == null) {
			reply(sender, "<red>❌</red> Giá trị không hợp lệ. Dùng <white>true</white> hoặc <white>false</white>.");
			return;
		}
		timeRecords.setFixedBillboard(value);
		timeRecords.refresh();
		reply(sender, "<green>✔</green> Đã đặt billboard cố định: <white>" + value + "</white>.");
	}

	private void handleSetBackground(CommandSender sender, String[] args) throws Exception {
		if (args.length < 3) {
			reply(sender, "<red>❌</red> Dùng: " + UiColors.command("/holo timerecords setbg <#RRGGBB|none>") + ".");
			return;
		}
		timeRecords.setBackgroundHex(args[2]);
		timeRecords.refresh();
		reply(sender, "<green>✔</green> Đã cập nhật màu nền hologram.");
	}

	private void handleSetOpacity(CommandSender sender, String[] args) throws Exception {
		if (args.length < 3) {
			reply(sender, "<red>❌</red> Dùng: " + UiColors.command("/holo timerecords setopacity <0..1>") + ".");
			return;
		}
		float opacity;
		try {
			opacity = Float.parseFloat(args[2]);
		} catch (NumberFormatException exception) {
			reply(sender, "<red>❌</red> Độ mờ không hợp lệ. Ví dụ: <white>0.85</white>.");
			return;
		}
		timeRecords.setOpacity(opacity);
		timeRecords.refresh();
		reply(sender, "<green>✔</green> Đã lưu độ mờ: <white>" + String.format(Locale.US, "%.2f", timeRecords.getOpacity())
			+ "</white> (áp dụng qua alpha nền của text hologram).");
	}

	private void handleRefresh(CommandSender sender) throws Exception {
		if (!timeRecords.refresh()) {
			reply(sender, "<yellow>ℹ</yellow> Không tìm thấy hologram đang tồn tại. Dùng " + UiColors.command("/holo timerecords create") + " để tạo mới.");
			return;
		}
		reply(sender, "<green>✔</green> Đã làm mới bảng kỷ lục.");
	}

	private void handleRemove(CommandSender sender) throws Exception {
		if (!timeRecords.remove()) {
			reply(sender, "<yellow>ℹ</yellow> Không có hologram để xóa.");
			return;
		}
		reply(sender, "<green>✔</green> Đã xóa hologram kỷ lục.");
	}

	private void handleInfo(CommandSender sender) {
		String location = Text.fmtPos(timeRecords.getStoredLocation());
		String bg = timeRecords.getBackgroundHex().isBlank() ? "none" : timeRecords.getBackgroundHex();
		reply(sender, "<yellow>●</yellow> Tên: <white>" + safe(timeRecords.getHologramName()) + "</white>");
		reply(sender, "<yellow>●</yellow> Vị trí: <white>" + safe(location) + "</white>");
		reply(sender, "<yellow>●</yellow> Fixed: <white>" + timeRecords.isFixedBillboard() + "</white>");
		reply(sender, "<yellow>●</yellow> Background: <white>" + safe(bg) + "</white>");
		reply(sender, "<yellow>●</yellow> Opacity: <white>" + String.format(Locale.US, "%.2f", timeRecords.getOpacity()) + "</white>");
	}

	private void sendHelp(CommandSender sender) {
		reply(sender, "<yellow>●</yellow> " + UiColors.command("/holo timerecords create"));
		reply(sender, "<yellow>●</yellow> " + UiColors.command("/holo timerecords setpos"));
		reply(sender, "<yellow>●</yellow> " + UiColors.command("/holo timerecords setfixed <true|false>"));
		reply(sender, "<yellow>●</yellow> " + UiColors.command("/holo timerecords setbg <#RRGGBB|none>"));
		reply(sender, "<yellow>●</yellow> " + UiColors.command("/holo timerecords setopacity <0..1>"));
		reply(sender, "<yellow>●</yellow> " + UiColors.command("/holo timerecords refresh"));
		reply(sender, "<yellow>●</yellow> " + UiColors.command("/holo timerecords remove"));
		reply(sender, "<yellow>●</yellow> " + UiColors.command("/holo timerecords info"));
	}

	private Boolean parseBool(String raw) {
		if (raw == null) {
			return null;
		}
		return switch (raw.toLowerCase(Locale.ROOT)) {
			case "true", "on", "yes", "1" -> true;
			case "false", "off", "no", "0" -> false;
			default -> null;
		};
	}

	private String safe(String value) {
		if (value == null || value.isBlank()) {
			return "-";
		}
		return value.replace("<", "\\<").replace(">", "\\>");
	}

	private void reply(CommandSender sender, String message) {
		if (sender instanceof Player player) {
			Text.msg(player, message);
			return;
		}
		sender.sendMessage(Text.colorize(message
			.replace("<red>", "&c").replace("</red>", "")
			.replace("<green>", "&a").replace("</green>", "")
			.replace("<yellow>", "&e").replace("</yellow>", "")
			.replace("<white>", "&f").replace("</white>", "")));
	}

	private List<String> filter(List<String> options, String prefix) {
		if (prefix == null || prefix.isEmpty()) {
			return options;
		}
		String lower = prefix.toLowerCase(Locale.ROOT);
		return options.stream()
			.filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
			.toList();
	}
}
