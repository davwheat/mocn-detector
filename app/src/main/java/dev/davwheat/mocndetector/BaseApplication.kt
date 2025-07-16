package dev.davwheat.mocndetector

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.HiltAndroidApp
import dev.davwheat.mocndetector.services.TelephonyService
import timber.log.Timber

@HiltAndroidApp
class BaseApplication : Application() {
    override fun onCreate() {
        Timber.plant(Timber.DebugTree())

        super.onCreate()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startForegroundService(
                Intent(this, TelephonyService::class.java)
            )
        }
    }
}
