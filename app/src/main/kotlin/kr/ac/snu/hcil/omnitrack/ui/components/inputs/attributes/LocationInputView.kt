package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewStub
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.LatLngToAddressTask
import kr.ac.snu.hcil.omnitrack.utils.contains
import kr.ac.snu.hcil.omnitrack.utils.getActivity

/**
 * Created by Young-Ho on 8/3/2016.
 *
 * https://github.com/googlesamples/android-play-places/blob/master/PlaceCompleteFragment/Application/src/main/java/com/example/google/playservices/placecompletefragment/MainActivity.java
 *
 */
class LocationInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<LatLng>(R.layout.component_location_picker, context, attrs), OnMapReadyCallback, View.OnClickListener, LatLngToAddressTask.OnFinishListener, GoogleMap.OnCameraIdleListener {

    companion object {
        const val REQUEST_TYPE_GOOGLE_PLACE_PICKER = 2
    }

    override val typeId: Int = VIEW_TYPE_LOCATION

    override var value: LatLng = LatLng(0.0, 0.0)
        set(value) {
            if (field != value) {
                field = value
                fitToValueLocation(false)
                onValueChanged(value)
                //addressView.text = value.getAddress(context)?.getAddressLine(0)
                reserveAddressChange(value)
            }
        }

    private var isAdjustMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value

                if (value == true) // adjustMode
                {
                    controlPanel.visibility = View.GONE

                    if (adjustPanel == null) {
                        adjustPanel = inflateAdjustPanel()
                    } else {
                        adjustPanel?.visibility = View.VISIBLE
                    }

                    colorFrame.setBackgroundResource(R.drawable.map_view_frame_adjust_mode)


                } else {
                    controlPanel.visibility = View.VISIBLE

                    if (adjustPanel != null) {
                        adjustPanel?.visibility = View.GONE
                    }

                    colorFrame.setBackgroundResource(R.drawable.map_view_frame)
                }

