package de.mhid.opensource.socialdistancemeter.activity;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import de.mhid.opensource.socialdistancemeter.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
    }
}
