package kr.ac.snu.hcil.omnitrack.core.speech

import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.fields.OTFieldManager
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemEditionViewModelBase
import java.util.ArrayList


/**
 * Created by Yuhan Luo on 21. 4. 2
 */

class InputProcess (val context: Context, inputView: AFieldInputView <out Any>){

    var errorMessage = ""
    val inputView = inputView

    /* Process the speech input of different data fields */
    fun passInput (inputStr: String, field: OTFieldDAO?): Boolean{
        var fieldValue: Any? = ""
         when (inputView.typeId) {
             (AFieldInputView.VIEW_TYPE_NUMBER) -> {
                 fieldValue = WordsToNumber(inputStr).getNumber()
                 errorMessage =  "Sorry, the system couldn't detect numbers"
             }
             (AFieldInputView.VIEW_TYPE_TIME_POINT) -> {
                 fieldValue = TimeHandler().getTimePoint(inputStr)
                 errorMessage = "Sorry, the system couldn't detect any time point"
             }
             (AFieldInputView.VIEW_TYPE_TIME_RANGE_PICKER) -> {
                 fieldValue = TimeHandler().getTimeDuration(inputStr)
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
                     errorMessage = "Rating number out of range"
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

        if (fieldValue != null)
        {
            inputView.setAnyValue (fieldValue)
            return true
        }

        return false
    }


    fun passGlobalInput (inputStr: String, currentAttributeViewModelList: ArrayList<ItemEditionViewModelBase.AttributeInputViewModel>){

        for (viewModel in currentAttributeViewModelList){
            var fieldValue: Any? = null
            val field: OTFieldDAO = viewModel.fieldDAO
            val fieldName = field.name

            when (field.type) {
                (OTFieldManager.TYPE_NUMBER) -> {
                    fieldValue = WordsToNumber(inputStr).getNumber()
                }
                (OTFieldManager.TYPE_TIME) -> {
                    fieldValue = TimeHandler().getTimePoint(inputStr)
                }
                (OTFieldManager.TYPE_TIMESPAN) -> {
                    fieldValue = TimeHandler().getTimeDuration(inputStr)
                    //errorMessage = "Sorry, the system couldn't detect time range information"
                }
                (OTFieldManager.TYPE_CHOICE) -> {
                    //if(StrCompareHelper().isMatch(inputStr, fieldName))
                        fieldValue = StrToChoice(inputStr).getChoiceIds(context, field!!)
                   //errorMessage = "Sorry, the system couldn't detect existing options"
                }
                (OTFieldManager.TYPE_RATING) -> {
                    val wordToNumber = WordsToNumber(inputStr)
                    //fieldValue = wordToNumber.getRating(context, field!!)
                }
                (OTFieldManager.TYPE_TEXT) -> {
                    if(StrCompareHelper().isMatch(inputStr, fieldName))
                        fieldValue = inputStr
                }
            }

            println ("field type: ${field.type}, field name: $fieldName, field value: ${fieldValue.toString()}")

            if (fieldValue != null)
                viewModel.setValueOnly(field.localId, fieldValue)
        }
    }

    //TODO: handling speech recognition error
    fun RecognitionError (){

    }

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
