/*
 * Copyright 2011-2014, Institute of Cybernetics at Tallinn University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ee.ioc.phon.android.speak;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

import ee.ioc.phon.android.speak.provider.Server;

/**
 * <p>Preferences activity. Updates some preference-summaries automatically,
 * if the user changes a preference.</p>
 *
 * @author Kaarel Kaljurand
 */
public class PreferencesRecognitionServiceHttp extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    private static final int ACTIVITY_SELECT_SERVER_URL = 1;

    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_server_http);

        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        setSummary(
                (Preference) findPreference(getString(R.string.keyAutoStopAfterTime)),
                getString(R.string.summaryAutoStopAfterTime),
                sp.getString(getString(R.string.keyAutoStopAfterTime), "?"));

        setSummary(
                (Preference) findPreference(getString(R.string.keyRecordingRate)),
                getString(R.string.summaryRecordingRate),
                sp.getString(getString(R.string.keyRecordingRate), "?"));

    }


    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
        Preference service = (Preference) findPreference(getString(R.string.keyServerHttp));
        service.setSummary(sp.getString(getString(R.string.keyServerHttp), getString(R.string.defaultServerHttp)));

        service.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(preference.getIntent(), ACTIVITY_SELECT_SERVER_URL);
                return true;
            }

        });
    }


    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref instanceof EditTextPreference) {
            EditTextPreference etp = (EditTextPreference) pref;
            pref.setSummary(etp.getText());
        } else if (pref instanceof ListPreference) {
            ListPreference lp = (ListPreference) pref;
            if (lp.getTitle().equals(getString(R.string.titleAutoStopAfterTime))) {
                setSummary(pref, getString(R.string.summaryAutoStopAfterTime), lp.getValue());
            } else if (lp.getTitle().equals(getString(R.string.titleRecordingRate))) {
                setSummary(pref, getString(R.string.summaryRecordingRate), lp.getValue());
            }
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case ACTIVITY_SELECT_SERVER_URL:
                Uri serverUri = data.getData();
                if (serverUri == null) {
                    toast(getString(R.string.errorFailedGetServerUrl));
                } else {
                    long id = Long.parseLong(serverUri.getPathSegments().get(1));
                    String url = Utils.idToValue(this, Server.Columns.CONTENT_URI, Server.Columns._ID, Server.Columns.URL, id);
                    if (url != null) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(getString(R.string.keyServerHttp), url);
                        editor.commit();
                    }
                }
                break;
        }
    }


    private void setSummary(Preference pref, String strText, String strArg) {
        if (pref != null) {
            pref.setSummary(String.format(strText, strArg));
        }
    }

    private void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

}