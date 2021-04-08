package kr.ac.snu.hcil.omnitrack.core.speech

import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO

/**
 * Created by Yuhan Luo on 21. 4. 2
 */

class InputProcess (val context: Context, inputView: AFieldInputView <out Any>) {

    val inputfield = inputView
    val ctx = context

    /* Process the speech input of different data fields */
    fun passInput (inputStr: String, field: OTFieldDAO?): Any?{
        var fieldValue: Any? = ""
         when (inputfield.typeId) {
             (AFieldInputView.VIEW_TYPE_NUMBER) -> {
                 fieldValue = WordsToNumber(inputStr).getNumber()
             }
             (AFieldInputView.VIEW_TYPE_TIME_POINT) -> {

             }
             (AFieldInputView.VIEW_TYPE_TIME_RANGE_PICKER) -> {

             }
             (AFieldInputView.VIEW_TYPE_CHOICE) -> {

             }
             (AFieldInputView.VIEW_TYPE_RATING_STARS) -> {
                 fieldValue = WordsToNumber(inputStr).getRating(context, field!!)
             }
             (AFieldInputView.VIEW_TYPE_RATING_LIKERT) -> {
                 fieldValue = WordsToNumber(inputStr).getRating(context, field!!)
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
