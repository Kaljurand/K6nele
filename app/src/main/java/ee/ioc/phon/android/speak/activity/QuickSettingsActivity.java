package ee.ioc.phon.android.speak.activity;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;

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

        Button b1 = (Button) findViewById(R.id.buttonApplyDeveloperDefaults);
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mngr.setDefaultsDevel();
            }
        });

        Button b2Ws = (Button) findViewById(R.id.buttonApplyWsServerGlobalWs);
        b2Ws.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mngr.setWsServerGlobalWs();
            }
        });

        Button b2Wss = (Button) findViewById(R.id.buttonApplyWsServerGlobalWss);
        b2Wss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mngr.setWsServerGlobalWss();
            }
        });

        Button b3 = (Button) findViewById(R.id.buttonApplyWsServerLocal);
        b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mngr.setWsServerLocal();
            }
        });
    }
}