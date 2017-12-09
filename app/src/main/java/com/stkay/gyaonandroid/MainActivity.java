package com.stkay.gyaonandroid;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.android.cameraview.CameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.ButterKnife;

public class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context context = this;

    private String gyaonId;

    private Button recButton;

    private ImageButton preferenceButton;

    private ProgressBar uploadProgress;

    private CameraView cameraView;

    private View previewFrame;

    private ImageView previewView;

    private GyaonRecorder recorder;

    private SharedPreferences pref;

    private File file;

    private static final String TAG = "MainActivity";


    private CameraView.Callback cameraCallback = new CameraView.Callback() {
        @Override
        public void onPictureTaken(CameraView cameraView, byte[] data) {
            super.onPictureTaken(cameraView, data);
            Log.d(TAG, "onPictureTaken");
            file = saveByteToFile(data);
            cameraView.stop();
            cameraView.setVisibility(View.GONE);
            uploadProgress.incrementProgressBy(20);
            startPreview(data);
        }

        private void startPreview(byte[] bytes) {
            Bitmap bmp;
            bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            previewView.setImageBitmap(bmp);
            previewFrame.setVisibility(View.VISIBLE);
        }

        private File saveByteToFile(byte[] bytes) {
            String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.JAPAN).format(new Date());
            String imageFileName = "JPEG-" + timeStamp;

            File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Gyaon");
            storageDir.mkdirs();
            File file = null;
            OutputStream outputStream = null;

            try {
                file = File.createTempFile(imageFileName, ".jpg", storageDir);
                outputStream = new FileOutputStream(file);
                outputStream.write(bytes);
            } catch (IOException e) {
                //TODO 再試行するとか
                Log.d(TAG, "Failed to save jpg file");
                e.printStackTrace();
            } finally {
                //TODO try-with-resources
                if (outputStream != null) {
                    try {
                        Log.d(TAG, "Succeeded to save jpg file");
                        Log.d(TAG, file.getAbsolutePath());
                        registerDatabase(file.getAbsolutePath());
                        outputStream.close();
                    } catch (IOException e) {
                        Log.d(TAG, "Failed to close stream");
                        e.printStackTrace();
                    }
                }
            }

            return file;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestAppPermissions();
        setContentView(R.layout.activity_main);

        recButton = ButterKnife.findById(this, R.id.rec_button);
        preferenceButton = ButterKnife.findById(this, R.id.pref_button);
        uploadProgress = ButterKnife.findById(this, R.id.upload_progress);
        cameraView = ButterKnife.findById(this, R.id.cameraview);
        previewFrame = ButterKnife.findById(this, R.id.preview_frame);
        previewView = ButterKnife.findById(this, R.id.preview);

        cameraView.addCallback(cameraCallback);

        preferenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, SettingActivity.class);
                startActivity(intent);
            }
        });

        assert recButton != null;
        recButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.d(TAG, "onActionDown");
                    previewFrame.setVisibility(View.GONE);
                    cameraView.setVisibility(View.VISIBLE);
                    recorder.start();
                    cameraView.start();
                    changeStatusBarColor(true);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    Log.d(TAG, "onActionUp");
                    recorder.stop();
                    uploadProgress.setProgress(0);
                    changeRecButtonState(false);
                    changeStatusBarColor(false);
                    cameraView.takePicture();
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
                } else {
                    window.setStatusBarColor(ContextCompat.getColor(activity, R.color.colorPrimaryDark));
                }
            }
        });

        ButterKnife.bind(this);
    }

    private void changeRecButtonState(boolean isEnabled) {
        recButton.setEnabled(isEnabled);
    }

    @Override
    protected void onPause() {
        cameraView.stop();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        changeRecButtonState(true);

        pref = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);
        gyaonId = pref.getString(getString(R.string.id_key), "");
        Log.d(TAG, "GYAON ID : " + gyaonId);

        GyaonListener gyaonListener = new GyaonListener();
        recorder = new GyaonRecorder(this, gyaonListener);
        recorder.setGyaonId(gyaonId);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.id_key))) {
            gyaonId = sharedPreferences.getString(getString(R.string.id_key), "masuilab");
        }
    }

    class GyaonListener {
        void onUpload(String _key) {
            Activity activity = (Activity) context;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    uploadProgress.incrementProgressBy(60);
                    changeRecButtonState(true);
                }
            });
            if (file != null) {
                GyazoUploader.uploadImage(file, _key, new GyazoListener());
                return;
            }
            uploadProgress.incrementProgressBy(20);
        }
    }

    class GyazoListener {
        void onUpload() {
            uploadProgress.incrementProgressBy(10);
        }

        void onLink() {
            uploadProgress.incrementProgressBy(10);
        }
    }

    private void registerDatabase(String file) {
        ContentValues contentValues = new ContentValues();
        ContentResolver contentResolver = MainActivity.this.getContentResolver();
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        contentValues.put("_data", file);
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
    }

    private void requestAppPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_COARSE_LOCATION") != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        "android.permission.RECORD_AUDIO",
                        "android.permission.WRITE_EXTERNAL_STORAGE",
                        "android.permission.ACCESS_COARSE_LOCATION",
                        "android.permission.ACCESS_FINE_LOCATION",
                        "android.permission.CAMERA"}, 0);
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0) {
            for (int result : grantResults) {
                if (result == -1) {
                    finishAndRemoveTask();
                }
            }
        }
    }
}
