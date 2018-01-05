package kr.ac.snu.hcil.omnitrack.ui.pages.services

import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import io.reactivex.disposables.SerialDisposable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.ui.activities.OTFragment
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.pages.home.MeasureFactoryAdapter
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.dipRound
import kr.ac.snu.hcil.omnitrack.utils.net.NetworkHelper
import org.jetbrains.anko.support.v4.act

/**
 * Created by Young-Ho on 7/29/2016.
 */
class ServiceListFragment : OTFragment() {

    private lateinit var listView: RecyclerView

    private lateinit var adapter: Adapter

    private val activateColor: Int by lazy { ContextCompat.getColor(act, R.color.colorPointed) }

    private val deactivateColor: Int by lazy { ContextCompat.getColor(act, R.color.colorRed_Light) }

    private val onActivatingColor: Int by lazy { ContextCompat.getColor(act, R.color.material_grey_100) }

    private val internetRequiredAlertBuilder: MaterialDialog.Builder by lazy {
        DialogHelper.makeSimpleAlertBuilder(act, act.getString(R.string.msg_external_service_activation_requires_internet))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_home_services, container, false)

        listView = rootView.findViewById(R.id.ui_recyclerview_with_fallback)

        listView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        //listView.addItemDecoration(HorizontalDividerItemDecoration(0, 20))

        adapter = Adapter()

        listView.adapter = adapter

        return rootView
    }

    /*
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        println("permission result: $requestCode ${pendedActivations.size()}")
        if (pendedActivations[requestCode] != null) {
            if (grantResults.indexOf(PackageManager.PERMISSION_DENIED) == -1) {
                creationSubscriptions.add(
                        OTExternalService.availableServices[requestCode].startActivationActivityAsync(context).subscribe({
                            success ->
                            if (success) {
                                pendedActivations[requestCode].holderState = OTExternalService.ServiceState.ACTIVATED
                            } else {
                                pendedActivations[requestCode].holderState = OTExternalService.ServiceState.DEACTIVATED
                            }
                            pendedActivations.removeAt(requestCode)
                        }, { pendedActivations[requestCode].holderState = OTExternalService.ServiceState.DEACTIVATED }, { pendedActivations.removeAt(requestCode) })
                )
            } else {
                //activation failed.
                println("some permissions not granted. activation failed.")
                pendedActivations[requestCode].holderState = OTExternalService.ServiceState.DEACTIVATED
                pendedActivations.removeAt(requestCode)
            }
        }
    }*/

    private inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getService(position))
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.external_service_list_element, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return OTExternalService.availableServices.size
        }

        private fun getService(position: Int): OTExternalService {
            return OTExternalService.availableServices[position]
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            val thumbView: ImageView
            val nameView: TextView
            val descriptionView: TextView

            val activationButton: AppCompatButton
            val progressBar: ProgressBar

            val measureFactoryListView: RecyclerView

            val measureFactoryAdapter = MeasureFactoryAdapter(context!!)

            val serviceStateSubscription = SerialDisposable()

            var holderState: OTExternalService.ServiceState = OTExternalService.ServiceState.DEACTIVATED
                set(value) {
                    if (field != value) {
                        field = value
                        applyState(field)
                    }
                }

            init {
                thumbView = view.findViewById(R.id.thumb)
                nameView = view.findViewById(R.id.name)
                descriptionView = view.findViewById(R.id.description)

                progressBar = view.findViewById(R.id.ui_progress_bar)

                measureFactoryListView = view.findViewById(R.id.ui_supported_measure_list)
                measureFactoryListView.adapter = measureFactoryAdapter
                measureFactoryListView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                measureFactoryListView.addItemDecoration(HorizontalDividerItemDecoration(ContextCompat.getColor(act, R.color.separator_Light), dipRound(0.6f)))

                activationButton = view.findViewById(R.id.ui_button_activate)
                activationButton.background.setColorFilter(activateColor, PorterDuff.Mode.MULTIPLY)
                activationButton.setOnClickListener {
                    val service = getService(adapterPosition)

                    println("service state: ${service.state}")
                    when (service.state) {
                        OTExternalService.ServiceState.ACTIVATED -> {
                            service.deactivate()
                            eventLogger.get().logServiceActivationChangeEvent(service.identifier, false)
                        }
                        OTExternalService.ServiceState.ACTIVATING -> {

                        }
                        OTExternalService.ServiceState.DEACTIVATED -> {
                            if (service.isInternetRequiredForActivation && !NetworkHelper.isConnectedToInternet()) {
                                internetRequiredAlertBuilder.show()
                            } else {
                                /*
                                if (!service.permissionGranted) {
                                    pendedActivations.setValueAt(adapterPosition, this@ViewHolder)
                                    service.grantPermissions(this@ServiceListFragment, adapterPosition)
                                } else {
                                */
                                creationSubscriptions.add(
                                        service.startActivationActivityAsync(act).subscribe({
                                            success ->
                                            if (success)
                                                eventLogger.get().logServiceActivationChangeEvent(service.identifier, true)
                                        }, { })
                                )
                                //}
                            }
                        }
                    }
                }
            }

            private fun applyState(state: OTExternalService.ServiceState) {
                TransitionManager.beginDelayedTransition(view as ViewGroup)
                when (state) {
                    OTExternalService.ServiceState.ACTIVATED -> {
                        progressBar.visibility = View.GONE
                        activationButton.setText(R.string.msg_deactivate)
                        activationButton.background.setColorFilter(deactivateColor, PorterDuff.Mode.MULTIPLY)
                        activationButton.isEnabled = true
                    }
                    OTExternalService.ServiceState.ACTIVATING -> {
                        progressBar.visibility = View.VISIBLE
                        activationButton.background.setColorFilter(onActivatingColor, PorterDuff.Mode.MULTIPLY)
                        activationButton.setText(R.string.msg_service_activating)
                        activationButton.isEnabled = false
                    }
                    OTExternalService.ServiceState.DEACTIVATED -> {
                        progressBar.visibility = View.GONE
                        activationButton.setText(R.string.msg_activate)
                        activationButton.background.setColorFilter(activateColor, PorterDuff.Mode.MULTIPLY)
                        activationButton.isEnabled = true
                    }
                }
            }

            fun bind(service: OTExternalService) {
                nameView.text = act.resources.getString(service.nameResourceId)
                descriptionView.text = act.resources.getString(service.descResourceId)
                thumbView.setImageResource(service.thumbResourceId)

                measureFactoryAdapter.service = service

                holderState = service.state

                val serviceStateSubscription = service.onStateChanged.subscribe {
                    state ->
                    holderState = state
                }

                this.serviceStateSubscription.set(serviceStateSubscription)
                creationSubscriptions.add(serviceStateSubscription)
            }
        }
    }
}