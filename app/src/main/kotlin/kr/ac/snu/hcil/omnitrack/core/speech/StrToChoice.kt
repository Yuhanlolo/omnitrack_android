package kr.ac.snu.hcil.omnitrack.core.speech

import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTChoiceFieldHelper
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.ChoiceInputView
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
        val multiChoice = choiceField.getIsMultiSelectionAllowed(field)
        val appendingAllowed = choiceField.getIsAppendingFromViewAllowed(field)
        var size = entries!!.size
        var anyMatch = false

        println("choice entry 1: ${entries!![1].text}, size: $size")

        if (size != 0){
            for (entry in entries){
                println("choice entry: $entry")
                // need to revisit this, there're might be some other ambiguous matching cases
                if (isMatch(inputStr, entry.text)) {
                    anyMatch = true
                    if (!selectedIndex.contains(entry.id)){
                        if(!multiChoice!!){
                            selectedIndex.clear()
                        }
                        selectedIndex.add(entry.id)
                        println("Choice input value changed, size: ${selectedIndex?.size}, index 0 value: ${selectedIndex?.get(selectedIndex?.size-1)}")
                    }
                }
            }

            // Found no matches with inputStr so add a new entry & make sure we can add new entry
            if (!anyMatch && appendingAllowed) {
                val choiceInputView = ChoiceInputView(context)
                choiceInputView.appendNewRow(inputStr)
                val entry = choiceInputView.entries[entries.size]

                if (!selectedIndex.contains(entry.id)){
                    if(!multiChoice!!){
                        selectedIndex.clear()
                    }
                    selectedIndex.add(entry.id)
                    println("New Choice: ${entry.text} ${entry.id} (id) ${entries.size} (size)")
                }
            }

            println("choice iterator size: $size")
        }
            return selectedIndex.toIntArray()
    }

    fun getSynonym (inputStr: String): String {
        var str = inputStr
        val digits = "0123456789"
        val strDigits: Array<String> = arrayOf("zero", "one", "two", "three", "four", "five", "six",
                "seven", "eight", "nine")

        for (i in 0 until str.length - 1) {
            if (digits.contains(str[i])) {
                str = str.replace(str.substring(i, i+1), strDigits[str[i] - '0'], true)
            }
        }

        return str
    }

    fun isMatch (str1: String, str2: String): Boolean {
        val str1Synonym = getSynonym(str1)
        val str2Synonym = getSynonym(str2)

        return (str1.contains(str2, true) || str2.contains(str1, true) ||
                str1Synonym.contains(str2, true) || str2.contains(str1Synonym, true) ||
                str1.contains(str2Synonym, true) || str2Synonym.contains(str1, true) ||
                str1Synonym.contains(str2Synonym, true) || str2Synonym.contains(str1Synonym, true))
    }

}