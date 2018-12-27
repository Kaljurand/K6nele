/*
 * Copyright 2011-2016, Institute of Cybernetics at Tallinn University of Technology
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
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import ee.ioc.phon.android.speak.provider.Server;
import ee.ioc.phon.android.speak.utils.Utils;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

public class PreferencesRecognitionServiceHttp extends PreferenceActivity {

    private static final int ACTIVITY_SELECT_SERVER_URL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
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
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        PreferenceUtils.putPrefString(prefs, getResources(), R.string.keyHttpServer, url);
                    }
                }
                break;
            default:
                break;
        }
    }

    private void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }


    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences_server_http);

            SharedPreferences sp = getPreferenceScreen().getSharedPreferences();

            setSummary(
                    findPreference(getString(R.string.keyAutoStopAfterTime)),
                    getString(R.string.summaryAutoStopAfterTime),
                    sp.getString(getString(R.string.keyAutoStopAfterTime), "?"));

            setSummary(
                    findPreference(getString(R.string.keyRecordingRate)),
                    getString(R.string.summaryRecordingRate),
                    sp.getString(getString(R.string.keyRecordingRate), "?"));
        }


        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }


        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
            Preference service = findPreference(getString(R.string.keyHttpServer));
            service.setSummary(sp.getString(getString(R.string.keyHttpServer), getString(R.string.defaultHttpServer)));

            service.setOnPreferenceClickListener(preference -> {
                getActivity().startActivityForResult(preference.getIntent(), ACTIVITY_SELECT_SERVER_URL);
                return true;
            });
        }


        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);
            if (pref instanceof EditTextPreference) {
                EditTextPreference etp = (EditTextPreference) pref;
                pref.setSummary(etp.getText());
            } else if (pref instanceof ListPreference) {
                ListPreference lp = (ListPreference) pref;
                if (getString(R.string.keyAutoStopAfterTime).equals(key)) {
                    setSummary(pref, getString(R.string.summaryAutoStopAfterTime), lp.getValue());
                } else if (getString(R.string.keyRecordingRate).equals(key)) {
                    setSummary(pref, getString(R.string.summaryRecordingRate), lp.getValue());
                }
            }
        }

        private void setSummary(Preference pref, String strText, String strArg) {
            if (pref != null) {
                pref.setSummary(String.format(strText, strArg));
            }
        }
    }
}