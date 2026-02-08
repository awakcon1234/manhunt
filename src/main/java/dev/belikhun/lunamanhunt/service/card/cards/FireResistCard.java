package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FireResistCard extends BaseCard {
	public FireResistCard() {
		super("fire_resist", "Kháng lửa", CardType.POWER_UP, CardRarity.UNCOMMON, 2,
			"Miễn nhiễm lửa trong 60 giây.", Material.BLAZE_POWDER, 60);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyPotionToSpeedrunner(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 60, 0));
	}

	@Override
	public boolean clear(CardContext context) {
		return context.clearPotionFromSpeedrunner(PotionEffectType.FIRE_RESISTANCE);
	}
}
