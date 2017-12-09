package com.stkay.gyaonandroid;

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
        final String GYAON_ID_KEY = getString(R.string.id_key);

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        final String id = getActivity().getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE).getString(GYAON_ID_KEY, "");
        final Preference idPref = findPreference(GYAON_ID_KEY);
        idPref.setSummary(id);

        idPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(newValue.toString());
                return true;
            }
        });
    }
}
