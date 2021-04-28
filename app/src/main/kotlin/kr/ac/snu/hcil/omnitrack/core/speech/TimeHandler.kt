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
    val timeZone = TimeZone.getDefault()
    val splitWords = arrayOf("to", "end", "ends", "ended", "then", "and", "am", "pm", "a.m.", "p.m.")

    fun timeParser(inputStr: String): String?{
        var timeStr: String? = null
        try {
            val groups: List<DateGroup> = parser.parse(inputStr)
            timeStr  = groups!!.get(0)!!.dates.toString()
        } catch (e: Exception){

        }

        val size = timeStr!!.length
        timeStr = timeStr.substring(1, size-1) //remove []
        return timeStr
    }

    fun getTimePoint (inputStr: String):TimePoint? {
        return TimePoint(getMillseconds(timeParser(inputStr)!!.toString()), timeZone.id)
    }

    fun getTimeDuration(inputStr: String):TimeSpan? {

        var timeStr_1: String? = null
        var timeStr_2: String? = null
        var timespan: TimeSpan? = null

        val timeStr  = timeParser(inputStr)!!.split(", ")!!.toTypedArray() //note that the split symbol is followed by a blank space
        val size =  timeStr?.size?: 0

        if(size > 0)
            timeStr_1 = timeStr!!.get(0)

        if(size > 1)
            timeStr_2 = timeStr!!.get(1)
        else {
            val spiltIndex = getSplitWord(inputStr)
            if(spiltIndex > 0){
                timeStr_1 = timeParser(inputStr.substring(0, spiltIndex))
                timeStr_2 = timeParser(inputStr.substring(spiltIndex, inputStr.length))
            }
        }

        //println ("time point 1: $timeStr_1, 2: $timeStr_2")

        if (timeStr_1 != null && timeStr_2 != null){
            timespan = TimeSpan.fromPoints(getMillseconds(timeStr_1), getMillseconds(timeStr_2), timeZone)
        }else if(timeStr_1 != null && timeStr_2 == null){

        }

        return timespan
    }

    private fun getSplitWord(inputStr: String) :Int{
        for (word in splitWords){
            if(inputStr.contains(word, true)){
                return inputStr.indexOf(word, 0, true) + word.length
            }
        }
        return 0
    }

    private fun getMillseconds(dateString: String): Long {
        val dateformat: DateFormat = SimpleDateFormat ("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
        try {
            val date = dateformat.parse(dateString)
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