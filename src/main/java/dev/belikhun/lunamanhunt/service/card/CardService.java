package dev.belikhun.lunamanhunt.service.card;

import dev.belikhun.lunamanhunt.model.card.CardDefinition;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.cards.BlazeRodCard;
import dev.belikhun.lunamanhunt.service.card.cards.CompassScrambleCard;
import dev.belikhun.lunamanhunt.service.card.cards.CurseBlindCard;
import dev.belikhun.lunamanhunt.service.card.cards.CurseBrokenHandCard;
import dev.belikhun.lunamanhunt.service.card.cards.CurseDizzyCard;
import dev.belikhun.lunamanhunt.service.card.cards.CurseHungryCard;
import dev.belikhun.lunamanhunt.service.card.cards.CurseShiningCard;
import dev.belikhun.lunamanhunt.service.card.cards.CurseSonicCard;
import dev.belikhun.lunamanhunt.service.card.cards.CurseTooSlowCard;
import dev.belikhun.lunamanhunt.service.card.cards.CurseZombieOutbreakCard;
import dev.belikhun.lunamanhunt.service.card.cards.EnderEyeCard;
import dev.belikhun.lunamanhunt.service.card.cards.FireResistCard;
import dev.belikhun.lunamanhunt.service.card.cards.HulkCard;
import dev.belikhun.lunamanhunt.service.card.cards.HunterBlindnessCard;
import dev.belikhun.lunamanhunt.service.card.cards.HunterSlownessCard;
import dev.belikhun.lunamanhunt.service.card.cards.InvisibleCard;
import dev.belikhun.lunamanhunt.service.card.cards.LavaSwimmerCard;
import dev.belikhun.lunamanhunt.service.card.cards.MoreLivesCard;
import dev.belikhun.lunamanhunt.service.card.cards.RegenCard;
import dev.belikhun.lunamanhunt.service.card.cards.ResistanceCard;
import dev.belikhun.lunamanhunt.service.card.cards.RunningMinionCard;
import dev.belikhun.lunamanhunt.service.card.cards.SpeedBoostCard;
import dev.belikhun.lunamanhunt.service.card.cards.SpeeeeeeedCard;
import dev.belikhun.lunamanhunt.service.card.cards.SpyCard;
import dev.belikhun.lunamanhunt.service.card.cards.YearnForTheMinesCard;
import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.service.compass.CompassService;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.point.PointService;
import dev.belikhun.lunamanhunt.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
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
		return List.copyOf(match.getDrawnCards());
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
			Text.msg(player, "<red>❌</red> Chỉ speedrunner mới được giữ thẻ.");
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
		Text.msg(player, "<green>✔</green> Đã giữ thẻ <yellow>" + card.getName() + "</yellow>.");
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
			Text.msg(player, "<red>❌</red> Chỉ speedrunner mới được bỏ thẻ.");
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
			Text.msg(player, "<red>❌</red> Chỉ speedrunner mới được dùng thẻ.");
			return false;
		}
		if (!canUseCard(match, card)) {
			Text.msg(player, "<red>❌</red> Bạn không đủ điểm để dùng thẻ này.");
			return false;
		}
		CardContext context = new CardContext(plugin, match, player, lobbyManager, compassService);
		if (!card.apply(context)) {
			return false;
		}
		trackActiveCard(match, card);
		match.setSpeedrunnerPoints(match.getSpeedrunnerPoints() - card.getCost());
		Text.msg(player, "<green>✔</green> Đã dùng thẻ <yellow>" + card.getName() + "</yellow>.");
		return true;
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
		addPoint(match, 1);
		drawNewCards(match);
		Text.msg(player, "<green>✔</green> Bạn nhận được " + pointService.formatPoints(1) + " từ thành tựu.");
		Text.msg(player, "<yellow>ℹ</yellow> Đã mở " + POOL_COUNT + " thẻ mới, hãy giữ vào ô thẻ.");
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
			meta.displayName(mmNoItalic(rarityColor + rarityIcon + " <white>" + card.getName() + "</white>"));
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
		CardContext context = new CardContext(plugin, match, speedrunner, lobbyManager, compassService);
		entry.card().clear(context);
	}

	private Player resolveSpeedrunner(Match match) {
		UUID speedrunnerId = match == null ? null : match.getSpeedrunnerId();
		return speedrunnerId == null ? null : Bukkit.getPlayer(speedrunnerId);
	}

	private String formatCardDuration(int durationSeconds) {
		if (durationSeconds <= 0) {
			return "Không giới hạn";
		}
		int minutes = durationSeconds / 60;
		int seconds = durationSeconds % 60;
		if (minutes <= 0) {
			return durationSeconds + " giây";
		}
		if (seconds == 0) {
			if (minutes % 60 == 0) {
				int hours = minutes / 60;
				return hours + " giờ";
			}
			return minutes + " phút";
		}
		return minutes + " phút " + seconds + " giây";
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
		cards.add(new FireResistCard());
		cards.add(new ResistanceCard());
		cards.add(new HunterSlownessCard());
		cards.add(new HunterBlindnessCard());
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
		return cards;
	}
}
