package dev.belikhun.lunamanhunt.model.kit;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

public class SpeedrunnerKitDefinition {
	private final String id;
	private final String name;
	private final Material icon;
	private final List<String> description;
	private final List<ItemStack> items;

	public SpeedrunnerKitDefinition(String id, String name, Material icon, List<String> description,
			List<ItemStack> items) {
		this.id = Objects.requireNonNull(id, "id");
		this.name = Objects.requireNonNull(name, "name");
		this.icon = Objects.requireNonNull(icon, "icon");
		this.description = List.copyOf(description == null ? List.of() : description);
		this.items = List.copyOf(items == null ? List.of() : items);
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Material getIcon() {
		return icon;
	}

	public List<String> getDescription() {
		return description;
	}

	public List<ItemStack> getItems() {
		return items;
	}
}
