package dev.davwheat.mocndetector.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Output
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.davwheat.mocndetector.LocalSnackbarHostState
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun SettingsSheet(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val scope = rememberCoroutineScope()
    val snackbarState = LocalSnackbarHostState.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.data?.data?.let { destinationUri ->
                viewModel.exportJsonData(destinationUri)
                snackbarState?.let { s ->
                    scope.launch(NonCancellable) {
                        s.showSnackbar("Data written successfully")
                    }
                }
                onDismissRequest()
            }
        }

    Column(
        modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, top = 16.dp)
    ) {
        Surface(
            onClick = {
                launcher.launch(
                    Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/jsonl"
                        putExtra(Intent.EXTRA_TITLE, "mocn_dump.jsonl")
                    }
                )
            },
        ) {
            Row(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Output, contentDescription = null)
                Text("Export data to JSONL")
            }

        }

        var showConfirmDelete by rememberSaveable { mutableStateOf(false) }
        if (showConfirmDelete) {
            AlertDialog(
                onDismissRequest = { showConfirmDelete = false },
                title = {
                    Text("Delete all data")
                },
                text = {
                    Text("Are you sure you want to delete all data? This cannot be undone.")
                },
                dismissButton = {
                    TextButton(onClick = {
                        showConfirmDelete = false
                    }) {
                        Text("Cancel")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteAllData()
                            showConfirmDelete = false
                            snackbarState?.let { s ->
                                scope.launch(NonCancellable) {
                                    s.showSnackbar("All data deleted successfully")
                                }
                            }
                            onDismissRequest()
                        },
                        colors = ButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.errorContainer.copy(
                                alpha = 0.5f
                            ),
                            disabledContentColor = MaterialTheme.colorScheme.onErrorContainer.copy(
                                alpha = 0.5f
                            ),
                        )
                    ) {
                        Text("Delete")
                    }
                }
            )
        }
        Surface(onClick = { showConfirmDelete = true }) {
            Row(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Text("Delete all data")
            }
        }

        val updateInterval by viewModel.updateInterval.collectAsStateWithLifecycle()
        var showUpdateIntervalDialog by rememberSaveable { mutableStateOf(false) }
        if (showUpdateIntervalDialog) {
            val updateIntervalTextFieldState = rememberTextFieldState(
                updateInterval.toString()
            )
            val error by remember(updateIntervalTextFieldState) {
                snapshotFlow { updateIntervalTextFieldState.text }
                    .map {
                        it.toString().toIntOrNull()?.let { value ->
                            value < 5 || value > 600
                        } ?: true
                    }
            }
                .collectAsStateWithLifecycle(false)
            AlertDialog(
                onDismissRequest = { showUpdateIntervalDialog = false },
                title = {
                    Text("Change update interval")
                },
                text = {
                    TextField(
                        state = updateIntervalTextFieldState,
                        label = { Text("Interval (seconds)") },
                        supportingText = {
                            Text("Between 5 and 600 seconds")
                        },
                        inputTransformation = DigitOnlyInputTransformation(600),
                        isError = error,
                    )
                },
                dismissButton = {
                    TextButton(onClick = {
                        showUpdateIntervalDialog = false
                    }) {
                        Text("Cancel")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val interval =
                                updateIntervalTextFieldState.text.toString().toIntOrNull()

                            interval?.let { viewModel.setRefreshInterval(it) }
                            showUpdateIntervalDialog = false
                            snackbarState?.let { s ->
                                scope.launch(NonCancellable) {
                                    s.showSnackbar("Update interval changed to ${interval}s")
                                }
                            }
                            onDismissRequest()
                        },
                        enabled = !error,
                    ) {
                        Text("Save")
                    }
                }
            )
        }
        Surface(onClick = { showUpdateIntervalDialog = true }) {
            Row(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Timer, contentDescription = null)
                Text("Change update interval (${updateInterval}s)")
            }
        }
    }
}


class DigitOnlyInputTransformation(private val maxValue: Int) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        val charSeq = asCharSequence()
        val intValue = charSeq.toString().toIntOrNull()

        if (!charSeq.isDigitsOnly()) {
            revertAllChanges()
        } else if (intValue != null && intValue > maxValue) {
            replace(0, length, "$maxValue")
        } else {
            // Strip leading zeroes
            val newText = charSeq.toString().let { if (it != "0") it.trimStart('0') else it }

            replace(0, length, newText)
        }
    }
}
