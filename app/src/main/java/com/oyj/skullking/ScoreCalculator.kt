package com.oyj.skullking

import kotlin.math.abs

object ScoreCalculator {
    fun calculate(round: Int, bid: Int, tricks: Int): Int =
        if (bid == tricks) {
            if (bid == 0) round * 10 else bid * 20
        } else {
            -10 * abs(bid - tricks)
        }
}
