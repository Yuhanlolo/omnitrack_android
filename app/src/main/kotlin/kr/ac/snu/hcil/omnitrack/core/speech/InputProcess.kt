package kr.ac.snu.hcil.omnitrack.core.speech

import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import android.content.Context
import androidx.work.Operation
import kr.ac.snu.hcil.android.common.containers.AnyInputModalitywithResult
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

class InputProcess (val context: Context, inputView: AFieldInputView <out Any>){

    var errorMessage = ""
    val inputView = inputView
    var allDataFilled: String? = null

    var successStatus = -1
    val DATA_FILLED_SUCCESS = 1
    val DATA_FILLED_FAILED = 0
    val GLOBAL_SPEECH_MARK = "GLOBAL_SPEECH"

    /* Process the speech input of different data fields */
    fun passInput (inputStr: String, field: OTFieldDAO?): Any?{
        var fieldValue: Any? = ""
         when (inputView.typeId) {
             (AFieldInputView.VIEW_TYPE_NUMBER) -> {
                 fieldValue = WordsToNumber(inputStr).getNumber()
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
                 fieldValue = StrToChoice(inputStr).getChoiceIds(context, field!!)
                 errorMessage = "Sorry, the system couldn't match your input with existing options. " +
                         "Please mention one of the options in ${field.name}"
             }
             (AFieldInputView.VIEW_TYPE_RATING_STARS) -> {
                 val wordToNumber = WordsToNumber(inputStr)
                 fieldValue = wordToNumber.getRating(context, field!!)
                 if(wordToNumber.outofRange){
                     val range  = wordToNumber.getRange(context, field!!)
                     errorMessage = "Rating number out of range. " +
                             "Please say a number between 1 to $range in ${field.name}."
                 }else{
                     errorMessage = "Sorry, the system couldn't detect rating numbers. " +
                             "Please include number information in ${field.name}."
                 }
             }
             (AFieldInputView.VIEW_TYPE_RATING_LIKERT) -> {
                 val wordToNumber = WordsToNumber(inputStr)
                 fieldValue = wordToNumber.getRating(context, field!!)
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
                            fieldValue = WordsToNumber(seg).getNumber()
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
                    fieldValue = StrToChoice(inputwithPunct).getChoiceIds(context, field!!)
                   // println("fieldValue: $fieldValue")
                   // errorMessage = "Sorry, the system couldn't detect existing options"
                }
                (OTFieldManager.TYPE_RATING) -> {
                    for (seg in sentenceSeg){
                        if(StrCompareHelper().isMatch(seg, fieldName) || StrCompareHelper().ratingOrStar(seg)){
                            val wordToNumber = WordsToNumber(seg)
                            fieldValue = wordToNumber.getRating(context, field!!)
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

//        recordList.add(AnyInputModalitywithResult(GLOBAL_SPEECH_MARK, -1, true, successStatus, inputwithPunct))
//        return recordList

    }

    fun uiUpdate (){

    }

    //TODO: handling speech recognition error
    fun RecognitionError (inputModality: AnyInputModalitywithResult){

    }

//    fun sendRequestToPunctuator(inputStr: String, currentAttributeViewModelList: ArrayList<ItemEditionViewModelBase.AttributeInputViewModel>, recordList: MutableList<AnyInputModalitywithResult>): MutableList<AnyInputModalitywithResult>{
//        val url = URL("http://bark.phon.ioc.ee/punctuator")
//        val requestParam = "text=$inputStr"
//        var inputWithPunct: String = inputStr
//
//
//        val http = url.openConnection() as HttpURLConnection
//        http.setReadTimeout(1000)
//        http.setConnectTimeout(1500)
//        http.requestMethod = "POST"
//        http.doOutput = true
//        http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
//
//        doAsync {
//        try {
//            val os = http.getOutputStream()
//            val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
//            writer.write(requestParam)
//            writer.flush()
//            writer.close()
//
//            inputWithPunct = http.inputStream.bufferedReader().readText()
//
//            println("http request responses: $inputWithPunct, ${http.responseCode},  ${http.responseMessage}")
//
//        } catch (exception: Exception) {
//            successStatus = NETWOKR_ERR
//            println("http request exception: $exception, ${http.responseCode}, ${http.responseMessage}")
//        } finally {
//            http.disconnect()
//        }
//
//            uiThread {
//
//                passGlobalInput(inputWithPunct, currentAttributeViewModelList)
//                recordList.add(AnyInputModalitywithResult(GLOBAL_SPEECH_MARK, -1, true, successStatus, inputStr))
//            }
//
//        }
//
//        return recordList
//    }



//    private fun openSentenceStream(inputStr: String){
//        //Apex.init(Apex.ApexBuilder().addParser("en", EnglishParser()).build())
//        //val processText = Apex.nlp("en", inputStr)
//
//        //Loading sentence detector model
//        var inputStream: InputStream? = null
//        var model: SentenceModel? = null
//        var sentences: Array<String>? = null
//        var sentencesIndex: Array<Span>? = null
//        try {
//            inputStream = context.getAssets().open("en-sent.bin")
//            model = SentenceModel(inputStream)
//
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//
//        //Instantiating the SentenceDetectorME class
//        if (model != null) {
//            val detector = SentenceDetectorME(model)
//
//            //Detecting the sentence
//            sentences = detector.sentDetect(inputStr)
//            sentencesIndex = detector.sentPosDetect(inputStr)
//        }
//
//        if (sentences != null) {
//            for (sentence in sentences){
//                println ("sentences: $sentence")
//            }
//        }
//
//    }


}
