package kr.ac.snu.hcil.omnitrack.core.speech

import com.joestelmach.natty.*
import kr.ac.snu.hcil.omnitrack.core.types.TimePoint
import kr.ac.snu.hcil.omnitrack.core.types.TimeSpan
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Yuhan Luo on 21. 4. 9
 */

class TimeHandler{

    private val parser = Parser()
    private val timeZone = TimeZone.getDefault()
    //private val TIME_OF_DAY = arrayOf("am", "pm", "a.m.", "p.m.")
    private val SPLIT_WORDS = arrayOf("am", "pm", "a.m.", "p.m.", "ago", "end", "ends", "ended", "ending", "then", "till", "until", "last", "lasts", "lasted", "lasting", "and")
    private val DURATION_WORDS = arrayOf("last", "lasts", "lasted", "lasting")
    private val TIME_UNITS = arrayOf("month", "months","week", "weeks", "day", "days", "hour", "hours", "minute", "minutes", "second", "seconds")
    private val UNIT_DURATION = arrayOf(30 * 24 * 3600, 30 * 24 * 3600, 7 * 24 * 3600, 7 * 24 * 3600, 24 * 3600, 24 * 3600, 3600, 3600, 60, 60, 1, 1)
    private val RELATIVE_TIME = arrayOf("ago", "later")

    fun timeParser(inputStr: String): String? {
        var timeStr: String? = null

        try {
            val groups: List<DateGroup> = parser.parse(inputStr)
            timeStr  = groups[0].dates.toString()
            val size = timeStr!!.length
            timeStr = timeStr.substring(1, size-1) //remove []
        } catch (e: Exception){

        }


        return timeStr
    }

    fun getTimePoint (inputStr: String):TimePoint? {
        if (timeParser(inputStr) == null)
            return null

        return TimePoint(getMilliseconds(timeParser(inputStr).toString(), inputStr), timeZone.id)
    }

    fun getTimeDuration(inputStr: String):TimeSpan? {

        var timeStr_1: String? = null
        var timeStr_2: String? = null
        var timespan: TimeSpan? = null

        if (timeParser(inputStr) == null)
            return null

        val timeStr  = timeParser(inputStr)!!.split(", ")!!.toTypedArray() //note that the split symbol is followed by a blank space
        timeStr_1 = timeStr.get(0)

        if(timeStr.size > 1){
            timeStr_2 = timeStr.get(1)
            timespan = TimeSpan.fromPoints(getMilliseconds(timeStr_1!!.toString(), ""), getMilliseconds(timeStr_2, ""), timeZone)
        } else {
            val spiltIndexFirst= getSplitWordIndexFirst(inputStr, SPLIT_WORDS)

            if (spiltIndexFirst > 0){
                var inputStrFirst = inputStr.substring(0, spiltIndexFirst)
                if (inputStrFirst.contains(",")){
                    inputStrFirst = inputStrFirst.substringBefore(",")
                }

                timeStr_1 = timeParser(inputStrFirst)

                val spiltIndexSecond = getSplitWordIndexSecond(inputStr, SPLIT_WORDS)
                val inputStrSecond = inputStr.substring(spiltIndexSecond, inputStr.length)

                println ("time point string : $inputStrFirst, timestr_1: $timeStr_1, string 2: $inputStrSecond")

                val timeunitWordList = getSplitWord(inputStrSecond, TIME_UNITS)
                val durationWordList = getSplitWord(inputStrSecond, DURATION_WORDS)
                val relativeWordIndex = containsRelativeTimePoint(inputStrSecond)

                if(durationWordList!!.isNotEmpty() && timeunitWordList!!.isNotEmpty() &&
                        (relativeWordIndex > getSplitWordIndexFirst(inputStrSecond, DURATION_WORDS) || relativeWordIndex == 0 )){
                    // utterance contains hours/minutes but not ago or later, indicating a time duration expression

                    var duration = 0
                    for (timeUnit in timeunitWordList){
                        var durationNum = getNumber(inputStrSecond, timeUnit)
                        println ("time point durationNum: $durationNum")
                        duration =+ durationNum * UNIT_DURATION[TIME_UNITS.indexOf(timeUnit)] * 1000
                    }
                    println ("time point 1: $timeStr_1, duraiton: $duration")
                    if (timeStr_1 != null)
                        timespan = TimeSpan.fromDuration(getMilliseconds(timeStr_1.toString(), ""), duration.toLong(), timeZone)
                    else
                        timespan = TimeSpan.fromDuration(System.currentTimeMillis() - duration.toLong(), duration.toLong(), timeZone)
                } else {
                    timeStr_2 = timeParser(inputStrSecond)
                    if (timeStr_1 != null && timeStr_2 != null)
                        timespan = TimeSpan.fromPoints(getMilliseconds(timeStr_1.toString(), ""), getMilliseconds(timeStr_2.toString(), ""), timeZone)
                }
            }else{

            }
        }
        println ("time point span: $timespan")
        return timespan
    }

