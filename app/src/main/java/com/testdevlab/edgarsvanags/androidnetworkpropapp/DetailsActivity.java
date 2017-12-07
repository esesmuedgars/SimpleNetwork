package com.testdevlab.edgarsvanags.androidnetworkpropapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
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
    double rate;

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
                Intent intent = new Intent(DetailsActivity.this, WorkService.class);
                startService(intent);
            }
        });
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

    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    void isServicesAvailable() {
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(DetailsActivity.this);
        if (available == ConnectionResult.SUCCESS) {
            MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(DetailsActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            showMessage(getString(R.string.googlePlayUnavailable));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng currentIPPosition = new LatLng(coordinates.first, coordinates.second);
        googleMap.addMarker(new MarkerOptions().position(currentIPPosition).title(getString(R.string.markerName)));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(currentIPPosition));
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(receiver, new IntentFilter(WorkService.MESSAGE_ACTION));
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @SuppressLint("DefaultLocale")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (intent.hasExtra("dataTransfer") || rate == 0) {
                    System.out.println("BroadcastReceived");
                    rate = intent.getDoubleExtra("dataTransfer", -1);
                    showMessage(String.format("%f MBps", rate));
                    System.out.println(String.format("%f MBps", rate));
                }
            }
        }
    };

    @Override
    protected void onStop() {
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.unregisterReceiver(receiver);
        super.onStop();
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
