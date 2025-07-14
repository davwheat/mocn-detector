package dev.davwheat.mocndetector

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.davwheat.mocndetector.db.AppDatabase


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    fun providesDatabase(
        application: Application
    ): AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "appdatabase.db"
    ).build()

    @Provides
    fun providesMocnInfoDao(
        database: AppDatabase
    ) = database.mocnInfoDao()
}
