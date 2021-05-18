package kr.ac.snu.hcil.omnitrack.core.speech

import java.util.*
import kotlin.math.max
import smile.nlp.stemmer.*

/**
 * Created by Yuhan Luo on 21. 5. 18
 */

class StrCompareHelper(){

    fun getSynonym (inputStr: String): String {
        var str = inputStr
        val digits = "0123456789"
        val strDigits: Array<String> = arrayOf("zero", "one", "two", "three", "four", "five", "six",
                "seven", "eight", "nine")

        val stringtokens = inputStr.split(" ")

        for (i in 0 until str.length - 1) {
            if (digits.contains(str[i])) {
                str = str.replace(str.substring(i, i+1), strDigits[str[i] - '0'], true)
            }
        }

        for (token in stringtokens) {
            val tokenStem: String? = getStem(token)
            if (tokenStem != null){
                var newStr = ""
                val index = str.indexOf(token)

                if (index == 0){
                    newStr = tokenStem + str.substring(index + token.length)
                }else if (index+ token.length == str.length){
                    newStr = str.substring(0, index) + tokenStem
                }else{
                    newStr = str.substring(0, index) + tokenStem + str.substring(index + token.length)
                }
                str = newStr
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

    private fun getStem (originalStr: String): String?{
        val porter = PorterStemmer()
        val stem = porter.stem(originalStr)

        if (!stem.equals(originalStr))
            return stem

        return null
    }

}