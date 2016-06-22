package com.stkay.gyaonandroid;

import android.app.Application;

import com.deploygate.sdk.DeployGate;

/**
 * Created by satake on 16/06/22.
 */
public class GyaonApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DeployGate.install(this);
    }
}
