package com.stkay.gyaonandroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";

    private String gyaonId;

    private Intent intent;

    private GyaonRecorder recorder;

    private SharedPreferences pref;

    private static final int REQUEST_SYSTEM_OVERLAY = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText idEditText = ButterKnife.findById(this, R.id.gyaonId);
        final Button recButton = ButterKnife.findById(this, R.id.rec_button);
        final Button startServiceButton = ButterKnife.findById(this, R.id.start_service_button);

        pref = getSharedPreferences("pref", MODE_PRIVATE);
        gyaonId = pref.getString("gyaonId", "");
        assert idEditText != null;
        idEditText.setText(gyaonId);

        recorder = new GyaonRecorder(this);
        recorder.setGyaonId(gyaonId);

        ButterKnife.bind(this);

        //request permissions
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_COARSE_LOCATION") != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        "android.permission.RECORD_AUDIO",
                        "android.permission.WRITE_EXTERNAL_STORAGE",
                        "android.permission.ACCESS_COARSE_LOCATION",
                        "android.permission.ACCESS_FINE_LOCATION"}, 0);
            }
            if (!checkOverlayPermission()) {
                requestOverlayPermission();
            }
        }

        idEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, s.toString());
                gyaonId = s.toString();
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("gyaonId", gyaonId);
                editor.apply();
                recorder.setGyaonId(gyaonId);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        assert recButton != null;
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

    private void startService() {
        intent = new Intent(this, FloatingRecButtonService.class);
        intent.putExtra("gyaonId", gyaonId);
        startService(intent);
    }

    private void stopService() {
        stopService(intent);
    }

    private Boolean checkOverlayPermission() {
        return Settings.canDrawOverlays(this);
    }

    public void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        this.startActivityForResult(intent, REQUEST_SYSTEM_OVERLAY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SYSTEM_OVERLAY) {
            if (!checkOverlayPermission()) {
                requestOverlayPermission();
            }
        }
    }
}
