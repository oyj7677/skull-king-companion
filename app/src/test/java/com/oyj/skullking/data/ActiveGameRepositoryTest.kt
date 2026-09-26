package com.oyj.skullking.data

import com.oyj.skullking.domain.GameStatus
import com.oyj.skullking.domain.RoundPlayerInput
import com.oyj.skullking.domain.RuleSet
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class ActiveGameRepositoryTest {
    private val store = InMemoryActiveGameStore()
    private val repository = DefaultActiveGameRepository(store)

    @Test
    fun `start new game validates player count and replaces the previous active game`() {
        runBlocking {
            assertFails<IllegalArgumentException> {
                repository.startNewGame(playerNames = listOf("A"))
            }
            assertFails<IllegalArgumentException> {
                repository.startNewGame(playerNames = List(9) { "P$it" })
            }

            val first = repository.startNewGame(playerNames = listOf("Anne", "Ben"))
            val second = repository.startNewGame(playerNames = listOf("Cara", "Dan", "Eli"))

            assertNotEquals(first.id, second.id)
            assertEquals(listOf("Cara", "Dan", "Eli"), repository.getActiveGame()?.players?.map { it.name })
            assertEquals(10, second.totalRounds)
            assertEquals(RuleSet.Standard, second.ruleSet)
        }
    }

    @Test
    fun `player roster can change only before the first saved round`() {
        runBlocking {
            val game = repository.startNewGame(playerNames = listOf("Anne", "Ben"))

            val editedBeforeStart = repository.updatePlayers(game.id, listOf("Anne", "Ben", "Cara"))
            assertEquals(listOf("Anne", "Ben", "Cara"), editedBeforeStart.players.map { it.name })

            repository.saveRound(
                roundNumber = 1,
                entries = editedBeforeStart.players.map { player ->
                    RoundPlayerInput(playerId = player.id, bid = 0, tricks = 0, roundBonus = 0)
                },
            )

            assertFails<IllegalStateException> {
                repository.updatePlayers(game.id, listOf("Anne", "Ben"))
            }
        }
    }

    @Test
    fun `saving a round requires one entry per player and completes the game after the last round`() {
        runBlocking {
            val game = repository.startNewGame(playerNames = listOf("Anne", "Ben"), totalRounds = 2)

            assertFails<IllegalArgumentException> {
                repository.saveRound(
                    roundNumber = 1,
                    entries = listOf(RoundPlayerInput(game.players[0].id, bid = 0, tricks = 0, roundBonus = 0)),
                )
            }

            repository.saveRound(
                roundNumber = 1,
                entries = listOf(
                    RoundPlayerInput(game.players[0].id, bid = 0, tricks = 0, roundBonus = 0),
                    RoundPlayerInput(game.players[1].id, bid = 1, tricks = 0, roundBonus = 50),
                ),
            )
            val completed = repository.saveRound(
                roundNumber = 2,
                entries = listOf(
                    RoundPlayerInput(game.players[0].id, bid = 1, tricks = 1, roundBonus = 20),
                    RoundPlayerInput(game.players[1].id, bid = 0, tricks = 1, roundBonus = 40),
                ),
            )

            assertEquals(GameStatus.Completed, completed.status)
            assertEquals(50, completed.players.first { it.name == "Anne" }.totalScore)
            assertEquals(-30, completed.players.first { it.name == "Ben" }.totalScore)
            assertEquals(0, completed.rounds[0].scores.first { it.playerId == game.players[1].id }.bonusScore)
            assertEquals(0, completed.rounds[1].scores.first { it.playerId == game.players[1].id }.bonusScore)
        }
    }

    @Test
    fun `editing a saved round recalculates later cumulative totals and keeps completed result`() {
        runBlocking {
            val game = repository.startNewGame(playerNames = listOf("Anne", "Ben"), totalRounds = 2)
            repository.saveRound(
                roundNumber = 1,
                entries = listOf(
                    RoundPlayerInput(game.players[0].id, bid = 0, tricks = 0, roundBonus = 0),
                    RoundPlayerInput(game.players[1].id, bid = 0, tricks = 0, roundBonus = 0),
                ),
            )
            repository.saveRound(
                roundNumber = 2,
                entries = listOf(
                    RoundPlayerInput(game.players[0].id, bid = 1, tricks = 1, roundBonus = 0),
                    RoundPlayerInput(game.players[1].id, bid = 1, tricks = 0, roundBonus = 0),
                ),
            )

            val edited = repository.saveRound(
                roundNumber = 1,
                entries = listOf(
                    RoundPlayerInput(game.players[0].id, bid = 0, tricks = 1, roundBonus = 0),
                    RoundPlayerInput(game.players[1].id, bid = 0, tricks = 0, roundBonus = 0),
                ),
            )

            assertEquals(GameStatus.Completed, edited.status)
            assertEquals(10, edited.players.first { it.name == "Anne" }.totalScore)
            assertEquals(0, edited.players.first { it.name == "Ben" }.totalScore)
            assertEquals(10, edited.rounds[1].scores.first { it.playerId == game.players[0].id }.cumulativeScore)
        }
    }

    @Test
    fun `completed game is restored until the user explicitly starts a replacement`() {
        runBlocking {
            val game = repository.startNewGame(playerNames = listOf("Anne", "Ben"), totalRounds = 1)
            repository.saveRound(
                roundNumber = 1,
                entries = listOf(
                    RoundPlayerInput(game.players[0].id, bid = 0, tricks = 0, roundBonus = 0),
                    RoundPlayerInput(game.players[1].id, bid = 1, tricks = 1, roundBonus = 0),
                ),
            )

            val restoredRepository = DefaultActiveGameRepository(store)
            assertEquals(GameStatus.Completed, restoredRepository.getActiveGame()?.status)

            restoredRepository.startNewGame(playerNames = listOf("Cara", "Dan"))
            assertEquals(listOf("Cara", "Dan"), repository.getActiveGame()?.players?.map { it.name })
        }
    }

    @Test
    fun `clearing the active game leaves no active game to restore`() {
        runBlocking {
            repository.startNewGame(playerNames = listOf("Anne", "Ben"))

            repository.clearActiveGame()

            assertNull(repository.getActiveGame())
        }
    }
}

