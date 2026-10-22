package com.arham.ssorganizer.ui.viewer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arham.ssorganizer.data.local.entity.ScreenshotEntity
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenSwipeableViewer(
    screenshots: List<ScreenshotEntity>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onOpenInfo: (ScreenshotEntity) -> Unit,
    onEdit: (ScreenshotEntity) -> Unit
) {
    if (screenshots.isEmpty()) return

    val validInitialIndex = initialIndex.coerceIn(0, screenshots.size - 1)
    val pagerState = rememberPagerState(
        initialPage = validInitialIndex,
        pageCount = { screenshots.size }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val context = LocalContext.current
        var showControls by remember { mutableStateOf(true) }
        val currentScreenshot = screenshots.getOrNull(pagerState.currentPage) ?: return@Dialog

        // Scale of the currently visible screenshot
        var currentScale by remember { mutableFloatStateOf(1f) }

        // When current page changes, reset the active scale so new page is always scrollable
        LaunchedEffect(pagerState.currentPage) {
            currentScale = 1f
        }

        // Latch to prevent duplicate share intents within 1.2 seconds
        var lastShareTimestamp by remember { mutableLongStateOf(0L) }
        val handleShare: (ScreenshotEntity) -> Unit = remember(context) {
            { targetScreenshot ->
                val now = SystemClock.uptimeMillis()
                if (now - lastShareTimestamp > 1200L) {
                    lastShareTimestamp = now
                    shareScreenshot(context, targetScreenshot.uri)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                // Only allow swiping pages when not zoomed in
                userScrollEnabled = currentScale <= 1.05f,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val screenshot = screenshots[page]
                ZoomableScreenshotImage(
                    screenshot = screenshot,
                    isCurrentPage = page == pagerState.currentPage,
                    onScaleChanged = { scale ->
                        if (page == pagerState.currentPage) {
                            currentScale = scale
                        }
                    },
                    onToggleControls = {
                        showControls = !showControls
                    },
                    onSwipeUpShare = {
                        handleShare(screenshot)
                    }
                )
            }

            // Top Control Bar Overlay
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
                                text = "${pagerState.currentPage + 1} of ${screenshots.size}",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentScreenshot.displayName,
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // On-screen Share Action button
                        IconButton(
                            onClick = {
                                handleShare(currentScreenshot)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Screenshot",
                                tint = Color.White
                            )
                        }

                        // Edit Action
                        IconButton(onClick = { onEdit(currentScreenshot) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit App / Chat",
                                tint = Color.White
                            )
                        }

                        // Info / Inspector Action
                        IconButton(onClick = { onOpenInfo(currentScreenshot) }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Screenshot Info & OCR",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Bottom Info Pill Overlay
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
                                text = currentScreenshot.detectedApp,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        if (!currentScreenshot.chatName.isNullOrBlank()) {
                            Text(
                                text = currentScreenshot.chatName,
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (currentScreenshot.width > 0 && currentScreenshot.height > 0) {
                            Text(
                                text = "• ${currentScreenshot.width}×${currentScreenshot.height}",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Zoomable, Pannable, and Swipeable individual screenshot view.
 *
 * Implements a non-blocking gesture hierarchy:
 * 1. Double tap: toggles 1x and 2.5x zoom.
 * 2. Single tap: toggles overlay controls.
 * 3. 1x Scale:
 *    - Horizontal swipe: NOT consumed here; allows HorizontalPager to flip pages smoothly.
 *    - Upward swipe: Detected with vertical direction dominance and threshold; displays tactile
 *      upward glide and triggers onSwipeUpShare when released.
 *    - Two-finger pinch: Initiates zoom into the image.
 * 4. Zoomed Scale (> 1.05x):
 *    - HorizontalPager is disabled.
 *    - Single-finger and multi-finger pan smoothly translates the zoomed image with boundaries.
 *    - Pinch adjusts zoom level (clamped 1f - 5f).
 *    - Swipe-to-share is disabled while zoomed to prevent accidental triggers during inspection.
 */
@Composable
private fun ZoomableScreenshotImage(
    screenshot: ScreenshotEntity,
    isCurrentPage: Boolean,
    onScaleChanged: (Float) -> Unit,
    onToggleControls: () -> Unit,
    onSwipeUpShare: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Swipe up offset for share gesture
    var swipeUpRawY by remember { mutableFloatStateOf(0f) }
    val animatedSwipeUpY by animateFloatAsState(
        targetValue = swipeUpRawY,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "SwipeUpGlide"
    )

    val shareThresholdPx = remember(density) {
        with(density) { 110.dp.toPx() }
    }

    // Reset zoom when image changes or page is deactivated
    LaunchedEffect(screenshot.mediaStoreId) {
        scale = 1f
        offset = Offset.Zero
        swipeUpRawY = 0f
        onScaleChanged(1f)
    }

    if (!isCurrentPage && scale > 1.05f) {
        LaunchedEffect(Unit) {
            scale = 1f
            offset = Offset.Zero
            onScaleChanged(1f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            // 1. Tap & Double-Tap detection
            .pointerInput(scale) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            offset = Offset.Zero
                        }
                        onScaleChanged(scale)
                    }
                )
            }
            // 2. Gesture pipeline: 2-finger pinch, 1-finger pan (when zoomed), and upward swipe-to-share (when 1x)
            .pointerInput(scale, shareThresholdPx) {
                val touchSlop = viewConfiguration.touchSlop

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var accumulatedDx = 0f
                    var accumulatedDy = 0f
                    var isSwipeUpActive = false
                    var isHorizontalScrollActive = false
                    var isZoomingActive = false

                    do {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.size

                        if (scale > 1.05f) {
                            // --- ZOOMED IN MODE ---
                            // In zoomed mode, HorizontalPager is disabled.
                            // Pan and pinch take precedence. Swipe-to-share is disabled.
                            if (pointerCount >= 2) {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()

                                scale = (scale * zoomChange).coerceIn(1f, 5f)
                                offset = clampOffset(offset + panChange, scale, containerSize)
                                event.changes.forEach { it.consume() }
                                onScaleChanged(scale)
                            } else if (pointerCount == 1) {
                                val panChange = event.calculatePan()
                                offset = clampOffset(offset + panChange, scale, containerSize)
                                event.changes.forEach { it.consume() }
                            }
                        } else {
                            // --- 1X SCALE MODE ---
                            if (pointerCount >= 2) {
                                // Pinch to zoom initiation
                                val zoomChange = event.calculateZoom()
                                if (abs(zoomChange - 1f) > 0.015f) {
                                    isZoomingActive = true
                                    scale = (scale * zoomChange).coerceIn(1f, 5f)
                                    event.changes.forEach { it.consume() }
                                    onScaleChanged(scale)
                                }
                            } else if (pointerCount == 1 && !isZoomingActive) {
                                val pan = event.calculatePan()
                                accumulatedDx += pan.x
                                accumulatedDy += pan.y

                                // Direction disambiguation once touch slop is passed
                                if (!isSwipeUpActive && !isHorizontalScrollActive) {
                                    if (accumulatedDy < -touchSlop && abs(accumulatedDy) > abs(accumulatedDx) * 1.3f) {
                                        // Dominant upward drag -> Lock into swipe-to-share
                                        isSwipeUpActive = true
                                    } else if (abs(accumulatedDx) > touchSlop) {
                                        // Dominant horizontal drag -> Let HorizontalPager handle it
                                        isHorizontalScrollActive = true
                                    }
                                }

                                if (isSwipeUpActive) {
                                    // Consume vertical drag pointer events so HorizontalPager ignores them
                                    event.changes.forEach { it.consume() }
                                    swipeUpRawY = (swipeUpRawY + pan.y * 0.5f).coerceAtMost(0f)
                                } else if (isHorizontalScrollActive) {
                                    // DO NOT consume event changes: Let HorizontalPager receive unconsumed horizontal delta!
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    // Gesture finished (all fingers lifted)
                    if (isSwipeUpActive) {
                        if (abs(swipeUpRawY) >= shareThresholdPx) {
                            onSwipeUpShare()
                        }
                        // Smoothly spring back to 0
                        swipeUpRawY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val visualTranslationY = if (scale <= 1.05f) {
            (animatedSwipeUpY * 0.45f).coerceIn(-180f, 0f)
        } else {
            offset.y
        }

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(screenshot.uri)
                .crossfade(true)
                .build(),
            contentDescription = screenshot.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = if (scale > 1.05f) offset.x else 0f,
                    translationY = visualTranslationY
                )
        )

        // Visual Pill Indicator during upward swipe gesture
        AnimatedVisibility(
            visible = scale <= 1.05f && abs(swipeUpRawY) > 35f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) {
            val isReadyToShare = abs(swipeUpRawY) >= shareThresholdPx
            Surface(
                color = if (isReadyToShare) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isReadyToShare) "Release to Share" else "Swipe up to Share",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Constrains the pan offset so the zoomed image cannot be panned completely off the screen.
 */
internal fun clampOffset(offset: Offset, scale: Float, size: IntSize): Offset {
    if (scale <= 1.05f || size.width <= 0 || size.height <= 0) {
        return Offset.Zero
    }
    val maxOffsetX = (size.width * (scale - 1f)) / 2f
    val maxOffsetY = (size.height * (scale - 1f)) / 2f

    return Offset(
        x = offset.x.coerceIn(-maxOffsetX, maxOffsetX),
        y = offset.y.coerceIn(-maxOffsetY, maxOffsetY)
    )
}

/**
 * Invokes standard Android ACTION_SEND share sheet.
 */
private fun shareScreenshot(context: Context, uriString: String) {
    try {
        val uri = Uri.parse(uriString)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Screenshot"))
    } catch (e: Exception) {
        // Fallback safety
    }
}
