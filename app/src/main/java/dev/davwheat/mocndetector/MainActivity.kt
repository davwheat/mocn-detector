package dev.davwheat.mocndetector

import IsThereMocn
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint
import dev.davwheat.mocndetector.extension.plus
import dev.davwheat.mocndetector.ui.settings.SettingsSheet
import dev.davwheat.mocndetector.ui.theme.MOCNDetectorTheme

val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState?> { null }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MOCNDetectorTheme {
                CompositionLocalProvider(value = LocalSnackbarHostState provides remember { SnackbarHostState() }) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = {
                            SnackbarHost(LocalSnackbarHostState.current!!)
                        }, floatingActionButton = {
                            var showingBottomSheet by rememberSaveable { mutableStateOf(false) }
                            val sheetState = rememberModalBottomSheetState()
                            ExtendedFloatingActionButton(
                                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                                text = { Text("Settings") },
                                onClick = { showingBottomSheet = true },
                            )

                            if (showingBottomSheet) {
                                ModalBottomSheet(
                                    modifier =
                                        Modifier.windowInsetsPadding(
                                            WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                                        ),
                                    onDismissRequest = { showingBottomSheet = false },
                                    sheetState = sheetState,
                                    containerColor = MaterialTheme.colorScheme.background,
                                ) {
                                    SettingsSheet(
                                        onDismissRequest = {
                                            showingBottomSheet = false
                                        },
                                    )
                                }
                            }
                        }) { innerPadding ->
                        val fineLocationPermission = rememberPermissionState(
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        )
                        val phoneStatePermission = rememberPermissionState(
                            android.Manifest.permission.READ_PHONE_STATE
                        )

                        if (fineLocationPermission.status.isGranted && phoneStatePermission.status.isGranted) {
                            IsThereMocn(
                                modifier = Modifier.fillMaxSize(),
                                padding = innerPadding + PaddingValues(bottom = 96.dp),
                            )
                        } else {
                            Column(
                                Modifier
                                    .padding(innerPadding)
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(
                                    4.dp,
                                    Alignment.CenterVertically
                                ),
                            ) {
                                Text(
                                    text = "Location and phone state permissions are required to detect MOCN, due to Android being Android."
                                )
                                Button(onClick = {
                                    if (!fineLocationPermission.status.isGranted) {
                                        fineLocationPermission.launchPermissionRequest()
                                    } else {
                                        phoneStatePermission.launchPermissionRequest()
                                    }
                                }) {
                                    Text("Grant permission")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
