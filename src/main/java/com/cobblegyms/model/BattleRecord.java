package com.cobblegyms.model;

import java.util.UUID;

public class BattleRecord {
    private int id;
    private UUID leaderId;
    private UUID challengerId;
    private String challengerName;
    private String result;
    private String leaderTeamData;
    private String challengerTeamData;
    private int turns;
    private long timestamp;
    private long seasonId;
    private boolean canReplay;

    public BattleRecord() {}

    public BattleRecord(UUID leaderId, UUID challengerId, String challengerName, String result,
                        String leaderTeamData, String challengerTeamData, int turns, long seasonId) {
        this.leaderId = leaderId;
        this.challengerId = challengerId;
        this.challengerName = challengerName;
        this.result = result;
        this.leaderTeamData = leaderTeamData;
        this.challengerTeamData = challengerTeamData;
        this.turns = turns;
        this.timestamp = System.currentTimeMillis();
        this.seasonId = seasonId;
        this.canReplay = turns <= 1;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public UUID getLeaderId() { return leaderId; }
    public void setLeaderId(UUID leaderId) { this.leaderId = leaderId; }

    public UUID getChallengerId() { return challengerId; }
    public void setChallengerId(UUID challengerId) { this.challengerId = challengerId; }

    public String getChallengerName() { return challengerName; }
    public void setChallengerName(String challengerName) { this.challengerName = challengerName; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getLeaderTeamData() { return leaderTeamData; }
    public void setLeaderTeamData(String leaderTeamData) { this.leaderTeamData = leaderTeamData; }

    public String getChallengerTeamData() { return challengerTeamData; }
    public void setChallengerTeamData(String challengerTeamData) { this.challengerTeamData = challengerTeamData; }

    public int getTurns() { return turns; }
    public void setTurns(int turns) { this.turns = turns; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getSeasonId() { return seasonId; }
    public void setSeasonId(long seasonId) { this.seasonId = seasonId; }

    public boolean isCanReplay() { return canReplay; }
    public void setCanReplay(boolean canReplay) { this.canReplay = canReplay; }

    public boolean isWin() { return "win".equalsIgnoreCase(result); }
}
