package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import dev.belikhun.lunamanhunt.util.UiColors;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HunterSlownessCard extends BaseCard {
	public HunterSlownessCard() {
		super("hunter_slowness", "Làm chậm " + UiColors.roleHunterLower(),
			CardType.EFFECT, CardRarity.COMMON, 1,
			"Làm chậm " + UiColors.roleHunterLower() + " trong 12 giây.", Material.SNOWBALL, 12);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyPotionToHunters(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 12, 1));
	}

	@Override
	public boolean clear(CardContext context) {
		return context.clearPotionFromHunters(PotionEffectType.SLOWNESS);
	}
}
