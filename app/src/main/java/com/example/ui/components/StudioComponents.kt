package com.example.ui.components

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.i18n.StudioStrings
import com.example.model.AspectRatioOption
import com.example.model.GenerationProgress
import com.example.model.MainTab
import com.example.model.SubtitleSegment
import com.example.model.SubtitleStyleOption
import com.example.model.VideoFilterOption
import com.example.model.VideoStyleOption
import com.example.ui.theme.AmberCredit
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f)
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        content()
    }
}

@Composable
fun CreditBadge(
    credits: Long,
    planTier: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag("credit_badge"),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, AmberCredit.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = "Credits",
                tint = AmberCredit,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (planTier == "UNLIMITED") "∞" else credits.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = planTier,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun StudioBottomBar(
    activeTab: MainTab,
    strings: StudioStrings,
    onSelectTab: (MainTab) -> Unit
) {
    val items = listOf(
        Triple(MainTab.HOME, strings.navHome, Pair(Icons.Filled.Home, Icons.Outlined.Home)),
        Triple(MainTab.CREATE, strings.navCreate, Pair(Icons.Filled.MovieCreation, Icons.Outlined.MovieCreation)),
        Triple(MainTab.TOOLS, strings.navTools, Pair(Icons.Filled.Build, Icons.Outlined.Build)),
        Triple(MainTab.LIBRARY, strings.navLibrary, Pair(Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary)),
        Triple(MainTab.PROFILE, strings.navProfile, Pair(Icons.Filled.Person, Icons.Outlined.Person))
    )
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 6.dp
    ) {
        items.forEach { (tab, label, icons) ->
            val selected = activeTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                icon = {
                    Icon(
                        imageVector = if (selected) icons.first else icons.second,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun AspectRatioSelector(
    options: List<AspectRatioOption>,
    selected: AspectRatioOption,
    lang: String,
    onSelect: (AspectRatioOption) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { option ->
            val isSelected = option == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(option) },
                modifier = Modifier.testTag("aspect_${option.apiValue}"),
                label = {
                    Text(
                        text = if (lang == "id") option.labelId else option.labelEn,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun StyleChipRow(
    selected: VideoStyleOption,
    onSelect: (VideoStyleOption) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        items(VideoStyleOption.entries) { style ->
            val isSelected = style == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(style) },
                modifier = Modifier.testTag("style_${style.name.lowercase()}"),
                label = { Text(style.label, style = MaterialTheme.typography.bodySmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}

@Composable
fun GenerationProgressCard(
    progress: GenerationProgress,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = progress.isActive) {
        val animatedPct by animateFloatAsState(
            targetValue = (progress.percent / 100f).coerceIn(0f, 1f),
            label = "gen_progress"
        )
        GlassCard(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = progress.stepLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${progress.percent}% • ~${progress.etaSeconds}s",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan
                    )
                }
                LinearProgressIndicator(
                    progress = { animatedPct },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = ElectricViolet,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun VideoPlayerCard(
    videoFile: File,
    aspectRatio: AspectRatioOption,
    subtitles: List<SubtitleSegment> = emptyList(),
    subtitleStyle: SubtitleStyleOption = SubtitleStyleOption.TIKTOK_BOLD,
    filterOption: VideoFilterOption = VideoFilterOption.ORIGINAL,
    trimRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember(videoFile.absolutePath) { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var durationMs by remember(videoFile.absolutePath) { mutableIntStateOf(1000) }
    var currentPosMs by remember(videoFile.absolutePath) { mutableIntStateOf(0) }
    var sliderRatio by remember(videoFile.absolutePath) { mutableFloatStateOf(0f) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    val ratioFloat = when (aspectRatio) {
        AspectRatioOption.PORTRAIT_9_16 -> 9f / 16f
        AspectRatioOption.SQUARE_1_1 -> 1f
        AspectRatioOption.LANDSCAPE_16_9 -> 16f / 9f
    }

    LaunchedEffect(isPlaying, videoFile.absolutePath, trimRange) {
        while (isPlaying) {
            val vv = videoViewRef
            if (vv != null && vv.isPlaying) {
                val pos = vv.currentPosition
                val dur = vv.duration.coerceAtLeast(1)
                durationMs = dur
                currentPosMs = pos
                val startMs = (dur * trimRange.start).toInt()
                val endMs = (dur * trimRange.endInclusive).toInt().coerceAtLeast(startMs + 500)
                if (pos >= endMs) {
                    vv.seekTo(startMs)
                }
                sliderRatio = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
            }
            delay(200L)
        }
    }

    DisposableEffect(videoFile.absolutePath) {
        onDispose {
            runCatching { videoViewRef?.stopPlayback() }
            mediaPlayerRef = null
        }
    }

    val activeSubtitle = remember(currentPosMs, subtitles) {
        val sec = currentPosMs / 1000f
        subtitles.firstOrNull { sec in it.startSec..it.endSec }
    }

    val tintOverlayColor = when (filterOption) {
        VideoFilterOption.ORIGINAL -> Color.Transparent
        VideoFilterOption.VIVID -> Color(0x2200E5FF)
        VideoFilterOption.CINEMATIC -> Color(0x280D9488)
        VideoFilterOption.NOIR -> Color(0x44000000)
        VideoFilterOption.WARM -> Color(0x2AF59E0B)
    }

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratioFloat)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setVideoURI(Uri.fromFile(videoFile))
                            setOnPreparedListener { mp ->
                                mediaPlayerRef = mp
                                mp.isLooping = true
                                durationMs = mp.duration.coerceAtLeast(1)
                                val vol = if (isMuted) 0f else 1f
                                mp.setVolume(vol, vol)
                                if (isPlaying) start()
                            }
                            videoViewRef = this
                        }
                    },
                    update = { view ->
                        videoViewRef = view
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Color Filter preview tint layer
                if (tintOverlayColor != Color.Transparent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(tintOverlayColor)
                    )
                }

                // Live Karaoke / TikTok subtitle overlay
                if (activeSubtitle != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                            .background(
                                color = when (subtitleStyle) {
                                    SubtitleStyleOption.TIKTOK_BOLD -> Color(0xCC090A12)
                                    SubtitleStyleOption.KARAOKE_GLOW -> Color(0xAA2A1E4D)
                                    SubtitleStyleOption.MINIMAL_CLEAN -> Color(0x88000000)
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        val annotated = buildAnnotatedString {
                            val words = activeSubtitle.text.split(" ")
                            words.forEachIndexed { idx, word ->
                                val isHighlight = activeSubtitle.highlightWord.isNotBlank() &&
                                    word.contains(activeSubtitle.highlightWord, ignoreCase = true)
                                withStyle(
                                    SpanStyle(
                                        color = if (isHighlight) NeonCyan else Color.White,
                                        fontWeight = if (subtitleStyle == SubtitleStyleOption.TIKTOK_BOLD || isHighlight) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Medium
                                        }
                                    )
                                ) {
                                    append(word)
                                }
                                if (idx < words.lastIndex) append(" ")
                            }
                        }
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Play/Pause center badge when paused
                if (!isPlaying) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xAA090A12),
                        modifier = Modifier
                            .size(56.dp)
                            .clickable {
                                videoViewRef?.start()
                                isPlaying = true
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play video",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val vv = videoViewRef
                        if (vv != null) {
                            if (isPlaying) {
                                vv.pause()
                                isPlaying = false
                            } else {
                                vv.start()
                                isPlaying = true
                            }
                        }
                    },
                    modifier = Modifier.testTag("video_play_pause_btn")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }

                Slider(
                    value = sliderRatio,
                    onValueChange = { ratio ->
                        sliderRatio = ratio
                        val seekMs = (durationMs * ratio).toInt()
                        videoViewRef?.seekTo(seekMs)
                    },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = ElectricViolet
                    )
                )

                Text(
                    text = "${currentPosMs / 1000}s/${durationMs / 1000}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        val v = if (isMuted) 0f else 1f
                        runCatching { mediaPlayerRef?.setVolume(v, v) }
                    },
                    modifier = Modifier.testTag("video_mute_btn")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Toggle audio"
                    )
                }
            }
        }
    }
}

@Composable
fun GradientPrimaryButton(
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val brush = if (enabled) {
        Brush.horizontalGradient(listOf(ElectricViolet, Color(0xFF6D28D9), NeonCyan))
    } else {
        Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(brush)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
