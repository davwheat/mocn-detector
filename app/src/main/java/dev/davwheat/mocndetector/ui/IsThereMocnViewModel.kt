package dev.davwheat.mocndetector.ui

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.davwheat.mocndetector.db.mocninfo.MocnInfoDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@SuppressLint("MissingPermission")
@HiltViewModel
class IsThereMocnViewModel @Inject constructor(
    application: Application,
    mocnInfoDao: MocnInfoDao,
) :
    AndroidViewModel(application) {

    val mocnHistory = mocnInfoDao.watchAll().distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
