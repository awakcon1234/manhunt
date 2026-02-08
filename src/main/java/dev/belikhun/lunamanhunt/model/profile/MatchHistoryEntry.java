package dev.belikhun.lunamanhunt.model.profile;

import dev.belikhun.lunamanhunt.model.match.MatchWinner;

public class MatchHistoryEntry {
	private String matchId;
	private MatchWinner winner;
	private String role;
	private boolean won;
	private long startedAtMillis;
	private long endedAtMillis;
	private long durationMillis;

	public String getMatchId() {
		return matchId;
	}

	public void setMatchId(String matchId) {
		this.matchId = matchId;
	}

	public MatchWinner getWinner() {
		return winner;
	}

	public void setWinner(MatchWinner winner) {
		this.winner = winner;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public boolean isWon() {
		return won;
	}

	public void setWon(boolean won) {
		this.won = won;
	}

	public long getStartedAtMillis() {
		return startedAtMillis;
	}

	public void setStartedAtMillis(long startedAtMillis) {
		this.startedAtMillis = startedAtMillis;
	}

	public long getEndedAtMillis() {
		return endedAtMillis;
	}

	public void setEndedAtMillis(long endedAtMillis) {
		this.endedAtMillis = endedAtMillis;
	}

	public long getDurationMillis() {
		return durationMillis;
	}

	public void setDurationMillis(long durationMillis) {
		this.durationMillis = durationMillis;
	}
}
