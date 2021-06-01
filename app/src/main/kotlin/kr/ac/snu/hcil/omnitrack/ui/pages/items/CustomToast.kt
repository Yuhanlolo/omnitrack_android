package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import kr.ac.snu.hcil.omnitrack.R
import android.os.Handler
import android.view.View
import android.widget.LinearLayout


class CustomToast(context: Context?, activity: Activity, error: Boolean) : Toast(context) {

    val activity = activity
    var layout: View? = null
    var textView: TextView? = null
    var toastContainer: LinearLayout? = null

    init{
        layout = activity.layoutInflater.inflate (
                R.layout.custom_toast_layout,
                activity.findViewById(R.id.toast_container)
        )

        toastContainer = layout!!.findViewById(R.id.custom_toast_container)
        textView = layout!!.findViewById<TextView>(R.id.toast_text)

        if(error){
            toastContainer!!.setBackgroundColor(Color.parseColor("#FE4949"))
        }

        this.setGravity(Gravity.CENTER, 0, 0)
        this.setDuration(LENGTH_LONG)
        this.setView(layout)
        this.textView!!.setText("Listening ...")
    }

    fun textUpdate(message: String){
        if (!message.equals(""))
        this.textView!!.setText(message)
    }

    fun showCustomToast(duraton: Long) {
        this.show()
        // hide the toast sooner
        val handler = Handler()
        handler.postDelayed( Runnable { this.cancel() }, duraton)
    }
}