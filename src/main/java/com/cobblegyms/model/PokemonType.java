package com.cobblegyms.model;

public enum PokemonType {
    NORMAL("Normal", "\u00a7f"),
    FIRE("Fire", "\u00a76"),
    WATER("Water", "\u00a79"),
    GRASS("Grass", "\u00a7a"),
    ELECTRIC("Electric", "\u00a7e"),
    ICE("Ice", "\u00a7b"),
    FIGHTING("Fighting", "\u00a7c"),
    POISON("Poison", "\u00a75"),
    GROUND("Ground", "\u00a76"),
    FLYING("Flying", "\u00a73"),
    PSYCHIC("Psychic", "\u00a7d"),
    BUG("Bug", "\u00a72"),
    ROCK("Rock", "\u00a77"),
    GHOST("Ghost", "\u00a75"),
    DRAGON("Dragon", "\u00a71"),
    DARK("Dark", "\u00a78"),
    STEEL("Steel", "\u00a77"),
    FAIRY("Fairy", "\u00a7d");

    private final String displayName;
    private final String colorCode;

    PokemonType(String displayName, String colorCode) {
        this.displayName = displayName;
        this.colorCode = colorCode;
    }

    public String getDisplayName() { return displayName; }
    public String getColorCode() { return colorCode; }
    public String getColoredName() { return colorCode + displayName; }

    public static PokemonType fromString(String name) {
        for (PokemonType type : values()) {
            if (type.name().equalsIgnoreCase(name) || type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
