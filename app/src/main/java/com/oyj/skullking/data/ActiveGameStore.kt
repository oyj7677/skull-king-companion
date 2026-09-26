package com.oyj.skullking.data

import androidx.room.withTransaction
import com.oyj.skullking.domain.GameStatus
import com.oyj.skullking.domain.RuleSet

interface ActiveGameStore {
    suspend fun getActiveGame(): StoredActiveGame?
    suspend fun replaceActiveGame(
        playerNames: List<String>,
        totalRounds: Int,
        ruleSet: RuleSet,
    ): StoredActiveGame

    suspend fun replacePlayers(gameId: Long, playerNames: List<String>): StoredActiveGame
    suspend fun upsertRoundScores(
        gameId: Long,
        roundNumber: Int,
        roundScores: List<StoredRoundScore>,
    ): StoredActiveGame

    suspend fun updateGameStatus(gameId: Long, status: GameStatus): StoredActiveGame
    suspend fun clearActiveGame()
}

class RoomActiveGameStore(
    private val database: SkullKingDatabase,
) : ActiveGameStore {
    private val dao = database.activeGameDao()

    override suspend fun getActiveGame(): StoredActiveGame? =
        database.withTransaction {
            loadActiveGame()
        }

    override suspend fun replaceActiveGame(
        playerNames: List<String>,
        totalRounds: Int,
        ruleSet: RuleSet,
    ): StoredActiveGame =
        database.withTransaction {
            dao.deleteActiveGames()
            val gameId = dao.insertGame(
                StoredGame(
                    totalRounds = totalRounds,
                    ruleSet = ruleSet,
                    status = GameStatus.InProgress,
                ),
            )
            val players = playerNames.mapIndexed { index, name ->
                StoredPlayer(gameId = gameId, name = name, position = index)
            }
            dao.insertPlayers(players)
            requireNotNull(loadActiveGame()) { "Active game was not created" }
        }

    override suspend fun replacePlayers(gameId: Long, playerNames: List<String>): StoredActiveGame =
        database.withTransaction {
            dao.deletePlayers(gameId)
            val players = playerNames.mapIndexed { index, name ->
                StoredPlayer(gameId = gameId, name = name, position = index)
            }
            dao.insertPlayers(players)
            requireNotNull(loadActiveGame()) { "Active game was not updated" }
        }

    override suspend fun upsertRoundScores(
        gameId: Long,
        roundNumber: Int,
        roundScores: List<StoredRoundScore>,
    ): StoredActiveGame =
        database.withTransaction {
            dao.deleteRoundScores(gameId, roundNumber)
            dao.insertRoundScores(roundScores)
            requireNotNull(loadActiveGame()) { "Round was not saved" }
        }

    override suspend fun updateGameStatus(gameId: Long, status: GameStatus): StoredActiveGame =
        database.withTransaction {
            dao.updateGameStatus(gameId, status)
            requireNotNull(loadActiveGame()) { "Active game was not updated" }
        }

    override suspend fun clearActiveGame() {
        database.withTransaction {
            dao.deleteActiveGames()
        }
    }

    private suspend fun loadActiveGame(): StoredActiveGame? {
        val game = dao.getActiveGame() ?: return null
        return StoredActiveGame(
            game = game,
            players = dao.getPlayers(game.id),
            scores = dao.getRoundScores(game.id),
        )
    }
}
