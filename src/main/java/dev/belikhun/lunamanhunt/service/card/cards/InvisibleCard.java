package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class InvisibleCard extends BaseCard {
	public InvisibleCard() {
		super("invisible", "Tàng hình", CardType.EFFECT, CardRarity.UNCOMMON, 2,
			"Tàng hình trong 1 phút.", Material.POTION);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyPotionToSpeedrunner(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 60, 0));
	}
}
