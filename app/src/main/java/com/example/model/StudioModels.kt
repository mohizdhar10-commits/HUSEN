package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Creator",
    val email: String = "",
    val planTier: String = "FREE",
    val credits: Long = 50L,
    val totalGenerations: Long = 0L,
    val preferredLanguage: String = "id",
    val darkTheme: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudioProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val projectType: String = ProjectType.TEXT_TO_VIDEO.name,
    val prompt: String = "",
    val enhancedPrompt: String? = null,
    val style: String = "Cinematic",
    val aspectRatio: String = "16:9",
    val resolution: String = "720p",
    val status: String = ProjectStatus.COMPLETED.name,
    val creditCost: Long = 10L,
    val localMediaPath: String? = null,
    val interactionId: String? = null,
    val scriptJson: String? = null,
    val subtitlesJson: String? = null,
    val errorMessage: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class ProjectType {
    TEXT_TO_VIDEO,
    IMAGE_TO_VIDEO,
    IMAGE_GEN,
    IMAGE_EDIT,
    SCRIPT,
    VOICEOVER
}

enum class ProjectStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED
}

enum class CreateMode {
    TEXT_TO_VIDEO,
    IMAGE_TO_VIDEO,
    IMAGE_STUDIO
}

enum class ToolTab {
    SCRIPT_STORYBOARD,
    PROMPT_ENHANCER,
    VOICEOVER_TTS,
    SUBTITLE_EDITOR
}

enum class MainTab {
    HOME,
    CREATE,
    TOOLS,
    LIBRARY,
    PROFILE
}

enum class AspectRatioOption(val apiValue: String, val labelId: String, val labelEn: String) {
    LANDSCAPE_16_9("16:9", "16:9 Lanskap", "16:9 Landscape"),
    PORTRAIT_9_16("9:16", "9:16 Vertikal", "9:16 Portrait"),
    SQUARE_1_1("1:1", "1:1 Persegi", "1:1 Square")
}

enum class VideoStyleOption(val label: String, val promptSuffix: String) {
    CINEMATIC("Cinematic", "cinematic 35mm film, anamorphic lens flare, dramatic volumetric lighting"),
    REALISTIC("Realistis", "ultra photorealistic, natural lighting, 8k DSLR footage"),
    ANIME("Anime", "high-budget anime animation style, vibrant cel shading, expressive motion"),
    PIXAR_3D("3D / Pixar", "stylized 3D CGI animation, subsurface scattering, vibrant studio lighting"),
    CARTOON_2D("Kartun 2D", "clean modern 2D vector animation, bold outlines, lively motion"),
    PIXEL_ART("Pixel Art", "retro 16-bit pixel art animation, crisp dithering, nostalgic palette"),
    CLAYMATION("Claymation", "handcrafted stop-motion claymation texture, miniature studio lighting")
}

enum class CameraMotionOption(val labelId: String, val labelEn: String, val instruction: String) {
    AUTO("Otomatis AI", "AI Auto", "natural smooth cinematic camera movement"),
    ZOOM_IN("Zoom Masuk", "Slow Zoom In", "slow dramatic dolly zoom in toward the main subject"),
    PAN_RIGHT("Geser Kanan", "Pan Right", "smooth horizontal camera pan from left to right"),
    ORBIT("Orbit 360", "Orbit Around", "graceful orbital camera sweep around the subject"),
    HANDHELD("Handheld Dinamis", "Dynamic Handheld", "subtle energetic handheld camera tracking motion")
}

enum class ResolutionOption(val apiValue: String, val creditCost: Int) {
    RES_360P("360p", 5),
    RES_720P("720p", 10),
    RES_1080P("1080p", 15)
}

enum class SubtitleStyleOption(val labelId: String, val labelEn: String) {
    TIKTOK_BOLD("TikTok Bold", "TikTok Bold"),
    KARAOKE_GLOW("Karaoke Glow", "Karaoke Glow"),
    MINIMAL_CLEAN("Minimal Bersih", "Minimal Clean")
}

enum class VideoFilterOption(val labelId: String, val labelEn: String) {
    ORIGINAL("Original", "Original"),
    VIVID("Vivid Neon", "Vivid Neon"),
    CINEMATIC("Teal & Orange", "Teal & Orange"),
    NOIR("Noir B&W", "Noir B&W"),
    WARM("Golden Warm", "Golden Warm")
}

enum class VoiceEmotionOption(val labelId: String, val labelEn: String, val pitch: Float) {
    NEUTRAL("Netral Jelas", "Clear Neutral", 1.0f),
    ENTHUSIASTIC("Antusias Promo", "Enthusiastic", 1.15f),
    CALM("Tenang Dokumenter", "Calm Documentary", 0.90f),
    DRAMATIC("Dramatis Trailer", "Dramatic Trailer", 0.82f)
}

data class StoryboardScene(
    val sceneNumber: Int,
    val durationSec: Int,
    val visualPrompt: String,
    val narration: String,
    val cameraAngle: String
)

data class StoryboardScript(
    val title: String,
    val musicMood: String,
    val scenes: List<StoryboardScene>
)

data class SubtitleSegment(
    val startSec: Float,
    val endSec: Float,
    val text: String,
    val highlightWord: String
)

data class StudioTemplate(
    val id: String,
    val titleId: String,
    val titleEn: String,
    val categoryId: String,
    val categoryEn: String,
    val aspectRatio: AspectRatioOption,
    val style: VideoStyleOption,
    val prompt: String,
    val mode: CreateMode,
    val drawableResName: String
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class GenerationProgress(
    val isActive: Boolean = false,
    val stepLabel: String = "",
    val percent: Int = 0,
    val etaSeconds: Int = 0
)
