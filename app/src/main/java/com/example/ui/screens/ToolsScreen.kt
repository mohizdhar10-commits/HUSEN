package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.i18n.StudioStrings
import com.example.media.MediaStorageHelper
import com.example.model.SubtitleStyleOption
import com.example.model.ToolTab
import com.example.model.VideoFilterOption
import com.example.model.VoiceEmotionOption
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.VideoPlayerCard
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.viewmodel.StudioUiModel
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun ToolsScreen(
    ui: StudioUiModel,
    lang: String,
    strings: StudioStrings,
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val isSpeaking by viewModel.ttsEngine.isSpeaking.collectAsStateWithLifecycle()

    val tabs = listOf(
        ToolTab.SCRIPT_STORYBOARD to strings.toolScriptTitle,
        ToolTab.PROMPT_ENHANCER to strings.toolEnhancerTitle,
        ToolTab.VOICEOVER_TTS to strings.toolVoiceoverTitle,
        ToolTab.SUBTITLE_EDITOR to strings.toolSubtitleEditorTitle
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tabs) { (tab, label) ->
                    FilterChip(
                        selected = ui.activeToolTab == tab,
                        onClick = { viewModel.selectToolTab(tab) },
                        modifier = Modifier.testTag("tool_tab_${tab.name.lowercase()}"),
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            if (ui.generationError != null) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = ui.generationError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            when (ui.activeToolTab) {
                ToolTab.SCRIPT_STORYBOARD -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Description, contentDescription = null, tint = NeonCyan)
                                Text(strings.toolScriptTitle, style = MaterialTheme.typography.titleMedium)
                            }
                            OutlinedTextField(
                                value = ui.scriptTopic,
                                onValueChange = { viewModel.updateScriptTopic(it) },
                                placeholder = { Text(strings.topicHint, style = MaterialTheme.typography.bodySmall) },
                                minLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("script_topic_input")
                            )
                            GradientPrimaryButton(
                                text = if (ui.isGeneratingScript) "..." else strings.generateScriptBtn,
                                enabled = !ui.isGeneratingScript && ui.scriptTopic.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("generate_script_btn"),
                                onClick = { viewModel.generateStoryboardScript() }
                            )
                        }
                    }

                    ui.generatedStoryboard?.let { storyboard ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(storyboard.title, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = "Music Mood: ${storyboard.musicMood}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonCyan
                                )
                                storyboard.scenes.forEach { scene ->
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Scene ${scene.sceneNumber} (${scene.durationSec}s) • ${scene.cameraAngle}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = ElectricViolet
                                            )
                                            Text(
                                                text = scene.visualPrompt,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "\"${scene.narration}\"",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = { viewModel.sendSceneToCreate(scene, asImage = true) },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Image", style = MaterialTheme.typography.labelSmall)
                                                }
                                                Button(
                                                    onClick = { viewModel.sendSceneToCreate(scene, asImage = false) },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(Icons.Filled.Movie, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Video", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                ToolTab.PROMPT_ENHANCER -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = NeonCyan)
                                Text(strings.toolEnhancerTitle, style = MaterialTheme.typography.titleMedium)
                            }
                            OutlinedTextField(
                                value = ui.enhancerInput,
                                onValueChange = { viewModel.updateEnhancerInput(it) },
                                placeholder = {
                                    Text(
                                        text = if (lang == "id") "Ide singkat (cth: kucing astronot di Mars)" else "Short idea (e.g. astronaut cat on Mars)",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                minLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("enhancer_input")
                            )
                            GradientPrimaryButton(
                                text = if (ui.isEnhancingPrompt) strings.enhancingPrompt else strings.enhancePromptBtn,
                                enabled = !ui.isEnhancingPrompt && ui.enhancerInput.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("run_enhancer_btn"),
                                onClick = { viewModel.runStandalonePromptEnhancer() }
                            )

                            if (ui.enhancerOutput.isNotBlank()) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(ui.enhancerOutput, style = MaterialTheme.typography.bodyMedium)
                                        Button(
                                            onClick = { viewModel.useEnhancedPromptInCreate() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(strings.openInStudioBtn)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                ToolTab.VOICEOVER_TTS -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, tint = NeonCyan)
                                Text(strings.toolVoiceoverTitle, style = MaterialTheme.typography.titleMedium)
                            }
                            OutlinedTextField(
                                value = ui.voiceText,
                                onValueChange = { viewModel.updateVoiceText(it) },
                                placeholder = { Text(strings.voiceTextHint, style = MaterialTheme.typography.bodySmall) },
                                minLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("voice_text_input")
                            )
                            Text(strings.languageVoiceLabel, style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = ui.voiceLang == "id",
                                    onClick = { viewModel.updateVoiceLang("id") },
                                    label = { Text("Indonesia (id-ID)") }
                                )
                                FilterChip(
                                    selected = ui.voiceLang == "en",
                                    onClick = { viewModel.updateVoiceLang("en") },
                                    label = { Text("English (en-US)") }
                                )
                            }
                            Text(
                                text = "${strings.speedLabel}: ${"%.1f".format(ui.voiceSpeed)}x",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Slider(
                                value = ui.voiceSpeed,
                                onValueChange = { viewModel.updateVoiceSpeed(it) },
                                valueRange = 0.6f..1.8f
                            )
                            Text(strings.emotionLabel, style = MaterialTheme.typography.titleSmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(VoiceEmotionOption.entries) { emo ->
                                    FilterChip(
                                        selected = ui.voiceEmotion == emo,
                                        onClick = { viewModel.updateVoiceEmotion(emo) },
                                        label = {
                                            Text(if (lang == "id") emo.labelId else emo.labelEn)
                                        }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (isSpeaking) viewModel.stopVoiceover() else viewModel.playVoiceover()
                                    },
                                    enabled = ui.voiceText.isNotBlank(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tts_play_stop_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isSpeaking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isSpeaking) strings.stopSpeakingBtn else strings.speakNowBtn)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.exportVoiceoverWav() },
                                    enabled = ui.voiceText.isNotBlank(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tts_export_wav_btn")
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.exportWavBtn, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            ui.exportedWavFile?.takeIf { it.exists() }?.let { wavFile ->
                                OutlinedButton(
                                    onClick = {
                                        MediaStorageHelper.shareFile(context, wavFile, "audio/wav", strings.shareBtn)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${strings.shareBtn} WAV")
                                }
                            }
                        }
                    }
                }

                ToolTab.SUBTITLE_EDITOR -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Subtitles, contentDescription = null, tint = NeonCyan)
                                Text(strings.toolSubtitleEditorTitle, style = MaterialTheme.typography.titleMedium)
                            }
                            OutlinedTextField(
                                value = ui.subtitleTopic,
                                onValueChange = { viewModel.updateSubtitleTopic(it) },
                                placeholder = { Text(strings.subtitleTopicHint, style = MaterialTheme.typography.bodySmall) },
                                minLines = 2,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("subtitle_topic_input")
                            )
                            Text(strings.subtitleStyleLabel, style = MaterialTheme.typography.titleSmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(SubtitleStyleOption.entries) { styleOpt ->
                                    FilterChip(
                                        selected = ui.subtitleStyle == styleOpt,
                                        onClick = { viewModel.updateSubtitleStyle(styleOpt) },
                                        label = { Text(if (lang == "id") styleOpt.labelId else styleOpt.labelEn) }
                                    )
                                }
                            }
                            Button(
                                onClick = { viewModel.generateAutoSubtitles() },
                                enabled = !ui.isGeneratingSubtitles && ui.subtitleTopic.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("generate_subtitles_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                            ) {
                                if (ui.isGeneratingSubtitles) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(strings.generateSubtitlesBtn)
                                }
                            }

                            if (ui.subtitles.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ui.subtitles.forEach { seg ->
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "${seg.startSec}s - ${seg.endSec}s",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NeonCyan
                                                )
                                                Text(
                                                    text = seg.text,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.weight(1f).padding(start = 10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Text(strings.videoFilterLabel, style = MaterialTheme.typography.titleSmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(VideoFilterOption.entries) { fOpt ->
                                    FilterChip(
                                        selected = ui.selectedFilter == fOpt,
                                        onClick = { viewModel.updateVideoFilter(fOpt) },
                                        label = { Text(if (lang == "id") fOpt.labelId else fOpt.labelEn) }
                                    )
                                }
                            }

                            Text(
                                text = "${strings.trimClipLabel}: ${(ui.trimRange.start * 100).toInt()}% - ${(ui.trimRange.endInclusive * 100).toInt()}%",
                                style = MaterialTheme.typography.titleSmall
                            )
                            RangeSlider(
                                value = ui.trimRange,
                                onValueChange = { viewModel.updateTrimRange(it) }
                            )
                        }
                    }

                    if (ui.generatedVideoFile != null && ui.generatedVideoFile.exists()) {
                        VideoPlayerCard(
                            videoFile = ui.generatedVideoFile,
                            aspectRatio = ui.selectedAspectRatio,
                            subtitles = ui.subtitles,
                            subtitleStyle = ui.subtitleStyle,
                            filterOption = ui.selectedFilter,
                            trimRange = ui.trimRange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
