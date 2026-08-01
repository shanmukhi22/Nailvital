package com.nailvital.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.channels.BufferOverflow
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.nailvital.app.api.ApiClient
import com.nailvital.app.api.SessionManager
import com.nailvital.app.api.VoiceCommandRequest

// ─────────────────────────────────────────────────────
//  Composition Local for Broadcasts
// ─────────────────────────────────────────────────────
val LocalVoiceActions = staticCompositionLocalOf<SharedFlow<VoiceAction>?> { null }

// ─────────────────────────────────────────────────────
//  Voice Assistant State
// ─────────────────────────────────────────────────────
enum class VoiceState {
    IDLE,        // Mic is off
    LISTENING,   // Actively recording
    PROCESSING,  // Recognized text, executing command
    SPEAKING,    // TTS is speaking
    ERROR        // Something went wrong
}

// ─────────────────────────────────────────────────────
//  Navigation Actions — every screen + action in app
// ─────────────────────────────────────────────────────
sealed class VoiceAction {
    // Navigation
    object GoHome         : VoiceAction()
    object GoScan         : VoiceAction()
    object GoHistory      : VoiceAction()
    object GoChat         : VoiceAction()
    object GoProfile      : VoiceAction()
    object GoWiki         : VoiceAction()
    object GoBack         : VoiceAction()
    // Auth
    object GoLogin        : VoiceAction()
    object GoRegister     : VoiceAction()
    object GoForgotPass   : VoiceAction()
    object Logout         : VoiceAction()
    // Profile sub-screens
    object GoPersonalDetails  : VoiceAction()
    object GoChangePassword   : VoiceAction()
    // Scan actions
    object TakePhoto      : VoiceAction()
    // History actions
    object GenerateReport : VoiceAction()
    // Help / Info
    object ReadCommands   : VoiceAction()
    object OpenHelp       : VoiceAction()
    // Scroll
    object ScrollUp       : VoiceAction()
    object ScrollDown     : VoiceAction()
    // Flow/Affirmation
    object Continue       : VoiceAction()
    // Deep links
    data class GoToDisease(val name: String) : VoiceAction()
    object LoginGuest     : VoiceAction()
    object GoAbout        : VoiceAction()
    // Unknown
    object Unknown        : VoiceAction()
}

