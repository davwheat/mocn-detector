package dev.davwheat.mocndetector.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore("user_preferences")

interface UserPreferencesRepository {

    suspend fun setRefreshInterval(seconds: Int): Result<Unit>

    suspend fun getRefreshInterval(): Result<Int>

    fun watchRefreshInterval(): Flow<Int>
}

class MyUserPreferencesRepository @Inject constructor(
    private val userDataStorePreferences: DataStore<Preferences>
) : UserPreferencesRepository {

    override suspend fun setRefreshInterval(seconds: Int) =
        Result.runCatching {
            require(seconds in 5..600) { "Refresh interval must be between 5 and 600 seconds" }

            userDataStorePreferences.edit { preferences ->
                preferences[REFRESH_INTERVAL_SECS] = seconds
            }

            Unit
        }

    override suspend fun getRefreshInterval() =
        Result.runCatching {
            val flow = userDataStorePreferences.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }
                .map { preferences ->
                    preferences[REFRESH_INTERVAL_SECS]
                }

            flow.firstOrNull() ?: 10
        }


    override fun watchRefreshInterval() =
        userDataStorePreferences.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[REFRESH_INTERVAL_SECS] ?: 10
            }
            .distinctUntilChanged()


    private companion object {

        private val REFRESH_INTERVAL_SECS = intPreferencesKey(
            "refresh_interval_seconds"
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {
    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        myUserPreferencesRepository: MyUserPreferencesRepository
    ): UserPreferencesRepository

    companion object {
        @Provides
        @Singleton
        fun provideUserDataStorePreferences(
            @ApplicationContext applicationContext: Context
        ): DataStore<Preferences> {
            return applicationContext.userDataStore
        }
    }
}
