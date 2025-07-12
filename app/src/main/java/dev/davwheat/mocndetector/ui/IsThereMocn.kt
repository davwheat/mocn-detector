import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.davwheat.mocndetector.db.mocninfo.MocnInfo
import dev.davwheat.mocndetector.ui.IsThereMocnViewModel
import dev.davwheat.mocndetector.ui.RAT
import dev.davwheat.mocndetector.ui.theme.MOCNDetectorTheme
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun IsThereMocn(
    modifier: Modifier = Modifier,
    padding: PaddingValues,
    viewModel: IsThereMocnViewModel = viewModel()
) {
    val history by viewModel.mocnHistory.collectAsStateWithLifecycle()

    val state = rememberLazyListState()
    LaunchedEffect(history) {
        snapshotFlow { state.firstVisibleItemIndex }
            .collect {
                // Scroll to the top if a new item is added.
                // (But only if user is scrolled to the top already.)
                if (it <= 1) {
                    state.scrollToItem(0)
                }
            }
    }

    LazyColumn(modifier, state = state, contentPadding = padding) {
        history.forEach {
            item(key = it.id) {
                Column {
                    MocnItem(modifier = Modifier.fillMaxWidth(), mocnInfo = it)
                    HorizontalDivider(modifier=Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun MocnItem(modifier: Modifier = Modifier, mocnInfo: MocnInfo) {
    Column(
        modifier = modifier
            .then(
                if (mocnInfo.isMocnDetected) Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                else if (mocnInfo.isRanSharing) Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (mocnInfo.isMocnDetected) "MOCN detected" else "No MOCN detected",
                color = if (mocnInfo.isMocnDetected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Medium,
            )
            Text(
                if (mocnInfo.isRanSharing) "RAN sharing" else "No RAN sharing",
                color = if (mocnInfo.isRanSharing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Medium,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "HPLMN: ${mocnInfo.hplmn ?: "Unknown"}",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Text(
                "RPLMN: ${mocnInfo.rplmn ?: "Unknown"}",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            "Additional PLMNs: ${
                if (mocnInfo.additionalPlmns.isEmpty()) "None" else mocnInfo.additionalPlmns.joinToString(
                    ", "
                )
            }",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Column {
            mocnInfo.gcis.forEach {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Outlined.SignalCellularAlt, contentDescription = null, modifier=Modifier.size(20.dp))
                    Text(
                        "PLMN: ${it.first} / CI: ${it.second} / RAT: ${it.third}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Text(
            mocnInfo.checkedAt.toFormattedString(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

fun ZonedDateTime.toFormattedString(): String =
    DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss")
        .format(withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime())

@Composable
@Preview
private fun MocnItemPreview() {
    MOCNDetectorTheme {
        Scaffold { p ->
            Column(Modifier.padding(p)) {
                MocnItem(
                    mocnInfo = MocnInfo(
                        id = -1,
                        checkedAt = ZonedDateTime.now(),
                        hplmn = "23415",
                        rplmn = "23420",
                        additionalPlmns = setOf("23420", "23415"),
                        isRanSharing = true,
                        isMocnDetected = true,
                        gcis = listOf(
                            Triple("234-20", "12345", RAT.NR)
                        ),
                    )
                )
                MocnItem(
                    mocnInfo = MocnInfo(
                        id = -1,
                        checkedAt = ZonedDateTime.now(),
                        hplmn = "23415",
                        rplmn = "23420",
                        additionalPlmns = setOf("23420", "23415"),
                        isRanSharing = true,
                        isMocnDetected = false,
                        gcis = listOf(
                            Triple("234-20", "12345", RAT.NR)
                        ),
                    )
                )
                MocnItem(
                    mocnInfo = MocnInfo(
                        id = -1,
                        checkedAt = ZonedDateTime.now(),
                        hplmn = "23415",
                        rplmn = "23420",
                        additionalPlmns = setOf("23420", "23415"),
                        isRanSharing = false,
                        isMocnDetected = false,
                        gcis = listOf(
                            Triple("234-20", "12345", RAT.NR)
                        ),
                    )
                )
            }
        }
    }
}
