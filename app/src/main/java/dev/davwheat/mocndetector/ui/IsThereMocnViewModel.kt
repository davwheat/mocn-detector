package dev.davwheat.mocndetector.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.telephony.CellIdentity
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellIdentityTdscdma
import android.telephony.CellIdentityWcdma
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.davwheat.mocndetector.db.mocninfo.MocnInfo
import dev.davwheat.mocndetector.db.mocninfo.MocnInfoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime
import javax.inject.Inject

@Serializable
enum class RAT {
    @SerialName("LTE")
    LTE,

    @SerialName("NR")
    NR,

    @SerialName("GSM")
    GSM,

    @SerialName("WCDMA")
    WCDMA,

    @SerialName("TDSCDMA")
    TDSCDMA,

    @SerialName("UNKNOWN")
    UNKNOWN;

    companion object {
        fun fromCellIdentity(cellIdentity: CellIdentity): RAT = when (cellIdentity) {
            is CellIdentityLte -> LTE
            is CellIdentityNr -> NR
            is CellIdentityGsm -> GSM
            is CellIdentityWcdma -> WCDMA
            is CellIdentityTdscdma -> TDSCDMA
            else -> UNKNOWN
        }
    }
}

fun createGciRatTriple(cellIdentity: CellIdentity): Triple<String, String, RAT> {
    val gci = when (cellIdentity) {
        is CellIdentityLte -> "${cellIdentity.ci}-${cellIdentity.pci} / ${cellIdentity.bands.joinToString { "B$it" }}"
        is CellIdentityNr -> "${cellIdentity.nci}-${cellIdentity.pci} / ${cellIdentity.bands.joinToString { "n$it" }}"
        is CellIdentityGsm -> "${cellIdentity.cid}-${cellIdentity.lac}"
        is CellIdentityWcdma -> "${cellIdentity.cid}-${cellIdentity.lac}-${cellIdentity.psc}"
        is CellIdentityTdscdma -> "${cellIdentity.cid}-${cellIdentity.lac}-${cellIdentity.cpid}"
        else -> "Unknown"
    }
    return Triple(
        "${cellIdentity.mccString}-${cellIdentity.mncString}",
        gci,
        RAT.fromCellIdentity(cellIdentity)
    )
}

@SuppressLint("MissingPermission")
@HiltViewModel
class IsThereMocnViewModel @Inject constructor(
    application: Application,
    private val telephonyManager: TelephonyManager,
    private val subscriptionManager: SubscriptionManager,
    private val mocnInfoDao: MocnInfoDao,
) :
    AndroidViewModel(application) {

    val mocnHistory = mocnInfoDao.watchAll().distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                subscriptionManager.getActiveSubscriptionInfoList()?.forEach {
                    getMocnInfo(telephonyManager.createForSubscriptionId(it.subscriptionId))?.let {
                        mocnInfoDao.insert(it)
                    }
                }
                delay(10_000) // Check every 10 seconds
            }
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun getMocnInfo(telephonyManager: TelephonyManager): MocnInfo? {
        val hplmn = telephonyManager.simOperator // format is mccmnc, e.g., "23420"
        if (hplmn.isNullOrEmpty()) return null // no sim or no signal

        val allCellInfo: List<CellInfo>? = telephonyManager.allCellInfo
        if (allCellInfo.isNullOrEmpty()) return null
        val registeredCellInfo = allCellInfo.filter { it.isRegistered }

        // Try and match by PLMN
//        val matchedCellInfo = registeredCellInfo.firstOrNull {
//            it.cellIdentity.mccString == hplmn.substring(0, 3)
//                    && it.cellIdentity.mncString == hplmn.substring(3)
//        }
//
//        if (matchedCellInfo != null) {
//            return createMocnInfoFromCellInfo(matchedCellInfo, hplmn, registeredCellInfo)
//        } else {
            val ci = registeredCellInfo.firstOrNull() ?: return null
            return createMocnInfoFromCellInfo(ci, hplmn, registeredCellInfo)
//        }
    }
}

fun createMocnInfoFromCellInfo(
    cellInfo: CellInfo,
    hplmn: String,
    cells: List<CellInfo>
): MocnInfo? {
    val cellIdentity: CellIdentity = when (cellInfo) {
        is CellInfoLte -> cellInfo.cellIdentity
        is CellInfoGsm -> cellInfo.cellIdentity
        is CellInfoWcdma -> cellInfo.cellIdentity
        is CellInfoNr -> cellInfo.cellIdentity
        else -> null
    } ?: return null

    val rplmn = (cellIdentity.mccString ?: "") + (cellIdentity.mncString ?: "")
    val additionalPlmns = when (
        cellIdentity) {
        is CellIdentityLte ->
            cellIdentity.additionalPlmns

        is CellIdentityNr ->
            cellIdentity.additionalPlmns

        is CellIdentityGsm ->
            cellIdentity.additionalPlmns

        else ->
            emptySet()
    }

    // detection logic from before
    val isRanSharing = additionalPlmns.isNotEmpty()
    val allBroadcastedPlmns = additionalPlmns + rplmn
    val isMocnDetected =
        isRanSharing && allBroadcastedPlmns.contains(hplmn) && rplmn != hplmn

    return MocnInfo(
        checkedAt = ZonedDateTime.now(),
        hplmn = hplmn,
        rplmn = rplmn,
        additionalPlmns = additionalPlmns,
        isRanSharing = isRanSharing,
        isMocnDetected = isMocnDetected,
        gcis = cells.map { cell ->
            createGciRatTriple(cellIdentity)
        }.distinct()
    )
}

val CellIdentity.mccString: String?
    get() = when (this) {
        is CellIdentityLte -> mccString
        is CellIdentityNr -> mccString
        is CellIdentityGsm -> mccString
        is CellIdentityWcdma -> mccString
        is CellIdentityTdscdma -> mccString
        else -> null
    }

val CellIdentity.mncString: String?
    get() = when (this) {
        is CellIdentityLte -> mncString
        is CellIdentityNr -> mncString
        is CellIdentityGsm -> mncString
        is CellIdentityWcdma -> mncString
        is CellIdentityTdscdma -> mncString
        else -> null
    }
