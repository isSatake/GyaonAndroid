package com.stkay.gyaonandroid;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.jar.Manifest;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";
    private MediaRecorder mediaRecorder = new MediaRecorder();
    private String path = Environment.getExternalStorageDirectory() + "/gyaon.mp3";
    private EditText gyaonId;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gyaonId = (EditText) findViewById(R.id.gyaonId);
        gyaonId.setText("767fa354e1f2199c6f24194fba79584c");
        Button recButton = (Button) findViewById(R.id.rec_button);

        if (
                ContextCompat.checkSelfPermission(this, "android.permission.SYSTEM_ALERT_WINDOW") != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    "android.permission.RECORD_AUDIO",
                    "android.permission.WRITE_EXTERNAL_STORAGE",
                    "android.permission.INTERNET",
                    "android.permission.SYSTEM_ALERT_WINDOW"}, 0);
        }

        recButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.d(TAG, "onActionDown");
                    recStart();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    Log.d(TAG, "onActionUp");
                    recStop();
                }
                return true;
            }
        });

        intent = new Intent(this, FloatingRecButtonService.class);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(intent);
    }

    private void recStart() {
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setAudioEncodingBitRate(128000); //24?16?
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(path);
        try {
            Log.d(TAG, "rec start");
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            Log.d(TAG, "rec failed");
            e.printStackTrace();
        }
        //upload wav to gyaon server
    }

    private void recStop() {
        Log.d(TAG, "rec stop");
        mediaRecorder.stop();
        mediaRecorder.reset();
        if(checkNetworkStatus()) {
            upload();
        }
    }

    private Boolean checkNetworkStatus() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null) {
            Log.d(TAG, "network connected");
            return networkInfo.isConnected();
        } else {
            Log.d(TAG, "network disconnected");
            return false;
        }
    }

    private void upload() {
        Log.d(TAG, "upload start");
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .addFormDataPart("file", "hoge.mp3", RequestBody.create(MediaType.parse("audio/aac"), new File(path)))
                .build();
        String id = gyaonId.getText().toString();

        Request request = new Request.Builder()
                .addHeader("Cookie", "gyaonId=" + id)
                .url("https://gyaon.herokuapp.com/upload")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "upload failed");

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, response.body().string());
            }
        });
    }
}
