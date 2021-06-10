package kr.ac.snu.hcil.omnitrack.core.speech

import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTChoiceFieldHelper
import java.util.*
import kotlin.math.max
import smile.nlp.stemmer.*


/**
 * Created by Yuhan Luo on 21. 4. 13
 */

class StrToChoice{

    fun getChoiceIds (context: Context, field: OTFieldDAO?, inputStr: String): IntArray? {
        val choiceField = OTChoiceFieldHelper(context)
        val entries = choiceField.getChoiceEntries(field!!)

        var selectedIndex: ArrayList<Int> = ArrayList()
        val multiChoice = choiceField.getIsMultiSelectionAllowed(field)
        //val appendingAllowed = choiceField.getIsAppendingFromViewAllowed(field)
        var size = entries!!.size
        var anyMatch = false

       // println("choice entry 1: ${entries!![1].text}, size: $size")

        if (size != 0){
            for (entry in entries){
                println("choice entry: $entry")
                // need to revisit this, there're might be some other ambiguous matching cases
                if (StrCompareHelper().isMatch(inputStr, entry.text)) {
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

            // found no matches, check edit distance
 //           if (!anyMatch) {
//                var entryId = 0
//                var minDist = Integer.MAX_VALUE
//
//                for (entry in entries) {
//                    for (word in inputStr.split(" ")) {
//                        var editDist = editDistance(word, entry.text)
//
//                        if (editDist < minDist) {
//                            minDist = editDist
//                            entryId = entry.id
//                        }
//                    }
//                }
//
//                if (!selectedIndex.contains(entryId)){
//                    if(!multiChoice!!)
//                        selectedIndex.clear()
//                    selectedIndex.add(entryId)
//                } //          }

            // Found no matches with inputStr so add a new entry & make sure we can add new entry
//            if (!anyMatch && appendingAllowed) {
//                val choiceInputView = ChoiceInputView(context)
//                choiceInputView.appendNewRow(inputStr)
//                val entry = choiceInputView.entries[entries.size]
//
//                if (!selectedIndex.contains(entry.id)){
//                    if(!multiChoice!!){
//                        selectedIndex.clear()
//                    }
//                    selectedIndex.add(entry.id)
//                    println("New Choice: ${entry.text} ${entry.id} (id) ${entries.size} (size)")
//                }
//            }
        }

        if(!anyMatch)
            return null

            return selectedIndex.toIntArray()
    }

    fun getARandomChoice (context: Context, field: OTFieldDAO?): String{
        val choiceField = OTChoiceFieldHelper(context)
        val entries = choiceField.getChoiceEntries(field!!)

        val range = entries!!.size-1
        val randomNum = (0 .. range).random()
        var index = 0
        var randomEntryText: String = ""

        for (entry in entries){
            if (index == randomNum)
                randomEntryText = entry.text
            index ++
        }

//        if(randomEntryText.equals(""))
//            randomEntryText = getARandomChoice (context, field)

        return randomEntryText
    }


//    fun editDistance(X: String, Y: String): Int {
//        val m = X.length
//        val n = Y.length
//
//        val L = Array(m + 1) { IntArray(n + 1) }
//        for (i in 0..m) {
//            for (j in 0..n) {
//                if (i == 0 || j == 0) {
//                    L[i][j] = 0
//                } else if (X[i - 1] == Y[j - 1]) {
//                    L[i][j] = L[i - 1][j - 1] + 1
//                } else {
//                    L[i][j] = max(L[i - 1][j], L[i][j - 1])
//                }
//            }
//        }
//        val lcs = L[m][n]
//
//        return m - lcs + (n - lcs)
//    }

}