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

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

//import com.koushikdutta.async.http.AsyncHttpClient;
//import com.koushikdutta.async.http.WebSocket;


public class PreferencesRecognitionServiceWs extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        //private WebSocket mWebSocket;

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
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onStop() {
            super.onStop();
            //if (mWebSocket != null && mWebSocket.isOpen()) {
            //    mWebSocket.close();
            //}
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);
            if (pref instanceof EditTextPreference) {
                EditTextPreference etp = (EditTextPreference) pref;
                pref.setSummary(etp.getText());
                //setSummaryWithStatus(pref, etp.getText());
            }
        }

        private void setSummary(SharedPreferences prefs, Resources res) {
            int key = R.string.keyWsServer;
            final Preference pref = findPreference(getString(key));
            if (pref != null) {
                final String urlSpeech = PreferenceUtils.getPrefString(prefs, res, key);
                pref.setSummary(String.format(getString(R.string.summaryWsServer), urlSpeech));
                //setSummaryWithStatus(pref, urlSpeech);
            }
        }

        // TODO: continuously update
        /*
        private void setSummaryWithStatus(final Preference pref, final String urlSpeech) {
            String urlStatus = urlSpeech.substring(0, urlSpeech.lastIndexOf('/') + 1) + "status";
            AsyncHttpClient.getDefaultInstance().websocket(urlStatus, "", new AsyncHttpClient.WebSocketConnectCallback() {
                @Override
                public void onCompleted(Exception ex, WebSocket webSocket) {
                    if (ex != null) {
                        // TODO: exception if this touches the view after onStringAvailable has done it?
                        pref.setSummary(String.format(getString(R.string.summaryWsServerWithStatusError),
                                urlSpeech, ex.getLocalizedMessage()));
                        return;
                    }
                    mWebSocket = webSocket;
                    mWebSocket.setStringCallback(new WebSocket.StringCallback() {
                        public void onStringAvailable(String s) {
                            Log.i(s);
                            try {
                                JSONObject json = new JSONObject(s);
                                int numWorkersAvailable = json.getInt("num_workers_available");
                                pref.setSummary(String.format(getString(R.string.summaryWsServerWithStatus),
                                        urlSpeech, numWorkersAvailable));
                            } catch (JSONException e) {
                                pref.setSummary(String.format(getString(R.string.summaryWsServerWithStatusError),
                                        urlSpeech, e.getLocalizedMessage()));
                            }
                        }
                    });
                }
            });
        }
        */
    }
}