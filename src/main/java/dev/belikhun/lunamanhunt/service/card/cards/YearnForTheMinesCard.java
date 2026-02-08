package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class YearnForTheMinesCard extends BaseCard {
	public YearnForTheMinesCard() {
		super("yearn_for_mines", "Nhớ mỏ", CardType.EFFECT, CardRarity.COMMON, 1,
			"Đào nhanh trong 1 phút.", Material.IRON_PICKAXE);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyPotionToSpeedrunner(new PotionEffect(PotionEffectType.HASTE, 20 * 60, 0));
	}
}
