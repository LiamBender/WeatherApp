package com.example.weatherapp;

import android.graphics.Point;
import android.view.MotionEvent;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.DefaultSceneViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.SceneView;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;

import java.util.List;


/**
 * MapController ansvarar för själva karlogiken som är kopplad till ArcGis Sceneview
 * Klassen initierar 3d-globen och hanterar klick för att plocka koordinater och geokodning för sök/zoom
 */
public class MapController {

    /**
     * Listener som tar emot koordinater när användaren trycker på kartan
     */
    public interface OnPickListener {
        /**
         * Anropas när en plats har valts på kartan
         * @param lat Latitud
         * @param lon Longitud
         */
        void onPick(double lat, double lon);
    }

    /**
     * Callback som hanterar sök/zoom och leverar en label vid lyckat resultat eller felmeddelande vid misslyckat
     */
    public interface SearchCallback {
        /**
         * Anropas när en sökning har lyckats och kartan har zoomats till resultatet
         * @param label textsträng vid lyckat resultat
         */
        void onSuccess(String label);

        /**
         * Anropas om sökningen misslyckas eller om inga reulstat hittats
         * @param message textsträng vid felmeddelande
         */
        void onError(String message);
    }

    private SceneView sceneView;
    private LocatorTask locatorTask;

    private volatile boolean pickEnabled = false;
    private OnPickListener onPickListener;

    /**
     * Initierar Arcgis SceneView med 3d-glob, sätter en ArcGIS APY-nyckel samt kopplar in geokodning och aktiverar klicklogiken
     * @param sceneView Används för att rendera 3D-kartan
     * @param arcGisApiKey APi-nyckeln för ArcGis
     */
    public void setup(SceneView sceneView, String arcGisApiKey) {
        this.sceneView = sceneView;

        ArcGISRuntimeEnvironment.setApiKey(arcGisApiKey);

        ArcGISScene scene = new ArcGISScene(BasemapStyle.ARCGIS_IMAGERY);

        Surface surface = new Surface();
        surface.getElevationSources().add(new ArcGISTiledElevationSource(
                "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
        ));
        scene.setBaseSurface(surface);

        this.sceneView.setScene(scene);

        Camera start = new Camera(0.0, 0.0, 30_000_000, 0.0, 0.0, 0.0);
        this.sceneView.setViewpointCamera(start);

        locatorTask = new LocatorTask(
                "https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer"
        );

        this.sceneView.setOnTouchListener(new DefaultSceneViewOnTouchListener(this.sceneView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (!pickEnabled) return super.onSingleTapConfirmed(e);

                Point screenPoint = new Point(Math.round(e.getX()), Math.round(e.getY()));
                com.esri.arcgisruntime.geometry.Point mapPoint =
                        MapController.this.sceneView.screenToBaseSurface(screenPoint);

                if (mapPoint == null) return true;

                com.esri.arcgisruntime.geometry.Point wgs84 =
                        (com.esri.arcgisruntime.geometry.Point) GeometryEngine.project(
                                mapPoint,
                                SpatialReferences.getWgs84()
                        );

                double lon = wgs84.getX();
                double lat = wgs84.getY();

                if (onPickListener != null) {
                    onPickListener.onPick(lat, lon);
                }

                return true;
            }
        });
    }

    /**
     * Aktiverar eller inaktiverar möjligheten att väla en plats genom tryck på kartan
     * @param enabled true för att aktivera platsval, men annars är den false
     */
    public void setPickEnabled(boolean enabled) {
        this.pickEnabled = enabled;
    }

    /**
     * Sätter en listener som tar emot Lat/Lon värdena när användaren väljer en punkt på kartan via tryckning
     * @param listener tar emot valda koordinater
     */
    public void setOnPickListener(OnPickListener listener) {
        this.onPickListener = listener;
    }

    /**
     *  Söker efter en plats med geokodning och zoomar kameran till den platsen
     * @param query Söksträngen som kan vara t.ex. stad eller adress
     * @param callback Callbacken för lyckat resultat som sköts via en label eller felmeddelande
     */
    public void searchAndZoom(String query, SearchCallback callback) {
        if (locatorTask == null || sceneView == null) {
            if (callback != null) callback.onError("Kartan är inte initialiserad.");
            return;
        }

        GeocodeParameters params = new GeocodeParameters();
        params.setMaxResults(1);
        params.getResultAttributeNames().add("*");

        ListenableFuture<List<GeocodeResult>> future = locatorTask.geocodeAsync(query, params);

        future.addDoneListener(() -> {
            try {
                List<GeocodeResult> results = future.get();
                if (results == null || results.isEmpty()) {
                    if (callback != null) callback.onError("Inga träffar för: " + query);
                    return;
                }

                GeocodeResult best = results.get(0);
                com.esri.arcgisruntime.geometry.Point location = best.getDisplayLocation();

                Viewpoint vp = new Viewpoint(location, 10_000);
                sceneView.setViewpointAsync(vp, 1.5f);

                if (callback != null) callback.onSuccess(best.getLabel());

            } catch (Exception ex) {
                if (callback != null) callback.onError(ex.getMessage());
            }
        });
    }

    /**
     * Zoomar kameran till Gävle
     */
    public void zoomToGavle() {
        if (sceneView == null) return;

        Camera gavle = new Camera(
                60.6749,
                17.1413,
                20_000.0,
                0.0,
                0.0,
                0.0
        );
        sceneView.setViewpointCameraAsync(gavle, 1.5f);
    }

    /**
     * Återställer kameran till en mer global startvy
     */
    public void resetView() {
        if (sceneView == null) return;

        Camera reset = new Camera(0.0, 0.0, 30_000_000, 0.0, 0.0, 0.0);
        sceneView.setViewpointCameraAsync(reset, 1.5f);
    }

    /**
     * Pausar SceneView
     */
    public void onPause() {
        if (sceneView != null) sceneView.pause();
    }

    /**
     * Återupptar SceneView
     */
    public void onResume() {
        if (sceneView != null) sceneView.resume();
    }

    /**
     * Förstör SceneView
     */
    public void onDestroy() {
        if (sceneView != null) sceneView.dispose();
    }
}
