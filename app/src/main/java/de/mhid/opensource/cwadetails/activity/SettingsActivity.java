package de.mhid.opensource.cwadetails.activity;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import de.mhid.opensource.cwadetails.R;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        getSupportFragmentManager().beginTransaction().replace(R.id.activity_settings, new SettingsFragment()).commit();
    }
}
