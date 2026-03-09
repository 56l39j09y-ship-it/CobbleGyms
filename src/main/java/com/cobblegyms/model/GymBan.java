package com.cobblegyms.model;

import java.util.UUID;

public class GymBan {
    private int id;
    private UUID bannedUuid;
    private UUID bannerUuid;
    private GymRole bannerType;
    private long expiresAt;
    private String reason;

    public GymBan() {}

    public GymBan(UUID bannedUuid, UUID bannerUuid, GymRole bannerType, long expiresAt, String reason) {
        this.bannedUuid = bannedUuid;
        this.bannerUuid = bannerUuid;
        this.bannerType = bannerType;
        this.expiresAt = expiresAt;
        this.reason = reason;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public UUID getBannedUuid() { return bannedUuid; }
    public void setBannedUuid(UUID bannedUuid) { this.bannedUuid = bannedUuid; }

    public UUID getBannerUuid() { return bannerUuid; }
    public void setBannerUuid(UUID bannerUuid) { this.bannerUuid = bannerUuid; }

    public GymRole getBannerType() { return bannerType; }
    public void setBannerType(GymRole bannerType) { this.bannerType = bannerType; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public long getRemainingMillis() {
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }
}
