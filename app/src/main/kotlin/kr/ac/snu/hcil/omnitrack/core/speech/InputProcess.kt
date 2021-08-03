package kr.ac.snu.hcil.omnitrack.core.speech

import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.fields.OTFieldManager
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemEditionViewModelBase
import java.util.ArrayList
import smile.nlp.tokenizer.SimpleSentenceSplitter

/**
 * Created by Yuhan Luo on 21. 4. 2
 */

class InputProcess (context: Context, inputView: AFieldInputView <out Any>?){

    var errorMessage = ""
    val inputView = inputView
    var allDataFilled: String? = null
    val context = context

    var successStatus = -1
    val DATA_FILLED_SUCCESS = 1
    val DATA_FILLED_FAILED = 0

    val MAX_CHAR_PER_LINE = 35
    val MAX_LINES = 4

    /* Process the speech input of different data fields */
    fun passInput (inputStr: String, field: OTFieldDAO?): Any?{
        var fieldValue: Any? = ""
         when (inputView!!.typeId) {
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
                             "Please say a number between ${formatDataToString(range?.get(0)!!)} " +
                             "to ${formatDataToString(range?.get(1)!!)} in ${field.name}"
                 }else{
                     errorMessage = "Sorry, the system couldn't detect rating numbers. " +
                             "Please say a number between ${formatDataToString(WordsToNumber().getRange(context, field)?.get(0)!!)} " +
                             "to ${formatDataToString(WordsToNumber().getRange(context, field)?.get(1)!!)} in ${field.name}"
                 }
             }
             (AFieldInputView.VIEW_TYPE_RATING_LIKERT) -> {
                 val wordToNumber = WordsToNumber()
                 fieldValue = wordToNumber.getRating(context, field!!, inputStr)
                 if(wordToNumber.outofRange){
                     errorMessage = "Rating number out of range. " +
                             "Please say a number between ${formatDataToString(WordsToNumber().getRange(context, field)?.get(0)!!)} " +
                             "to ${formatDataToString(WordsToNumber().getRange(context, field)?.get(1)!!)} in ${field.name}"
                 }else{
                     errorMessage = "Sorry, the system couldn't detect Likert scale numbers. " +
                             "Please say a number between ${formatDataToString(WordsToNumber().getRange(context, field)?.get(0)!!)} " +
                             "to ${formatDataToString(WordsToNumber().getRange(context, field)?.get(1)!!)} in ${field.name}"
                 }
             }
            (AFieldInputView.VIEW_TYPE_SHORT_TEXT) -> {
                    fieldValue = inputStr
            }
            (AFieldInputView.VIEW_TYPE_LONG_TEXT) -> {
                if (inputView.value != null)
                    fieldValue = inputView.value.toString() + inputStr
                else
                    fieldValue = inputStr
            }
        }

        if (fieldValue == null)
            successStatus = DATA_FILLED_FAILED
        else
            successStatus = DATA_FILLED_SUCCESS

       return fieldValue
    }


    fun passGlobalInput (sentences: String, currentAttributeViewModelList: ArrayList<ItemEditionViewModelBase.AttributeInputViewModel>): Int{

        val sentenceList = sentences.split(".", "?", "!")
        //val sentenceList = (sentenceBreak(sentences)).toCollection(ArrayList())
        errorMessage = ""

        //println("http responses sentenceList: $sentenceList")

        for (viewModel in currentAttributeViewModelList){
            var fieldValue: Any? = null
            val field: OTFieldDAO = viewModel.fieldDAO
            val fieldName = field.name

            when (field.type) {
                (OTFieldManager.TYPE_NUMBER) -> {
                    for (seg in sentenceList){
                        if(StrCompareHelper().isMatch(fieldName, seg)){
                            fieldValue = WordsToNumber().getNumber(seg)
                            break
                        }
                    }
                }
                (OTFieldManager.TYPE_TIME) -> {
                    fieldValue = TimeHandler().getTimePoint(sentences)
                }
                (OTFieldManager.TYPE_TIMESPAN) -> {
                    fieldValue = TimeHandler().getTimeDuration(sentences)
                }
                (OTFieldManager.TYPE_CHOICE) -> {
                    val choiceKeywords = locationAmbiguity(fieldName, sentences)
                    fieldValue = StrToChoice().getChoiceIds(context, field, choiceKeywords)
                }
                (OTFieldManager.TYPE_RATING) -> {
                    for (seg in sentenceList){
                        if(StrCompareHelper().isMatch(fieldName, seg) || StrCompareHelper().ratingOrStar(seg) || StrCompareHelper().productivityOrFeelingRating(fieldName, seg)){
                            val wordToNumber = WordsToNumber()

                            if (productivityVSFeeling(fieldName, seg))
                                fieldValue = wordToNumber.getRating(context, field!!, seg)

                            break
                        }
                    }
                }
                (OTFieldManager.TYPE_TEXT) -> {
//                    for (seg in sentenceList){
//                        //println("field type name: $fieldName, seg: $seg, ismatch: ${StrCompareHelper().isMatch(fieldName, seg)}")
//                        if(StrCompareHelper().isMatch(fieldName, seg)){
//                            fieldValue = taskText(fieldName, seg)
//                            break
//                        }

                        if (taskText(fieldName, sentences) != null)
                            fieldValue = taskText(fieldName, sentences)
                        else if (productivityReason(fieldName, sentences) != null)
                            fieldValue = productivityReason(fieldName, sentences)
                        else if (feelingReason(fieldName, sentences) != null)
                            fieldValue = feelingReason(fieldName, sentences)
                        else if (breakText(fieldName, sentences) != null)
                            fieldValue = breakText(fieldName, sentences)
                   // }
                }
            }

            /*println ("field type: ${field.type}, field name: $fieldName, field value: ${fieldValue.toString()}, filled or not: ${viewModel.isFilled}")
            * &&viewModel.isFilled */

            if (fieldValue != null){
                viewModel.setValueOnly(field.localId, fieldValue)
                allDataFilled += "1"
            }else{
                allDataFilled += "0"
            }
        }

        if(!allDataFilled!!.contains("1")){
            successStatus = DATA_FILLED_FAILED
            errorMessage  = "Sorry, the system couldn't match your input to existing data fields. " +
                    "Please try to include any field names in your input as keywords."

        }else {
            successStatus = DATA_FILLED_SUCCESS
        }

        return successStatus
    }

    private fun sentenceBreak (inputStr: String): Array<String>{
        var sentences = SimpleSentenceSplitter.getInstance().split(inputStr)
        return sentences
    }

    /* bold the key words/phrase with HTML format */
    fun displayExamplesHTML (field: OTFieldDAO?): String {

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
                promptMessage ="Give a rating from <b>${formatDataToString(WordsToNumber().getRange(context, field)?.get(0)!!)}</b> " +
                        "to <b>${formatDataToString(WordsToNumber().getRange(context, field)?.get(1)!!)}</b>"

                val textRange = WordsToNumber().getRangeText(field.name)
                if (textRange != null){
                    promptMessage += " or from <b>${textRange[0]}</b> to <b>${textRange[1]}</b>"
                }
            }
            (OTFieldManager.TYPE_TEXT) -> {
                promptMessage = "Say anything open-ended."
            }
        }

        return promptMessage
    }

    fun displayExamples (field: OTFieldDAO?): String {

        var promptMessage = ""
        when (field!!.type) {
            (OTFieldManager.TYPE_NUMBER) -> {
                promptMessage = "Say a number such as 1 or 2."
            }
            (OTFieldManager.TYPE_TIME) -> {
                promptMessage = "Say a time point such as \"7 am today\" or \"two hours ago\"."
            }
            (OTFieldManager.TYPE_TIMESPAN) -> {
                promptMessage = "Say a time span with start and end time points such as \"from 9 to 10 am\"."
            }
            (OTFieldManager.TYPE_CHOICE) -> {
                promptMessage = "Say a choice such as ${StrToChoice().getARandomChoice(context, field)}."
            }
            (OTFieldManager.TYPE_RATING) -> {

                promptMessage ="Give a rating from >${formatDataToString(WordsToNumber().getRange(context, field)?.get(0)!!)} " +
                        "to ${formatDataToString(WordsToNumber().getRange(context, field)?.get(1)!!)}"

                val textRange = WordsToNumber().getRangeText(field.name)
                if (textRange != null){
                    promptMessage += " or from <b>${textRange[0]}</b> to <b>${textRange[1]}</b>."
                }
            }
            (OTFieldManager.TYPE_TEXT) -> {
                promptMessage = "Say anything open-ended."
            }
        }

        return promptMessage
    }

    fun displayGlobalSpeechExamplesHTML (currentAttributeViewModelList: ArrayList<ItemEditionViewModelBase.AttributeInputViewModel>, trackerTitle: String): String {

        val field_1 = currentAttributeViewModelList.get(0).fieldDAO
        val field_2 = currentAttributeViewModelList.get(1).fieldDAO

        val fieldName_1 = field_1.name
        val fieldName_2 = field_2.name

        var exampleStr = "I had a cup of tea two hours ago."

        if (trackerTitle.contains("productivity", true))
            exampleStr = "I was working on a job-related task at home from 3 to 5 pm."

        if (trackerTitle.contains("break", true))
            exampleStr = "I took a break from 3 to 3:30 pm to have some coffee."

        return "Say something to capture multiple data fields such as <b>\"$exampleStr\"</b>"

//        for (viewModel in currentAttributeViewModelList){
//            val field: OTFieldDAO = viewModel.fieldDAO
//            val fieldName = field.name
//
//            when (field!!.type) {
//                (OTFieldManager.TYPE_NUMBER) -> {
//                    promptMessage += "$fieldName is <b>${(1 .. 10).random()}</b>.<br/>"
//                }
//                (OTFieldManager.TYPE_TIME) -> {
//                    promptMessage += "$fieldName was <b>two hours ago</b>.<br/>"
//                }
//                (OTFieldManager.TYPE_TIMESPAN) -> {
//                    promptMessage += "$fieldName <b>from 9 to 10 am today</b>.<br/>"
//                }
//                (OTFieldManager.TYPE_CHOICE) -> {
//                    promptMessage += "$fieldName is <b>${StrToChoice().getARandomChoice(context, field)}</b>.<br/>"
//                }
//                (OTFieldManager.TYPE_RATING) -> {
//                    val range = WordsToNumber().getRange(context, field)?.get(1)
//                    val randomNumber = (1 .. range!!.toInt()).random()
//                    if (WordsToNumber().getRatingType(context, field) == RatingOptions.DisplayType.Star)
//                        promptMessage +="$fieldName is <b>${randomNumber} stars</b>.<br/>"
//                    else
//                        promptMessage +="$fieldName is <b>${randomNumber}</b>.<br/>"
//
//                }
//                (OTFieldManager.TYPE_TEXT) -> {
//                    promptMessage += ""
//                }
//            }
//
//            if (count == size -1)
//                promptMessage  = promptMessage.substring(0, promptMessage.length-5)
//            count++
//        }

    }

    private fun randomNumberDistinct (start: Int, end: Int, value: Int): Int{
        var num = (start .. end).random()
        if(num == value)
            num = randomNumberDistinct (start, end, value)
        return num
    }

    fun formatDataToString(d: Double): String {
        return if (d == d.toLong().toDouble())
            String.format("%d", d.toLong())
        else
            String.format("%s", d)
    }

    fun displayGlobalSpeechExamples (currentAttributeViewModelList: ArrayList<ItemEditionViewModelBase.AttributeInputViewModel>, trackerTitle: String): String {

//        val range = currentAttributeViewModelList.size - 1
//        val randomItemIndex_1 = (0 .. range).random()
//        val randomItemIndex_2 = randomNumberDistinct(0, range, randomItemIndex_1)
//
//        val field_1 = currentAttributeViewModelList.get(randomItemIndex_1).fieldDAO
//        val field_2 = currentAttributeViewModelList.get(randomItemIndex_2).fieldDAO
//
//        val fieldName_1 = field_1.name
//        val fieldName_2 = field_2.name


        val field_1 = currentAttributeViewModelList.get(0).fieldDAO
        val field_2 = currentAttributeViewModelList.get(1).fieldDAO
        val field_3 = currentAttributeViewModelList.get(2).fieldDAO

        val fieldName_1 = field_1.name
        val fieldName_2 = field_2.name
        val fieldName_3 = field_3.name

        var exampleStr = "I had a cup of coffee at 9 am."

        if (trackerTitle.contains("productivity", true)){
            exampleStr = "I did some coursework at school from 7 to 9 am."
            return "For example, capture $fieldName_1, $fieldName_2, and $fieldName_3 together by saying \"$exampleStr\""}
        else if (trackerTitle.contains("break", true)){
            exampleStr = "I took a break to meditate from 7 to 7:30 pm."
            return "For example, capture $fieldName_1 and $fieldName_2 together by saying \"$exampleStr\""
        }

        return "For example, capture $fieldName_1 and $fieldName_2 together by saying \"$exampleStr\""
    }


    fun includeTextField (currentAttributeViewModelList: ArrayList<ItemEditionViewModelBase.AttributeInputViewModel>): String?{
        for (viewModel in currentAttributeViewModelList){
            val field: OTFieldDAO = viewModel.fieldDAO
            when (field!!.type) {

                (OTFieldManager.TYPE_TEXT) -> {
                    return field!!.name
                }
            }
        }
        return ""
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

        return textLinCheck(resStr!!) + "_"
    }

    fun joinFinalRes (accumStr: String?, partialStr: String): String{

//        if(accumStr == null || partialStr == null){
//            return joinTexts(accumStr, partialStr)!!
//        }

        var tempStr = partialStr

        if (tempStr.length > 1 && partialStr.lastIndexOf('.') == partialStr.length - 1) {
            tempStr = partialStr.substring(0, partialStr.length - 1)
        }

        if (tempStr.length > 1 && !tempStr.matches("I\\s+".toRegex())) {
            tempStr = tempStr.substring(0, 1).toLowerCase() + tempStr.substring(1)
        }

        if(accumStr == null){
            return tempStr
        }

        val finalStr = joinTexts(accumStr, tempStr)!!.replace("([a-zA-Z])([,.])(\\s+|$)", "$1$3")
                    .replace("(\\s+)(2022)(\\s+)", "$12020 to$3")

        return finalStr

    }

    private fun textLinCheck(originalText: String?): String{

        var resStr = originalText
        if (resStr != null) {
            if (resStr.length > MAX_CHAR_PER_LINE * MAX_LINES){
                val tempStr = resStr.substring(resStr.length - MAX_CHAR_PER_LINE * MAX_LINES)
                val index = tempStr.indexOf(" ")
                resStr = tempStr.substring(index + 1)
            }
        }else{
            resStr = ""
        }

        return resStr

    }

    /* Manually deal with speech input related to productivity and break */
    private fun locationAmbiguity (fieldName: String, inputSentence: String): String {
        if(!fieldName.contains("location", true)){
            if(inputSentence.contains("other place", true))
                return inputSentence.replace("other place", "")

            return inputSentence
        }

        var realLocation = ""

        if (inputSentence.contains("at other place", true) || inputSentence.contains("in other place", true)){
            return "others"
        }

        if (inputSentence.contains("at", true)){
            realLocation = inputSentence.substring(inputSentence.toLowerCase().indexOf("at") + 3 , inputSentence.length)
        }else if (inputSentence.contains("in", true)){
            realLocation = inputSentence.substring(inputSentence.toLowerCase().indexOf("in") + 3 , inputSentence.length)
        }

        return realLocation
    }

    private fun productivityReason (fieldName: String, inputSentence: String): String? {
        if (fieldName.contains("explain", true) && fieldName.contains("productivity", true)){
            if ((inputSentence.contains("productivity", true) || inputSentence.contains("productive", true))){
                if (inputSentence.contains("because", true))
                    return inputSentence.substring(inputSentence.toLowerCase().indexOf("because"), inputSentence.length)
                if (inputSentence.contains("rationale", true))
                    return inputSentence.substring(inputSentence.toLowerCase().indexOf("rationale"), inputSentence.length)
                if (inputSentence.contains("explanation", true))
                    return inputSentence.substring(inputSentence.toLowerCase().indexOf("explanation"), inputSentence.length)
                if (inputSentence.contains("reason", true))
                    return inputSentence.substring(inputSentence.toLowerCase().indexOf("reason"), inputSentence.length)
            }
        }

        return null
    }

    private fun feelingReason (fieldName: String, inputSentence: String): String? {
        if (fieldName.contains("why", true) && fieldName.contains("feel", true)){
            if ((inputSentence.contains("felt", true) || inputSentence.contains("feel", true))
                    && (inputSentence.contains("negative", true) || inputSentence.contains("positive", true)
                            ||inputSentence.contains("this way", true) ||inputSentence.contains("neutral", true))){
                if (inputSentence.contains("because", true))
                    return inputSentence.substring(inputSentence.toLowerCase().indexOf("because"), inputSentence.length)
                if (inputSentence.contains("reason", true))
                    return inputSentence.substring(inputSentence.toLowerCase().indexOf("reason"), inputSentence.length)
            }
        }

        return null
    }

    private fun taskText(fieldName: String, inputSentence: String): String?{
        if (fieldName.contains("task description", true) && inputSentence.contains("task", true)){
            if (inputSentence.contains("includ", true))
                 return inputSentence.substring(inputSentence.toLowerCase().indexOf("includ"), inputSentence.length)
            else if (inputSentence.contains("specific", true))
                return inputSentence.substring(inputSentence.toLowerCase().indexOf("specific"), inputSentence.length)
            else if (inputSentence.length - inputSentence.toLowerCase().indexOf("task") < 10
                        || (inputSentence.length - inputSentence.toLowerCase().indexOf("task") >= 10 && (includeLocationOnly(inputSentence) || includeTime(inputSentence))))
                    return null
            else
                return inputSentence.substring(inputSentence.toLowerCase().indexOf("task"), inputSentence.length)
        }

//        if (fieldName.contains("task description", true) && !isTaskCategoryFilled(currentAttributeViewModelList))
//                return false

            return null
    }

    private fun isFilled (fieldName: String, currentAttributeViewModelList: ArrayList<ItemEditionViewModelBase.AttributeInputViewModel>):Boolean{
        for (viewModel in currentAttributeViewModelList){

            if (fieldName.equals("task category", true)){
                if (viewModel.isFilled)
                    return true
            }
        }

        return false
    }

    private fun includeLocationOnly (input: String):Boolean{
        if ((input.contains("home", true) || input.contains("school", true)
                || input.contains("office", true) || input.contains("other place", true))
                && (!input.contains("related")))
            return true

        return false
    }

    private fun includeTime (input: String): Boolean{
        if (TimeHandler().getTimeDuration(input) != null)
            return true

        return false
    }

    private fun breakText(fieldName: String, inputSentence: String): String?{

        if (fieldName.contains("break activity", true)){
            if(inputSentence.contains("did", true))
                return inputSentence.substring(inputSentence.toLowerCase().indexOf("did"), inputSentence.length)
            if(inputSentence.contains("do", true))
                return inputSentence.substring(inputSentence.toLowerCase().indexOf("do"), inputSentence.length)
            if(inputSentence.contains("have", true))
                return inputSentence.substring(inputSentence.toLowerCase().indexOf("have"), inputSentence.length)
            if(inputSentence.contains("had", true))
                return inputSentence.substring(inputSentence.toLowerCase().indexOf("had"), inputSentence.length)
            if(inputSentence.contains("having", true))
                return inputSentence.substring(inputSentence.toLowerCase().indexOf("having"), inputSentence.length)
            if(inputSentence.contains("get", true))
                return inputSentence.substring(inputSentence.toLowerCase().indexOf("get"), inputSentence.length)
            if(inputSentence.contains("got", true))
                return inputSentence.substring(inputSentence.toLowerCase().indexOf("got"), inputSentence.length)
        }
            return null
    }

    private fun productivityVSFeeling (fieldName: String, inputSentence: String): Boolean{

        if (inputSentence.contains("neutral", true) || inputSentence.contains("four", true)
                || inputSentence.contains("4", true)){
            if(inputSentence.contains("productivity", true) || fieldName.contains("productive", true)){
                if (!fieldName.contains("productivity", true))
                    return false
            }
            else{
                if(!fieldName.contains("feel", true))
                    return false
            }
        }

        return true
    }

}
