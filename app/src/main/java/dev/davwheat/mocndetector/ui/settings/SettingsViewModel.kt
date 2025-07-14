package dev.davwheat.mocndetector.ui.settings

import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.davwheat.mocndetector.db.mocninfo.MocnInfoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

@SuppressLint("MissingPermission")
@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val mocnInfoDao: MocnInfoDao,
) :
    AndroidViewModel(application) {

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
}
