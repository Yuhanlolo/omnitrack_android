package kr.ac.snu.hcil.omnitrack.core.speech

import java.util.*
import kotlin.math.max
import smile.nlp.stemmer.*

/**
 * Created by Yuhan Luo on 21. 5. 18
 */

class StrCompareHelper{

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
            }

            if(token.contains("-")){
                str = str.replace("-", "")
            }
        }

        return str
    }

    fun isMatch (str1: String, str2: String): Boolean {

        if (str1.equals("") || str2.equals("") || str1.equals(" ") || str2.equals(" "))
            return false

        val str1Synonym = getSynonym(str1)
        val str2Synonym = getSynonym(str2)

        return (str1.contains(str2, true) || str2.contains(str1, true) ||
                str1Synonym.contains(str2, true) || str2.contains(str1Synonym, true) ||
                str1.contains(str2Synonym, true) || str2Synonym.contains(str1, true) ||
                str1Synonym.contains(str2Synonym, true) || str2Synonym.contains(str1Synonym, true))
    }

    fun ratingOrStar (str: String): Boolean{
        if(str.contains("star") || str.contains("rating") || str.contains("rate"))
            return true
        return false
    }

    private fun getStem (originalStr: String): String?{
        val porter = PorterStemmer()
        val stem = porter.stem(originalStr)

        if (!stem.equals(originalStr))
            return stem

        return null
    }

}