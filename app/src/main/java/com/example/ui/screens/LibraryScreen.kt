package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.i18n.StudioStrings
import com.example.media.MediaStorageHelper
import com.example.model.AspectRatioOption
import com.example.model.ProjectType
import com.example.model.StudioProject
import com.example.model.UiState
import com.example.ui.components.GlassCard
import com.example.ui.components.VideoPlayerCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonCyan
import com.example.ui.viewmodel.StudioUiModel
import com.example.ui.viewmodel.StudioViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    ui: StudioUiModel,
    projectsState: UiState<List<StudioProject>>,
    lang: String,
    strings: StudioStrings,
    viewModel: StudioViewModel
) {
    val context = LocalContext.current

    ui.selectedLibraryProject?.let { selected ->
        BackHandler { viewModel.openLibraryProject(null) }
        ModalBottomSheet(
            onDismissRequest = { viewModel.openLibraryProject(null) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(selected.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${selected.projectType} • ${selected.aspectRatio} • ${selected.resolution} • ${selected.style}",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonCyan
                )
                Text(selected.prompt, style = MaterialTheme.typography.bodyMedium)

                val mediaFile = selected.localMediaPath?.let { File(it) }?.takeIf { it.exists() }
                if (mediaFile != null) {
                    if (selected.projectType == ProjectType.TEXT_TO_VIDEO.name ||
                        selected.projectType == ProjectType.IMAGE_TO_VIDEO.name
                    ) {
                        val ratio = if (selected.aspectRatio == "9:16") {
                            AspectRatioOption.PORTRAIT_9_16
                        } else {
                            AspectRatioOption.LANDSCAPE_16_9
                        }
                        VideoPlayerCard(videoFile = mediaFile, aspectRatio = ratio)
                    } else if (selected.projectType == ProjectType.IMAGE_GEN.name ||
                        selected.projectType == ProjectType.IMAGE_EDIT.name
                    ) {
                        val bmp = remember(mediaFile.absolutePath) {
                            BitmapFactory.decodeFile(mediaFile.absolutePath)
                        }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = selected.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.loadProjectIntoStudio(selected) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.openInStudioBtn, style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { viewModel.duplicateProject(selected) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.duplicateBtn, style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (mediaFile != null) {
                    val mime = when (selected.projectType) {
                        ProjectType.VOICEOVER.name -> "audio/wav"
                        ProjectType.IMAGE_GEN.name, ProjectType.IMAGE_EDIT.name -> "image/png"
                        else -> "video/mp4"
                    }
                    OutlinedButton(
                        onClick = { MediaStorageHelper.shareFile(context, mediaFile, mime, strings.shareBtn) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.shareBtn)
                    }
                }

                TextButton(
                    onClick = { viewModel.deleteProject(selected) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.deleteBtn, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    val filterTabs = listOf(
        "ALL" to strings.filterAll,
        "VIDEO" to strings.filterVideos,
        "IMAGE" to strings.filterImages,
        "SCRIPT" to strings.filterScripts
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = ui.librarySearch,
                onValueChange = { viewModel.updateLibrarySearch(it) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = {
                    Text(
                        text = if (lang == "id") "Cari proyek..." else "Search projects...",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library_search_input")
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filterTabs) { (key, label) ->
                    FilterChip(
                        selected = ui.libraryFilter == key,
                        onClick = { viewModel.updateLibraryFilter(key) },
                        modifier = Modifier.testTag("library_filter_${key.lowercase()}"),
                        label = { Text(label) }
                    )
                }
            }

            when (projectsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = projectsState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    val filtered = projectsState.data.filter { item ->
                        val matchesFilter = when (ui.libraryFilter) {
                            "VIDEO" -> item.projectType == ProjectType.TEXT_TO_VIDEO.name || item.projectType == ProjectType.IMAGE_TO_VIDEO.name
                            "IMAGE" -> item.projectType == ProjectType.IMAGE_GEN.name || item.projectType == ProjectType.IMAGE_EDIT.name
                            "SCRIPT" -> item.projectType == ProjectType.SCRIPT.name || item.projectType == ProjectType.VOICEOVER.name
                            else -> true
                        }
                        val q = ui.librarySearch.trim()
                        val matchesQuery = q.isBlank() ||
                            item.title.contains(q, ignoreCase = true) ||
                            item.prompt.contains(q, ignoreCase = true)
                        matchesFilter && matchesQuery
                    }

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VideoLibrary,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(strings.libraryEmptyTitle, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = strings.libraryEmptyHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.id }) { project ->
                                val typeIcon = when (project.projectType) {
                                    ProjectType.IMAGE_GEN.name, ProjectType.IMAGE_EDIT.name -> Icons.Filled.Image
                                    ProjectType.SCRIPT.name -> Icons.Filled.Description
                                    ProjectType.VOICEOVER.name -> Icons.Filled.RecordVoiceOver
                                    else -> Icons.Filled.Movie
                                }
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("project_card_${project.id}"),
                                    onClick = { viewModel.openLibraryProject(project) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(typeIcon, contentDescription = project.projectType, tint = NeonCyan)
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = project.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${project.projectType} • ${project.aspectRatio} • ${project.resolution}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeonCyan
                                            )
                                            Text(
                                                text = project.prompt,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = EmeraldSuccess.copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                text = project.status,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldSuccess,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
    }
}
