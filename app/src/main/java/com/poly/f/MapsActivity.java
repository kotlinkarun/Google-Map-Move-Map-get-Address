package com.poly.f;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.poly.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener
 {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static String TAG = "MAP LOCATION";
    private Context mContext;
    private TextView edt_pickup_location,edt_drop_location;
    private LatLng mCenterLatLong;
    private TextView btn_pickup,btn_drop;
    private ImageView imageMarker,btn_current_location;

    private double current_latitude = 0.0;
    private double current_longitude = 0.0;


    private SharedPreferences pref_pickup;
    private SharedPreferences pref_drop;
    private String current_pickup_address;
    private String current_drop_address;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mContext = this;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    askLocationSettings();
                    checkLocationPermission();
                    getMyLocation();
                    buildGoogleApiClient();

            }
        } catch (Exception e) {}

        pref_pickup = getSharedPreferences("MyPickup", Context.MODE_PRIVATE);
        pref_drop = getSharedPreferences("MyDrop", Context.MODE_PRIVATE);


//------------------------------------------------

        edt_pickup_location = findViewById(R.id.edt_pickup_location);
        edt_drop_location = findViewById(R.id.edt_drop_location);
        btn_current_location = findViewById(R.id.btn_current_location);
        btn_pickup = findViewById(R.id.btn_pickup);
        btn_drop = findViewById(R.id.btn_drop);
        imageMarker = findViewById(R.id.imageMarker);
//--------------------------------------------------

        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);


        btn_current_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            askLocationSettings();
                            checkLocationPermission();
                            getMyLocation();
                            buildGoogleApiClient();
                        }
                    }
                } catch (Exception e) {}
            }
        });


//-----------------------------Pickup-----Click----------------------------------
        btn_pickup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageMarker.setImageResource(R.drawable.ic_pin_pickup);

                SharedPreferences.Editor edit = pref_drop.edit();
                edit.putString("key_pickup_lat", String.valueOf(mCenterLatLong.latitude));
                edit.putString("key_drop_long", String.valueOf(mCenterLatLong.longitude));
                edit.putString("key_address",String.valueOf(current_drop_address));
                edit.commit();

                String s=pref_pickup.getString("key_address","");
                String drop_lat=pref_pickup.getString("key_pickup_lat","");
                String drop_long=pref_pickup.getString("key_drop_long","");
                if(s==null || s=="" && drop_lat==null || drop_lat==""  && drop_long==null || drop_long=="" ) {
                    edt_pickup_location.setText("");
                }
                else {
                    mMap.clear();
                    edt_pickup_location.setText(s);
                    LatLng latLng = new LatLng(Double.valueOf(drop_lat.trim()).doubleValue(), Double.valueOf(drop_long.trim()).doubleValue());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));
                }


                mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                    @Override
                    public void onCameraIdle() {
                        current_pickup_address=onCameraPositionChanged_Pickup(mMap.getCameraPosition());
                        if(current_pickup_address==null) {
                            edt_pickup_location.setText("");
                        }else edt_pickup_location.setText(current_pickup_address);
               }});
            }
        });

//-----------------------------Drop-----Click----------------------------------
        btn_drop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageMarker.setImageResource(R.drawable.ic_pin_drop);
                SharedPreferences.Editor edit = pref_pickup.edit();
                edit.putString("key_pickup_lat", String.valueOf(mCenterLatLong.latitude));
                edit.putString("key_drop_long", String.valueOf(mCenterLatLong.longitude));
                edit.putString("key_address",String.valueOf(current_pickup_address));
                edit.commit();


                String s=pref_drop.getString("key_address","");
                String drop_lat=pref_drop.getString("key_pickup_lat","");
                String drop_long=pref_drop.getString("key_drop_long","");
                if(s==null || s=="" && drop_lat==null || drop_lat==""  && drop_long==null || drop_long=="" ) {
                    edt_drop_location.setText("");
                }
                else {
                    mMap.clear();
                    edt_drop_location.setText(s);
                    LatLng latLng = new LatLng(Double.valueOf(drop_lat.trim()).doubleValue(), Double.valueOf(drop_long.trim()).doubleValue());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));
                }

                mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                 @Override
                 public void onCameraIdle() {
                     current_drop_address=onCameraPositionChanged_Drop(mMap.getCameraPosition());
                     edt_drop_location.setText(current_drop_address);

                 }});
            }
        });
    }
 //--------------------------------end Drop-----------------------------------

