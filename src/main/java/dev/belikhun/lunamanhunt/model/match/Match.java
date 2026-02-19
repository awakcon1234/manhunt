package dev.belikhun.lunamanhunt.model.match;

import dev.belikhun.lunamanhunt.model.card.CardDefinition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Match {
	private final String id;
	private final UUID ownerId;
	private MatchState state;
	private MatchSettings settings;
	private MatchWorlds worlds;
	private Long overworldSeed;
	private Long netherSeed;
	private Long endSeed;
	private UUID speedrunnerId;
	private MatchWinner winner;
	private final Set<UUID> lobbyPlayers = new HashSet<>();
	private final Set<UUID> hunters = new HashSet<>();
	private final Set<UUID> invitedPlayers = new HashSet<>();
	private final List<CardDefinition> drawnCards = new ArrayList<>();
	private final List<CardDefinition> keptCards = new ArrayList<>();
	private final MatchStats stats = new MatchStats();
	private int speedrunnerPoints;
	private long createdAtMillis;
	private Long startedAtMillis;
	private Long endedAtMillis;

	public Match(String id, UUID ownerId, MatchSettings settings) {
		this.id = id;
		this.ownerId = ownerId;
		this.settings = settings;
		this.state = MatchState.LOBBY;
		this.createdAtMillis = System.currentTimeMillis();
	}

	public String getId() {
		return id;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public MatchState getState() {
		return state;
	}

	public void setState(MatchState state) {
		this.state = state;
	}

	public MatchSettings getSettings() {
		return settings;
	}

	public void setSettings(MatchSettings settings) {
		this.settings = settings;
	}

	public MatchWorlds getWorlds() {
		return worlds;
	}

	public void setWorlds(MatchWorlds worlds) {
		this.worlds = worlds;
	}

	public Long getOverworldSeed() {
		return overworldSeed;
	}

	public void setOverworldSeed(Long overworldSeed) {
		this.overworldSeed = overworldSeed;
	}

	public Long getNetherSeed() {
		return netherSeed;
	}

	public void setNetherSeed(Long netherSeed) {
		this.netherSeed = netherSeed;
	}

	public Long getEndSeed() {
		return endSeed;
	}

	public void setEndSeed(Long endSeed) {
		this.endSeed = endSeed;
	}

	public UUID getSpeedrunnerId() {
		return speedrunnerId;
	}

	public void setSpeedrunnerId(UUID speedrunnerId) {
		this.speedrunnerId = speedrunnerId;
	}

	public MatchWinner getWinner() {
		return winner;
	}

	public void setWinner(MatchWinner winner) {
		this.winner = winner;
	}

	public Set<UUID> getLobbyPlayers() {
		return lobbyPlayers;
	}

	public Set<UUID> getHunters() {
		return hunters;
	}

	public Set<UUID> getInvitedPlayers() {
		return invitedPlayers;
	}

	public List<CardDefinition> getDrawnCards() {
		return drawnCards;
	}

	public List<CardDefinition> getKeptCards() {
		return keptCards;
	}

	public int getSpeedrunnerPoints() {
		return speedrunnerPoints;
	}

	public MatchStats getStats() {
		return stats;
	}

	public void setSpeedrunnerPoints(int speedrunnerPoints) {
		this.speedrunnerPoints = speedrunnerPoints;
	}

	public long getCreatedAtMillis() {
		return createdAtMillis;
	}

	public void setCreatedAtMillis(long createdAtMillis) {
		this.createdAtMillis = createdAtMillis;
	}

	public Long getStartedAtMillis() {
		return startedAtMillis;
	}

	public void setStartedAtMillis(Long startedAtMillis) {
		this.startedAtMillis = startedAtMillis;
	}

	public Long getEndedAtMillis() {
		return endedAtMillis;
	}

	public void setEndedAtMillis(Long endedAtMillis) {
		this.endedAtMillis = endedAtMillis;
	}
}
