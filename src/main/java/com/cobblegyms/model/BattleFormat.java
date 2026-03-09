package com.cobblegyms.model;

public enum BattleFormat {
    SINGLES("Singles"),
    DOUBLES("Doubles");

    private final String displayName;

    BattleFormat(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public static BattleFormat fromString(String name) {
        for (BattleFormat fmt : values()) {
            if (fmt.name().equalsIgnoreCase(name) || fmt.displayName.equalsIgnoreCase(name)) {
                return fmt;
            }
        }
        return SINGLES;
    }
}
