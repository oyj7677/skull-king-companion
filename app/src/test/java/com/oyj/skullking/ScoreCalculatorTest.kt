package com.oyj.skullking

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreCalculatorTest {
    @Test
    fun `zero bid earns ten points per round`() {
        assertEquals(40, ScoreCalculator.calculate(round = 4, bid = 0, tricks = 0))
    }

    @Test
    fun `matched positive bid earns twenty points per trick`() {
        assertEquals(60, ScoreCalculator.calculate(round = 5, bid = 3, tricks = 3))
    }

    @Test
    fun `missed bid is penalized by difference`() {
        assertEquals(-20, ScoreCalculator.calculate(round = 6, bid = 3, tricks = 1))
    }

    @Test
    fun `missed zero bid loses ten points for every card dealt`() {
        assertEquals(-90, ScoreCalculator.calculate(round = 9, bid = 0, tricks = 2))
    }

    @Test
    fun `matched bid adds the entered round bonus`() {
        assertEquals(
            RoundScore(baseScore = 40, bonusScore = 30, totalScore = 70),
            ScoreCalculator.calculate(RoundScoreInput(round = 3, bid = 2, tricks = 2, roundBonus = 30))
        )
    }
}