//-----------------------------Check Runtime Permissions--------------------

    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(this, "Don't Permission denied ", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }


private boolean checkPlayServices() {
    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
    if (resultCode != ConnectionResult.SUCCESS) {
        if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
            GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
        } else {
            //finish();
        }
        return false;
    }
    return true;
}

//--------Ask----settings----------------
 private void askLocationSettings(){

     if (checkPlayServices()) {
         if (!isLocationEnabled(mContext)) {
             AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
             dialog.setMessage("Location not enabled!");
             dialog.setPositiveButton("Open location settings", new DialogInterface.OnClickListener() {
                 @Override
                 public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                     Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                     startActivity(myIntent);
                 }
             });
             dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                 @Override
                 public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                 }
             });
             dialog.show();
         }
         buildGoogleApiClient();
     } else {
         Toast.makeText(mContext, "Location not supported in this device", Toast.LENGTH_SHORT).show();
     }

 }

    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        try {
            locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return locationMode != Settings.Secure.LOCATION_MODE_OFF;
    }

//----------getCurrent Location-------------------------
    private void getMyLocation() {
        mMap.clear();
        LatLng latLng = new LatLng(current_latitude,current_longitude);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15.0f);
        mMap.animateCamera(cameraUpdate);
    }

    private String onCameraPositionChanged_Pickup(CameraPosition position) {
        mCenterLatLong = position.target;
        mMap.clear();
        try {
            Location mLocation = new Location("");
            mLocation.setLatitude(mCenterLatLong.latitude);
            mLocation.setLongitude(mCenterLatLong.longitude);
            LatLng latLongs = new LatLng(mCenterLatLong.latitude,mCenterLatLong.longitude);
            return  getAddress(latLongs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String onCameraPositionChanged_Drop(CameraPosition position) {
        mCenterLatLong = position.target;
        mMap.clear();
        try {
            Location mLocation = new Location("");
            mLocation.setLatitude(mCenterLatLong.latitude);
            mLocation.setLongitude(mCenterLatLong.longitude);
            LatLng latLongs = new LatLng(mCenterLatLong.latitude,mCenterLatLong.longitude);
            return  getAddress(latLongs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "OnMapReady");
        mMap = googleMap;
        MapStyleOptions style = MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style);
        this.mMap.setMapStyle(style);

        imageMarker.setImageResource(R.drawable.ic_pin_pickup);
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                current_pickup_address=onCameraPositionChanged_Pickup(mMap.getCameraPosition());
                if(current_pickup_address==null) {
                    edt_pickup_location.setText("");
                }else edt_pickup_location.setText(current_pickup_address);
            }});

    }

    @SuppressLint("SetTextI18n")
    private void changeMap(Location location) {
        Log.d(TAG, "Reaching map" + mMap);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (mMap != null) {
            mMap.getUiSettings().setZoomControlsEnabled(false);
            LatLng latLong;
            latLong = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.setMyLocationEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLong, 15.0f));
            LatLng latLongs = new LatLng(location.getLatitude(),location.getLongitude());
            edt_pickup_location.setText(" "+getAddress(latLongs));
        } else {
            Toast.makeText(getApplicationContext(), "Sorry! unable to create maps", Toast.LENGTH_SHORT).show();
        }
    }

    private String getAddress(LatLng location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String addresstxt = "";
        try {
            List<Address> addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1);
            if (null != addresses && !addresses.isEmpty()) {
                addresstxt = "" + addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return addresstxt;
    }
    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            changeMap(mLastLocation);
            Log.d(TAG, "ON connected");

        } else
            try {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

            } catch (Exception e) {
                e.printStackTrace();
            }
        try {
            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(10000);
            mLocationRequest.setFastestInterval(5000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }
    @Override
    public void onLocationChanged(Location location) {
        try {
            LatLng latLng;
            if (location != null)
                current_latitude=location.getLatitude();
                current_longitude=location.getLongitude();
            latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));
            changeMap(location);

            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
    @Override
    protected void onStart() {
        super.onStart();
        try {
            mGoogleApiClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

}
