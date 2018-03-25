package com.stkay.gyaonandroid;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by stk on 2017/12/06.
 */

public class SettingFragment extends PreferenceFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        final String PREF_FIE_NAME = getString(R.string.pref_file_name);
        final String GYAON_ID_KEY = getString(R.string.id_key);
        final String IS_CAMERA_KEY = getString(R.string.id_is_camera_active);
        final SharedPreferences pref = getActivity().getSharedPreferences(PREF_FIE_NAME, MODE_PRIVATE);

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        final String id = pref.getString(GYAON_ID_KEY, "");
        final Preference idPref = findPreference(GYAON_ID_KEY);
        idPref.setSummary(id);

        final Boolean isCameraActive = pref.getBoolean(IS_CAMERA_KEY, true);
        final Preference cameraPref = findPreference(IS_CAMERA_KEY);
        cameraPref.setDefaultValue(isCameraActive);

        idPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(newValue.toString());
                return true;
            }
        });
    }
}
