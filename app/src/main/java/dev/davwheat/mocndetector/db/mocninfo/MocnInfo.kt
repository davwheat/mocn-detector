package dev.davwheat.mocndetector.db.mocninfo

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.davwheat.mocndetector.serialization.ZonedDateTimeSerializer
import dev.davwheat.mocndetector.services.RAT
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Entity
@Parcelize
@Serializable
data class MocnInfo(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val checkedAt: ZonedDateTime,
    val hplmn: String?,
    val rplmn: String?,
    val additionalPlmns: Set<String>,
    val isRanSharing: Boolean, // is the cell broadcasting multiple plmns?
    val isMocnDetected: Boolean, // are you on a partner network in a mocn setup?
    val gcis: List<Triple<String, String, RAT>>,
) : Parcelable
