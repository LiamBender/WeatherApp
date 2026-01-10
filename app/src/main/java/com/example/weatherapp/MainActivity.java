package com.example.weatherapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.SceneView;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;

// API nyckel: AAPTxy8BH1VEsoebNVZXo8HurGuzNiuipj3FeKc7J3bGv8PxaLmSPdL30j-EBd9QLoGMD_VzMMqRVcQxkejHMwvYAPzmagdF8iDpZwAUiIQlizCbl8yghTT45j_VO7pOHA4onCcwzIoBWRBi3urDOLYz28bkKbm-Z321SONwSwbObxe_OAGCpOorwmIq4JTpNLhwgPfp49fwIAl0Sxu5HvKJhQ1LRTdO8GjxwTI555-IKuE.AT1_5p5pSRQ3




public class MainActivity extends AppCompatActivity {

    private static final String ARCGIS_API_KEY =
            "AAPTxy8BH1VEsoebNVZXo8HurGuzNiuipj3FeKc7J3bGv8PxaLmSPdL30j-EBd9QLoGMD_VzMMqRVcQxkejHMwvYAPzmagdF8iDpZwAUiIQlizCbl8yghTT45j_VO7pOHA4onCcwzIoBWRBi3urDOLYz28bkKbm-Z321SONwSwbObxe_OAGCpOorwmIq4JTpNLhwgPfp49fwIAl0Sxu5HvKJhQ1LRTdO8GjxwTI555-IKuE.AT1_5p5pSRQ3";

    private SceneView sceneView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ArcGISRuntimeEnvironment.setApiKey(ARCGIS_API_KEY);

        sceneView = findViewById(R.id.sceneView);

        ArcGISScene scene = new ArcGISScene(BasemapStyle.ARCGIS_IMAGERY);

        Surface surface = new Surface();
        surface.getElevationSources().add(
                new ArcGISTiledElevationSource(
                        "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
                )
        );
        scene.setBaseSurface(surface);

        sceneView.setScene(scene);

        Camera camera = new Camera(
                60.6749,   // latitude (Gävle)
                17.1413,   // longitude
                20_000.0,   // altitude in meters (zoomed in)
                0.0,       // heading (north)
                0.0,      // pitch (tilt for 3D feel)
                0.0        // roll
        );

        sceneView.setViewpointCamera(camera);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sceneView != null) sceneView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sceneView != null) sceneView.resume();
    }

    @Override
    protected void onDestroy() {
        if (sceneView != null) sceneView.dispose();
        super.onDestroy();
    }
}
