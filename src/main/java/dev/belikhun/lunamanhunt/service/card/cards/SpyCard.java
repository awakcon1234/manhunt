package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import dev.belikhun.lunamanhunt.util.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.StructureType;

public class SpyCard extends BaseCard {
	public SpyCard() {
		super("spy", "Điệp viên", CardType.POWER_UP, CardRarity.RARE, 2,
			"Tọa độ pháo đài gần nhất.", Material.COMPASS);
	}

	@Override
	public boolean apply(CardContext context) {
		Player speedrunner = context.getSpeedrunner();
		if (speedrunner == null) {
			return false;
		}
		World world = speedrunner.getWorld();
		Location location = findStronghold(world, speedrunner.getLocation());
		if (location == null) {
			Text.msg(speedrunner, "<red>❌</red> Không tìm thấy pháo đài gần đây.");
			return false;
		}
		Text.msg(speedrunner, "<yellow>ℹ</yellow> Pháo đài gần nhất tại <white>"
			+ location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "</white>.");
		return true;
	}

	private Location findStronghold(World world, Location from) {
		try {
			java.lang.reflect.Method method = World.class.getMethod(
				"locateNearestStructure", Location.class, StructureType.class, int.class, boolean.class);
			Object result = method.invoke(world, from, StructureType.STRONGHOLD, 200, false);
			if (result == null) {
				return null;
			}
			if (result instanceof Location location) {
				return location;
			}
			java.lang.reflect.Method getLocation = result.getClass().getMethod("getLocation");
			Object location = getLocation.invoke(result);
			return location instanceof Location resultLocation ? resultLocation : null;
		} catch (ReflectiveOperationException ex) {
			return null;
		}
	}
}
