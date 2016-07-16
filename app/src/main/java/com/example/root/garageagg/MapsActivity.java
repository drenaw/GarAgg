package com.example.root.garageagg;

import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.SyncStateContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.vision.text.Text;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean mPermissionDenied = false;
    private String[] results = null;

    double latitude = 28.466161;
    double longitude = 77.0097144;

    private Location loc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        enableMyLocation();

        String param = "" + latitude + "," + longitude;

        FetchCoordinatesTask fetchCoordinatesTask = new FetchCoordinatesTask();
        fetchCoordinatesTask.execute(param);
                /*for(int i=0;i<10;i++){
            Log.v("FINAL ISHH",results[i]);
        }*/




        /*// Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/

    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    public class FetchCoordinatesTask extends AsyncTask<String,Void,String[]>{

        private final String LOG_TAG = FetchCoordinatesTask.class.getSimpleName();

        private String[] extractFromJSON (String cordsJSONString)
            throws JSONException{

            final String OWM_RESULTS = "results";
            final String OWM_GEOMETRY = "geometry";
            final String OWM_LOCATION = "location";
            final String OWM_LAT = "lat";
            final String OWM_LONG = "lng";
            final String OWM_NAME = "name";

            JSONObject coordinatesJSON = new JSONObject(cordsJSONString);
            JSONArray coordinatesArray = coordinatesJSON.getJSONArray(OWM_RESULTS);

            String[] resultStrs = new String[10];
            for(int i=0;i<coordinatesArray.length()&&i<10;i++){
                String name;
                String lat;
                String lon;

                JSONObject eachGarage = coordinatesArray.getJSONObject(i);
                name = eachGarage.getString(OWM_NAME);

                JSONObject eachGarageLocation = eachGarage.getJSONObject(OWM_GEOMETRY).getJSONObject(OWM_LOCATION);
                lat = eachGarageLocation.getString(OWM_LAT);
                lon = eachGarageLocation.getString(OWM_LONG);

                resultStrs[i] = "" + name + "," + lat + "," + lon;

                //Log.v(LOG_TAG,resultStrs[i]);

            }
            return resultStrs;
        }

        //d283f85200db9c0fc71fa30bac840070


        @Override
        protected String[] doInBackground(String... params) {
            if(params.length==0){
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String coordinatesJSONString = null;

            try{
                final String FETCH_BASE_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?distance=5000&keyword=garage&key=%20AIzaSyDZjQsptxdipWXiKysLVxJ-qmQ3ZW_fMZ0";
                final String LOCATION_PARAM = "location";

                Uri builtUri = Uri.parse(FETCH_BASE_URL).buildUpon()
                        .appendQueryParameter(LOCATION_PARAM,params[0])
                        .build();
                URL url = new URL(builtUri.toString());
                Log.v(LOG_TAG,"Built URI" + builtUri.toString());
                urlConnection = (HttpURLConnection)url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if(inputStream==null){
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while((line=reader.readLine())!=null){
                    buffer.append(line + "\n");
                }
                if(buffer.length()==0){
                    return null;
                }
                coordinatesJSONString = buffer.toString();
                ///log.v(LOG_TAG,"Coordinates JSON String: " + coordinatesJSONString);

                /*for (String lin:coordinatesJSONString.split("\n")){
                    Log.v(LOG_TAG,lin);
                }*/
            }catch (IOException e){
                Log.e("ERROR WHILE FETCHING","Error",e);
                return null;
            }finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("PlaceholderFragment", "Error closing stream", e);
                    }
                }
            }

            try{
                return extractFromJSON(coordinatesJSONString);
            }catch(JSONException e){
                Log.e(LOG_TAG,e.getMessage(),e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String[] strings) {
            super.onPostExecute(strings);

            for(int i=0;i<10;i++){
                String[] parts = strings[i].split(",");
                //Log.v("MARK",parts[0] + "##" + parts[1] + "##" + parts[2]);

                /*// Add a marker in Sydney and move the camera
                LatLng sydney = new LatLng(-34, 151);
                mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/

                Double d1 = Double.parseDouble(parts[1]);
                Double d2 = Double.parseDouble(parts[2]);
                LatLng garage = new LatLng(d1,d2);
                mMap.addMarker(new MarkerOptions().position(garage).title(parts[0]));
            }
        }
    }
}
