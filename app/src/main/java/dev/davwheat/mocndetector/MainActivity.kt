package dev.davwheat.mocndetector

import IsThereMocn
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint
import dev.davwheat.mocndetector.ui.theme.MOCNDetectorTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MOCNDetectorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val fineLocationPermission = rememberPermissionState(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    val phoneStatePermission = rememberPermissionState(
                        android.Manifest.permission.READ_PHONE_STATE
                    )

                    if (fineLocationPermission.status.isGranted && phoneStatePermission.status.isGranted) {
                        IsThereMocn(modifier = Modifier.fillMaxSize(), padding = innerPadding)
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
