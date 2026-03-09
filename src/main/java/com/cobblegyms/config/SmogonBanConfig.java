package com.cobblegyms.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SmogonBanConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static SmogonBanConfig instance;

    public Set<String> bannedPokemon = new HashSet<>(Arrays.asList(
        // Uber tier
        "mewtwo", "mewtwomegax", "mewtwomegay", "rayquaza", "rayquazamega",
        "groudon", "groudonprimal", "kyogre", "kyogreprimal", "arceus",
        "zacian", "zaciancrowned", "zamazenta", "zamazentacrowned",
        "eternatus", "eternatuseternamax", "calyrex", "calyrexicerider",
        "calyrexshadowrider", "koraidon", "miraidon", "necrozma",
        "necrozmaduskmane", "necrozmadawnwings", "necrozmaultranecrozma",
        "lugia", "hooh", "reshiram", "zekrom", "kyurem", "kyuremblack",
        "kyuremwhite", "solgaleo", "lunala", "necrozma", "melmetal",
        "xerneas", "yveltal", "zygarde", "zygarde10", "zygardecomplete",
        "palkia", "dialga", "giratina", "giratinaorigin", "regigigas",
        "slaking", "shayminsky", "deoxys", "deoxysattack", "deoxysdefense",
        "deoxysspeed", "darmanitangalar", "blazikenspeed", "genesect",
        "marshadow", "naganadel", "stakataka", "blacephalon", "zeraora",
        "magearna", "volcanion", "hoopa", "hoopaconfined", "hoopaunbound",
        "landorusincarnate", "enamorus", "urshifurapidstrike", "urshifusinglestrike",
        "kubfu", "zarude", "glastrier", "spectrier", "eiscue", "indeedee",
        "spectrier", "chienpaotauros", "chienpao", "tinkaton", "annihilape",
        "gholdengo", "ironbundle", "fluttermane", "greattusk", "ironmoth",
        "roaringmoon", "walkingwake", "ironleaves", "ting-lu", "chienpaotauros"
    ));

    public Set<String> bannedMoves = new HashSet<>(Arrays.asList(
        // OHKO moves
        "fissure", "sheercold", "guillotine", "horndrill",
        // Evasion
        "doubleteam", "minimize",
        // Sleep
        "spore", "sleeppowder", "hypnosis", "darkvoid",
        // Baton Pass chains
        "batonpass",
        // Other broken
        "swagger", "flatter"
    ));

    public Set<String> bannedAbilities = new HashSet<>(Arrays.asList(
        "moody",
        "powerofalchemy",
        "receiverability",
        "powerConstruct",
        "illusion"
    ));

    public Set<String> bannedItems = new HashSet<>(Arrays.asList(
        "souldew",
        "kingsrock",
        "razorfang"
    ));

    // Per-gym extra bans: leaderId -> banned pokemon name (one per season)
    public Map<String, String> gymExtraBans = new HashMap<>();

    private SmogonBanConfig() {}

    public static SmogonBanConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static SmogonBanConfig load() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("cobblegyms");
        Path configFile = configDir.resolve("bans.json");

        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            if (!Files.exists(configFile)) {
                SmogonBanConfig defaults = new SmogonBanConfig();
                defaults.save();
                instance = defaults;
                return defaults;
            }

            try (Reader reader = Files.newBufferedReader(configFile)) {
                SmogonBanConfig config = GSON.fromJson(reader, SmogonBanConfig.class);
                if (config == null) config = new SmogonBanConfig();
                instance = config;
                return config;
            }
        } catch (IOException e) {
            System.err.println("[CobbleGyms] Failed to load ban config: " + e.getMessage());
            SmogonBanConfig defaults = new SmogonBanConfig();
            instance = defaults;
            return defaults;
        }
    }

    public void save() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("cobblegyms");
        Path configFile = configDir.resolve("bans.json");

        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("[CobbleGyms] Failed to save ban config: " + e.getMessage());
        }
    }

    public boolean isPokemonBanned(String speciesName) {
        return bannedPokemon.contains(speciesName.toLowerCase().replaceAll("[^a-z0-9]", ""));
    }

    public boolean isMoveBanned(String moveName) {
        return bannedMoves.contains(moveName.toLowerCase().replaceAll("[^a-z0-9]", ""));
    }

    public boolean isAbilityBanned(String abilityName) {
        return bannedAbilities.contains(abilityName.toLowerCase().replaceAll("[^a-z0-9]", ""));
    }

    public boolean isItemBanned(String itemName) {
        return bannedItems.contains(itemName.toLowerCase().replaceAll("[^a-z0-9]", ""));
    }

    public boolean isGymExtraBanned(String leaderId, String pokemonName) {
        String extra = gymExtraBans.get(leaderId);
        if (extra == null) return false;
        return extra.equalsIgnoreCase(pokemonName.replaceAll("[^a-z0-9A-Z]", ""));
    }

    public void addPokemonBan(String speciesName) {
        bannedPokemon.add(speciesName.toLowerCase().replaceAll("[^a-z0-9]", ""));
        save();
    }

    public void removePokemonBan(String speciesName) {
        bannedPokemon.remove(speciesName.toLowerCase().replaceAll("[^a-z0-9]", ""));
        save();
    }

    public void addMoveBan(String moveName) {
        bannedMoves.add(moveName.toLowerCase().replaceAll("[^a-z0-9]", ""));
        save();
    }

    public void removeMoveBan(String moveName) {
        bannedMoves.remove(moveName.toLowerCase().replaceAll("[^a-z0-9]", ""));
        save();
    }

    public void addAbilityBan(String abilityName) {
        bannedAbilities.add(abilityName.toLowerCase().replaceAll("[^a-z0-9]", ""));
        save();
    }

    public void removeAbilityBan(String abilityName) {
        bannedAbilities.remove(abilityName.toLowerCase().replaceAll("[^a-z0-9]", ""));
        save();
    }

    public void addItemBan(String itemName) {
        bannedItems.add(itemName.toLowerCase().replaceAll("[^a-z0-9]", ""));
        save();
    }

    public void removeItemBan(String itemName) {
        bannedItems.remove(itemName.toLowerCase().replaceAll("[^a-z0-9]", ""));
        save();
    }

    public void setGymExtraBan(String leaderId, String pokemonName) {
        gymExtraBans.put(leaderId, pokemonName);
        save();
    }

    public void removeGymExtraBan(String leaderId) {
        gymExtraBans.remove(leaderId);
        save();
    }
}
