package com.testdevlab.edgarsvanags.androidnetworkpropapp;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.webkit.DownloadListener;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by edgars.vanags on 05/12/2017.
 */

public class WorkService extends IntentService {
    public static final String MESSAGE_ACTION = "com.testdevlab.edgarsvanags.androidnetworkpropapp.speedtest";

    long fileSid;
    DownloadManager downloadManager;
    long startTime;
    long endTime;

    public WorkService() {
        super("WorkService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) return;
        startTime = System.currentTimeMillis();

        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        Uri uri = Uri.parse("https://speed.hetzner.de/100MB.bin");
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        fileSid = downloadManager.enqueue(request);
    }

    BroadcastReceiver onComplete = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            endTime = System.currentTimeMillis();
            downloadManager.remove(fileSid);

            long delta = endTime - startTime;
            double speed = 100/(((double) delta)/1000);
            sendResult(speed);

            context.unregisterReceiver(this);
        }
    };

    void sendResult(double rate) {
        Intent outgoing = new Intent(MESSAGE_ACTION);
        outgoing.putExtra("dataTransfer", rate);
        LocalBroadcastManager.getInstance(WorkService.this).sendBroadcast(outgoing);
    }
}
