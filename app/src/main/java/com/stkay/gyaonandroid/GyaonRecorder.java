package com.stkay.gyaonandroid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by satake on 16/06/16.
 */
public class GyaonRecorder {
    private final String TAG = "Recorder";

    private MediaRecorder mediaRecorder = new MediaRecorder();

    private String path = Environment.getExternalStorageDirectory() + "/gyaon.mp4";

    private String gyaonId;

    private Context context;

    private Handler handler;

    private LocationManager locationManager;

    private AudioManager audioManager;

    private Boolean isRecording = false;

    Boolean getIsRecording() {
        return isRecording;
    }

    GyaonRecorder(Context c) {
        context = c;
        handler = new Handler();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

//        if (bluetoothAdapter != null) {
//            if (audioManager.isBluetoothScoAvailableOffCall()) {
//                BroadcastReceiver receiver = new BroadcastReceiver() {
//                    @Override
//                    public void onReceive(Context context, Intent intent) {
//                        String action = intent.getAction();
//
//                        switch (action) {
//                            case BluetoothDevice.ACTION_ACL_CONNECTED:
//                                BluetoothDevice mConnectedHeadset = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                                BluetoothClass bluetoothClass = mConnectedHeadset.getBluetoothClass();
//                                if (bluetoothClass != null) {
//                                    // Check if device is a headset. Besides the 2 below, are there other
//                                    // device classes also qualified as headset?
//                                    int deviceClass = bluetoothClass.getDeviceClass();
//                                    if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE
//                                            || deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET) {
//                                        // start bluetooth Sco audio connection.
//                                        // Calling startBluetoothSco() always returns faIL here,
//                                        // that why a count down timer is implemented to call
//                                        // startBluetoothSco() in the onTick.
//                                        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
//
//                                        // override this if you want to do other thing when the device is connected.
////                        onHeadsetConnected();
//                                        audioManager.startBluetoothSco();
//                                    }
//                                }
//
//                                Log.d(TAG, mConnectedHeadset.getName() + " connected");
//                                break;
//                            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
//                                Log.d(TAG, "Headset disconnected");
//
//                                audioManager.setMode(AudioManager.MODE_NORMAL);
//                                audioManager.stopBluetoothSco();
//
//                                // override this if you want to do other thing when the device is disconnected.
////                onHeadsetDisconnected();
//                                break;
//                            case AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED:
//                                int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_DISCONNECTED);
//                                Log.d(TAG, "state: " + state);
//                                if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
//                                    Toast.makeText(context, "start SCO", Toast.LENGTH_SHORT).show();
//                                } else {
//                                    Log.d(TAG, "restart SCO: " + state);
//                                    audioManager.stopBluetoothSco();
//                                    audioManager.startBluetoothSco();
//                                }
//                                break;
//                        }
//                    }
//                };
//                context.registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
//                context.registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
//                context.registerReceiver(receiver, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
//                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
//            }
//        }
    }

    void setGyaonId(String id) {
        this.gyaonId = id;
    }

    void start() {
        isRecording = true;
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setAudioEncodingBitRate(128000);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(path);
        try {
            Log.d(TAG, "rec start");
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            Log.d(TAG, "rec failed");
            isRecording = false;
            e.printStackTrace();
        }
        Toast.makeText(context, "Start Recording", Toast.LENGTH_SHORT).show();
    }

    void stop() {
        isRecording = false;
        Log.d(TAG, "rec stop");
        handler.postDelayed(stop, 200);
    }

    private void upload(Location location) {
        Log.d(TAG, "upload start");
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "gyaon.mp4", RequestBody.create(MediaType.parse("audio/aac"), new File(path)))
                .addFormDataPart("lat", location.getLatitude() + "")
                .addFormDataPart("lon", location.getLongitude() + "")
                .build();

        Request request = new Request.Builder()
                .url("https://gyaon.herokuapp.com/upload/" + gyaonId)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "upload failed");
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, response.body().string());
            }
        });
    }

    private Runnable stop = new Runnable() {
        @Override
        public void run() throws SecurityException {
            mediaRecorder.stop();
            mediaRecorder.reset();
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            locationManager.requestSingleUpdate(criteria, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    upload(location);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            }, null);
            Toast.makeText(context, "Stop Recording", Toast.LENGTH_SHORT).show();
        }
    };

}
