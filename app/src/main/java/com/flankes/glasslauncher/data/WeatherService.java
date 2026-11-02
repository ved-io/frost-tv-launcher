package com.flankes.glasslauncher.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class WeatherService {

    public interface Callback {
        void onWeatherLoaded(String weatherSummary);
    }

    public static class CityInfo {
        public final String name;
        public final String country;
        public final float lat;
        public final float lon;

        public CityInfo(String name, String country, float lat, float lon) {
            this.name = name;
            this.country = country;
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public String toString() {
            return country.isEmpty() ? name : (name + ", " + country);
        }
    }

    public interface CityCallback {
        void onCitiesLoaded(List<CityInfo> cities);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public WeatherService(Context context) {
        this.context = context.getApplicationContext();
    }

    public void fetchWeather(Callback callback) {
        new Thread(() -> {
            SharedPreferences prefs = context.getSharedPreferences("glass_launcher_prefs", Context.MODE_PRIVATE);
            boolean enabled = prefs.getBoolean("weather_enabled", true);
            if (!enabled) {
                mainHandler.post(() -> callback.onWeatherLoaded(""));
                return;
            }

            float lat = prefs.getFloat("weather_lat", 52.52f);
            float lon = prefs.getFloat("weather_lon", 13.405f);
            String unit = prefs.getString("weather_unit", "c");
            boolean isFahrenheit = "f".equalsIgnoreCase(unit);
            String lang = prefs.getString("app_language", "de");

            HttpURLConnection conn = null;
            try {
                String urlStr = "https://api.open-meteo.com/v1/forecast?latitude=" + lat +
                                "&longitude=" + lon +
                                "&current=temperature_2m,weather_code&timezone=auto" +
                                (isFahrenheit ? "&temperature_unit=fahrenheit" : "");
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONObject current = json.getJSONObject("current");
                    double temp = current.getDouble("temperature_2m");
                    int code = current.getInt("weather_code");

                    String desc = getWeatherDescription(code, lang);
                    String tempUnitStr = isFahrenheit ? "°F" : "°C";
                    String summary = Math.round(temp) + tempUnitStr + " " + desc;

                    mainHandler.post(() -> callback.onWeatherLoaded(summary));
                    return;
                }
            } catch (Exception e) {
                // Ignore network errors
            } finally {
                if (conn != null) conn.disconnect();
            }

            mainHandler.post(() -> callback.onWeatherLoaded(""));
        }).start();
    }

    public static void searchCities(String query, CityCallback callback) {
        searchCities(query, "de", callback);
    }

    public static void searchCities(String query, String lang, CityCallback callback) {
        new Thread(() -> {
            List<CityInfo> results = new ArrayList<>();
            HttpURLConnection conn = null;
            try {
                String encoded = URLEncoder.encode(query, "UTF-8");
                String langParam = (lang != null && !lang.isEmpty()) ? ("&language=" + lang) : "";
                String urlStr = "https://geocoding-api.open-meteo.com/v1/search?count=6&name=" + encoded + langParam;
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    if (json.has("results")) {
                        JSONArray arr = json.getJSONArray("results");
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            String name = obj.optString("name", "");
                            String country = obj.optString("country_code", obj.optString("country", ""));
                            float lat = (float) obj.optDouble("latitude", 0.0);
                            float lon = (float) obj.optDouble("longitude", 0.0);
                            results.add(new CityInfo(name, country, lat, lon));
                        }
                    }
                }
            } catch (Exception ignored) {}
            finally {
                if (conn != null) conn.disconnect();
            }

            new Handler(Looper.getMainLooper()).post(() -> callback.onCitiesLoaded(results));
        }).start();
    }

    private String getWeatherDescription(int code, String lang) {
        if ("en".equalsIgnoreCase(lang)) {
            switch (code) {
                case 0: return "Clear";
                case 1: return "Mostly clear";
                case 2: return "Partly cloudy";
                case 3: return "Overcast";
                case 45: case 48: return "Fog";
                case 51: case 53: case 55: return "Drizzle";
                case 61: case 63: case 65: return "Rain";
                case 71: case 73: case 75: return "Snow";
                case 80: case 81: case 82: return "Showers";
                case 95: case 96: case 99: return "Thunderstorm";
                default: return "Fair";
            }
        } else if ("es".equalsIgnoreCase(lang)) {
            switch (code) {
                case 0: return "Despejado";
                case 1: return "Mayormente despejado";
                case 2: return "Parcialmente nublado";
                case 3: return "Nublado";
                case 45: case 48: return "Niebla";
                case 51: case 53: case 55: return "Llovizna";
                case 61: case 63: case 65: return "Lluvia";
                case 71: case 73: case 75: return "Nieve";
                case 80: case 81: case 82: return "Chubascos";
                case 95: case 96: case 99: return "Tormenta";
                default: return "Despejado";
            }
        } else if ("fr".equalsIgnoreCase(lang)) {
            switch (code) {
                case 0: return "Dégagé";
                case 1: return "Ensoleillé";
                case 2: return "Partiellement nuageux";
                case 3: return "Couvert";
                case 45: case 48: return "Brouillard";
                case 51: case 53: case 55: return "Bruine";
                case 61: case 63: case 65: return "Pluie";
                case 71: case 73: case 75: return "Neige";
                case 80: case 81: case 82: return "Averses";
                case 95: case 96: case 99: return "Orage";
                default: return "Beau temps";
            }
        } else if ("pt".equalsIgnoreCase(lang)) {
            switch (code) {
                case 0: return "Limpo";
                case 1: return "Predominantemente limpo";
                case 2: return "Parcialmente nublado";
                case 3: return "Encoberto";
                case 45: case 48: return "Nevoeiro";
                case 51: case 53: case 55: return "Garoa";
                case 61: case 63: case 65: return "Chuva";
                case 71: case 73: case 75: return "Neve";
                case 80: case 81: case 82: return "Pancadas de chuva";
                case 95: case 96: case 99: return "Trovoada";
                default: return "Bom tempo";
            }
        } else if ("it".equalsIgnoreCase(lang)) {
            switch (code) {
                case 0: return "Sereno";
                case 1: return "Prevalentemente sereno";
                case 2: return "Parzialmente nuvoloso";
                case 3: return "Coperto";
                case 45: case 48: return "Nebbia";
                case 51: case 53: case 55: return "Pioggerella";
                case 61: case 63: case 65: return "Pioggia";
                case 71: case 73: case 75: return "Neve";
                case 80: case 81: case 82: return "Rovesci";
                case 95: case 96: case 99: return "Temporale";
                default: return "Sereno";
            }
        } else {
            // German default
            switch (code) {
                case 0: return "Klar";
                case 1: return "Überwiegend klar";
                case 2: return "Teils bewölkt";
                case 3: return "Bewölkt";
                case 45: case 48: return "Nebel";
                case 51: case 53: case 55: return "Nieselregen";
                case 61: case 63: case 65: return "Regen";
                case 71: case 73: case 75: return "Schneefall";
                case 80: case 81: case 82: return "Regenschauer";
                case 95: case 96: case 99: return "Gewitter";
                default: return "Heiter";
            }
        }
    }
}
