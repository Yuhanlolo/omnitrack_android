package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment
import kr.ac.snu.hcil.omnitrack.R
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView

/**
 * Created by Yuhan Luo on 2021. 5. 31.
 */

class TranscriptDialogFragment: DialogFragment() {

    private var layout: View? = null
    private var transcriptText: TextView? = null
    private var speech_anim: LottieAnimationView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        getDialog()!!.getWindow().setBackgroundDrawableResource(R.drawable.round_corner_background)
        layout = inflater.inflate(R.layout.transcript_layout, container, false)
        return layout
    }

    override fun show(manager: FragmentManager, tag: String?) {
        super.show(manager, tag)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        transcriptText = layout!!.findViewById<TextView>(R.id.transcripts)
        speech_anim = layout!!.findViewById<LottieAnimationView>(R.id.ui_speech_anim_dialog)
        speech_anim!!.playAnimation()
    }

    override fun dismiss() {
        super.dismiss()
        speech_anim!!.pauseAnimation()
    }

    fun updateTextLive (transcript: String){
        if (!transcript.equals(""))
            transcriptText!!.setText(transcript)
        else
            transcriptText!!.setText("Listening ...")
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.75).toInt()
        val height = (resources.displayMetrics.heightPixels)
        dialog!!.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
