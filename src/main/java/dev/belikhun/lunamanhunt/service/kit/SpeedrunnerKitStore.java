package dev.belikhun.lunamanhunt.service.kit;

import dev.belikhun.lunamanhunt.model.kit.SpeedrunnerKitDefinition;
import dev.belikhun.lunamanhunt.util.UiColors;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SpeedrunnerKitStore {
	private final Map<String, SpeedrunnerKitDefinition> kitById = new LinkedHashMap<>();
	private final List<SpeedrunnerKitDefinition> kits = new ArrayList<>();
	private final String defaultKitId = "starter";

	public SpeedrunnerKitStore() {
		register(buildNoneKit());
		register(buildStarterKit());
		register(buildExplorerKit());
		register(buildCombatKit());
		register(buildRangerKit());
	}

	public List<SpeedrunnerKitDefinition> getAll() {
		return List.copyOf(kits);
	}

	public SpeedrunnerKitDefinition getById(String id) {
		return id == null ? null : kitById.get(id.toLowerCase());
	}

	public SpeedrunnerKitDefinition getDefaultKit() {
		return getById(defaultKitId);
	}

	public String getDefaultKitId() {
		return defaultKitId;
	}

	private void register(SpeedrunnerKitDefinition kit) {
		kitById.put(kit.getId().toLowerCase(), kit);
		kits.add(kit);
	}

	private SpeedrunnerKitDefinition buildNoneKit() {
		return new SpeedrunnerKitDefinition(
			"none",
			"Không có",
			Material.BARRIER,
			List.of("<gray>Không nhận vật phẩm khởi đầu.</gray>"),
			List.of()
		);
	}

	private SpeedrunnerKitDefinition buildStarterKit() {
		return new SpeedrunnerKitDefinition(
			"starter",
			"Cơ bản cân bằng",
			Material.STONE_SWORD,
			List.of(
				"<gray>Cân bằng giữa combat và sinh tồn.</gray>",
				"<gray>Phù hợp cho hầu hết tình huống.</gray>"
			),
			List.of(
				item(Material.STONE_SWORD, 1),
				item(Material.STONE_PICKAXE, 1),
				item(Material.STONE_AXE, 1),
				item(Material.LEATHER_HELMET, 1),
				item(Material.LEATHER_CHESTPLATE, 1),
				item(Material.LEATHER_LEGGINGS, 1),
				item(Material.LEATHER_BOOTS, 1),
				item(Material.COOKED_BEEF, 16),
				item(Material.TORCH, 16)
			)
		);
	}

	private SpeedrunnerKitDefinition buildExplorerKit() {
		return new SpeedrunnerKitDefinition(
			"explorer",
			"Khám phá",
			Material.COMPASS,
			List.of(
				"<gray>Ưu tiên đào mỏ và di chuyển.</gray>",
				"<gray>Chuẩn bị tốt cho việc tìm khoáng.</gray>"
			),
			List.of(
				item(Material.IRON_PICKAXE, 1),
				item(Material.STONE_SWORD, 1),
				item(Material.WATER_BUCKET, 1),
				item(Material.COOKED_BEEF, 12),
				item(Material.TORCH, 32)
			)
		);
	}

	private SpeedrunnerKitDefinition buildCombatKit() {
		return new SpeedrunnerKitDefinition(
			"combat",
			"Chiến đấu",
			Material.IRON_SWORD,
			List.of(
				"<gray>Tập trung cho giao tranh.</gray>",
				"<gray>Có khiên và đồ phòng thủ.</gray>"
			),
			List.of(
				item(Material.IRON_SWORD, 1),
				item(Material.SHIELD, 1),
				item(Material.IRON_HELMET, 1),
				item(Material.IRON_BOOTS, 1),
				item(Material.COOKED_PORKCHOP, 14)
			)
		);
	}

	private SpeedrunnerKitDefinition buildRangerKit() {
		return new SpeedrunnerKitDefinition(
			"ranger",
			UiColors.roleHunter(),
			Material.BOW,
			List.of(
				"<gray>Ưu tiên tầm xa và linh hoạt.</gray>",
				"<gray>Hỗ trợ giữ khoảng cách.</gray>"
			),
			List.of(
				item(Material.BOW, 1),
				item(Material.ARROW, 24),
				item(Material.LEATHER_HELMET, 1),
				item(Material.LEATHER_BOOTS, 1),
				item(Material.COOKED_CHICKEN, 16)
			)
		);
	}

	private ItemStack item(Material material, int amount) {
		return new ItemStack(material, amount);
	}
}
