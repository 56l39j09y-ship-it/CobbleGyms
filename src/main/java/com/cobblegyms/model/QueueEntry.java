package com.cobblegyms.model;

import java.util.UUID;

public class QueueEntry {
    private int id;
    private UUID leaderId;
    private UUID challengerId;
    private String challengerName;
    private long queuedAt;
    private String status;

    public static final String STATUS_WAITING = "waiting";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_DONE = "done";
    public static final String STATUS_CANCELLED = "cancelled";

    public QueueEntry() {}

    public QueueEntry(UUID leaderId, UUID challengerId, String challengerName) {
        this.leaderId = leaderId;
        this.challengerId = challengerId;
        this.challengerName = challengerName;
        this.queuedAt = System.currentTimeMillis();
        this.status = STATUS_WAITING;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public UUID getLeaderId() { return leaderId; }
    public void setLeaderId(UUID leaderId) { this.leaderId = leaderId; }

    public UUID getChallengerId() { return challengerId; }
    public void setChallengerId(UUID challengerId) { this.challengerId = challengerId; }

    public String getChallengerName() { return challengerName; }
    public void setChallengerName(String challengerName) { this.challengerName = challengerName; }

    public long getQueuedAt() { return queuedAt; }
    public void setQueuedAt(long queuedAt) { this.queuedAt = queuedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isWaiting() { return STATUS_WAITING.equals(status); }
    public boolean isActive() { return STATUS_ACTIVE.equals(status); }
}
