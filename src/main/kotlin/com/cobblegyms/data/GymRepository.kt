package com.cobblegyms.data

import com.cobblegyms.data.models.*
import java.sql.ResultSet
import java.util.UUID

object GymRepository {
    private fun conn() = DatabaseManager.getConnection()
    
    // ===== GYM LEADERS =====
    
    fun getGymLeader(uuid: UUID): GymLeaderData? {
        val sql = "SELECT * FROM gym_leaders WHERE player_uuid = ?"
        conn().prepareStatement(sql).use { stmt ->
            stmt.setString(1, uuid.toString())
            val rs = stmt.executeQuery()
            return if (rs.next()) mapGymLeader(rs) else null
        }
    }
    
    fun getGymLeaderByType(type: PokemonType): GymLeaderData? {
        val sql = "SELECT * FROM gym_leaders WHERE gym_type = ?"
        conn().prepareStatement(sql).use { stmt ->
            stmt.setString(1, type.name)
            val rs = stmt.executeQuery()
            return if (rs.next()) mapGymLeader(rs) else null
        }
    }
    
    fun getAllGymLeaders(): List<GymLeaderData> {
        val sql = "SELECT * FROM gym_leaders"
        conn().createStatement().use { stmt ->
            val rs = stmt.executeQuery(sql)
            val list = mutableListOf<GymLeaderData>()
            while (rs.next()) list.add(mapGymLeader(rs))
            return list
        }
    }
    
    fun upsertGymLeader(leader: GymLeaderData) {
        val sql = """
            INSERT INTO gym_leaders (player_uuid, player_name, gym_type, battle_format, is_open, gym_location,
                extra_banned_pokemon, extra_ban_season, multi_team_enabled, team1, team2, team3, current_team_slot)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(player_uuid) DO UPDATE SET
                player_name=excluded.player_name,
                gym_type=excluded.gym_type,
                battle_format=excluded.battle_format,
                is_open=excluded.is_open,
                gym_location=excluded.gym_location,
                extra_banned_pokemon=excluded.extra_banned_pokemon,
                extra_ban_season=excluded.extra_ban_season,
                multi_team_enabled=excluded.multi_team_enabled,
                team1=excluded.team1,
                team2=excluded.team2,
                team3=excluded.team3,
                current_team_slot=excluded.current_team_slot
        """.trimIndent()
        conn().prepareStatement(sql).use { stmt ->
            stmt.setString(1, leader.playerUuid.toString())
            stmt.setString(2, leader.playerName)
            stmt.setString(3, leader.gymType.name)
            stmt.setString(4, leader.battleFormat.name)
            stmt.setInt(5, if (leader.isOpen) 1 else 0)
            stmt.setString(6, leader.gymLocation?.toJson())
            stmt.setString(7, leader.extraBannedPokemon)
            stmt.setInt(8, leader.extraBanSeason)
            stmt.setInt(9, if (leader.multiTeamEnabled) 1 else 0)
            stmt.setString(10, leader.team1)
            stmt.setString(11, leader.team2)
            stmt.setString(12, leader.team3)
            stmt.setInt(13, leader.currentTeamSlot)
            stmt.executeUpdate()
        }
    }
    
    fun deleteGymLeader(uuid: UUID) {
        conn().prepareStatement("DELETE FROM gym_leaders WHERE player_uuid = ?").use { stmt ->
            stmt.setString(1, uuid.toString())
            stmt.executeUpdate()
        }
    }
    
    // ===== ELITE FOUR =====
    
    fun getEliteFour(uuid: UUID): EliteFourData? {
        conn().prepareStatement("SELECT * FROM elite_four WHERE player_uuid = ?").use { stmt ->
            stmt.setString(1, uuid.toString())
            val rs = stmt.executeQuery()
            return if (rs.next()) mapEliteFour(rs) else null
        }
    }
    
