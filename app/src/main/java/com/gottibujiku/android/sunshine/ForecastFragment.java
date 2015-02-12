package com.gottibujiku.android.sunshine;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Holds the weather items in place
 *
 * @author Newton Bujiku
 * @since February, 2015
 */
public class ForecastFragment extends Fragment {

    //private static final String URL = "http://api.openweathermap.org/data/2.5/forecast/daily?q=constantine,dz&mode=json&units=metric&cnt=7";
    private ArrayAdapter<String> mForecastAdapter;//an adapter to populate the ListView

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);//this fragment will have menu options

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        //Dummy data for the ListView,weekly data
        String[] data = {
                "Mon 6/23 - Sunny - 31/17",
                "Tue 6/24 - Foggy - 21/8",
                "Wed 6/25 - Cloudy - 22/17",
                "Thurs 6/26 - Rainy - 18/11",
                "Fri 6/27 - Foggy - 21/10",
                "Sat 6/28 - TRAPPED IN WEATHERSTATION - 23/18",
                "Sun 6/29 - Sunny - 20/7"
        };
        List<String> weekForecast = new ArrayList<String>(
                Arrays.asList(data)
        );

        mForecastAdapter = new ArrayAdapter<String>(
                getActivity(),//current context,an activity for a fragment
                R.layout.list_item_forecast,//ID of the list item layout
                R.id.list_item_forecast_textview,//ID of the textview to populate
                weekForecast//Data
        );

        // new FetchWeatherTask().execute(URL);
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        return rootView;
    }

    //Inflate the menu options with fragment items
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_refresh:
                new FetchWeatherTask().execute("Constantine,dz");//fetch weather data
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Starts a task off theb UI thread to fetch the weather data
     */
    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

       // private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {
           /* Uri.Builder builder = new Uri.Builder();
            builder.scheme("http")
                    .authority("api.openweathermap.org")
                    .appendPath("data")
                    .appendPath("2.5")
                    .appendPath("forecast")
                    .appendPath("daily")
                    .appendQueryParameter("q",params[0])
                    .appendQueryParameter("mode","json")
                    .appendQueryParameter("units","metric")
                    .appendQueryParameter("cnt","7");*/

            String format = "json";
            String units = "metric";
            int numDays = 7;
            final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";

            Uri uri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, params[0])
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, String.valueOf(numDays))
                    .build();
            return getWeatherDataFromJson(fetchJSONData(uri), numDays);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);
          if(result != null){
              //only update if there is data
              mForecastAdapter.clear();
              mForecastAdapter.addAll(result);
              //mForecastAdapter.notifyDataSetChanged(); notifyDataSetChanged is called implicitly
          }
        }


        /* The date/time conversion code is going to be moved outside the asynctask later,
 * so for convenience we're breaking it out into its own method now.
 */
        private String getReadableDateString(long time) {
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            Date date = new Date(time * 1000);
            SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
            return format.format(date).toString();
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         * <p/>
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays) {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DATETIME = "dt";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = null;
            String[] resultStrs = new String[numDays];
            try {
                forecastJson = new JSONObject(forecastJsonStr);

                JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);


                for (int i = 0; i < weatherArray.length(); i++) {
                    // For now, using the format "Day, description, hi/low"
                    String day;
                    String description;
                    String highAndLow;

                    // Get the JSON object representing the day
                    JSONObject dayForecast = weatherArray.getJSONObject(i);

                    // The date/time is returned as a long.  We need to convert that
                    // into something human-readable, since most people won't read "1400356800" as
                    // "this saturday".
                    long dateTime = dayForecast.getLong(OWM_DATETIME);
                    day = getReadableDateString(dateTime);

                    // description is in a child array called "weather", which is 1 element long.
                    JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                    description = weatherObject.getString(OWM_DESCRIPTION);

                    // Temperatures are in a child object called "temp".  Try not to name variables
                    // "temp" when working with temperature.  It confuses everybody.
                    JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                    double high = temperatureObject.getDouble(OWM_MAX);
                    double low = temperatureObject.getDouble(OWM_MIN);

                    highAndLow = formatHighLows(high, low);
                    resultStrs[i] = day + " - " + description + " - " + highAndLow;


                }

            }catch(JSONException e){
                e.printStackTrace();
            }

            return resultStrs;
        }

        /**
         * Opens a connection to the openweather API and fetches  weather data
         * in JSON format
         *
         * @param uri - hyperlink for a given location
         * @return - weather data in JSON format
         */
        public String fetchJSONData(Uri uri) {
            //declared outside of the try-catch block so that they can be closed in finally block
            java.net.URL url;
            BufferedReader in = null;
            HttpURLConnection connection = null;
            StringBuilder builder = null;//buffer for reading returned data
            try {
                //create the request and open the connection

                url = new URL(uri.toString());
                //url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                //open input stream and read the response
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                if (in != null) {

                    builder = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        builder.append(line);//buffer the data
                    }
                }
                //if everything went successfully return data
                return builder.toString();

            } catch (MalformedURLException e) {

            } catch (IOException e) {

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }

                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }

            }

            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return null;
        }
    }


}
