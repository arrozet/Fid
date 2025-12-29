package com.example.fid.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.*

class VoiceRecognitionHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onReadyForSpeech: () -> Unit = {},
    private val onBeginningOfSpeech: () -> Unit = {},
    private val onPartialResult: (String) -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            setupRecognitionListener()
        } else {
            onError("El reconocimiento de voz no está disponible en este dispositivo")
        }
    }

    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "========== onReadyForSpeech ==========")
                params?.keySet()?.forEach { key ->
                    Log.d(TAG, "Params: $key -> ${params.get(key)}")
                }
                onReadyForSpeech()
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "========== onBeginningOfSpeech ==========")
                Log.d(TAG, "🎤 User started speaking")
                onBeginningOfSpeech()
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Log de nivel de audio cada cierto umbral
                if (rmsdB > 0) {
                    Log.v(TAG, "Audio level (RMS): $rmsdB dB")
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // No se usa generalmente
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "========== onEndOfSpeech ==========")
                Log.d(TAG, "User stopped speaking, processing...")
                isListening = false
            }

            override fun onError(error: Int) {
                Log.e(TAG, "onError: $error")
                isListening = false
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Error de audio. Verifica el micrófono"
                    SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes. Activa el micrófono"
                    SpeechRecognizer.ERROR_NETWORK -> "Error de red. Verifica tu conexión a internet"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de espera agotado. Verifica tu conexión"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No se entendió lo que dijiste. Intenta hablar más claro y cerca del micrófono"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El reconocedor está ocupado. Espera un momento"
                    SpeechRecognizer.ERROR_SERVER -> "Error del servidor de Google. Intenta de nuevo"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó voz. Habla más fuerte o acerca el micrófono"
                    else -> "Error desconocido ($error)"
                }
                onError(errorMessage)
            }

            override fun onResults(results: Bundle?) {
                Log.d(TAG, "========== onResults ==========")
                isListening = false
                
                // Log de todas las claves del bundle para debugging
                results?.keySet()?.forEach { key ->
                    Log.d(TAG, "Bundle key: $key -> ${results.get(key)}")
                }
                
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.d(TAG, "Total matches: ${matches?.size ?: 0}")
                
                if (!matches.isNullOrEmpty()) {
                    matches.forEachIndexed { index, match ->
                        Log.d(TAG, "Match $index: $match")
                    }
                    val recognizedText = matches[0]
                    Log.d(TAG, "✓ Final recognized text: $recognizedText")
                    onResult(recognizedText)
                } else {
                    Log.w(TAG, "⚠️ Results bundle was empty or null")
                    onError("No se recibieron resultados del reconocedor")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Resultados parciales en tiempo real
                Log.d(TAG, "========== onPartialResults ==========")
                
                // Log de todas las claves del bundle
                partialResults?.keySet()?.forEach { key ->
                    Log.d(TAG, "Partial Bundle key: $key -> ${partialResults.get(key)}")
                }
                
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.d(TAG, "Partial matches count: ${matches?.size ?: 0}")
                
                if (!matches.isNullOrEmpty()) {
                    matches.forEachIndexed { index, match ->
                        Log.d(TAG, "Partial $index: $match")
                    }
                    val partialText = matches[0]
                    Log.d(TAG, "🎤 Partial result: $partialText")
                    onPartialResult(partialText)
                } else {
                    Log.d(TAG, "No partial results yet")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // No se usa generalmente
            }
        })
    }

    fun startListening() {
        if (isListening) {
            Log.w(TAG, "Already listening")
            return
        }

        Log.d(TAG, "========== START LISTENING ==========")
        Log.d(TAG, "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        Log.d(TAG, "Android version: ${android.os.Build.VERSION.RELEASE}")
        Log.d(TAG, "Recognition available: ${SpeechRecognizer.isRecognitionAvailable(context)}")

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // Configurar idioma español explícitamente
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-ES")
            // Habilitar resultados parciales
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Solicitar más resultados para mayor precisión
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // Aumentar el tiempo de espera de voz
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
            // Pedir que reconozca aunque no esté seguro
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }

        Log.d(TAG, "Recognition intent configured:")
        Log.d(TAG, "  - Language: es-ES")
        Log.d(TAG, "  - Model: FREE_FORM")
        Log.d(TAG, "  - Partial results: enabled")
        Log.d(TAG, "  - Max results: 5")

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
            Log.d(TAG, "✓ Speech recognizer started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error starting recognition", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            e.printStackTrace()
            onError("Error al iniciar el reconocimiento: ${e.message}")
        }
    }

    fun stopListening() {
        if (!isListening) {
            return
        }
        speechRecognizer?.stopListening()
        isListening = false
        Log.d(TAG, "Stopped listening")
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
        Log.d(TAG, "Destroyed")
    }

    fun isCurrentlyListening(): Boolean = isListening

    companion object {
        private const val TAG = "VoiceRecognitionHelper"
    }
}

