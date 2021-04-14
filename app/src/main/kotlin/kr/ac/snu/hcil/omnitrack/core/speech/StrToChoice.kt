package kr.ac.snu.hcil.omnitrack.core.speech

import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTChoiceFieldHelper
import java.util.*

/**
 * Created by Yuhan Luo on 21. 4. 13
 */

class StrToChoice(inputStr: String){

    val inputStr = inputStr

    fun getChoiceIds (context: Context, field: OTFieldDAO?): IntArray? {
        val choiceField = OTChoiceFieldHelper(context)
        val entries = choiceField.getChoiceEntries(field!!)

        var selectedIndex: ArrayList<Int> = ArrayList()
        val multiChoice = choiceField.getIsMultiSelectionAllowed(field!!)
        var size = entries!!.size

        println("choice entry 1: ${entries!![1].text}, size: $size")

        if(size != 0){
            for (entry in entries){
                println("choice entry: $entry")
                // need to revisit this, there're might be some other ambiguous matching cases
                if(inputStr.contains(entry.text, true) || entry.text.contains(inputStr, true)){
                    if(!selectedIndex.contains(entry.id)){
                        if(!multiChoice!!){
                            selectedIndex.clear()
                        }
                        selectedIndex.add(entry.id)
                        println("Choice input value changed, size: ${selectedIndex?.size}, index 0 value: ${selectedIndex?.get(selectedIndex?.size-1)}")
                    }
                }
            }
            println("choice iterator size: $size")
        }
            return selectedIndex.toIntArray()
    }

}