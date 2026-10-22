package com.arham.ssorganizer.ui.screenshots

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arham.ssorganizer.data.local.dao.AppCount
import com.arham.ssorganizer.data.local.entity.ScreenshotEntity
import com.arham.ssorganizer.ui.home.ScanProgressState
import com.arham.ssorganizer.ui.home.ScreenshotViewModel
import com.arham.ssorganizer.ui.settings.SettingsDialog
import com.arham.ssorganizer.ui.viewer.FullScreenSwipeableViewer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotGridScreen(
    viewModel: ScreenshotViewModel,
    appName: String? = null,
    chatName: String? = null,
    onBack: (() -> Unit)? = null,
    onNavigateToSearch: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val totalCount by viewModel.totalCount.collectAsState()
    val doneCount by viewModel.doneCount.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val failedCount by viewModel.failedCount.collectAsState()
    val lastScanTime by viewModel.lastScanTime.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isClassifying by viewModel.isClassifying.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val appCounts by viewModel.appCounts.collectAsState()
    val extraFolders by viewModel.extraFolders.collectAsState(initial = emptySet())

    val rawScreenshots by if (appName != null) {
        remember(appName, chatName) {
            if (chatName != null) {
                val qChat = if (chatName == "__unspecified__") null else chatName
                viewModel.getScreenshotsForChat(appName, qChat)
            } else {
                viewModel.getScreenshotsForApp(appName)
            }
        }.collectAsState(initial = emptyList())
    } else {
        viewModel.screenshots.collectAsState()
    }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showMiuiTip by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    var inspectScreenshot by remember { mutableStateOf<ScreenshotEntity?>(null) }
    var editScreenshot by remember { mutableStateOf<ScreenshotEntity?>(null) }
    var selectedAppFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.syncWithMediaStore()
    }

    val displayedScreenshots = remember(rawScreenshots, selectedAppFilter, appName) {
        if (appName != null) {
            rawScreenshots
        } else if (selectedAppFilter == null) {
            rawScreenshots
        } else {
            rawScreenshots.filter { it.detectedApp == selectedAppFilter }
        }
    }

    val titleText = remember(appName, chatName) {
        when {
            chatName != null -> if (chatName == "__unspecified__") "General / Direct" else chatName
            appName != null -> appName
            else -> "All Screenshots"
        }
    }

    val subtitleText = remember(appName, chatName, displayedScreenshots.size, isSyncing, isClassifying, doneCount, totalCount) {
        when {
            isSyncing -> "Syncing screenshots..."
            isClassifying -> "Classifying apps..."
            appName != null || chatName != null -> "${displayedScreenshots.size} screenshots"
            else -> "$doneCount of $totalCount indexed"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (onNavigateToSearch != null) {
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.syncWithMediaStore() },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync MediaStore"
                            )
                        }
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Folder Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Scan Progress Banner
            AnimatedVisibility(visible = scanProgress.isScanning) {
                ScanProgressBarCard(
                    scanProgress = scanProgress,
                    onCancelScan = { viewModel.cancelScan() }
                )
            }

            // Normal Scan Controls & Stats Header (only shown on root "All Screenshots" view)
            if (appName == null && !scanProgress.isScanning) {
                ScanHeaderCard(
                    total = totalCount,
                    done = doneCount,
                    pending = pendingCount,
                    failed = failedCount,
                    lastScanTime = lastScanTime,
                    isSyncing = isSyncing,
                    onStartScan = { viewModel.startScan() }
                )
            }

            // App Categories Breakdown Filter Bar (only on root "All Screenshots" view)
            if (appName == null && appCounts.isNotEmpty()) {
                AppCategoryChipsRow(
                    appCounts = appCounts,
                    selectedApp = selectedAppFilter,
                    onSelectApp = { selectedAppFilter = it }
                )
            }

            // Xiaomi MIUI Tip
            AnimatedVisibility(visible = showMiuiTip) {
                MiuiTipCard(
                    onDismiss = { showMiuiTip = false },
                    onOpenSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }
                )
            }

            // Syncing / Classification indicator
            if ((isSyncing || isClassifying) && displayedScreenshots.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }

            // Screenshots Grid
            if (displayedScreenshots.isEmpty()) {
                if (isSyncing) {
                    SyncLoadingView()
                } else if (syncError != null) {
                    SyncErrorView(
                        message = syncError ?: "Error",
                        onRetry = { viewModel.syncWithMediaStore() }
                    )
                } else {
                    EmptyScreenshotsView(onOpenSettings = { showSettingsDialog = true })
                }
            } else {
                ScreenshotsGrid(
                    screenshots = displayedScreenshots,
                    onItemClick = { clickedItem ->
                        val idx = displayedScreenshots.indexOf(clickedItem)
                        viewerIndex = if (idx >= 0) idx else 0
                    },
                    onItemLongClick = { clickedItem ->
                        editScreenshot = clickedItem
                    }
                )
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Full-Screen Swipeable Viewer with pinch-to-zoom & share
    viewerIndex?.let { index ->
        FullScreenSwipeableViewer(
            screenshots = displayedScreenshots,
            initialIndex = index,
            onDismiss = { viewerIndex = null },
            onOpenInfo = { item ->
                inspectScreenshot = item
            },
            onEdit = { item ->
                editScreenshot = item
            }
        )
    }

    // Hidden Debug / Inspection Dialog
    inspectScreenshot?.let { item ->
        ScreenshotInspectDialog(
            screenshot = item,
            onDismiss = { inspectScreenshot = null },
            onEdit = {
                inspectScreenshot = null
                editScreenshot = item
            }
        )
    }

    // Manual Correction Dialog
    editScreenshot?.let { item ->
        ManualCorrectionDialog(
            screenshot = item,
            onSave = { newApp, newChat ->
                viewModel.updateManualCorrection(item.mediaStoreId, newApp, newChat)
                editScreenshot = null
            },
            onDismiss = { editScreenshot = null }
        )
    }
}

