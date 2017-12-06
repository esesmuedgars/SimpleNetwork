package com.testdevlab.edgarsvanags.androidnetworkpropapp;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class DetailsActivity extends Activity implements OnMapReadyCallback {

    TextView display;
    TextView coordinatesDisplay;
    String ip;
    SharedPreferences preferences;
    TextView editText;
    Pair<Double, Double> coordinates;

    private static final int ERROR_DIALOG_REQUEST = 9001;

    public static void startForResult(Activity activity, int requestCode, String ip) {
        Intent intent = new Intent(activity, DetailsActivity.class);
        intent.putExtra("IPAdress", ip);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        display = findViewById(R.id.detailsIPdisplay);
        editText = findViewById(R.id.editText);
        coordinatesDisplay = findViewById(R.id.coordinatesLabel);
        preferences = getSharedPreferences("save", MODE_PRIVATE);

        ip = getIntent().getStringExtra("IPAdress");
        if (ip != null) {
            display.setText(ip);
            new GetCoordinatesTask(DetailsActivity.this, ip).execute();
        }

        editText.setText(preferences.getString(ip, ""));

        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
                finish();
            }
        });

        findViewById(R.id.startTest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: start speed test
                showMessage("Speed test!");
            }
        });

        if (isServicesAvailable()) {
            MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        save();
    }

    void save() {
        String ipSid = editText.getText().toString();
        if (!ipSid.isEmpty()) {
            preferences.edit().putString(ip, ipSid).apply();
        }
    }

    void adjustCoordinates() {
        coordinatesDisplay.setText(String.format(getString(R.string.coordinatesLabel), coordinates.first, coordinates.second));
        isServicesAvailable();
    }

    // TODO: Remove showMessage() method
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    boolean isServicesAvailable() {
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(DetailsActivity.this);
        if (available == ConnectionResult.SUCCESS) {
            showMessage("SUCCESS");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(DetailsActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            showMessage("Google Play Store is NOT available");
        }
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (coordinates != null) {
            LatLng currentIPPosition = new LatLng(coordinates.first, coordinates.second);
            googleMap.addMarker(new MarkerOptions().position(currentIPPosition).title("IP source"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(currentIPPosition));
        } else {
            onMapReady(googleMap);
        }
    }

    static class GetCoordinatesTask extends AsyncTask<Void, Void, Pair<Double, Double>> {
        WeakReference<DetailsActivity> activity;
        String ip;

        GetCoordinatesTask(DetailsActivity activity, String ip) {
            this.activity = new WeakReference<>(activity);
            this.ip = ip;
        }

        @Override
        protected Pair<Double, Double> doInBackground(Void... voids) {
            String urlString = "https://freegeoip.net/json/" + ip;

            try {
                URL url = URI.create(urlString).toURL();
                HttpURLConnection connection = ((HttpURLConnection) url.openConnection());

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                connection.getInputStream()
                        )
                );

                String result = reader.readLine();
                connection.disconnect();

                try {
                    double latitude = new JSONObject(result).getDouble("latitude");
                    double longitude = new JSONObject(result).getDouble("longitude");
                    return Pair.create(latitude, longitude);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Pair<Double, Double> doubleDoublePair) {
            super.onPostExecute(doubleDoublePair);
            final DetailsActivity detailsActivity = this.activity.get();
            if (detailsActivity != null) {
                if (doubleDoublePair != null) {
                    detailsActivity.coordinates = doubleDoublePair;
                    detailsActivity.adjustCoordinates();
                } else {
                    new DetailsActivity.GetCoordinatesTask(detailsActivity, detailsActivity.ip).execute();
                }
            }
        }
    }
}