private class InMemoryActiveGameStore : ActiveGameStore {
    private var nextGameId = 1L
    private var nextPlayerId = 1L
    private var activeGame: StoredGame? = null
    private var players = emptyList<StoredPlayer>()
    private var scores = emptyList<StoredRoundScore>()

    override suspend fun getActiveGame(): StoredActiveGame? =
        activeGame?.let { game ->
            StoredActiveGame(
                game = game,
                players = players,
                scores = scores,
            )
        }

    override suspend fun replaceActiveGame(playerNames: List<String>, totalRounds: Int, ruleSet: RuleSet): StoredActiveGame {
        val game = StoredGame(
            id = nextGameId++,
            totalRounds = totalRounds,
            ruleSet = ruleSet,
            status = GameStatus.InProgress,
        )
        activeGame = game
        players = playerNames.mapIndexed { index, name ->
            StoredPlayer(
                id = nextPlayerId++,
                gameId = game.id,
                name = name,
                position = index,
            )
        }
        scores = emptyList()
        return getActiveGame() ?: error("active game was not created")
    }

    override suspend fun replacePlayers(gameId: Long, playerNames: List<String>): StoredActiveGame {
        val game = requireActiveGame(gameId)
        players = playerNames.mapIndexed { index, name ->
            StoredPlayer(
                id = nextPlayerId++,
                gameId = game.id,
                name = name,
                position = index,
            )
        }
        scores = emptyList()
        return getActiveGame() ?: error("active game was not updated")
    }

    override suspend fun upsertRoundScores(gameId: Long, roundNumber: Int, roundScores: List<StoredRoundScore>): StoredActiveGame {
        requireActiveGame(gameId)
        scores = scores
            .filterNot { it.gameId == gameId && it.roundNumber == roundNumber } + roundScores
        return getActiveGame() ?: error("round was not saved")
    }

    override suspend fun updateGameStatus(gameId: Long, status: GameStatus): StoredActiveGame {
        val game = requireActiveGame(gameId)
        activeGame = game.copy(status = status)
        return getActiveGame() ?: error("active game was not updated")
    }

    override suspend fun clearActiveGame() {
        activeGame = null
        players = emptyList()
        scores = emptyList()
    }

    private fun requireActiveGame(gameId: Long): StoredGame =
        requireNotNull(activeGame) { "No active game" }
            .also { require(it.id == gameId) { "Game $gameId is not active" } }
}

private suspend inline fun <reified T : Throwable> assertFails(noinline block: suspend () -> Unit): T {
    try {
        block()
    } catch (throwable: Throwable) {
        if (throwable is T) return throwable
        throw AssertionError("Expected ${T::class.java.simpleName}, but was ${throwable::class.java.simpleName}", throwable)
    }
    fail("Expected ${T::class.java.simpleName}")
    error("unreachable")
}