/**
 * Issue 1: Full-Screen Image Viewer
 * Displays the original screenshot in its true aspect ratio without cropping,
 * with back button, info action (opens Inspector), and manual edit action.
 */
@Composable
private fun FullScreenViewerDialog(
    screenshot: ScreenshotEntity,
    onDismiss: () -> Unit,
    onOpenInfo: () -> Unit,
    onEdit: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val context = LocalContext.current
        var showControls by remember { mutableStateOf(true) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showControls = !showControls
                }
        ) {
            // Uncropped full image at exact aspect ratio
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(screenshot.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = screenshot.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Top Bar Overlay
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 28.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close Viewer",
                                tint = Color.White
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = screenshot.displayName,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val dimensionsText = if (screenshot.width > 0 && screenshot.height > 0) {
                                "${screenshot.width} × ${screenshot.height} px"
                            } else {
                                ""
                            }
                            if (dimensionsText.isNotBlank()) {
                                Text(
                                    text = dimensionsText,
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        // Edit App/Chat Action
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit App / Chat",
                                tint = Color.White
                            )
                        }

                        // Info / Inspector Action
                        IconButton(onClick = onOpenInfo) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Screenshot Info & OCR",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Bottom App Info Pill
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = screenshot.detectedApp,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        if (!screenshot.chatName.isNullOrBlank()) {
                            Text(
                                text = screenshot.chatName,
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppCategoryChipsRow(
    appCounts: List<AppCount>,
    selectedApp: String?,
    onSelectApp: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = selectedApp == null,
            onClick = { onSelectApp(null) },
            label = { Text("All") }
        )
        for (item in appCounts) {
            FilterChip(
                selected = selectedApp == item.detectedApp,
                onClick = {
                    onSelectApp(if (selectedApp == item.detectedApp) null else item.detectedApp)
                },
                label = { Text("${item.detectedApp} (${item.count})") }
            )
        }
    }
}

@Composable
internal fun ScanProgressBarCard(
    scanProgress: ScanProgressState,
    onCancelScan: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scanning Screenshots...",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (scanProgress.total > 0) {
                            "Progress: ${scanProgress.current} / ${scanProgress.total}"
                        } else {
                            scanProgress.statusText
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                OutlinedButton(
                    onClick = onCancelScan,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancel")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (scanProgress.total > 0) {
                val progressFraction = scanProgress.current.toFloat() / scanProgress.total.toFloat()
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@Composable
internal fun ScanHeaderCard(
    total: Int,
    done: Int,
    pending: Int,
    failed: Int,
    lastScanTime: Long?,
    isSyncing: Boolean,
    onStartScan: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusBadge(label = "Done: $done", color = Color(0xFF2E7D32))
                        StatusBadge(label = "Pending: $pending", color = Color(0xFFF57C00))
                        if (failed > 0) {
                            StatusBadge(label = "Failed: $failed", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (lastScanTime != null) {
                        val formattedDate = remember(lastScanTime) {
                            val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                            sdf.format(Date(lastScanTime))
                        }
                        Text(
                            text = "Last scan: $formattedDate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Button(
                    onClick = onStartScan,
                    enabled = !isSyncing && pending > 0,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (pending > 0) "Scan ($pending)" else "Scan")
                }
            }
        }
    }
}

@Composable
internal fun StatusBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
internal fun MiuiTipCard(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Xiaomi / MIUI Tip",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Set battery saver to 'No restrictions' & enable autostart so MIUI doesn't stop long scans.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                TextButton(
                    onClick = onOpenSettings,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Open App Settings", style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss tip",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SyncLoadingView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Discovering screenshots on your device...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SyncErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⚠️",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Failed to sync screenshots",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun ScreenshotsGrid(
    screenshots: List<ScreenshotEntity>,
    onItemClick: (ScreenshotEntity) -> Unit,
    onItemLongClick: (ScreenshotEntity) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 105.dp),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = screenshots,
            key = { it.mediaStoreId }
        ) { item ->
            ScreenshotGridItem(
                screenshot = item,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScreenshotGridItem(
    screenshot: ScreenshotEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(screenshot.uri)
                .size(320)
                .crossfade(false)
                .build(),
            contentDescription = screenshot.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // App pill badge in top-left
        if (screenshot.detectedApp.isNotBlank() && screenshot.detectedApp != "Pending") {
            val appPillColor = when (screenshot.detectedApp) {
                "WhatsApp" -> Color(0xFF1B5E20)
                "Instagram" -> Color(0xFF880E4F)
                "Telegram" -> Color(0xFF0D47A1)
                "Careem" -> Color(0xFF2E7D32)
                "inDrive" -> Color(0xFF00796B)
                "Uber" -> Color(0xFF212121)
                "Easypaisa" -> Color(0xFF00695C)
                "JazzCash" -> Color(0xFFBF360C)
                "Messenger" -> Color(0xFF0277BD)
                "X" -> Color(0xFF212121)
                "YouTube" -> Color(0xFFB71C1C)
                "Chrome" -> Color(0xFFE65100)
                "Settings" -> Color(0xFF37474F)
                else -> Color(0xFF424242)
            }
            Surface(
                color = appPillColor.copy(alpha = 0.85f),
                shape = RoundedCornerShape(topStart = 6.dp, bottomEnd = 8.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = screenshot.detectedApp,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Status badge overlay in bottom-right corner
        val badgeColor = when (screenshot.status) {
            ScreenshotEntity.STATUS_DONE -> Color(0xFF2E7D32)
            ScreenshotEntity.STATUS_FAILED -> MaterialTheme.colorScheme.error
            else -> Color(0xFFF57C00)
        }
        val badgeIcon = when (screenshot.status) {
            ScreenshotEntity.STATUS_DONE -> Icons.Default.Check
            ScreenshotEntity.STATUS_FAILED -> Icons.Default.Warning
            else -> null
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(18.dp)
                .background(badgeColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (badgeIcon != null) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = screenshot.status,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Inspection / Debug Dialog:
 * Shows raw OCR text, rule-based detection score, matched keywords, and extracted chat name.
 */
@Composable
internal fun ScreenshotInspectDialog(
    screenshot: ScreenshotEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inspector",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit App/Chat")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = screenshot.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Detection Info Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Detected App:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text(screenshot.detectedApp, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }

                        if (screenshot.classificationSource == ScreenshotEntity.SOURCE_MANUAL) {
                            Text(
                                text = "Source: Manual correction (saved)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Rule Score: ${screenshot.appConfidence}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (screenshot.matchedKeywords.isNotBlank()) {
                            Text(
                                text = "Matched: ${screenshot.matchedKeywords}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Chat / Contact:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text(screenshot.chatName ?: "(None detected)", style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        val statusDesc = when (screenshot.classificationStatus) {
                            ScreenshotEntity.STATUS_CLASSIFIED -> "Classified (High confidence)"
                            ScreenshotEntity.STATUS_UNSUPPORTED_LANGUAGE -> "No Latin text recognized (e.g. Urdu, graphics, or blank)"
                            else -> "Unclassified (Insufficient keywords)"
                        }
                        Text(
                            text = "Status: $statusDesc",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Raw OCR Text (${screenshot.ocrText.length} chars):",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (screenshot.ocrText.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = screenshot.ocrText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    Text(
                        text = "(No text was recognized in this screenshot)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Manual Correction Dialog:
 * Long-press or Inspector shortcut to override app detection and chat name.
 */
@Composable
internal fun ManualCorrectionDialog(
    screenshot: ScreenshotEntity,
    onSave: (String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var appName by remember { mutableStateOf(if (screenshot.detectedApp == "Pending") "" else screenshot.detectedApp) }
    var chatName by remember { mutableStateOf(screenshot.chatName ?: "") }

    val commonApps = listOf("WhatsApp", "Instagram", "Careem", "inDrive", "Uber", "YouTube", "Messenger", "Telegram", "Easypaisa", "JazzCash", "Banking", "Chrome", "Settings", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit App & Chat") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Quick select app:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (app in commonApps) {
                        FilterChip(
                            selected = appName.equals(app, ignoreCase = true),
                            onClick = { appName = app },
                            label = { Text(app, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("App Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = chatName,
                    onValueChange = { chatName = it },
                    label = { Text("Chat / Contact Name (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (appName.isNotBlank()) {
                        onSave(appName, chatName.ifBlank { null })
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EmptyScreenshotsView(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📁",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No screenshots found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No images found in standard Screenshots folders. You can add extra folder paths in settings.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        TextButton(onClick = onOpenSettings) {
            Text("Add Extra Folders")
        }
    }
}

@Composable
internal fun FolderSettingsDialog(
    extraFolders: Set<String>,
    onAddFolder: (String) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newFolderPath by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Scan Folders") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Default pattern: matches any path containing \"Screenshots\" (e.g. Pictures/Screenshots, DCIM/Screenshots).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Extra Folders:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                if (extraFolders.isEmpty()) {
                    Text(
                        text = "No extra folders added.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(vertical = 4.dp)
                    ) {
                        items(extraFolders.toList()) { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = folder,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onRemoveFolder(folder) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove folder",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newFolderPath,
                        onValueChange = { newFolderPath = it },
                        label = { Text("Path snippet (e.g. DCIM)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newFolderPath.isNotBlank()) {
                                onAddFolder(newFolderPath.trim())
                                newFolderPath = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add folder",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
