package com.example.argus3streamer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var hasAllPermissions: Boolean = false

    private val permissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
        this::onPermissionResult
    )

    private val wifiManager: WifiManager by lazy(LazyThreadSafetyMode.NONE) {
        applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val locationManager: LocationManager by lazy(LazyThreadSafetyMode.NONE) {
        applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // request permissions
        permissions.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )

        // set click listeners
        findViewById<Button>(R.id.buttonDisplay)?.setOnClickListener(this::displayBtnClicked)
        findViewById<Button>(R.id.buttonStream)?.setOnClickListener(this::streamBtnClicked)
    }

    /**
     * returns true if location[network or gps location] is available
     */
    private fun isLocationEnabled(): Boolean {
        var gpsEnabled = false
        var networkEnabled = false

        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return gpsEnabled || networkEnabled
    }

    /**
     * returns true if a condition is not met.
     * returns false if all conditions are satisfied.
     */
    private fun hasNotMetConditions(): Boolean {

        // returns true if permissions were denied
        if (!hasAllPermissions) {
            Toast.makeText(this, " Permissions Needed", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "hasNotMetConditions: Permissions Needed")
            return true
        }

        // returns true if wifi is not enabled
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, " Wifi Should Be turned On", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "hasNotMetConditions: Wifi Should Be turned On")
            return true
        }

        // returns true if location is not enabled
        if (!isLocationEnabled()) {
            Toast.makeText(this, " Turn on Location", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "hasNotMetConditions: Turn on Location")
            return true
        }

        // return false as all conditions have been met
        return false
    }

    /**
     * starts display activity
     */
    private fun displayBtnClicked(view: View) {

        // return if conditions[ permissions, wifi, location] not satisfied
        if (hasNotMetConditions())
            return

        // move to display activity
        Log.d(TAG, "Starting DisplayActivity")
        val i = Intent(this, DisplayActivity::class.java)
        startActivity(i)
    }

    /**
     * starts stream activity.
     */
    private fun streamBtnClicked(view: View) {

        // return if conditions[ permissions, wifi, location] not satisfied
        if (hasNotMetConditions())
            return

        // move to stream activity
        Log.d(TAG, "Starting StreamActivity")
        val i = Intent(this, StreamActivity::class.java)
        startActivity(i)
    }

    private fun onPermissionResult(results: Map<String, Boolean>) {
        Log.d(TAG, "onPermissionResult : $results")
        val hasCameraPermission = results[Manifest.permission.CAMERA] ?: false
        val hasLocationPermission = results[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        hasAllPermissions = hasCameraPermission && hasLocationPermission
        // todo show snack bar that takes to permission setting if permissions were denied.
    }
}
