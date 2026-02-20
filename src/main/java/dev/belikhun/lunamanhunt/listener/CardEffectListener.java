package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.service.card.CardService;
import dev.belikhun.lunamanhunt.util.Text;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CardEffectListener implements Listener {
	private static final String CARD_STUN = "stun";
	private static final String CARD_SPAWNIST = "spawnist";
	private static final String CARD_ADRENALINE = "adrenaline";
	private static final String CARD_IMMORTAL = "immortal";
	private static final String CARD_CURSE_PORTAL_SICKNESS = "interdimensional_sickness";
	private static final String CARD_CURSE_VEGAN = "vegan";

	private static final double STUN_CHANCE = 0.10;
	private static final int STUN_DURATION_TICKS = 20 * 5;
	private static final double ADRENALINE_HEALTH_THRESHOLD = 8.0;
	private static final int ADRENALINE_BUFF_TICKS = 20 * 8;
	private static final long ADRENALINE_COOLDOWN_MS = 20_000L;
	private static final int PORTAL_SICKNESS_TICKS = 20 * 8;

	private static final Set<Material> NON_VEGAN_FOODS = EnumSet.of(
		Material.BEEF,
		Material.COOKED_BEEF,
		Material.PORKCHOP,
		Material.COOKED_PORKCHOP,
		Material.CHICKEN,
		Material.COOKED_CHICKEN,
		Material.MUTTON,
		Material.COOKED_MUTTON,
		Material.RABBIT,
		Material.COOKED_RABBIT,
		Material.COD,
		Material.COOKED_COD,
		Material.SALMON,
		Material.COOKED_SALMON,
		Material.TROPICAL_FISH,
		Material.PUFFERFISH,
		Material.ROTTEN_FLESH,
		Material.SPIDER_EYE
	);

	private final CardService cardService;
	private final Random random = new Random();
	private final Set<UUID> spawnistGuard = ConcurrentHashMap.newKeySet();
	private final ConcurrentHashMap<UUID, Long> adrenalineCooldownUntil = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<UUID, Long> portalSicknessCooldownUntil = new ConcurrentHashMap<>();

	public CardEffectListener(CardService cardService) {
		this.cardService = Objects.requireNonNull(cardService, "cardService");
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (event == null) {
			return;
		}
		Player player = event.getPlayer();
		if (player == null) {
			return;
		}
		spawnistGuard.remove(player.getUniqueId());
		cardService.reapplyActiveCurseEffects(player);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerHit(EntityDamageByEntityEvent event) {
		if (event == null) {
			return;
		}
		if (!(event.getDamager() instanceof Player attacker)) {
			return;
		}
		if (!(event.getEntity() instanceof Player victim)) {
			return;
		}
		Match match = cardService.getRunningMatch(attacker);
		if (match == null || !cardService.isSpeedrunner(match, attacker) || !cardService.isHunter(match, victim)) {
			return;
		}
		if (!cardService.isCardActive(match, CARD_STUN)) {
			return;
		}
		if (random.nextDouble() >= STUN_CHANCE) {
			return;
		}
		victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, STUN_DURATION_TICKS, 6, false, true, true));
		victim.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, STUN_DURATION_TICKS, 2, false, true, true));
		victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, STUN_DURATION_TICKS, 1, false, true, true));
		Text.msg(victim, "<red>❌</red> Bạn bị <yellow>choáng</yellow> trong 5 giây!");
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (event == null) {
			return;
		}
		Entity entity = event.getEntity();
		if (!(entity instanceof Player player)) {
			return;
		}
		Match match = cardService.getRunningMatch(player);
		if (match == null || !cardService.isSpeedrunner(match, player)) {
			return;
		}

		if (cardService.isCardActive(match, CARD_IMMORTAL)) {
			event.setCancelled(true);
			return;
		}

		double finalDamage = event.getFinalDamage();
		double health = player.getHealth();
		if (health - finalDamage <= 0.0) {
			handleSpawnistRescue(player, match, event);
			return;
		}

		handleAdrenaline(player, match, health - finalDamage);
	}

	private void handleSpawnistRescue(Player player, Match match, EntityDamageEvent event) {
		UUID id = player.getUniqueId();
		if (!cardService.isCardActive(match, CARD_SPAWNIST)) {
			return;
		}
		if (spawnistGuard.contains(id)) {
			return;
		}
		if (!cardService.consumeSpawnistCharge(match)) {
			return;
		}
		spawnistGuard.add(id);
		event.setCancelled(true);
		player.setHealth(1.0D);
		player.setFireTicks(0);
		player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 1, false, true, true));
		player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 10, 1, false, true, true));
		player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 20, 0, false, true, true));
		player.playEffect(EntityEffect.PROTECTED_FROM_DEATH);
		player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
		Text.msg(player, "<green>✔</green> <yellow>Cơ hội thứ hai</yellow> đã kích hoạt!");
	}

	private void handleAdrenaline(Player player, Match match, double remainingHealth) {
		if (!cardService.isCardActive(match, CARD_ADRENALINE)) {
			return;
		}
		if (remainingHealth > ADRENALINE_HEALTH_THRESHOLD) {
			return;
		}
		UUID id = player.getUniqueId();
		long now = System.currentTimeMillis();
		long until = adrenalineCooldownUntil.getOrDefault(id, 0L);
		if (until > now) {
			return;
		}
		adrenalineCooldownUntil.put(id, now + ADRENALINE_COOLDOWN_MS);
		player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ADRENALINE_BUFF_TICKS, 0, false, true, true));
		player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ADRENALINE_BUFF_TICKS, 1, false, true, true));
		player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, ADRENALINE_BUFF_TICKS, 1, false, true, true));
		Text.msg(player, "<yellow>ℹ</yellow> <green>Adrenaline bùng nổ!</green>");
	}

	@EventHandler(ignoreCancelled = true)
	public void onPortal(PlayerPortalEvent event) {
		if (event == null || event.getPlayer() == null) {
			return;
		}
		handlePortalSickness(event.getPlayer());
	}

	@EventHandler(ignoreCancelled = true)
	public void onTeleport(PlayerTeleportEvent event) {
		if (event == null || event.getPlayer() == null) {
			return;
		}
		switch (event.getCause()) {
			case NETHER_PORTAL, END_PORTAL, END_GATEWAY -> handlePortalSickness(event.getPlayer());
			default -> {
			}
		}
	}

	private void handlePortalSickness(Player player) {
		UUID playerId = player.getUniqueId();
		long now = System.currentTimeMillis();
		long cooldownUntil = portalSicknessCooldownUntil.getOrDefault(playerId, 0L);
		if (cooldownUntil > now) {
			return;
		}
		portalSicknessCooldownUntil.put(playerId, now + 1000L);

		Match match = cardService.getRunningMatch(player);
		if (match == null || !cardService.isHunter(match, player)) {
			return;
		}
		if (!cardService.isCardActive(match, CARD_CURSE_PORTAL_SICKNESS)) {
			return;
		}
		player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, PORTAL_SICKNESS_TICKS, 0, false, true, true));
		player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, PORTAL_SICKNESS_TICKS, 0, false, true, true));
		Text.msg(player, "<red>❌</red> Bạn bị <yellow>say không gian</yellow> sau khi qua cổng!");
	}

	@EventHandler(ignoreCancelled = true)
	public void onConsume(PlayerItemConsumeEvent event) {
		if (event == null || event.getPlayer() == null || event.getItem() == null) {
			return;
		}
		Player player = event.getPlayer();
		Match match = cardService.getRunningMatch(player);
		if (match == null || !cardService.isHunter(match, player)) {
			return;
		}
		if (!cardService.isCardActive(match, CARD_CURSE_VEGAN)) {
			return;
		}
		Material food = event.getItem().getType();
		if (!NON_VEGAN_FOODS.contains(food)) {
			return;
		}
		event.setCancelled(true);
		Text.msg(player, "<red>❌</red> Lời nguyền <yellow>Thuần chay</yellow>: bạn không thể ăn món này!");
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
	}
}