    private fun getSplitWordIndexFirst(inputStr: String, wordsArr: Array<String>) :Int{
        for (word in wordsArr){
            val stringtokens = StringTokenizer(inputStr)
            while (stringtokens.hasMoreTokens()) {
                val token = stringtokens.nextToken()
                if(token.equals(word, true)){
                    return inputStr.indexOf(word, 0, true) + word.length
                }
            }
        }
        return 0
    }

    private fun removeNoisyWords(originalStr: String): String{
        var cleanedStr = originalStr
        val index = containsRelativeTimePoint(originalStr)
        if (index > 0){
            cleanedStr = originalStr.substring(originalStr.length)
        }
         return cleanedStr
    }

    private fun getSplitWordIndexSecond(inputStr: String, wordsArr: Array<String>) :Int{
        val stringtokens = StringTokenizer(inputStr)
        while (stringtokens.hasMoreTokens()) {
            val token = stringtokens.nextToken()
            for (word in wordsArr){
                if(token.equals(word, true)){
                    return inputStr.indexOf(word, 0, true)
                }
            }
        }

        return 0
    }

    private fun getSplitWord(inputStr: String, wordsArr: Array<String>): MutableList<String>?{
        var timeUnitList: MutableList<String>? = arrayListOf()
        for (word in wordsArr){
            if(inputStr.contains(word, true)){
                timeUnitList!!.add(word)
            }
        }
        return timeUnitList
    }

    private fun containsRelativeTimePoint (inputStr: String): Int{
        for (word in RELATIVE_TIME){
            if (inputStr.contains(word, true))
                return return inputStr.indexOf(word, 0, true)
        }
        return 0
    }

    private fun getNumber (inputStr: String, word: String): Int {
        val index = inputStr.indexOf(word)
        val beforeWord = inputStr.substring(0, index-1).split(' ').last()
        //println ("time point beforeWord: $beforeWord")
        val num = WordsToNumber().getNumber(beforeWord)

        if(num != null)
            return num.toInt()

        return 0
    }

    private fun getMilliseconds(dateString: String, originalString: String): Long {
        val dateformat: DateFormat = SimpleDateFormat ("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
        try {
            val date = dateformat.parse(dateString)

            // fix tonight and last night bugs
            if (originalString != "") {
                if (originalString.contains("last night", true) && date.hours <= 12)
                    return date.getTime() - (12 * 60 * 60 * 1000)

                if (originalString.contains("night", true) && date.hours < 12)
                    return date.getTime() + (12 * 60 * 60 * 1000)
            }

            return date.getTime()
        }catch (e: Exception){
            return System.currentTimeMillis()
        }
    }


//    fun multiTimeParser(inputStr: String): String?{
//        /* example datetime output: [Fri Apr 09 08:12:39 EDT 2021] */
//        /* when the utterance contains multiple timestamps*/
//        var listTimeStr: MutableList<String>? = arrayListOf()
//        try {
//            val groups: List<DateGroup> = parser.parse(inputStr)
//            for (group in groups) {
//                var timeStr  = group.dates.toString()
//                val size = timeStr!!.length
//                timeStr = timeStr.substring(1, size-1) //remove []
//                listTimeStr!!.add(timeStr)
//            }
//        }catch (e: Exception){
//
//        }
//        return listTimeStr!!.joinToString()
//    }
}