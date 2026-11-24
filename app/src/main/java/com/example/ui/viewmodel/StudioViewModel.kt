package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ai.AiContentProvider
import com.example.data.ai.GeminiStudioProvider
import com.example.data.firebase.StudioRepository
import com.example.i18n.stringsFor
import com.example.media.MediaStorageHelper
import com.example.media.VoiceoverTtsEngine
import com.example.model.AspectRatioOption
import com.example.model.CameraMotionOption
import com.example.model.CreateMode
import com.example.model.GenerationProgress
import com.example.model.MainTab
import com.example.model.ProjectStatus
import com.example.model.ProjectType
import com.example.model.ResolutionOption
import com.example.model.StoryboardScene
import com.example.model.StoryboardScript
import com.example.model.StudioProject
import com.example.model.StudioTemplate
import com.example.model.SubtitleSegment
import com.example.model.SubtitleStyleOption
import com.example.model.ToolTab
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.VideoFilterOption
import com.example.model.VideoStyleOption
import com.example.model.VoiceEmotionOption
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class StudioUiModel(
    val activeTab: MainTab = MainTab.HOME,
    val createMode: CreateMode = CreateMode.TEXT_TO_VIDEO,
    val activeToolTab: ToolTab = ToolTab.SCRIPT_STORYBOARD,
    val promptText: String = "",
    val selectedStyle: VideoStyleOption = VideoStyleOption.CINEMATIC,
    val selectedAspectRatio: AspectRatioOption = AspectRatioOption.LANDSCAPE_16_9,
    val selectedResolution: ResolutionOption = ResolutionOption.RES_720P,
    val selectedCameraMotion: CameraMotionOption = CameraMotionOption.AUTO,
    val attachedBitmap: Bitmap? = null,
    val generatedVideoFile: File? = null,
    val generatedImageBitmap: Bitmap? = null,
    val generatedImageFile: File? = null,
    val activeInteractionId: String? = null,
    val activeProjectId: String? = null,
    val followUpEditText: String = "",
    val progress: GenerationProgress = GenerationProgress(),
    val isEnhancingPrompt: Boolean = false,
    val generationError: String? = null,
    val snackbarMessage: String? = null,
    // Tools state
    val scriptTopic: String = "",
    val isGeneratingScript: Boolean = false,
    val generatedStoryboard: StoryboardScript? = null,
    val enhancerInput: String = "",
    val enhancerOutput: String = "",
    val voiceText: String = "",
    val voiceLang: String = "id",
    val voiceSpeed: Float = 1.0f,
    val voiceEmotion: VoiceEmotionOption = VoiceEmotionOption.NEUTRAL,
    val exportedWavFile: File? = null,
    val subtitleTopic: String = "",
    val isGeneratingSubtitles: Boolean = false,
    val subtitleStyle: SubtitleStyleOption = SubtitleStyleOption.TIKTOK_BOLD,
    val subtitles: List<SubtitleSegment> = emptyList(),
    val selectedFilter: VideoFilterOption = VideoFilterOption.ORIGINAL,
    val trimRange: ClosedFloatingPointRange<Float> = 0f..1f,
    val librarySearch: String = "",
    val libraryFilter: String = "ALL",
    val selectedLibraryProject: StudioProject? = null
)

