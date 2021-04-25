package kr.ac.snu.hcil.omnitrack.core.speech

import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.types.Fraction

/**
 * Created by Yuhan Luo on 21. 4. 2
 */

class InputProcess (val context: Context, inputView: AFieldInputView <out Any>) {

    val inputfield = inputView
    var errorMessage = ""

    /* Process the speech input of different data fields */
    fun passInput (inputStr: String, field: OTFieldDAO?): Any?{
        var fieldValue: Any? = ""
         when (inputfield.typeId) {
             (AFieldInputView.VIEW_TYPE_NUMBER) -> {
                 fieldValue = WordsToNumber(inputStr).getNumber()
                 errorMessage =  "Sorry, the system couldn't detect numbers"
             }
             (AFieldInputView.VIEW_TYPE_TIME_POINT) -> {
                 fieldValue = TimeHandler(inputStr).getTimeInfo()
                 errorMessage = "Sorry, the system couldn't detect any time point"
             }
             (AFieldInputView.VIEW_TYPE_TIME_RANGE_PICKER) -> {
                 errorMessage = "Sorry, the system couldn't detect time range information"
             }
             (AFieldInputView.VIEW_TYPE_CHOICE) -> {
                 fieldValue = StrToChoice(inputStr).getChoiceIds(context, field!!)
                 errorMessage = "Sorry, the system couldn't detect existing options"
             }
             (AFieldInputView.VIEW_TYPE_RATING_STARS) -> {
                 val wordToNumber = WordsToNumber(inputStr)
                 fieldValue = wordToNumber.getRating(context, field!!)
                 if(wordToNumber.outofRange){
                     errorMessage = "Rating number out of range"
                 }else{
                     errorMessage = "Sorry, the system couldn't detect rating numbers"
                 }
             }
             (AFieldInputView.VIEW_TYPE_RATING_LIKERT) -> {
                 val wordToNumber = WordsToNumber(inputStr)
                 fieldValue = wordToNumber.getRating(context, field!!)
                 if(wordToNumber.outofRange){
                     errorMessage = "Sorry, the system couldn't detect Rating number out of range"
                 }else{
                     errorMessage = "Sorry, the system couldn't detect rating numbers"
                 }
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

    //TODO: handling speech recognition error
    fun RecognitionError (){

    }
}
