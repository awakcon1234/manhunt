package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import dev.belikhun.lunamanhunt.util.UiColors;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Random;

public class CurseZombieOutbreakCard extends BaseCard {
	private final Random random = new Random();

	public CurseZombieOutbreakCard() {
		super("zombie_outbreak", "Bầy zombie", CardType.CURSE, CardRarity.EPIC, 3,
			"Triệu hồi bầy zombie quanh " + UiColors.hunter("thợ săn") + ".", Material.ZOMBIE_SPAWN_EGG);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyToHunters(this::spawnZombies);
	}

	private void spawnZombies(Player target) {
		World world = target.getWorld();
		Location base = target.getLocation();
		for (int i = 0; i < 4; i++) {
			double offsetX = random.nextInt(7) - 3;
			double offsetZ = random.nextInt(7) - 3;
			Location spawn = base.clone().add(offsetX, 0, offsetZ);
			world.spawnEntity(spawn, EntityType.ZOMBIE);
		}
	}
}
