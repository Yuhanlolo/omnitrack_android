package kr.ac.snu.hcil.omnitrack.core.speech
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

class WordsToNumber(){

    val DIGITS = arrayOf("zero", "oh", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")
    val NUMS = arrayOf(0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9)

//    val TENS = arrayOf<String>("twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
//    val TEENS = arrayOf("ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
//    val MAGNITUDES = arrayOf("hundred", "thousand", "million", "point")
//    val ZERO = arrayOf("zero", "oh")

    /* Note: Google speech recognizer automatically handles numbers >=10, numbers with decimals, and cases when the a utterance includes only a number or number + unit
    * When a sentences includes numbers < 10 with other words ("I had one cup of coffee"), the speech recognizer will keep the number in full-spelling */
    fun getNumber (inputStr: String): BigDecimal {
        val stringtokens = StringTokenizer(inputStr)
        var number = BigDecimal (0)
        var isNumerfound = false
        val numFormat = NumberFormat.getInstance(Locale.getDefault())

        // if multiple numbers are detected, return the latest one
        while (stringtokens.hasMoreTokens()) {
            val token = stringtokens.nextToken()
            try {
                number =  numFormat.parse(token).toString().toBigDecimal()
                isNumerfound = true
                println("Arabic number detected: $token")
            }catch (e: Exception){
                if (getDigitIndex(token)!=-1){
                    println("English number word detected: $token")
                    isNumerfound = true
                    number = replaceNumbers(getDigitIndex(token))
                } else{ //TODO: error check
                }
            }
        }

        if(isNumerfound)
            return number
        else
            return BigDecimal(0)
            //TODO: error check
    }

    fun replaceNumbers (numsIndex: Int): BigDecimal {
           return NUMS.get(numsIndex).toBigDecimal()
    }

    fun getDigitIndex(input: String): Int {
        DIGITS.forEachIndexed{index, element ->
            if(input == element)
                return index
        }
        return -1
    }
}