package dev.belikhun.lunamanhunt.model.match;

public class MatchWorlds {
    private String overworldName;
    private String netherName;
    private String endName;
    private boolean overworldReady;
    private boolean netherReady;
    private boolean endReady;

    public String getOverworldName() {
        return overworldName;
    }

    public void setOverworldName(String overworldName) {
        this.overworldName = overworldName;
    }

    public String getNetherName() {
        return netherName;
    }

    public void setNetherName(String netherName) {
        this.netherName = netherName;
    }

    public String getEndName() {
        return endName;
    }

    public void setEndName(String endName) {
        this.endName = endName;
    }

    public boolean isOverworldReady() {
        return overworldReady;
    }

    public void setOverworldReady(boolean overworldReady) {
        this.overworldReady = overworldReady;
    }

    public boolean isNetherReady() {
        return netherReady;
    }

    public void setNetherReady(boolean netherReady) {
        this.netherReady = netherReady;
    }

    public boolean isEndReady() {
        return endReady;
    }

    public void setEndReady(boolean endReady) {
        this.endReady = endReady;
    }

    public boolean areAllReady() {
        return overworldReady && netherReady && endReady;
    }
}
