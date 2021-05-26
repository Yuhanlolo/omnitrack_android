package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import kr.ac.snu.hcil.omnitrack.R
import org.jetbrains.anko.layoutInflater
import android.os.Handler


class CustomToast(context: Context?, activity: Activity) : Toast(context) {

    var textStr: String = ""
    var durationLong: Long = 1000
    val ctx = context
    val activity = activity

    fun setText(message: String){
        val layout = activity.layoutInflater.inflate (
                R.layout.custom_toast_layout,
                activity.findViewById(R.id.toast_container)
        )

        val textView = layout.findViewById<TextView>(R.id.toast_text)
        textView.setText(textStr)
    }

    fun setLayout (length: Int, activity: Activity) {

        val layout = activity.layoutInflater.inflate (
                R.layout.custom_toast_layout,
                activity.findViewById(R.id.toast_container)
        )

        // set the text of the TextView of the message
        val textView = layout.findViewById<TextView>(R.id.toast_text)
        textView.setText(textStr)

        // use the application extension function
        this.setGravity(Gravity.BOTTOM, 0, 240)
        this.setDuration(length)
        this.setView(layout)
    }

    fun showErrorToast(length: Int, activity: Activity)  {
        val layout = activity.layoutInflater.inflate (
                R.layout.error_toast_layout,
                activity.findViewById(R.id.toast_container)
        )

        // set the text of the TextView of the message
        val textView = layout.findViewById<TextView>(R.id.toast_text)
        textView.text = textStr

        // use the application extension function
        this.apply {
            setGravity(Gravity.BOTTOM, 0, 240)
            duration = length
            view = layout
            show()
        }
    }


    fun showCustomToast() {

        // use the application extension function
        this.apply {
            show()
        }

        // hide the toast sooner
        val handler = Handler()
        handler.postDelayed( Runnable { this.cancel() }, durationLong)
    }
}