package com.cobblegyms.system;

import com.cobblegyms.database.DatabaseManager;
import com.cobblegyms.model.QueueEntry;
import com.cobblegyms.util.MessageUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class QueueManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleGyms");
    private static QueueManager instance;
    private MinecraftServer server;

    private QueueManager() {}

    public static QueueManager getInstance() {
        if (instance == null) instance = new QueueManager();
        return instance;
    }

    public void initialize(MinecraftServer server) {
        this.server = server;
    }

    public int addToQueue(UUID leaderId, UUID challengerId, String challengerName) {
        if (DatabaseManager.getInstance().isInAnyQueue(challengerId)) return -1;
        QueueEntry entry = new QueueEntry(leaderId, challengerId, challengerName);
        int id = DatabaseManager.getInstance().insertQueueEntry(entry);
        entry.setId(id);

        int position = getBattlePosition(challengerId, leaderId);
        if (server != null) {
            ServerPlayerEntity challenger = server.getPlayerManager().getPlayer(challengerId);
            if (challenger != null) {
                MessageUtil.sendSuccess(challenger, "Joined queue! Position: " + position);
            }
            ServerPlayerEntity leader = server.getPlayerManager().getPlayer(leaderId);
            if (leader != null) {
                MessageUtil.sendInfo(leader, challengerName + " joined your queue. Queue size: "
                        + getQueue(leaderId).size());
            }
        }
        return id;
    }

    public void removeFromQueue(int queueId) {
        DatabaseManager.getInstance().updateQueueStatus(queueId, QueueEntry.STATUS_CANCELLED);
    }

    public List<QueueEntry> getQueue(UUID leaderId) {
        return DatabaseManager.getInstance().getQueueForLeader(leaderId);
    }

    public boolean isInQueue(UUID challengerId) {
        return DatabaseManager.getInstance().isInAnyQueue(challengerId);
    }

    public QueueEntry startNextBattle(UUID leaderId) {
        List<QueueEntry> queue = getQueue(leaderId);
        if (queue.isEmpty()) return null;
        QueueEntry next = queue.get(0);
        DatabaseManager.getInstance().updateQueueStatus(next.getId(), QueueEntry.STATUS_ACTIVE);
        if (server != null) {
            ServerPlayerEntity leader = server.getPlayerManager().getPlayer(leaderId);
            ServerPlayerEntity challenger = server.getPlayerManager().getPlayer(next.getChallengerId());
            if (challenger != null) {
                MessageUtil.sendSuccess(challenger, "Your battle is starting! Good luck!");
            }
            if (leader != null) {
                MessageUtil.sendInfo(leader, "Starting battle with " + next.getChallengerName());
            }
            if (leader != null && challenger != null) {
                BattleManager.getInstance().startBattle(leaderId, next.getChallengerId());
            }
        }
        return next;
    }

    public void cancelQueue(UUID leaderId) {
        DatabaseManager.getInstance().cancelQueueForLeader(leaderId);
        if (server != null) {
            ServerPlayerEntity leader = server.getPlayerManager().getPlayer(leaderId);
            if (leader != null) {
                MessageUtil.sendInfo(leader, "All pending queue entries cancelled.");
            }
        }
    }

    public int getBattlePosition(UUID challengerId, UUID leaderId) {
        return DatabaseManager.getInstance().getQueuePosition(challengerId, leaderId);
    }

    public QueueEntry getActiveEntry(UUID leaderId) {
        return DatabaseManager.getInstance().getActiveQueueEntry(leaderId);
    }

    public void markDone(int queueId) {
        DatabaseManager.getInstance().updateQueueStatus(queueId, QueueEntry.STATUS_DONE);
    }
}
