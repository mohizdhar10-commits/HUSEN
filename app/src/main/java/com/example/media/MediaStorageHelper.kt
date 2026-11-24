package com.example.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object MediaStorageHelper {

    private fun mediaDir(context: Context): File {
        val dir = File(context.filesDir, "studio_media")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun newVideoOutputFile(context: Context, prefix: String = "vid"): File {
        return File(mediaDir(context), "${prefix}_${System.currentTimeMillis()}.mp4")
    }

    fun newAudioOutputFile(context: Context, prefix: String = "tts"): File {
        return File(mediaDir(context), "${prefix}_${System.currentTimeMillis()}.wav")
    }

    suspend fun saveBitmapLocally(context: Context, bitmap: Bitmap, prefix: String = "img"): File =
        withContext(Dispatchers.IO) {
            val file = File(mediaDir(context), "${prefix}_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        }

    suspend fun decodeScaledBitmap(context: Context, uri: Uri, maxDim: Int = 1280): Bitmap =
        withContext(Dispatchers.IO) {
            val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                } ?: throw IllegalStateException("Cannot read selected image.")
            }

            val width = rawBitmap.width
            val height = rawBitmap.height
            val scale = minOf(1f, maxDim.toFloat() / maxOf(width, height).toFloat())
            if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    rawBitmap,
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                rawBitmap
            }
        }

    suspend fun exportVideoToGallery(context: Context, sourceFile: File, title: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(sourceFile.exists()) { "Video file does not exist." }
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, "${sanitizeName(title)}.mp4")
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/VidGenAIStudio")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                }
                val resolver = context.contentResolver
                val itemUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("MediaStore failed to create video entry.")
                resolver.openOutputStream(itemUri)?.use { out ->
                    sourceFile.inputStream().use { input -> input.copyTo(out) }
                } ?: throw IllegalStateException("Cannot write video stream.")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)
                }
            }
        }

    suspend fun exportImageToGallery(context: Context, sourceFile: File, title: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(sourceFile.exists()) { "Image file does not exist." }
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "${sanitizeName(title)}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/VidGenAIStudio")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val resolver = context.contentResolver
                val itemUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("MediaStore failed to create image entry.")
                resolver.openOutputStream(itemUri)?.use { out ->
                    sourceFile.inputStream().use { input -> input.copyTo(out) }
                } ?: throw IllegalStateException("Cannot write image stream.")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)
                }
            }
        }

    fun shareFile(context: Context, file: File, mimeType: String, shareTitle: String) {
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Created with VidGenAI Studio")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, shareTitle))
    }

    private fun sanitizeName(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(32).ifBlank { "vidgenai_export" }
    }
}
