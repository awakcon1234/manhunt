package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CurseSonicCard extends BaseCard {
	public CurseSonicCard() {
		super("sonic", "Không muốn làm Sonic", CardType.CURSE, CardRarity.RARE, 2,
			"Tốc độ 100 trong 30 giây.", Material.SUGAR, 30);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyPotionToHunters(new PotionEffect(PotionEffectType.SPEED, 20 * 30, 99));
	}

	@Override
	public boolean clear(CardContext context) {
		return context.clearPotionFromHunters(PotionEffectType.SPEED);
	}
}
