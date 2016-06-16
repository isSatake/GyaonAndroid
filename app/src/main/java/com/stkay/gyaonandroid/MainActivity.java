package com.stkay.gyaonandroid;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";
    private EditText gyaonId;
    private Intent intent;
    private GyaonRecorder recorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gyaonId = (EditText) findViewById(R.id.gyaonId);
        gyaonId.setText("767fa354e1f2199c6f24194fba79584c");
        recorder = new GyaonRecorder(gyaonId.getText().toString());
        Button recButton = (Button) findViewById(R.id.rec_button);

        if (ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, "android.permission.INTERNET") != PackageManager.PERMISSION_GRANTED ||
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
                    recorder.start();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    Log.d(TAG, "onActionUp");
                    recorder.stop();
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
}
