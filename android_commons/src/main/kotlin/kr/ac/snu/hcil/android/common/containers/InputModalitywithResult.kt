package kr.ac.snu.hcil.android.common.containers

open class InputModalitywithResult<T>(open var fieldId: String?, open var fieldName: String?, open var fieldType: Int, open var isSpeech: Boolean, open var succeed: Int, open var originalInput :String?)

data class AnyInputModalitywithResult(override var fieldId: String?, override var fieldName: String?, override var fieldType: Int, override var isSpeech: Boolean, override var succeed: Int, override var originalInput :String?) : InputModalitywithResult<Any>(fieldId, fieldName, fieldType, isSpeech, succeed, originalInput) {

    constructor(nullable: Nullable<out Any>):this(null,null, -1,false, -1, "NA")

    override fun toString(): String {
        return "{\"Id\": \"$fieldId\", \"field\": \"$fieldName\", \"type\": \"$fieldType\", \"Speech\": $isSpeech, \"succeed\": $succeed, \"input\": \"${originalInput.toString()}\"}"
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other === this -> true
            other is AnyInputModalitywithResult ->other.fieldId == fieldId && other.fieldName == fieldName && other.fieldType == fieldType && other.isSpeech == isSpeech && other.succeed == succeed && other.originalInput == originalInput
            else -> false
        }
    }

    fun reset(){
        fieldId = null
        fieldName = null
        fieldType = -1
        isSpeech = false
        succeed = -1
        originalInput = "NA"
    }
}