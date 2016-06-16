package com.stkay.gyaonandroid;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by satake on 16/06/15.
 */
public class FloatingRecButtonService extends Service {
    final String TAG = "Service";
    View button_view;
    WindowManager windowManager;
    WindowManager.LayoutParams layoutParams;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Service", "onCreate");
        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,       // アプリケーションのTOPに配置
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |  // フォーカスを当てない(下の画面の操作がd系なくなるため)
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, // モーダル以外のタッチを背後のウィンドウへ送信
                PixelFormat.TRANSLUCENT
        );
        windowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = LayoutInflater.from(this);
        button_view = inflater.inflate(R.layout.floating_rec_button, null);
        button_view.findViewById(R.id.button).setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.d(TAG, "onActionDown");
//                    recStart();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    Log.d(TAG, "onActionUp");
//                    recStop();
                }
                return true;
            }
        });
        windowManager.addView(button_view, layoutParams);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        windowManager.removeView(button_view);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }
}
