package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MoreLivesCard extends BaseCard {
	public MoreLivesCard() {
		super("more_lives", "Thêm 1 tim", CardType.POWER_UP, CardRarity.UNCOMMON, 1,
			"Tăng 1 tim trong 2 phút.", Material.GOLDEN_APPLE);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyPotionToSpeedrunner(new PotionEffect(PotionEffectType.HEALTH_BOOST, 20 * 120, 0));
	}
}
