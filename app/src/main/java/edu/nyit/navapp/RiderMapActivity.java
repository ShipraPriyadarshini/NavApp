package edu.nyit.navapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;

public class RiderMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private Button signout_btn, profile_btn, request_btn;
    private LatLng pickupLoc;
    private boolean driverFound = false;
    private String driverID;
    private int radius = 1;
    private Marker DriverMarker;
    //    private Marker RiderMarker; not needed as the rider can see their own location
    private Boolean cancelRequest = false;
    GoogleApiClient mGoogleApiClient;
    Location lastLocation;
    LocationRequest requestLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_rider_map);
        request_btn = (Button) findViewById(R.id.request_btn);
        profile_btn = (Button) findViewById(R.id.profile_btn);
        signout_btn = (Button) findViewById(R.id.signout_btn);
        signout_btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(RiderMapActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return;
            }
        });
        profile_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent=new Intent(RiderMapActivity.this,ProfileActivity.class);
                startActivity(intent);
                return;
            }
        });
        request_btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (cancelRequest) {
                    cancelRequest = false;

                    if (driverID != null) {
                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("users").child("drivers").child(driverID);
                        driverRef.setValue(true);
                    }
                    query.removeAllListeners();
                    driverRef.removeEventListener(driverRefListener);
                    driverFound = false;
                    radius = 1;
                    String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("riderRequest");
                    GeoFire geofire = new GeoFire(ref);
                    geofire.removeLocation(userID);
//                    if (RiderMarker != null) {
//                        RiderMarker.remove();
//                    }
                    if (DriverMarker != null) {
                        DriverMarker.remove();
                    }
                    request_btn.setText("GET A RIDE");


                } else {
                    cancelRequest = true;
                    String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("riderRequest");
                    GeoFire geofire = new GeoFire(ref);
                    geofire.setLocation(userID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));

                    pickupLoc = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
//                    RiderMarker = mMap.addMarker(new MarkerOptions().position(pickupLoc).title("Rider is Here"));
                    request_btn.setText("Requesting Ride");

                    findDriver();
                }

            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    GeoQuery query;//declaring outside the function helps in stopping the listener when request is cancelled

    //recursively find the closest driver in a specific radius
    private void findDriver() {
        DatabaseReference location = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
        GeoFire geoFire = new GeoFire(location);
        query = geoFire.queryAtLocation(new GeoLocation(pickupLoc.latitude, pickupLoc.longitude), radius);//min radius 1 Meter, max radius 15 Miles
        query.removeAllListeners();
        query.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                //if multiple drivers are found in the radius, this will pick the first available driver
                if (!driverFound && cancelRequest) {
                    driverFound = true;
                    driverID = key;
                    Log.i("driver request", "Driver found");

                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("users").child("drivers").child(driverID);
                    String riderID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map = new HashMap();
                    map.put("riderID", riderID);
                    driverRef.updateChildren(map);

                    getDriverLocation(driverID);

                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound) {
                    //max radius set to 24 km(15 miles)
                    if (radius <= 24) {
                        Log.i("driver request", "Driver not found, radius++");
                        radius++;
                        findDriver();
                    } else {
                        Log.i("driver request", "Max radius reached");
                        Context context = getApplicationContext();
                        CharSequence text = "No drivers in 15 mile range. Please log out and try again later";
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();
                    }
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
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

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(13));


    }

    private DatabaseReference driverRef;//declaring outside the function helps in stopping the listener when request is cancelled
    private ValueEventListener driverRefListener;

    private void getDriverLocation(String key) {
        // check for drivers Working in database
        driverRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(key).child("l");
        driverRefListener = driverRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && cancelRequest) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double latitude = 0;
                    double longitude = 0;
                    request_btn.setText(" Driver found. ");
                    if (map.get(0) != null) {
                        latitude = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        longitude = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLoc = new LatLng(latitude, longitude);
                    if (DriverMarker != null) {
                        DriverMarker.remove();
                    }
                    Location riderLoc = new Location("");
                    riderLoc.setLatitude(pickupLoc.latitude);
                    riderLoc.setLongitude(pickupLoc.longitude);

                    Location locationDriver = new Location("");
                    locationDriver.setLatitude(driverLoc.latitude);
                    locationDriver.setLongitude(driverLoc.longitude);

                    DecimalFormat df2 = new DecimalFormat("#.##");//limit decimal to 2 digits
                    double distance = riderLoc.distanceTo(locationDriver);
                    distance = distance * 0.00062;//meters to miles

                    if (distance < 0.50 && distance > 0.10) {
                        request_btn.setText(" Driver arriving soon " + df2.format(distance) + " miles. Press button to cancel the ride");
                    } else if (distance <= 0.10 && distance >= 0.04) {
                        request_btn.setText(" Driver is here.");
                    } else if (distance < 0.04) { // this condition is just to simulate the start of the trip as the emulator location services does not permit multiple routes.
                        //so if the driver had a start trip button it would be useless as he and the rider will never move
                        request_btn.setText(" Trip has started. Press button to end the trip");
                    } else {
                        request_btn.setText(" Driver Found: " + df2.format(distance) + " miles. Press button to cancel the ride");
                    }
                    DriverMarker = mMap.addMarker(new MarkerOptions().position(driverLoc).title("Your Driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_driver)));


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestLocation = new LocationRequest();
        requestLocation.setInterval(2000);
        requestLocation.setFastestInterval(2000);
        requestLocation.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, requestLocation, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

}