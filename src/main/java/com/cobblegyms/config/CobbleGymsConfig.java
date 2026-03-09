package com.cobblegyms.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class CobbleGymsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static CobbleGymsConfig instance;

    // Season settings
    public int seasonDurationDays = 30;
    public int minBattlesForReward = 25;
    public double minWinrateForReward = 0.5;

    // Challenge settings
    public long challengeCooldownSeconds = 86400L; // 24 hours
    public int tempBanMaxHours = 72;

    // Discord settings
    public String discordWebhookUrl = "";
    public String discordBotToken = "";
    public Map<String, String> leaderDiscordChannels = new HashMap<>();

    // Gym locations per type
    public Map<String, GymLocation> gymLocations = new HashMap<>();

    // Reward settings
    public String gymLeaderWeeklyRewardCommand = "give %player% minecraft:diamond 5";
    public String e4WeeklyRewardCommand = "give %player% minecraft:diamond_block 1";
    public String championWeeklyRewardCommand = "give %player% minecraft:netherite_ingot 1";
    public String seasonEndRewardCommand = "give %player% minecraft:gold_ingot %amount%";

    public static class GymLocation {
        public double x, y, z;
        public String world = "minecraft:overworld";

        public GymLocation() {}
        public GymLocation(double x, double y, double z, String world) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.world = world;
        }
    }

    private CobbleGymsConfig() {}

    public static CobbleGymsConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static CobbleGymsConfig load() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("cobblegyms");
        Path configFile = configDir.resolve("config.json");

        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            if (!Files.exists(configFile)) {
                CobbleGymsConfig defaults = new CobbleGymsConfig();
                defaults.save();
                instance = defaults;
                return defaults;
            }

            try (Reader reader = Files.newBufferedReader(configFile)) {
                CobbleGymsConfig config = GSON.fromJson(reader, CobbleGymsConfig.class);
                if (config == null) config = new CobbleGymsConfig();
                instance = config;
                return config;
            }
        } catch (IOException e) {
            System.err.println("[CobbleGyms] Failed to load config: " + e.getMessage());
            CobbleGymsConfig defaults = new CobbleGymsConfig();
            instance = defaults;
            return defaults;
        }
    }

    public void save() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("cobblegyms");
        Path configFile = configDir.resolve("config.json");

        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("[CobbleGyms] Failed to save config: " + e.getMessage());
        }
    }

    public long getSeasonDurationMillis() {
        return (long) seasonDurationDays * 24 * 60 * 60 * 1000;
    }

    public long getChallengeCooldownMillis() {
        return challengeCooldownSeconds * 1000;
    }
}
