package dev.belikhun.lunamanhunt.model.card;

import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;

public interface CardDefinition {
	String getId();

	String getName();

	CardType getType();

	CardRarity getRarity();

	int getCost();

	String getDescription();

	Material getIcon();

	boolean isTimed();

	int getDurationSeconds();

	boolean apply(CardContext context);

	boolean clear(CardContext context);
}
