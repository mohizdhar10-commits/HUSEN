package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.i18n.StudioStrings
import com.example.media.MediaStorageHelper
import com.example.model.AspectRatioOption
import com.example.model.CameraMotionOption
import com.example.model.CreateMode
import com.example.model.ResolutionOption
import com.example.ui.components.AspectRatioSelector
import com.example.ui.components.GenerationProgressCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.StyleChipRow
import com.example.ui.components.VideoPlayerCard
import com.example.ui.theme.AmberCredit
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.viewmodel.StudioUiModel
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun CreateScreen(
    ui: StudioUiModel,
    lang: String,
    strings: StudioStrings,
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.attachPhotoFromUri(uri)
        }
    }

    val modes = listOf(
        CreateMode.TEXT_TO_VIDEO to strings.modeTextToVideo,
        CreateMode.IMAGE_TO_VIDEO to strings.modeImageToVideo,
        CreateMode.IMAGE_STUDIO to strings.modeImageStudio
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
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = ui.createMode == mode,
                        onClick = { viewModel.selectCreateMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                        modifier = Modifier.testTag("create_mode_${mode.name.lowercase()}")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }

            if (ui.createMode == CreateMode.IMAGE_TO_VIDEO || ui.createMode == CreateMode.IMAGE_STUDIO) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (ui.attachedBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    bitmap = ui.attachedBitmap.asImageBitmap(),
                                    contentDescription = "Selected photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { viewModel.clearAttachedPhoto() },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                            RoundedCornerShape(999.dp)
                                        )
                                        .testTag("clear_photo_btn")
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = strings.clearPhotoBtn)
                                }
                            }
                            if (ui.createMode == CreateMode.IMAGE_STUDIO) {
                                Text(
                                    text = strings.editModeNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonCyan
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pick_photo_btn")
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (ui.attachedBitmap == null) strings.pickPhotoBtn else strings.changePhotoBtn)
                        }
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.promptLabel,
                            style = MaterialTheme.typography.titleSmall
                        )
                        TextButton(
                            onClick = { viewModel.enhanceCurrentPrompt() },
                            enabled = !ui.isEnhancingPrompt && ui.promptText.isNotBlank(),
                            modifier = Modifier.testTag("enhance_prompt_btn")
                        ) {
                            if (ui.isEnhancingPrompt) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.enhancingPrompt, style = MaterialTheme.typography.labelSmall)
                            } else {
                                Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.enhancePromptBtn, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = ui.promptText,
                        onValueChange = { viewModel.updatePrompt(it) },
                        placeholder = {
                            Text(
                                text = if (ui.createMode == CreateMode.IMAGE_STUDIO) strings.promptHintImage else strings.promptHintVideo,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_prompt_input")
                    )

                    Text(text = strings.styleLabel, style = MaterialTheme.typography.titleSmall)
                    StyleChipRow(
                        selected = ui.selectedStyle,
                        onSelect = { viewModel.updateStyle(it) }
                    )

                    if (ui.createMode == CreateMode.IMAGE_TO_VIDEO) {
                        Text(text = strings.cameraMotionLabel, style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CameraMotionOption.entries.take(3).forEach { motion ->
                                FilterChip(
                                    selected = ui.selectedCameraMotion == motion,
                                    onClick = { viewModel.updateCameraMotion(motion) },
                                    label = {
                                        Text(
                                            text = if (lang == "id") motion.labelId else motion.labelEn,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Text(text = strings.aspectRatioLabel, style = MaterialTheme.typography.titleSmall)
                    val ratioOptions = if (ui.createMode == CreateMode.IMAGE_STUDIO) {
                        AspectRatioOption.entries
                    } else {
                        listOf(AspectRatioOption.LANDSCAPE_16_9, AspectRatioOption.PORTRAIT_9_16)
                    }
                    AspectRatioSelector(
                        options = ratioOptions,
                        selected = ui.selectedAspectRatio,
                        lang = lang,
                        onSelect = { viewModel.updateAspectRatio(it) }
                    )

                    if (ui.createMode != CreateMode.IMAGE_STUDIO) {
                        Text(text = strings.resolutionLabel, style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ResolutionOption.entries.forEach { res ->
                                FilterChip(
                                    selected = ui.selectedResolution == res,
                                    onClick = { viewModel.updateResolution(res) },
                                    modifier = Modifier.testTag("res_${res.apiValue}"),
                                    label = {
                                        Text(
                                            text = "${res.apiValue} (${res.creditCost}⚡)",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }
                    }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.estimatedCost}: ${viewModel.estimatedCreditCost()} ${strings.creditsLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberCredit
                )
                Text(
                    text = if (ui.createMode == CreateMode.IMAGE_STUDIO) "gemini-nano-banana-2.1" else "gemini-omni-1.2-flash",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            GradientPrimaryButton(
                text = when (ui.createMode) {
                    CreateMode.IMAGE_STUDIO -> if (ui.attachedBitmap != null) strings.editImageBtn else strings.generateImageBtn
                    else -> strings.generateVideoBtn
                },
                enabled = !ui.progress.isActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generate_primary_btn"),
                onClick = { viewModel.startCreateGeneration() }
            )

            GenerationProgressCard(progress = ui.progress)

            // Video output preview + multi-turn conversational edit + export/share
            if (ui.createMode != CreateMode.IMAGE_STUDIO && ui.generatedVideoFile != null && ui.generatedVideoFile.exists()) {
                VideoPlayerCard(
                    videoFile = ui.generatedVideoFile,
                    aspectRatio = ui.selectedAspectRatio,
                    subtitles = ui.subtitles,
                    subtitleStyle = ui.subtitleStyle,
                    filterOption = ui.selectedFilter,
                    trimRange = ui.trimRange
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.saveCurrentMediaToGallery(isVideo = true) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_video_btn")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.saveToGalleryBtn, style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = {
                            MediaStorageHelper.shareFile(
                                context = context,
                                file = ui.generatedVideoFile,
                                mimeType = "video/mp4",
                                shareTitle = strings.shareBtn
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_video_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.shareBtn, style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (!ui.activeInteractionId.isNullOrBlank()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = ui.followUpEditText,
                                onValueChange = { viewModel.updateFollowUpEditText(it) },
                                placeholder = { Text(strings.followUpEditHint, style = MaterialTheme.typography.bodySmall) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("followup_video_edit_input")
                            )
                            Button(
                                onClick = { viewModel.applyFollowUpVideoEdit() },
                                enabled = ui.followUpEditText.isNotBlank() && !ui.progress.isActive,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("apply_video_edit_btn")
                            ) {
                                Icon(Icons.Filled.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.applyVideoEditBtn)
                            }
                        }
                    }
                }
            }

            // Image output preview + one-tap Animate to Video + export/share
            if (ui.createMode == CreateMode.IMAGE_STUDIO && ui.generatedImageBitmap != null) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val imgRatio = when (ui.selectedAspectRatio) {
                            AspectRatioOption.PORTRAIT_9_16 -> 9f / 16f
                            AspectRatioOption.SQUARE_1_1 -> 1f
                            AspectRatioOption.LANDSCAPE_16_9 -> 16f / 9f
                        }
                        Image(
                            bitmap = ui.generatedImageBitmap.asImageBitmap(),
                            contentDescription = "Generated AI Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(imgRatio)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.saveCurrentMediaToGallery(isVideo = false) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.saveToGalleryBtn, style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    ui.generatedImageFile?.let { file ->
                                        MediaStorageHelper.shareFile(context, file, "image/png", strings.shareBtn)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.shareBtn, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Button(
                            onClick = { viewModel.animateGeneratedImageToVideo() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("animate_generated_image_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = androidx.compose.ui.graphics.Color(0xFF061B22)
                            )
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.animateToVideoBtn)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
