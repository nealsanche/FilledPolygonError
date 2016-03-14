package org.acme.filledpolygonerror;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.graphics.ColorUtils;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.maps.android.geojson.GeoJsonLayer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

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
     * This is where we can add markers or lines, add listeners or move the camera.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        BufferedReader streamReader = null;
        try {
            streamReader = new BufferedReader(new InputStreamReader(getAssets().open("features.json"), "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);

            Gson gson = new GsonBuilder().create();
            JsonElement initial = gson.fromJson(responseStrBuilder.toString(), JsonElement.class);
            JsonObject geoJsonObject = initial.getAsJsonObject()
                    .get("Fire Ban").getAsJsonObject()
                    .get("geojson").getAsJsonObject();

            JsonObject simplify = MapUtils.simplify(geoJsonObject, 0.001);


            JSONObject jsonObject = new JSONObject(simplify.toString());

            final GeoJsonLayer layer6 = new GeoJsonLayer(googleMap, jsonObject);
            layer6.getDefaultPolygonStyle().setFillColor(ColorUtils.setAlphaComponent(Color.RED, 100));
            layer6.getDefaultPolygonStyle().setStrokeColor(ColorUtils.setAlphaComponent(Color.RED, 255));
            layer6.getDefaultPolygonStyle().setStrokeWidth(2.0f * getResources().getDisplayMetrics().density);
            layer6.addLayerToMap();

            CameraPosition pos = CameraPosition.fromLatLngZoom(new LatLng(55.21, -114.6), 5);
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos));

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
