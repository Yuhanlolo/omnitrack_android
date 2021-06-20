package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import kr.ac.snu.hcil.omnitrack.R
import androidx.fragment.app.FragmentManager
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.fields.OTFieldManager
import android.content.Context
import android.text.Html
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTChoiceFieldHelper
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTRatingFieldHelper
import java.util.Random
import kr.ac.snu.hcil.omnitrack.core.speech.StrCompareHelper
import kr.ac.snu.hcil.omnitrack.core.speech.StrToChoice
import kr.ac.snu.hcil.omnitrack.core.speech.TimeHandler
import kr.ac.snu.hcil.omnitrack.core.speech.WordsToNumber

/**
 * Created by Yuhan Luo on 2021. 5. 31.
 */

class TranscriptDialogFragment: DialogFragment() {

    private var layout: View? = null
    private var transcriptText: TextView? = null
    private var speech_anim: LottieAnimationView? = null

    private var params: WindowManager.LayoutParams? = null
    private var xPos: Int = 0
    private var yPos: Int = 300
    private var dialogGravity: Int = Gravity.BOTTOM


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
        //transcriptText!!.gravity = Gravity.CENTER_HORIZONTAL
        params = dialog!!.window!!.attributes

        speech_anim = layout!!.findViewById<LottieAnimationView>(R.id.ui_speech_anim_dialog)
        speech_anim!!.playAnimation()

        displayExamples(arguments?.getString("prompt"))

    }

    override fun dismiss() {
        speech_anim!!.pauseAnimation()
        super.dismiss()
    }

    fun updateTextLive (transcript: String){
        transcriptText!!.setTextColor(Color.parseColor("#808080"))
        transcriptText!!.setText(transcript)
    }

    fun updatePosition (xOffSet: Int, yOffSet: Int){
        dialogGravity = Gravity.TOP
        xPos = xOffSet
        yPos = yOffSet
    }

    fun resetPosition (){
        xPos = 0
        yPos = 300
        dialogGravity = Gravity.BOTTOM
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.75).toInt()
        val height = (resources.displayMetrics.heightPixels)

        params!!.gravity = dialogGravity
        params!!.x = xPos
        params!!.y = yPos

        dialog!!.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun displayExamples (prompt: String?){
        transcriptText!!.setTextColor(Color.parseColor("#42C595"))
        if(prompt != null)
        transcriptText!!.setText(Html.fromHtml(prompt))
    }

}
