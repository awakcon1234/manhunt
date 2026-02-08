package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import dev.belikhun.lunamanhunt.util.Text;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public class RunningMinionCard extends BaseCard {
	public RunningMinionCard() {
		super("running_minion", "Minion chạy", CardType.EFFECT, CardRarity.RARE, 2,
			"Thu nhỏ cơ thể trong 2 phút.", Material.RABBIT_FOOT);
	}

	@Override
	public boolean apply(CardContext context) {
		Player speedrunner = context.getSpeedrunner();
		if (speedrunner == null) {
			return false;
		}
		Attribute scaleAttribute = resolveScaleAttribute();
		if (scaleAttribute == null) {
			Text.msg(speedrunner, "<red>❌</red> Không thể thay đổi kích thước lúc này.");
			return false;
		}
		AttributeInstance scale = speedrunner.getAttribute(scaleAttribute);
		if (scale == null) {
			Text.msg(speedrunner, "<red>❌</red> Không thể thay đổi kích thước lúc này.");
			return false;
		}
		double original = scale.getBaseValue();
		scale.setBaseValue(0.5D);
		context.runLater(() -> {
			AttributeInstance current = speedrunner.getAttribute(scaleAttribute);
			if (current != null) {
				current.setBaseValue(original);
			}
		}, 20L * 120L);
		return true;
	}

	private Attribute resolveScaleAttribute() {
		try {
			return Attribute.valueOf("GENERIC_SCALE");
		} catch (IllegalArgumentException ignored) {
		}
		try {
			return Attribute.valueOf("SCALE");
		} catch (IllegalArgumentException ignored) {
		}
		return null;
	}
}
