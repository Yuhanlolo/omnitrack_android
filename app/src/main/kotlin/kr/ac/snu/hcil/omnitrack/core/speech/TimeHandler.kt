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

    val parser = Parser()
    val groups = parser.parse(inputStr)

    fun getTimeInfo(): TimePoint{
        val datetime = groups.get(0).dates
        /* example datetime output: [Fri Apr 09 08:12:39 EDT 2021] */

        /* when the utterance contains multiple timestamps*/
        //for (group in groups) {}
        val timezone = TimeZone.getDefault()
        return TimePoint(getMillseconds(datetime.toString()), timezone.id)
    }

    fun getMillseconds(nattyDate: String): Long {
        val dateformat: DateFormat = SimpleDateFormat ("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
        val subStrNatty = nattyDate.substring(1, 29) //remove []
        val date = dateformat.parse(subStrNatty)
        val millSeconds = date.getTime()
        //println ("millseconds from natty: $millSeconds")
        return millSeconds
    }
}