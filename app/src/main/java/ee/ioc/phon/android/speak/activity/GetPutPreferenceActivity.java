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
import android.text.TextUtils;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import ee.ioc.phon.android.speak.Executable;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.utils.Utils;
import ee.ioc.phon.android.speechutils.utils.HttpUtils;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;


/**
 * Simple activity for showing and changing the stored preferences.
 * Meant mostly to be able to change the settings on a device with limited GUI
 * (e.g. Android Things, Android Wear), using adb or rewrite rules.
 * TODO: maybe implement it instead as a service or broadcast receiver
 * TODO: allow GUI to be skipped with EXTRA SKIP_UI but then require a custom dangerous permission
 * TODO: set val as a return value instead of toasting it
 * TODO: add a String EXTRA "OP". If defined then the given operation is used to combine the existing
 * value with the new value. The operation can be set add, list append, arithmetical add, Boolean XOR, etc.
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
        boolean skipUi = PreferenceUtils.getPrefBoolean(prefs, getResources(), R.string.keyGetPutPrefSkipUi, R.bool.defaultGetPutPrefSkipUi);

        // If EXTRA_VAL is provided then change the value of the key,
        // if not then show the value of the key.
        if (extras.containsKey(EXTRA_VAL)) {
            Object val = extras.get(EXTRA_VAL);
            if (val == null) {
                // adb --esn
                execute(String.format(getString(R.string.taskPrefRemove), key),
                        createExecutableRemove(prefs, key), skipUi);
            } else {
                if (val instanceof String) {
                    String valAsStr = (String) val;
                    // In case is_url==true then interpret the value as a URL and instead
                    // of setting the key to the value, set the key to the content behind the URL
                    // (or an error message, if something goes wrong).
                    // Note that this currently works only if the type of the value is String.
                    if (extras.getBoolean(EXTRA_IS_URL, false)) {
                        execute(String.format(getString(R.string.taskPrefPutUrl), key, valAsStr),
                                createExecutablePutUrl(prefs, key, valAsStr), skipUi);
                    } else {
                        execute(String.format(getString(R.string.taskPrefPut), key, val),
                                createExecutablePut(prefs, key, valAsStr), skipUi);
                    }
                } else {
                    String valAsStr;
                    if (val instanceof String[]) {
                        valAsStr = TextUtils.join(",", (String[]) val);
                    } else {
                        valAsStr = val.toString();
                    }
                    execute(String.format(getString(R.string.taskPrefPut), key, valAsStr),
                            createExecutablePut(prefs, key, val), skipUi);
                }
            }
        } else {
            toast(key + " == " + prefs.getAll().get(key));
            finish();
        }
    }

    private void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void execute(final String message, final Executable ex, boolean skipUi) {
        if (skipUi) {
            ex.execute();
            toast(message);
            finish();
        } else {
            Utils.getYesNoDialog(
                    this,
                    message,
                    ex,
                    this::finish
            ).show();
        }
    }

    private Executable createExecutablePutUrl(final SharedPreferences prefs, final String key, final String url) {
        return () -> {
            final SharedPreferences.Editor editor = prefs.edit();
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
            }.execute(url);
            finish();
        };
    }

    private Executable createExecutablePut(final SharedPreferences prefs, final String key, final Object val) {
        return () -> {
            SharedPreferences.Editor editor = prefs.edit();
            if (val instanceof String[]) {
                // adb --esa
                editor.putStringSet(key, new HashSet<>(Arrays.asList((String[]) val)));
            } else if (val instanceof Boolean) {
                editor.putBoolean(key, (Boolean) val);
            } else if (val instanceof Integer) {
                editor.putInt(key, (Integer) val);
            } else if (val instanceof Float) {
                editor.putFloat(key, (Float) val);
            } else if (val instanceof Long) {
                editor.putLong(key, (Long) val);
            } else {
                editor.putString(key, val.toString());
            }
            editor.apply();
            finish();
        };
    }

    private Executable createExecutableRemove(final SharedPreferences prefs, final String key) {
        return () -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(key);
            editor.apply();
            finish();
        };
    }
}