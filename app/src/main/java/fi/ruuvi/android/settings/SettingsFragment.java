package fi.ruuvi.android.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import fi.ruuvi.android.R;


/**
 * Created by tmakinen on 14.6.2017.
 */

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load preferences from XLM resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
