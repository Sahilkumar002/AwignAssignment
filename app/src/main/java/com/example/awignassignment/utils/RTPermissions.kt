package com.example.awignassignment.utils

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.awignassignment.R

const val LOCATION_RT_REQUEST_CODE = 1


class RTPermissions(private val activity: AppCompatActivity) {


    private val locationArray =
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)


    /* Camera permissions */

    fun isLocationGranted(): Boolean =
        PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
            activity, Manifest.permission.ACCESS_FINE_LOCATION
        )

    fun requestPermissionForLocation() = if (ActivityCompat.shouldShowRequestPermissionRationale(
            activity, Manifest.permission.ACCESS_FINE_LOCATION
        )
    ) {
        showAlertDialog(activity.getString(R.string.locationPermissionNeeded), { _, _ ->
            ActivityCompat.requestPermissions(activity, locationArray, LOCATION_RT_REQUEST_CODE)
        })
    } else {
        ActivityCompat.requestPermissions(activity, locationArray, LOCATION_RT_REQUEST_CODE)
    }


    private fun showAlertDialog(
        message: String, okListener: DialogInterface.OnClickListener,
        cancelListener: DialogInterface.OnClickListener? = null
    ) {
        AlertDialog.Builder(activity)
            .setMessage(message)
            .setPositiveButton(activity.getString(R.string.ok), okListener)
            .setNegativeButton(activity.getString(R.string.cancel_text), cancelListener)
            .create()
            .show()
    }


}