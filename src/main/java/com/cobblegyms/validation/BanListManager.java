package com.cobblegyms.validation;

import com.cobblegyms.config.SmogonBanConfig;

import java.util.Set;

public class BanListManager {

    private static BanListManager instance;

    private BanListManager() {}

    public static BanListManager getInstance() {
        if (instance == null) instance = new BanListManager();
        return instance;
    }

    public Set<String> getBannedPokemon() {
        return SmogonBanConfig.getInstance().bannedPokemon;
    }

    public Set<String> getBannedMoves() {
        return SmogonBanConfig.getInstance().bannedMoves;
    }

    public Set<String> getBannedAbilities() {
        return SmogonBanConfig.getInstance().bannedAbilities;
    }

    public Set<String> getBannedItems() {
        return SmogonBanConfig.getInstance().bannedItems;
    }

    public void addPokemonBan(String speciesName) {
        SmogonBanConfig.getInstance().addPokemonBan(speciesName);
    }

    public void removePokemonBan(String speciesName) {
        SmogonBanConfig.getInstance().removePokemonBan(speciesName);
    }

    public void addMoveBan(String moveName) {
        SmogonBanConfig.getInstance().addMoveBan(moveName);
    }

    public void removeMoveBan(String moveName) {
        SmogonBanConfig.getInstance().removeMoveBan(moveName);
    }

    public void addAbilityBan(String abilityName) {
        SmogonBanConfig.getInstance().addAbilityBan(abilityName);
    }

    public void removeAbilityBan(String abilityName) {
        SmogonBanConfig.getInstance().removeAbilityBan(abilityName);
    }

    public void addItemBan(String itemName) {
        SmogonBanConfig.getInstance().addItemBan(itemName);
    }

    public void removeItemBan(String itemName) {
        SmogonBanConfig.getInstance().removeItemBan(itemName);
    }

    public boolean isPokemonBanned(String speciesName) {
        return SmogonBanConfig.getInstance().isPokemonBanned(speciesName);
    }

    public boolean isMoveBanned(String moveName) {
        return SmogonBanConfig.getInstance().isMoveBanned(moveName);
    }

    public boolean isAbilityBanned(String abilityName) {
        return SmogonBanConfig.getInstance().isAbilityBanned(abilityName);
    }

    public boolean isItemBanned(String itemName) {
        return SmogonBanConfig.getInstance().isItemBanned(itemName);
    }
}
