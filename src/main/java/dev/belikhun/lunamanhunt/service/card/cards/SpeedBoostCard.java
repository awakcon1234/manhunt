package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpeedBoostCard extends BaseCard {
	public SpeedBoostCard() {
		super("speed_boost", "Tăng tốc", CardType.POWER_UP, CardRarity.COMMON, 1,
			"Tăng tốc chạy trong 30 giây.", Material.SUGAR);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyPotionToSpeedrunner(new PotionEffect(PotionEffectType.SPEED, 20 * 30, 1));
	}
}
