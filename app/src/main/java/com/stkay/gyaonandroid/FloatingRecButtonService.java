package com.stkay.gyaonandroid;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by satake on 16/06/15.
 */
public class FloatingRecButtonService extends Service {
    final String TAG = "Service";

    private View button_view;

    private WindowManager windowManager;

    private WindowManager.LayoutParams layoutParams;

    private GyaonRecorder recorder;

    private SharedPreferences pref;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Service", "onCreate");
        recorder = new GyaonRecorder(this);
        pref = getSharedPreferences("pref", MODE_PRIVATE);
        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,       // アプリケーションのTOPに配置
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |  // フォーカスを当てない(下の画面の操作ができなくなるため)
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, // モーダル以外のタッチを背後のウィンドウへ送信
                PixelFormat.TRANSLUCENT
        );
        layoutParams.x = pref.getInt("buttonX", 0);
        layoutParams.y = pref.getInt("buttonY", 0);
        windowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = LayoutInflater.from(this);
        button_view = inflater.inflate(R.layout.floating_rec_button, null);
        button_view.findViewById(R.id.button).setOnTouchListener(new View.OnTouchListener() {
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
        button_view.findViewById(R.id.handle).setOnTouchListener(new View.OnTouchListener() {
            Integer touchX,
                    touchY,
                    oldTouchX,
                    oldTouchY,
                    initButtonX,
                    initButtonY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initButtonX = layoutParams.x;
                        initButtonY = layoutParams.y;
                        oldTouchX = (int) event.getRawX();
                        oldTouchY = (int) event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        touchX = (int) event.getRawX();
                        touchY = (int) event.getRawY();
                        layoutParams.x = initButtonX + (touchX - oldTouchX);
                        layoutParams.y = initButtonY + (touchY - oldTouchY);
                        windowManager.updateViewLayout(button_view, layoutParams);
                        break;
                }
                return true;
            }
        });

        MediaSession.Callback callback = new MediaSession.Callback() {
            @Override
            public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
                KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                Log.d(TAG, event.toString());
                if(event.getAction() != KeyEvent.ACTION_DOWN){
                    return true;
                }
                int keyCode = event.getKeyCode();
                if(keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE){
                    if(recorder.getIsRecording()){
                        recorder.stop();
                    }else{
                        recorder.start();
                    }
                    return true;
                }
                return false;
            }
        };

        MediaSession session = new MediaSession(this, TAG);
        session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setCallback(callback);
        session.setActive(true);

        windowManager.addView(button_view, layoutParams);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        recorder.setGyaonId(intent.getStringExtra("gyaonId"));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("buttonX", layoutParams.x);
        editor.putInt("buttonY", layoutParams.y);
        editor.apply();
        windowManager.removeView(button_view);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
