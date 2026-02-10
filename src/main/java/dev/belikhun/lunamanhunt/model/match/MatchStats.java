package dev.belikhun.lunamanhunt.model.match;

public class MatchStats {
	private int huntersKilled;
	private int mobKills;
	private int naturalDeaths;
	private int cardsUsed;
	private double damageToHunters;
	private Long timeToNetherMillis;
	private Long timeToEndMillis;

	public int getHuntersKilled() {
		return huntersKilled;
	}

	public void incrementHuntersKilled() {
		huntersKilled++;
	}

	public int getMobKills() {
		return mobKills;
	}

	public void incrementMobKills() {
		mobKills++;
	}

	public int getNaturalDeaths() {
		return naturalDeaths;
	}

	public void incrementNaturalDeaths() {
		naturalDeaths++;
	}

	public int getCardsUsed() {
		return cardsUsed;
	}

	public void incrementCardsUsed() {
		cardsUsed++;
	}

	public double getDamageToHunters() {
		return damageToHunters;
	}

	public void addDamageToHunters(double amount) {
		if (amount <= 0.0) {
			return;
		}
		damageToHunters += amount;
	}

	public Long getTimeToNetherMillis() {
		return timeToNetherMillis;
	}

	public void setTimeToNetherMillis(Long timeToNetherMillis) {
		if (this.timeToNetherMillis != null || timeToNetherMillis == null) {
			return;
		}
		this.timeToNetherMillis = timeToNetherMillis;
	}

	public Long getTimeToEndMillis() {
		return timeToEndMillis;
	}

	public void setTimeToEndMillis(Long timeToEndMillis) {
		if (this.timeToEndMillis != null || timeToEndMillis == null) {
			return;
		}
		this.timeToEndMillis = timeToEndMillis;
	}
}
