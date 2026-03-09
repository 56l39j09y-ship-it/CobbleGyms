package com.cobblegyms.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GymLeaderData {
    private UUID leaderId;
    private String username;
    private PokemonType type1;
    private PokemonType type2;
    private BattleFormat format;
    private GymRole role;
    private boolean active;
    private int teamSlots;
    private double locationX;
    private double locationY;
    private double locationZ;
    private String world;
    private List<String> teamData;
    private int currentTeamIndex;

    public GymLeaderData() {
        this.teamData = new ArrayList<>();
        this.active = false;
        this.teamSlots = 1;
        this.currentTeamIndex = 0;
    }

    public GymLeaderData(UUID leaderId, String username, PokemonType type1, BattleFormat format, GymRole role) {
        this();
        this.leaderId = leaderId;
        this.username = username;
        this.type1 = type1;
        this.format = format;
        this.role = role;
    }

    public UUID getLeaderId() { return leaderId; }
    public void setLeaderId(UUID leaderId) { this.leaderId = leaderId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public PokemonType getType1() { return type1; }
    public void setType1(PokemonType type1) { this.type1 = type1; }

    public PokemonType getType2() { return type2; }
    public void setType2(PokemonType type2) { this.type2 = type2; }

    public BattleFormat getFormat() { return format; }
    public void setFormat(BattleFormat format) { this.format = format; }

    public GymRole getRole() { return role; }
    public void setRole(GymRole role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getTeamSlots() { return teamSlots; }
    public void setTeamSlots(int teamSlots) { this.teamSlots = Math.max(1, Math.min(3, teamSlots)); }

    public double getLocationX() { return locationX; }
    public void setLocationX(double locationX) { this.locationX = locationX; }

    public double getLocationY() { return locationY; }
    public void setLocationY(double locationY) { this.locationY = locationY; }

    public double getLocationZ() { return locationZ; }
    public void setLocationZ(double locationZ) { this.locationZ = locationZ; }

    public String getWorld() { return world; }
    public void setWorld(String world) { this.world = world; }

    public List<String> getTeamData() { return teamData; }
    public void setTeamData(List<String> teamData) { this.teamData = teamData != null ? teamData : new ArrayList<>(); }

    public int getCurrentTeamIndex() { return currentTeamIndex; }
    public void setCurrentTeamIndex(int currentTeamIndex) { this.currentTeamIndex = currentTeamIndex; }

    public String getTypeDisplay() {
        if (type2 != null) {
            return type1.getColoredName() + "\u00a77/\u00a7r" + type2.getColoredName();
        }
        return type1.getColoredName();
    }

    public boolean hasTeam(int slot) {
        return slot >= 0 && slot < teamData.size() && teamData.get(slot) != null && !teamData.get(slot).isEmpty();
    }

    public String getTeamForSlot(int slot) {
        if (slot >= 0 && slot < teamData.size()) return teamData.get(slot);
        return null;
    }

    public void setTeamForSlot(int slot, String data) {
        while (teamData.size() <= slot) teamData.add(null);
        teamData.set(slot, data);
    }
}
