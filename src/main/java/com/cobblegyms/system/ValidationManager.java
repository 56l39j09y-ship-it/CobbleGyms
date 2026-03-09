package com.cobblegyms.system;

import com.cobblegyms.model.GymLeaderData;
import com.cobblegyms.util.MessageUtil;
import com.cobblegyms.validation.SmogonValidator;
import com.cobblegyms.validation.SmogonValidator.ValidationViolation;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;

public class ValidationManager {
    private static ValidationManager instance;
    private final SmogonValidator validator = new SmogonValidator();

    private ValidationManager() {}

    public static ValidationManager getInstance() {
        if (instance == null) instance = new ValidationManager();
        return instance;
    }

    public List<ValidationViolation> getViolations(ServerPlayerEntity player, UUID targetLeaderId) {
        return validator.getViolations(player, targetLeaderId);
    }

    public boolean isTeamValid(ServerPlayerEntity player, UUID targetLeaderId) {
        return validator.isTeamValid(player, targetLeaderId);
    }

    public void validateTeam(ServerPlayerEntity player, UUID targetLeaderId) {
        List<ValidationViolation> violations = getViolations(player, targetLeaderId);
        if (violations.isEmpty()) {
            MessageUtil.sendSuccess(player, "Your team is valid and ready to battle!");
        } else {
            MessageUtil.sendError(player, "Your team has " + violations.size() + " violation(s):");
            for (ValidationViolation v : violations) {
                MessageUtil.sendError(player, "  \u00a7c\u25cf\u00a7r " + v.description());
            }
        }
    }

    public void validateTeamForLeader(ServerPlayerEntity player, GymLeaderData leader) {
        validateTeam(player, leader != null ? leader.getLeaderId() : null);
    }
}
