package dev.davwheat.mocndetector.db.mocninfo

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.davwheat.mocndetector.ui.RAT
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@Entity
@Parcelize
data class MocnInfo(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val checkedAt: ZonedDateTime,
    val hplmn: String?,
    val rplmn: String?,
    val additionalPlmns: Set<String>,
    val isRanSharing: Boolean, // is the cell broadcasting multiple plmns?
    val isMocnDetected: Boolean, // are you on a partner network in a mocn setup?
    val gcis: List<Triple<String, String, RAT>>,
): Parcelable
