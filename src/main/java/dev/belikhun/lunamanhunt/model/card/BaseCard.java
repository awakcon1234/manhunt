package dev.belikhun.lunamanhunt.model.card;

import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;

public abstract class BaseCard implements CardDefinition {
	private final String id;
	private final String name;
	private final CardType type;
	private final CardRarity rarity;
	private final int cost;
	private final String description;
	private final Material icon;

	protected BaseCard(String id, String name, CardType type, CardRarity rarity, int cost, String description, Material icon) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.rarity = rarity;
		this.cost = cost;
		this.description = description;
		this.icon = icon;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public CardType getType() {
		return type;
	}

	@Override
	public CardRarity getRarity() {
		return rarity;
	}

	@Override
	public int getCost() {
		return cost;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public Material getIcon() {
		return icon;
	}

	@Override
	public abstract boolean apply(CardContext context);
}
