package com.cobblegyms.database;

import com.cobblegyms.model.*;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleGyms");
    private static DatabaseManager instance;
    private Connection connection;
    private Path dbPath;

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            Path configDir = FabricLoader.getInstance().getConfigDir().resolve("cobblegyms");
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
            dbPath = configDir.resolve("data.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
            connection.setAutoCommit(true);
            createTables();
            LOGGER.info("[CobbleGyms] Database initialized at {}", dbPath);
        } catch (Exception e) {
            LOGGER.error("[CobbleGyms] Database initialization failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS gym_leaders (
                    uuid TEXT PRIMARY KEY,
                    username TEXT NOT NULL,
                    type1 TEXT NOT NULL,
                    type2 TEXT,
                    format TEXT NOT NULL DEFAULT 'SINGLES',
                    gym_type TEXT NOT NULL DEFAULT 'GYM_LEADER',
                    active INTEGER NOT NULL DEFAULT 0,
                    team_slots INTEGER NOT NULL DEFAULT 1,
                    location_x REAL DEFAULT 0,
                    location_y REAL DEFAULT 64,
                    location_z REAL DEFAULT 0,
                    world TEXT DEFAULT 'minecraft:overworld'
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_badges (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL,
                    username TEXT NOT NULL,
                    gym_type TEXT NOT NULL,
                    season_id INTEGER NOT NULL,
                    earned_at INTEGER NOT NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_e4_wins (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL,
                    username TEXT NOT NULL,
                    e4_uuid TEXT NOT NULL,
                    season_id INTEGER NOT NULL,
                    won_at INTEGER NOT NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_champion_wins (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL,
                    username TEXT NOT NULL,
                    season_id INTEGER NOT NULL,
                    won_at INTEGER NOT NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS battle_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    leader_uuid TEXT NOT NULL,
                    challenger_uuid TEXT NOT NULL,
                    challenger_username TEXT NOT NULL,
                    result TEXT NOT NULL,
                    leader_team TEXT,
                    challenger_team TEXT,
                    turns INTEGER NOT NULL DEFAULT 0,
                    timestamp INTEGER NOT NULL,
                    season_id INTEGER NOT NULL,
                    can_replay INTEGER NOT NULL DEFAULT 0
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS battle_queue (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    leader_uuid TEXT NOT NULL,
                    challenger_uuid TEXT NOT NULL,
                    challenger_username TEXT NOT NULL,
                    queued_at INTEGER NOT NULL,
                    status TEXT NOT NULL DEFAULT 'waiting'
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS seasons (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    start_date INTEGER NOT NULL,
                    end_date INTEGER NOT NULL,
                    active INTEGER NOT NULL DEFAULT 1
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_gym_bans (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    banned_uuid TEXT NOT NULL,
                    banner_uuid TEXT NOT NULL,
                    banner_type TEXT NOT NULL,
                    expires_at INTEGER NOT NULL,
                    reason TEXT
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS gym_teams (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    leader_uuid TEXT NOT NULL,
                    team_slot INTEGER NOT NULL,
                    team_data_json TEXT NOT NULL,
                    UNIQUE(leader_uuid, team_slot)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS gym_extra_bans (
                    leader_uuid TEXT NOT NULL,
                    banned_pokemon TEXT NOT NULL,
                    season_id INTEGER NOT NULL,
                    PRIMARY KEY(leader_uuid, season_id)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS weekly_stats (
                    leader_uuid TEXT NOT NULL,
                    week_start INTEGER NOT NULL,
                    battles INTEGER NOT NULL DEFAULT 0,
                    wins INTEGER NOT NULL DEFAULT 0,
                    losses INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(leader_uuid, week_start)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS saved_parties (
                    leader_uuid TEXT PRIMARY KEY,
                    party_json TEXT NOT NULL,
                    saved_at INTEGER NOT NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS challenge_cooldowns (
                    challenger_uuid TEXT NOT NULL,
                    leader_uuid TEXT NOT NULL,
                    last_challenge INTEGER NOT NULL,
                    PRIMARY KEY(challenger_uuid, leader_uuid)
                )
            """);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] Error closing database: {}", e.getMessage());
        }
    }

    // ========== Season methods ==========

    public Season createSeason(long startDate, long endDate) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO seasons (start_date, end_date, active) VALUES (?, ?, 1)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, startDate);
            ps.setLong(2, endDate);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return new Season(keys.getInt(1), startDate, endDate, true);
            }
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] createSeason error: {}", e.getMessage());
        }
        return null;
    }

    public Season getCurrentSeason() {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, start_date, end_date, active FROM seasons WHERE active = 1 ORDER BY id DESC LIMIT 1")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Season(rs.getInt("id"), rs.getLong("start_date"),
                        rs.getLong("end_date"), rs.getBoolean("active"));
            }
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] getCurrentSeason error: {}", e.getMessage());
        }
        return null;
    }

    public void deactivateSeason(int seasonId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE seasons SET active = 0 WHERE id = ?")) {
            ps.setInt(1, seasonId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] deactivateSeason error: {}", e.getMessage());
        }
    }

    // ========== Gym Leader methods ==========

    public void upsertGymLeader(GymLeaderData data) {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO gym_leaders (uuid, username, type1, type2, format, gym_type, active, team_slots,
                    location_x, location_y, location_z, world)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    username=excluded.username, type1=excluded.type1, type2=excluded.type2,
                    format=excluded.format, gym_type=excluded.gym_type, active=excluded.active,
                    team_slots=excluded.team_slots, location_x=excluded.location_x,
                    location_y=excluded.location_y, location_z=excluded.location_z, world=excluded.world
                """)) {
            ps.setString(1, data.getLeaderId().toString());
            ps.setString(2, data.getUsername());
            ps.setString(3, data.getType1().name());
            ps.setString(4, data.getType2() != null ? data.getType2().name() : null);
            ps.setString(5, data.getFormat().name());
            ps.setString(6, data.getRole().name());
            ps.setInt(7, data.isActive() ? 1 : 0);
            ps.setInt(8, data.getTeamSlots());
            ps.setDouble(9, data.getLocationX());
            ps.setDouble(10, data.getLocationY());
            ps.setDouble(11, data.getLocationZ());
            ps.setString(12, data.getWorld() != null ? data.getWorld() : "minecraft:overworld");
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] upsertGymLeader error: {}", e.getMessage());
        }
    }

    public void deleteGymLeader(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM gym_leaders WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] deleteGymLeader error: {}", e.getMessage());
        }
    }

    public List<GymLeaderData> getAllGymLeaders() {
        List<GymLeaderData> leaders = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM gym_leaders")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                GymLeaderData data = mapGymLeader(rs);
                loadTeamData(data);
                leaders.add(data);
            }
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] getAllGymLeaders error: {}", e.getMessage());
        }
        return leaders;
    }

    public GymLeaderData getGymLeader(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM gym_leaders WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                GymLeaderData data = mapGymLeader(rs);
                loadTeamData(data);
                return data;
            }
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] getGymLeader error: {}", e.getMessage());
        }
        return null;
    }

    private GymLeaderData mapGymLeader(ResultSet rs) throws SQLException {
        GymLeaderData data = new GymLeaderData();
        data.setLeaderId(UUID.fromString(rs.getString("uuid")));
        data.setUsername(rs.getString("username"));
        data.setType1(PokemonType.valueOf(rs.getString("type1")));
        String type2Str = rs.getString("type2");
        if (type2Str != null) data.setType2(PokemonType.valueOf(type2Str));
        data.setFormat(BattleFormat.valueOf(rs.getString("format")));
        data.setRole(GymRole.valueOf(rs.getString("gym_type")));
        data.setActive(rs.getInt("active") == 1);
        data.setTeamSlots(rs.getInt("team_slots"));
        data.setLocationX(rs.getDouble("location_x"));
        data.setLocationY(rs.getDouble("location_y"));
        data.setLocationZ(rs.getDouble("location_z"));
        data.setWorld(rs.getString("world"));
        return data;
    }

    private void loadTeamData(GymLeaderData data) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT team_slot, team_data_json FROM gym_teams WHERE leader_uuid = ? ORDER BY team_slot")) {
            ps.setString(1, data.getLeaderId().toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int slot = rs.getInt("team_slot");
                String teamJson = rs.getString("team_data_json");
                data.setTeamForSlot(slot - 1, teamJson);
            }
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] loadTeamData error: {}", e.getMessage());
        }
    }

    public void updateGymLeaderActive(UUID uuid, boolean active) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE gym_leaders SET active = ? WHERE uuid = ?")) {
            ps.setInt(1, active ? 1 : 0);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] updateGymLeaderActive error: {}", e.getMessage());
        }
    }

    // ========== Battle Records ==========

    public int insertBattleRecord(BattleRecord record) {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO battle_records (leader_uuid, challenger_uuid, challenger_username,
                    result, leader_team, challenger_team, turns, timestamp, season_id, can_replay)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, record.getLeaderId().toString());
            ps.setString(2, record.getChallengerId().toString());
            ps.setString(3, record.getChallengerName());
            ps.setString(4, record.getResult());
            ps.setString(5, record.getLeaderTeamData());
            ps.setString(6, record.getChallengerTeamData());
            ps.setInt(7, record.getTurns());
            ps.setLong(8, record.getTimestamp());
            ps.setLong(9, record.getSeasonId());
            ps.setInt(10, record.isCanReplay() ? 1 : 0);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] insertBattleRecord error: {}", e.getMessage());
        }
        return -1;
    }

    public List<BattleRecord> getBattleRecords(UUID leaderId, int limit) {
        List<BattleRecord> records = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM battle_records WHERE leader_uuid = ? ORDER BY timestamp DESC LIMIT ?")) {
            ps.setString(1, leaderId.toString());
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) records.add(mapBattleRecord(rs));
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] getBattleRecords error: {}", e.getMessage());
        }
        return records;
    }

    public List<BattleRecord> getAllBattleRecords(int limit) {
        List<BattleRecord> records = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM battle_records ORDER BY timestamp DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) records.add(mapBattleRecord(rs));
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] getAllBattleRecords error: {}", e.getMessage());
        }
        return records;
    }

    public BattleRecord getLastLoss(UUID leaderId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM battle_records WHERE leader_uuid = ? AND result = 'loss' ORDER BY timestamp DESC LIMIT 1")) {
            ps.setString(1, leaderId.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapBattleRecord(rs);
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] getLastLoss error: {}", e.getMessage());
        }
        return null;
    }

    public void updateBattleReplayFlag(int battleId, boolean canReplay) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE battle_records SET can_replay = ? WHERE id = ?")) {
            ps.setInt(1, canReplay ? 1 : 0);
            ps.setInt(2, battleId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] updateBattleReplayFlag error: {}", e.getMessage());
        }
    }

    private BattleRecord mapBattleRecord(ResultSet rs) throws SQLException {
        BattleRecord r = new BattleRecord();
        r.setId(rs.getInt("id"));
        r.setLeaderId(UUID.fromString(rs.getString("leader_uuid")));
        r.setChallengerId(UUID.fromString(rs.getString("challenger_uuid")));
        r.setChallengerName(rs.getString("challenger_username"));
        r.setResult(rs.getString("result"));
        r.setLeaderTeamData(rs.getString("leader_team"));
        r.setChallengerTeamData(rs.getString("challenger_team"));
        r.setTurns(rs.getInt("turns"));
        r.setTimestamp(rs.getLong("timestamp"));
        r.setSeasonId(rs.getLong("season_id"));
        r.setCanReplay(rs.getInt("can_replay") == 1);
        return r;
    }

    // ========== Queue methods ==========

    public int insertQueueEntry(QueueEntry entry) {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO battle_queue (leader_uuid, challenger_uuid, challenger_username, queued_at, status)
                VALUES (?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entry.getLeaderId().toString());
            ps.setString(2, entry.getChallengerId().toString());
            ps.setString(3, entry.getChallengerName());
            ps.setLong(4, entry.getQueuedAt());
            ps.setString(5, entry.getStatus());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] insertQueueEntry error: {}", e.getMessage());
        }
        return -1;
    }

    public List<QueueEntry> getQueueForLeader(UUID leaderId) {
        List<QueueEntry> queue = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM battle_queue WHERE leader_uuid = ? AND status = 'waiting' ORDER BY queued_at ASC")) {
            ps.setString(1, leaderId.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) queue.add(mapQueueEntry(rs));
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] getQueueForLeader error: {}", e.getMessage());
        }
        return queue;
    }

    public QueueEntry getActiveQueueEntry(UUID leaderId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM battle_queue WHERE leader_uuid = ? AND status = 'active' LIMIT 1")) {
            ps.setString(1, leaderId.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapQueueEntry(rs);
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] getActiveQueueEntry error: {}", e.getMessage());
        }
        return null;
    }

    public boolean isInAnyQueue(UUID challengerId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM battle_queue WHERE challenger_uuid = ? AND status IN ('waiting', 'active')")) {
            ps.setString(1, challengerId.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] isInAnyQueue error: {}", e.getMessage());
        }
        return false;
    }

    public void updateQueueStatus(int entryId, String status) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE battle_queue SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, entryId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] updateQueueStatus error: {}", e.getMessage());
        }
    }

    public void cancelQueueForLeader(UUID leaderId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE battle_queue SET status = 'cancelled' WHERE leader_uuid = ? AND status = 'waiting'")) {
            ps.setString(1, leaderId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] cancelQueueForLeader error: {}", e.getMessage());
        }
    }

    public int getQueuePosition(UUID challengerId, UUID leaderId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM battle_queue WHERE leader_uuid = ? AND status = 'waiting' AND queued_at <= (SELECT queued_at FROM battle_queue WHERE challenger_uuid = ? AND leader_uuid = ? AND status = 'waiting' LIMIT 1)")) {
            ps.setString(1, leaderId.toString());
            ps.setString(2, challengerId.toString());
            ps.setString(3, leaderId.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] getQueuePosition error: {}", e.getMessage());
        }
        return -1;
    }

    private QueueEntry mapQueueEntry(ResultSet rs) throws SQLException {
        QueueEntry e = new QueueEntry();
        e.setId(rs.getInt("id"));
        e.setLeaderId(UUID.fromString(rs.getString("leader_uuid")));
        e.setChallengerId(UUID.fromString(rs.getString("challenger_uuid")));
        e.setChallengerName(rs.getString("challenger_username"));
        e.setQueuedAt(rs.getLong("queued_at"));
        e.setStatus(rs.getString("status"));
        return e;
    }

    // ========== Badge methods ==========

    public void grantBadge(UUID playerUuid, String playerName, String gymType, int seasonId) {
        if (hasBadge(playerUuid, gymType, seasonId)) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO player_badges (uuid, username, gym_type, season_id, earned_at) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerName);
            ps.setString(3, gymType);
            ps.setInt(4, seasonId);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] grantBadge error: {}", e.getMessage());
        }
    }

    public boolean hasBadge(UUID playerUuid, String gymType, int seasonId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM player_badges WHERE uuid = ? AND gym_type = ? AND season_id = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, gymType);
            ps.setInt(3, seasonId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] hasBadge error: {}", e.getMessage());
        }
        return false;
    }

    public List<String> getBadgesForSeason(UUID playerUuid, int seasonId) {
        List<String> badges = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT gym_type FROM player_badges WHERE uuid = ? AND season_id = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.setInt(2, seasonId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) badges.add(rs.getString("gym_type"));
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] getBadgesForSeason error: {}", e.getMessage());
        }
        return badges;
    }

    public int countBadgesForSeason(UUID playerUuid, int seasonId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM player_badges WHERE uuid = ? AND season_id = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.setInt(2, seasonId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] countBadgesForSeason error: {}", e.getMessage());
        }
        return 0;
    }

    // ========== E4 / Champion wins ==========

    public void grantE4Win(UUID playerUuid, String playerName, UUID e4Uuid, int seasonId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO player_e4_wins (uuid, username, e4_uuid, season_id, won_at) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerName);
            ps.setString(3, e4Uuid.toString());
            ps.setInt(4, seasonId);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] grantE4Win error: {}", e.getMessage());
        }
    }

    public int countE4WinsForSeason(UUID playerUuid, int seasonId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(DISTINCT e4_uuid) FROM player_e4_wins WHERE uuid = ? AND season_id = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.setInt(2, seasonId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] countE4WinsForSeason error: {}", e.getMessage());
        }
        return 0;
    }

    public void grantChampionWin(UUID playerUuid, String playerName, int seasonId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO player_champion_wins (uuid, username, season_id, won_at) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerName);
            ps.setInt(3, seasonId);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] grantChampionWin error: {}", e.getMessage());
        }
    }

    // ========== Bans ==========

    public void insertBan(GymBan ban) {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO player_gym_bans (banned_uuid, banner_uuid, banner_type, expires_at, reason)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            ps.setString(1, ban.getBannedUuid().toString());
            ps.setString(2, ban.getBannerUuid().toString());
            ps.setString(3, ban.getBannerType().name());
            ps.setLong(4, ban.getExpiresAt());
            ps.setString(5, ban.getReason());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] insertBan error: {}", e.getMessage());
        }
    }

    public List<GymBan> getActiveBansForBanner(UUID bannerUuid) {
        List<GymBan> bans = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM player_gym_bans WHERE banner_uuid = ? AND expires_at > ?")) {
            ps.setString(1, bannerUuid.toString());
            ps.setLong(2, System.currentTimeMillis());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) bans.add(mapBan(rs));
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] getActiveBansForBanner error: {}", e.getMessage());
        }
        return bans;
    }

    public boolean isBanned(UUID bannedUuid, UUID bannerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM player_gym_bans WHERE banned_uuid = ? AND banner_uuid = ? AND expires_at > ?")) {
            ps.setString(1, bannedUuid.toString());
            ps.setString(2, bannerUuid.toString());
            ps.setLong(3, System.currentTimeMillis());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] isBanned error: {}", e.getMessage());
        }
        return false;
    }

    public void removeBan(UUID bannedUuid, UUID bannerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM player_gym_bans WHERE banned_uuid = ? AND banner_uuid = ?")) {
            ps.setString(1, bannedUuid.toString());
            ps.setString(2, bannerUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] removeBan error: {}", e.getMessage());
        }
    }

    public void cleanupExpiredBans() {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM player_gym_bans WHERE expires_at <= ?")) {
            ps.setLong(1, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] cleanupExpiredBans error: {}", e.getMessage());
        }
    }

    private GymBan mapBan(ResultSet rs) throws SQLException {
        GymBan ban = new GymBan();
        ban.setId(rs.getInt("id"));
        ban.setBannedUuid(UUID.fromString(rs.getString("banned_uuid")));
        ban.setBannerUuid(UUID.fromString(rs.getString("banner_uuid")));
        ban.setBannerType(GymRole.valueOf(rs.getString("banner_type")));
        ban.setExpiresAt(rs.getLong("expires_at"));
        ban.setReason(rs.getString("reason"));
        return ban;
    }

    // ========== Weekly Stats ==========

    public WeeklyStats getWeeklyStats(UUID leaderId, long weekStart) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM weekly_stats WHERE leader_uuid = ? AND week_start = ?")) {
            ps.setString(1, leaderId.toString());
            ps.setLong(2, weekStart);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                WeeklyStats stats = new WeeklyStats(leaderId, weekStart);
                stats.setBattles(rs.getInt("battles"));
                stats.setWins(rs.getInt("wins"));
                stats.setLosses(rs.getInt("losses"));
                return stats;
            }
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] getWeeklyStats error: {}", e.getMessage());
        }
        return new WeeklyStats(leaderId, weekStart);
    }

    public void updateWeeklyStats(UUID leaderId, long weekStart, boolean won) {
        try {
            WeeklyStats existing = getWeeklyStats(leaderId, weekStart);
            if (existing.getBattles() == 0) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO weekly_stats (leader_uuid, week_start, battles, wins, losses) VALUES (?, ?, 1, ?, ?)")) {
                    ps.setString(1, leaderId.toString());
                    ps.setLong(2, weekStart);
                    ps.setInt(3, won ? 1 : 0);
                    ps.setInt(4, won ? 0 : 1);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE weekly_stats SET battles = battles + 1, wins = wins + ?, losses = losses + ? WHERE leader_uuid = ? AND week_start = ?")) {
                    ps.setInt(1, won ? 1 : 0);
                    ps.setInt(2, won ? 0 : 1);
                    ps.setString(3, leaderId.toString());
                    ps.setLong(4, weekStart);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] updateWeeklyStats error: {}", e.getMessage());
        }
    }

    // ========== Team management ==========

    public void saveTeamSlot(UUID leaderId, int slot, String teamJson) {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO gym_teams (leader_uuid, team_slot, team_data_json)
                VALUES (?, ?, ?)
                ON CONFLICT(leader_uuid, team_slot) DO UPDATE SET team_data_json=excluded.team_data_json
                """)) {
            ps.setString(1, leaderId.toString());
            ps.setInt(2, slot);
            ps.setString(3, teamJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] saveTeamSlot error: {}", e.getMessage());
        }
    }

    public String getTeamSlot(UUID leaderId, int slot) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT team_data_json FROM gym_teams WHERE leader_uuid = ? AND team_slot = ?")) {
            ps.setString(1, leaderId.toString());
            ps.setInt(2, slot);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("team_data_json");
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] getTeamSlot error: {}", e.getMessage());
        }
        return null;
    }

    public void saveParty(UUID leaderId, String partyJson) {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO saved_parties (leader_uuid, party_json, saved_at)
                VALUES (?, ?, ?)
                ON CONFLICT(leader_uuid) DO UPDATE SET party_json=excluded.party_json, saved_at=excluded.saved_at
                """)) {
            ps.setString(1, leaderId.toString());
            ps.setString(2, partyJson);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] saveParty error: {}", e.getMessage());
        }
    }

    public String getSavedParty(UUID leaderId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT party_json FROM saved_parties WHERE leader_uuid = ?")) {
            ps.setString(1, leaderId.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("party_json");
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] getSavedParty error: {}", e.getMessage());
        }
        return null;
    }

    public void deleteSavedParty(UUID leaderId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM saved_parties WHERE leader_uuid = ?")) {
            ps.setString(1, leaderId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] deleteSavedParty error: {}", e.getMessage());
        }
    }

    // ========== Challenge cooldowns ==========

    public void setChallengeCooldown(UUID challengerUuid, UUID leaderUuid) {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO challenge_cooldowns (challenger_uuid, leader_uuid, last_challenge)
                VALUES (?, ?, ?)
                ON CONFLICT(challenger_uuid, leader_uuid) DO UPDATE SET last_challenge=excluded.last_challenge
                """)) {
            ps.setString(1, challengerUuid.toString());
            ps.setString(2, leaderUuid.toString());
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] setChallengeCooldown error: {}", e.getMessage());
        }
    }

    public long getLastChallengeTime(UUID challengerUuid, UUID leaderUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT last_challenge FROM challenge_cooldowns WHERE challenger_uuid = ? AND leader_uuid = ?")) {
            ps.setString(1, challengerUuid.toString());
            ps.setString(2, leaderUuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("last_challenge");
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] getLastChallengeTime error: {}", e.getMessage());
        }
        return 0L;
    }

    public void resetChallengeCooldown(UUID challengerUuid, UUID leaderUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM challenge_cooldowns WHERE challenger_uuid = ? AND leader_uuid = ?")) {
            ps.setString(1, challengerUuid.toString());
            ps.setString(2, leaderUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] resetChallengeCooldown error: {}", e.getMessage());
        }
    }

    // ========== Rankings ==========

    public List<WeeklyStats> getAllWeeklyStats(long weekStart) {
        List<WeeklyStats> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM weekly_stats WHERE week_start = ? ORDER BY wins DESC, battles DESC")) {
            ps.setLong(1, weekStart);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                WeeklyStats stats = new WeeklyStats(
                        UUID.fromString(rs.getString("leader_uuid")),
                        rs.getLong("week_start"));
                stats.setBattles(rs.getInt("battles"));
                stats.setWins(rs.getInt("wins"));
                stats.setLosses(rs.getInt("losses"));
                list.add(stats);
            }
        } catch (SQLException e) {
            LOGGER.error("[CobbleGyms] getAllWeeklyStats error: {}", e.getMessage());
        }
        return list;
    }
}
