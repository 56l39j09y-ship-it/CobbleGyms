package com.cobblegyms.system;

import com.cobblegyms.database.DatabaseManager;
import com.cobblegyms.model.*;
import com.cobblegyms.util.CobblemonUtil;
import com.cobblegyms.util.MessageUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BattleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleGyms");
    private static BattleManager instance;

    private MinecraftServer server;
    private final Map<UUID, UUID> activeBattles = new ConcurrentHashMap<>();
    private final Map<String, Integer> battleTurns = new ConcurrentHashMap<>();

    private BattleManager() {}

    public static BattleManager getInstance() {
        if (instance == null) instance = new BattleManager();
        return instance;
    }

    public void initialize(MinecraftServer server) {
        this.server = server;
    }

    public void startBattle(UUID leaderId, UUID challengerId) {
        if (server == null) return;
        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(leaderId);
        ServerPlayerEntity challenger = server.getPlayerManager().getPlayer(challengerId);
        if (leader == null || challenger == null) {
            LOGGER.warn("[CobbleGyms] Cannot start battle: player(s) offline. leader={}, challenger={}", leaderId, challengerId);
            return;
        }
        GymLeaderData leaderData = GymManager.getInstance().getLeader(leaderId);
        if (leaderData == null) return;
        activeBattles.put(leaderId, challengerId);
        battleTurns.put(battleKey(leaderId, challengerId), 0);
        CobblemonUtil.startBattle(leader, challenger, leaderData.getFormat());
        MessageUtil.sendTitle(leader, "\u00a76Battle Starting!", "\u00a77vs " + challenger.getName().getString());
        MessageUtil.sendTitle(challenger, "\u00a76Battle Starting!", "\u00a77vs " + leader.getName().getString());
    }

    public void onBattleEnd(UUID leaderId, UUID challengerId, String result, int turns,
                            String leaderTeam, String challengerTeam) {
        activeBattles.remove(leaderId);
        String key = battleKey(leaderId, challengerId);
        battleTurns.remove(key);

        Season season = SeasonManager.getInstance().getCurrentSeason();
        long seasonId = season != null ? season.getId() : 0;

        ServerPlayerEntity challenger = server != null ? server.getPlayerManager().getPlayer(challengerId) : null;
        String challengerName = challenger != null ? challenger.getName().getString() : challengerId.toString();

        BattleRecord record = new BattleRecord(leaderId, challengerId, challengerName,
                result, leaderTeam, challengerTeam, turns, seasonId);
        int recordId = DatabaseManager.getInstance().insertBattleRecord(record);
        record.setId(recordId);

        boolean leaderWon = "win".equalsIgnoreCase(result);
        RewardManager.getInstance().updateWeeklyStats(leaderId, leaderWon);

        if (!leaderWon && challenger != null) {
            grantBadgeOrProgress(leaderId, challengerId, challenger.getName().getString(), (int) seasonId);
        }

        DiscordManager.getInstance().sendBattleRecord(leaderId, record);

        QueueEntry active = QueueManager.getInstance().getActiveEntry(leaderId);
        if (active != null) {
            QueueManager.getInstance().markDone(active.getId());
        }

        if (server != null) {
            ServerPlayerEntity leader = server.getPlayerManager().getPlayer(leaderId);
            if (leader != null) {
                String msg = leaderWon ? "\u00a7aYou won the battle against " + challengerName + "!"
                        : "\u00a7cYou lost the battle to " + challengerName + ".";
                MessageUtil.sendInfo(leader, msg);
            }
            if (challenger != null) {
                String msg = !leaderWon ? "\u00a7aYou won! Badge earned!"
                        : "\u00a7cYou lost! Better luck next time!";
                MessageUtil.sendInfo(challenger, msg);
            }
        }
    }

    private void grantBadgeOrProgress(UUID leaderId, UUID challengerId, String challengerName, int seasonId) {
        GymLeaderData leaderData = GymManager.getInstance().getLeader(leaderId);
        if (leaderData == null) return;
        DatabaseManager db = DatabaseManager.getInstance();
        if (leaderData.getRole() == GymRole.GYM_LEADER) {
            String gymType = leaderData.getType1().name();
            db.grantBadge(challengerId, challengerName, gymType, seasonId);
            if (server != null) {
                ServerPlayerEntity challenger = server.getPlayerManager().getPlayer(challengerId);
                if (challenger != null) {
                    MessageUtil.sendSuccess(challenger, "You earned the "
                            + leaderData.getType1().getColoredName() + "\u00a7a Badge!");
                    MessageUtil.sendTitle(challenger, "\u00a7aBadge Earned!",
                            leaderData.getType1().getColoredName() + "\u00a7r Badge");
                }
            }
        } else if (leaderData.getRole() == GymRole.ELITE_FOUR) {
            db.grantE4Win(challengerId, challengerName, leaderId, seasonId);
            if (server != null) {
                ServerPlayerEntity challenger = server.getPlayerManager().getPlayer(challengerId);
                if (challenger != null) {
                    MessageUtil.sendSuccess(challenger, "You defeated Elite Four member "
                            + leaderData.getUsername() + "!");
                    int e4Wins = db.countE4WinsForSeason(challengerId, seasonId);
                    if (e4Wins >= GymManager.getInstance().getAllE4().size()) {
                        MessageUtil.sendSuccess(challenger, "You have defeated all Elite Four members! Challenge the Champion!");
                    }
                }
            }
        } else if (leaderData.getRole() == GymRole.CHAMPION) {
            db.grantChampionWin(challengerId, challengerName, seasonId);
            if (server != null) {
                ServerPlayerEntity challenger = server.getPlayerManager().getPlayer(challengerId);
                if (challenger != null) {
                    MessageUtil.sendTitle(challenger, "\u00a76NEW CHAMPION!", "\u00a7e" + challengerName);
                    MessageUtil.broadcast(server, "\u00a7e\u00a7l" + challengerName
                            + " has become the new Champion!");
                }
            }
        }
    }

    public void incrementTurns(UUID leaderId, UUID challengerId) {
        String key = battleKey(leaderId, challengerId);
        battleTurns.merge(key, 1, Integer::sum);
    }

    public int getTurns(UUID leaderId, UUID challengerId) {
        return battleTurns.getOrDefault(battleKey(leaderId, challengerId), 0);
    }

    public UUID getActiveBattle(UUID leaderId) {
        return activeBattles.get(leaderId);
    }

    public boolean isBattleActive(UUID leaderId) {
        return activeBattles.containsKey(leaderId);
    }

    public void requestReplay(int battleId, UUID requesterId) {
        BattleRecord record = DatabaseManager.getInstance().getAllBattleRecords(Integer.MAX_VALUE)
                .stream().filter(r -> r.getId() == battleId).findFirst().orElse(null);
        if (record == null || !record.isCanReplay()) return;
        LOGGER.info("[CobbleGyms] Replay requested for battle {} by {}", battleId, requesterId);
    }

    public void approveReplay(int battleId, UUID adminId) {
        BattleRecord record = DatabaseManager.getInstance().getAllBattleRecords(Integer.MAX_VALUE)
                .stream().filter(r -> r.getId() == battleId).findFirst().orElse(null);
        if (record == null) return;
        DatabaseManager.getInstance().resetChallengeCooldown(record.getChallengerId(), record.getLeaderId());
        DatabaseManager.getInstance().updateBattleReplayFlag(battleId, false);
        if (server != null) {
            ServerPlayerEntity challenger = server.getPlayerManager().getPlayer(record.getChallengerId());
            if (challenger != null) {
                MessageUtil.sendSuccess(challenger, "Replay approved! Your cooldown has been reset.");
            }
        }
        LOGGER.info("[CobbleGyms] Replay approved for battle {} by admin {}", battleId, adminId);
    }

    public List<BattleRecord> getBattleHistory(UUID leaderId, int limit) {
        return DatabaseManager.getInstance().getBattleRecords(leaderId, limit);
    }

    public BattleRecord getRecentLoss(UUID leaderId) {
        return DatabaseManager.getInstance().getLastLoss(leaderId);
    }

    private String battleKey(UUID leaderId, UUID challengerId) {
        return leaderId.toString() + ":" + challengerId.toString();
    }
}
