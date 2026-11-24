package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.AspectRatioOption
import com.example.model.StoryboardScene
import com.example.model.StoryboardScript
import com.example.model.SubtitleSegment
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

data class VideoGenerationResult(
    val localVideoFile: File,
    val interactionId: String?
)

data class ModerationResult(
    val allowed: Boolean,
    val reason: String
)

/**
 * Adapter pattern interface so AI providers can be swapped cleanly.
 */
interface AiContentProvider {
    suspend fun moderatePrompt(prompt: String): ModerationResult
    suspend fun enhancePrompt(prompt: String, style: String, language: String): String
    suspend fun generateStoryboard(
        topic: String,
        style: String,
        aspectRatio: String,
        language: String
    ): StoryboardScript
    suspend fun generateSubtitles(
        transcriptOrTopic: String,
        language: String
    ): List<SubtitleSegment>
    suspend fun generateOrEditImage(
        prompt: String,
        inputBitmap: Bitmap?,
        aspectRatio: AspectRatioOption
    ): Bitmap
    suspend fun generateTextToVideo(
        prompt: String,
        style: String,
        aspectRatio: AspectRatioOption,
        resolution: String,
        outputFile: File
    ): VideoGenerationResult
    suspend fun generateImageToVideo(
        imageBitmap: Bitmap,
        motionInstruction: String,
        aspectRatio: AspectRatioOption,
        resolution: String,
        outputFile: File
    ): VideoGenerationResult
    suspend fun editVideoWithInteraction(
        previousInteractionId: String,
        editInstruction: String,
        aspectRatio: AspectRatioOption,
        outputFile: File
    ): VideoGenerationResult
}

/**
 * Combines Firebase AI Logic (gemini-3.8-flash + gemini-nano-banana-2.1)
 * with Gemini Interactions API (gemini-omni-1.2-flash for Text/Image-to-Video).
 */
