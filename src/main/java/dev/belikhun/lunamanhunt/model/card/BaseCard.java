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
	private final boolean timed;
	private final int durationSeconds;

	protected BaseCard(String id, String name, CardType type, CardRarity rarity, int cost, String description, Material icon) {
		this(id, name, type, rarity, cost, description, icon, 0);
	}

	protected BaseCard(String id, String name, CardType type, CardRarity rarity, int cost, String description, Material icon,
			int durationSeconds) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.rarity = rarity;
		this.cost = cost;
		this.description = description;
		this.icon = icon;
		this.durationSeconds = Math.max(0, durationSeconds);
		this.timed = this.durationSeconds > 0;
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
	public boolean isTimed() {
		return timed;
	}

	@Override
	public int getDurationSeconds() {
		return durationSeconds;
	}

	@Override
	public abstract boolean apply(CardContext context);

	@Override
	public boolean clear(CardContext context) {
		return true;
	}
}
