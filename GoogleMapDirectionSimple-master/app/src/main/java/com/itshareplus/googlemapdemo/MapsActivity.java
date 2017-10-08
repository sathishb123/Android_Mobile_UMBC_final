package com.itshareplus.googlemapdemo;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import Modules.DirectionFinder;
import Modules.DirectionFinderListener;
import Modules.Route;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, DirectionFinderListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{

    private Button btnFindPath;
    private EditText etOrigin;
    private EditText etDestination;
    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();
    private ProgressDialog progressDialog;
    private GoogleMap mMap;
    double latitude;
    double longitude;
    private int PROXIMITY_RADIUS = 1000000000;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        //Check if Google Play Services Available or not
        if (!CheckGooglePlayServices()) {
            Log.d("onCreate", "Finishing test case since Google Play Services are not available");
            finish();
        }
        else {
            Log.d("onCreate","Google Play Services available.");
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);}


        private boolean CheckGooglePlayServices(){
            GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
            int result = googleAPI.isGooglePlayServicesAvailable(this);
            if(result != ConnectionResult.SUCCESS) {
                if(googleAPI.isUserResolvableError(result)) {
                    googleAPI.getErrorDialog(this, result,
                            0).show();
                }
                return false;
            }
            return true;
        }

    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }


        /////////////////////////
        btnFindPath = (Button) findViewById(R.id.btnFindPath);
        etOrigin = (EditText) findViewById(R.id.etOrigin);
        etDestination = (EditText) findViewById(R.id.etDestination);

        btnFindPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendRequest();
            }
        });
    }



    // added


    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }


    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    private String getUrl(double latitude, double longitude, String nearbyPlace) {

        StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=" + latitude + "," + longitude);
        googlePlacesUrl.append("&radius=" + PROXIMITY_RADIUS);
        googlePlacesUrl.append("&type=" + nearbyPlace);
        googlePlacesUrl.append("&sensor=true");
        googlePlacesUrl.append("&key=" + "AIzaSyATuUiZUkEc_UgHuqsBJa1oqaODI-3mLs0");
        Log.d("getUrl", googlePlacesUrl.toString());
        return (googlePlacesUrl.toString());
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("onLocationChanged", "entered");

        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //Place current location marker
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocationMarker = mMap.addMarker(markerOptions);
// hardcoded hospitals around the area
        MarkerOptions markerOptionsa = new MarkerOptions();
        markerOptionsa.title("Johns Hopkins Medical Center");
        markerOptionsa.icon(BitmapDescriptorFactory.fromResource(R.drawable.hospital_icon4));
        latitude = 39.330038;
        longitude = -76.620480;
        LatLng latLnga = new LatLng(latitude, longitude);
        markerOptionsa.position(latLnga);
        mCurrLocationMarker = mMap.addMarker(markerOptionsa);
//
        MarkerOptions markerOptionsb = new MarkerOptions();
        markerOptionsb.title("Saint Agnes Healthcare");
        markerOptionsb.icon(BitmapDescriptorFactory.fromResource(R.drawable.hospital_icon4));
        latitude = 39.330038;
        longitude = -76.620480;
        LatLng latLngb = new LatLng(latitude, longitude);
        markerOptionsb.position(latLngb);
        mCurrLocationMarker = mMap.addMarker(markerOptionsb);
//
        String hospital_name[] = {"Spring Grove Medical Center: ", " South Baltimore County Medical Center: " , "Patient First - Catonsville: ", "Mercy Medical Center: ", "MedStar Medical Group: "};
        double[] latitudesS = new double[] {39.270507, 39.261070, 39.287249, 39.297346, 39.268646};
        double[] longitudesS = new double[] {-76.722839, -76.673744 , -76.756828, -76.612289, -76.698806};
        String Addresses[] = {"55 Wade Ave, Catonsville, MD 21228", "1701 Twin Springs Rd, Halethrope, MD 21227", "6333 Baltimore National Pike, Catonsville, MD 21228" , "345 St Paul Pl, Baltimore, MD 21202", "46600 Wilkens Ave #100, Baltimore, MD 21229"};
       //
        MarkerOptions markerOptionsc = new MarkerOptions();
        markerOptionsc.title(hospital_name[0]);
        markerOptionsc.icon(BitmapDescriptorFactory.fromResource(R.drawable.hospital_icon4));
        latitude = latitudesS[0];
        longitude = longitudesS[0];
        LatLng latLngc = new LatLng(latitude, longitude);
        markerOptionsc.position(latLngc);
        mCurrLocationMarker = mMap.addMarker(markerOptionsc);
        //
        MarkerOptions markerOptionsd = new MarkerOptions();
        markerOptionsd.title(hospital_name[1]);
        markerOptionsd.icon(BitmapDescriptorFactory.fromResource(R.drawable.hospital_icon4));
        latitude = latitudesS[1];
        longitude = longitudesS[1];
        LatLng latLngd = new LatLng(latitude, longitude);
        markerOptionsd.position(latLngd);
        mCurrLocationMarker = mMap.addMarker(markerOptionsd);
        //
        MarkerOptions markerOptionse = new MarkerOptions();
        markerOptionse.title(hospital_name[2]);
        markerOptionse.icon(BitmapDescriptorFactory.fromResource(R.drawable.hospital_icon4));
        latitude = latitudesS[2];
        longitude = longitudesS[2];
        LatLng latLnge = new LatLng(latitude, longitude);
        markerOptionse.position(latLngc);
        mCurrLocationMarker = mMap.addMarker(markerOptionse);
        //
        MarkerOptions markerOptionsf = new MarkerOptions();
        markerOptionsf.title(hospital_name[3]);
        markerOptionsf.icon(BitmapDescriptorFactory.fromResource(R.drawable.hospital_icon4));
        latitude = latitudesS[3];
        longitude = longitudesS[3];
        LatLng latLngf = new LatLng(latitude, longitude);
        markerOptionsf.position(latLngf);
        mCurrLocationMarker = mMap.addMarker(markerOptionsf);
        //
        MarkerOptions markerOptionsg = new MarkerOptions();
        markerOptionsg.title(hospital_name[4]);
        markerOptionsg.icon(BitmapDescriptorFactory.fromResource(R.drawable.hospital_icon4));
        latitude = latitudesS[4];
        longitude = longitudesS[4];
        LatLng latLngg = new LatLng(latitude, longitude);
        markerOptionsg.position(latLngc);
        mCurrLocationMarker = mMap.addMarker(markerOptionsg);


        // - for obstruction to road

        String obstruction = "Obstruction on Road. Please find alternative route.";
        double[] latitudesSS = new double[] {39.256097, 39.304672, 39.240042, 39.277159, 39.289161 };
        double[] longitudesSS = new double[]{-76.716119, -76.797101, -76.616159, -76.570025, -76.623922};
        //
        MarkerOptions obstruction1 = new MarkerOptions();
       obstruction1.title(obstruction);
        obstruction1.icon(BitmapDescriptorFactory.fromResource(R.drawable.stop1));
        latitude = latitudesSS[0];
        longitude = longitudesSS[0];
        LatLng latLngaa = new LatLng(latitude, longitude);
        obstruction1.position(latLngaa);
        mCurrLocationMarker = mMap.addMarker(obstruction1);

        //
        MarkerOptions obstruction2 = new MarkerOptions();
        obstruction2.title(obstruction);
        obstruction2.icon(BitmapDescriptorFactory.fromResource(R.drawable.stop1));
        latitude = latitudesSS[1];
        longitude = longitudesSS[1];
        LatLng latLngbb = new LatLng(latitude, longitude);
       obstruction2.position(latLngbb);
        mCurrLocationMarker = mMap.addMarker(obstruction2);

//
        MarkerOptions obstruction3 = new MarkerOptions();
        obstruction3.title(obstruction);
        obstruction3.icon(BitmapDescriptorFactory.fromResource(R.drawable.stop1));
        latitude = latitudesSS[2];
        longitude = longitudesSS[2];
        LatLng latLngcc = new LatLng(latitude, longitude);
        obstruction3.position(latLngcc);
        mCurrLocationMarker = mMap.addMarker(obstruction3);
//
        MarkerOptions obstruction4 = new MarkerOptions();
        obstruction4.title(obstruction);
        obstruction4.icon(BitmapDescriptorFactory.fromResource(R.drawable.stop1));
        latitude = latitudesSS[3];
        longitude = longitudesSS[3];
        LatLng latLngdd = new LatLng(latitude, longitude);
        obstruction4.position(latLngdd);
        mCurrLocationMarker = mMap.addMarker(obstruction4);
//

        MarkerOptions obstruction5 = new MarkerOptions();
        obstruction5.title(obstruction);
        obstruction5.icon(BitmapDescriptorFactory.fromResource(R.drawable.stop1));
        latitude = latitudesSS[4];
        longitude = longitudesSS[4];
        LatLng latLngee = new LatLng(latitude, longitude);
        obstruction5.position(latLngee);
        mCurrLocationMarker = mMap.addMarker(obstruction5);


// looking at redcross now: - RedCross Station

      String redcross = "RedCross Station";
        double[] latitudesSSS = new double[] {39.290083, 39.298888, 39.325360 };
        double[] longitudesSSS = new double[]{-76.628136, -76.643356, -76.732384};
        //
        MarkerOptions redcross1 = new MarkerOptions();
       redcross1.title(redcross);
        redcross1.icon(BitmapDescriptorFactory.fromResource(R.drawable.redcross));
        latitude = latitudesSSS[0];
        longitude = longitudesSSS[0];
        LatLng latLngaaa = new LatLng(latitude, longitude);
        redcross1.position(latLngaaa);
        mCurrLocationMarker = mMap.addMarker(redcross1);
        //
        MarkerOptions redcross2 = new MarkerOptions();
        redcross2.title(redcross);
        redcross2.icon(BitmapDescriptorFactory.fromResource(R.drawable.redcross));
        latitude = latitudesSSS[1];
        longitude = longitudesSSS[1];
        LatLng latLngbbb = new LatLng(latitude, longitude);
        redcross2.position(latLngbbb);
        mCurrLocationMarker = mMap.addMarker(redcross2);
        //
        MarkerOptions redcross3 = new MarkerOptions();
        redcross3.title(redcross);
        redcross3.icon(BitmapDescriptorFactory.fromResource(R.drawable.redcross));
        latitude = latitudesSSS[2];
        longitude = longitudesSSS[2];
        LatLng latLngccc = new LatLng(latitude, longitude);
        redcross3.position(latLngbbb);
        mCurrLocationMarker = mMap.addMarker(redcross3);
        //

        String user1 = "Needs bottled water, ShopVac, and clothes";
        String user2 = "Needs ShopVac, baby food, and baby diapers";
        double[] latitudesSSSS = new double[] {39.257591, 39.268199 };
        double[] longitudesSSSS = new double[]{-76.701351, -76.737330};
        //
        MarkerOptions user11 = new MarkerOptions();
       user11.title(user1);
        user11.icon(BitmapDescriptorFactory.fromResource(R.drawable.user));
        latitude = latitudesSSSS[0];
        longitude = longitudesSSSS[0];
        LatLng latLngaaaa = new LatLng(latitude, longitude);
        user11.position(latLngaaaa);
        mCurrLocationMarker = mMap.addMarker(user11);
        //
        MarkerOptions user22 = new MarkerOptions();
        user22.title(user2);
        user22.icon(BitmapDescriptorFactory.fromResource(R.drawable.user));
        latitude = latitudesSSSS[1];
        longitude = longitudesSSSS[1];
        LatLng latLngbbbb = new LatLng(latitude, longitude);
        user22.position(latLngbbbb);
        mCurrLocationMarker = mMap.addMarker(user22);

//

        String organizer1 = "Can offer 8 packs of diapers and baby food.";
        String organizer2 = "Can offer baby clothes and baby food.";
        double[] latitudesSSSSS = new double[] {39.264727, 39.289125};
        double[] longitudesSSSSS = new double[]{-76.704899, -76.625453};
        //
        MarkerOptions organizer11 = new MarkerOptions();
        organizer11.title(organizer1);
        organizer11.icon(BitmapDescriptorFactory.fromResource(R.drawable.organizer));
        latitude = latitudesSSSSS[0];
        longitude = longitudesSSSSS[0];
        LatLng latLngaaaaa = new LatLng(latitude, longitude);
        organizer11.position(latLngaaaaa);
        mCurrLocationMarker = mMap.addMarker(organizer11);
        //
        MarkerOptions organizer22 = new MarkerOptions();
        organizer22.title(organizer2);
        organizer22.icon(BitmapDescriptorFactory.fromResource(R.drawable.organizer));
        latitude = latitudesSSSSS[1];
        longitude = longitudesSSSSS[1];
        LatLng latLngbbbbb = new LatLng(latitude, longitude);
        organizer22.position(latLngbbbbb);
        mCurrLocationMarker = mMap.addMarker(organizer22);

        //

// - ends input of data - moving towards actually cam movement
        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
        Toast.makeText(MapsActivity.this,"Your Current Location", Toast.LENGTH_LONG).show();

        Log.d("onLocationChanged", String.format("latitude:%.3f longitude:%.3f",latitude,longitude));

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            Log.d("onLocationChanged", "Removing Location Updates");
        }
        Log.d("onLocationChanged", "Exit");

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }

    // end addition



    private void sendRequest() {
        String origin = etOrigin.getText().toString();
        String destination = etDestination.getText().toString();
        if (origin.isEmpty()) {
            Toast.makeText(this, "Please enter origin address!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (destination.isEmpty()) {
            Toast.makeText(this, "Please enter destination address!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            new DirectionFinder(this, origin, destination).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }





    @Override
    public void onDirectionFinderStart() {
        progressDialog = ProgressDialog.show(this, "Please wait.",
                "Finding direction..!", true);

        if (originMarkers != null) {
            for (Marker marker : originMarkers) {
                marker.remove();
            }
        }

        if (destinationMarkers != null) {
            for (Marker marker : destinationMarkers) {
                marker.remove();
            }
        }

        if (polylinePaths != null) {
            for (Polyline polyline:polylinePaths ) {
                polyline.remove();
            }
        }
    }

    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
        progressDialog.dismiss();
        polylinePaths = new ArrayList<>();
        originMarkers = new ArrayList<>();
        destinationMarkers = new ArrayList<>();

        for (Route route : routes) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 16));
            ((TextView) findViewById(R.id.tvDuration)).setText(route.duration.text);
            ((TextView) findViewById(R.id.tvDistance)).setText(route.distance.text);

            originMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue))
                    .title(route.startAddress)
                    .position(route.startLocation)));
            destinationMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.end_green))
                    .title(route.endAddress)
                    .position(route.endLocation)));

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(Color.BLUE).
                    width(10);

            for (int i = 0; i < route.points.size(); i++)
                polylineOptions.add(route.points.get(i));

            polylinePaths.add(mMap.addPolyline(polylineOptions));
        }
    }
}