// ─────────────────────────────────────────────────────
//  VoiceAssistantManager
// ─────────────────────────────────────────────────────
class VoiceAssistantManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    // Observable state for UI
    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState

    private val _lastHeardText = MutableStateFlow("")
    val lastHeardText: StateFlow<String> = _lastHeardText

    private var isContinuous = false

    // Broadcasts actions to the entire app
    private val _voiceActions = MutableSharedFlow<VoiceAction>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val voiceActions: SharedFlow<VoiceAction> = _voiceActions

    // ── Init TTS ─────────────────────────────────────
    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.95f)
                
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _voiceState.value = VoiceState.SPEAKING
                    }
                    override fun onDone(utteranceId: String?) {
                        if (isContinuous) {
                            // Automatically restart listening once the assistant is done speaking
                            CoroutineScope(Dispatchers.Main).launch {
                                // Small delay to avoid clipping the start of speech
                                kotlinx.coroutines.delay(300)
                                startListeningInternal()
                            }
                        } else {
                            _voiceState.value = VoiceState.IDLE
                        }
                    }
                    override fun onError(utteranceId: String?) {
                        _voiceState.value = VoiceState.IDLE
                    }
                })
                ttsReady = true
            }
        }
    }

    // ── Start Listening ───────────────────────────────
    fun toggleListening() {
        if (_voiceState.value == VoiceState.LISTENING || _voiceState.value == VoiceState.PROCESSING || isContinuous) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun startListening() {
        isContinuous = true
        if (_voiceState.value == VoiceState.LISTENING) return

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                _voiceState.value = VoiceState.LISTENING
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                _lastHeardText.value = text
                handleCommand(text)
                
                // We DON'T restart here if we are about to speak. 
                // handleCommand will trigger speak(), and onDone will restart listening.
            }

            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO             -> "Microphone error"
                    SpeechRecognizer.ERROR_NO_MATCH          -> null // Don't speak for no match
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT    -> null // Silent restart
                    SpeechRecognizer.ERROR_NETWORK           -> "Network error"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY   -> null // Already listening?
                    else -> null
                }
                
                if (msg != null) {
                    _voiceState.value = VoiceState.ERROR
                    speak(msg)
                } else {
                    // Silent restart for pure timeouts
                    if (isContinuous) {
                        CoroutineScope(Dispatchers.Main).launch {
                            kotlinx.coroutines.delay(500)
                            if (isContinuous) startListeningInternal()
                        }
                    } else {
                        _voiceState.value = VoiceState.IDLE
                    }
                }
            }

            override fun onBeginningOfSpeech()              { }
            override fun onBufferReceived(buffer: ByteArray?)  { }
            override fun onEndOfSpeech()                     { _voiceState.value = VoiceState.PROCESSING }
            override fun onEvent(eventType: Int, params: Bundle?) { }
            override fun onPartialResults(partialResults: Bundle?) { }
            override fun onRmsChanged(rmsdB: Float)          { }
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,   RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,         Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,      3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,  false)
            putExtra(RecognizerIntent.EXTRA_PROMPT,           "Say a command…")
        }
        speechRecognizer?.startListening(intent)
    }

    private fun startListeningInternal() {
        if (_voiceState.value == VoiceState.LISTENING) return
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,   RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,         Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,      3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,  false)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        isContinuous = false
        speechRecognizer?.stopListening()
        _voiceState.value = VoiceState.IDLE
    }

    // ── Text to Speech ────────────────────────────────
    fun speak(text: String) {
        if (!ttsReady || text.isBlank()) return
        _voiceState.value = VoiceState.SPEAKING
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nv_tts_${System.currentTimeMillis()}")
    }

    // ── Command Parser (LLM Router) ───────────────────
    private fun handleCommand(raw: String) {
        val text = raw.lowercase().trim()
        if (text.isEmpty()) {
            _voiceState.value = VoiceState.IDLE
            return
        }
        _lastHeardText.value = raw
        _voiceState.value = VoiceState.PROCESSING

        val sessionManager = SessionManager(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = sessionManager.fetchAuthToken()
                val response = ApiClient.instance.voiceCommand("Bearer $token", VoiceCommandRequest(raw))
                
                withContext(Dispatchers.Main) {
                    when (response.action_type) {
                        "SPEAK" -> {
                            response.message?.let { speak(it) }
                            _voiceState.value = VoiceState.IDLE
                        }
                        "NAVIGATE" -> {
                            response.message?.let { speak(it) }
                            response.target?.let { t -> 
                                parseAndDispatchTarget(t)
                            }
                        }
                        "ACTION" -> {
                            response.message?.let { speak(it) }
                            response.target?.let { t -> 
                                parseAndDispatchTarget(t) 
                            }
                        }
                        "MULTI" -> {
                            response.message?.let { speak(it) }
                            response.commands?.forEach { cmd ->
                                parseAndDispatchTarget(cmd.target)
                            }
                        }
                        else -> {
                            speak("I'm not sure how to do that yet.")
                            _voiceState.value = VoiceState.IDLE
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    speak("Network error processing your command.")
                    _voiceState.value = VoiceState.IDLE
                }
            }
        }
    }

    private fun parseAndDispatchTarget(target: String) {
        val action = when {
            target.lowercase() == "home" -> VoiceAction.GoHome
            target.lowercase() == "scan" -> VoiceAction.GoScan
            target.lowercase() == "history" -> VoiceAction.GoHistory
            target.lowercase() == "chat" -> VoiceAction.GoChat
            target.lowercase() == "profile" -> VoiceAction.GoProfile
            target.lowercase() == "health_wiki" -> VoiceAction.GoWiki
            target.lowercase() == "login" -> VoiceAction.GoLogin
            target.lowercase() == "register" -> VoiceAction.GoRegister
            target.lowercase() == "forgot_password" -> VoiceAction.GoForgotPass
            target.lowercase() == "personal_details" -> VoiceAction.GoPersonalDetails
            target.lowercase() == "change_password" -> VoiceAction.GoChangePassword
            target.lowercase() == "take_photo" -> VoiceAction.TakePhoto
            target.lowercase() == "generate_report" -> VoiceAction.GenerateReport
            target.lowercase() == "scroll_up" -> VoiceAction.ScrollUp
            target.lowercase() == "scroll_down" -> VoiceAction.ScrollDown
            target.lowercase() == "continue" -> VoiceAction.Continue
            target.lowercase() == "guest_account" -> VoiceAction.LoginGuest
            target.lowercase() == "about" -> VoiceAction.GoAbout
            target.lowercase() == "logout" -> VoiceAction.Logout
            target.lowercase().startsWith("open_disease:") -> {
                val condition = target.substringAfter("open_disease:").trim()
                VoiceAction.GoToDisease(condition)
            }
            else -> null
        }
        action?.let {
            _voiceActions.tryEmit(it)
        }
        _voiceState.value = VoiceState.IDLE
    }

    // ── Cleanup ───────────────────────────────────────
    fun destroy() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
    }
}
