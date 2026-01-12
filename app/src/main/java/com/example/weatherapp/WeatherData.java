package com.example.weatherapp;

import java.util.Locale;

/**
 * WeatherData är väderinformation för en specifik plats
 *
 * Klassen används för att väderdata hämtas från OpenWeatherMapAPI
 * API:n innehåller information om temperatur, luftfuktighet, väderförhållanden, vind samt koordinater
 *
 */
public class WeatherData {

    public final String place;
    public final String country;

    public final double tempC;
    public final int humidity;

    public final String weatherMain;
    public final String description;

    public final double windSpeed;
    public final int windDeg;

    public final double lat;
    public final double lon;

    /**
     * Skapar ett WeatherData-objekt med väderinformation för en plats
     *
     * @param place        Namn på platsen
     * @param country      Landskod
     * @param tempC        Temperatur i grader Celsius
     * @param humidity     Luftfuktighet i procent
     * @param weatherMain  Huvudsaklig vädertyp
     * @param description  Detaljerad väderbeskrivning
     * @param windSpeed    Vindhastighet i m/s
     * @param windDeg      Vindriktning i grader
     * @param lat          Latitud
     * @param lon          Longitud
     */

    public WeatherData(
            String place,
            String country,
            double tempC,
            int humidity,
            String weatherMain,
            String description,
            double windSpeed,
            int windDeg,
            double lat,
            double lon
    ) {
        this.place = place;
        this.country = country;
        this.tempC = tempC;
        this.humidity = humidity;
        this.weatherMain = weatherMain;
        this.description = description;
        this.windSpeed = windSpeed;
        this.windDeg = windDeg;
        this.lat = lat;
        this.lon = lon;
    }

    /**
     * Returnerar en formaterad textsträng med väderinformation
     *
     * @return en läsbar sträng som innehåller väderinfo
     */
    public String toPrettyString() {
        return String.format(Locale.US,
                "Plats: %s, %s\n" +
                        "Temp: %.1f°C\n" +
                        "Luftfuktighet: %d%%\n" +
                        "Väder: %s (%s)\n" +
                        "Vind: %.1f m/s (%d°)\n\n" +
                        "(lat=%.5f, lon=%.5f)",
                place, country,
                tempC,
                humidity,
                weatherMain, description,
                windSpeed, windDeg,
                lat, lon
        );
    }
}
