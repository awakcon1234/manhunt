package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class LavaSwimmerCard extends BaseCard {
	public LavaSwimmerCard() {
		super("lava_swimmer", "Bơi trong dung nham", CardType.EFFECT, CardRarity.COMMON, 1,
			"Kháng lửa trong 1 phút.", Material.MAGMA_CREAM);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyPotionToSpeedrunner(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 60, 0));
	}
}
