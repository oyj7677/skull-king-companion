package com.oyj.skullking.domain

data class ActiveGame(
    val id: Long,
    val totalRounds: Int,
    val ruleSet: RuleSet,
    val status: GameStatus,
    val players: List<Player>,
    val rounds: List<Round>,
)

data class Player(
    val id: Long,
    val name: String,
    val position: Int,
    val totalScore: Int,
)

data class Round(
    val number: Int,
    val scores: List<PlayerRoundScore>,
)

data class PlayerRoundScore(
    val playerId: Long,
    val bid: Int,
    val tricks: Int,
    val baseScore: Int,
    val bonusScore: Int,
    val totalScore: Int,
    val cumulativeScore: Int,
)

data class RoundPlayerInput(
    val playerId: Long,
    val bid: Int,
    val tricks: Int,
    val roundBonus: Int,
)

enum class GameStatus {
    InProgress,
    Completed,
}

enum class RuleSet {
    Standard,
}
