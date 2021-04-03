package kr.ac.snu.hcil.omnitrack.core.speech

import android.content.Intent
import android.speech.RecognizerIntent
import android.content.Context
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import java.util.*

/**
 * Created by Yuhan Luo on 21. 4. 2
 */

class SpeechRecognizerUtility(context: Context) {

    interface StateListener {

        fun isListening(isListening: Boolean)
    }


//    interface ResultsListener {
//
//        fun onResults(possibleTexts: Array<out String>)
//    }

    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val miniListeningLength = 10
    private val maxiListeningLengthWhenIdle = 5

    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).also { intent ->
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, miniListeningLength*1000)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, maxiListeningLengthWhenIdle*1000)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
    }

   var stateListener: StateListener? = null

    fun setRecognitionListener(recognitionListener: RecognitionListener) {
        speechRecognizer.setRecognitionListener(recognitionListener)
    }

    fun start() {
        stateListener?.isListening(true)
        speechRecognizer.startListening(intent)
    }

    fun stop() {
        stateListener?.isListening(false)
        speechRecognizer.stopListening()
    }

//    fun getSpeechRecognizer():SpeechRecognizer{
//        return speechRecognizer
//    }

//    fun getIntent():Intent{
//        return intent
//    }

    fun destroy() {
        try {
            speechRecognizer.destroy()
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }
}

