package com.example.weatherapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.esri.arcgisruntime.mapping.view.SceneView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

/**
 * MainActivity har ansvaret för appens användargränssnitt och kopplar ihop kartan (MapController) med väderhämtningen (WeatherService)
 *
 * Activityn hanterar UI-interaktioner som t.ex. meny/drawer, sökning, val av punkt på kartan och presenterar väderdata för användaren
 *
 */

public class MainActivity extends AppCompatActivity {

    private static final String ARCGIS_API_KEY =
            "AAPTxy8BH1VEsoebNVZXo8HurGuzNiuipj3FeKc7J3bGv8PxaLmSPdL30j-EBd9QLoGMD_VzMMqRVcQxkejHMwvYAPzmagdF8iDpZwAUiIQlizCbl8yghTT45j_VO7pOHA4onCcwzIoBWRBi3urDOLYz28bkKbm-Z321SONwSwbObxe_OAGCpOorwmIq4JTpNLhwgPfp49fwIAl0Sxu5HvKJhQ1LRTdO8GjxwTI555-IKuE.AT1_5p5pSRQ3";

    private static final String OPENWEATHER_API_KEY =
            "27339e03b1ff3e028133a088da401eac";

    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;

    private SceneView sceneView;

    private SwitchMaterial switchPick;
    private TextView txtResult;

    private Button btnGoGavle;
    private Button btnResetView;

    private TextInputEditText etSearch;
    private Button btnSearch;

    private MapController mapController;
    private WeatherService weatherService;

    /**
     * Skapar och initierar gränssnittet, den kopplar knappar samt lyssnare och startar karttjänsten och vädertjänsten
     * @param savedInstanceState Sparat tillstånd
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayout);
        btnMenu = findViewById(R.id.btnMenu);

        sceneView = findViewById(R.id.sceneView);

        switchPick = findViewById(R.id.switchPick);
        txtResult = findViewById(R.id.txtResult);

        btnGoGavle = findViewById(R.id.btnGoGavle);
        btnResetView = findViewById(R.id.btnResetView);

        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);

        mapController = new MapController();
        weatherService = new WeatherService(OPENWEATHER_API_KEY);

        btnMenu.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        mapController.setup(sceneView, ARCGIS_API_KEY);

        mapController.setOnPickListener((lat, lon) -> {
            txtResult.setText(String.format(Locale.US,
                    "Valt:\nlat=%.5f, lon=%.5f\nHämtar väderdata", lat, lon));

            weatherService.fetchWeather(lat, lon, new WeatherService.WeatherCallback() {
                @Override
                public void onSuccess(WeatherData data) {
                    runOnUiThread(() -> txtResult.setText(data.toPrettyString()));
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> txtResult.setText("Väder hämtning misslyckad:\n" + message));
                }
            });
        });

        switchPick.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mapController.setPickEnabled(isChecked);
            txtResult.setText(isChecked ? "Klickfunktion är på!" : "Klickfunktion är av!");
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
            if (query.isEmpty()) {
                txtResult.setText("Skriv en stad eller adress att söka på.");
                return;
            }

            txtResult.setText("Söker: " + query + " ...");

            mapController.searchAndZoom(query, new MapController.SearchCallback() {
                @Override
                public void onSuccess(String label) {
                    runOnUiThread(() -> txtResult.setText("Hittade: " + label));
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> txtResult.setText("Sök misslyckades: " + message));
                }
            });

            drawerLayout.closeDrawer(GravityCompat.START);
        });

        btnGoGavle.setOnClickListener(v -> {
            mapController.zoomToGavle();
            txtResult.setText("Zoomar till Gävle...");
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        btnResetView.setOnClickListener(v -> {
            mapController.resetView();
            txtResult.setText("Återställ view.");
            drawerLayout.closeDrawer(GravityCompat.START);
        });
    }

    /**
     * Pausar kartvyn när activity går till bakgrunden
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapController.onPause();
    }

    /**
     * Återupptar kartvyn när activityn kommer tillbaka till förgrunden
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapController.onResume();
    }

    /**
     * Städar upp resurser och stoppar bakgrundsjobben när activityn förstörs
     */
    @Override
    protected void onDestroy() {
        weatherService.shutdown();
        mapController.onDestroy();
        super.onDestroy();
    }
}

