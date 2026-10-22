package com.arham.ssorganizer.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arham.ssorganizer.data.local.entity.ScreenshotEntity
import com.arham.ssorganizer.domain.detection.AppDetectionRules
import com.arham.ssorganizer.domain.search.SearchSnippetExtractor
import com.arham.ssorganizer.ui.home.ScreenshotViewModel
import com.arham.ssorganizer.ui.screenshots.ManualCorrectionDialog
import com.arham.ssorganizer.ui.screenshots.ScreenshotInspectDialog
import com.arham.ssorganizer.ui.theme.AppVisuals
import com.arham.ssorganizer.ui.viewer.FullScreenSwipeableViewer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: ScreenshotViewModel,
    initialQuery: String = "",
    initialAppName: String? = null,
    initialChatName: String? = null,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(initialQuery) }
    var selectedApp by remember { mutableStateOf<String?>(initialAppName) }
    var selectedChat by remember { mutableStateOf<String?>(initialChatName) }

    val appCounts by viewModel.appCounts.collectAsState()

    // Chats for the selected app (if selectedApp is a chat app)
    val isChatAppSelected = remember(selectedApp) {
        selectedApp?.let { AppDetectionRules.isChatApp(it) } ?: false
    }
    val chatCountsFlow = remember(selectedApp, isChatAppSelected) {
        if (isChatAppSelected && selectedApp != null) {
            viewModel.getChatCounts(selectedApp!!)
        } else {
            null
        }
    }
    val chatCounts by chatCountsFlow?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) }

    // Search results Flow from Room query
    val searchResultsFlow = remember(searchQuery, selectedApp, selectedChat) {
        viewModel.searchScreenshots(
            query = searchQuery,
            appName = selectedApp,
            chatName = selectedChat
        )
    }
    val searchResults by searchResultsFlow.collectAsState(initial = emptyList())

    // Fullscreen viewer, inspect and edit states
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    var inspectScreenshot by remember { mutableStateOf<ScreenshotEntity?>(null) }
    var editScreenshot by remember { mutableStateOf<ScreenshotEntity?>(null) }

    val hasActiveFilters = selectedApp != null || selectedChat != null || searchQuery.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search text, apps, chats...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search query"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    )
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
            // 1. App Filter Chips Row
            AppFilterChipsRow(
                apps = appCounts.map { it.detectedApp },
                selectedApp = selectedApp,
                onSelectApp = { app ->
                    selectedApp = app
                    selectedChat = null // Reset chat filter when app changes
                }
            )

            // 2. Chat / Contact Filter Chips Row (Visible only when a chat app is selected)
            if (isChatAppSelected && chatCounts.isNotEmpty()) {
                ChatFilterChipsRow(
                    chats = chatCounts.map { it.chatName },
                    selectedChat = selectedChat,
                    onSelectChat = { chat ->
                        selectedChat = chat
                    }
                )
            }

            // 3. Filter status bar (Shows active count & clear all option)
            if (hasActiveFilters) {
                FilterStatusBar(
                    resultCount = searchResults.size,
                    hasActiveFilters = hasActiveFilters,
                    onClearAll = {
                        searchQuery = ""
                        selectedApp = null
                        selectedChat = null
                    }
                )
            }

            // 4. Main Search Content: Results Grid or Contextual Empty State
            if (searchResults.isEmpty()) {
                if (searchQuery.isBlank() && selectedApp == null && selectedChat == null) {
                    InitialSearchPromptView(
                        onSelectSuggestion = { suggestion ->
                            searchQuery = suggestion
                        }
                    )
                } else {
                    NoSearchResultsView(
                        query = searchQuery,
                        hasFilters = selectedApp != null || selectedChat != null,
                        onReset = {
                            searchQuery = ""
                            selectedApp = null
                            selectedChat = null
                        }
                    )
                }
            } else {
                SearchResultsGrid(
                    screenshots = searchResults,
                    searchQuery = searchQuery,
                    onItemClick = { clickedItem ->
                        val idx = searchResults.indexOf(clickedItem)
                        viewerIndex = if (idx >= 0) idx else 0
                    },
                    onItemLongClick = { clickedItem ->
                        editScreenshot = clickedItem
                    }
                )
            }
        }
    }

    // Fullscreen Viewer
    viewerIndex?.let { index ->
        FullScreenSwipeableViewer(
            screenshots = searchResults,
            initialIndex = index,
            onDismiss = { viewerIndex = null },
            onOpenInfo = { item -> inspectScreenshot = item },
            onEdit = { item -> editScreenshot = item }
        )
    }

    // Inspection Dialog
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
 * Horizontally scrollable row of App filter chips.
 */
