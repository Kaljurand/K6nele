package ee.ioc.phon.android.speak.activity;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;

import ee.ioc.phon.android.speak.QuickSettingsManager;
import ee.ioc.phon.android.speak.R;

public class QuickSettingsActivity extends Activity {

    private Button mB1, mB2, mB3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quick_settings);
        mB1 = (Button) findViewById(R.id.buttonApplyDeveloperDefaults);
        mB1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                QuickSettingsManager mngr = new QuickSettingsManager(
                        PreferenceManager.getDefaultSharedPreferences(QuickSettingsActivity.this),
                        getResources());
                mngr.setDefaultsDevel();
            }
        });

        mB2 = (Button) findViewById(R.id.buttonApplyWsServerGlobal);
        mB2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                QuickSettingsManager mngr = new QuickSettingsManager(
                        PreferenceManager.getDefaultSharedPreferences(QuickSettingsActivity.this),
                        getResources());
                mngr.setWsServerGlobal();
            }
        });

        mB3 = (Button) findViewById(R.id.buttonApplyWsServerLocal);
        mB3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                QuickSettingsManager mngr = new QuickSettingsManager(
                        PreferenceManager.getDefaultSharedPreferences(QuickSettingsActivity.this),
                        getResources());
                mngr.setWsServerLocal();
            }
        });
    }
}