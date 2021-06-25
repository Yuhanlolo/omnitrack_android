package kr.ac.snu.hcil.omnitrack.core.speech

import java.util.*
import kotlin.math.max
import smile.nlp.stemmer.*

/**
 * Created by Yuhan Luo on 21. 5. 18
 */

class StrCompareHelper{

    fun isMatch (label: String, inputStr: String): Boolean {

        if (inputStr.equals("") || inputStr.equals(" "))
            return false

        if (label.contains(inputStr, true) || inputStr.contains(label, true))
            return true

        val labelSynonyms = getSynonym(label)
        val inputSynonyms = getSynonym(inputStr)

        if (labelSynonyms.isNotEmpty()){
            for (labelSynonym in labelSynonyms) {
                if (inputStr.contains(labelSynonym, true) || labelSynonym.contains(inputStr, true))
                    return true
            }
        }

        if (inputSynonyms.isNotEmpty()){
            for (inputSynonym in inputSynonyms){
                if(label.contains(inputSynonym, true) || inputSynonym.contains(label, true))
                    return true
            }
        }

        return false
    }

    fun ratingOrStar (str: String): Boolean{
        if(str.contains("star", true) || str.contains("rating", true)
                || str.contains("rate", true) || str.contains("score", true)
                || str.contains("level", true))
            return true
        return false
    }

    private fun getSynonym (originalStr: String): ArrayList<String>{
        var synonymList: ArrayList<String> = ArrayList<String>()
        var str = originalStr

        /* add number synonym (e.g., "Choice 1" and "Choice one"), if any */
        if (!originalStr.equals(getNumSynonym(originalStr)))
            synonymList.add(getNumSynonym(originalStr))

        val stringtokens = originalStr.split(" ")
        for (token in stringtokens) {
            val tokenStem: String? = getStem(token)

            /* add stem words as synonym, if any */
            if (tokenStem != null){
                val index = str.indexOf(token)
                var newStr = ""
                if (index == 0){
                    newStr = tokenStem + str.substring(index + token.length)
                }else if (index + token.length == str.length){
                    newStr = str.substring(0, index) + tokenStem
                }else{
                    newStr = str.substring(0, index) + tokenStem + str.substring(index + token.length)
                }
                str = newStr
                synonymList.add(str)
            }

            /* if the original String contains '-', remove '-' and add it as a synonym */
            if(token.contains("-")){
                str = originalStr.replace("-", " ")
                synonymList.add(str)
                }
            }

        /* if input String contains '-related', remove '-related' and add it as a synonym */
        if(originalStr.contains("-related", true)){
            str = originalStr.replace("-related", "")
            synonymList.add(str)
        }

        /* if input String contains 'description', remove 'description' and add it as a synonym */
        if(originalStr.contains(" description", true)){
            str = originalStr.replace(" description", "")
            synonymList.add(str)
        }

        //println("originalStr: $originalStr, synonymList: $synonymList")
        return synonymList
    }

    private fun getNumSynonym (inputStr: String): String {
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

    private fun getStem (originalStr: String): String?{
        val porter = PorterStemmer()
        val stem = porter.stem(originalStr)

        if (!stem.equals(originalStr))
            return stem

        return null
    }

}