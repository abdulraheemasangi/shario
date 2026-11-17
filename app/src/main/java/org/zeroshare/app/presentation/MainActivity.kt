package org.zeroshare.app.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import org.zeroshare.app.core.security.QrPayloadCodec
import org.zeroshare.app.domain.model.PeerDevice
import org.zeroshare.app.domain.model.ShareableItem
import org.zeroshare.app.domain.model.TransferStatus
import org.zeroshare.app.presentation.discovery.DiscoveryReceiverScreen
import org.zeroshare.app.presentation.discovery.DiscoverySenderScreen
import org.zeroshare.app.presentation.discovery.QrScannerScreen
import org.zeroshare.app.presentation.history.HistoryScreen
import org.zeroshare.app.presentation.home.HomeScreen
import org.zeroshare.app.presentation.picker.FilePickerScreen
import org.zeroshare.app.presentation.theme.GlassBackground
import org.zeroshare.app.presentation.theme.SharioTheme
import org.zeroshare.app.presentation.transfer.TransferProgressScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.loadMedia()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            SharioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GlassBackground
                ) {
                    AppNavigation(
                        viewModel = viewModel,
                        hasStoragePermissions = { hasRequiredStoragePermissions() },
                        onRequestPermissions = { checkAndRequestPermissions() },
                        onOpenSettings = { openAppSettings() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasRequiredStoragePermissions()) {
            viewModel.loadMedia()
        }
    }

    fun hasRequiredStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (ignored: Exception) {}
    }

    fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        // Media & EXIF Location permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Essential for preserving original GPS coordinates in photo/video EXIF
            permissions.add(Manifest.permission.ACCESS_MEDIA_LOCATION)
        }

        permissions.add(Manifest.permission.CAMERA)

        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            requestPermissionsLauncher.launch(ungranted.toTypedArray())
        } else {
            viewModel.loadMedia()
        }
    }
}

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    hasStoragePermissions: () -> Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val navController = rememberNavController()
    var queuedItemsToSend by remember { mutableStateOf<List<ShareableItem>>(emptyList()) }

    val transferState by viewModel.transferSessionState.collectAsState()

    // Problem 5 Fix: Automatically navigate to active transfer screen ONLY when transfer actually starts transferring
    LaunchedEffect(transferState.status) {
        if (transferState.status == TransferStatus.TRANSFERRING) {
            navController.navigate("transfer") {
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToSend = {
                    if (!hasStoragePermissions()) {
                        onRequestPermissions()
                    }
                    navController.navigate("picker")
                },
                onNavigateToReceive = {
                    viewModel.prepareToReceive("Phone Receiver")
                    navController.navigate("receive")
                },
                onNavigateToHistory = { navController.navigate("history") }
            )
        }

        composable("picker") {
            val photos by viewModel.photos.collectAsState()
            val videos by viewModel.videos.collectAsState()
            val audio by viewModel.audio.collectAsState()
            val documents by viewModel.documents.collectAsState()
            val apps by viewModel.apps.collectAsState()

            FilePickerScreen(
                photos = photos,
                videos = videos,
                audio = audio,
                documents = documents,
                apps = apps,
                hasPermission = hasStoragePermissions(),
                onRequestPermissions = onRequestPermissions,
                onOpenSettings = onOpenSettings,
                onProceedToSend = { selected ->
                    queuedItemsToSend = selected
                    viewModel.startDiscovery()
                    navController.navigate("discovery")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("discovery") {
            val peers by viewModel.discoveredPeers.collectAsState()
            DiscoverySenderScreen(
                discoveredPeers = peers,
                onSelectPeer = { peer ->
                    viewModel.startTransferToPeer(peer, queuedItemsToSend, null)
                },
                onScanQrCode = {
                    navController.navigate("scanner")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("scanner") {
            QrScannerScreen(
                onQrCodeDetected = { qrText ->
                    try {
                        val payload = QrPayloadCodec.decode(qrText)
                        val peer = PeerDevice(
                            id = payload.receiverDeviceName,
                            name = payload.receiverDeviceName,
                            address = payload.ipAddress,
                            port = payload.port,
                            connectionType = payload.connectionType
                        )
                        viewModel.startTransferToPeer(peer, queuedItemsToSend, payload.aesKeyBase64)
                    } catch (ignored: Exception) {}
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("receive") {
            val payload by viewModel.receiverQrPayload.collectAsState()
            payload?.let {
                DiscoveryReceiverScreen(
                    payload = it,
                    onBack = {
                        viewModel.cancelTransfer()
                        navController.popBackStack()
                    }
                )
            }
        }

        composable("transfer") {
            TransferProgressScreen(
                state = transferState,
                onPause = { viewModel.pauseTransfer() },
                onResume = { viewModel.resumeTransfer() },
                onCancel = {
                    viewModel.cancelTransfer()
                    navController.navigate("home") { popUpTo("home") { inclusive = true } }
                },
                onFinish = {
                    navController.navigate("history") { popUpTo("home") }
                }
            )
        }

        composable("history") {
            val records by viewModel.historyRecords.collectAsState()
            HistoryScreen(
                records = records,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
