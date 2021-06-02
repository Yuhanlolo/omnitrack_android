package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment
import kr.ac.snu.hcil.omnitrack.R
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.activity_guide.*

/**
 * Created by Yuhan Luo on 2021. 5. 30.
 */

class GuideDialogFragment: DialogFragment() {

    private var layout: View? = null
    private var guideExample: TextView? = null
    private var textFieldPrompt: TextView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        getDialog()!!.getWindow().setBackgroundDrawableResource(R.drawable.round_corner)
        layout = inflater.inflate(R.layout.activity_guide, container, false)
        return layout
    }

    override fun show(manager: FragmentManager, tag: String?) {
        super.show(manager, tag)
    }

    fun updateText (message: String){
        guideExample!!.setText(Html.fromHtml(message))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        guide_cancel_button!!.setOnClickListener (object : View.OnClickListener {
            override fun onClick(view: View?) {
                dismiss()
            }
        })

        guideExample = layout!!.findViewById<TextView>(R.id.global_speech_prompt)
        textFieldPrompt = layout!!.findViewById<TextView>(R.id.global_speech_textfield_prompt)

        updateText(arguments?.getString("prompt")!!)

        val textFieldName = arguments?.getString("textfield")!!
        if(!textFieldName.equals("")){
            textFieldPrompt!!.setText("You can record any information in $textFieldName")
        }
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
        val height = (resources.displayMetrics.heightPixels)
        dialog!!.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
