/*
 * Copyright 2015-2016, Institute of Cybernetics at Tallinn University of Technology
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
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speechutils.editor.UtteranceRewriter;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

/**
 * Loads the rewrites from the EXTRAs of an incoming VIEW- or SEND-intent, or if they are missing,
 * then launches ACTION_GET_CONTENT to load the rewrites from its result data.
 * In case of an incoming VIEW/SEND-intent we only accept "text/tab-separated-values" (see the manifest).
 * However, if the user explicitly launches a file picker from Kõnele, then any "text/*" files
 * can be picked.
 */
public class RewritesLoaderActivity extends Activity {

    //private static final String TYPE = "text/tab-separated-values";
    private static final String TYPE = "text/*";
    private static final int GET_CONTENT_REQUEST_CODE = 1;

    private UtteranceRewriter utteranceRewriter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rewrites_loader);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Resources res = getResources();
        final Button bRewritesLoader = findViewById(R.id.bRewritesNameOk);
        final AutoCompleteTextView et = findViewById(R.id.etRewritesNameText);
        et.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                bRewritesLoader.performClick();
                return true;
            }
            return false;
        });
        bRewritesLoader.setOnClickListener(view -> saveAndShow(prefs, res, et.getText().toString()));
        List<String> keysSorted = new ArrayList<>(PreferenceUtils.getPrefMapKeys(prefs, res, R.string.keyRewritesMap));

        // If there are already some rewrites then we show their names as well
        if (!keysSorted.isEmpty()) {
            Collections.sort(keysSorted);
            String[] names = keysSorted.toArray(new String[0]);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
            et.setAdapter(adapter);

            final LinearLayout ll = findViewById(R.id.llRewritesChooser);
            ll.setVisibility(View.VISIBLE);
            final ListView lv = findViewById(R.id.lvRewrites);
            lv.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, names));

            lv.setOnItemClickListener((parent, view, position, id) -> saveAndShow(prefs, res, (String) lv.getItemAtPosition(position)));
        }

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri == null && intent.getExtras() == null) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType(TYPE);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            Intent chooser = Intent.createChooser(intent, "");
            startActivityForResult(chooser, GET_CONTENT_REQUEST_CODE);
        } else {
            // Responding to SEND and VIEW actions
            String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            if (subject != null) {
                et.setText(subject);
                et.setSelection(subject.length());
            }
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text == null) {
                if (uri == null) {
                    uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                }
                if (uri != null) {
                    if ("k6".equals(uri.getScheme())) {
                        byte[] data = Base64.decode(uri.getSchemeSpecificPart().substring(2), Base64.NO_WRAP | Base64.URL_SAFE);
                        try {
                            utteranceRewriter = new UtteranceRewriter(new String(data, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            // TODO: dont ignore
                        }
                    } else {
                        utteranceRewriter = loadFromUri(uri);
                    }
                }
            } else {
                utteranceRewriter = new UtteranceRewriter(text);
            }
            finishIfFailed();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == GET_CONTENT_REQUEST_CODE && resultCode == Activity.RESULT_OK && resultData != null) {
            Uri uri = resultData.getData();
            if (uri != null) {
                utteranceRewriter = loadFromUri(uri);
            }
        }
        finishIfFailed();
    }

    private UtteranceRewriter loadFromUri(Uri uri) {
        try {
            return new UtteranceRewriter(getContentResolver(), uri);
        } catch (IOException e) {
            String errorMessage = String.format(getString(R.string.errorLoadRewrites), e.getLocalizedMessage());
            toast(errorMessage);
        }
        return null;
    }

    private void finishIfFailed() {
        if (utteranceRewriter == null) {
            finish();
        }
    }

    private void saveAndShow(SharedPreferences prefs, Resources res, String name) {
        if (utteranceRewriter != null) {
            PreferenceUtils.putPrefMapEntry(prefs, res, R.string.keyRewritesMap, name, utteranceRewriter.toTsv());
            Intent intent = new Intent(this, RewritesActivity.class);
            intent.putExtra(RewritesActivity.EXTRA_NAME, name);
            intent.putExtra(RewritesActivity.EXTRA_ERRORS, utteranceRewriter.getErrorsAsStringArray());
            startActivity(intent);
        }
        finish();
    }

    private void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}