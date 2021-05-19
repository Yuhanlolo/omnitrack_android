package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.content.Context
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.os.Vibrator
import android.os.VibrationEffect
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.core.content.ContextCompat
import butterknife.bindView
import com.afollestad.materialdialogs.MaterialDialog
import com.github.salomonbrys.kotson.*
import com.google.gson.JsonObject
import io.noties.markwon.Markwon
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_new_item.*
import kotlinx.android.synthetic.main.description_panel_frame.view.*
import kr.ac.snu.hcil.android.common.containers.AnyInputModalitywithResult
import kr.ac.snu.hcil.android.common.containers.AnyValueWithTimestamp
import kr.ac.snu.hcil.android.common.view.DialogHelper
import kr.ac.snu.hcil.android.common.view.InterfaceHelper
import kr.ac.snu.hcil.android.common.view.container.LockableFrameLayout
import kr.ac.snu.hcil.android.common.view.container.decoration.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.android.common.view.indicator.LoadingIndicatorBar
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.database.models.OTDescriptionPanelDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTTrackerLayoutElementDAO
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import kr.ac.snu.hcil.omnitrack.ui.pages.ConnectionIndicatorStubProxy
import kr.ac.snu.hcil.omnitrack.core.speech.SpeechRecognizerUtility
import kr.ac.snu.hcil.omnitrack.core.speech.InputProcess
import java.util.*
import kotlin.properties.Delegates
import com.airbnb.lottie.LottieAnimationView

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
/**
 * Created by Young-Ho Kim on 16. 7. 24
 */
abstract class AItemDetailActivity<ViewModelType : ItemEditionViewModelBase>(val viewModelClass: Class<ViewModelType>) : MultiButtonActionBarActivity(R.layout.activity_new_item), View.OnClickListener{

    class RequiredFieldsNotCompleteException(val inCompleteFieldLocalIds: Array<String>) : Exception("Required fields are not completed.")

    companion object {

        const val KEY_ITEM_SAVED = "itemSaved"

        const val REQUEST_CODE_REDIRECT_SURVEY = 23153
    }

    private val schemaAdapter = SchemaAdapter()

    protected lateinit var viewModel: ViewModelType

    private val layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

    private val loadingIndicatorBar: LoadingIndicatorBar by bindView(R.id.ui_loading_indicator)

    protected val currentAttributeViewModelList = ArrayList<ItemEditionViewModelBase.AttributeInputViewModel>()
    private val currentDescriptionPanelList = ArrayList<OTDescriptionPanelDAO>()
    private val schemaLayout = ArrayList<OTTrackerLayoutElementDAO>()

    //State=============================================================================================================
    protected var itemSaved: Boolean = false
    //==================================================================================================================

    protected val invalidOutsideDialogBuilder: MaterialDialog.Builder by lazy {
        DialogHelper.makeSimpleAlertBuilder(this, "", null, null) {
            finish()
        }
    }

    abstract fun getTitle(trackerName: String): String

    abstract fun initViewModel(viewModel: ViewModelType, savedInstanceState: Bundle?)

