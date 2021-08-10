package kr.ac.snu.hcil.omnitrack.core.speech
import android.content.Context
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.types.Fraction
import kr.ac.snu.hcil.omnitrack.core.types.RatingOptions
import kr.ac.snu.hcil.omnitrack.core.types.RatingOptions.DisplayType
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTRatingFieldHelper
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

/**
 * Created by Yuhan Luo on 21. 4. 5
 */

class WordsToNumber(){

    val DIGITS = arrayOf("zero", "oh", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")
    val NUMS = arrayOf(0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    var outofRange = false

//    val decimalFormat = DecimalFormat("#.#")
//    val TENS = arrayOf<String>("twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
//    val TEENS = arrayOf("ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
//    val MAGNITUDES = arrayOf("hundred", "thousand", "million", "point")
//    val ZERO = arrayOf("zero", "oh")

    /* Note: Google speech recognizer automatically handles numbers >=10, numbers with decimals, and cases when the a utterance includes only a number or number + unit
    * When a sentences includes numbers < 10 with other words ("I had one cup of coffee"), the speech recognizer will keep the number in full-spelling */
    fun getNumber (inputStr: String): BigDecimal? {

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
            }catch (e: Exception){
                if (getDigitIndex(token)!=-1){
                    isNumerfound = true
                    number = replaceNumbers(getDigitIndex(token))
                }
            }
        }

        if(isNumerfound)
            return number
        else
            return null
            // if no number is found, return null
    }

    fun getRating(context:Context, field:OTFieldDAO?, inputStr: String): Fraction?{
        val originalNum = getNumber(inputStr)?.toFloat() // the float version of upper, to avoid rounding at the first place

        val ratingField = OTRatingFieldHelper(context)
        val ratingOptions = ratingField.getRatingOptions(field!!)



        if(ratingOptions != null) {
            val under = ratingOptions.getMaximumPrecisionIntegerRangeLength()
            val midpoint = (under.toInt()-1)/2 + 1

            if (originalNum == null) {
                if (inputStr.contains("very productive", true) || inputStr.contains("very positive", true)){
                    return Fraction(under, under) /* 7 */
                } else if (inputStr.contains("productive at all", true) || inputStr.contains("very negative", true)){
                    return Fraction(0.toShort(), under) /* 1 */
                } else if (inputStr.contains("somewhat not productive", true) || inputStr.contains("somewhat negative", true)){
                    return Fraction((midpoint - 1).toShort(), under) /* 3 */
                } else if (inputStr.contains("not productive", true) || inputStr.contains("negative", true)){
                    return Fraction(1.toShort(), under) /* 2 */
                }else if (inputStr.contains("neutral", true)){
                    return Fraction(midpoint.toShort(), under) /* 4 */
                }  else if (inputStr.contains("somewhat productive", true) || inputStr.contains("somewhat positive", true)){
                    return Fraction((midpoint + 1).toShort(), under) /* 5 */
                } else if (inputStr.contains("productive", true) || inputStr.contains("positive", true)){
                    return Fraction((under - 1).toShort(), under) /* 6 */
                }

                return null
            } else {
                if (originalNum > under + 1) {
                    outofRange = true
                    return null
                }
                //println ("number: $originalNum, under: $under")

                var franctionValue = Fraction(originalNum!!.toShort(), under)

                if (ratingOptions.type == DisplayType.Star && ratingOptions.isFractional) {
                    if (originalNum * 2 > under) {
                        outofRange = true
                        return null
                    }
                    franctionValue = Fraction((originalNum * 2).toShort(), under)
                } else if (ratingOptions.type == DisplayType.Likert && !ratingOptions.isFractional) {
                    franctionValue = Fraction((originalNum - 1).toShort(), under)
                } else if (ratingOptions.type == DisplayType.Likert && ratingOptions.isFractional) {
                    if ((originalNum - 1) * 10 > under) {
                        outofRange = true
                        return null
                    }
                    franctionValue = Fraction(((originalNum - 1) * 10).toShort(), under)
                }

                return franctionValue
            }
        } else{
            return null
            // if rating option is not valid, return null
        }
    }


    fun getRange (context:Context, field:OTFieldDAO?): DoubleArray?{

        val ratingField = OTRatingFieldHelper(context)
        val ratingOptions = ratingField.getRatingOptions(field!!)

        var range = DoubleArray(2)

        if(ratingOptions != null) {
            range[1] = ratingOptions.getMaximumPrecisionIntegerRangeLength().toDouble() + 1

            if (ratingOptions.type == DisplayType.Star && ratingOptions.isFractional)
                range[1] = ratingOptions.getMaximumPrecisionIntegerRangeLength().toDouble()/2
            else if (ratingOptions.type == DisplayType.Star && !ratingOptions.isFractional)
                range[1] = ratingOptions.getMaximumPrecisionIntegerRangeLength().toDouble()
            else if (ratingOptions.type == DisplayType.Likert && ratingOptions.isFractional)
                range[1] = ratingOptions.getMaximumPrecisionIntegerRangeLength().toDouble()/10 + 1
        }

        if (ratingOptions.isFractional)
            range[0] = 0.5
        else
            range[0] = 1.toDouble()

            return range
    }

    fun getRangeText (fieldName: String): Array<String>? {

       if (fieldName.contains("productivity score", true)){
           return arrayOf("not productive at all", "very productive")
       }

        if (fieldName.contains("rate your feelings", true)){
            return arrayOf("very negative", "very positive")
        }

        return null
    }

    fun getRatingType (context:Context, field:OTFieldDAO?): DisplayType{
        val ratingField = OTRatingFieldHelper(context)
        val ratingOptions = ratingField.getRatingOptions(field!!)

        return ratingOptions.type
    }


    private fun replaceNumbers (numsIndex: Int): BigDecimal {
           return NUMS.get(numsIndex).toBigDecimal()
    }

    private fun getDigitIndex(input: String): Int {
        DIGITS.forEachIndexed{index, element ->
            if(input.equals(element, true))
                return index
        }
        return -1
    }
}