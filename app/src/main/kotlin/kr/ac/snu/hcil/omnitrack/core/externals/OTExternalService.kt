package kr.ac.snu.hcil.omnitrack.core.externals

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.support.v4.app.Fragment
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.core.externals.device.AndroidDeviceService
import kr.ac.snu.hcil.omnitrack.core.externals.google.fit.GoogleFitService
import kr.ac.snu.hcil.omnitrack.core.externals.microsoft.band.MicrosoftBandService
import kr.ac.snu.hcil.omnitrack.core.externals.shaomi.miband.MiBandService
import kr.ac.snu.hcil.omnitrack.utils.INameDescriptionResourceProvider
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*

/**
 * Created by younghokim on 16. 7. 28..
 */
abstract class OTExternalService(val identifier: String, val minimumSDK: Int) : INameDescriptionResourceProvider {
    companion object {

        const val REQUEST_CODE_GOOGLE_FIT = 0

        val availableServices: Array<OTExternalService> by lazy {
            arrayOf(AndroidDeviceService, GoogleFitService, MicrosoftBandService, MiBandService)
        }

        private val preferences: SharedPreferences
            get() = OmniTrackApplication.app.getSharedPreferences("ExternalServices", Context.MODE_PRIVATE)


        /***
         * Get whether the service's activation state stored in system
         */
        fun getIsActivatedFlag(service: OTExternalService): Boolean {
            return getIsActivatedFlag(service.identifier)
        }

        fun getIsActivatedFlag(serviceIdentifier: String): Boolean {
            return preferences.getBoolean(serviceIdentifier + "_activated", false)
        }

        fun setIsActivatedFlag(service: OTExternalService, isActivated: Boolean) {
            preferences.edit().putBoolean(service.identifier + "_activated", isActivated).apply()
        }
    }

    enum class ServiceState {
        DEACTIVATED, ACTIVATING, ACTIVATED
    }

    open val permissionGranted: Boolean
        get() {
            for (permission in requiredPermissionsRecursive) {
                if (OmniTrackApplication.app.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            return true
        }

    protected open val _measureFactories = ArrayList<OTMeasureFactory>()

    val measureFactories: List<OTMeasureFactory> get() {
        return _measureFactories
    }

    var state: ServiceState = ServiceState.DEACTIVATED
        protected set


    init {
        state = if (getIsActivatedFlag(identifier) == true) {
            ServiceState.ACTIVATED
        } else {
            ServiceState.DEACTIVATED
        }
    }

    fun activateAsync(context: Context, connectedHandler: ((Boolean) -> Unit)? = null) {
        state = ServiceState.ACTIVATING

        onActivateAsync(context, {
            result ->
            if (result == true) {
                setIsActivatedFlag(this, true)
                state = ServiceState.ACTIVATED
            }

            connectedHandler?.invoke(result)
        })
    }

    abstract fun onActivateAsync(context: Context, connectedHandler: ((Boolean) -> Unit)? = null)


    fun deactivate() {
        setIsActivatedFlag(this, false)
        state = ServiceState.DEACTIVATED
        onDeactivate()
    }

    abstract fun onDeactivate()

    val activated = Event<Any>()
    val deactivated = Event<Any>()

    open val requiredPermissions: Array<String> = arrayOf()

    abstract val thumbResourceId: Int

    fun grantPermissions(caller: Fragment, requestCode: Int) {
        caller.requestPermissions(requiredPermissionsRecursive, requestCode)
    }

    abstract fun prepareServiceAsync(preparedHandler: ((Boolean) -> Unit)?)

    protected val requiredPermissionsRecursive: Array<String> by lazy {
        val result = HashSet<String>()

        result.addAll(requiredPermissions)
        for (factory in _measureFactories) {
            result.addAll(factory.requiredPermissions)
        }

        result.toTypedArray()
    }

    abstract fun handleActivityActivationResult(resultCode: Int)

}