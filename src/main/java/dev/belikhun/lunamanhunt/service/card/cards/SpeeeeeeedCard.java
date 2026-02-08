package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpeeeeeeedCard extends BaseCard {
	public SpeeeeeeedCard() {
		super("speed_frenzy", "Tốc độ điên cuồng", CardType.EFFECT, CardRarity.COMMON, 1,
			"Tốc độ II trong 1 phút.", Material.SUGAR);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyPotionToSpeedrunner(new PotionEffect(PotionEffectType.SPEED, 20 * 60, 1));
	}
}
