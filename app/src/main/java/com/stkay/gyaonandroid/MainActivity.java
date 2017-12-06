package com.stkay.gyaonandroid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;

public class MainActivity extends Activity {

    private final String GYAON_ID_KEY = "gyaonId";

    private Context context = this;

    private String gyaonId;

    private Button recButton;

    private ImageButton preferenceButton;

    private ProgressBar uploadProgress;

    private ProgressBar captureProgress;

    private FrameLayout textureFrame;

    private GyaonRecorder recorder;

    private SharedPreferences pref;

    private Uri cameraUri;

    private CameraDevice camera;

    private CameraCaptureSession captureSession;

    private CaptureRequest previewRequest;

    private CaptureRequest stillRequest;

    private String selectedCameraId;

    private TextureView textureView;

    private ImageReader imageReader;

    private Handler backgroundHandler = new Handler();

    private boolean isProcessing = false;

    private boolean isFinishedRecording = false;

    private File file;

    private static final String TAG = "MainActivity";

    private static final String TAG_CAMERA_URI = "CaptureUri";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestAppPermissions();
        setContentView(R.layout.activity_main);

        recButton = ButterKnife.findById(this, R.id.rec_button);
        preferenceButton = ButterKnife.findById(this, R.id.pref_button);
        uploadProgress = ButterKnife.findById(this, R.id.upload_progress);
        captureProgress = ButterKnife.findById(this, R.id.capture_progress);
        textureFrame = ButterKnife.findById(this, R.id.texture_frame);

