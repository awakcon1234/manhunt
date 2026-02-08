package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class RegenCard extends BaseCard {
	public RegenCard() {
		super("regen", "Hồi phục", CardType.POWER_UP, CardRarity.UNCOMMON, 2,
			"Hồi máu trong 10 giây.", Material.GHAST_TEAR, 10);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyPotionToSpeedrunner(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 0));
	}

	@Override
	public boolean clear(CardContext context) {
		return context.clearPotionFromSpeedrunner(PotionEffectType.REGENERATION);
	}
}
