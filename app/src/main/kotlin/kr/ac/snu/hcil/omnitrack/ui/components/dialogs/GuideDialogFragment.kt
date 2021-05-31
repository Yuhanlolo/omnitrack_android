package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment
import kr.ac.snu.hcil.omnitrack.R
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.activity_guide.*

/**
 * Created by Yuhan Luo on 2021. 5. 30.
 */

class GuideDialogFragment: DialogFragment() {

    //private val cancelButton: Button by bindView (R.id.guide_cancel_button)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        getDialog()!!.getWindow().setBackgroundDrawableResource(R.drawable.round_corner)
        return inflater.inflate(R.layout.activity_guide, container, false)
    }

    override fun show(manager: FragmentManager, tag: String?) {
        super.show(manager, tag)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        guide_cancel_button!!.setOnClickListener (object : View.OnClickListener {
            override fun onClick(view: View?) {
                dismiss()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
        val height = (resources.displayMetrics.heightPixels)
        dialog!!.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
