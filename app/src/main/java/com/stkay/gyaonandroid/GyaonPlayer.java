package com.stkay.gyaonandroid;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;

/**
 * Created by satake on 16/06/22.
 */
public class GyaonPlayer {
    private final String TAG = "Player";
    private MediaPlayer mediaPlayer;
    private String path;

    public GyaonPlayer() {
        path = Environment.getExternalStorageDirectory() + "/gyaon.mp3";
    }

    public void play() {
        if (mediaPlayer != null) {
            stop();
        }
        mediaPlayer = new MediaPlayer();
        try {
            Log.d(TAG, "play start");
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
        } catch (IOException e1) {
            Log.d(TAG, "play failed");
            e1.printStackTrace();
        }
        mediaPlayer.start();
    }

    public void stop() {
        Log.d(TAG, "play stop");
        mediaPlayer.stop();
        mediaPlayer.reset();
        mediaPlayer.release();
        mediaPlayer = null;
    }
}
