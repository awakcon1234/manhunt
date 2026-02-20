package dev.belikhun.lunamanhunt.service.card;

import dev.belikhun.lunamanhunt.model.card.CardDefinition;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.cards.BlazeRodCard;
import dev.belikhun.lunamanhunt.service.card.cards.CompassScrambleCard;
import dev.belikhun.lunamanhunt.service.card.cards.CurseInterdimensionalSicknessCard;
import dev.belikhun.lunamanhunt.service.card.cards.CurseBlindCard;
import dev.belikhun.lunamanhunt.service.card.cards.CurseBrokenHandCard;
import dev.belikhun.lunamanhunt.service.card.cards.CurseDizzyCard;
import dev.belikhun.lunamanhunt.service.card.cards.CurseHungryCard;
import dev.belikhun.lunamanhunt.service.card.cards.CurseShiningCard;
import dev.belikhun.lunamanhunt.service.card.cards.CurseSonicCard;
import dev.belikhun.lunamanhunt.service.card.cards.CurseTooSlowCard;
import dev.belikhun.lunamanhunt.service.card.cards.CurseVeganCard;
import dev.belikhun.lunamanhunt.service.card.cards.CurseZombieOutbreakCard;
import dev.belikhun.lunamanhunt.service.card.cards.AdrenalineCard;
import dev.belikhun.lunamanhunt.service.card.cards.EnderEyeCard;
import dev.belikhun.lunamanhunt.service.card.cards.FireResistCard;
import dev.belikhun.lunamanhunt.service.card.cards.HulkCard;
import dev.belikhun.lunamanhunt.service.card.cards.HunterBlindnessCard;
import dev.belikhun.lunamanhunt.service.card.cards.HunterSlownessCard;
import dev.belikhun.lunamanhunt.service.card.cards.ImmortalCard;
import dev.belikhun.lunamanhunt.service.card.cards.InvisibleCard;
import dev.belikhun.lunamanhunt.service.card.cards.LavaSwimmerCard;
import dev.belikhun.lunamanhunt.service.card.cards.MoreLivesCard;
import dev.belikhun.lunamanhunt.service.card.cards.RegenCard;
import dev.belikhun.lunamanhunt.service.card.cards.ResistanceCard;
import dev.belikhun.lunamanhunt.service.card.cards.RunningMinionCard;
import dev.belikhun.lunamanhunt.service.card.cards.SpawnistCard;
import dev.belikhun.lunamanhunt.service.card.cards.SpeedBoostCard;
import dev.belikhun.lunamanhunt.service.card.cards.SpeeeeeeedCard;
import dev.belikhun.lunamanhunt.service.card.cards.StunCard;
import dev.belikhun.lunamanhunt.service.card.cards.SpyCard;
import dev.belikhun.lunamanhunt.service.card.cards.YearnForTheMinesCard;
import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.service.compass.CompassService;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.point.PointService;
import dev.belikhun.lunamanhunt.util.Text;
import dev.belikhun.lunamanhunt.util.UiColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CardService {
	private static final int POOL_COUNT = 7;
	private static final int MAX_SLOTS = 3;

	private final JavaPlugin plugin;
	private final LobbyManager lobbyManager;
	private final CompassService compassService;
	private final PointService pointService;
	private final List<CardDefinition> cardPool;
	private final Random random = new Random();
	private final Map<String, List<ActiveCard>> activeCards = new ConcurrentHashMap<>();
	private final Map<String, Integer> spawnistCharges = new ConcurrentHashMap<>();

	public CardService(JavaPlugin plugin, LobbyManager lobbyManager, CompassService compassService, PointService pointService) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.compassService = Objects.requireNonNull(compassService, "compassService");
		this.pointService = Objects.requireNonNull(pointService, "pointService");
		this.cardPool = buildDefaultPool();
	}

	public List<CardDefinition> getCardPool() {
		return cardPool;
	}

	public JavaPlugin getPlugin() {
		return plugin;
	}

	public List<String> getAllCardIds() {
		List<String> ids = new ArrayList<>(cardPool.size());
		for (CardDefinition card : cardPool) {
			ids.add(card.getId());
		}
		return ids;
	}

	public List<String> getEnabledCardIds(Match match) {
		if (match == null) {
			return List.of();
		}
		ensureEnabledCards(match);
		return List.copyOf(match.getSettings().getEnabledCardIds());
	}

	public List<CardDefinition> getEnabledCards(Match match) {
		if (match == null) {
			return List.of();
		}
		ensureEnabledCards(match);
		List<String> enabled = match.getSettings().getEnabledCardIds();
		List<CardDefinition> result = new ArrayList<>();
		for (CardDefinition card : cardPool) {
			if (enabled.contains(card.getId())) {
				result.add(card);
			}
		}
		return result;
	}

	public boolean setCardEnabled(Match match, String cardId, boolean enabled) {
		if (match == null || cardId == null || cardId.isBlank()) {
			return false;
		}
		ensureEnabledCards(match);
		String normalized = normalizeCardId(cardId);
		CardDefinition card = getCardById(normalized);
		if (card == null) {
			return false;
		}
		List<String> enabledIds = match.getSettings().getEnabledCardIds();
		if (enabled && !enabledIds.contains(card.getId())) {
			enabledIds.add(card.getId());
			return true;
		}
		if (!enabled && enabledIds.remove(card.getId())) {
			return true;
		}
		return false;
	}

	public CardDefinition getCardById(String cardId) {
		if (cardId == null) {
			return null;
		}
		String normalized = normalizeCardId(cardId);
		for (CardDefinition card : cardPool) {
			if (card.getId().equalsIgnoreCase(normalized)) {
				return card;
			}
		}
		return null;
	}

	public int getPoolCount() {
		return POOL_COUNT;
	}

	public int getMaxSlots() {
		return MAX_SLOTS;
	}

	public List<CardDefinition> getDrawnCards(Match match) {
		if (match == null) {
			return List.of();
		}
		List<CardDefinition> source = match.getDrawnCards();
		if (source == null || source.isEmpty()) {
			return new ArrayList<>();
		}
		List<CardDefinition> safe = new ArrayList<>(source);
		return safe;
	}

	public List<CardDefinition> getKeptCards(Match match) {
		if (match == null) {
			return List.of();
		}
		return new ArrayList<>(ensureCardSlots(match));
	}

	public List<ActiveCard> getActiveCards(Match match) {
		if (match == null) {
			return List.of();
		}
		List<ActiveCard> entries = activeCards.get(match.getId());
		if (entries == null || entries.isEmpty()) {
			return List.of();
		}
		return List.copyOf(entries);
	}

	public void clearActiveCards(Match match) {
		if (match == null) {
			return;
		}
		spawnistCharges.remove(match.getId());
		List<ActiveCard> entries = activeCards.get(match.getId());
		if (entries == null || entries.isEmpty()) {
			return;
		}
		for (ActiveCard entry : List.copyOf(entries)) {
			clearActiveCard(entry);
		}
	}

	public int getPoints(Match match) {
		return match == null ? 0 : match.getSpeedrunnerPoints();
	}

	public void addPoint(Match match, int amount) {
		if (match == null) {
			return;
		}
		int next = Math.max(0, match.getSpeedrunnerPoints() + amount);
		match.setSpeedrunnerPoints(next);
	}

	public void drawNewCards(Match match) {
		if (match == null) {
			return;
		}
		List<CardDefinition> draw = drawRandomCards(match, POOL_COUNT);
		match.getDrawnCards().clear();
		match.getDrawnCards().addAll(draw);
	}

	public boolean canUseCard(Match match, CardDefinition card) {
		if (match == null || card == null) {
			return false;
		}
		return match.getSpeedrunnerPoints() >= card.getCost();
	}

	public boolean useKeptCard(Player player, Match match, int slotIndex) {
		if (player == null || match == null) {
			return false;
		}
		List<CardDefinition> slots = ensureCardSlots(match);
		if (slotIndex < 0 || slotIndex >= slots.size()) {
			return false;
		}
		CardDefinition card = slots.get(slotIndex);
		if (card == null) {
			Text.msg(player, "<red>❌</red> Ô thẻ này đang trống.");
			return false;
		}
		if (!applyCard(player, match, card)) {
			return false;
		}
		slots.set(slotIndex, null);
		return true;
	}

	public boolean usePoolCard(Player player, Match match, int poolIndex) {
		if (player == null || match == null) {
			return false;
		}
		if (match.getState() != MatchState.RUNNING) {
			Text.msg(player, "<red>❌</red> Chỉ dùng thẻ khi trận đang diễn ra.");
			return false;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		if (speedrunnerId == null || !speedrunnerId.equals(player.getUniqueId())) {
			Text.msg(player, "<red>❌</red> Chỉ " + UiColors.roleSpeedrunner() + " mới được dùng thẻ.");
			return false;
		}
		List<CardDefinition> pool = match.getDrawnCards();
		if (poolIndex < 0 || poolIndex >= pool.size()) {
			return false;
		}
		CardDefinition card = pool.get(poolIndex);
		if (card == null) {
			return false;
		}
		if (!applyCard(player, match, card)) {
			return false;
		}
		pool.set(poolIndex, null);
		return true;
	}

	public boolean keepCardFromPool(Player player, Match match, int poolIndex) {
		if (player == null || match == null) {
			return false;
		}
		if (match.getState() != MatchState.RUNNING) {
			Text.msg(player, "<red>❌</red> Chỉ chọn thẻ khi trận đang diễn ra.");
			return false;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		if (speedrunnerId == null || !speedrunnerId.equals(player.getUniqueId())) {
			Text.msg(player, "<red>❌</red> Chỉ " + UiColors.roleSpeedrunner() + " mới được giữ thẻ.");
			return false;
		}
		List<CardDefinition> pool = match.getDrawnCards();
		if (poolIndex < 0 || poolIndex >= pool.size()) {
			return false;
		}
		CardDefinition card = pool.get(poolIndex);
		if (card == null) {
			return false;
		}
		List<CardDefinition> slots = ensureCardSlots(match);
		if (slots.contains(card)) {
			Text.msg(player, "<yellow>ℹ</yellow> Bạn đã giữ thẻ này rồi.");
			return false;
		}
		int emptyIndex = slots.indexOf(null);
		if (emptyIndex < 0) {
			Text.msg(player, "<red>❌</red> Ô thẻ đã đầy, hãy bỏ bớt trước.");
			return false;
		}
		slots.set(emptyIndex, card);
		pool.set(poolIndex, null);
		Text.msg(player, "<green>✔</green> Đã giữ thẻ <yellow>" + card.getName() + "</yellow>.");
		player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.1f);
		return true;
	}

	public boolean discardKeptCard(Player player, Match match, int slotIndex) {
		if (player == null || match == null) {
			return false;
		}
		if (match.getState() != MatchState.RUNNING) {
			Text.msg(player, "<red>❌</red> Chỉ bỏ thẻ khi trận đang diễn ra.");
			return false;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		if (speedrunnerId == null || !speedrunnerId.equals(player.getUniqueId())) {
			Text.msg(player, "<red>❌</red> Chỉ " + UiColors.roleSpeedrunner() + " mới được bỏ thẻ.");
			return false;
		}
		List<CardDefinition> slots = ensureCardSlots(match);
		if (slotIndex < 0 || slotIndex >= slots.size()) {
			return false;
		}
		CardDefinition card = slots.get(slotIndex);
		if (card == null) {
			Text.msg(player, "<red>❌</red> Ô thẻ này đang trống.");
			return false;
		}
		slots.set(slotIndex, null);
		Text.msg(player, "<green>✔</green> Đã bỏ thẻ <yellow>" + card.getName() + "</yellow>.");
		player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.9f);
		return true;
	}

	private boolean applyCard(Player player, Match match, CardDefinition card) {
		if (player == null || match == null || card == null) {
			return false;
		}
		if (match.getState() != MatchState.RUNNING) {
			Text.msg(player, "<red>❌</red> Chỉ dùng thẻ khi trận đang diễn ra.");
			return false;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		if (speedrunnerId == null || !speedrunnerId.equals(player.getUniqueId())) {
			Text.msg(player, "<red>❌</red> Chỉ " + UiColors.roleSpeedrunner() + " mới được dùng thẻ.");
			return false;
		}
		if (!canUseCard(match, card)) {
			Text.msg(player, "<red>❌</red> Bạn không đủ điểm để dùng thẻ này.");
			return false;
		}
		CardContext context = new CardContext(plugin, match, player, lobbyManager, compassService, this);
		if (!card.apply(context)) {
			return false;
		}
		if (card.getType() == CardType.CURSE) {
			broadcastCurseUsed(match, player, card);
		}
		trackActiveCard(match, card);
		match.getStats().incrementCardsUsed();
		match.setSpeedrunnerPoints(match.getSpeedrunnerPoints() - card.getCost());
		Text.msg(player, "<green>✔</green> Đã dùng thẻ <yellow>" + card.getName() + "</yellow>.");
		player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
		return true;
	}

	public void reapplyActiveCurseEffects(Player hunter) {
		if (hunter == null) {
			return;
		}
		Bukkit.getScheduler().runTaskLater(plugin, () -> applyActiveCurseEffects(hunter), 1L);
	}

	private void applyActiveCurseEffects(Player hunter) {
		if (hunter == null) {
			return;
		}
		Match match = lobbyManager.getMatchForPlayer(hunter.getUniqueId());
		if (match == null || match.getState() != MatchState.RUNNING) {
			return;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		if (speedrunnerId != null && speedrunnerId.equals(hunter.getUniqueId())) {
			return;
		}
		List<ActiveCard> entries = activeCards.get(match.getId());
		if (entries == null || entries.isEmpty()) {
			return;
		}
		long now = System.currentTimeMillis();
		for (ActiveCard entry : List.copyOf(entries)) {
			CardDefinition card = entry.card();
			if (card == null || card.getType() != CardType.CURSE || !card.isTimed()) {
				continue;
			}
			long remainingMs = entry.expiresAtMillis() - now;
			if (remainingMs <= 0L) {
				continue;
			}
			int remainingSeconds = (int) Math.max(1L, (remainingMs + 999L) / 1000L);
			applyCursePotion(hunter, card, remainingSeconds);
		}
	}

	public void handleSpeedrunnerAdvancement(Player player) {
		if (player == null) {
			return;
		}
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null || match.getState() != MatchState.RUNNING) {
			return;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		if (speedrunnerId == null || !speedrunnerId.equals(player.getUniqueId())) {
			return;
		}
		int gainedPoints = resolveAdvancementPoints(match);
		addPoint(match, gainedPoints);
		drawNewCards(match);
		Text.msg(player, "<green>✔</green> Bạn nhận được " + pointService.formatPoints(gainedPoints)
			+ " từ thành tựu. Hiện có: " + pointService.formatPoints(match.getSpeedrunnerPoints()) + ". <gray>(" + UiColors.command("/c") + " để sử dụng điểm)</gray>");
		Text.msg(player, "<yellow>ℹ</yellow> <green>Đã mở " + POOL_COUNT + " thẻ mới</green>, hãy giữ vào ô thẻ.");
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.3f);
	}

	private int resolveAdvancementPoints(Match match) {
		if (match == null) {
			return 1;
		}
		int basePoints = cfgInt("balance.cards.advancement.basePoints", 1);
		int extraPerStep = cfgInt("balance.cards.advancement.extraPointsPerStep", 1);
		int huntersPerStep = Math.max(1, cfgInt("balance.cards.advancement.huntersPerStep", 2));
		int maxPoints = Math.max(basePoints, cfgInt("balance.cards.advancement.maxPoints", 5));

		int hunters = Math.max(0, match.getHunters().size());
		int extraSteps = Math.max(0, (hunters - 1) / huntersPerStep);
		int scaled = basePoints + (extraSteps * Math.max(0, extraPerStep));
		return Math.max(1, Math.min(maxPoints, scaled));
	}

	public void sendCardList(Player player, Match match) {
		if (player == null || match == null) {
			return;
		}
		List<CardDefinition> cards = ensureCardSlots(match);
		boolean hasCard = false;
		for (CardDefinition card : cards) {
			if (card != null) {
				hasCard = true;
				break;
			}
		}
		if (!hasCard) {
			Text.msg(player, "<yellow>ℹ</yellow> Chưa có thẻ nào trong ô giữ.");
			return;
		}
		Text.msg(player, "<yellow>ℹ</yellow> Thẻ đang giữ:");
		for (int i = 0; i < cards.size(); i++) {
			CardDefinition card = cards.get(i);
			if (card == null) {
				continue;
			}
			Text.msg(player, "<yellow>●</yellow> <white>" + (i + 1) + ". " + card.getName()
				+ "</white> <gray>(" + pointService.formatPoints(card.getCost()) + ")</gray>");
		}
	}

	public ItemStack buildCardItem(CardDefinition card, int index, NamespacedKey actionKey, NamespacedKey targetKey,
			String actionValue) {
		return buildCardItemInternal(card, actionKey, targetKey, actionValue, index);
	}

	public ItemStack buildCardInfoItem(CardDefinition card) {
		return buildCardItemInternal(card, null, null, null, null);
	}

	private ItemStack buildCardItemInternal(CardDefinition card, NamespacedKey actionKey, NamespacedKey targetKey,
			String actionValue, Integer index) {
		Material material = card.getIcon() != null
			? card.getIcon()
			: (card.getType() == CardType.POWER_UP ? Material.GLOWSTONE_DUST : Material.FERMENTED_SPIDER_EYE);
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			String rarityColor = formatCardRarityColor(card.getRarity());
			String rarityIcon = formatCardRarityIcon(card.getRarity());
			String rarityColorClose = rarityColor.replace("<", "</");
			meta.displayName(mmNoItalic(rarityColor + rarityIcon + " " + card.getName() + rarityColorClose));
			List<String> lore = new ArrayList<>();
			lore.add("");
			lore.add("<gray>● <white>Loại thẻ: " + formatCardTypeDisplay(card.getType()) + "</white>");
			lore.add("<gray>● <white>Độ hiếm: " + formatCardRarityDisplay(card.getRarity()) + "</white>");
			lore.add("<gray>● <white>Chi phí: " + pointService.formatPoints(card.getCost()) + "</white>");
			if (card.isTimed()) {
				lore.add("<gray>● <white>Thời lượng: " + formatCardDuration(card.getDurationSeconds()) + "</white>");
			}
			lore.add("");
			List<String> descriptionLines = wrapCardDescription(card.getDescription());
			lore.add("<color:#36a8ff>▍</color> <white>" + descriptionLines.get(0) + "</white>");
			if (descriptionLines.size() > 1) {
				lore.add("<color:#36a8ff>▍</color> <white>" + descriptionLines.get(1) + "</white>");
			}
			if ("card_keep".equals(actionValue)) {
				lore.add("");
				lore.add("<yellow>ℹ</yellow> <gray>Chuột trái:</gray> <white>dùng ngay.</white>");
				lore.add("<yellow>ℹ</yellow> <gray>Chuột phải:</gray> <white>giữ vào ô thẻ.</white>");
			}
			lore.add("");
			lore.add("<dark_gray>#card/" + card.getId() + "</dark_gray>");
			meta.lore(mmNoItalic(lore));
			if (actionKey != null && actionValue != null) {
				meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, actionValue);
				if (targetKey != null && index != null) {
					meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, String.valueOf(index));
				}
			}
			item.setItemMeta(meta);
		}
		return item;
	}

	public String formatCardTypeDisplay(CardType type) {
		if (type == null) {
			return "Không rõ";
		}
		return switch (type) {
			case POWER_UP -> "<#7fd36b>Hỗ trợ</#7fd36b>";
			case EFFECT -> "<#36a8ff>Hiệu ứng</#36a8ff>";
			case CURSE -> "<#ff6b6b>Lời nguyền</#ff6b6b>";
		};
	}

	public String formatCardRarityDisplay(CardRarity rarity) {
		if (rarity == null) {
			return "Không rõ";
		}
		return switch (rarity) {
			case COMMON -> "<#9aa0a6>Thường</#9aa0a6>";
			case UNCOMMON -> "<#6ccf7d>Hiếm</#6ccf7d>";
			case RARE -> "<#4da3ff>Rất hiếm</#4da3ff>";
			case EPIC -> "<#d37bff>Sử thi</#d37bff>";
			case LEGENDARY -> "<#ffcc4d>Huyền thoại</#ffcc4d>";
		};
	}

	public String formatCardRarityColor(CardRarity rarity) {
		if (rarity == null) {
			return "<white>";
		}
		return switch (rarity) {
			case COMMON -> "<#9aa0a6>";
			case UNCOMMON -> "<#6ccf7d>";
			case RARE -> "<#4da3ff>";
			case EPIC -> "<#d37bff>";
			case LEGENDARY -> "<#ffcc4d>";
		};
	}

	public String formatCardRarityIcon(CardRarity rarity) {
		if (rarity == null) {
			return "▲";
		}
		return switch (rarity) {
			case COMMON -> "▲";
			case UNCOMMON -> "■";
			case RARE -> "◆";
			case EPIC -> "⬟";
			case LEGENDARY -> "⬢";
		};
	}

	public List<String> wrapCardDescription(String description) {
		String text = description == null ? "" : description.trim();
		if (text.isEmpty()) {
			return List.of(" ");
		}
		int max = 36;
		if (text.length() <= max) {
			return List.of(text);
		}
		int split = text.lastIndexOf(' ', max);
		if (split <= 0 || split >= text.length() - 1) {
			return List.of(text.substring(0, Math.min(max, text.length())), text.substring(Math.min(max, text.length())));
		}
		String first = text.substring(0, split).trim();
		String second = text.substring(split + 1).trim();
		return List.of(first, second);
	}

	private void trackActiveCard(Match match, CardDefinition card) {
		if (match == null || card == null || !card.isTimed()) {
			return;
		}
		int durationSeconds = card.getDurationSeconds();
		if (durationSeconds <= 0) {
			return;
		}
		long now = System.currentTimeMillis();
		ActiveCard entry = new ActiveCard(match, card, now, now + (durationSeconds * 1000L));
		activeCards.computeIfAbsent(match.getId(), key -> new ArrayList<>()).add(entry);
		Bukkit.getScheduler().runTaskLater(plugin, () -> clearActiveCard(entry), durationSeconds * 20L);
	}

	private void clearActiveCard(ActiveCard entry) {
		if (entry == null || entry.match() == null) {
			return;
		}
		Match match = entry.match();
		List<ActiveCard> entries = activeCards.get(match.getId());
		if (entries == null || !entries.remove(entry)) {
			return;
		}
		if (entries.isEmpty()) {
			activeCards.remove(match.getId());
		}
		Player speedrunner = resolveSpeedrunner(match);
		CardContext context = new CardContext(plugin, match, speedrunner, lobbyManager, compassService, this);
		entry.card().clear(context);
	}

	public Match getRunningMatch(Player player) {
		if (player == null) {
			return null;
		}
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null || match.getState() != MatchState.RUNNING) {
			return null;
		}
		return match;
	}

	public boolean isSpeedrunner(Match match, Player player) {
		if (match == null || player == null) {
			return false;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		return speedrunnerId != null && speedrunnerId.equals(player.getUniqueId());
	}

	public boolean isHunter(Match match, Player player) {
		if (match == null || player == null) {
			return false;
		}
		return match.getHunters().contains(player.getUniqueId());
	}

	public boolean isCardActive(Match match, String cardId) {
		if (match == null || cardId == null || cardId.isBlank()) {
			return false;
		}
		String normalizedCardId = normalizeCardId(cardId);
		long now = System.currentTimeMillis();
		List<ActiveCard> entries = activeCards.get(match.getId());
		if (entries != null && !entries.isEmpty()) {
			for (ActiveCard entry : List.copyOf(entries)) {
				CardDefinition active = entry.card();
				if (active == null || normalizeCardId(active.getId()) == null) {
					continue;
				}
				if (entry.expiresAtMillis() > 0L && entry.expiresAtMillis() <= now) {
					continue;
				}
				if (normalizeCardId(active.getId()).equals(normalizedCardId)) {
					return true;
				}
			}
		}
		if ("spawnist".equals(normalizedCardId)) {
			return spawnistCharges.getOrDefault(match.getId(), 0) > 0;
		}
		return false;
	}

	public void addSpawnistCharge(Match match, int amount) {
		if (match == null || amount <= 0) {
			return;
		}
		int maxCharges = resolveSpawnistMaxCharges(match);
		int current = Math.max(0, spawnistCharges.getOrDefault(match.getId(), 0));
		int next = Math.min(maxCharges, current + amount);
		if (next <= 0) {
			spawnistCharges.remove(match.getId());
			return;
		}
		spawnistCharges.put(match.getId(), next);
	}

	public boolean consumeSpawnistCharge(Match match) {
		if (match == null) {
			return false;
		}
		String matchId = match.getId();
		Integer current = spawnistCharges.get(matchId);
		if (current == null || current <= 0) {
			return false;
		}
		if (current <= 1) {
			spawnistCharges.remove(matchId);
		} else {
			spawnistCharges.put(matchId, current - 1);
		}
		return true;
	}

	private int resolveSpawnistMaxCharges(Match match) {
		if (match == null) {
			return 1;
		}
		int threshold = Math.max(1, cfgInt("balance.cards.spawnist.highHunterThreshold", 5));
		int lowMax = Math.max(1, cfgInt("balance.cards.spawnist.maxChargesLowHunters", 1));
		int highMax = Math.max(lowMax, cfgInt("balance.cards.spawnist.maxChargesHighHunters", 2));
		int hunters = Math.max(0, match.getHunters().size());
		return hunters >= threshold ? highMax : lowMax;
	}

	private int cfgInt(String path, int fallback) {
		if (plugin == null || plugin.getConfig() == null || path == null || path.isBlank()) {
			return fallback;
		}
		return plugin.getConfig().getInt(path, fallback);
	}

	private Player resolveSpeedrunner(Match match) {
		UUID speedrunnerId = match == null ? null : match.getSpeedrunnerId();
		return speedrunnerId == null ? null : Bukkit.getPlayer(speedrunnerId);
	}

	private void broadcastCurseUsed(Match match, Player speedrunner, CardDefinition card) {
		if (match == null || speedrunner == null || card == null) {
			return;
		}
		String message = "<red>❌</red> " + UiColors.roleSpeedrunner() + " "
			+ Text.playerNameDisplay(speedrunner.getUniqueId())
			+ " đã dùng lời nguyền <yellow>" + card.getName() + "</yellow>.";
		Set<UUID> targets = match.getHunters();
		if (targets == null || targets.isEmpty()) {
			targets = match.getLobbyPlayers();
		}
		for (UUID id : Set.copyOf(targets)) {
			if (speedrunner.getUniqueId().equals(id)) {
				continue;
			}
			Player hunter = Bukkit.getPlayer(id);
			if (hunter != null) {
				Text.msg(hunter, message);
			}
		}
	}

	private void applyCursePotion(Player hunter, CardDefinition card, int remainingSeconds) {
		if (hunter == null || card == null || remainingSeconds <= 0) {
			return;
		}
		String cardId = normalizeCardId(card.getId());
		if (cardId == null) {
			return;
		}
		int duration = Math.max(1, remainingSeconds) * 20;
		PotionEffect effect = switch (cardId) {
			case "too_slow" -> new PotionEffect(PotionEffectType.SLOWNESS, duration, 1);
			case "dizzy" -> new PotionEffect(PotionEffectType.NAUSEA, duration, 0);
			case "blind" -> new PotionEffect(PotionEffectType.BLINDNESS, duration, 0);
			case "hungry" -> new PotionEffect(PotionEffectType.HUNGER, duration, 0);
			case "broken_hand" -> new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, 0);
			case "sonic" -> new PotionEffect(PotionEffectType.SPEED, duration, 99);
			case "shining" -> new PotionEffect(PotionEffectType.GLOWING, duration, 0);
			default -> null;
		};
		if (effect != null) {
			hunter.addPotionEffect(effect);
		}
	}

	private String formatCardDuration(int durationSeconds) {
		if (durationSeconds <= 0) {
			return "Không giới hạn";
		}
		int totalSeconds = Math.max(0, durationSeconds);
		int hours = totalSeconds / 3600;
		int minutes = (totalSeconds % 3600) / 60;
		int seconds = totalSeconds % 60;
		if (hours > 0) {
			return String.format("%d:%02d:%02d", hours, minutes, seconds);
		}
		return String.format("%d:%02d", minutes, seconds);
	}

	private Component mmNoItalic(String miniMessage) {
		return Text.mm(miniMessage).decoration(TextDecoration.ITALIC, false);
	}

	private List<Component> mmNoItalic(List<String> lines) {
		return lines.stream()
			.map(this::mmNoItalic)
			.toList();
	}

	private List<CardDefinition> drawRandomCards(Match match, int count) {
		List<CardDefinition> pool = getEnabledCards(match);
		if (pool.isEmpty()) {
			return List.of();
		}
		pool = new ArrayList<>(pool);
		Collections.shuffle(pool, random);
		return pool.subList(0, Math.min(count, pool.size()));
	}

	private List<CardDefinition> ensureCardSlots(Match match) {
		List<CardDefinition> slots = match.getKeptCards();
		while (slots.size() < MAX_SLOTS) {
			slots.add(null);
		}
		if (slots.size() > MAX_SLOTS) {
			slots.subList(MAX_SLOTS, slots.size()).clear();
		}
		return slots;
	}

	private void ensureEnabledCards(Match match) {
		if (match == null) {
			return;
		}
		List<String> enabled = match.getSettings().getEnabledCardIds();
		List<String> normalized = new ArrayList<>();
		for (String id : enabled) {
			String value = normalizeCardId(id);
			if (value == null || value.isBlank()) {
				continue;
			}
			CardDefinition card = getCardById(value);
			if (card != null && !normalized.contains(card.getId())) {
				normalized.add(card.getId());
			}
		}
		if (normalized.isEmpty()) {
			normalized.addAll(getAllCardIds());
		}
		match.getSettings().setEnabledCardIds(normalized);
	}

	private String normalizeCardId(String cardId) {
		return cardId == null ? null : cardId.trim().toLowerCase(Locale.ROOT);
	}

	private List<CardDefinition> buildDefaultPool() {
		List<CardDefinition> cards = new ArrayList<>();
		cards.add(new SpeedBoostCard());
		cards.add(new RegenCard());
		cards.add(new StunCard());
		cards.add(new SpawnistCard());
		cards.add(new AdrenalineCard());
		cards.add(new FireResistCard());
		cards.add(new ResistanceCard());
		cards.add(new HunterSlownessCard());
		cards.add(new HunterBlindnessCard());
		cards.add(new ImmortalCard());
		cards.add(new MoreLivesCard());
		cards.add(new EnderEyeCard());
		cards.add(new BlazeRodCard());
		cards.add(new SpyCard());
		cards.add(new SpeeeeeeedCard());
		cards.add(new InvisibleCard());
		cards.add(new CompassScrambleCard());
		cards.add(new RunningMinionCard());
		cards.add(new HulkCard());
		cards.add(new YearnForTheMinesCard());
		cards.add(new LavaSwimmerCard());
		cards.add(new CurseTooSlowCard());
		cards.add(new CurseDizzyCard());
		cards.add(new CurseBlindCard());
		cards.add(new CurseHungryCard());
		cards.add(new CurseZombieOutbreakCard());
		cards.add(new CurseBrokenHandCard());
		cards.add(new CurseSonicCard());
		cards.add(new CurseShiningCard());
		cards.add(new CurseInterdimensionalSicknessCard());
		cards.add(new CurseVeganCard());
		return cards;
	}
}
