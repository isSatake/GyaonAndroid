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
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
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
import android.widget.Toast;

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

    private Context context = this;

    private String gyaonId;

    private Button recButton;

    private GyaonRecorder recorder;

    private SharedPreferences pref;

    private Uri cameraUri;

    String key;

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

    private static final String TAG = "MainActivity";

    private static final String TAG_CAMERA_URI = "CaptureUri";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestAppPermissions();
        setContentView(R.layout.activity_main);

        EditText idEditText = ButterKnife.findById(this, R.id.gyaonId);
        recButton = ButterKnife.findById(this, R.id.rec_button);

        pref = getSharedPreferences("pref", MODE_PRIVATE);
        gyaonId = pref.getString("gyaonId", "");
        assert idEditText != null;
        idEditText.setText(gyaonId);

        UploadListener uploadListener = new UploadListener();

        recorder = new GyaonRecorder(this, uploadListener);
        recorder.setGyaonId(gyaonId);

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
                    isFinishedRecording = false;
                    recorder.start();
                    changeStatusBarColor(true);
                    createCameraPreviewSession(); //カメラプレビュー開始
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    Log.d(TAG, "onActionUp");
                    isFinishedRecording = true;
                    recorder.stop();
                    changeStatusBarColor(false);
                    //カメラプレビュー終了
                    //撮影
                    //結果表示
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

        initSurfaces();
        ButterKnife.bind(this);
    }

    private void initSurfaces() {
        if(textureView == null){
            textureView = ButterKnife.findById(this, R.id.texture);
        }
        textureView.setSurfaceTextureListener(previewSurfaceTextureListener); //View準備完了コールバック

        imageReader = ImageReader.newInstance(960, 720, ImageFormat.JPEG, 2); //ImageReader初期化
        imageReader.setOnImageAvailableListener(stillImageReaderAvailableListener, backgroundHandler); //ImageReader準備完了コールバック
    }

    private void changeRecButtonState(boolean isEnabled) {
        recButton.setEnabled(isEnabled);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(TAG_CAMERA_URI, cameraUri);
    }

    class UploadListener {
        void onUpload(String _key) {
            key = _key;
//            cameraIntent();
        }
    }

    //カメラと接続
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

    //カメラから画像を取得する
    //この手続をCameraPreviewSessionと呼んでいる
    private void createCameraPreviewSession() {
        Log.d(TAG, "createCameraPreviewSession");

        //カメラ画像を流すSurfaceを設定(複数可)
        List<Surface> outputs = Arrays.asList(getSurfaceFromTexture(textureView), imageReader.getSurface()); //各リクエストで出力先を設定してるのになぜここでも必要なのか
        previewRequest = makePreviewRequest();
        stillRequest = makeStillCaptureRequest();

        //カメラに画像をくれと言う
        //プレビュー用、撮影用といった複数の出力先(Surface)と、状態遷移(AF,AE,撮影,etc)のコールバックを渡す
        //このsessionは、別のsessionをattachする/cameraがcloseされる まで生き続ける
        //急にcloseされても、作業途中のsessionはちゃんと完了される
        try {
            camera.createCaptureSession(outputs, captureSessionCallBack, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //静止画撮影用リクエストを作成
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

    //撮影前のプレビュー用リクエストを作成
    private CaptureRequest makePreviewRequest() {
        CaptureRequest.Builder previewRequestBuilder = null;
        try {
            previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        previewRequestBuilder.addTarget(getSurfaceFromTexture(textureView)); //カメラ画像を流すSurfaceをセット
        return previewRequestBuilder.build(); //リクエスト完成
    }

    private Surface getSurfaceFromTexture(TextureView textureView) {
        SurfaceTexture texture = textureView.getSurfaceTexture();
        texture.setDefaultBufferSize(960, 720);
        return new Surface(texture);
    }

    //CaptureSessionの状態遷移コールバック
    private CameraCaptureSession.StateCallback captureSessionCallBack = new CameraCaptureSession.StateCallback() {
        //カメラの設定が完了した (最初の1回だけ)
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

    //撮影前プレビュー用リクエストを投げる
    //setRepeatingRequest→30FPSでカメラ画像を送ってくれる→targetとして設定したtextureViewにカメラ画像が見える
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

    //撮影用リクエストを投げる
    private void startCapture() {
        Log.d(TAG, "Start capture");
        try {
            isProcessing = true;
            stopPreview();
            //1回しかリクエストできない→1フレームしか画像を取れない→AF/AEしてる暇がない→AF/AEはsetRepeatingRequestでやらないといけない
            captureSession.capture(stillRequest, stillCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //プレビューリクエストのコールバック
    //画像はViewで受け取るので渡ってこない
    private final CameraCaptureSession.CaptureCallback previewCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
        }

        //stopRepeatingしても数回発火してしまう
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

    //撮影リクエストのコールバック
    private final CameraCaptureSession.CaptureCallback stillCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            Log.d(TAG, "onStillCaptureCompleted");
            isProcessing = false;
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
            File file = saveByteToFile(bytes);
            if(file != null) {
                GyazoUploader.uploadImage(file, key);
            }
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
