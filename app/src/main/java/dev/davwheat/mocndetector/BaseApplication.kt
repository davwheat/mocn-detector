package dev.davwheat.mocndetector

import android.app.Application
import android.content.Intent
import dagger.hilt.android.HiltAndroidApp
import dev.davwheat.mocndetector.services.TelephonyService
import timber.log.Timber

@HiltAndroidApp
class BaseApplication : Application() {
    override fun onCreate() {
        Timber.plant(Timber.DebugTree())

        super.onCreate()

        startService(
            Intent(this, TelephonyService::class.java)
        )
    }
}
