package com.example.maprefuge;

import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Xml;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.*;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Ref;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private List<Refuge> mRefugeList = new ArrayList<Refuge>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        UiSettings settings = mMap.getUiSettings();

        settings.setCompassEnabled(true);
        settings.setZoomControlsEnabled(true);
        settings.setRotateGesturesEnabled(true);
        settings.setScrollGesturesEnabled(true);
        settings.setTiltGesturesEnabled(true);
        settings.setZoomGesturesEnabled(true);

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        mRefugeList.clear();
        parseXML();
        addMarker();

        Refuge refuge = mRefugeList.get(0);
        if(refuge != null){
            CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(new LatLng(refuge.getLatitude(),refuge.getLongitude()),15);
            mMap.moveCamera(cu);
        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
            public void onMapClick(LatLng latLng){
                calcDistance(latLng);
                sortRefugeList();
                updateMaker();
                addLine(latLng);
            }
        });

        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));

    }

    public void addLine(LatLng point){
        CircleOptions circleOptions = new CircleOptions().center(point).radius(3);
        mMap.addCircle(circleOptions);

        for(Refuge refuge : mRefugeList){
            if(refuge == null){
                continue;
            }
            if(refuge.isNear()){
                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.add(point);
                polylineOptions.add(new LatLng(refuge.getLatitude(),refuge.getLongitude()));
                polylineOptions.color(Color.GRAY);
                polylineOptions.width(3);
                polylineOptions.geodesic(true);
                mMap.addPolyline(polylineOptions);
            }
        }
    }

    private void updateMaker(){
        int i = 0;
        mMap.clear();
        for(Refuge r : mRefugeList){
            if(r != null) {
                MarkerOptions options = new MarkerOptions();
                options.position(new LatLng(r.getLatitude(), r.getLongitude()));
                options.title(r.getName() + " " + r.getDistance() + "m");
                options.snippet(r.getAddress());
                BitmapDescriptor icon;
                if (i > 2) {
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA);
                    r.setNear(false);
                } else {
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
                    r.setNear(true);
                }
                options.icon(icon);
                Marker marker = mMap.addMarker(options);
                if (i == 0) {
                    marker.showInfoWindow();
                }
                i++;
            }
        }

    }

    private void sortRefugeList(){
        Collections.sort(mRefugeList, new Comparator<Refuge>() {
            public int compare(Refuge lhs, Refuge rhs) {
                return lhs.getDistance() - rhs.getDistance();
            }
        });
    }

    private void calcDistance(LatLng point){
        double startLat = point.latitude;
        double startLng = point.longitude;
        float[] results = new float[3];
        for(Refuge r : mRefugeList){
            if(r != null){
                Location.distanceBetween(startLat,startLng,r.getLatitude(),r.getLongitude(),results);
                r.setDistance(results[0]);
            }
        }
    }

    private void parseXML(){
        AssetManager assetManager = getResources().getAssets();
        try{
            InputStream is = assetManager.open("refuge_nonoichi.xml");
            InputStreamReader inputStreamReader = new InputStreamReader(is);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(inputStreamReader);
            String title = "";
            String address = "";
            String lat = "";
            String lon = "";

            int eventType = parser.getEventType();
            while(eventType != XmlPullParser.END_DOCUMENT){
                switch (eventType){
                    case XmlPullParser.START_TAG:
                        String tag = parser.getName();
                        if("marker".equals(tag)){
                            title = parser.getAttributeValue(null,"title");
                            address = parser.getAttributeValue(null,"adress");
                            lat = parser.getAttributeValue(null,"lat");
                            lon = parser.getAttributeValue(null,"lng");
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        String endTag = parser.getName();
                        if("marker".equals(endTag)){
                            newRefuge(title,address,Double.valueOf(lat),Double.valueOf(lon));
                        }
                        break;
                    case XmlPullParser.TEXT:
                        break;
                }
                eventType = parser.next();
            }
        }catch(Exception e){
            e.printStackTrace();
        }


    }

    private void newRefuge(String title,String address,double lat,double lon){
        mRefugeList.add(new Refuge(title,address,lat,lon));
    }

    private void addMarker(){
        for(Refuge r: mRefugeList){
            if(r != null){
                MarkerOptions options = new MarkerOptions();
                options.position(new LatLng(r.getLatitude(),r.getLongitude()));
                options.title(r.getName());
                options.snippet(r.getAddress());
                mMap.addMarker(options);
            }
        }
    }


}
