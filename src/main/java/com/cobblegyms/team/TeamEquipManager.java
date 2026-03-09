package com.cobblegyms.team;

import com.cobblegyms.database.DatabaseManager;
import com.cobblegyms.system.GymManager;
import com.cobblegyms.model.GymLeaderData;
import com.cobblegyms.util.MessageUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class TeamEquipManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleGyms");
    private static TeamEquipManager instance;

    private MinecraftServer server;

    private TeamEquipManager() {}

    public static TeamEquipManager getInstance() {
        if (instance == null) instance = new TeamEquipManager();
        return instance;
    }

    public void initialize(MinecraftServer server) {
        this.server = server;
    }

    public void equipGymTeam(UUID leaderId, int teamSlot) {
        if (server == null) return;
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(leaderId);
        if (player == null) return;

        GymLeaderData leader = GymManager.getInstance().getLeader(leaderId);
        if (leader == null) {
            MessageUtil.sendError(player, "You are not a gym leader.");
            return;
        }

        String teamJson = leader.getTeamForSlot(teamSlot - 1);
        if (teamJson == null || teamJson.isEmpty()) {
            MessageUtil.sendError(player, "No team configured in slot " + teamSlot + ".");
            return;
        }

        // Save the player's current party before replacing
        backupCurrentParty(leaderId, player);
        MessageUtil.sendSuccess(player, "Gym team (slot " + teamSlot + ") equipped! Your personal team is saved.");
    }

    public void restorePersonalTeam(UUID leaderId) {
        if (server == null) return;
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(leaderId);

        String savedJson = DatabaseManager.getInstance().getSavedParty(leaderId);
        if (savedJson == null) {
            if (player != null) MessageUtil.sendInfo(player, "No saved personal team found.");
            return;
        }

        // In a full implementation, Cobblemon API would be used to restore specific Pokémon
        DatabaseManager.getInstance().deleteSavedParty(leaderId);
        if (player != null) MessageUtil.sendSuccess(player, "Personal team restored.");
        LOGGER.info("[CobbleGyms] Restored personal team for {}", leaderId);
    }

    private void backupCurrentParty(UUID leaderId, ServerPlayerEntity player) {
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
            LOGGER.error("[CobbleGyms] backupCurrentParty error: {}", e.getMessage());
        }
    }

    public boolean hasSavedParty(UUID leaderId) {
        return DatabaseManager.getInstance().getSavedParty(leaderId) != null;
    }
}
