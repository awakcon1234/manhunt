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
			"Thu nhỏ cơ thể trong 2 phút.", Material.RABBIT_FOOT, 120);
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
		scale.setBaseValue(0.5D);
		return true;
	}

	@Override
	public boolean clear(CardContext context) {
		Player speedrunner = context.getSpeedrunner();
		if (speedrunner == null) {
			return false;
		}
		Attribute scaleAttribute = resolveScaleAttribute();
		if (scaleAttribute == null) {
			return false;
		}
		AttributeInstance scale = speedrunner.getAttribute(scaleAttribute);
		if (scale == null) {
			return false;
		}
		scale.setBaseValue(1.0D);
		return true;
	}

	private Attribute resolveScaleAttribute() {
		Attribute attribute = findAttribute("GENERIC_SCALE");
		if (attribute != null) {
			return attribute;
		}
		return findAttribute("SCALE");
	}

	private Attribute findAttribute(String name) {
		try {
			java.lang.reflect.Field field = Attribute.class.getField(name);
			Object value = field.get(null);
			return value instanceof Attribute attribute ? attribute : null;
		} catch (ReflectiveOperationException ignored) {
			return null;
		}
	}
}
