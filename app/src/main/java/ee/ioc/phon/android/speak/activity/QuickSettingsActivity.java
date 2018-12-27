package ee.ioc.phon.android.speak.activity;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;

import ee.ioc.phon.android.speak.QuickSettingsManager;
import ee.ioc.phon.android.speak.R;

public class QuickSettingsActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quick_settings);

        final QuickSettingsManager mngr = new QuickSettingsManager(
                PreferenceManager.getDefaultSharedPreferences(QuickSettingsActivity.this),
                getResources());

        findViewById(R.id.buttonApplyDeveloperDefaults)
                .setOnClickListener(view -> mngr.setDefaultsDevel());
    }
}