package com.cobblegyms.events

class BattleEventListener {

    fun onBattleStart() {
        // Logic for battle start detection
    }

    fun onTurnCount(turn: Int) {
        // Logic for turn counting
    }

    fun onMoveExecuted(move: String) {
        // Logic for move execution tracking
    }

    fun onBattleEnd() {
        // Logic for battle end detection
    }

    fun recordVictory(defeat: Boolean) {
        // Logic for victory/defeat recording
    }
}