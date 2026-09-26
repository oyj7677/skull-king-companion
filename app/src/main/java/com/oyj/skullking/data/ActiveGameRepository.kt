package com.oyj.skullking.data

import com.oyj.skullking.ScoreCalculator
import com.oyj.skullking.domain.ActiveGame
import com.oyj.skullking.domain.GameStatus
import com.oyj.skullking.domain.GameConstraints
import com.oyj.skullking.domain.Player
import com.oyj.skullking.domain.PlayerRoundScore
import com.oyj.skullking.domain.Round
import com.oyj.skullking.domain.RoundPlayerInput
import com.oyj.skullking.domain.RuleSet

interface ActiveGameRepository {
    suspend fun getActiveGame(): ActiveGame?
    suspend fun startNewGame(
        playerNames: List<String>,
        totalRounds: Int = GameConstraints.DefaultTotalRounds,
        ruleSet: RuleSet = RuleSet.Standard,
    ): ActiveGame

    suspend fun updatePlayers(gameId: Long, playerNames: List<String>): ActiveGame
    suspend fun saveRound(roundNumber: Int, entries: List<RoundPlayerInput>): ActiveGame
    suspend fun clearActiveGame()
}

class DefaultActiveGameRepository(
    private val store: ActiveGameStore,
) : ActiveGameRepository {
    override suspend fun getActiveGame(): ActiveGame? =
        store.getActiveGame()?.toDomain()

    override suspend fun startNewGame(
        playerNames: List<String>,
        totalRounds: Int,
        ruleSet: RuleSet,
    ): ActiveGame {
        val names = validatePlayerNames(playerNames)
        require(totalRounds > 0) { "Total rounds must be positive" }

        return store.replaceActiveGame(
            playerNames = names,
            totalRounds = totalRounds,
            ruleSet = ruleSet,
        ).toDomain()
    }

    override suspend fun updatePlayers(gameId: Long, playerNames: List<String>): ActiveGame {
        val current = requireNotNull(store.getActiveGame()) { "No active game" }
        require(current.game.id == gameId) { "Game $gameId is not active" }
        check(current.scores.isEmpty()) { "Player roster is locked after the first saved round" }

        return store.replacePlayers(gameId, validatePlayerNames(playerNames)).toDomain()
    }

    override suspend fun saveRound(roundNumber: Int, entries: List<RoundPlayerInput>): ActiveGame {
        val current = requireNotNull(store.getActiveGame()) { "No active game" }
        val game = current.game
        val players = current.players

        require(roundNumber in 1..game.totalRounds) {
            "Round number must be between 1 and ${game.totalRounds}"
        }
        require(entries.size == players.size) { "Round must include one score for every player" }

        val playerIds = players.map { it.id }.toSet()
        val inputIds = entries.map { it.playerId }
        require(inputIds.toSet() == playerIds && inputIds.size == playerIds.size) {
            "Round must include exactly the active game's players"
        }

        val roundScores = entries.map { input ->
            require(input.bid >= 0) { "Bid must be zero or greater" }
            require(input.tricks >= 0) { "Tricks must be zero or greater" }
            require(input.bid <= roundNumber) { "Bid cannot exceed the round's trick limit" }
            require(input.tricks <= roundNumber) { "Tricks cannot exceed the round's trick limit" }
            require(input.roundBonus >= 0) { "Round bonus must be zero or greater" }

            val baseScore = ScoreCalculator.calculate(
                round = roundNumber,
                bid = input.bid,
                tricks = input.tricks,
            )
            val bonusScore = if (game.ruleSet.allowsRoundBonus && input.bid == input.tricks) input.roundBonus else 0
            StoredRoundScore(
                gameId = game.id,
                roundNumber = roundNumber,
                playerId = input.playerId,
                bid = input.bid,
                tricks = input.tricks,
                baseScore = baseScore,
                bonusScore = bonusScore,
                totalScore = baseScore + bonusScore,
            )
        }

        val saved = store.upsertRoundScores(
            gameId = game.id,
            roundNumber = roundNumber,
            roundScores = roundScores,
        )
        val status = if (saved.completedRoundNumbers().size == game.totalRounds) {
            GameStatus.Completed
        } else {
            GameStatus.InProgress
        }

        return if (saved.game.status == status) {
            saved.toDomain()
        } else {
            store.updateGameStatus(game.id, status).toDomain()
        }
    }

    override suspend fun clearActiveGame() {
        store.clearActiveGame()
    }

    private fun validatePlayerNames(playerNames: List<String>): List<String> {
        require(playerNames.size in GameConstraints.MinPlayers..GameConstraints.MaxPlayers) {
            "Active game requires ${GameConstraints.MinPlayers} to ${GameConstraints.MaxPlayers} players"
        }
        return playerNames.map { name ->
            name.trim().also { require(it.isNotEmpty()) { "Player name must not be blank" } }
        }
    }

    private fun StoredActiveGame.completedRoundNumbers(): Set<Int> =
        scores.groupBy { it.roundNumber }
            .filterValues { roundScores -> roundScores.size == players.size }
            .keys

    private fun StoredActiveGame.toDomain(): ActiveGame {
        val orderedPlayers = players.sortedBy { it.position }
        val scoresByRound = scores.groupBy { it.roundNumber }.toSortedMap()
        val runningTotals = orderedPlayers.associate { it.id to 0 }.toMutableMap()
        val rounds = scoresByRound.map { (roundNumber, storedScores) ->
            val scoresByPlayer = storedScores.associateBy { it.playerId }
            Round(
                number = roundNumber,
                scores = orderedPlayers.mapNotNull { player ->
                    scoresByPlayer[player.id]?.let { score ->
                        val cumulative = runningTotals.getValue(player.id) + score.totalScore
                        runningTotals[player.id] = cumulative
                        PlayerRoundScore(
                            playerId = player.id,
                            bid = score.bid,
                            tricks = score.tricks,
                            baseScore = score.baseScore,
                            bonusScore = score.bonusScore,
                            totalScore = score.totalScore,
                            cumulativeScore = cumulative,
                        )
                    }
                },
            )
        }

        return ActiveGame(
            id = game.id,
            totalRounds = game.totalRounds,
            ruleSet = game.ruleSet,
            status = game.status,
            players = orderedPlayers.map { player ->
                Player(
                    id = player.id,
                    name = player.name,
                    position = player.position,
                    totalScore = runningTotals.getValue(player.id),
                )
            },
            rounds = rounds,
        )
    }
}