@Composable
private fun AppFilterChipsRow(
    apps: List<String>,
    selectedApp: String?,
    onSelectApp: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedApp == null,
            onClick = { onSelectApp(null) },
            label = { Text("All Apps") }
        )

        apps.forEach { appName ->
            val isSelected = selectedApp == appName
            val appColor = AppVisuals.getAppColor(appName)
            FilterChip(
                selected = isSelected,
                onClick = {
                    onSelectApp(if (isSelected) null else appName)
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(appColor, CircleShape)
                    )
                },
                label = { Text(appName) }
            )
        }
    }
}

/**
 * Horizontally scrollable row of Chat / Contact filter chips for a chat app.
 */
@Composable
private fun ChatFilterChipsRow(
    chats: List<String?>,
    selectedChat: String?,
    onSelectChat: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedChat == null,
            onClick = { onSelectChat(null) },
            label = { Text("All Chats") }
        )

        chats.forEach { chatName ->
            val isUnspecified = chatName.isNullOrBlank()
            val chipLabel = if (isUnspecified) "General / Direct" else chatName!!
            val resolvedValue = if (isUnspecified) "__unspecified__" else chatName
            val isSelected = selectedChat == resolvedValue

            FilterChip(
                selected = isSelected,
                onClick = {
                    onSelectChat(if (isSelected) null else resolvedValue)
                },
                label = { Text(chipLabel) }
            )
        }
    }
}

/**
 * Compact bar showing results count and a quick "Clear all" button.
 */
@Composable
private fun FilterStatusBar(
    resultCount: Int,
    hasActiveFilters: Boolean,
    onClearAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$resultCount screenshot${if (resultCount != 1) "s" else ""} found",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (hasActiveFilters) {
            TextButton(
                onClick = onClearAll,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = "Clear All",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Grid of search results with thumbnail, app pill, and matching OCR text snippet.
 */
@Composable
private fun SearchResultsGrid(
    screenshots: List<ScreenshotEntity>,
    searchQuery: String,
    onItemClick: (ScreenshotEntity) -> Unit,
    onItemLongClick: (ScreenshotEntity) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 105.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = screenshots,
            key = { it.mediaStoreId }
        ) { item ->
            SearchResultGridItem(
                screenshot = item,
                searchQuery = searchQuery,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) }
            )
        }
    }
}

@Composable
private fun SearchResultGridItem(
    screenshot: ScreenshotEntity,
    searchQuery: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val snippet = remember(screenshot.ocrText, searchQuery) {
        if (searchQuery.isNotBlank()) {
            SearchSnippetExtractor.extractSnippet(screenshot.ocrText, searchQuery)
        } else {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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

                // App pill badge
                if (screenshot.detectedApp.isNotBlank() && screenshot.detectedApp != "Pending") {
                    val appColor = AppVisuals.getAppColor(screenshot.detectedApp)
                    Surface(
                        color = appColor.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = screenshot.detectedApp,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Matching OCR text snippet preview if found
            if (!snippet.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = snippet,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Initial empty state when no query or filters are typed.
 * Offers quick suggestion chips for common screenshot keywords.
 */
@Composable
private fun InitialSearchPromptView(
    onSelectSuggestion: (String) -> Unit
) {
    val suggestions = listOf("OTP", "Receipt", "Code", "Password", "Invoice", "Delivery", "Address", "Payment")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Search Screenshots",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Search across recognized OCR text, apps, contacts, and file names.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Suggestions",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center
        ) {
            suggestions.forEach { tag ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable { onSelectSuggestion(tag) }
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Empty state when no matches are found for the active query and filters.
 */
@Composable
private fun NoSearchResultsView(
    query: String,
    hasFilters: Boolean,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔍",
            style = MaterialTheme.typography.displayMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No screenshots found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        val description = if (query.isNotBlank() && hasFilters) {
            "No screenshots match \"$query\" with the selected filters."
        } else if (query.isNotBlank()) {
            "No screenshots containing text \"$query\" were found."
        } else {
            "No screenshots match the selected filters."
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = onReset) {
            Text("Clear Search & Filters")
        }
    }
}
