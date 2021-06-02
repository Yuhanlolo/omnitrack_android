package kr.ac.snu.hcil.omnitrack.core.speech

import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import android.content.Context
import android.graphics.Color
import androidx.work.Operation
import kr.ac.snu.hcil.android.common.containers.AnyInputModalitywithResult
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.fields.OTFieldManager
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemEditionViewModelBase
import java.util.ArrayList
import java.net.URL
import java.net.HttpURLConnection
import java.io.OutputStreamWriter
import java.io.BufferedWriter
import java.lang.Exception
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * Created by Yuhan Luo on 21. 4. 2
 */

class InputProcess (context: Context, inputView: AFieldInputView <out Any>){

    var errorMessage = ""
    val inputView = inputView
    var allDataFilled: String? = null
    val context = context

    var successStatus = -1
    val DATA_FILLED_SUCCESS = 1
    val DATA_FILLED_FAILED = 0

    val MAX_CHAR_PER_LINE = 40
    val MAX_LINES = 3

    /* Process the speech input of different data fields */
    fun passInput (inputStr: String, field: OTFieldDAO?): Any?{
        var fieldValue: Any? = ""
         when (inputView.typeId) {
             (AFieldInputView.VIEW_TYPE_NUMBER) -> {
                 fieldValue = WordsToNumber().getNumber(inputStr)
                 errorMessage =  "Sorry, the system couldn't detect numbers. " +
                         "Please include number information in ${field!!.name}"
             }
             (AFieldInputView.VIEW_TYPE_TIME_POINT) -> {
                 fieldValue = TimeHandler().getTimePoint(inputStr)
                 errorMessage = "Sorry, the system couldn't detect any time point. " +
                         "Please include time information such as \" 7 am in the morning\" or \" two hours ago \" in ${field!!.name}"
             }
             (AFieldInputView.VIEW_TYPE_TIME_RANGE_PICKER) -> {
                 fieldValue = TimeHandler().getTimeDuration(inputStr)
                 errorMessage = "Sorry, the system couldn't detect time span information. " +
                         "Please include start and end time points in ${field!!.name}"
             }
             (AFieldInputView.VIEW_TYPE_CHOICE) -> {
                 fieldValue = StrToChoice().getChoiceIds(context, field!!, inputStr)
                 errorMessage = "Sorry, the system couldn't match your input with existing options. " +
                         "Please mention one of the options in ${field.name}"
             }
             (AFieldInputView.VIEW_TYPE_RATING_STARS) -> {
                 val wordToNumber = WordsToNumber()
                 fieldValue = wordToNumber.getRating(context, field!!, inputStr)
                 if(wordToNumber.outofRange){
                     val range  = wordToNumber.getRange(context, field!!)
                     errorMessage = "Rating number out of range. " +
                             "Please say a number between ${range?.get(0)} to ${range?.get(1)} in ${field.name}."
                 }else{
                     errorMessage = "Sorry, the system couldn't detect rating numbers. " +
                             "Please include number information in ${field.name}."
                 }
             }
             (AFieldInputView.VIEW_TYPE_RATING_LIKERT) -> {
                 val wordToNumber = WordsToNumber()
                 fieldValue = wordToNumber.getRating(context, field!!, inputStr)
                 if(wordToNumber.outofRange){
                     val range  = wordToNumber.getRange(context, field!!)
                     errorMessage = "Rating number out of range. " +
                             "Please say a number between 1 to $range in ${field.name}."
                 }else{
                     errorMessage = "Sorry, the system couldn't detect rating numbers. " +
                             "Please include number information in ${field.name}."
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


    fun passGlobalInput (inputwithPunct: String, currentAttributeViewModelList: ArrayList<ItemEditionViewModelBase.AttributeInputViewModel>): Int{

        val sentenceSeg = inputwithPunct.split(".", "?", "!")
        errorMessage = ""

        println("http responses sentenceSeg: $sentenceSeg")

        for (viewModel in currentAttributeViewModelList){
            var fieldValue: Any? = null
            val field: OTFieldDAO = viewModel.fieldDAO
            val fieldName = field.name
            //val match = currentAttributeViewModelList.find { it.fieldLocalId == field.localId }

            when (field.type) {
                (OTFieldManager.TYPE_NUMBER) -> {
                    for (seg in sentenceSeg){
                        if(StrCompareHelper().isMatch(seg, fieldName)){
                            fieldValue = WordsToNumber().getNumber(seg)
                            break
                        }
                    }
                }
                (OTFieldManager.TYPE_TIME) -> {
                    fieldValue = TimeHandler().getTimePoint(inputwithPunct)
                }
                (OTFieldManager.TYPE_TIMESPAN) -> {
                    fieldValue = TimeHandler().getTimeDuration(inputwithPunct)
                    //errorMessage = "Sorry, the system couldn't detect time range information"
                }
                (OTFieldManager.TYPE_CHOICE) -> {
                    //if(StrCompareHelper().isMatch(inputwithPunct, fieldName))
                    fieldValue = StrToChoice().getChoiceIds(context, field!!, inputwithPunct)
                   // println("fieldValue: $fieldValue")
                   // errorMessage = "Sorry, the system couldn't detect existing options"
                }
                (OTFieldManager.TYPE_RATING) -> {
                    for (seg in sentenceSeg){
                        if(StrCompareHelper().isMatch(seg, fieldName) || StrCompareHelper().ratingOrStar(seg)){
                            val wordToNumber = WordsToNumber()
                            fieldValue = wordToNumber.getRating(context, field!!, seg)
                            break
                        }
                    }
                }
                (OTFieldManager.TYPE_TEXT) -> {
                    for (seg in sentenceSeg){
                       // println("field type name: $fieldName, seg: $seg, ismatch: ${StrCompareHelper().isMatch(seg, fieldName)}")
                        if(StrCompareHelper().isMatch(seg, fieldName)){
                            fieldValue = seg
                            break
                        }
                    }
                }
            }

            // println ("field type: ${field.type}, field name: $fieldName, field value: ${fieldValue.toString()}, filled or not: ${viewModel.isFilled}")

            if (fieldValue != null && !viewModel.isFilled){
                viewModel.setValueOnly(field.localId, fieldValue)
                allDataFilled += "1"
            }else{
                allDataFilled += "0"
            }
        }

        if(!allDataFilled!!.contains("1")){
            successStatus = DATA_FILLED_FAILED
            errorMessage  = "Sorry, the system couldn't match your input to existing data fields. Please try to include any field names in your input as keywords."

        }else {
            successStatus = DATA_FILLED_SUCCESS
        }

        return successStatus
    }

    fun displayExamples (context: Context, field: OTFieldDAO?): String {

//        if (field == null){
//            return ""
//        }

        var promptMessage = ""
        when (field!!.type) {
            (OTFieldManager.TYPE_NUMBER) -> {
                promptMessage = "Say a <b>number</b>."
            }
            (OTFieldManager.TYPE_TIME) -> {
                promptMessage = "Say a time point such as \"<b>7 am today</b>\" or \"<b>two hours ago</b>\"."
            }
            (OTFieldManager.TYPE_TIMESPAN) -> {
                promptMessage = "Say a time span with start and end time points such as \"<b>from 9 to 10 am</b>\"."
            }
            (OTFieldManager.TYPE_CHOICE) -> {
                promptMessage = "Say a choice such as <b>${StrToChoice().getARandomChoice(context, field)}</b>."
            }
            (OTFieldManager.TYPE_RATING) -> {
                promptMessage ="Give a rating from <b>${WordsToNumber().getRange(context, field)?.get(0)}</b> to <b>${WordsToNumber().getRange(context, field)?.get(1)}</b>."
            }
            (OTFieldManager.TYPE_TEXT) -> {
                promptMessage = "Say anything you want."
            }
        }

        return promptMessage
    }

    // Reference: https://github.com/umdsquare/data-at-hand-mobile/blob/b36c3a00aadcf003da254f7c9826dcb2013c4115/android/app/src/main/java/com/dataathand/speech/ASpeechToTextModule.java#L95
    fun joinTexts (left: String?, right: String?): String?{

        var resStr: String? = null

        if (left != null && right == null) {
            resStr = left
        } else if (left == null && right != null) {
            resStr = right
        } else if (left != null && right != null) {
            return (left + " " + right).trim().replace("\\s+", " ")
        }

        if (resStr != null) {
            val size  = resStr.length
            if (size > MAX_CHAR_PER_LINE * MAX_LINES){
                val tempStr = resStr.substring(size - MAX_CHAR_PER_LINE * MAX_LINES)
                val index = tempStr.indexOf(" ")
                resStr = tempStr.substring(index + 1)
            }
        }

        return resStr
    }

}
