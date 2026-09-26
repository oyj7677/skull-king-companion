package com.oyj.skullking.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.oyj.skullking.domain.GameStatus
import com.oyj.skullking.domain.RuleSet

@Entity(tableName = "active_games")
data class StoredGame(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val totalRounds: Int,
    val ruleSet: RuleSet,
    val status: GameStatus,
)

@Entity(
    tableName = "players",
    foreignKeys = [
        ForeignKey(
            entity = StoredGame::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("gameId")],
)
data class StoredPlayer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val name: String,
    val position: Int,
)

@Entity(
    tableName = "round_scores",
    primaryKeys = ["gameId", "roundNumber", "playerId"],
    foreignKeys = [
        ForeignKey(
            entity = StoredGame::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StoredPlayer::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("gameId"), Index("playerId")],
)
data class StoredRoundScore(
    val gameId: Long,
    val roundNumber: Int,
    val playerId: Long,
    val bid: Int,
    val tricks: Int,
    val baseScore: Int,
    val bonusScore: Int,
    val totalScore: Int,
)

data class StoredActiveGame(
    val game: StoredGame,
    val players: List<StoredPlayer>,
    val scores: List<StoredRoundScore>,
)
