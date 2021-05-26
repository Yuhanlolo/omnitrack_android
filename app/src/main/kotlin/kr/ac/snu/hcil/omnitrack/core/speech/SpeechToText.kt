package kr.ac.snu.hcil.omnitrack.core.speech

import android.content.Context


/**
 * Created by Yuhan Luo on 21. 5. 25
 */

// Reference: https://github.com/umdsquare/data-at-hand-mobile/blob/b36c3a00aadcf003da254f7c9826dcb2013c4115/android/app/src/main/java/com/dataathand/speech/ASpeechToTextModule.java#L95

class SpeechToText (val context: Context) {

    fun joinTexts (left: String?, right: String?): String?{
        if (left == null && right == null) {
            return null
        } else if (left != null && right == null) {
            return left
        } else if (left == null && right != null) {
            return right
        } else {
            return (left + " " + right).trim().replace("\\s+", " ")
        }
    }
}