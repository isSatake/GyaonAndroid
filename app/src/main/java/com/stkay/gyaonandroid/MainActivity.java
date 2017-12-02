package com.stkay.gyaonandroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.ButterKnife;

public class MainActivity extends Activity {

    private Context context = this;

    private String gyaonId;

    private Intent intent;

    private GyaonRecorder recorder;

    private SharedPreferences pref;

    private Uri cameraUri;

    String key;

    private static final int REQUEST_SYSTEM_OVERLAY = 100;

    private static final int REQUEST_CAMERA = 200;

    private static final String TAG = "MainActivity";

    private static final String TAG_CAMERA_URI = "CaptureUri";

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

        UploadListener uploadListener = new UploadListener();

        recorder = new GyaonRecorder(this, uploadListener);
        recorder.setGyaonId(gyaonId);

        ButterKnife.bind(this);

        //request permissions
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_COARSE_LOCATION") != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        "android.permission.RECORD_AUDIO",
                        "android.permission.WRITE_EXTERNAL_STORAGE",
                        "android.permission.ACCESS_COARSE_LOCATION",
                        "android.permission.ACCESS_FINE_LOCATION",
                        "android.permission.WRITE_EXTERNAL_STORAGE"}, 0);
            }
            if (!checkOverlayPermission()) {
                requestOverlayPermission();
            }
        }

        if (savedInstanceState != null) {
            cameraUri = savedInstanceState.getParcelable(TAG_CAMERA_URI);
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
                    changeStatusBarColor(true);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    Log.d(TAG, "onActionUp");
                    recorder.stop();
                    changeStatusBarColor(false);
                }
                return false;
            }

            private void changeStatusBarColor(boolean isRec) {
                Activity activity = (Activity) context;
                Window window = activity.getWindow();
                if (isRec) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                    window.setStatusBarColor(ContextCompat.getColor(activity, R.color.colorRecording));
                }else{
                    window.setStatusBarColor(ContextCompat.getColor(activity, R.color.colorPrimaryDark));
                }
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

    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(TAG_CAMERA_URI, cameraUri);
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

    private void cameraIntent() {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.JAPAN).format(new Date());
        String imageFileName = "JPEG-" + timeStamp + "-";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image;

        try {
            image = File.createTempFile(imageFileName, ".jpg", storageDir);

        } catch (IOException e) {
            Toast.makeText(this, "Failed to launch camera", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return;
        }

        cameraUri = FileProvider.getUriForFile(this, getPackageName(), image);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
        startActivityForResult(intent, REQUEST_CAMERA);
        Log.d(TAG, "Launch camera");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SYSTEM_OVERLAY) {
            if (!checkOverlayPermission()) {
                requestOverlayPermission();
            }
        }
        if (requestCode == REQUEST_CAMERA) {
            GyazoUploader uploader = new GyazoUploader(this, gyaonId);
            uploader.uploadImage(cameraUri, key);
        }
    }

    class UploadListener {
        void onUpload(String _key) {
            key = _key;
            cameraIntent();
        }
    }
}