    override fun onSessionLogContent(contentObject: JsonObject) {
        super.onSessionLogContent(contentObject)
        if (isFinishing) {
            contentObject["item_saved"] = itemSaved
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {

        outState.putBoolean(KEY_ITEM_SAVED, itemSaved)

        super.onSaveInstanceState(outState)

        this.viewModel.onSaveInstanceState(outState)
    }

    private lateinit var markwon: Markwon


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        markwon = Markwon.create(this.baseContext)

        setActionBarButtonMode(MultiButtonActionBarActivity.Mode.SaveCancel)
        rightActionBarTextButton?.visibility = View.GONE
        rightActionBarButton?.visibility = View.GONE
        rightActionBarSubButton?.visibility = View.GONE

        ui_button_next.setOnClickListener(this)

        ui_attribute_list.layoutManager = layoutManager
        ui_attribute_list.addItemDecoration(HorizontalImageDividerItemDecoration(R.drawable.horizontal_separator_pattern, this))
        (ui_attribute_list.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        ui_attribute_list.adapter = schemaAdapter

        loadingIndicatorBar.setMessage(R.string.msg_indicator_message_item_autocomplete)

        viewModel = ViewModelProviders.of(this).get(viewModelClass)
        initViewModel(viewModel, savedInstanceState)

        if (savedInstanceState != null) {
            this.itemSaved = savedInstanceState.getBoolean(KEY_ITEM_SAVED)
        }

        creationSubscriptions.add(
                viewModel.isBusyObservable.subscribe { isBusy ->
                    if (isBusy)
                        loadingIndicatorBar.show()
                    else loadingIndicatorBar.dismiss()
                }
        )

        creationSubscriptions.add(
                viewModel.trackerNameObservable.subscribe { name ->
                    title = getTitle(name)
                }
        )

        creationSubscriptions.add(
                viewModel.schemaInformationObservable.subscribe { (attrViewModelList, panelList, layout) ->
                    currentAttributeViewModelList.clear()
                    currentAttributeViewModelList.addAll(attrViewModelList)

                    currentDescriptionPanelList.clear()
                    if (panelList != null) {
                        currentDescriptionPanelList.addAll(panelList)
                    }

                    schemaLayout.clear()
                    if (layout != null && layout.isNotEmpty()) {
                        schemaLayout.addAll(layout)
                    } else {
                        schemaLayout.addAll(currentAttributeViewModelList.map { OTTrackerLayoutElementDAO().apply { this.type = OTTrackerLayoutElementDAO.TYPE_FIELD; this.reference = it._id!! } })
                    }

                    schemaAdapter.notifyDataSetChanged()
                }
        )

        /*
        creationSubscriptions.add(
                viewModel.attributeViewModelListObservable.subscribe { list ->
                    currentAttributeViewModelList.clear()
                    currentAttributeViewModelList.addAll(list)
                    attributeListAdapter.notifyDataSetChanged()
                }
        )*/

        creationSubscriptions.add(
                viewModel.hasTrackerRemovedOutside.subscribe {
                    invalidOutsideDialogBuilder.content(R.string.msg_format_removed_outside_return_home, getString(R.string.msg_text_tracker))
                    invalidOutsideDialogBuilder.show()
                }
        )

        creationSubscriptions.add(
                viewModel.isAllInputCompleteObservable.subscribe {
                    if (it) {
                        onTrackerInputComplete()
                    } else onTrackerInputIncomplete()
                }
        )
    }

    protected open fun onTrackerInputComplete() {

    }

    protected open fun onTrackerInputIncomplete() {

    }

    override fun onPause() {
        super.onPause()

        for (inputView in schemaAdapter.inputViews) {
            inputView.clearFocus()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        for (inputView in schemaAdapter.inputViews) {
            inputView.onDestroy()
        }

    }

    override fun onLowMemory() {
        super.onLowMemory()
        for (inputView in schemaAdapter.inputViews) {
            inputView.onLowMemory()
        }
    }

    override fun onToolbarLeftButtonClicked() {
        //back button
        itemSaved = false
    }

    override fun onBackPressed() {
        onToolbarLeftButtonClicked()
    }

    override fun onClick(v: View?) {
        if (v === ui_button_next) {
            onToolbarRightButtonClicked()
        }
    }

    protected fun checkInputComplete(): Completable {
        return Single.zip(
                schemaAdapter.inputViews.map { it.forceApplyValueAsync() }
        ) { zipped -> zipped }.flatMapCompletable {
            val incompleteFieldLocalIds = currentAttributeViewModelList.asSequence().filter { attributeViewModel ->
                attributeViewModel.isValidated == false
            }.map { it.fieldLocalId }.toList()

            if (incompleteFieldLocalIds.isNotEmpty()) {
                throw RequiredFieldsNotCompleteException(incompleteFieldLocalIds.toTypedArray())
            } else {
                return@flatMapCompletable Completable.complete()
            }
        }.observeOn(AndroidSchedulers.mainThread()).doOnError { ex ->
            if (ex is RequiredFieldsNotCompleteException) {
                val incompleteFields = currentAttributeViewModelList.asSequence().mapIndexed { index, attributeInputViewModel -> Pair(index, attributeInputViewModel) }.filter { ex.inCompleteFieldLocalIds.contains(it.second.fieldLocalId) }.toList()

                val minPosition = incompleteFields.minBy { it.first }?.first
                if (minPosition != null) {
                    val topFitScroller = object : LinearSmoothScroller(this@AItemDetailActivity) {
                        override fun getVerticalSnapPreference(): Int {
                            return LinearSmoothScroller.SNAP_TO_START
                        }
                    }

                    topFitScroller.targetPosition = minPosition
                    ui_attribute_list.layoutManager?.startSmoothScroll(topFitScroller)
                }

                Toast.makeText(this@AItemDetailActivity, "${ex.inCompleteFieldLocalIds.size} required fields are not completed.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    protected open fun preOnActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (!preOnActivityResult(requestCode, resultCode, data)) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val attributePosition = AFieldInputView.getPositionFromRequestCode(requestCode)
                val inputView = schemaAdapter.inputViews.find { it.position == attributePosition }
                inputView?.setValueFromActivityResult(data, AFieldInputView.getRequestTypeFromRequestCode(requestCode))
            }
        }
    }

    inner class SchemaAdapter : RecyclerView.Adapter<SchemaAdapter.ViewHolder>() {

        val inputViews = HashSet<AFieldInputView<*>>()

        fun getItem(position: Int): OTTrackerLayoutElementDAO {
            return schemaLayout[position]
        }

        override fun getItemViewType(position: Int): Int {

            val layoutElm = getItem(position)

            return when (layoutElm.type) {
                OTTrackerLayoutElementDAO.TYPE_FIELD -> {
                    val attr = currentAttributeViewModelList.find { it._id == layoutElm.reference }!!.fieldDAO
                    (applicationContext as OTAndroidApp).applicationComponent.getAttributeViewFactoryManager().get(attr.type).getInputViewType(false, attr)
                }
                OTTrackerLayoutElementDAO.TYPE_DESCRIPTION -> -1
                else -> -1
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SchemaAdapter.ViewHolder {

            if (viewType == -1) {
                val frame = LayoutInflater.from(this@AItemDetailActivity).inflate(R.layout.description_panel_frame, parent, false)
                return DescriptionViewHolder(frame)
            } else {
                val frame = LayoutInflater.from(this@AItemDetailActivity).inflate(R.layout.attribute_input_frame, parent, false)

                val inputView = AFieldInputView.makeInstance(viewType, this@AItemDetailActivity)
                inputViews.add(inputView)

                inputView.onCreate(null)

                return FieldViewHolder(inputView, frame)
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindLayoutElm(getItem(position), position)
        }

        override fun getItemCount(): Int {
            return schemaLayout.size
        }

        abstract inner class ViewHolder(frame: View) : RecyclerView.ViewHolder(frame) {
            fun bindLayoutElm(layoutElm: OTTrackerLayoutElementDAO, position: Int) {
                when (layoutElm.type) {
                    OTTrackerLayoutElementDAO.TYPE_FIELD -> {
                        val v = currentAttributeViewModelList.find { it._id == layoutElm.reference }
                        if (v != null) {
                            onBindField(v, position)
                        }
                    }
                    OTTrackerLayoutElementDAO.TYPE_DESCRIPTION -> {
                        val v = currentDescriptionPanelList.find { it._id == layoutElm.reference }
                        if (v != null) {
                            onBindDescription(v)
                        }
                    }
                }
            }

            protected open fun onBindField(fieldViewModel: ItemEditionViewModelBase.AttributeInputViewModel, position: Int) {}
            protected open fun onBindDescription(descriptionPanel: OTDescriptionPanelDAO) {}
        }

        inner class DescriptionViewHolder(frame: View) : ViewHolder(frame) {
            override fun onBindDescription(descriptionPanel: OTDescriptionPanelDAO) {
                markwon.setMarkdown(this.itemView.ui_description, descriptionPanel.content)
            }
        }


        inner class FieldViewHolder(val inputView: AFieldInputView<out Any>, frame: View) : View.OnClickListener, View.OnTouchListener, ViewHolder(frame) {

            private val columnNameView: TextView by bindView(R.id.ui_column_name)
            private val requiredMarker: View by bindView(R.id.ui_required_marker)
            //private val attributeTypeView: TextView by bindView(R.id.ui_attribute_type)

            private val container: LockableFrameLayout by bindView(R.id.ui_input_view_container)

            private val timestampIndicator: TextView by bindView(R.id.ui_timestamp)

            private val speechButton: AppCompatImageView by bindView(R.id.ui_speech_input)

            private val speech_anim: LottieAnimationView by bindView(R.id.ui_speech_anim)

            private var itemTimestamp: Long? = null

            private val connectionIndicatorStubProxy: ConnectionIndicatorStubProxy

            private val loadingIndicatorInContainer: View by bindView(R.id.ui_container_indicator)

            internal val validationIndicator: AppCompatImageView by bindView(R.id.ui_validation_indicator)

            private val internalSubscriptions = CompositeDisposable()

            private var currentValidationState: Boolean by Delegates.observable(true) {property, old, new ->
               /* if (new) {
                    //valid
                    if (validationIndicator.progress != 1f || validationIndicator.progress != 0f) {
                        validationIndicator.setMinProgress(0.5f)
                        validationIndicator.setMaxProgress(1.0f)
                        validationIndicator.progress = 0.5f
                        validationIndicator.playAnimation()
                    }
                } else {
                    //invalid
                    if (validationIndicator.progress != 0.5f) {
                        validationIndicator.setMinProgress(0.0f)
                        validationIndicator.setMaxProgress(0.5f)
                        validationIndicator.progress = 0f
                        validationIndicator.playAnimation()
                    }
                }*/
                validationIndicator.isActivated = new
            }

            val context: Context = getApplicationContext()
            val RECORD_REQUEST_CODE = 101
            var field: OTFieldDAO? = null

            //  information to be saved to the database
            var inputModality = AnyInputModalitywithResult(null, false, false, "NA")
            var recordList :MutableList<AnyInputModalitywithResult> = arrayListOf()

            val speechRecognizerUtility = SpeechRecognizerUtility(context)
            val inputProcess = InputProcess(context, inputView)
            val vibrator = getApplicationContext()?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val GLOBAL_SPEECH_MARK = "GLOBAL_SPEECH_MARK"

            init {

                container.addView(inputView, 0)

                hideSpeechInputIcon()

                connectionIndicatorStubProxy = ConnectionIndicatorStubProxy(frame, R.id.ui_connection_indicator_stub)

                timestampIndicator.setOnClickListener(this)

                checkAudioPermission(context)

                speechButton.setOnTouchListener(this)

                ui_speech_global.setOnTouchListener(this)

                setSpeechListener ()

                /*
                optionButton.setOnClickListener {
                    /*
                    val tracker = tracker
                    val fieldLocalId = fieldLocalId
                    if (tracker != null && fieldLocalId != null) {
                        val historyDialog = RecentItemValuePickerBottomSheetFragment.getInstance(tracker._id, fieldLocalId)
                        historyDialog.show(supportFragmentManager, RecentItemValuePickerBottomSheetFragment.TAG)
                    }*/
                }*/
            }

            /* This is a temporary solution to hide speech input icon for image, location, and audio record data field */
            private fun hideSpeechInputIcon(){
                if (inputView.typeId == AFieldInputView.VIEW_TYPE_LOCATION || inputView.typeId == AFieldInputView.VIEW_TYPE_AUDIO_RECORD
                        || inputView.typeId == AFieldInputView.VIEW_TYPE_IMAGE){
                    speechButton.visibility = View.INVISIBLE
                }
            }

            private fun setSpeechListener (){
                speechRecognizerUtility.setRecognitionListener(object : RecognitionListener{
                    override fun onReadyForSpeech(bundle: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(f: Float) {}
                    override fun onBufferReceived(bytes: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(i: Int) {}

                    override fun onResults(bundle: Bundle) {
                        val result = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (result != null) {
                            val inputResult =  result[0]
                            if (inputView.typeId != AFieldInputView.VIEW_TYPE_LONG_TEXT && inputView.typeId != AFieldInputView.VIEW_TYPE_SHORT_TEXT)
                                Toast(this@AItemDetailActivity).showCustomToast(inputResult, Toast.LENGTH_SHORT, this@AItemDetailActivity)
                            passSpeechInputToDataField(inputResult, field)
                        }
                    }

                    override fun onPartialResults(bundle: Bundle) {}
                    override fun onEvent(i: Int, bundle: Bundle?) {}

                })
            }

            private fun passSpeechInputToDataField (inputStr:String, field: OTFieldDAO?) {
                if(field != null){
                  val recognitionSuccess = inputProcess.passInput(inputStr, field)
                    if (recognitionSuccess){
                        inputModality = AnyInputModalitywithResult(field!!.localId, true, true, inputStr)
                    } else {
                        inputModality = AnyInputModalitywithResult(field!!.localId, true, false, inputStr)
                        recordList.add(inputModality)
                        Toast(this@AItemDetailActivity).showErrorToast(inputProcess.errorMessage, Toast.LENGTH_SHORT, this@AItemDetailActivity)
                    }
                }else{ /* Global speech input */
                    inputProcess.sendRequestToPunctuator(inputStr, currentAttributeViewModelList)
                    inputModality = AnyInputModalitywithResult(GLOBAL_SPEECH_MARK, true, true, inputStr)
                }
            }

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (v == speechButton){
                    when (event!!.action) {
                        MotionEvent.ACTION_DOWN ->{
                            vibratePhone()
                            field = currentAttributeViewModelList.get(this.layoutPosition).fieldDAO
                            speechRecognizerUtility.start()
                            startAnimationEffect()
                        }

                        MotionEvent.ACTION_UP ->{
                            stopAnimationEffect()
                            speechRecognizerUtility.stop()

                        }
                    }
                }

                if(v == ui_speech_global){
                    when (event!!.action) {
                        MotionEvent.ACTION_DOWN ->{
                            vibratePhone()
                            field = null
                            speechRecognizerUtility.start()
                        }

                        MotionEvent.ACTION_UP ->{
                            speechRecognizerUtility.stop()
                        }
                    }
                }
                return false
            }

            override fun onClick(v: View?) {
                if (v === timestampIndicator) {
                    this.setTimestampIndicatorText(this.itemTimestamp)
                }
            }

            private fun checkAudioPermission(context:Context) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {// M = 23
                    if (ContextCompat.checkSelfPermission(context, "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this@AItemDetailActivity, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_REQUEST_CODE)
                    }
                }
            }

            private fun vibratePhone(){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {// M = 26
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(100)
                }
            }

            //TODO: find a way to make this function run once per minute
            private fun setTimestampIndicatorText(timestamp: Long?) {
                this.itemTimestamp = timestamp
                if (timestamp == null || timestamp == 0L || !this.currentValidationState) {
                    timestampIndicator.text = ""
                } else {
                    val now = System.currentTimeMillis()
                    timestampIndicator.text = if (now - timestamp < 1 * DateUtils.MINUTE_IN_MILLIS) {
                        resources.getString(R.string.time_just_now)
                    } else {
                        DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL)
                    }
                }
            }

            private fun startAnimationEffect(){
                timestampIndicator.visibility = View.INVISIBLE
                speech_anim.visibility = View.VISIBLE
                speech_anim.playAnimation()
            }

            private fun stopAnimationEffect(){
                speech_anim.pauseAnimation()
                speech_anim.progress = 0f
                speech_anim.visibility = View.INVISIBLE
                timestampIndicator.visibility = View.VISIBLE
            }

            override fun onBindField(attributeViewModel: ItemEditionViewModelBase.AttributeInputViewModel, position: Int) {

                inputView.position = position

                var required = false

                InterfaceHelper.alertBackground(this.itemView)

                //validationIndicator.isActivated = attributeViewModel.isFilled

                internalSubscriptions.clear()

                val field = currentAttributeViewModelList.get(this.layoutPosition).fieldDAO

                (applicationContext as OTAndroidApp).applicationComponent.getAttributeViewFactoryManager().get(attributeViewModel.fieldDAO.type).refreshInputViewUI(inputView, attributeViewModel.fieldDAO)
                internalSubscriptions.add(
                        attributeViewModel.columnNameObservable.subscribe { name ->
                            columnNameView.text = name
                        }
                )

                internalSubscriptions.add(
                        attributeViewModel.isRequiredObservable.subscribe {
                            if(it) required = true
                            requiredMarker.visibility = if (it) {
                                View.VISIBLE
                            } else {
                                View.INVISIBLE
                            }
                        }
                )

                internalSubscriptions.add(
                        inputView.valueChanged.observable.subscribe { (_, args) ->
                            val now = System.currentTimeMillis()
                            attributeViewModel.value = AnyValueWithTimestamp(args, now)

                            if(inputModality.isSpeech){
                                recordList.add(inputModality)
                                inputModality = AnyInputModalitywithResult(null, false, false, "NA")
                            }else{
                                inputModality.field_Id = field.localId
                                inputModality.originalInput = inputView.value.toString()
                                recordList.add(inputModality)
                            }
                            attributeViewModel.inputModalityList = recordList
                            println("metadata recordList in detail (AItem): ${attributeViewModel.inputModalityList}")
                        }
                )

                internalSubscriptions.add(
                        attributeViewModel.validationObservable.subscribe { isValid ->
                            currentValidationState = isValid
                            if (isValid) {
                                requiredMarker.visibility = View.INVISIBLE
                            } else {
                                if (required) requiredMarker.visibility = View.VISIBLE
                            }
                        }
                )

                internalSubscriptions.add(
                        attributeViewModel.filledObservable.subscribe { isFilled->
                            validationIndicator.isActivated = isFilled
                        }
                )

                internalSubscriptions.add(
                        attributeViewModel.onAttributeChanged.subscribe {
                            (applicationContext as OTAndroidApp).applicationComponent.getAttributeViewFactoryManager().get(attributeViewModel.fieldDAO.type).refreshInputViewUI(inputView, attributeViewModel.fieldDAO)
                        }
                )


                connectionIndicatorStubProxy.onBind(attributeViewModel.fieldDAO, attributeViewModel.fieldDAO.getParsedConnection(this@AItemDetailActivity))

                internalSubscriptions.add(
                        attributeViewModel.stateObservable.observeOn(AndroidSchedulers.mainThread()).subscribe { state ->
                            if (state == OTItemBuilderWrapperBase.EAttributeValueState.Idle) {
                                container.locked = false
                                inputView.alpha = 1.0f
                            } else {
                                container.locked = true
                                inputView.alpha = 0.12f
                            }
                            when (state) {
                                OTItemBuilderWrapperBase.EAttributeValueState.Idle -> {
                                    loadingIndicatorInContainer.visibility = View.GONE
                                }
                                OTItemBuilderWrapperBase.EAttributeValueState.Processing -> {
                                    loadingIndicatorInContainer.visibility = View.VISIBLE
                                }
                                OTItemBuilderWrapperBase.EAttributeValueState.GettingExternalValue -> {
                                    loadingIndicatorInContainer.visibility = View.VISIBLE
                                }
                            }
                        }
                )

                internalSubscriptions.add(
                        attributeViewModel.valueObservable.observeOn(AndroidSchedulers.mainThread()).subscribe { valueNullable ->
                            if (inputView.value != valueNullable.datum?.value) {
                                inputView.valueChanged.suspend = true
                                inputView.setAnyValue(valueNullable.datum?.value)
                                inputView.valueChanged.suspend = false
                            }

                            TooltipCompat.setTooltipText(timestampIndicator, valueNullable.datum?.timestamp?.let { (applicationContext as OTAndroidApp).applicationComponent.getLocalTimeFormats().FORMAT_DATETIME.format(Date(it)) })
                            setTimestampIndicatorText(valueNullable.datum?.timestamp)
                        }
                )

                creationSubscriptions.addAll(internalSubscriptions)

                inputView.boundAttributeObjectId = attributeViewModel.fieldDAO._id


                inputView.onResume()
            }
        }
    }
}
