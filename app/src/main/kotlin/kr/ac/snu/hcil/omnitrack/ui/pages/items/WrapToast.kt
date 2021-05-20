package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Activity
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import kr.ac.snu.hcil.omnitrack.R
import androidx.core.os.HandlerCompat.postDelayed
import android.os.Handler


fun Toast.showCustomToast(message: String, length: Int, activity: Activity)  {
    val layout = activity.layoutInflater.inflate (
            R.layout.custom_toast_layout,
            activity.findViewById(R.id.toast_container)
    )

    // set the text of the TextView of the message
    val textView = layout.findViewById<TextView>(R.id.toast_text)
    textView.text = message

    // use the application extension function 
    this.apply {
        setGravity(Gravity.BOTTOM, 0, 240)
        duration = length
        view = layout
        show()
    }
}

fun Toast.showErrorToast(message: String, length: Int, activity: Activity)  {
    val layout = activity.layoutInflater.inflate (
            R.layout.error_toast_layout,
            activity.findViewById(R.id.toast_container)
    )

    // set the text of the TextView of the message
    val textView = layout.findViewById<TextView>(R.id.toast_text)
    textView.text = message

    // use the application extension function
    this.apply {
        setGravity(Gravity.BOTTOM, 0, 240)
        duration = length
        view = layout
        show()
    }
}

fun Toast.showShortToast(message: String, length: Long, activity: Activity)  {
    val layout = activity.layoutInflater.inflate (
            R.layout.custom_toast_layout,
            activity.findViewById(R.id.toast_container)
    )

    // set the text of the TextView of the message
    val textView = layout.findViewById<TextView>(R.id.toast_text)
    textView.text = message

    // use the application extension function
    this.apply {
        setGravity(Gravity.BOTTOM, 0, 240)
        duration = Toast.LENGTH_SHORT
        view = layout
        show()
    }

    // hide the toast sooner
    val handler = Handler()
    handler.postDelayed( Runnable { this.cancel() }, length)
}