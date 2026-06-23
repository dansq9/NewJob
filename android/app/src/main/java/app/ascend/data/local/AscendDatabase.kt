package app.ascend.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TrackedJobEntity::class], version = 1, exportSchema = false)
abstract class AscendDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao
}
