package com.example.awignassignment

import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.awignassignment.databinding.ActivityMainBinding
import com.example.awignassignment.utils.LOCATION_RT_REQUEST_CODE
import com.example.awignassignment.utils.RTPermissions
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {


    private var circle: Circle? = null
    private var googleMap: GoogleMap? = null
    private var mapFragment: SupportMapFragment? = null
    private lateinit var viewBinding: ActivityMainBinding
    private val defaultZoom: Float = 15F
    private val defaultLocation = LatLng(-33.8523341, 151.2106085)
    private val rtPermissions: RTPermissions by lazy { RTPermissions(this) }
    private val fusedLocationProviderClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        mapFragment = supportFragmentManager.findFragmentById(R.id.mapFrag) as SupportMapFragment?
        mapFragment?.getMapAsync(mapReadyCallback)


    }

    private val mapReadyCallback = OnMapReadyCallback { asyncMap ->
        /* do activities after map is created*/
        googleMap = asyncMap
        setClickListenersNow()
        checkLocationPermission()

    }

    private fun setClickListenersNow() {

        googleMap?.setOnMapClickListener { latLng ->
            /* add marker here */
            circle?.let { crc ->

                val distance = FloatArray(2)
                Location.distanceBetween(
                    latLng.latitude, latLng.longitude, crc.center.latitude, crc.center.longitude,
                    distance
                );

                if (distance[0] <= crc.radius) {
                    googleMap?.addMarker(
                        MarkerOptions().position(latLng).title("New position")
                    )
                } else {
                    Toast.makeText(
                        this, "You Can not place marker outside highlighted  area",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }

        }

    }

    private fun checkLocationPermission() {
        if (rtPermissions.isLocationGranted()) {
            checkGPSStatus()
            updateMapView()
        } else {
            rtPermissions.requestPermissionForLocation()
        }

    }

    private fun checkGPSStatus() {
        val setClient = LocationServices.getSettingsClient(this)
        val builder =
            LocationSettingsRequest.Builder()
                .addLocationRequest(LocationRequest.create().apply {
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                }).setAlwaysShow(true)

        val task = setClient.checkLocationSettings(builder.build())
        task.apply {
            addOnSuccessListener { response ->
                if (response.locationSettingsStates.isLocationPresent) {
                    updateMapView()
                }
            }
            addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        exception.startResolutionForResult(this@MainActivity, 9195)
                    } catch (e: IntentSender.SendIntentException) {
                        e.printStackTrace()
                    }
                }

            }
        }

    }

    private fun updateMapView() {
        googleMap?.let { gMap ->
            try {
                updateLocationPinOnMap()
                gMap.isMyLocationEnabled = true
                gMap.uiSettings.isMyLocationButtonEnabled = true

            } catch (e: SecurityException) {
                Log.e("Exception: %s", e.message, e)
            }

        }

    }

    private fun changeMyLocationPosition() {
        mapFragment?.apply {
            val locationButton =
                (view?.findViewById<View>(Integer.parseInt("1"))?.parent as View).findViewById<View>(
                    Integer.parseInt("2")
                )
            val rlp = locationButton.layoutParams as RelativeLayout.LayoutParams
            rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
            rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            rlp.setMargins(0, 0, 30, 30)
        }


    }

    private fun updateLocationPinOnMap() {
        try {
            val locationResult = fusedLocationProviderClient.lastLocation
            locationResult.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    task.result?.let { loc ->
                        googleMap?.uiSettings?.isMyLocationButtonEnabled = true

                        googleMap?.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(loc.latitude, loc.longitude), defaultZoom
                            )
                        )
                        addCircleToMap(LatLng(loc.latitude, loc.longitude))

                    }
                } else {
                    googleMap?.apply {
                        moveCamera(
                            CameraUpdateFactory.newLatLngZoom(defaultLocation, defaultZoom)
                        )
                        uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }

        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun addCircleToMap(loc: LatLng) {
        val circleOptions = CircleOptions().center(LatLng(loc.latitude, loc.longitude))
            .radius(800.0)
            .strokeWidth(1.0f)
            .strokeColor(ContextCompat.getColor(this, R.color.purple_700))
            .fillColor(ContextCompat.getColor(this, R.color.purple_200))
        circle?.remove()
        circle = googleMap?.addCircle(circleOptions)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 9195) {
            if (resultCode == RESULT_OK) {
                updateMapView()
            } else {
                Toast.makeText(
                    this, "Location is needed to run this application",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_RT_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkGPSStatus()
                } else {
                    rtPermissions.requestPermissionForLocation()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkGPSStatus()
    }


}