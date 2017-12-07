package com.testdevlab.edgarsvanags.androidnetworkpropapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class MainActivity extends Activity {
    public static final int REQUEST_DETAILS = 82;

    TextView IPdisplay;
    TextView rateDisplay;
    SharedPreferences preferences;
    String ip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        IPdisplay = findViewById(R.id.IPdisplay);
        rateDisplay = findViewById(R.id.rateLabel);
        rateDisplay.setText(String.format(getString(R.string.rateLabelSet), "n/a"));
        preferences = getSharedPreferences("save", MODE_PRIVATE);
        updateIP();

        findViewById(R.id.detailsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DetailsActivity.startForResult(MainActivity.this, REQUEST_DETAILS, ip);
            }
        });

        findViewById(R.id.requestIP).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateIP();
            }
        });

        findViewById(R.id.shareButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareResult();
            }
        });
    }

    private void shareResult() {
        String name = preferences.getString(ip, "Unnamed");

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        if (!name.equals("Unnamed")) {
            intent.putExtra(Intent.EXTRA_TEXT, String.format("Address: %s, Name: %s", ip, name));
        } else {
            intent.putExtra(Intent.EXTRA_TEXT, String.format("Address: %s", ip));
        }
        intent.setType("text/plain");
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        adjustDisplay();
    }

    private void updateIP() {
        new GetIPTask(MainActivity.this).execute();
    }

    void adjustDisplay() {
        IPdisplay.setText(preferences.getString(ip, ip));
        rateDisplay.setText(preferences.getString("rateLabel", getString(R.string.rateLabel)));
    }

    static class GetIPTask extends AsyncTask<Void, Void, String> {
        WeakReference<MainActivity> activity;

        GetIPTask(MainActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(Void... voids) {
            String urlString = "https://api.ipify.org/?format=json";

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
                    return new JSONObject(result).getString("ip");
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
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            final MainActivity mainActivity= this.activity.get();
            if (mainActivity != null) {
                if (s != null) {
                    mainActivity.ip = s;
                    mainActivity.adjustDisplay();
                } else {
                    new GetIPTask(mainActivity).execute();
                }
            }
        }
    }
}
