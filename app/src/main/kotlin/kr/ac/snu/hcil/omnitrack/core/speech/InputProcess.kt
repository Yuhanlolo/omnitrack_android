package kr.ac.snu.hcil.omnitrack.core.speech

import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import android.content.Context
import androidx.core.text.isDigitsOnly
import java.math.BigDecimal
import java.util.StringTokenizer
import java.text.NumberFormat
import java.util.Locale

/**
 * Created by Yuhan Luo on 21. 4. 2
 */

class InputProcess (val context: Context) {

    /* Process the speech input of different data fields
    *  If the field type is single or multiple choice, also need to pass the field option list */
    fun passInput (fieldID:Int, inputStr: String, optionList : ArrayList<String> = ArrayList()): Any?{
        var fieldValue: Any? = ""
         when (fieldID) {
             (AFieldInputView.VIEW_TYPE_NUMBER) -> {
                 val words2Num = WordsToNumber()
                 fieldValue = words2Num.getNumber(inputStr)
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


    //TODO: If the field type is single or multiple choice, check whether the speech input matches existing options
    fun matchChoice(optionList : ArrayList<String>){

    }

    //TODO: handling speech recognition error
    fun RecognitionError (){

    }
}
