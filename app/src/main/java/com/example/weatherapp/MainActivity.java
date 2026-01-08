package com.example.weatherapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.view.MapView;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;

    // TODO: Lägg in din ArcGIS Location Platform / ArcGIS Online API key här
    // API nyckel: AAPTxy8BH1VEsoebNVZXo8HurGuzNiuipj3FeKc7J3bGv8PxaLmSPdL30j-EBd9QLoGMD_VzMMqRVcQxkejHMwvYAPzmagdF8iDpZwAUiIQlizCbl8yghTT45j_VO7pOHA4onCcwzIoBWRBi3urDOLYz28bkKbm-Z321SONwSwbObxe_OAGCpOorwmIq4JTpNLhwgPfp49fwIAl0Sxu5HvKJhQ1LRTdO8GjxwTI555-IKuE.AT1_5p5pSRQ3
    private static final String API_KEY = "AAPTxy8BH1VEsoebNVZXo8HurGuzNiuipj3FeKc7J3bGv8PxaLmSPdL30j-EBd9QLoGMD_VzMMqRVcQxkejHMwvYAPzmagdF8iDpZwAUiIQlizCbl8yghTT45j_VO7pOHA4onCcwzIoBWRBi3urDOLYz28bkKbm-Z321SONwSwbObxe_OAGCpOorwmIq4JTpNLhwgPfp49fwIAl0Sxu5HvKJhQ1LRTdO8GjxwTI555-IKuE.AT1_5p5pSRQ3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Sätt API key (krävs för basemaps)
        ArcGISRuntimeEnvironment.setApiKey(API_KEY);

        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);

        // Skapa en karta med basemap
        ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC);
        mapView.setMap(map);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.pause(); // rekommenderas av Esri
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.resume(); // rekommenderas av Esri
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) mapView.dispose(); // frigör resurser
        super.onDestroy();
    }
}
