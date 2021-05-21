package kr.ac.snu.hcil.android.common.containers

import kr.ac.snu.hcil.android.common.events.Event

open class InputModalitywithResult<T>(open var fieldName: String?, open var fieldType: Int, open var isSpeech: Boolean, open var succeed: Int, open var originalInput :String?)

data class AnyInputModalitywithResult(override var fieldName: String?, override var fieldType: Int, override var isSpeech: Boolean, override var succeed: Int, override var originalInput :String?) : InputModalitywithResult<Any>(fieldName, fieldType, isSpeech, succeed, originalInput) {

    constructor(nullable: Nullable<out Any>):this(null, -1,false, -1, "NA")

    override fun toString(): String {
        return "{\"field name\": \"$fieldName\", \"field type\": \"$fieldType\", \"Speech\": $isSpeech, \"succeed\": $succeed, \"Original Input\": \"${originalInput.toString()}\"}"
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other === this -> true
            other is AnyInputModalitywithResult ->other.fieldName == fieldName && other.fieldType == fieldType && other.isSpeech == isSpeech && other.succeed == succeed && other.originalInput == originalInput
            else -> false
        }
    }

    fun reset(){
        fieldName = null
        fieldType = -1
        isSpeech = false
        succeed = -1
        originalInput = "NA"
    }
}