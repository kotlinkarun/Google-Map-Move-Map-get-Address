package com.poly

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.AsyncTask
import android.os.Looper
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.text.DateFormat
import java.util.*


class GetLocation(val context: AppCompatActivity){




    companion object {
        private val TAG = GetLocation::class.java.simpleName
        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 200000
      //  private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 100000
       // private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 5000
        const val REQUEST_CHECK_SETTINGS = 100

       val  PLACE_AUTOCOMPLETE_REQUEST_CODE=424
    }

    private var mLastUpdateTime: String? = null


    // bunch of location related apis
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mSettingsClient: SettingsClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var mLocationSettingsRequest: LocationSettingsRequest? = null
    private var mLocationCallback: LocationCallback? = null
    private var mCurrentLocation: Location? = null

    // boolean flag to toggle the ui
    var mRequestingLocationUpdates: Boolean? = null


    init {
        init()
     }

    /*Init Location */
    private fun init() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        mSettingsClient = LocationServices.getSettingsClient(context)
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                // location is received
                mCurrentLocation = locationResult!!.lastLocation
                mLastUpdateTime = DateFormat.getTimeInstance().format(Date())
                updateLocationUI()
            }
        }
        mRequestingLocationUpdates = false
        mLocationRequest = LocationRequest()
        mLocationRequest!!.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest!!.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        mLocationSettingsRequest = builder.build()

        updateLocationUI()
    }

    /* Check Runtime permission*/
    fun startLocation() {
        // Requesting ACCESS_FINE_LOCATION using Dexter library
        Dexter.withActivity(context)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                        mRequestingLocationUpdates = true
                        startLocationUpdates()
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse) {
                        if (response.isPermanentlyDenied) {
                            // open device settings when the permission is
                            // denied permanently
                            openSettings()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken) {
                        token.continuePermissionRequest()
                    }
                }).check()
    }

    /* Open Setting enable GPS*/
    private fun openSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
        intent.data = uri
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /* again Check Runtime permission*/
    fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }


    @SuppressLint("MissingPermission")
    /*Start Update......*/
    fun startLocationUpdates() {
        mSettingsClient!!
                .checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(context) {
                    Log.i(TAG, "All location settings are satisfied.")
                    Toast.makeText(context, "Started location updates!", Toast.LENGTH_SHORT).show()
                    mFusedLocationClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback!!, Looper.myLooper())
                    updateLocationUI()
                }
                .addOnFailureListener(context) { e ->
                    val statusCode = (e as ApiException).statusCode
                    when (statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                            Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " + "location settings ")
                            try {
                                // Show the dialog by calling startResolutionForResult(), and check the
                                // result in onActivityResult().
                                val rae = e as ResolvableApiException
                                rae.startResolutionForResult(context, REQUEST_CHECK_SETTINGS)
                            } catch (sie: IntentSender.SendIntentException) {
                                Log.i(TAG, "PendingIntent unable to execute request.")
                            }

                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            val errorMessage = "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings."
                            Log.e(TAG, errorMessage)

                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }

                    updateLocationUI()
                }
    }

   /* send broad cast lattude and .longtitude*/
    fun updateLocationUI() {
        if (mCurrentLocation != null) {

           // val s = "https://raw.githubusercontent.com/springkarun/api/master/.gitignore/t.json"
        //    val url ="https://maps.googleapis.com/maps/api/geocode/json?" + "latlng=" + mCurrentLocation!!.latitude + "," + mCurrentLocation!!.longitude + "&key=" + context.resources.getString(R.string.google_maps_key)
        //    RetrieveFeedTask().execute(url)


            val i = Intent("key_action")
            i.putExtra("lat", mCurrentLocation!!.latitude)
            i.putExtra("lng", mCurrentLocation!!.longitude)
            i.putExtra("add", getCompleteAddressString(LatLng(mCurrentLocation!!.latitude,mCurrentLocation!!.longitude)))
            LocalBroadcastManager.getInstance(context).sendBroadcast(i)
            Toast.makeText(context, "Lat loc: " + mCurrentLocation!!.latitude + ", Lng: " + mCurrentLocation!!.longitude, Toast.LENGTH_LONG).show()
        }
    }

    /*Show last location*/
    fun showLastKnownLocation() {
        if (mCurrentLocation != null) {
            Toast.makeText(context, "Lat: " + mCurrentLocation!!.latitude + ", Lng: " + mCurrentLocation!!.longitude, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Last known location is not available!", Toast.LENGTH_SHORT).show()
        }
    }

    /* Stop updated...*/
    fun stopLocationUpdates() {
        mRequestingLocationUpdates = false
        mFusedLocationClient!!
                .removeLocationUpdates(mLocationCallback!!)
                .addOnCompleteListener(context) {
                    Toast.makeText(context, "Location updates stopped!", Toast.LENGTH_SHORT).show()

                }
    }



     fun getCompleteAddressString(add:LatLng): String {
        var strAdd = ""
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(add.latitude, add.longitude, 1)
            if (addresses != null) {
                val returnedAddress = addresses[0]
                val strReturnedAddress = StringBuilder("")

                for (i in 0..returnedAddress.getMaxAddressLineIndex()) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n")
                }
                strAdd = strReturnedAddress.toString()
                Log.w("My address", strReturnedAddress.toString())
            } else {
                Log.w("My  address", "No Address returned!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.w("My  address", "Canont get Address!")
        }

        return strAdd
    }


    @SuppressLint("StaticFieldLeak")
    inner class RetrieveFeedTask : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg urls: String): String {
              val s: String
            val client = OkHttpClient();
            val request = Request.Builder().url(urls[0]).build();
            val response = client.newCall(request).execute().body()!!.string()
            val json = JSONObject(response)
            if (json.getString("status") == "OK") {
                val arr = json.getJSONArray("results")
                val obj = arr.getJSONObject(0)
                s=obj.getString("formatted_address")
            } else {
                Log.d("TAHS", "You have exceeded your daily request quata full..")
                s="You have exceeded your daily request quata full.."
            }
            return s
        }
        override fun onPostExecute(feed: String) {
            //response....address
        }
    }

}