    fun getAllEliteFour(): List<EliteFourData> {
        val list = mutableListOf<EliteFourData>()
        conn().createStatement().use { stmt ->
            val rs = stmt.executeQuery("SELECT * FROM elite_four")
            while (rs.next()) list.add(mapEliteFour(rs))
        }
        return list
    }
    
    fun upsertEliteFour(e4: EliteFourData) {
        val sql = """
            INSERT INTO elite_four (player_uuid, player_name, type1, type2, battle_format, is_open, arena_location,
                multi_team_enabled, team1, team2, team3, current_team_slot)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(player_uuid) DO UPDATE SET
                player_name=excluded.player_name,
                type1=excluded.type1,
                type2=excluded.type2,
                battle_format=excluded.battle_format,
                is_open=excluded.is_open,
                arena_location=excluded.arena_location,
                multi_team_enabled=excluded.multi_team_enabled,
                team1=excluded.team1,
                team2=excluded.team2,
                team3=excluded.team3,
                current_team_slot=excluded.current_team_slot
        """.trimIndent()
        conn().prepareStatement(sql).use { stmt ->
            stmt.setString(1, e4.playerUuid.toString())
            stmt.setString(2, e4.playerName)
            stmt.setString(3, e4.type1.name)
            stmt.setString(4, e4.type2.name)
            stmt.setString(5, e4.battleFormat.name)
            stmt.setInt(6, if (e4.isOpen) 1 else 0)
            stmt.setString(7, e4.arenaLocation?.toJson())
            stmt.setInt(8, if (e4.multiTeamEnabled) 1 else 0)
            stmt.setString(9, e4.team1)
            stmt.setString(10, e4.team2)
            stmt.setString(11, e4.team3)
            stmt.setInt(12, e4.currentTeamSlot)
            stmt.executeUpdate()
        }
    }
    
    fun deleteEliteFour(uuid: UUID) {
        conn().prepareStatement("DELETE FROM elite_four WHERE player_uuid = ?").use { stmt ->
            stmt.setString(1, uuid.toString())
            stmt.executeUpdate()
        }
    }
    
    // ===== CHAMPION =====
    
    fun getChampion(): ChampionData? {
        val rs = conn().createStatement().executeQuery("SELECT * FROM champion LIMIT 1")
        return if (rs.next()) mapChampion(rs) else null
    }
    
    fun setChampion(champion: ChampionData) {
        conn().createStatement().execute("DELETE FROM champion")
        val sql = """
            INSERT INTO champion (player_uuid, player_name, battle_format, is_open, arena_location,
                multi_team_enabled, team1, team2, team3, current_team_slot)
            VALUES (?,?,?,?,?,?,?,?,?,?)
        """.trimIndent()
        conn().prepareStatement(sql).use { stmt ->
            stmt.setString(1, champion.playerUuid.toString())
            stmt.setString(2, champion.playerName)
            stmt.setString(3, champion.battleFormat.name)
            stmt.setInt(4, if (champion.isOpen) 1 else 0)
            stmt.setString(5, champion.arenaLocation?.toJson())
            stmt.setInt(6, if (champion.multiTeamEnabled) 1 else 0)
            stmt.setString(7, champion.team1)
            stmt.setString(8, champion.team2)
            stmt.setString(9, champion.team3)
            stmt.setInt(10, champion.currentTeamSlot)
            stmt.executeUpdate()
        }
    }
    
    fun removeChampion() {
        conn().createStatement().execute("DELETE FROM champion")
    }
    
    // ===== PLAYER BADGES =====
    
