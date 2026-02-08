package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CurseBrokenHandCard extends BaseCard {
	public CurseBrokenHandCard() {
		super("broken_hand", "Tay gãy", CardType.CURSE, CardRarity.UNCOMMON, 2,
			"Mỏi tay trong 30 giây.", Material.WOODEN_PICKAXE);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyPotionToHunters(new PotionEffect(PotionEffectType.MINING_FATIGUE, 20 * 30, 0));
	}
}
