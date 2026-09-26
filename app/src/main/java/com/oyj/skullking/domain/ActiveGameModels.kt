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

object GameConstraints {
    const val MinPlayers = 2
    const val MaxPlayers = 8
    const val DefaultTotalRounds = 10
}

enum class GameStatus {
    InProgress,
    Completed,
}

enum class RuleSet {
    Standard,
    NoCardBonus,

    ;

    val allowsRoundBonus: Boolean
        get() = this == Standard
}
