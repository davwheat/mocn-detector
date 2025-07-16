package dev.davwheat.mocndetector.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.davwheat.mocndetector.MainActivity
import dev.davwheat.mocndetector.R
import dev.davwheat.mocndetector.db.mocninfo.MocnInfo
import dev.davwheat.mocndetector.db.mocninfo.MocnInfoDao
import dev.davwheat.mocndetector.di.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber
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

@AndroidEntryPoint
class TelephonyService : Service() {

    companion object {
        val isRunning = MutableStateFlow(false)
    }

    @Inject
    lateinit var telephonyManager: TelephonyManager

    @Inject
    lateinit var subscriptionManager: SubscriptionManager

    @Inject
    lateinit var mocnInfoDao: MocnInfoDao

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val binder = LocalBinder()
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var refreshInterval: StateFlow<Int>

    private var isMonitoring = false

    inner class LocalBinder : Binder() {
        fun getService(): TelephonyService = this@TelephonyService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        isRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("Starting TelephonyService")

        if (isMonitoring) {
            Timber.d("Already monitoring, ignoring start command")
            return START_STICKY
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.e("Permission READ_PHONE_STATE is not granted")
            stopSelf()
            return START_NOT_STICKY
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.e("Permission ACCESS_FINE_LOCATION is not granted")
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(1, createNotification())
        serviceScope.launch {
            isMonitoring = true
            Timber.d("Service started, setting up MOCN monitoring")
            refreshInterval = userPreferencesRepository
                .watchRefreshInterval()
                .stateIn(serviceScope)

            while (true) {
                Timber.d("Checking for MOCN")
                subscriptionManager.getActiveSubscriptionInfoList()?.forEach {
                    createMocnInfo(telephonyManager.createForSubscriptionId(it.subscriptionId))
                }
                Timber.d("Sleeping for ${refreshInterval.value} seconds before next check")
                delay(refreshInterval.value * 1000L)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        isRunning.value = false
        Timber.d("TelephonyService destroyed")
    }

    @SuppressLint("MissingPermission")
    private fun createMocnInfo(telephonyManager: TelephonyManager) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val hplmn = telephonyManager.simOperator // format is mccmnc, e.g., "23420"
        if (hplmn.isNullOrEmpty()) return // no sim or no signal

        val allCellInfo: List<CellInfo>? = telephonyManager.allCellInfo
        if (allCellInfo.isNullOrEmpty()) return
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
        val ci = registeredCellInfo.firstOrNull() ?: return
        val mocnInfo = createMocnInfoFromCellInfo(ci, hplmn, registeredCellInfo) ?: return
        //        }

        serviceScope.launch(Dispatchers.IO) {
            mocnInfoDao.insert(mocnInfo)
        }
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "TELEPHONY_SERVICE_CHANNEL"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            notificationChannelId,
            "Telephony Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }

        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Monitoring for MOCN")
            .setContentText("Recording data in the background")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()
    }
}

private fun createMocnInfoFromCellInfo(
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
