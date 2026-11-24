package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.i18n.StudioStrings
import com.example.model.CreateMode
import com.example.model.MainTab
import com.example.model.StudioTemplate
import com.example.model.ToolTab
import com.example.model.UserProfile
import com.example.ui.components.CreditBadge
import com.example.ui.components.GlassCard
import com.example.ui.theme.CoralRose
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonCyan

private data class QuickToolItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val tag: String,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    profile: UserProfile?,
    lang: String,
    darkTheme: Boolean,
    strings: StudioStrings,
    templates: List<StudioTemplate>,
    onToggleTheme: (Boolean) -> Unit,
    onToggleLanguage: (String) -> Unit,
    onNavigateTab: (MainTab) -> Unit,
    onOpenQuickTool: (CreateMode?, ToolTab?) -> Unit,
    onSelectTemplate: (StudioTemplate) -> Unit
) {
    val quickTools = listOf(
        QuickToolItem(
            title = strings.modeTextToVideo,
            subtitle = "Omni 1.2 • 16:9 / 9:16",
            icon = Icons.Filled.Movie,
            accent = ElectricViolet,
            tag = "quick_text_to_video",
            onClick = { onOpenQuickTool(CreateMode.TEXT_TO_VIDEO, null) }
        ),
        QuickToolItem(
            title = strings.modeImageToVideo,
            subtitle = if (lang == "id") "Animasi Foto + Kamera" else "Animate Photo + Camera",
            icon = Icons.Filled.AutoAwesome,
            accent = NeonCyan,
            tag = "quick_image_to_video",
            onClick = { onOpenQuickTool(CreateMode.IMAGE_TO_VIDEO, null) }
        ),
        QuickToolItem(
            title = strings.modeImageStudio,
            subtitle = "Nano Banana 2.1",
            icon = Icons.Filled.Image,
            accent = CoralRose,
            tag = "quick_image_studio",
            onClick = { onOpenQuickTool(CreateMode.IMAGE_STUDIO, null) }
        ),
        QuickToolItem(
            title = strings.toolScriptTitle,
            subtitle = if (lang == "id") "Storyboard Multi-Scene" else "Multi-Scene Storyboard",
            icon = Icons.Filled.Description,
            accent = EmeraldSuccess,
            tag = "quick_script",
            onClick = { onOpenQuickTool(null, ToolTab.SCRIPT_STORYBOARD) }
        ),
        QuickToolItem(
            title = strings.toolEnhancerTitle,
            subtitle = "Gemini 3.8 Flash",
            icon = Icons.Filled.AutoFixHigh,
            accent = NeonCyan,
            tag = "quick_enhancer",
            onClick = { onOpenQuickTool(null, ToolTab.PROMPT_ENHANCER) }
        ),
        QuickToolItem(
            title = strings.toolVoiceoverTitle,
            subtitle = "TTS ID & EN + WAV",
            icon = Icons.Filled.RecordVoiceOver,
            accent = ElectricViolet,
            tag = "quick_voiceover",
            onClick = { onOpenQuickTool(null, ToolTab.VOICEOVER_TTS) }
        ),
        QuickToolItem(
            title = strings.toolSubtitleEditorTitle,
            subtitle = if (lang == "id") "TikTok / Karaoke + Trim" else "TikTok / Karaoke + Trim",
            icon = Icons.Filled.Subtitles,
            accent = CoralRose,
            tag = "quick_subtitle",
            onClick = { onOpenQuickTool(null, ToolTab.SUBTITLE_EDITOR) }
        ),
        QuickToolItem(
            title = if (lang == "id") "Editor & Filter" else "Filter & Trim",
            subtitle = if (lang == "id") "Pratinjau & Ekspor MP4" else "Preview & Export MP4",
            icon = Icons.Filled.VideoSettings,
            accent = EmeraldSuccess,
            tag = "quick_editor",
            onClick = { onOpenQuickTool(null, ToolTab.SUBTITLE_EDITOR) }
        )
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.appTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (lang == "id") {
                                "Halo, ${profile?.displayName ?: "Kreator"}"
                            } else {
                                "Welcome, ${profile?.displayName ?: "Creator"}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = { onToggleLanguage(if (lang == "id") "en" else "id") },
                            modifier = Modifier.testTag("home_lang_btn")
                        ) {
                            Text(
                                text = lang.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan
                            )
                        }
                        IconButton(
                            onClick = { onToggleTheme(!darkTheme) },
                            modifier = Modifier.testTag("home_theme_btn")
                        ) {
                            Icon(
                                imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = strings.darkModeLabel
                            )
                        }
                        CreditBadge(
                            credits = profile?.credits ?: 50L,
                            planTier = profile?.planTier ?: "FREE",
                            onClick = { onNavigateTab(MainTab.PROFILE) }
                        )
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .clickable { onNavigateTab(MainTab.CREATE) }
                        .testTag("home_hero_card")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_studio),
                        contentDescription = strings.heroTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xE6090A12), Color(0x66090A12))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = ElectricViolet.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = strings.heroBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = strings.heroTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = strings.heroSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD5DAF2),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Button(
                                onClick = { onNavigateTab(MainTab.CREATE) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = Color(0xFF061B22)
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("home_hero_cta")
                            ) {
                                Text(
                                    text = strings.heroCta,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = strings.quickToolsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(quickTools.chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { tool ->
                        GlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag(tool.tag),
                            onClick = tool.onClick
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = tool.accent.copy(alpha = 0.18f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = tool.icon,
                                            contentDescription = tool.title,
                                            tint = tool.accent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = tool.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tool.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                Text(
                    text = strings.templatesTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(templates) { tpl ->
                        val imageRes = when (tpl.drawableResName) {
                            "img_template_neon_reels" -> R.drawable.img_template_neon_reels
                            "img_hero_studio" -> R.drawable.img_hero_studio
                            else -> R.drawable.img_template_cinematic
                        }
                        GlassCard(
                            modifier = Modifier
                                .width(230.dp)
                                .testTag("template_${tpl.id}"),
                            onClick = { onSelectTemplate(tpl) }
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(124.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = imageRes),
                                        contentDescription = if (lang == "id") tpl.titleId else tpl.titleEn,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = Color(0xBB090A12),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "${tpl.aspectRatio.apiValue} • ${if (lang == "id") tpl.categoryId else tpl.categoryEn}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonCyan,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (lang == "id") tpl.titleId else tpl.titleEn,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tpl.prompt,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = strings.useTemplate,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = NeonCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
