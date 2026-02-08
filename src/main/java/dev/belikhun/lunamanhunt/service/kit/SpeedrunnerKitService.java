package dev.belikhun.lunamanhunt.service.kit;

import dev.belikhun.lunamanhunt.model.kit.SpeedrunnerKitDefinition;
import dev.belikhun.lunamanhunt.model.match.MatchSettings;
import dev.belikhun.lunamanhunt.util.ItemStackCodec;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpeedrunnerKitService {
	private final SpeedrunnerKitStore store;

	public SpeedrunnerKitService(SpeedrunnerKitStore store) {
		this.store = Objects.requireNonNull(store, "store");
	}

	public List<SpeedrunnerKitDefinition> getKits() {
		return store.getAll();
	}

	public SpeedrunnerKitDefinition getKit(String id) {
		return store.getById(id);
	}

	public SpeedrunnerKitDefinition getDefaultKit() {
		return store.getDefaultKit();
	}

	public String getDefaultKitId() {
		return store.getDefaultKitId();
	}

	public void applyKit(MatchSettings settings, String kitId) {
		if (settings == null) {
			return;
		}
		SpeedrunnerKitDefinition kit = kitId == null ? null : getKit(kitId);
		if (kit == null) {
			kit = getDefaultKit();
		}
		if (kit == null) {
			settings.setSpeedrunnerKitId(null);
			settings.setSpeedrunnerKit(List.of());
			return;
		}
		settings.setSpeedrunnerKitId(kit.getId());
		settings.setSpeedrunnerKit(ItemStackCodec.encodeList(cloneItems(kit.getItems())));
	}

	public List<ItemStack> resolveKitItems(MatchSettings settings) {
		if (settings == null) {
			return List.of();
		}
		SpeedrunnerKitDefinition kit = getKit(settings.getSpeedrunnerKitId());
		if (kit != null) {
			return cloneItems(kit.getItems());
		}
		List<String> legacy = settings.getSpeedrunnerKit();
		if (legacy != null && !legacy.isEmpty()) {
			return ItemStackCodec.decodeList(legacy);
		}
		return List.of();
	}

	private List<ItemStack> cloneItems(List<ItemStack> items) {
		if (items == null || items.isEmpty()) {
			return List.of();
		}
		List<ItemStack> clones = new ArrayList<>(items.size());
		for (ItemStack item : items) {
			if (item != null) {
				clones.add(item.clone());
			}
		}
		return clones;
	}
}
