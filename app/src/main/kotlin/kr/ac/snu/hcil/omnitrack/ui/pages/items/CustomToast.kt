package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import kr.ac.snu.hcil.omnitrack.R
import android.os.Handler
import android.view.View


class CustomToast(context: Context?, activity: Activity) : Toast(context) {

    val activity = activity
    var layout: View? = null
    var textView: TextView? = null

    init{
        layout = activity.layoutInflater.inflate (
                R.layout.custom_toast_layout,
                activity.findViewById(R.id.toast_container)
        )
        textView = layout!!.findViewById<TextView>(R.id.toast_text)

        this.setGravity(Gravity.BOTTOM, 0, 260)
        this.setDuration(LENGTH_LONG)
        this.setView(layout)
        this.textView!!.setText("Listening ...")
    }

    fun textUpdate(message: String){
        if (!message.equals(""))
        this.textView!!.setText(message)
    }

    fun showCustomToast(duraiton: Long) {
        this.show()
        // hide the toast sooner
        val handler = Handler()
        handler.postDelayed( Runnable { this.cancel() }, duraiton)
    }
}