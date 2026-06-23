package app.ascend.di

import android.content.Context
import androidx.room.Room
import app.ascend.data.local.AscendDatabase
import app.ascend.data.local.TrackerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext ctx: Context): AscendDatabase =
        Room.databaseBuilder(ctx, AscendDatabase::class.java, "ascend.db").build()

    @Provides
    fun trackerDao(db: AscendDatabase): TrackerDao = db.trackerDao()
}