class StudioViewModel(
    private val appContext: Context,
    private val currentUserId: String,
    private val repository: StudioRepository,
    private val aiProvider: AiContentProvider,
    val ttsEngine: VoiceoverTtsEngine
) : ViewModel() {

    companion object {
        private const val TAG = "StudioViewModel"
        private const val STOP_TIMEOUT_MS = 5000L

        fun provideFactory(
            context: Context,
            currentUserId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appCtx = context.applicationContext
                StudioViewModel(
                    appContext = appCtx,
                    currentUserId = currentUserId,
                    repository = StudioRepository(appCtx),
                    aiProvider = GeminiStudioProvider(appCtx),
                    ttsEngine = VoiceoverTtsEngine(appCtx)
                )
            }
        }
    }

    private val _ui = MutableStateFlow(StudioUiModel())
    val ui: StateFlow<StudioUiModel> = _ui.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Profile stream error", root)
            emit(UiState.Error(root.message ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val projectsState: StateFlow<UiState<List<StudioProject>>> = repository.observeProjects(currentUserId)
        .map<List<StudioProject>, UiState<List<StudioProject>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Projects stream error", root)
            emit(UiState.Error(root.message ?: "Failed to load projects"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val templates: List<StudioTemplate> = listOf(
        StudioTemplate(
            id = "tpl_cinematic_drone",
            titleId = "Sinematik Alam Drone",
            titleEn = "Cinematic Drone Nature",
            categoryId = "YouTube Intro",
            categoryEn = "YouTube Intro",
            aspectRatio = AspectRatioOption.LANDSCAPE_16_9,
            style = VideoStyleOption.CINEMATIC,
            prompt = "Sweeping aerial drone shot gliding over emerald misty pine mountains at golden sunrise with volumetric sunbeams",
            mode = CreateMode.TEXT_TO_VIDEO,
            drawableResName = "img_template_cinematic"
        ),
        StudioTemplate(
            id = "tpl_neon_reels",
            titleId = "Neon Cyberpunk Reels",
            titleEn = "Neon Cyberpunk Reels",
            categoryId = "Reels / TikTok",
            categoryEn = "Reels / TikTok",
            aspectRatio = AspectRatioOption.PORTRAIT_9_16,
            style = VideoStyleOption.CINEMATIC,
            prompt = "Vertical cinematic tracking shot down a futuristic neon cyberpunk street after rain with glowing cyan and violet reflections",
            mode = CreateMode.TEXT_TO_VIDEO,
            drawableResName = "img_template_neon_reels"
        ),
        StudioTemplate(
            id = "tpl_product_promo",
            titleId = "Promo Produk Mewah",
            titleEn = "Luxury Product Promo",
            categoryId = "Iklan Produk",
            categoryEn = "Product Promo",
            aspectRatio = AspectRatioOption.SQUARE_1_1,
            style = VideoStyleOption.REALISTIC,
            prompt = "Sleek matte black flagship wireless headphones floating over a dark obsidian pedestal with dramatic cyan and violet rim lighting",
            mode = CreateMode.IMAGE_STUDIO,
            drawableResName = "img_hero_studio"
        ),
        StudioTemplate(
            id = "tpl_anime_story",
            titleId = "Animasi Anime Twilight",
            titleEn = "Anime Twilight Story",
            categoryId = "Animasi",
            categoryEn = "Animation",
            aspectRatio = AspectRatioOption.LANDSCAPE_16_9,
            style = VideoStyleOption.ANIME,
            prompt = "Breathtaking anime sky islands floating above golden clouds at twilight with wind-swept grass and gentle glowing particles",
            mode = CreateMode.TEXT_TO_VIDEO,
            drawableResName = "img_template_cinematic"
        )
    )

    private var activeGenerationJob: Job? = null

    init {
        viewModelScope.launch {
            Firebase.auth.currentUser?.let { user ->
                repository.ensureUserProfile(user)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsEngine.release()
    }

    private fun currentLang(): String {
        val state = userProfileState.value
        return if (state is UiState.Success) state.data?.preferredLanguage ?: "id" else "id"
    }

    private fun currentCredits(): Long {
        val state = userProfileState.value
        return if (state is UiState.Success) state.data?.credits ?: 0L else 50L
    }

    private fun currentPlanTier(): String {
        val state = userProfileState.value
        return if (state is UiState.Success) state.data?.planTier ?: "FREE" else "FREE"
    }

    fun selectTab(tab: MainTab) {
        _ui.update { it.copy(activeTab = tab) }
    }

    fun selectCreateMode(mode: CreateMode) {
        _ui.update { state ->
            val adjustedAspect = if (mode != CreateMode.IMAGE_STUDIO && state.selectedAspectRatio == AspectRatioOption.SQUARE_1_1) {
                AspectRatioOption.LANDSCAPE_16_9
            } else {
                state.selectedAspectRatio
            }
            state.copy(createMode = mode, selectedAspectRatio = adjustedAspect, generationError = null)
        }
    }

    fun selectToolTab(toolTab: ToolTab) {
        _ui.update { it.copy(activeToolTab = toolTab) }
    }

    fun updatePrompt(value: String) = _ui.update { it.copy(promptText = value, generationError = null) }
    fun updateStyle(style: VideoStyleOption) = _ui.update { it.copy(selectedStyle = style) }
    fun updateAspectRatio(ratio: AspectRatioOption) = _ui.update { it.copy(selectedAspectRatio = ratio) }
    fun updateResolution(res: ResolutionOption) = _ui.update { it.copy(selectedResolution = res) }
    fun updateCameraMotion(motion: CameraMotionOption) = _ui.update { it.copy(selectedCameraMotion = motion) }
    fun updateFollowUpEditText(value: String) = _ui.update { it.copy(followUpEditText = value) }
    fun clearError() = _ui.update { it.copy(generationError = null) }
    fun clearSnackbar() = _ui.update { it.copy(snackbarMessage = null) }
    fun setSnackbar(msg: String) = _ui.update { it.copy(snackbarMessage = msg) }

    fun applyTemplate(template: StudioTemplate) {
        _ui.update {
            it.copy(
                activeTab = MainTab.CREATE,
                createMode = template.mode,
                promptText = template.prompt,
                selectedStyle = template.style,
                selectedAspectRatio = template.aspectRatio,
                generationError = null
            )
        }
    }

    fun openQuickTool(mode: CreateMode? = null, toolTab: ToolTab? = null) {
        if (mode != null) {
            selectCreateMode(mode)
            _ui.update { it.copy(activeTab = MainTab.CREATE) }
        } else if (toolTab != null) {
            _ui.update { it.copy(activeTab = MainTab.TOOLS, activeToolTab = toolTab) }
        }
    }

    fun attachPhotoFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val bitmap = MediaStorageHelper.decodeScaledBitmap(appContext, uri)
                _ui.update { it.copy(attachedBitmap = bitmap, generationError = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(generationError = e.localizedMessage ?: "Failed to decode photo.") }
            }
        }
    }

    fun clearAttachedPhoto() {
        _ui.update { it.copy(attachedBitmap = null) }
    }

    fun animateGeneratedImageToVideo() {
        val bitmap = _ui.value.generatedImageBitmap ?: return
        _ui.update {
            val safeAspect = if (it.selectedAspectRatio == AspectRatioOption.PORTRAIT_9_16) {
                AspectRatioOption.PORTRAIT_9_16
            } else {
                AspectRatioOption.LANDSCAPE_16_9
            }
            it.copy(
                createMode = CreateMode.IMAGE_TO_VIDEO,
                attachedBitmap = bitmap,
                selectedAspectRatio = safeAspect,
                generationError = null
            )
        }
    }

    fun estimatedCreditCost(): Int {
        val state = _ui.value
        return when (state.createMode) {
            CreateMode.IMAGE_STUDIO -> 3
            CreateMode.TEXT_TO_VIDEO, CreateMode.IMAGE_TO_VIDEO -> state.selectedResolution.creditCost
        }
    }

    fun enhanceCurrentPrompt() {
        val raw = _ui.value.promptText.trim()
        if (raw.isBlank()) return
        viewModelScope.launch {
            _ui.update { it.copy(isEnhancingPrompt = true, generationError = null) }
            try {
                val enhanced = aiProvider.enhancePrompt(
                    prompt = raw,
                    style = _ui.value.selectedStyle.label,
                    language = currentLang()
                )
                _ui.update { it.copy(promptText = enhanced, isEnhancingPrompt = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Enhance prompt failed", e)
                _ui.update {
                    it.copy(
                        isEnhancingPrompt = false,
                        generationError = e.localizedMessage ?: "Failed to enhance prompt."
                    )
                }
            }
        }
    }

    fun startCreateGeneration() {
        val state = _ui.value
        val strings = stringsFor(currentLang())
        val prompt = state.promptText.trim()

        if (state.createMode == CreateMode.IMAGE_TO_VIDEO && state.attachedBitmap == null) {
            _ui.update {
                it.copy(
                    generationError = if (currentLang() == "id") {
                        "Pilih foto terlebih dahulu untuk mode Gambar ke Video."
                    } else {
                        "Please select a photo first for Image-to-Video mode."
                    }
                )
            }
            return
        }
        if (state.createMode != CreateMode.IMAGE_TO_VIDEO && prompt.isBlank()) {
            _ui.update {
                it.copy(
                    generationError = if (currentLang() == "id") {
                        "Masukkan prompt deskripsi terlebih dahulu."
                    } else {
                        "Please enter a prompt description first."
                    }
                )
            }
            return
        }

        val cost = estimatedCreditCost()
        if (currentPlanTier() != "UNLIMITED" && currentCredits() < cost) {
            _ui.update { it.copy(generationError = strings.insufficientCreditsMsg) }
            return
        }

        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            val effectivePrompt = prompt.ifBlank { state.selectedCameraMotion.instruction }
            _ui.update {
                it.copy(
                    generationError = null,
                    progress = GenerationProgress(
                        isActive = true,
                        stepLabel = if (currentLang() == "id") "Memeriksa keamanan konten..." else "Running safety check...",
                        percent = 12,
                        etaSeconds = 35
                    )
                )
            }

            try {
                val moderation = aiProvider.moderatePrompt(effectivePrompt)
                if (!moderation.allowed) {
                    _ui.update {
                        it.copy(
                            progress = GenerationProgress(),
                            generationError = "${strings.moderationBlockedMsg} (${moderation.reason})"
                        )
                    }
                    return@launch
                }

                val pType = when (state.createMode) {
                    CreateMode.TEXT_TO_VIDEO -> ProjectType.TEXT_TO_VIDEO
                    CreateMode.IMAGE_TO_VIDEO -> ProjectType.IMAGE_TO_VIDEO
                    CreateMode.IMAGE_STUDIO -> if (state.attachedBitmap != null) ProjectType.IMAGE_EDIT else ProjectType.IMAGE_GEN
                }
                val aspectForProject = if (state.createMode == CreateMode.IMAGE_STUDIO) {
                    state.selectedAspectRatio.apiValue
                } else {
                    if (state.selectedAspectRatio == AspectRatioOption.PORTRAIT_9_16) "9:16" else "16:9"
                }

                val newProject = StudioProject(
                    userId = currentUserId,
                    title = effectivePrompt.take(48),
                    projectType = pType.name,
                    prompt = effectivePrompt,
                    style = state.selectedStyle.label,
                    aspectRatio = aspectForProject,
                    resolution = if (state.createMode == CreateMode.IMAGE_STUDIO) "1K" else state.selectedResolution.apiValue,
                    status = ProjectStatus.PROCESSING.name,
                    creditCost = cost.toLong()
                )
                val projectId = repository.createProject(newProject).getOrNull()

                if (state.createMode == CreateMode.IMAGE_STUDIO) {
                    _ui.update {
                        it.copy(
                            progress = GenerationProgress(
                                isActive = true,
                                stepLabel = if (currentLang() == "id") "Merender gambar via Nano Banana 2.1..." else "Rendering via Nano Banana 2.1...",
                                percent = 55,
                                etaSeconds = 18
                            )
                        )
                    }
                    val resultBitmap = aiProvider.generateOrEditImage(
                        prompt = "$effectivePrompt, ${state.selectedStyle.promptSuffix}",
                        inputBitmap = state.attachedBitmap,
                        aspectRatio = state.selectedAspectRatio
                    )
                    val savedFile = MediaStorageHelper.saveBitmapLocally(appContext, resultBitmap, "nano_banana")
                    if (currentPlanTier() != "UNLIMITED") {
                        repository.spendCredits(currentUserId, cost)
                    }
                    if (projectId != null) {
                        repository.completeProject(currentUserId, projectId, savedFile.absolutePath)
                    }
                    _ui.update {
                        it.copy(
                            generatedImageBitmap = resultBitmap,
                            generatedImageFile = savedFile,
                            activeProjectId = projectId,
                            progress = GenerationProgress(),
                            snackbarMessage = if (currentLang() == "id") "Gambar AI selesai dibuat!" else "AI image created!"
                        )
                    }
                } else {
                    val tickerJob = launch {
                        val stages = if (currentLang() == "id") {
                            listOf(
                                28 to "Mengirim antrean ke gemini-omni-1.2-flash...",
                                48 to "Menyusun frame & gerakan kamera...",
                                68 to "Merender video & sinkronisasi audio...",
                                86 to "Mengunduh MP4 ke studio..."
                            )
                        } else {
                            listOf(
                                28 to "Dispatching job to gemini-omni-1.2-flash...",
                                48 to "Synthesizing frames & camera motion...",
                                68 to "Rendering video & synchronized audio...",
                                86 to "Downloading MP4 to studio..."
                            )
                        }
                        var eta = 32
                        for ((pct, msg) in stages) {
                            delay(4500L)
                            eta = (eta - 7).coerceAtLeast(4)
                            _ui.update { st ->
                                st.copy(progress = GenerationProgress(true, msg, pct, eta))
                            }
                        }
                    }

                    val outputFile = MediaStorageHelper.newVideoOutputFile(appContext, "omni_vid")
                    val vidResult = try {
                        if (state.createMode == CreateMode.TEXT_TO_VIDEO) {
                            aiProvider.generateTextToVideo(
                                prompt = effectivePrompt,
                                style = state.selectedStyle.promptSuffix,
                                aspectRatio = state.selectedAspectRatio,
                                resolution = state.selectedResolution.apiValue,
                                outputFile = outputFile
                            )
                        } else {
                            val motionText = buildString {
                                append(state.selectedCameraMotion.instruction)
                                if (prompt.isNotBlank()) append(". $prompt")
                                append(". Style: ${state.selectedStyle.promptSuffix}")
                            }
                            aiProvider.generateImageToVideo(
                                imageBitmap = state.attachedBitmap!!,
                                motionInstruction = motionText,
                                aspectRatio = state.selectedAspectRatio,
                                resolution = state.selectedResolution.apiValue,
                                outputFile = outputFile
                            )
                        }
                    } finally {
                        tickerJob.cancel()
                    }

                    if (currentPlanTier() != "UNLIMITED") {
                        repository.spendCredits(currentUserId, cost)
                    }
                    if (projectId != null) {
                        repository.completeProject(
                            userId = currentUserId,
                            projectId = projectId,
                            localMediaPath = vidResult.localVideoFile.absolutePath,
                            interactionId = vidResult.interactionId
                        )
                    }
                    _ui.update {
                        it.copy(
                            generatedVideoFile = vidResult.localVideoFile,
                            activeInteractionId = vidResult.interactionId,
                            activeProjectId = projectId,
                            progress = GenerationProgress(),
                            snackbarMessage = if (currentLang() == "id") "Video AI selesai dirender!" else "AI video rendered!"
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Generation timed out", e)
                _ui.update {
                    it.copy(
                        progress = GenerationProgress(),
                        generationError = if (currentLang() == "id") {
                            "Waktu proses habis. Coba ulangi atau gunakan resolusi 720p."
                        } else {
                            "Generation timed out. Please retry."
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Generation failed", e)
                _ui.update {
                    it.copy(
                        progress = GenerationProgress(),
                        generationError = e.localizedMessage ?: "Generation failed."
                    )
                }
            }
        }
    }

    fun applyFollowUpVideoEdit() {
        val state = _ui.value
        val interactionId = state.activeInteractionId
        val editPrompt = state.followUpEditText.trim()
        if (interactionId.isNullOrBlank() || editPrompt.isBlank()) return

        viewModelScope.launch {
            _ui.update {
                it.copy(
                    generationError = null,
                    progress = GenerationProgress(
                        isActive = true,
                        stepLabel = if (currentLang() == "id") "Mengedit video via Interactions API..." else "Editing video via Interactions API...",
                        percent = 45,
                        etaSeconds = 25
                    )
                )
            }
            try {
                val outFile = MediaStorageHelper.newVideoOutputFile(appContext, "omni_edit")
                val res = aiProvider.editVideoWithInteraction(
                    previousInteractionId = interactionId,
                    editInstruction = editPrompt,
                    aspectRatio = state.selectedAspectRatio,
                    outputFile = outFile
                )
                state.activeProjectId?.let { pid ->
                    repository.updateProjectMediaAndInteraction(
                        currentUserId,
                        pid,
                        res.localVideoFile.absolutePath,
                        res.interactionId
                    )
                }
                _ui.update {
                    it.copy(
                        generatedVideoFile = res.localVideoFile,
                        activeInteractionId = res.interactionId ?: interactionId,
                        followUpEditText = "",
                        progress = GenerationProgress(),
                        snackbarMessage = if (currentLang() == "id") "Video berhasil diedit!" else "Video edited!"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Follow-up edit failed", e)
                _ui.update {
                    it.copy(
                        progress = GenerationProgress(),
                        generationError = e.localizedMessage ?: "Video edit failed."
                    )
                }
            }
        }
    }

    fun saveCurrentMediaToGallery(isVideo: Boolean) {
        val state = _ui.value
        val strings = stringsFor(currentLang())
        viewModelScope.launch {
            val result = if (isVideo) {
                val file = state.generatedVideoFile ?: return@launch
                MediaStorageHelper.exportVideoToGallery(appContext, file, state.promptText.ifBlank { "vidgenai_video" })
            } else {
                val file = state.generatedImageFile ?: return@launch
                MediaStorageHelper.exportImageToGallery(appContext, file, state.promptText.ifBlank { "vidgenai_image" })
            }
            _ui.update {
                it.copy(
                    snackbarMessage = if (result.isSuccess) strings.savedSuccessMsg
                    else result.exceptionOrNull()?.localizedMessage ?: "Export failed."
                )
            }
        }
    }

    // --- Tools Tab Actions ---
    fun updateScriptTopic(v: String) = _ui.update { it.copy(scriptTopic = v) }
    fun updateEnhancerInput(v: String) = _ui.update { it.copy(enhancerInput = v) }
    fun updateVoiceText(v: String) = _ui.update { it.copy(voiceText = v) }
    fun updateVoiceLang(v: String) = _ui.update { it.copy(voiceLang = v) }
    fun updateVoiceSpeed(v: Float) = _ui.update { it.copy(voiceSpeed = v) }
    fun updateVoiceEmotion(v: VoiceEmotionOption) = _ui.update { it.copy(voiceEmotion = v) }
    fun updateSubtitleTopic(v: String) = _ui.update { it.copy(subtitleTopic = v) }
    fun updateSubtitleStyle(v: SubtitleStyleOption) = _ui.update { it.copy(subtitleStyle = v) }
    fun updateVideoFilter(v: VideoFilterOption) = _ui.update { it.copy(selectedFilter = v) }
    fun updateTrimRange(v: ClosedFloatingPointRange<Float>) = _ui.update { it.copy(trimRange = v) }

    fun generateStoryboardScript() {
        val topic = _ui.value.scriptTopic.trim()
        if (topic.isBlank()) return
        viewModelScope.launch {
            _ui.update { it.copy(isGeneratingScript = true, generationError = null) }
            try {
                val script = aiProvider.generateStoryboard(
                    topic = topic,
                    style = _ui.value.selectedStyle.label,
                    aspectRatio = _ui.value.selectedAspectRatio.apiValue,
                    language = currentLang()
                )
                val scriptJson = JSONObject().apply {
                    put("title", script.title)
                    put("musicMood", script.musicMood)
                    put("scenes", JSONArray().apply {
                        script.scenes.forEach { s ->
                            put(JSONObject().apply {
                                put("sceneNumber", s.sceneNumber)
                                put("durationSec", s.durationSec)
                                put("visualPrompt", s.visualPrompt)
                                put("narration", s.narration)
                                put("cameraAngle", s.cameraAngle)
                            })
                        }
                    })
                }.toString()

                repository.createProject(
                    StudioProject(
                        userId = currentUserId,
                        title = script.title.take(120),
                        projectType = ProjectType.SCRIPT.name,
                        prompt = topic,
                        style = _ui.value.selectedStyle.label,
                        aspectRatio = _ui.value.selectedAspectRatio.apiValue,
                        resolution = "720p",
                        status = ProjectStatus.COMPLETED.name,
                        creditCost = 1L,
                        scriptJson = scriptJson
                    )
                )
                _ui.update {
                    it.copy(
                        generatedStoryboard = script,
                        isGeneratingScript = false,
                        voiceText = script.scenes.joinToString(" ") { s -> s.narration },
                        subtitleTopic = script.scenes.joinToString(" ") { s -> s.narration },
                        snackbarMessage = if (currentLang() == "id") "Naskah & storyboard selesai!" else "Storyboard generated!"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Storyboard generation failed", e)
                _ui.update {
                    it.copy(
                        isGeneratingScript = false,
                        generationError = e.localizedMessage ?: "Storyboard generation failed."
                    )
                }
            }
        }
    }

    fun sendSceneToCreate(scene: StoryboardScene, asImage: Boolean) {
        _ui.update {
            it.copy(
                activeTab = MainTab.CREATE,
                createMode = if (asImage) CreateMode.IMAGE_STUDIO else CreateMode.TEXT_TO_VIDEO,
                promptText = "${scene.visualPrompt}. Camera: ${scene.cameraAngle}",
                generationError = null
            )
        }
    }

    fun runStandalonePromptEnhancer() {
        val raw = _ui.value.enhancerInput.trim()
        if (raw.isBlank()) return
        viewModelScope.launch {
            _ui.update { it.copy(isEnhancingPrompt = true, generationError = null) }
            try {
                val out = aiProvider.enhancePrompt(raw, _ui.value.selectedStyle.label, currentLang())
                _ui.update { it.copy(enhancerOutput = out, isEnhancingPrompt = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        isEnhancingPrompt = false,
                        generationError = e.localizedMessage ?: "Prompt enhancer failed."
                    )
                }
            }
        }
    }

    fun useEnhancedPromptInCreate() {
        val out = _ui.value.enhancerOutput
        if (out.isBlank()) return
        _ui.update {
            it.copy(
                activeTab = MainTab.CREATE,
                createMode = CreateMode.TEXT_TO_VIDEO,
                promptText = out
            )
        }
    }

    fun playVoiceover() {
        val state = _ui.value
        ttsEngine.speak(
            text = state.voiceText,
            languageCode = state.voiceLang,
            speed = state.voiceSpeed,
            pitch = state.voiceEmotion.pitch
        )
    }

    fun stopVoiceover() = ttsEngine.stop()

    fun exportVoiceoverWav() {
        val state = _ui.value
        if (state.voiceText.isBlank()) return
        val file = MediaStorageHelper.newAudioOutputFile(appContext, "voiceover")
        ttsEngine.exportToWav(
            text = state.voiceText,
            languageCode = state.voiceLang,
            speed = state.voiceSpeed,
            pitch = state.voiceEmotion.pitch,
            outputFile = file
        ) { success ->
            viewModelScope.launch {
                if (success) {
                    repository.createProject(
                        StudioProject(
                            userId = currentUserId,
                            title = state.voiceText.take(48),
                            projectType = ProjectType.VOICEOVER.name,
                            prompt = state.voiceText,
                            style = state.voiceEmotion.labelId,
                            aspectRatio = "16:9",
                            resolution = "720p",
                            status = ProjectStatus.COMPLETED.name,
                            creditCost = 0L,
                            localMediaPath = file.absolutePath
                        )
                    )
                }
                _ui.update {
                    it.copy(
                        exportedWavFile = if (success) file else null,
                        snackbarMessage = if (success) {
                            if (currentLang() == "id") "Voiceover WAV berhasil dibuat!" else "Voiceover WAV exported!"
                        } else {
                            if (currentLang() == "id") "Gagal mengekspor audio TTS." else "Failed to export TTS audio."
                        }
                    )
                }
            }
        }
    }

    fun generateAutoSubtitles() {
        val topic = _ui.value.subtitleTopic.trim()
        if (topic.isBlank()) return
        viewModelScope.launch {
            _ui.update { it.copy(isGeneratingSubtitles = true, generationError = null) }
            try {
                val segs = aiProvider.generateSubtitles(topic, currentLang())
                _ui.value.activeProjectId?.let { pid ->
                    val arr = JSONArray().apply {
                        segs.forEach { s ->
                            put(JSONObject().apply {
                                put("startSec", s.startSec)
                                put("endSec", s.endSec)
                                put("text", s.text)
                                put("highlightWord", s.highlightWord)
                            })
                        }
                    }.toString()
                    repository.updateProjectSubtitles(currentUserId, pid, arr)
                }
                _ui.update {
                    it.copy(
                        subtitles = segs,
                        isGeneratingSubtitles = false,
                        snackbarMessage = if (currentLang() == "id") "Subtitle otomatis dibuat!" else "Auto subtitles ready!"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        isGeneratingSubtitles = false,
                        generationError = e.localizedMessage ?: "Subtitle generation failed."
                    )
                }
            }
        }
    }

    // --- Library & Profile Actions ---
    fun updateLibrarySearch(q: String) = _ui.update { it.copy(librarySearch = q) }
    fun updateLibraryFilter(f: String) = _ui.update { it.copy(libraryFilter = f) }
    fun openLibraryProject(project: StudioProject?) = _ui.update { it.copy(selectedLibraryProject = project) }

    fun loadProjectIntoStudio(project: StudioProject) {
        val mediaFile = project.localMediaPath?.let { File(it) }?.takeIf { it.exists() }
        val loadedBitmap = if (mediaFile != null && (project.projectType == ProjectType.IMAGE_GEN.name || project.projectType == ProjectType.IMAGE_EDIT.name)) {
            BitmapFactory.decodeFile(mediaFile.absolutePath)
        } else null

        val targetMode = when (project.projectType) {
            ProjectType.IMAGE_TO_VIDEO.name -> CreateMode.IMAGE_TO_VIDEO
            ProjectType.IMAGE_GEN.name, ProjectType.IMAGE_EDIT.name -> CreateMode.IMAGE_STUDIO
            else -> CreateMode.TEXT_TO_VIDEO
        }
        val targetRatio = AspectRatioOption.entries.firstOrNull { it.apiValue == project.aspectRatio }
            ?: AspectRatioOption.LANDSCAPE_16_9

        _ui.update {
            it.copy(
                selectedLibraryProject = null,
                activeTab = MainTab.CREATE,
                createMode = targetMode,
                promptText = project.prompt,
                selectedAspectRatio = targetRatio,
                generatedVideoFile = if (targetMode != CreateMode.IMAGE_STUDIO) mediaFile else it.generatedVideoFile,
                generatedImageFile = if (targetMode == CreateMode.IMAGE_STUDIO) mediaFile else it.generatedImageFile,
                generatedImageBitmap = loadedBitmap ?: it.generatedImageBitmap,
                activeInteractionId = project.interactionId,
                activeProjectId = project.id
            )
        }
    }

    fun duplicateProject(project: StudioProject) {
        viewModelScope.launch {
            val copy = project.copy(
                id = "",
                title = "${project.title} (Copy)".take(120),
                createdAt = null,
                updatedAt = null
            )
            repository.createProject(copy)
            _ui.update {
                it.copy(
                    snackbarMessage = if (currentLang() == "id") "Proyek diduplikat" else "Project duplicated"
                )
            }
        }
    }

    fun deleteProject(project: StudioProject) {
        viewModelScope.launch {
            repository.deleteProject(currentUserId, project.id)
            _ui.update {
                it.copy(
                    selectedLibraryProject = null,
                    snackbarMessage = if (currentLang() == "id") "Proyek dihapus" else "Project deleted"
                )
            }
        }
    }

    fun claimDailyCredits() {
        viewModelScope.launch {
            val res = repository.claimDailyCredits(currentUserId)
            _ui.update {
                it.copy(
                    snackbarMessage = if (res.isSuccess) {
                        if (currentLang() == "id") "+10 kredit ditambahkan!" else "+10 credits added!"
                    } else {
                        res.exceptionOrNull()?.localizedMessage ?: "Failed to claim credits"
                    }
                )
            }
        }
    }

    fun toggleTheme(dark: Boolean) {
        viewModelScope.launch {
            repository.updatePreferences(currentUserId, currentLang(), dark)
        }
    }

    fun toggleLanguage(lang: String) {
        val currentDark = (userProfileState.value as? UiState.Success)?.data?.darkTheme ?: true
        viewModelScope.launch {
            repository.updatePreferences(currentUserId, lang, currentDark)
        }
    }
}
