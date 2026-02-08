package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HunterSlownessCard extends BaseCard {
	public HunterSlownessCard() {
		super("hunter_slowness", "Làm chậm thợ săn", CardType.EFFECT, CardRarity.COMMON, 1,
			"Làm chậm thợ săn trong 12 giây.", Material.SNOWBALL);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyPotionToHunters(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 12, 1));
	}
}
