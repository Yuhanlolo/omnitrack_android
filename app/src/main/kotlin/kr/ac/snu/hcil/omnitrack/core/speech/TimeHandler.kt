package kr.ac.snu.hcil.omnitrack.core.speech

import com.joestelmach.natty.*
import kr.ac.snu.hcil.omnitrack.core.types.TimePoint
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Yuhan Luo on 21. 4. 9
 */

class TimeHandler(inputStr: String){

    private val parser = Parser()
    private val inputStr = inputStr
    //val groups: List<DateGroup> =

    fun getTimeInfo(): TimePoint?{
        /* example datetime output: [Fri Apr 09 08:12:39 EDT 2021] */
        var timepoint: TimePoint? = null
        try {
            val groups: List<DateGroup> = parser.parse(inputStr)
            val datetime = groups!!.get(0)!!.dates
            val timezone = TimeZone.getDefault()
            timepoint = TimePoint(getMillseconds(datetime.toString()), timezone.id)
        }catch (e: Exception){

        }

        return timepoint

        /* when the utterance contains multiple timestamps*/
        //for (group in groups) {}
//            if(datetime != null){
//                val timezone = TimeZone.getDefault()
//                return TimePoint(getMillseconds(datetime.toString()), timezone.id)
//            }else
//                return null
    }

    private fun getMillseconds(nattyDate: String): Long {
        val dateformat: DateFormat = SimpleDateFormat ("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
        val subStrNatty = nattyDate.substring(1, 29) //remove []
        val date = dateformat.parse(subStrNatty)
        val millSeconds = date.getTime()
        return millSeconds
    }
}