package com.example.weatherapp;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WeatherService ansvarar för hämtning av väderdata från OpenWeatherMap API baserat på koordinaterna
 * Klassen ser till att utföra nätverksanrop på en bakgrundstråd och returnerar resultatet via en callback
 */
public class WeatherService {

    /**
     * Ett callback-interface som används för att leverera väderdata eller fel vid nätverksanrop
     */
    public interface WeatherCallback {
        /**
         * Anropas när väderdata har hämtats och tolkats korrekt
         * @param data Ett objekt av WeatherData med väderinfo
         */
        void onSuccess(WeatherData data);

        /**
         * Anropas när något gått snett/fel vid nätverksanrop
         * @param message Felmeddelande som förklarar vad som gick fel
         */
        void onError(String message);
    }

    private final String openWeatherApiKey;
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

    /**
     * Skapar en ny WeatherService och använder en given API-nyckel av OpenWeatherMap
     * @param openWeatherApiKey Detta är själva API-nyckeln för OpenWeatherMap API
     */
    public WeatherService(String openWeatherApiKey) {
        this.openWeatherApiKey = openWeatherApiKey;
    }

    /**
     * Hämtar väderdata från OpenWeatherMap baserat på angivna koordinater
     * Resultatet levereras asynkront via callback
     *
     * @param lat      Latitud
     * @param lon      Longitud
     * @param callback Callback som tar emot WeatherData vid lyckat anrop eller felmeddelande vid problem
     */

    public void fetchWeather(double lat, double lon, WeatherCallback callback) {
        networkExecutor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String urlStr = String.format(Locale.US,
                        "https://api.openweathermap.org/data/2.5/weather?lat=%.6f&lon=%.6f&appid=%s&units=metric",
                        lat, lon, openWeatherApiKey);

                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
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
                    if (callback != null) callback.onError("HTTP " + code + ": " + sb);
                    return;
                }

                JSONObject json = new JSONObject(sb.toString());

                String place = json.optString("name", "Unknown");
                String country = json.getJSONObject("sys").optString("country", "");

                JSONObject main = json.getJSONObject("main");
                double temp = main.getDouble("temp");
                int humidity = main.getInt("humidity");

                JSONObject weatherObj = json.getJSONArray("weather").getJSONObject(0);
                String weatherMain = weatherObj.optString("main", "");
                String description = weatherObj.optString("description", "");

                JSONObject wind = json.getJSONObject("wind");
                double windSpeed = wind.optDouble("speed", 0.0);
                int windDeg = wind.optInt("deg", 0);

                WeatherData data = new WeatherData(
                        place,
                        country,
                        temp,
                        humidity,
                        weatherMain,
                        description,
                        windSpeed,
                        windDeg,
                        lat,
                        lon
                );

                if (callback != null) callback.onSuccess(data);




            } catch (Exception ex) {
                if (callback != null) callback.onError(ex.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    /**
     * Avslutar själva nätverkstråden och stoppar/kommande nätverksjobb
     * Bör anropas när applikationen inte längre behöver göra förfrågningar till OpenWeatherMap API
     */
    public void shutdown() {
        networkExecutor.shutdownNow();
    }
}
