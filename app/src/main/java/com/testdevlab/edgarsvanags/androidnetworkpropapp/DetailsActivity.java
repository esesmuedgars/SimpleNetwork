package com.testdevlab.edgarsvanags.androidnetworkpropapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.w3c.dom.Text;

public class DetailsActivity extends Activity {

    TextView display;
    String ip;
    SharedPreferences preferences;
    TextView editText;

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
        preferences = getSharedPreferences("save", MODE_PRIVATE);

        ip = getIntent().getStringExtra("IPAdress");
        if (ip != null) {
            display.setText(ip);
        }

        editText.setText(preferences.getString(ip, ""));

        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
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
}
