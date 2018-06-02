package ee.ioc.phon.android.speak.activity;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

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

        // TODO: scan the local network to populate this automatically
        String[] urls = {
                "ws://192.168.0.15:4000/client/ws/speech",
                "ws://192.168.0.38:8080/client/ws/speech",
                "ws://bark.phon.ioc.ee:82/dev/duplex-speech-api/ws/speech",
                "wss://bark.phon.ioc.ee:8443/dev/duplex-speech-api/ws/speech"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, urls);

        final AutoCompleteTextView actv = findViewById(R.id.actvWsServerUrl);
        actv.setAdapter(adapter);
        actv.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mngr.setWsServer(actv.getText().toString());
                    return true;
                }
                return false;
            }
        });

        findViewById(R.id.buttonApplyDeveloperDefaults)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mngr.setDefaultsDevel();
                    }
                });
    }
}