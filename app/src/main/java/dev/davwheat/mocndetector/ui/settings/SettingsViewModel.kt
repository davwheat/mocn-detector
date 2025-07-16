package dev.davwheat.mocndetector.ui.settings

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.davwheat.mocndetector.di.UserPreferencesRepository
import dev.davwheat.mocndetector.db.mocninfo.MocnInfoDao
import dev.davwheat.mocndetector.services.TelephonyService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

@SuppressLint("MissingPermission")
@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val mocnInfoDao: MocnInfoDao,
    private val userPreferencesRepository: UserPreferencesRepository,
) :
    AndroidViewModel(application) {

    val isServiceRunning = TelephonyService.isRunning
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _updateInterval =
        userPreferencesRepository
            .watchRefreshInterval()
            .stateIn(viewModelScope, SharingStarted.Eagerly, 10)
    val updateInterval = _updateInterval

    fun exportJsonData(destination: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val json = Json

        try {
            val outputStream =
                application.contentResolver.openOutputStream(destination) ?: return@launch
            val buf = outputStream.bufferedWriter()

            var batch = mocnInfoDao.getAboveId(Long.MIN_VALUE)
            while (batch.isNotEmpty()) {
                batch.forEach {
                    buf.write(json.encodeToString(it) + "\n")
                }

                val lastId = batch.lastOrNull()?.id ?: break
                batch = mocnInfoDao.getAboveId(lastId)
            }
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun deleteAllData() = viewModelScope.launch(Dispatchers.IO) {
        mocnInfoDao.deleteAll()
    }

    fun startService() {
        application.startService(
            Intent(application, TelephonyService::class.java)
        )
    }

    fun stopService() {
        application.stopService(
            Intent(application, TelephonyService::class.java)
        )
    }

    fun setRefreshInterval(seconds: Int) = viewModelScope.launch(Dispatchers.IO) {
        userPreferencesRepository.setRefreshInterval(seconds)
    }
}
