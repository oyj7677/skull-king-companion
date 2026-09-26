package com.oyj.skullking.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.oyj.skullking.domain.GameStatus

@Dao
interface ActiveGameDao {
    @Query("SELECT * FROM active_games LIMIT 1")
    suspend fun getActiveGame(): StoredGame?

    @Query("SELECT * FROM players WHERE gameId = :gameId ORDER BY position ASC")
    suspend fun getPlayers(gameId: Long): List<StoredPlayer>

    @Query("SELECT * FROM round_scores WHERE gameId = :gameId ORDER BY roundNumber ASC")
    suspend fun getRoundScores(gameId: Long): List<StoredRoundScore>

    @Insert
    suspend fun insertGame(game: StoredGame): Long

    @Insert
    suspend fun insertPlayers(players: List<StoredPlayer>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoundScores(scores: List<StoredRoundScore>)

    @Query("DELETE FROM active_games")
    suspend fun deleteActiveGames()

    @Query("DELETE FROM players WHERE gameId = :gameId")
    suspend fun deletePlayers(gameId: Long)

    @Query("DELETE FROM round_scores WHERE gameId = :gameId AND roundNumber = :roundNumber")
    suspend fun deleteRoundScores(gameId: Long, roundNumber: Int)

    @Query("UPDATE active_games SET status = :status WHERE id = :gameId")
    suspend fun updateGameStatus(gameId: Long, status: GameStatus)
}
