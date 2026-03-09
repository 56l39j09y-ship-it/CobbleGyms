package com.cobblegyms.system;

import com.cobblegyms.database.DatabaseManager;
import com.cobblegyms.model.*;
import com.cobblegyms.util.MessageUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GymManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleGyms");
    private static GymManager instance;

    private final Map<UUID, GymLeaderData> leaders = new ConcurrentHashMap<>();
    private MinecraftServer server;

    private GymManager() {}

    public static GymManager getInstance() {
        if (instance == null) instance = new GymManager();
        return instance;
    }

    public void initialize(MinecraftServer server) {
        this.server = server;
        leaders.clear();
        DatabaseManager.getInstance().getAllGymLeaders().forEach(l -> leaders.put(l.getLeaderId(), l));
        LOGGER.info("[CobbleGyms] Loaded {} gym leaders from database.", leaders.size());
    }

    public void assignGymLeader(UUID playerId, String username, PokemonType type1,
                                 BattleFormat format, GymRole role) {
        GymLeaderData data = new GymLeaderData(playerId, username, type1, format, role);
        leaders.put(playerId, data);
        DatabaseManager.getInstance().upsertGymLeader(data);
    }

    public void assignE4(UUID playerId, String username, PokemonType type1,
                          PokemonType type2, BattleFormat format) {
        GymLeaderData data = new GymLeaderData(playerId, username, type1, format, GymRole.ELITE_FOUR);
        data.setType2(type2);
        leaders.put(playerId, data);
        DatabaseManager.getInstance().upsertGymLeader(data);
    }

    public void assignChampion(UUID playerId, String username, BattleFormat format) {
        GymLeaderData data = new GymLeaderData(playerId, username, PokemonType.NORMAL, format, GymRole.CHAMPION);
        leaders.put(playerId, data);
        DatabaseManager.getInstance().upsertGymLeader(data);
    }

    public void removeLeader(UUID leaderId) {
        leaders.remove(leaderId);
        DatabaseManager.getInstance().deleteGymLeader(leaderId);
    }

    public void openGym(UUID leaderId) {
        GymLeaderData data = leaders.get(leaderId);
        if (data != null) {
            data.setActive(true);
            DatabaseManager.getInstance().updateGymLeaderActive(leaderId, true);
            notifyLeader(leaderId, "\u00a7aYour gym is now OPEN!");
        }
    }

    public void closeGym(UUID leaderId) {
        GymLeaderData data = leaders.get(leaderId);
        if (data != null) {
            data.setActive(false);
            DatabaseManager.getInstance().updateGymLeaderActive(leaderId, false);
            notifyLeader(leaderId, "\u00a7cYour gym is now CLOSED.");
        }
    }

    private void notifyLeader(UUID leaderId, String message) {
        if (server != null) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(leaderId);
            if (player != null) {
                MessageUtil.sendInfo(player, message);
            }
        }
    }

    public List<GymLeaderData> getAllGymLeaders() {
        return leaders.values().stream()
                .filter(l -> l.getRole() == GymRole.GYM_LEADER)
                .toList();
    }

    public List<GymLeaderData> getAllE4() {
        return leaders.values().stream()
                .filter(l -> l.getRole() == GymRole.ELITE_FOUR)
                .toList();
    }

    public Optional<GymLeaderData> getChampion() {
        return leaders.values().stream()
                .filter(l -> l.getRole() == GymRole.CHAMPION)
                .findFirst();
    }

    public Optional<GymLeaderData> getGymLeaderByType(PokemonType type) {
        return leaders.values().stream()
                .filter(l -> l.getRole() == GymRole.GYM_LEADER && l.getType1() == type)
                .findFirst();
    }

    public GymLeaderData getLeader(UUID leaderId) {
        return leaders.get(leaderId);
    }

    public boolean isGymLeader(UUID playerId) {
        return leaders.containsKey(playerId);
    }

    public GymRole getRole(UUID playerId) {
        GymLeaderData data = leaders.get(playerId);
        return data != null ? data.getRole() : null;
    }

    public void setGymMode(UUID leaderId, BattleFormat format) {
        GymLeaderData data = leaders.get(leaderId);
        if (data != null) {
            data.setFormat(format);
            DatabaseManager.getInstance().upsertGymLeader(data);
        }
    }

    public void setTeamSlots(UUID leaderId, int slots) {
        GymLeaderData data = leaders.get(leaderId);
        if (data != null) {
            data.setTeamSlots(slots);
            DatabaseManager.getInstance().upsertGymLeader(data);
        }
    }

    public void setLocation(UUID leaderId, double x, double y, double z, String world) {
        GymLeaderData data = leaders.get(leaderId);
        if (data != null) {
            data.setLocationX(x);
            data.setLocationY(y);
            data.setLocationZ(z);
            data.setWorld(world);
            DatabaseManager.getInstance().upsertGymLeader(data);
        }
    }

    public Map<UUID, GymLeaderData> getAllLeaders() {
        return Collections.unmodifiableMap(leaders);
    }

    public void refreshFromDatabase() {
        leaders.clear();
        DatabaseManager.getInstance().getAllGymLeaders().forEach(l -> leaders.put(l.getLeaderId(), l));
    }
}
