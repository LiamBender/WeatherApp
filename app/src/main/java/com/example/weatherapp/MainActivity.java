// ESRI API nyckel: AAPTxy8BH1VEsoebNVZXo8HurGuzNiuipj3FeKc7J3bGv8PxaLmSPdL30j-EBd9QLoGMD_VzMMqRVcQxkejHMwvYAPzmagdF8iDpZwAUiIQlizCbl8yghTT45j_VO7pOHA4onCcwzIoBWRBi3urDOLYz28bkKbm-Z321SONwSwbObxe_OAGCpOorwmIq4JTpNLhwgPfp49fwIAl0Sxu5HvKJhQ1LRTdO8GjxwTI555-IKuE.AT1_5p5pSRQ3
// OpenWeather API nyckel: 27339e03b1ff3e028133a088da401eac

package com.example.weatherapp;

import android.graphics.Point;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.DefaultSceneViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.SceneView;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    // Put keys directly here (simple/dev mode). Don’t commit to public git.
    private static final String ARCGIS_API_KEY = "AAPTxy8BH1VEsoebNVZXo8HurGuzNiuipj3FeKc7J3bGv8PxaLmSPdL30j-EBd9QLoGMD_VzMMqRVcQxkejHMwvYAPzmagdF8iDpZwAUiIQlizCbl8yghTT45j_VO7pOHA4onCcwzIoBWRBi3urDOLYz28bkKbm-Z321SONwSwbObxe_OAGCpOorwmIq4JTpNLhwgPfp49fwIAl0Sxu5HvKJhQ1LRTdO8GjxwTI555-IKuE.AT1_5p5pSRQ3";
    private static final String OPENWEATHER_API_KEY = "27339e03b1ff3e028133a088da401eac";

    private SceneView sceneView;

    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;

    private SwitchMaterial switchPick;
    private TextView txtResult;

    private Button btnGoGavle;
    private Button btnResetView;

    private TextInputEditText etSearch;
    private Button btnSearch;

    private volatile boolean pickEnabled = false;

    private LocatorTask locatorTask;
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Find views ---
        drawerLayout = findViewById(R.id.drawerLayout);
        btnMenu = findViewById(R.id.btnMenu);

        sceneView = findViewById(R.id.sceneView);

        switchPick = findViewById(R.id.switchPick);
        txtResult = findViewById(R.id.txtResult);

        btnGoGavle = findViewById(R.id.btnGoGavle);
        btnResetView = findViewById(R.id.btnResetView);

        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);

        // --- Menu button ---
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // --- ArcGIS key ---
        ArcGISRuntimeEnvironment.setApiKey(ARCGIS_API_KEY);

        // --- Scene setup (3D globe) ---
        ArcGISScene scene = new ArcGISScene(BasemapStyle.ARCGIS_IMAGERY);

        Surface surface = new Surface();
        surface.getElevationSources().add(new ArcGISTiledElevationSource(
                "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
        ));
        scene.setBaseSurface(surface);

        sceneView.setScene(scene);

        // Start camera (planet view, centered)
        Camera start = new Camera(0.0, 0.0, 30_000_000, 0.0, 0.0, 0.0);
        sceneView.setViewpointCamera(start);

        // --- Geocoding (search) ---
        locatorTask = new LocatorTask(
                "https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer"
        );

        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
            if (query.isEmpty()) {
                txtResult.setText("Skriv en stad eller adress att söka på.");
                return;
            }

            txtResult.setText("Söker: " + query + " ...");
            searchAndZoom(query);
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        // --- Toggle pick mode ---
        switchPick.setOnCheckedChangeListener((buttonView, isChecked) -> {
            pickEnabled = isChecked;
            txtResult.setText(isChecked ? "Pick mode ON. Tap the globe." : "Pick mode OFF.");
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        // --- Tap listener (only when pickEnabled == true) ---
        sceneView.setOnTouchListener(new DefaultSceneViewOnTouchListener(sceneView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (!pickEnabled) return super.onSingleTapConfirmed(e);

                Point screenPoint = new Point(Math.round(e.getX()), Math.round(e.getY()));
                com.esri.arcgisruntime.geometry.Point mapPoint = sceneView.screenToBaseSurface(screenPoint);

                if (mapPoint == null) {
                    txtResult.setText("Could not resolve map point. Try again.");
                    return true;
                }

                // Project to WGS84 lat/lon
                com.esri.arcgisruntime.geometry.Point wgs84 =
                        (com.esri.arcgisruntime.geometry.Point) GeometryEngine.project(
                                mapPoint,
                                SpatialReferences.getWgs84()
                        );

                double lon = wgs84.getX();
                double lat = wgs84.getY();

                txtResult.setText(String.format(Locale.US,
                        "Picked:\nlat=%.5f, lon=%.5f\nFetching weather...", lat, lon));

                fetchWeather(lat, lon);
                return true;
            }
        });

        // --- Buttons ---
        btnGoGavle.setOnClickListener(v -> {
            Camera gavle = new Camera(
                    60.6749,
                    17.1413,
                    20_000.0,
                    0.0,
                    0.0,
                    0.0
            );
            sceneView.setViewpointCameraAsync(gavle, 1.5f);
            txtResult.setText("Zooming to Gävle...");
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        btnResetView.setOnClickListener(v -> {
            Camera reset = new Camera(0.0, 0.0, 30_000_000, 0.0, 0.0, 0.0);
            sceneView.setViewpointCameraAsync(reset, 1.5f);
            txtResult.setText("Reset view.");
            drawerLayout.closeDrawer(GravityCompat.START);
        });
    }

    private void searchAndZoom(String query) {
        GeocodeParameters params = new GeocodeParameters();
        params.setMaxResults(1);
        params.getResultAttributeNames().add("*");

        ListenableFuture<List<GeocodeResult>> future = locatorTask.geocodeAsync(query, params);

        future.addDoneListener(() -> {
            try {
                List<GeocodeResult> results = future.get();
                if (results == null || results.isEmpty()) {
                    runOnUiThread(() -> txtResult.setText("Inga träffar för: " + query));
                    return;
                }

                GeocodeResult best = results.get(0);

                com.esri.arcgisruntime.geometry.Point location = best.getDisplayLocation();

                // Flyg till platsen (10 km zoom)
                Viewpoint vp = new Viewpoint(location, 10_000);
                sceneView.setViewpointAsync(vp, 1.5f);

                runOnUiThread(() -> txtResult.setText("Hittade: " + best.getLabel()));

            } catch (Exception ex) {
                runOnUiThread(() -> txtResult.setText("Sök misslyckades: " + ex.getMessage()));
            }
        });
    }

    private void fetchWeather(double lat, double lon) {
        networkExecutor.execute(() -> {
            try {
                String urlStr = String.format(Locale.US,
                        "https://api.openweathermap.org/data/2.5/weather?lat=%.6f&lon=%.6f&appid=%s&units=metric",
                        lat, lon, OPENWEATHER_API_KEY);

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);

                int code = conn.getResponseCode();

                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream()
                ));

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                if (code < 200 || code >= 300) {
                    String err = sb.toString();
                    runOnUiThread(() -> txtResult.setText("Weather error (" + code + "):\n" + err));
                    return;
                }

                JSONObject json = new JSONObject(sb.toString());

                String place = json.optString("name", "Unknown place");
                JSONObject main = json.getJSONObject("main");
                double temp = main.getDouble("temp");
                int humidity = main.getInt("humidity");

                String desc = "";
                if (json.has("weather") && json.getJSONArray("weather").length() > 0) {
                    desc = json.getJSONArray("weather").getJSONObject(0).optString("description", "");
                }

                String finalText = String.format(Locale.US,
                        "Location: %s\nTemp: %.1f°C\nHumidity: %d%%\nDescription: %s",
                        place, temp, humidity, desc);

                runOnUiThread(() -> txtResult.setText(finalText));

            } catch (Exception ex) {
                runOnUiThread(() -> txtResult.setText("Weather fetch failed:\n" + ex.getMessage()));
            }
        });
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
        networkExecutor.shutdownNow();
        super.onDestroy();
    }
}
