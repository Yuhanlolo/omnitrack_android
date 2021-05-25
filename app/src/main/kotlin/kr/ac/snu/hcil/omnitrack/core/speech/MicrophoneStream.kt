package kr.ac.snu.hcil.omnitrack.core.speech

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import com.microsoft.cognitiveservices.speech.audio.AudioStreamFormat
import com.microsoft.cognitiveservices.speech.audio.PullAudioInputStreamCallback

/**
 * Created by Yuhan Luo on 21. 5. 24
 */

//Forked from https://github.com/microsoft/botframework-solutions/blob/master/samples/android/clients/VirtualAssistantClient/directlinespeech/src/main/java/com/microsoft/bot/builder/solutions/directlinespeech/MicrophoneStream.java
class MicrophoneStream: PullAudioInputStreamCallback() {

    // CONSTANTS
    val SAMPLE_RATE: Long = 16000

    // STATE
    var audioFormat: AudioStreamFormat? = null
    var recorder: AudioRecord? = null

    init {
        this.audioFormat = AudioStreamFormat.getWaveFormatPCM(SAMPLE_RATE, 16, 1)
        this.initMic()
    }

//    fun getAudioFormat(): AudioStreamFormat? {
//        return this.audioFormat
//    }

    override fun read(bytes: ByteArray): Int {
        val ret = this.recorder!!.read(bytes, 0, bytes.size)
        return ret
    }


    override fun close() {
        this.recorder!!.release()
        this.recorder = null
    }

    private fun initMic() {
        // Note: currently, the Speech SDK support 16 kHz sample rate, 16 bit samples, mono (single-channel) only.
        if (Build.VERSION.SDK_INT >= 23) {
            val af: AudioFormat = AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE.toInt())
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build()

            this.recorder = AudioRecord.Builder ()
                    .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                    .setAudioFormat(af)
                    .build();
        } else {
            this.recorder = AudioRecord (
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE.toInt(),
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    AudioRecord.getMinBufferSize(SAMPLE_RATE.toInt(),
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT))
        }

        this.recorder!!.startRecording()
    }
}

