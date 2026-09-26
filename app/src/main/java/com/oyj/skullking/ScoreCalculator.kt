package com.oyj.skullking

import kotlin.math.abs

data class RoundScoreInput(
    val round: Int,
    val bid: Int,
    val tricks: Int,
    val roundBonus: Int = 0
)

data class RoundScore(
    val baseScore: Int,
    val bonusScore: Int,
    val totalScore: Int
)

object ScoreCalculator {
    fun calculate(input: RoundScoreInput): RoundScore {
        val baseScore = calculate(input.round, input.bid, input.tricks)
        val bonusScore = if (input.bid == input.tricks) input.roundBonus else 0
        return RoundScore(baseScore, bonusScore, baseScore + bonusScore)
    }

    fun calculate(round: Int, bid: Int, tricks: Int): Int =
        if (bid == tricks) {
            if (bid == 0) round * 10 else bid * 20
        } else if (bid == 0) {
            -10 * round
        } else {
            -10 * abs(bid - tricks)
        }
}
