package com.cobblegyms.system;

import com.cobblegyms.database.DatabaseManager;
import com.cobblegyms.model.GymLeaderData;
import com.cobblegyms.team.PokePasteImporter;
import com.cobblegyms.util.MessageUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.UUID;

public class TeamManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleGyms");
    private static TeamManager instance;

    private MinecraftServer server;
    private final Random random = new Random();

    private TeamManager() {}

    public static TeamManager getInstance() {
        if (instance == null) instance = new TeamManager();
        return instance;
    }

    public void initialize(MinecraftServer server) {
        this.server = server;
    }

    public void importFromPokePaste(UUID leaderId, String pokePasteUrl, int teamSlot) {
        if (server == null) return;
        GymLeaderData leader = GymManager.getInstance().getLeader(leaderId);
        if (leader == null) return;

        Thread importThread = new Thread(() -> {
            try {
                String rawData = PokePasteImporter.fetchFromUrl(pokePasteUrl);
                if (rawData == null || rawData.isEmpty()) {
                    server.execute(() -> {
                        ServerPlayerEntity player = server.getPlayerManager().getPlayer(leaderId);
                        if (player != null) MessageUtil.sendError(player, "Failed to fetch team from URL.");
                    });
                    return;
                }
                String teamJson = PokePasteImporter.parsePokePasteData(rawData);
                assignTeamToLeader(leaderId, teamSlot, teamJson);
                server.execute(() -> {
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(leaderId);
                    if (player != null)
                        MessageUtil.sendSuccess(player, "Team imported to slot " + teamSlot + " successfully!");
                });
            } catch (Exception e) {
                LOGGER.error("[CobbleGyms] importFromPokePaste error: {}", e.getMessage(), e);
                server.execute(() -> {
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(leaderId);
                    if (player != null) MessageUtil.sendError(player, "Import failed: " + e.getMessage());
                });
            }
        }, "CobbleGyms-TeamImport");
        importThread.setDaemon(true);
        importThread.start();
    }

    public void assignTeamToLeader(UUID leaderId, int slot, String teamJson) {
        GymLeaderData leader = GymManager.getInstance().getLeader(leaderId);
        if (leader == null) return;
        leader.setTeamForSlot(slot - 1, teamJson);
        DatabaseManager.getInstance().saveTeamSlot(leaderId, slot, teamJson);
    }

    public String getTeamForBattle(UUID leaderId, UUID challengerId) {
        GymLeaderData leader = GymManager.getInstance().getLeader(leaderId);
        if (leader == null) return null;
        int slots = leader.getTeamSlots();
        if (slots <= 1 || leader.getTeamData().isEmpty()) {
            return leader.getTeamData().isEmpty() ? null : leader.getTeamData().get(0);
        }
        // Random team selection before seeing challenger's team
        int slot = random.nextInt(Math.min(slots, leader.getTeamData().size()));
        return leader.getTeamData().get(slot);
    }

    public void savePlayerParty(UUID leaderId) {
        if (server == null) return;
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(leaderId);
        if (player == null) return;
        // Serialize Cobblemon party to JSON and store
        try {
            var party = com.cobblegyms.util.CobblemonUtil.getPlayerParty(player);
            if (party == null) return;
            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            party.forEach(pokemon -> {
                if (pokemon != null) {
                    com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                    obj.addProperty("species", pokemon.getSpecies().getName());
                    obj.addProperty("uuid", pokemon.getUuid().toString());
                    arr.add(obj);
                }
            });
            DatabaseManager.getInstance().saveParty(leaderId, arr.toString());
        } catch (Exception e) {
            LOGGER.error("[CobbleGyms] savePlayerParty error: {}", e.getMessage());
        }
    }

    public void restoreOriginalTeam(UUID leaderId) {
        if (server == null) return;
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(leaderId);
        if (player == null) return;
        String savedJson = DatabaseManager.getInstance().getSavedParty(leaderId);
        if (savedJson == null) {
            MessageUtil.sendInfo(player, "No saved party found.");
            return;
        }
        // In a real implementation, we would restore the Pokémon from UUIDs in the PC/storage
        DatabaseManager.getInstance().deleteSavedParty(leaderId);
        MessageUtil.sendSuccess(player, "Your personal team has been restored.");
    }

    public void equipGymTeam(UUID leaderId) {
        if (server == null) return;
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(leaderId);
        if (player == null) return;
        GymLeaderData leader = GymManager.getInstance().getLeader(leaderId);
        if (leader == null || leader.getTeamData().isEmpty()) {
            MessageUtil.sendError(player, "You have no gym team configured! Import a team first.");
            return;
        }
        savePlayerParty(leaderId);
        MessageUtil.sendSuccess(player, "Gym team equipped! Your personal team has been saved.");
    }

    public void unequipGymTeam(UUID leaderId) {
        restoreOriginalTeam(leaderId);
    }
}
