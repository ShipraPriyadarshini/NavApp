package edu.nyit.navapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
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

import java.util.List;
import java.util.Map;

//This is the map activity for the driver
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private String riderId = "";
    private GoogleMap mMap;
    private Button signout_btn;
    private boolean signingOut=false;
    private LinearLayout riderProfile;
    private TextView riderFirst, riderLast, riderContact;
    GoogleApiClient mGoogleApiClient;
    Location lastLocation;
    LocationRequest requestLocation;
    private DatabaseReference riderRef;
    private String first, last ,conNum ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        riderProfile=(LinearLayout)findViewById(R.id.riderProfile);
        riderFirst=(TextView)findViewById(R.id.riderFName);
        riderLast=(TextView)findViewById(R.id.riderLName);
        riderContact=(TextView)findViewById(R.id.riderContact);

        signout_btn = (Button) findViewById(R.id.signout_btn);
        signout_btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                signingOut=true;
                disconnectFire();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MapsActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return;
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        getAssignedRider();
    }

    private void getAssignedRider() {
        String driverID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedRiderRef = FirebaseDatabase.getInstance().getReference().child("users").child("drivers").child(driverID).child("riderID");
        assignedRiderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    riderId = dataSnapshot.getValue().toString();
                    riderRef = FirebaseDatabase.getInstance().getReference().child("users").child("rider").child(riderId);
                    getPickupLoc();
                     getProfile();
                } else {
                    riderId = "";
                    if (pickup != null) {
                        pickup.remove();
                        riderProfile.setVisibility(View.INVISIBLE);
                    }
                    if (assignedRiderPickupLocRef != null) {
                        assignedRiderPickupLocRef.removeEventListener(RiderPickupListener);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getProfile() {
        riderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() >= 1) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("firstName")!=null){
                        first=map.get("firstName").toString();
                        riderFirst.setText(first);
                    }
                    if(map.get("lastName")!=null){
                        last=map.get("lastName").toString();
                        riderLast.setText(last);
                    }
                    if(map.get("contact")!=null){
                        conNum=map.get("contact").toString();
                        riderContact.setText(conNum);
                    }
                    riderProfile.setVisibility(View.VISIBLE);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }

        });
    }

    Marker pickup;
    private DatabaseReference assignedRiderPickupLocRef;
    ValueEventListener RiderPickupListener;

    private void getPickupLoc() {
        assignedRiderPickupLocRef = FirebaseDatabase.getInstance().getReference().child("riderRequest").child(riderId).child("l");
        RiderPickupListener = assignedRiderPickupLocRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !riderId.equals("")) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double latitude = 0;
                    double longitude = 0;
                    if (map.get(0) != null) {// default geofire latitude key is 0
                        latitude = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        longitude = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng riderLatLng = new LatLng(latitude, longitude);
                    pickup = mMap.addMarker(new MarkerOptions().position(riderLatLng).title("The pick up location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_rider)));
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

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

        if (getApplicationContext() != null) {
            lastLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(13));

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable");
            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversWorking");
            GeoFire geoFireAvailable = new GeoFire(refAvailable);
            GeoFire geoFireWorking = new GeoFire(refWorking);
            switch (riderId) {
                case "":
                    geoFireWorking.removeLocation((userId));
                    geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
                default:
                    geoFireAvailable.removeLocation((userId));
                    geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
            }


            geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()), new
                    GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                        }
                    });
        }
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
    private void disconnectFire(){
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driversAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(!signingOut){
            disconnectFire();
        }

    }

}


