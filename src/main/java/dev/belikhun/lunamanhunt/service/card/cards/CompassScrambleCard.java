package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import dev.belikhun.lunamanhunt.service.compass.CompassService;
import dev.belikhun.lunamanhunt.util.UiColors;
import org.bukkit.Material;

public class CompassScrambleCard extends BaseCard {
	public CompassScrambleCard() {
		super("compass_scramble", "Không thể tìm thấy", CardType.EFFECT, CardRarity.RARE, 2,
			"Làm nhiễu la bàn " + UiColors.roleHunterLower() + " trong 1 phút.", Material.ENDER_PEARL, 60);
	}

	@Override
	public boolean apply(CardContext context) {
		CompassService compassService = context.getCompassService();
		Match match = context.getMatch();
		if (compassService == null || match == null) {
			return false;
		}
		compassService.scrambleHunters(match, 20L * 60L);
		return true;
	}

	@Override
	public boolean clear(CardContext context) {
		CompassService compassService = context.getCompassService();
		Match match = context.getMatch();
		if (compassService == null || match == null) {
			return false;
		}
		compassService.clearScramble(match);
		return true;
	}
}
