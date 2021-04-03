package kr.ac.snu.hcil.omnitrack.core.speech

import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import android.content.Context

/**
 * Created by Yuhan Luo on 21. 4. 2
 */

class InputProcess (val context: Context) {

    /* Process the speech input of different data fields*/
    fun passInput (fieldID:Int, inputStr: String): Any?{
        var fieldValue: Any? = ""
         when (fieldID) {
             (AFieldInputView.VIEW_TYPE_NUMBER) -> {

             }
             (AFieldInputView.VIEW_TYPE_TIME_POINT) -> {

             }
             (AFieldInputView.VIEW_TYPE_TIME_RANGE_PICKER) -> {

             }
             (AFieldInputView.VIEW_TYPE_CHOICE) -> {

             }
             (AFieldInputView.VIEW_TYPE_RATING_STARS) -> {

             }
             (AFieldInputView.VIEW_TYPE_RATING_LIKERT) -> {

             }
            (AFieldInputView.VIEW_TYPE_SHORT_TEXT) -> {
                fieldValue = inputStr
            }
            (AFieldInputView.VIEW_TYPE_LONG_TEXT) -> {
                fieldValue = inputStr
            }
        }
        return fieldValue
    }

}
