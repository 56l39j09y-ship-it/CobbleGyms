package com.cobblegyms.model;

public class Season {
    private int id;
    private long startDate;
    private long endDate;
    private boolean active;

    public Season() {}

    public Season(int id, long startDate, long endDate, boolean active) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isExpired() {
        return System.currentTimeMillis() > endDate;
    }

    public long getRemainingMillis() {
        return Math.max(0, endDate - System.currentTimeMillis());
    }
}
