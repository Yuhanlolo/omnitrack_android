package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import kr.ac.snu.hcil.omnitrack.R
import androidx.fragment.app.FragmentManager
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import android.os.Handler
import android.text.Html


/**
 * Created by Yuhan Luo on 2021. 7. 31.
 */

class ErrorMsgFragment: DialogFragment() {

    private var layout: View? = null
    private var errorText: TextView? = null

    private var params: WindowManager.LayoutParams? = null
    private var xPos: Int = 0
    private var yPos: Int = 300
    private var dialogGravity: Int = Gravity.BOTTOM


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        getDialog()!!.getWindow().setBackgroundDrawableResource(R.drawable.error_container)
        layout = inflater.inflate(R.layout.error_msg_layout, container, false)

        return layout
    }

    override fun show(manager: FragmentManager, tag: String?) {
        super.show(manager, tag)

        val handler = Handler()
        handler.postDelayed( Runnable { this.dismiss() }, 3000)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        errorText = layout!!.findViewById<TextView>(R.id.error_text)
        params = dialog!!.window!!.attributes

        displayMsg(arguments?.getString("msg"))
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

    fun displayMsg (prompt: String?){
        if(prompt != null)
            errorText!!.setText(Html.fromHtml(prompt))
    }
}
