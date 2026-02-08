package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CurseTooSlowCard extends BaseCard {
	public CurseTooSlowCard() {
		super("too_slow", "Quá chậm", CardType.CURSE, CardRarity.COMMON, 1,
			"Làm chậm thợ săn trong 1 phút.", Material.BLUE_ICE);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyPotionToHunters(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 1));
	}
}
