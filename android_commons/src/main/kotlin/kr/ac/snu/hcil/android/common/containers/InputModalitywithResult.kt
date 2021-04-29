package kr.ac.snu.hcil.android.common.containers

import kr.ac.snu.hcil.android.common.events.Event

open class InputModalitywithResult<T>(open var field_Id:String?, open var isSpeech: Boolean, open var succeed: Boolean, open var originalInput :String?)

data class AnyInputModalitywithResult(override var field_Id:String?, override var isSpeech: Boolean, override var succeed: Boolean, override var originalInput :String?) : InputModalitywithResult<Any>(field_Id, isSpeech, succeed, originalInput) {

    constructor(nullable: Nullable<out Any>):this(null, false, false, "NA")

    override fun toString(): String {
        return "{\"field_id\": \"$field_Id\", \"isSpeech\": $isSpeech, \"succeed\": $succeed, \"originalInput\": \"${originalInput.toString()}\"}"
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other === this -> true
            other is AnyInputModalitywithResult ->other.field_Id == field_Id && other.isSpeech == isSpeech && other.succeed == succeed && other.originalInput == originalInput
            else -> false
        }
    }
}