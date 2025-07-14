package dev.davwheat.mocndetector.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Output
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.davwheat.mocndetector.LocalSnackbarHostState
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
                onDismissRequest()

                snackbarState?.let { s ->
                    scope.launch {
                        s.showSnackbar("Data written successfully")
                    }
                }
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
                            onDismissRequest()

                            snackbarState?.let { s ->
                                scope.launch {
                                    s.showSnackbar("All data deleted successfully")
                                }
                            }
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
    }
}
