package kr.ac.snu.hcil.omnitrack.core.speech
import android.content.Context
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.DecimalFormat
import java.math.RoundingMode
import java.util.*
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.types.Fraction
import kr.ac.snu.hcil.omnitrack.core.types.RatingOptions
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTRatingFieldHelper

/**
 * Created by Yuhan Luo on 21. 4. 5
 */

class WordsToNumber(inputStr: String){

    val DIGITS = arrayOf("zero", "oh", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")
    val NUMS = arrayOf(0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    val stringtokens = StringTokenizer(inputStr)
//    val decimalFormat = DecimalFormat("#.#")
//    val TENS = arrayOf<String>("twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
//    val TEENS = arrayOf("ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
//    val MAGNITUDES = arrayOf("hundred", "thousand", "million", "point")
//    val ZERO = arrayOf("zero", "oh")

    /* Note: Google speech recognizer automatically handles numbers >=10, numbers with decimals, and cases when the a utterance includes only a number or number + unit
    * When a sentences includes numbers < 10 with other words ("I had one cup of coffee"), the speech recognizer will keep the number in full-spelling */
    fun getNumber (): BigDecimal {
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

    fun getRating(context:Context, field:OTFieldDAO?): Fraction{
        val ratingField = OTRatingFieldHelper(context)
        val ratingOptions = ratingField.getRatingOptions(field!!)

        if(ratingOptions != null){
            var under = (ratingOptions.getMaximumPrecisionIntegerRangeLength())
            val originalNum = getNumber().toFloat() // the float version of upper, to avoid rounding at the first place
            var franctionValue = Fraction(originalNum.toShort(), under)

            println ("number: ${getNumber()}, to short: $originalNum")

           if(ratingOptions.type == RatingOptions.DisplayType.Star && ratingOptions.isFractional){
               franctionValue = Fraction((originalNum * 2).toShort(), under)
            } else if(ratingOptions.type == RatingOptions.DisplayType.Likert && !ratingOptions.isFractional){
               franctionValue = Fraction((originalNum - 1).toShort(), under)
           } else if (ratingOptions.type == RatingOptions.DisplayType.Likert && ratingOptions.isFractional){
               franctionValue = Fraction(((originalNum - 1) * 10).toShort(), under)
           }
            return franctionValue
        }else{
            return Fraction(0, 10)
            //TODO: error check
        }
    }

    fun replaceNumbers (numsIndex: Int): BigDecimal {
           return NUMS.get(numsIndex).toBigDecimal()
    }

    fun getDigitIndex(input: String): Int {
        DIGITS.forEachIndexed{index, element ->
            if(input.equals(element, true))
                return index
        }
        return -1
    }
}