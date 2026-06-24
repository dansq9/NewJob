package app.ascend.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TrackedJobEntity::class, ResumeEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AscendDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao
    abstract fun resumeDao(): ResumeDao
}
