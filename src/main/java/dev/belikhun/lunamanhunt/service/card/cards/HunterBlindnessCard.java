package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import dev.belikhun.lunamanhunt.util.UiColors;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HunterBlindnessCard extends BaseCard {
	public HunterBlindnessCard() {
		super("hunter_blindness", "Mù tạm thời", CardType.EFFECT, CardRarity.RARE, 3,
			"Làm mù " + UiColors.hunter("thợ săn") + " trong 6 giây.", Material.INK_SAC, 6);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyPotionToHunters(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 6, 0));
	}

	@Override
	public boolean clear(CardContext context) {
		return context.clearPotionFromHunters(PotionEffectType.BLINDNESS);
	}
}
