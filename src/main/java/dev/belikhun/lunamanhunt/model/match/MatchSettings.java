package dev.belikhun.lunamanhunt.model.match;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MatchSettings {
    private SpeedrunnerSelection speedrunnerSelection = SpeedrunnerSelection.RANDOM;
    private UUID speedrunnerId;
    private String worldSeed;
    private int worldBorderLimit = 1000;
    private int startingBorder = 10;
    private List<String> speedrunnerKit = new ArrayList<>();
    private MatchVisibility visibility = MatchVisibility.PUBLIC;
    private List<String> enabledCardIds = new ArrayList<>();

    public SpeedrunnerSelection getSpeedrunnerSelection() {
        return speedrunnerSelection;
    }

    public void setSpeedrunnerSelection(SpeedrunnerSelection speedrunnerSelection) {
        this.speedrunnerSelection = speedrunnerSelection;
    }

    public UUID getSpeedrunnerId() {
        return speedrunnerId;
    }

    public void setSpeedrunnerId(UUID speedrunnerId) {
        this.speedrunnerId = speedrunnerId;
    }

    public String getWorldSeed() {
        return worldSeed;
    }

    public void setWorldSeed(String worldSeed) {
        this.worldSeed = worldSeed;
    }

    public int getWorldBorderLimit() {
        return worldBorderLimit;
    }

    public void setWorldBorderLimit(int worldBorderLimit) {
        this.worldBorderLimit = worldBorderLimit;
    }

    public int getStartingBorder() {
        return startingBorder;
    }

    public void setStartingBorder(int startingBorder) {
        this.startingBorder = startingBorder;
    }

    public List<String> getSpeedrunnerKit() {
        return speedrunnerKit;
    }

    public void setSpeedrunnerKit(List<String> speedrunnerKit) {
        this.speedrunnerKit = speedrunnerKit == null ? new ArrayList<>() : speedrunnerKit;
    }

    public MatchVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(MatchVisibility visibility) {
        this.visibility = visibility;
    }

    public List<String> getEnabledCardIds() {
        return enabledCardIds;
    }

    public void setEnabledCardIds(List<String> enabledCardIds) {
        this.enabledCardIds = enabledCardIds == null ? new ArrayList<>() : enabledCardIds;
    }
}
