package com.example.media

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

class VoiceoverTtsEngine(context: Context) {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            _isReady.value = (status == TextToSpeech.SUCCESS)
            if (status == TextToSpeech.SUCCESS) {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            }
        }
    }

    fun speak(text: String, languageCode: String, speed: Float, pitch: Float) {
        val engine = tts ?: return
        if (!_isReady.value || text.isBlank()) return
        val locale = if (languageCode == "id") Locale.forLanguageTag("id-ID") else Locale.US
        engine.language = locale
        engine.setSpeechRate(speed.coerceIn(0.5f, 2.0f))
        engine.setPitch(pitch.coerceIn(0.6f, 1.5f))
        val params = Bundle()
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "vidgenai_tts_play")
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun exportToWav(
        text: String,
        languageCode: String,
        speed: Float,
        pitch: Float,
        outputFile: File,
        onComplete: (Boolean) -> Unit
    ) {
        val engine = tts
        if (engine == null || !_isReady.value || text.isBlank()) {
            onComplete(false)
            return
        }
        val locale = if (languageCode == "id") Locale.forLanguageTag("id-ID") else Locale.US
        engine.language = locale
        engine.setSpeechRate(speed.coerceIn(0.5f, 2.0f))
        engine.setPitch(pitch.coerceIn(0.6f, 1.5f))
        outputFile.parentFile?.mkdirs()

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                if (utteranceId == "vidgenai_tts_export") {
                    onComplete(outputFile.exists() && outputFile.length() > 0L)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                if (utteranceId == "vidgenai_tts_export") {
                    onComplete(false)
                }
            }
        })

        val res = engine.synthesizeToFile(text, Bundle(), outputFile, "vidgenai_tts_export")
        if (res != TextToSpeech.SUCCESS) {
            onComplete(false)
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
