package ee.ioc.phon.android.speak.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

public class ProcessTextActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence text = getIntent().getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        String clip = text.toString();
        PreferenceUtils.putPrefMapEntry(
                PreferenceManager.getDefaultSharedPreferences(this),
                getResources(),
                ee.ioc.phon.android.speechutils.R.string.keyClipboardMap,
                clip,
                clip
        );
        Toast.makeText(getApplicationContext(), clip, Toast.LENGTH_LONG).show();
        finish();
    }

}