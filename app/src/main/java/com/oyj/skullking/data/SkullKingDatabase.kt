package com.oyj.skullking.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StoredGame::class,
        StoredPlayer::class,
        StoredRoundScore::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class SkullKingDatabase : RoomDatabase() {
    abstract fun activeGameDao(): ActiveGameDao

    companion object {
        fun create(context: Context): SkullKingDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                SkullKingDatabase::class.java,
                "skull-king.db",
            ).build()
    }
}