                refreshMap()
            }
        }


    private val mapView: MapView

    private val controlPanel: View

    private val searchButton: View
    private val zoomInButton: View
    private val zoomOutButton: View
    private val adjustButton: View

    private val fitButton: View

    private val addressView: TextView

    private val colorFrame: View

    private val addressBusyIndicator: ProgressBar

    private val adjustPanelStub: ViewStub

    private var adjustPanel: View? = null

    private var adjustOkButton: View? = null
    private var adjustCancelButton: View? = null

    private var googleMap: GoogleMap? = null

    private var valueMarker: Marker? = null

    private var isLocationConversionTaskRunning = false
    private var queuedLocationToConvert: LatLng? = null

    init {

        mapView = findViewById(R.id.ui_map) as MapView

        addressView = findViewById(R.id.ui_address) as TextView

        addressBusyIndicator = findViewById(R.id.ui_address_busy_indicator) as ProgressBar
        addressBusyIndicator.visibility = View.INVISIBLE

        colorFrame = findViewById(R.id.ui_mapview_frame)

        controlPanel = findViewById(R.id.ui_control_panel)
        adjustPanelStub = findViewById(R.id.ui_adjust_panel_stub) as ViewStub

        zoomInButton = findViewById(R.id.ui_button_zoom_in)
        zoomOutButton = findViewById(R.id.ui_button_zoom_out)
        fitButton = findViewById(R.id.ui_button_fit)
        adjustButton = findViewById(R.id.ui_button_adjust)

        searchButton = findViewById(R.id.ui_button_search)

        zoomInButton.setOnClickListener(this)
        zoomOutButton.setOnClickListener(this)
        fitButton.setOnClickListener(this)
        adjustButton.setOnClickListener(this)
        searchButton.setOnClickListener(this)
    }

    private fun inflateAdjustPanel(): View {
        val view = adjustPanelStub.inflate()

        adjustOkButton = view.findViewById(R.id.ui_button_apply)
        adjustCancelButton = view.findViewById(R.id.ui_button_cancel)

        adjustOkButton?.setOnClickListener(this)
        adjustCancelButton?.setOnClickListener(this)

        return view
    }

    override fun onClick(view: View) {
        if (view === zoomInButton) {
            googleMap?.animateCamera(CameraUpdateFactory.zoomBy(1.0f))
        } else if (view === zoomOutButton) {
            googleMap?.animateCamera(CameraUpdateFactory.zoomBy(-1.0f))
        } else if (view === fitButton) {
            fitToValueLocation(animate = true)
        } else if (view === adjustButton) {
            isAdjustMode = true
        } else if (view === adjustOkButton) {

            if (googleMap != null)
                value = googleMap!!.cameraPosition.target

            isAdjustMode = false

        } else if (view === adjustCancelButton) {
            isAdjustMode = false
            reserveAddressChange(value)
            fitToValueLocation(animate = true)
        } else if (view === searchButton) {
            val activity = getActivity()
            if (activity != null) {
                if (position >= 0) {
                    val intent = PlacePicker.IntentBuilder().build(activity)
                    activity.startActivityForResult(intent, makeActivityForResultRequestCode(position, REQUEST_TYPE_GOOGLE_PLACE_PICKER))
                }
            }
            //getActivity()?.startActivityForResult()

        }
    }

    override fun setValueFromActivityResult(data: Intent, requestType: Int): Boolean {
        if (requestType == REQUEST_TYPE_GOOGLE_PLACE_PICKER) {
            val place = PlacePicker.getPlace(this.context, data)
            value = place.latLng
            return true
        } else return false
    }

    private fun fitToValueLocation(animate: Boolean) {
        if (googleMap != null) {

            val update = CameraUpdateFactory.newCameraPosition(CameraPosition.builder()
                    .target(value)
                    .zoom(14.0f)
                    .bearing(0f)
                    .build())

            if (animate) {
                googleMap?.animateCamera(update);
            } else {
                googleMap?.moveCamera(update);
            }

            if (valueMarker == null) {
                valueMarker = googleMap?.addMarker(MarkerOptions().position(value).draggable(false))
            } else {
                valueMarker?.position = value
            }
        }
    }

    private fun refreshMap() {
        if (isAdjustMode) {
            valueMarker?.alpha = 0.5f
        } else {
            valueMarker?.alpha = 1.0f
        }
    }

    override fun onCameraIdle() {
        if (isAdjustMode) {
            reserveAddressChange(googleMap!!.cameraPosition.target)
        }
    }


    private fun reserveAddressChange(location: LatLng) {
        if (isLocationConversionTaskRunning) {
            queuedLocationToConvert = location
        } else {
            isLocationConversionTaskRunning = true
            LatLngToAddressTask(this, this).execute(location)
            addressBusyIndicator.visibility = View.VISIBLE
        }
    }

    override fun onAddressReceived(address: String?) {
        if (queuedLocationToConvert == null) {
            addressBusyIndicator.visibility = View.INVISIBLE
            isLocationConversionTaskRunning = false
            if (address != null)
                addressView.text = address
        }

        if (queuedLocationToConvert != null) {
            LatLngToAddressTask(this, this).execute(queuedLocationToConvert)
            queuedLocationToConvert = null
        }
    }


    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        //find parent recyclerview
        if (ev.action == MotionEvent.ACTION_DOWN) {
            if (mapView.contains(ev.x, ev.y)) {
                parent.requestDisallowInterceptTouchEvent(true)
            }
        } else if (ev.action == MotionEvent.ACTION_CANCEL || ev.action == MotionEvent.ACTION_UP) {
            parent.requestDisallowInterceptTouchEvent(false)
        }

        return super.dispatchTouchEvent(ev)
    }

    override fun focus() {
    }

    override fun onMapReady(map: GoogleMap?) {
        this.googleMap = map
        this.googleMap?.setMinZoomPreference(3.0f)
        this.googleMap?.setMaxZoomPreference(17.0f)

        this.googleMap?.setOnCameraIdleListener(this)

        fitToValueLocation(false)
    }


    override fun onLowMemory() {
        mapView.onLowMemory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        mapView.onSaveInstanceState(outState)
    }

    override fun onResume() {
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
    }

    override fun onDestroy() {
        mapView.onDestroy()
    }
}