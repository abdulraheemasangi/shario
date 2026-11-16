package org.zeroshare.app.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import org.zeroshare.app.domain.model.ShareableItem
import org.zeroshare.app.domain.model.TransferStatus
import org.zeroshare.app.presentation.discovery.DiscoveryReceiverScreen
import org.zeroshare.app.presentation.discovery.DiscoverySenderScreen
import org.zeroshare.app.presentation.history.HistoryScreen
import org.zeroshare.app.presentation.home.HomeScreen
import org.zeroshare.app.presentation.picker.FilePickerScreen
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
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            SharioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel)
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
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
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    var queuedItemsToSend by remember { mutableStateOf<List<ShareableItem>>(emptyList()) }

    val transferState by viewModel.transferSessionState.collectAsState()

    // Automatically navigate to active transfer screen when transfer starts
    LaunchedEffect(transferState.status) {
        if (transferState.status == TransferStatus.TRANSFERRING || transferState.status == TransferStatus.CONNECTING) {
            navController.navigate("transfer") {
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToSend = { navController.navigate("picker") },
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
            val apps by viewModel.apps.collectAsState()

            FilePickerScreen(
                photos = photos,
                videos = videos,
                audio = audio,
                apps = apps,
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
                    // QR scanner flow
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("receive") {
            val payload by viewModel.receiverQrPayload.collectAsState()
            payload?.let {
                DiscoveryReceiverScreen(
                    payload = it,
                    onBack = { navController.popBackStack() }
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
