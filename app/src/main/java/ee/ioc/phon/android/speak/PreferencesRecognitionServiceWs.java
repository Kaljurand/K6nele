package ee.ioc.phon.android.speak;

/*
 * Copyright 2011-2018, Institute of Cybernetics at Tallinn University of Technology
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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

public class PreferencesRecognitionServiceWs extends PreferenceActivity {

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
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    PreferenceUtils.putPrefString(prefs, getResources(), R.string.keyWsServer, serverUri.toString());
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
            addPreferencesFromResource(R.xml.preferences_server_ws);
        }

        @Override
        public void onStart() {
            super.onStart();
            setSummary(getPreferenceScreen().getSharedPreferences(), getResources());
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            Preference service = findPreference(getString(R.string.keyWsServer));
            service.setOnPreferenceClickListener(preference -> {
                getActivity().startActivityForResult(preference.getIntent(), ACTIVITY_SELECT_SERVER_URL);
                return true;
            });
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onStop() {
            super.onStop();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);
            if (pref instanceof EditTextPreference) {
                EditTextPreference etp = (EditTextPreference) pref;
                pref.setSummary(etp.getText());
            }
        }

        private void setSummary(SharedPreferences prefs, Resources res) {
            int key = R.string.keyWsServer;
            final Preference pref = findPreference(getString(key));
            if (pref != null) {
                final String urlSpeech = PreferenceUtils.getPrefString(prefs, res, key, R.string.defaultWsServer);
                pref.setSummary(String.format(getString(R.string.summaryWsServer), urlSpeech));
            }
        }
    }
}