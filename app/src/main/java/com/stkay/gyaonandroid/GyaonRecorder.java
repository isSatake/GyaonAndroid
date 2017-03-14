package com.stkay.gyaonandroid;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
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
    final String TAG = "Recorder";

    private MediaRecorder mediaRecorder = new MediaRecorder();

    private String path = Environment.getExternalStorageDirectory() + "/gyaon.mp3";

    private String gyaonId;

    private Context context;

    private Handler handler;

    public GyaonRecorder(Context c) {
        context = c;
        handler = new Handler();
    }

    public void setGyaonId(String id){
        this.gyaonId = id;
    }

    public void start() {
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setAudioEncodingBitRate(128000);
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
        Toast.makeText(context, "Start Recording", Toast.LENGTH_SHORT).show();
    }

    public void stop() {
        Log.d(TAG, "rec stop");
        handler.postDelayed(stop, 200);
    }

    private void upload() {
        Log.d(TAG, "upload start");
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .addFormDataPart("file", "hoge.mp3", RequestBody.create(MediaType.parse("audio/aac"), new File(path)))
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
        public void run() {
            mediaRecorder.stop();
            mediaRecorder.reset();
            upload();
            Toast.makeText(context, "Stop Recording", Toast.LENGTH_SHORT).show();
        }
    };
}
