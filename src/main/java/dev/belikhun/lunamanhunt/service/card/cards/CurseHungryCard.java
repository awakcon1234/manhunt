package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CurseHungryCard extends BaseCard {
	public CurseHungryCard() {
		super("hungry", "Đói lả", CardType.CURSE, CardRarity.UNCOMMON, 2,
			"Gây đói trong 2 phút.", Material.ROTTEN_FLESH, 120);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyPotionToHunters(new PotionEffect(PotionEffectType.HUNGER, 20 * 120, 0));
	}

	@Override
	public boolean clear(CardContext context) {
		return context.clearPotionFromHunters(PotionEffectType.HUNGER);
	}
}
