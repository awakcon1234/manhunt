package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CurseBlindCard extends BaseCard {
	public CurseBlindCard() {
		super("blind", "Không thấy", CardType.CURSE, CardRarity.UNCOMMON, 2,
			"Làm mù thợ săn trong 10 giây.", Material.INK_SAC, 10);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyPotionToHunters(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 10, 0));
	}

	@Override
	public boolean clear(CardContext context) {
		return context.clearPotionFromHunters(PotionEffectType.BLINDNESS);
	}
}
