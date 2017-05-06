/*
 * Copyright 2017, Institute of Cybernetics at Tallinn University of Technology
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

package ee.ioc.phon.android.speak.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import ee.ioc.phon.android.speechutils.utils.HttpUtils;


/**
 * Simple activity for showing and changing the stored preferences.
 * Meant mostly to be able to change the settings on a notouch device (Android Things).
 * Needs to be exported to be callable from adb. Could be also hooked up to rewrite rules.
 * TODO: maybe implement it instead as a service or broadcast receiver
 * TODO: export it by default but require a custom dangerous permission
 * TODO: set val as a return value instead of toasting it
 */
public class GetPutPreferenceActivity extends Activity {

    public static final String EXTRA_KEY = "key";
    public static final String EXTRA_VAL = "val";
    public static final String EXTRA_IS_URL = "is_url";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        if (intent == null) {
            finish();
            return;
        }

        Bundle extras = intent.getExtras();

        if (extras == null) {
            finish();
            return;
        }

        final String key = extras.getString(EXTRA_KEY);

        if (key == null) {
            finish();
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GetPutPreferenceActivity.this);

        // If EXTRA_VAL is provided then change the value of the key,
        // if not then show the value of the key.
        if (extras.containsKey(EXTRA_VAL)) {
            Object val = extras.get(EXTRA_VAL);
            final SharedPreferences.Editor editor = prefs.edit();
            if (val == null) {
                // adb --esn
                editor.remove(key);
                editor.apply();
            } else {
                if (val instanceof String) {
                    String valAsStr = (String) val;
                    // In case is_url==true then interpret the value as a URL and instead
                    // of setting the key to the value, set the key to the content behind the URL
                    // (or an error message, if something goes wrong).
                    // Note that this currently works only if the type of the value is String.
                    if (extras.getBoolean(EXTRA_IS_URL, false)) {
                        toast(key + " := fetchUrl(" + valAsStr + ")");
                        new AsyncTask<String, Void, String>() {

                            @Override
                            protected String doInBackground(String... urls) {
                                try {
                                    return HttpUtils.getUrl(urls[0]);
                                } catch (IOException e) {
                                    return "ERROR: Unable to retrieve " + urls[0] + ": " + e.getLocalizedMessage();
                                }
                            }

                            @Override
                            protected void onPostExecute(String result) {
                                editor.putString(key, result);
                                editor.apply();
                            }
                        }.execute(valAsStr);
                    } else {
                        editor.putString(key, valAsStr);
                        editor.apply();
                    }
                } else if (val instanceof String[]) {
                    // adb --esa
                    editor.putStringSet(key, new HashSet<>(Arrays.asList((String[]) val)));
                    editor.apply();
                } else if (val instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) val);
                    editor.apply();
                } else if (val instanceof Integer) {
                    editor.putInt(key, (Integer) val);
                    editor.apply();
                } else if (val instanceof Float) {
                    editor.putFloat(key, (Float) val);
                    editor.apply();
                } else if (val instanceof Long) {
                    editor.putLong(key, (Long) val);
                    editor.apply();
                }
            }
            toast(key + " := " + val);
        } else {
            toast(key + " == " + prefs.getAll().get(key));
        }

        finish();
    }

    private void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}