        if (savedInstanceState != null) {
            cameraUri = savedInstanceState.getParcelable(TAG_CAMERA_URI);
        }

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
                    isFinishedRecording = false;
                    recorder.start();
                    changeStatusBarColor(true);
                    textureFrame.setBackground(null);
                    createCameraPreviewSession(); //カメラプレビュー開始
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    Log.d(TAG, "onActionUp");
                    isFinishedRecording = true;
                    recorder.stop();
                    uploadProgress.setProgress(0);
                    captureProgress.setVisibility(View.VISIBLE);
                    changeRecButtonState(false);
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
                } else {
                    window.setStatusBarColor(ContextCompat.getColor(activity, R.color.colorPrimaryDark));
                }
            }
        });

        ButterKnife.bind(this);
    }

    private void initSurfaces() {
        Log.d(TAG, "initSurfaces");
        captureProgress.setVisibility(View.GONE);

        if (textureView == null) {
            textureView = ButterKnife.findById(this, R.id.texture);
        }
        textureView.setSurfaceTextureListener(previewSurfaceTextureListener);

        imageReader = ImageReader.newInstance(720, 960, ImageFormat.JPEG, 2);
        imageReader.setOnImageAvailableListener(stillImageReaderAvailableListener, backgroundHandler);
    }

    private void changeRecButtonState(boolean isEnabled) {
        recButton.setEnabled(isEnabled);
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera(camera);
    }

    @Override
    protected void onResume() {
        super.onResume();

        pref = getSharedPreferences("pref", MODE_PRIVATE);
        gyaonId = pref.getString(GYAON_ID_KEY, "");
        Log.d(TAG, "GYAON ID : " + gyaonId);

        GyaonListener gyaonListener = new GyaonListener();
        recorder = new GyaonRecorder(this, gyaonListener);
        recorder.setGyaonId(gyaonId);

        initSurfaces();
    }

    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(TAG_CAMERA_URI, cameraUri);
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
            }
        }
    }

    class GyazoListener {
        void onUpload(){
            uploadProgress.incrementProgressBy(10);
        }
        void onLink(){
            uploadProgress.incrementProgressBy(10);
        }
    }

    private void openCamera() {
        Log.d(TAG, "openCamera");
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            selectedCameraId = manager.getCameraIdList()[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestAppPermissions();
                return;
            }
            manager.openCamera(selectedCameraId, cameraStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession() {
        Log.d(TAG, "createCameraPreviewSession");

        List<Surface> outputs = Arrays.asList(getSurfaceFromTexture(textureView), imageReader.getSurface());
        previewRequest = makePreviewRequest();
        stillRequest = makeStillCaptureRequest();

        try {
            camera.createCaptureSession(outputs, captureSessionCallBack, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CaptureRequest makeStillCaptureRequest() {
        CaptureRequest.Builder stillCaptureRequestBuilder = null;
        try {
            stillCaptureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        stillCaptureRequestBuilder.addTarget(imageReader.getSurface());

        try {
            stillCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, (ORIENTATIONS.get(getAppRotation()) + getCameraRotation(selectedCameraId) + 270) % 360);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return stillCaptureRequestBuilder.build();
    }

    private CaptureRequest makePreviewRequest() {
        CaptureRequest.Builder previewRequestBuilder = null;
        if(camera == null){
            openCamera();
        }
        try {
            previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        previewRequestBuilder.addTarget(getSurfaceFromTexture(textureView));
        return previewRequestBuilder.build();
    }

    private Surface getSurfaceFromTexture(TextureView textureView) {
        SurfaceTexture texture = textureView.getSurfaceTexture();
        texture.setDefaultBufferSize(960, 720);
        return new Surface(texture);
    }

    private CameraCaptureSession.StateCallback captureSessionCallBack = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            Log.d(TAG, "capture session onConfigured");
            if (null == camera) {
                return;
            }

            captureSession = session;
            startPreview();
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
        }
    };

    private void startPreview() {
        Log.d(TAG, "Start preview");
        try {
            captureSession.setRepeatingRequest(previewRequest, previewCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void stopPreview() {
        Log.d(TAG, "Stop preview");
        try {
            captureSession.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startCapture() {
        Log.d(TAG, "Start capture");
        try {
            isProcessing = true;
            stopPreview();
            captureSession.capture(stillRequest, stillCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraCaptureSession.CaptureCallback previewCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            if (!isProcessing &&
                    isFinishedRecording &&
                    result.get(CaptureResult.CONTROL_AF_STATE).equals(CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED) &&
                    result.get(CaptureResult.CONTROL_AE_STATE).equals(CaptureResult.CONTROL_AE_STATE_CONVERGED)) {
                startCapture();
            } else {
            }
        }
    };

    private final CameraCaptureSession.CaptureCallback stillCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            Log.d(TAG, "onStillCaptureCompleted");
            isProcessing = false;
            captureProgress.setVisibility(View.GONE);
            textureFrame.setBackground(getDrawable(R.drawable.texture_frame_border));
        }
    };

    private void registerDatabase(String file) {
        ContentValues contentValues = new ContentValues();
        ContentResolver contentResolver = MainActivity.this.getContentResolver();
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        contentValues.put("_data", file);
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
    }

    private void closeCamera(CameraDevice c) {
        if (c != null) {
            Log.d(TAG, "close camera");
            c.close();
            camera = null;
        } else {
            Log.d(TAG, "camera is null");
        }
        if (imageReader != null) {
            Log.d(TAG, "close imageReader");
            imageReader.close();
            imageReader = null;
        }
    }

    TextureView.SurfaceTextureListener previewSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    ImageReader.OnImageAvailableListener stillImageReaderAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "onImageAvailable");

            Image image = reader.acquireLatestImage();
            ByteBuffer buf = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buf.capacity()];
            buf.get(bytes);
            file = saveByteToFile(bytes);
            uploadProgress.incrementProgressBy(20);
            image.close();
            initSurfaces();
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
                        return file;
                    } catch (IOException e) {
                        Log.d(TAG, "Failed to close stream");
                        e.printStackTrace();
                    }
                }
            }

            return file;
        }
    };

    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            Log.d(TAG, "camera onOpened");
            camera = cameraDevice;
            changeRecButtonState(true);
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            Log.d(TAG, "camera onDisconnected");
            closeCamera(cameraDevice);
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            Log.d(TAG, "camera onError");
            closeCamera(cameraDevice);
        }
    };

    private int getCameraRotation(String id) throws CameraAccessException {
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
        return characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
    }

    private int getAppRotation() {
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        return rotation;
    }

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
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
            openCamera();
        }
    }
}
