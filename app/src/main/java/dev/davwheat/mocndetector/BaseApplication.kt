package dev.davwheat.mocndetector

import android.app.Application
import android.content.Intent
import dagger.hilt.android.HiltAndroidApp
import dev.davwheat.mocndetector.services.TelephonyService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

@HiltAndroidApp
class BaseApplication : Application() {
    override fun onCreate() {
        Timber.plant(Timber.DebugTree())

        super.onCreate()

        startForegroundService(
            Intent(this, TelephonyService::class.java)
        )
    }
}
