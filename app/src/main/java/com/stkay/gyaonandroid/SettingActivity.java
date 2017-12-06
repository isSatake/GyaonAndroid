package com.stkay.gyaonandroid;

import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.Nullable;

import com.stkay.gyaonandroid.R;
import com.stkay.gyaonandroid.SettingFragment;

/**
 * Created by stk on 2017/12/06.
 */

public class SettingActivity extends PreferenceActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Fragment fragment = new SettingFragment();
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, fragment) //android.R.id.contentって何
                .commit();

    }
}
