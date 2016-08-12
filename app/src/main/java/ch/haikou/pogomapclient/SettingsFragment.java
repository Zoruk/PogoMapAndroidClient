package ch.haikou.pogomapclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

/**
 * Created by Zoruk on 08.08.2016.
 */

public class SettingsFragment extends PreferenceFragment {

    private SharedPreferences pref;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register change listener
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister change listener
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
    }
}
