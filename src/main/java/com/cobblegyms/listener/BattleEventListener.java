package com.cobblegyms.listener;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblegyms.system.BattleManager;
import com.cobblegyms.system.GymManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class BattleEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleGyms");

    public static void register() {
        CobblemonEvents.BATTLE_VICTORY.subscribe(event -> {
            handleBattleEnd(event.getBattle(), true);
        });

        CobblemonEvents.BATTLE_FLED.subscribe(event -> {
            handleBattleEnd(event.getBattle(), false);
        });
    }

    private static void handleBattleEnd(PokemonBattle battle, boolean normalEnd) {
        try {
            var actors = battle.getActors().iterator();
            if (!actors.hasNext()) return;
            BattleActor actor1 = actors.next();
            if (!actors.hasNext()) return;
            BattleActor actor2 = actors.next();

            if (!(actor1 instanceof PlayerBattleActor player1Actor) ||
                !(actor2 instanceof PlayerBattleActor player2Actor)) {
                return;
            }

            UUID player1 = player1Actor.getPlayerUUID();
            UUID player2 = player2Actor.getPlayerUUID();

            UUID leaderId = null;
            UUID challengerId = null;

            // Determine which participant is the gym leader
            if (GymManager.getInstance().isGymLeader(player1)) {
                leaderId = player1;
                challengerId = player2;
            } else if (GymManager.getInstance().isGymLeader(player2)) {
                leaderId = player2;
                challengerId = player1;
            }

            if (leaderId == null) return;

            if (!BattleManager.getInstance().isBattleActive(leaderId)) return;

            int turns = BattleManager.getInstance().getTurns(leaderId, challengerId);

            // Determine winner from battle outcome
            String result;
            boolean leaderWon = player1Actor.isWinner() && leaderId.equals(player1);
            if (!leaderWon && !player1Actor.isWinner()) {
                leaderWon = leaderId.equals(player2) && player2Actor.isWinner();
            }
            result = leaderWon ? "win" : "loss";

            String leaderTeam = "";
            String challengerTeam = "";

            final UUID finalLeaderId = leaderId;
            final UUID finalChallengerId = challengerId;
            final String finalResult = result;
            final int finalTurns = turns;

            BattleManager.getInstance().onBattleEnd(
                    finalLeaderId, finalChallengerId, finalResult,
                    finalTurns, leaderTeam, challengerTeam
            );
        } catch (Exception e) {
            LOGGER.error("[CobbleGyms] Error handling battle end: {}", e.getMessage(), e);
        }
    }
}
