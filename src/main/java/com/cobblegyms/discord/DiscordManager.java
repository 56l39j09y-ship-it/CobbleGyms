package com.cobblegyms.discord;

import com.cobblegyms.config.CobbleGymsConfig;
import com.cobblegyms.database.DatabaseManager;
import com.cobblegyms.model.BattleRecord;
import com.cobblegyms.model.GymLeaderData;
import com.cobblegyms.model.WeeklyStats;
import com.cobblegyms.system.GymManager;
import com.cobblegyms.util.TimeUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class DiscordManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleGyms");
    private static DiscordManager instance;

    private JDA jda;
    private boolean enabled = false;

    private DiscordManager() {}

    public static DiscordManager getInstance() {
        if (instance == null) instance = new DiscordManager();
        return instance;
    }

    public void initialize() {
        CobbleGymsConfig config = CobbleGymsConfig.getInstance();
        if (config.discordBotToken == null || config.discordBotToken.isEmpty()) {
            LOGGER.info("[CobbleGyms] Discord bot token not set, Discord integration disabled.");
            return;
        }
        try {
            jda = JDABuilder.createLight(config.discordBotToken, GatewayIntent.GUILD_MESSAGES)
                    .build();
            jda.awaitReady();
            enabled = true;
            LOGGER.info("[CobbleGyms] Discord bot connected successfully.");
        } catch (Exception e) {
            LOGGER.warn("[CobbleGyms] Failed to connect to Discord: {}", e.getMessage());
            enabled = false;
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
            jda = null;
            enabled = false;
        }
    }

    public void sendBattleRecord(UUID leaderId, BattleRecord record) {
        if (!enabled || jda == null) return;
        CobbleGymsConfig config = CobbleGymsConfig.getInstance();
        String channelId = config.leaderDiscordChannels.get(leaderId.toString());
        if (channelId == null) return;

        GymLeaderData leader = GymManager.getInstance().getLeader(leaderId);
        String leaderName = leader != null ? leader.getUsername() : leaderId.toString();

        String emoji = record.isWin() ? "✅" : "❌";
        String message = String.format(
                "%s **Battle Record** | Season\n" +
                "**Gym Leader:** %s\n" +
                "**Challenger:** %s\n" +
                "**Result:** %s (%s)\n" +
                "**Turns:** %d\n" +
                "**Time:** %s",
                emoji, leaderName, record.getChallengerName(),
                record.getResult().toUpperCase(), emoji,
                record.getTurns(),
                TimeUtil.formatTimestamp(record.getTimestamp())
        );
        sendToChannel(channelId, message);
    }

    public void sendWeeklyReport(UUID leaderId, WeeklyStats stats) {
        if (!enabled || jda == null) return;
        CobbleGymsConfig config = CobbleGymsConfig.getInstance();
        String channelId = config.leaderDiscordChannels.get(leaderId.toString());
        if (channelId == null) return;

        GymLeaderData leader = GymManager.getInstance().getLeader(leaderId);
        String leaderName = leader != null ? leader.getUsername() : leaderId.toString();

        String message = String.format(
                "📊 **Weekly Summary** for **%s**\n" +
                "Battles: **%d** | Wins: **%d** | Losses: **%d** | Winrate: **%s**",
                leaderName, stats.getBattles(), stats.getWins(), stats.getLosses(), stats.getWinratePercent()
        );
        sendToChannel(channelId, message);
    }

    public void sendWeeklyReportAll(long weekStart) {
        if (!enabled || jda == null) return;
        GymManager.getInstance().getAllLeaders().values().forEach(leader -> {
            WeeklyStats stats = DatabaseManager.getInstance().getWeeklyStats(leader.getLeaderId(), weekStart);
            sendWeeklyReport(leader.getLeaderId(), stats);
        });
    }

    private void sendToChannel(String channelId, String message) {
        if (jda == null) return;
        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage(message).queue(
                        success -> {},
                        error -> LOGGER.warn("[CobbleGyms] Failed to send Discord message: {}", error.getMessage())
                );
            }
        } catch (Exception e) {
            LOGGER.warn("[CobbleGyms] Discord send error: {}", e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