    fun getPlayerBadges(playerUuid: UUID, seasonId: Int): List<PlayerBadge> {
        val list = mutableListOf<PlayerBadge>()
        conn().prepareStatement("SELECT * FROM player_badges WHERE player_uuid = ? AND season_id = ?").use { stmt ->
            stmt.setString(1, playerUuid.toString())
            stmt.setInt(2, seasonId)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                list.add(PlayerBadge(
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
                    rs.getInt("season_id"),
                    PokemonType.valueOf(rs.getString("gym_type")),
                    rs.getLong("earned_at")
                ))
            }
        }
        return list
    }
    
    fun awardBadge(playerUuid: UUID, playerName: String, seasonId: Int, gymType: PokemonType) {
        conn().prepareStatement("""
            INSERT OR IGNORE INTO player_badges (player_uuid, player_name, season_id, gym_type)
            VALUES (?,?,?,?)
        """).use { stmt ->
            stmt.setString(1, playerUuid.toString())
            stmt.setString(2, playerName)
            stmt.setInt(3, seasonId)
            stmt.setString(4, gymType.name)
            stmt.executeUpdate()
        }
    }
    
    fun hasBadge(playerUuid: UUID, seasonId: Int, gymType: PokemonType): Boolean {
        conn().prepareStatement(
            "SELECT 1 FROM player_badges WHERE player_uuid=? AND season_id=? AND gym_type=?"
        ).use { stmt ->
            stmt.setString(1, playerUuid.toString())
            stmt.setInt(2, seasonId)
            stmt.setString(3, gymType.name)
            return stmt.executeQuery().next()
        }
    }
    
    fun hasAllBadges(playerUuid: UUID, seasonId: Int): Boolean {
        val allTypes = getAllGymLeaders().map { it.gymType }.toSet()
        if (allTypes.isEmpty()) return false
        return allTypes.all { hasBadge(playerUuid, seasonId, it) }
    }
    
    // ===== E4 VICTORIES =====
    
    fun getPlayerE4Victories(playerUuid: UUID, seasonId: Int): List<PlayerE4Victory> {
        val list = mutableListOf<PlayerE4Victory>()
        conn().prepareStatement("SELECT * FROM player_e4_victories WHERE player_uuid=? AND season_id=?").use { stmt ->
            stmt.setString(1, playerUuid.toString())
            stmt.setInt(2, seasonId)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                list.add(PlayerE4Victory(
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
                    rs.getInt("season_id"),
                    UUID.fromString(rs.getString("e4_uuid")),
                    rs.getString("e4_name"),
                    rs.getLong("earned_at")
                ))
            }
        }
        return list
    }
    
    fun awardE4Victory(playerUuid: UUID, playerName: String, seasonId: Int, e4Uuid: UUID, e4Name: String) {
        conn().prepareStatement("""
            INSERT OR IGNORE INTO player_e4_victories (player_uuid, player_name, season_id, e4_uuid, e4_name)
            VALUES (?,?,?,?,?)
        """).use { stmt ->
            stmt.setString(1, playerUuid.toString())
            stmt.setString(2, playerName)
            stmt.setInt(3, seasonId)
            stmt.setString(4, e4Uuid.toString())
            stmt.setString(5, e4Name)
            stmt.executeUpdate()
        }
    }
    
    fun hasBeatenAllE4(playerUuid: UUID, seasonId: Int): Boolean {
        val allE4 = getAllEliteFour()
        if (allE4.isEmpty()) return false
        val victories = getPlayerE4Victories(playerUuid, seasonId)
        return allE4.all { e4 -> victories.any { it.e4Uuid == e4.playerUuid } }
    }
    
    // ===== BATTLE RECORDS =====
    
    fun saveBattleRecord(record: BattleRecord) {
        conn().prepareStatement("""
            INSERT INTO battle_records (season_id, battle_type, leader_uuid, leader_name,
                challenger_uuid, challenger_name, winner, challenger_team, leader_team, turns, notes)
            VALUES (?,?,?,?,?,?,?,?,?,?,?)
        """).use { stmt ->
            stmt.setInt(1, record.seasonId)
            stmt.setString(2, record.battleType)
            stmt.setString(3, record.leaderUuid.toString())
            stmt.setString(4, record.leaderName)
            stmt.setString(5, record.challengerUuid.toString())
            stmt.setString(6, record.challengerName)
            stmt.setString(7, record.winner)
            stmt.setString(8, record.challengerTeam)
            stmt.setString(9, record.leaderTeam)
            stmt.setInt(10, record.turns)
            stmt.setString(11, record.notes)
            stmt.executeUpdate()
        }
    }
    
    fun getBattleRecordsForLeader(leaderUuid: UUID, limit: Int = 50): List<BattleRecord> {
        val list = mutableListOf<BattleRecord>()
        conn().prepareStatement(
            "SELECT * FROM battle_records WHERE leader_uuid=? ORDER BY battle_time DESC LIMIT ?"
        ).use { stmt ->
            stmt.setString(1, leaderUuid.toString())
            stmt.setInt(2, limit)
            val rs = stmt.executeQuery()
            while (rs.next()) list.add(mapBattleRecord(rs))
        }
        return list
    }
    
    // ===== COOLDOWNS =====
    
    fun getChallengeCooldown(challengerUuid: UUID, targetUuid: UUID, targetType: String): Long? {
        conn().prepareStatement(
            "SELECT last_challenge FROM challenge_cooldowns WHERE challenger_uuid=? AND target_uuid=? AND target_type=?"
        ).use { stmt ->
            stmt.setString(1, challengerUuid.toString())
            stmt.setString(2, targetUuid.toString())
            stmt.setString(3, targetType)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.getLong("last_challenge") else null
        }
    }
    
    fun setChallengeCooldown(challengerUuid: UUID, targetUuid: UUID, targetType: String, time: Long) {
        conn().prepareStatement("""
            INSERT INTO challenge_cooldowns (challenger_uuid, target_uuid, target_type, last_challenge)
            VALUES (?,?,?,?)
            ON CONFLICT(challenger_uuid, target_uuid, target_type) DO UPDATE SET last_challenge=excluded.last_challenge
        """).use { stmt ->
            stmt.setString(1, challengerUuid.toString())
            stmt.setString(2, targetUuid.toString())
            stmt.setString(3, targetType)
            stmt.setLong(4, time)
            stmt.executeUpdate()
        }
    }
    
    // ===== GYM BANS =====
    
    fun addGymBan(ban: GymBan) {
        conn().prepareStatement("""
            INSERT INTO gym_bans (gym_leader_uuid, banned_player_uuid, banned_player_name, reason, ban_until)
            VALUES (?,?,?,?,?)
        """).use { stmt ->
            stmt.setString(1, ban.gymLeaderUuid.toString())
            stmt.setString(2, ban.bannedPlayerUuid.toString())
            stmt.setString(3, ban.bannedPlayerName)
            stmt.setString(4, ban.reason)
            stmt.setLong(5, ban.banUntil)
            stmt.executeUpdate()
        }
    }
    
    fun isPlayerBanned(gymLeaderUuid: UUID, playerUuid: UUID): Boolean {
        conn().prepareStatement(
            "SELECT 1 FROM gym_bans WHERE gym_leader_uuid=? AND banned_player_uuid=? AND ban_until>?"
        ).use { stmt ->
            stmt.setString(1, gymLeaderUuid.toString())
            stmt.setString(2, playerUuid.toString())
            stmt.setLong(3, System.currentTimeMillis() / 1000)
            return stmt.executeQuery().next()
        }
    }
    
    fun removeGymBan(gymLeaderUuid: UUID, playerUuid: UUID) {
        conn().prepareStatement(
            "DELETE FROM gym_bans WHERE gym_leader_uuid=? AND banned_player_uuid=?"
        ).use { stmt ->
            stmt.setString(1, gymLeaderUuid.toString())
            stmt.setString(2, playerUuid.toString())
            stmt.executeUpdate()
        }
    }
    
    // ===== WEEKLY STATS =====
    
    fun getWeeklyStats(playerUuid: UUID, weekStart: Long): WeeklyStats? {
        conn().prepareStatement(
            "SELECT * FROM weekly_stats WHERE player_uuid=? AND week_start=?"
        ).use { stmt ->
            stmt.setString(1, playerUuid.toString())
            stmt.setLong(2, weekStart)
            val rs = stmt.executeQuery()
            if (!rs.next()) return null
            return WeeklyStats(
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("player_name"),
                rs.getString("role"),
                rs.getLong("week_start"),
                rs.getInt("battles"),
                rs.getInt("wins"),
                rs.getInt("losses")
            )
        }
    }
    
    fun incrementWeeklyStats(playerUuid: UUID, playerName: String, role: String, weekStart: Long, win: Boolean) {
        conn().prepareStatement("""
            INSERT INTO weekly_stats (player_uuid, player_name, role, week_start, battles, wins, losses)
            VALUES (?,?,?,?,1,?,?)
            ON CONFLICT(player_uuid, week_start) DO UPDATE SET
                battles=battles+1,
                wins=wins+?,
                losses=losses+?
        """).use { stmt ->
            stmt.setString(1, playerUuid.toString())
            stmt.setString(2, playerName)
            stmt.setString(3, role)
            stmt.setLong(4, weekStart)
            stmt.setInt(5, if (win) 1 else 0)
            stmt.setInt(6, if (!win) 1 else 0)
            stmt.setInt(7, if (win) 1 else 0)
            stmt.setInt(8, if (!win) 1 else 0)
            stmt.executeUpdate()
        }
    }
    
    fun getWeeklyLeaderboard(role: String, weekStart: Long): List<WeeklyStats> {
        val list = mutableListOf<WeeklyStats>()
        conn().prepareStatement(
            "SELECT * FROM weekly_stats WHERE role=? AND week_start=? ORDER BY wins DESC, battles DESC"
        ).use { stmt ->
            stmt.setString(1, role)
            stmt.setLong(2, weekStart)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                list.add(WeeklyStats(
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
                    rs.getString("role"),
                    rs.getLong("week_start"),
                    rs.getInt("battles"),
                    rs.getInt("wins"),
                    rs.getInt("losses")
                ))
            }
        }
        return list
    }
    
    // ===== SEASONS =====
    
    fun getCurrentSeason(): SeasonData? {
        val rs = conn().createStatement().executeQuery(
            "SELECT * FROM seasons WHERE active=1 ORDER BY id DESC LIMIT 1"
        )
        return if (rs.next()) mapSeason(rs) else null
    }
    
    fun createSeason(number: Int, startTime: Long): SeasonData {
        conn().prepareStatement(
            "INSERT INTO seasons (number, start_time, active) VALUES (?,?,1)"
        ).use { stmt ->
            stmt.setInt(1, number)
            stmt.setLong(2, startTime)
            stmt.executeUpdate()
        }
        return getCurrentSeason()!!
    }
    
    fun endSeason(seasonId: Int, endTime: Long) {
        conn().prepareStatement(
            "UPDATE seasons SET active=0, end_time=? WHERE id=?"
        ).use { stmt ->
            stmt.setLong(1, endTime)
            stmt.setInt(2, seasonId)
            stmt.executeUpdate()
        }
    }
    
    fun getAllSeasons(): List<SeasonData> {
        val list = mutableListOf<SeasonData>()
        conn().createStatement().use { stmt ->
            val rs = stmt.executeQuery("SELECT * FROM seasons ORDER BY number DESC")
            while (rs.next()) list.add(mapSeason(rs))
        }
        return list
    }
    
    // ===== QUEUE =====
    
    fun addToQueue(entry: QueueEntry) {
        conn().prepareStatement("""
            INSERT INTO challenge_queue (target_uuid, target_type, challenger_uuid, challenger_name, position)
            VALUES (?,?,?,?,?)
        """).use { stmt ->
            stmt.setString(1, entry.targetUuid.toString())
            stmt.setString(2, entry.targetType)
            stmt.setString(3, entry.challengerUuid.toString())
            stmt.setString(4, entry.challengerName)
            stmt.setInt(5, entry.position)
            stmt.executeUpdate()
        }
    }
    
    fun getQueue(targetUuid: UUID): List<QueueEntry> {
        val list = mutableListOf<QueueEntry>()
        conn().prepareStatement(
            "SELECT * FROM challenge_queue WHERE target_uuid=? ORDER BY position ASC"
        ).use { stmt ->
            stmt.setString(1, targetUuid.toString())
            val rs = stmt.executeQuery()
            while (rs.next()) {
                list.add(QueueEntry(
                    UUID.fromString(rs.getString("target_uuid")),
                    rs.getString("target_type"),
                    UUID.fromString(rs.getString("challenger_uuid")),
                    rs.getString("challenger_name"),
                    rs.getLong("queued_at"),
                    rs.getInt("position")
                ))
            }
        }
        return list
    }
    
    fun removeFromQueue(targetUuid: UUID, challengerUuid: UUID) {
        conn().prepareStatement(
            "DELETE FROM challenge_queue WHERE target_uuid=? AND challenger_uuid=?"
        ).use { stmt ->
            stmt.setString(1, targetUuid.toString())
            stmt.setString(2, challengerUuid.toString())
            stmt.executeUpdate()
        }
    }
    
    fun clearQueue(targetUuid: UUID) {
        conn().prepareStatement("DELETE FROM challenge_queue WHERE target_uuid=?").use { stmt ->
            stmt.setString(1, targetUuid.toString())
            stmt.executeUpdate()
        }
    }
    
    fun isInQueue(targetUuid: UUID, challengerUuid: UUID): Boolean {
        conn().prepareStatement(
            "SELECT 1 FROM challenge_queue WHERE target_uuid=? AND challenger_uuid=?"
        ).use { stmt ->
            stmt.setString(1, targetUuid.toString())
            stmt.setString(2, challengerUuid.toString())
            return stmt.executeQuery().next()
        }
    }
    
    // ===== ACTIVE BATTLES =====
    
    fun getActiveBattle(leaderUuid: UUID): ActiveBattle? {
        conn().prepareStatement(
            "SELECT * FROM active_battles WHERE leader_uuid=? AND status='ACTIVE'"
        ).use { stmt ->
            stmt.setString(1, leaderUuid.toString())
            val rs = stmt.executeQuery()
            return if (rs.next()) mapActiveBattle(rs) else null
        }
    }
    
    fun saveActiveBattle(battle: ActiveBattle) {
        conn().prepareStatement("""
            INSERT OR REPLACE INTO active_battles (battle_id, battle_type, leader_uuid, challenger_uuid, turns, status)
            VALUES (?,?,?,?,?,?)
        """).use { stmt ->
            stmt.setString(1, battle.battleId)
            stmt.setString(2, battle.battleType)
            stmt.setString(3, battle.leaderUuid.toString())
            stmt.setString(4, battle.challengerUuid.toString())
            stmt.setInt(5, battle.turns)
            stmt.setString(6, battle.status)
            stmt.executeUpdate()
        }
    }
    
    fun updateBattleTurns(battleId: String, turns: Int) {
        conn().prepareStatement("UPDATE active_battles SET turns=? WHERE battle_id=?").use { stmt ->
            stmt.setInt(1, turns)
            stmt.setString(2, battleId)
            stmt.executeUpdate()
        }
    }
    
    fun endActiveBattle(battleId: String) {
        conn().prepareStatement("UPDATE active_battles SET status='ENDED' WHERE battle_id=?").use { stmt ->
            stmt.setString(1, battleId)
            stmt.executeUpdate()
        }
    }
    
    // ===== MAPPER HELPERS =====
    
    private fun mapGymLeader(rs: ResultSet): GymLeaderData = GymLeaderData(
        rs.getInt("id"),
        UUID.fromString(rs.getString("player_uuid")),
        rs.getString("player_name"),
        PokemonType.valueOf(rs.getString("gym_type")),
        BattleFormat.valueOf(rs.getString("battle_format")),
        rs.getInt("is_open") == 1,
        rs.getString("gym_location")?.let { GymLocation.fromJson(it) },
        rs.getString("extra_banned_pokemon"),
        rs.getInt("extra_ban_season"),
        rs.getInt("multi_team_enabled") == 1,
        rs.getString("team1"),
        rs.getString("team2"),
        rs.getString("team3"),
        rs.getInt("current_team_slot")
    )
    
    private fun mapEliteFour(rs: ResultSet): EliteFourData = EliteFourData(
        rs.getInt("id"),
        UUID.fromString(rs.getString("player_uuid")),
        rs.getString("player_name"),
        PokemonType.valueOf(rs.getString("type1")),
        PokemonType.valueOf(rs.getString("type2")),
        BattleFormat.valueOf(rs.getString("battle_format")),
        rs.getInt("is_open") == 1,
        rs.getString("arena_location")?.let { GymLocation.fromJson(it) },
        rs.getInt("multi_team_enabled") == 1,
        rs.getString("team1"),
        rs.getString("team2"),
        rs.getString("team3"),
        rs.getInt("current_team_slot")
    )
    
    private fun mapChampion(rs: ResultSet): ChampionData = ChampionData(
        rs.getInt("id"),
        UUID.fromString(rs.getString("player_uuid")),
        rs.getString("player_name"),
        BattleFormat.valueOf(rs.getString("battle_format")),
        rs.getInt("is_open") == 1,
        rs.getString("arena_location")?.let { GymLocation.fromJson(it) },
        rs.getInt("multi_team_enabled") == 1,
        rs.getString("team1"),
        rs.getString("team2"),
        rs.getString("team3"),
        rs.getInt("current_team_slot")
    )
    
    private fun mapBattleRecord(rs: ResultSet) = BattleRecord(
        rs.getInt("id"),
        rs.getInt("season_id"),
        rs.getString("battle_type"),
        UUID.fromString(rs.getString("leader_uuid")),
        rs.getString("leader_name"),
        UUID.fromString(rs.getString("challenger_uuid")),
        rs.getString("challenger_name"),
        rs.getString("winner"),
        rs.getString("challenger_team"),
        rs.getString("leader_team"),
        rs.getInt("turns"),
        rs.getLong("battle_time"),
        rs.getString("notes")
    )
    
    private fun mapActiveBattle(rs: ResultSet) = ActiveBattle(
        rs.getString("battle_id"),
        rs.getString("battle_type"),
        UUID.fromString(rs.getString("leader_uuid")),
        UUID.fromString(rs.getString("challenger_uuid")),
        rs.getLong("start_time"),
        rs.getInt("turns"),
        rs.getString("status")
    )
    
    private fun mapSeason(rs: ResultSet) = SeasonData(
        rs.getInt("id"),
        rs.getInt("number"),
        rs.getLong("start_time"),
        rs.getLong("end_time").takeIf { it != 0L },
        rs.getInt("active") == 1
    )
}

// Extension to serialize/deserialize GymLocation as JSON string
fun GymLocation.toJson(): String = """{"world":"$world","x":$x,"y":$y,"z":$z,"yaw":$yaw,"pitch":$pitch}"""

fun GymLocation.Companion.fromJson(json: String): GymLocation? = try {
    val w = Regex(""""world":"([^"]+)"""").find(json)?.groupValues?.get(1) ?: return null
    val x = Regex(""""x":([\d.-]+)""").find(json)?.groupValues?.get(1)?.toDouble() ?: return null
    val y = Regex(""""y":([\d.-]+)""").find(json)?.groupValues?.get(1)?.toDouble() ?: return null
    val z = Regex(""""z":([\d.-]+)""").find(json)?.groupValues?.get(1)?.toDouble() ?: return null
    val yaw = Regex(""""yaw":([\d.-]+)""").find(json)?.groupValues?.get(1)?.toFloat() ?: 0f
    val pitch = Regex(""""pitch":([\d.-]+)""").find(json)?.groupValues?.get(1)?.toFloat() ?: 0f
    GymLocation(w, x, y, z, yaw, pitch)
} catch (e: Exception) { null }
