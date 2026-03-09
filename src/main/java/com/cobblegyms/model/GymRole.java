package com.cobblegyms.model;

public enum GymRole {
    GYM_LEADER("Gym Leader"),
    ELITE_FOUR("Elite Four"),
    CHAMPION("Champion");

    private final String displayName;

    GymRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public static GymRole fromString(String name) {
        for (GymRole role : values()) {
            if (role.name().equalsIgnoreCase(name) || role.displayName.equalsIgnoreCase(name)) {
                return role;
            }
        }
        return null;
    }
}
