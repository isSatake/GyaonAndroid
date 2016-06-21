package com.stkay.gyaonandroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";
    private String gyaonId;
    private Intent intent;
    private GyaonRecorder recorder;
    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EditText idEditText = (EditText) findViewById(R.id.gyaonId);
        final Button recButton = (Button) findViewById(R.id.rec_button);
        final Button startServiceButton = (Button) findViewById(R.id.start_service_button);
        recButton.setEnabled(false);
        startServiceButton.setEnabled(false);
        pref = getSharedPreferences("pref", MODE_PRIVATE);
        gyaonId = pref.getString("gyaonId", "");
        idEditText.setText(gyaonId);
        recorder = new GyaonRecorder(this);
        recorder.setGyaonId(gyaonId);

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

        if(gyaonId.length() == 32){
            recButton.setEnabled(true);
            startServiceButton.setEnabled(true);
        }

        idEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, start + " " + s.toString());
                if(start == 31){
                    gyaonId = s.toString();
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("gyaonId", gyaonId);
                    editor.commit();
                    recorder.setGyaonId(gyaonId);
                    recButton.setEnabled(true);
                    startServiceButton.setEnabled(true);
                }else{
                    recButton.setEnabled(false);
                    startServiceButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

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
        startServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService();
    }

    private void startService(){
        intent = new Intent(this, FloatingRecButtonService.class);
        intent.putExtra("gyaonId", gyaonId);
        startService(intent);
    }

    private void stopService(){
        stopService(intent);
    }
}
