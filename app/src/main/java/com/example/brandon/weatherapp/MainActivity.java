package com.example.brandon.weatherapp;

import android.databinding.DataBindingUtil;
import android.graphics.Typeface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.example.brandon.weatherapp.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    Typeface mWeatherIcons;
    Typeface mRobotoRegular;
    Typeface mRobotoLight;
    Typeface mRobotoThin;

    LocationTracker locationTracker;

    ActivityMainBinding ui;

    Handler mRunnableHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = DataBindingUtil.setContentView(this, R.layout.activity_main);

        mWeatherIcons = Typeface.createFromAsset(getAssets(), "weathericons.ttf");
        mRobotoRegular = Typeface.createFromAsset(getAssets(), "Roboto-Regular.ttf");
        mRobotoLight = Typeface.createFromAsset(getAssets(), "Roboto-Light.ttf");
        mRobotoThin = Typeface.createFromAsset(getAssets(), "Roboto-Thin.ttf");

        ui.strIcon.setTypeface(mWeatherIcons);
        ui.strIcnLowTemp.setTypeface(mWeatherIcons);
        ui.strIcnHighTemp.setTypeface(mWeatherIcons);
        ui.strLocation.setTypeface(mRobotoRegular);
        ui.strDescription.setTypeface(mRobotoLight);
        ui.strTemperature.setTypeface(mRobotoLight);

        mRunnableHandler = new Handler();
        locationTracker = new LocationTracker(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUpdateWeather.run();
    }

    private double convertTemperature(double kelvin, boolean isMetric) {
        if (isMetric) {
            return kelvin - 273.15;
        }
        return kelvin * 9 / 5 - 459.67;
    }

    private String getWeatherIcon(JSONObject weatherResponse) {
        try {
            String iconId = weatherResponse
                    .getJSONArray("weather")
                    .getJSONObject(0)
                    .getString("icon")
                    .substring(0, 2);

            switch (iconId) {
                // id conversions are found here https://openweathermap.org/weather-conditions
                case "01":
                    return "\uf00d";

                case "02":
                    return "\uf002";

                case "03":
                    return "\uf013";

                case "04":
                    return "\uf041";

                case "09":
                    return "\uf01a";

                case "10":
                    return "\uf019";

                case "11":
                    return "\uf01e";

                case "13":
                    return "\uf01b";

                case "50": // mist
                    return "\uf021";

                default:
                    return "\uf00d";
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void updateUi(JSONObject weatherResponse) {
        try {
            JSONObject sys = weatherResponse.getJSONObject("sys");
            JSONObject clouds = weatherResponse.getJSONObject("clouds");
            JSONObject wind = weatherResponse.getJSONObject("wind");
            JSONArray weather = weatherResponse.getJSONArray("weather");
            JSONObject main = weatherResponse.getJSONObject("main");

            String locationName = weatherResponse.getString("name");
            String country = sys.getString("country");
            ui.strLocation.setText(locationName + ", " + country);

            String description = weather.getJSONObject(0).getString("description");
            ui.strDescription.setText(description);

            double temp = convertTemperature(main.getDouble("temp"), false);
            String strTemp = String.format("%.1f°F", temp);
            ui.strTemperature.setText(strTemp);

            double minTemp = convertTemperature(main.getDouble("temp_min"), false);
            String strMinTemp = String.format("%.1f°F", minTemp);
            ui.strLowTemp.setText(strMinTemp);

            double maxTemp = convertTemperature(main.getDouble("temp_max"), false);
            String strMaxTemp = String.format("%.1f°F", maxTemp);
            ui.strHighTemp.setText(strMaxTemp);

            int cloudiness = clouds.getInt("all");
            String strCloud = String.format("Cloudiness %d%%", cloudiness);
            ui.strCloudiness.setText(strCloud);

            int humidity = main.getInt("humidity");
            String strHumidity = String.format("Humidity %d%%", humidity);
            ui.strHumidity.setText(strHumidity);

            double windSpeed = wind.getDouble("speed") * 2.23694;
            String strSpeed = String.format("Wind Speed %.1f mph", windSpeed);
            ui.strWindSpeed.setText(strSpeed);

            String icon = getWeatherIcon(weatherResponse);
            ui.strIcon.setText(icon);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    Runnable mUpdateWeather = new Runnable() {
        @Override
        public void run() {
            new GetWeatherFromApi().execute();
            mRunnableHandler.postDelayed(mUpdateWeather, 5 * 60 * 1000);
        }
    };

    private class GetWeatherFromApi extends AsyncTask<Void, Void, String> {
        Location location;

        protected void onPreExecute() {
            this.location = locationTracker.getLocation();
            if(this.location == null){
                Toast.makeText(MainActivity.this, "Turn your location services on", Toast.LENGTH_LONG).show();
            }
        }

        protected String doInBackground(Void... urls) {
            try {
                double lat = location.getLatitude(); //31.7614414;
                double lon = location.getLongitude(); //35.1826296;
                String urlFormat = "http://api.openweathermap.org/data/2.5/weather?lat=%.4f&lon=%.4f&appid=79032ea448ece32d3bcf922b545b68bf";
                URL url = new URL(String.format(urlFormat, lat, lon));

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }
        }

        protected void onPostExecute(String response) {
            if (response == null) return;
            try {
                JSONObject json = new JSONObject(response);
                updateUi(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

}