class GeminiStudioProvider(
    private val context: Context
) : AiContentProvider {

    companion object {
        private const val TAG = "GeminiStudioProvider"
        private const val TEXT_MODEL = "gemini-3.8-flash"
        private const val IMAGE_MODEL = "gemini-nano-banana-2.1"
        private const val VIDEO_MODEL = "gemini-omni-1.2-flash"
        private const val INTERACTIONS_URL = "https://generativelanguage.googleapis.com/v1beta/interactions"
    }

    private val textModel by lazy {
        Firebase.ai.generativeModel(modelName = TEXT_MODEL)
    }

    private val moderationModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "allowed" to Schema.boolean(description = "True if safe for creative video/image generation"),
                "reason" to Schema.string(description = "Short explanation if blocked, or 'OK'")
            )
        )
        Firebase.ai.generativeModel(
            modelName = TEXT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val storyboardModel by lazy {
        val sceneSchema = Schema.obj(
            properties = mapOf(
                "sceneNumber" to Schema.integer(),
                "durationSec" to Schema.integer(),
                "visualPrompt" to Schema.string(description = "Detailed visual prompt for AI video generator"),
                "narration" to Schema.string(description = "Voiceover line for this scene"),
                "cameraAngle" to Schema.string(description = "Camera framing or movement")
            )
        )
        val rootSchema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string(),
                "musicMood" to Schema.string(),
                "scenes" to Schema.array(items = sceneSchema)
            )
        )
        Firebase.ai.generativeModel(
            modelName = TEXT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = rootSchema
            }
        )
    }

    private val subtitleModel by lazy {
        val segmentSchema = Schema.obj(
            properties = mapOf(
                "startSec" to Schema.double(),
                "endSec" to Schema.double(),
                "text" to Schema.string(),
                "highlightWord" to Schema.string()
            )
        )
        val rootSchema = Schema.obj(
            properties = mapOf(
                "segments" to Schema.array(items = segmentSchema)
            )
        )
        Firebase.ai.generativeModel(
            modelName = TEXT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = rootSchema
            }
        )
    }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun moderatePrompt(prompt: String): ModerationResult {
        val lower = prompt.lowercase()
        val quickBlockTerms = listOf("deepfake", "nude", "nsfw", "porn", "gore", "self-harm")
        if (quickBlockTerms.any { lower.contains(it) }) {
            return ModerationResult(false, "Blocked safety keyword detected.")
        }
        return try {
            val response = withTimeout(30_000L) {
                moderationModel.generateContent(
                    "Evaluate if this creative media prompt is safe (no NSFW/explicit nudity, non-consensual deepfake of real people, extreme violence, or illegal harm). Prompt: \"$prompt\""
                )
            }
            val json = JSONObject(response.text ?: """{"allowed":true,"reason":"OK"}""")
            ModerationResult(
                allowed = json.optBoolean("allowed", true),
                reason = json.optString("reason", "OK")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Moderation fallback check: ${e.message}")
            ModerationResult(true, "OK")
        }
    }

    override suspend fun enhancePrompt(prompt: String, style: String, language: String): String {
        val langLabel = if (language == "id") "Indonesian or English cinematic prompt" else "English cinematic prompt"
        val instruction = """
            You are an expert AI film director and prompt engineer.
            Expand this user idea into one rich, vivid, production-ready prompt for AI video/image generation.
            Include subject details, lighting, camera lens/motion, atmosphere, and style ($style).
            Output ONLY the enhanced prompt text (max 90 words, no quotes or markdown).
            Language preference: $langLabel.
            Idea: $prompt
        """.trimIndent()
        val response = withTimeout(45_000L) {
            textModel.generateContent(instruction)
        }
        return response.text?.trim()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Prompt enhancer returned empty output.")
    }

    override suspend fun generateStoryboard(
        topic: String,
        style: String,
        aspectRatio: String,
        language: String
    ): StoryboardScript {
        val langName = if (language == "id") "Indonesian" else "English"
        val prompt = """
            Create a 4-scene short video storyboard for the topic: "$topic".
            Visual style: $style. Aspect ratio: $aspectRatio.
            Write title, musicMood, and narration in $langName.
            Write visualPrompt in clear descriptive English optimized for AI video generation.
            Each scene durationSec should be 3 to 5 seconds.
        """.trimIndent()

        val response = withTimeout(60_000L) {
            storyboardModel.generateContent(prompt)
        }
        val raw = response.text ?: throw IllegalStateException("Empty storyboard response.")
        val json = JSONObject(raw)
        val scenesArr = json.getJSONArray("scenes")
        val scenes = (0 until scenesArr.length()).map { idx ->
            val obj = scenesArr.getJSONObject(idx)
            StoryboardScene(
                sceneNumber = obj.optInt("sceneNumber", idx + 1),
                durationSec = obj.optInt("durationSec", 4),
                visualPrompt = obj.getString("visualPrompt"),
                narration = obj.getString("narration"),
                cameraAngle = obj.optString("cameraAngle", "Medium Cinematic Shot")
            )
        }
        return StoryboardScript(
            title = json.optString("title", topic.take(40)),
            musicMood = json.optString("musicMood", "Upbeat Electronic"),
            scenes = scenes
        )
    }

    override suspend fun generateSubtitles(
        transcriptOrTopic: String,
        language: String
    ): List<SubtitleSegment> {
        val langName = if (language == "id") "Indonesian" else "English"
        val prompt = """
            Generate 5 punchy, short social-media subtitle segments (total ~15 seconds, 0.0 to 15.0)
            in $langName for this script/topic: "$transcriptOrTopic".
            Keep each segment concise (3-6 words) and pick one highlightWord from each segment text.
        """.trimIndent()
        val response = withTimeout(45_000L) {
            subtitleModel.generateContent(prompt)
        }
        val raw = response.text ?: throw IllegalStateException("Empty subtitle response.")
        val arr = JSONObject(raw).getJSONArray("segments")
        return (0 until arr.length()).map { idx ->
            val obj = arr.getJSONObject(idx)
            SubtitleSegment(
                startSec = obj.optDouble("startSec", idx * 3.0).toFloat(),
                endSec = obj.optDouble("endSec", (idx + 1) * 3.0).toFloat(),
                text = obj.getString("text"),
                highlightWord = obj.optString("highlightWord", "")
            )
        }
    }

    override suspend fun generateOrEditImage(
        prompt: String,
        inputBitmap: Bitmap?,
        aspectRatio: AspectRatioOption
    ): Bitmap {
        val mappedAspect = when (aspectRatio) {
            AspectRatioOption.LANDSCAPE_16_9 -> AspectRatio.LANDSCAPE_16x9
            AspectRatioOption.PORTRAIT_9_16 -> AspectRatio.PORTRAIT_9x16
            AspectRatioOption.SQUARE_1_1 -> AspectRatio.SQUARE_1x1
        }
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(mappedAspect)
                .setImageSize(ImageSize.SIZE_1K)
                .build()
        }
        val imageModel = Firebase.ai.generativeModel(
            modelName = IMAGE_MODEL,
            generationConfig = config
        )

        val response = withTimeout(60_000L) {
            if (inputBitmap != null) {
                imageModel.generateContent(
                    content {
                        image(inputBitmap)
                        text(prompt)
                    }
                )
            } else {
                imageModel.generateContent(prompt)
            }
        }
        val imagePart = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()
        return imagePart?.image
            ?: throw IllegalStateException("Model $IMAGE_MODEL did not return an image.")
    }

    override suspend fun generateTextToVideo(
        prompt: String,
        style: String,
        aspectRatio: AspectRatioOption,
        resolution: String,
        outputFile: File
    ): VideoGenerationResult {
        val apiAspect = if (aspectRatio == AspectRatioOption.PORTRAIT_9_16) "9:16" else "16:9"
        val fullPrompt = "$prompt. Visual style: $style. Aspect ratio: $apiAspect."
        val payload = JSONObject().apply {
            put("model", VIDEO_MODEL)
            put("input", fullPrompt)
            put(
                "response_format",
                JSONObject().apply {
                    put("type", "video")
                    put("aspect_ratio", apiAspect)
                    put("resolution", resolution)
                    put("delivery", "uri")
                }
            )
        }
        return executeVideoInteraction(payload, outputFile)
    }

    override suspend fun generateImageToVideo(
        imageBitmap: Bitmap,
        motionInstruction: String,
        aspectRatio: AspectRatioOption,
        resolution: String,
        outputFile: File
    ): VideoGenerationResult {
        val apiAspect = if (aspectRatio == AspectRatioOption.PORTRAIT_9_16) "9:16" else "16:9"
        val base64Jpeg = encodeBitmapToBase64Jpeg(imageBitmap)
        val inputArray = JSONArray().apply {
            put(
                JSONObject().apply {
                    put("type", "image")
                    put("data", base64Jpeg)
                    put("mime_type", "image/jpeg")
                }
            )
            put(
                JSONObject().apply {
                    put("type", "text")
                    put(
                        "text",
                        "Animate this reference image into smooth, realistic motion video. Motion: $motionInstruction. Maintain subject consistency."
                    )
                }
            )
        }
        val payload = JSONObject().apply {
            put("model", VIDEO_MODEL)
            put("input", inputArray)
            put(
                "generation_config",
                JSONObject().apply {
                    put(
                        "video_config",
                        JSONObject().apply {
                            put("task", "image_to_video")
                        }
                    )
                }
            )
            put(
                "response_format",
                JSONObject().apply {
                    put("type", "video")
                    put("aspect_ratio", apiAspect)
                    put("resolution", resolution)
                    put("delivery", "uri")
                }
            )
        }
        return executeVideoInteraction(payload, outputFile)
    }

    override suspend fun editVideoWithInteraction(
        previousInteractionId: String,
        editInstruction: String,
        aspectRatio: AspectRatioOption,
        outputFile: File
    ): VideoGenerationResult {
        val apiAspect = if (aspectRatio == AspectRatioOption.PORTRAIT_9_16) "9:16" else "16:9"
        val payload = JSONObject().apply {
            put("model", VIDEO_MODEL)
            put("previous_interaction_id", previousInteractionId)
            put("input", editInstruction)
            put(
                "response_format",
                JSONObject().apply {
                    put("type", "video")
                    put("aspect_ratio", apiAspect)
                    put("delivery", "uri")
                }
            )
        }
        return executeVideoInteraction(payload, outputFile)
    }

    private suspend fun executeVideoInteraction(
        payload: JSONObject,
        outputFile: File
    ): VideoGenerationResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException(
                "GEMINI_API_KEY is not configured. Open the Secrets panel in AI Studio and add your GEMINI_API_KEY for gemini-omni-1.2-flash Interactions API video generation."
            )
        }

        val reqBody = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(INTERACTIONS_URL)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(reqBody)
            .build()

        val responseJson = retryOnTransientFailure {
            httpClient.newCall(request).execute().use { resp ->
                val bodyStr = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Interactions API HTTP ${resp.code}: $bodyStr")
                }
                JSONObject(bodyStr)
            }
        }

        val interactionId = responseJson.optString("id").takeIf { it.isNotBlank() }
        val videoOutput = extractVideoOutput(responseJson)
            ?: throw IllegalStateException("Interactions API completed without video output.")

        outputFile.parentFile?.mkdirs()

        if (!videoOutput.base64Data.isNullOrBlank()) {
            val bytes = Base64.decode(videoOutput.base64Data, Base64.DEFAULT)
            outputFile.writeBytes(bytes)
            return@withContext VideoGenerationResult(outputFile, interactionId)
        }

        val fileUri = videoOutput.uri
            ?: throw IllegalStateException("Video output missing both inline data and URI.")

        // Poll file readiness if URI references Gemini Files API
        if (fileUri.contains("/files/")) {
            waitForFileActive(fileUri, apiKey)
        }

        val downloadUrl = if (fileUri.contains("alt=media")) {
            fileUri
        } else {
            val sep = if (fileUri.contains("?")) "&" else "?"
            "$fileUri${sep}alt=media"
        }

        val dlReq = Request.Builder()
            .url(downloadUrl)
            .addHeader("x-goog-api-key", apiKey)
            .get()
            .build()

        httpClient.newCall(dlReq).execute().use { dlResp ->
            if (!dlResp.isSuccessful) {
                throw IllegalStateException("Failed to download video HTTP ${dlResp.code}")
            }
            val bytes = dlResp.body?.bytes()
                ?: throw IllegalStateException("Empty video download body.")
            outputFile.writeBytes(bytes)
        }

        VideoGenerationResult(outputFile, interactionId)
    }

    private suspend fun waitForFileActive(fileUri: String, apiKey: String) {
        val statusUrl = fileUri.substringBefore(":download").substringBefore("?")
        repeat(24) {
            val statusReq = Request.Builder()
                .url(statusUrl)
                .addHeader("x-goog-api-key", apiKey)
                .get()
                .build()
            val state = try {
                httpClient.newCall(statusReq).execute().use { resp ->
                    if (!resp.isSuccessful) return@use "ACTIVE"
                    val json = JSONObject(resp.body?.string().orEmpty())
                    json.optString("state", "ACTIVE")
                }
            } catch (_: Exception) {
                "ACTIVE"
            }
            if (state == "ACTIVE") return
            if (state == "FAILED") throw IllegalStateException("Video processing failed on server.")
            delay(3000L)
        }
    }

    private suspend fun <T> retryOnTransientFailure(block: () -> T): T {
        var lastError: Exception? = null
        repeat(2) { attempt ->
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                val msg = e.message.orEmpty()
                val transient = msg.contains("HTTP 429") || msg.contains("HTTP 503") || msg.contains("timeout", true)
                if (!transient || attempt == 1) throw e
                delay(2000L)
            }
        }
        throw lastError ?: IllegalStateException("Request failed")
    }

    private data class ExtractedVideo(val uri: String?, val base64Data: String?)

    private fun extractVideoOutput(root: JSONObject): ExtractedVideo? {
        // Check steps[] -> model_output -> content[]
        val steps = root.optJSONArray("steps")
        if (steps != null) {
            for (i in steps.length() - 1 downTo 0) {
                val step = steps.optJSONObject(i) ?: continue
                val contentArr = step.optJSONArray("content") ?: continue
                for (j in 0 until contentArr.length()) {
                    val part = contentArr.optJSONObject(j) ?: continue
                    val mime = part.optString("mime_type", part.optString("mimeType"))
                    val type = part.optString("type")
                    if (type == "video" || mime.startsWith("video/")) {
                        val uri = part.optString("uri").takeIf { it.isNotBlank() }
                        val data = part.optString("data").takeIf { it.isNotBlank() }
                        if (uri != null || data != null) return ExtractedVideo(uri, data)
                    }
                }
            }
        }
        // Check outputs[] fallback
        val outputs = root.optJSONArray("outputs")
        if (outputs != null) {
            for (i in 0 until outputs.length()) {
                val part = outputs.optJSONObject(i) ?: continue
                val uri = part.optString("uri").takeIf { it.isNotBlank() }
                val data = part.optString("data").takeIf { it.isNotBlank() }
                if (uri != null || data != null) return ExtractedVideo(uri, data)
            }
        }
        // Check output_video object fallback
        val outVid = root.optJSONObject("output_video")
        if (outVid != null) {
            val uri = outVid.optString("uri").takeIf { it.isNotBlank() }
            val data = outVid.optString("data").takeIf { it.isNotBlank() }
            if (uri != null || data != null) return ExtractedVideo(uri, data)
        }
        return null
    }

    private fun encodeBitmapToBase64Jpeg(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
