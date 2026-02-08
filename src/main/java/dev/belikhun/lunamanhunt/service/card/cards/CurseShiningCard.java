package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CurseShiningCard extends BaseCard {
	public CurseShiningCard() {
		super("shining", "Phát sáng", CardType.CURSE, CardRarity.COMMON, 1,
			"Phát sáng thợ săn trong 2 phút.", Material.GLOW_INK_SAC);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyPotionToHunters(new PotionEffect(PotionEffectType.GLOWING, 20 * 120, 0));
	}
}
