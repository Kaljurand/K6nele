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
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import ee.ioc.phon.android.speak.ExecutableString;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.utils.Utils;
import ee.ioc.phon.android.speechutils.editor.UtteranceRewriter;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

/**
 * Loads the rewrites from the EXTRAs of an incoming SEND-intent, or if they are missing,
 * then launches ACTION_GET_CONTENT to load the rewrites from its result data.
 * In case of an incoming SEND-intent we only accept "text/tab-separated-values".
 * However, if the user explicitly launches a file picker from Kõnele, then any "text/*" files
 * can be picked.
 *
 * TODO: make a custom dialog (with scrolling)
 * TODO: finish() if import fails
 */
public class RewritesLoaderActivity extends Activity {

    //private static final String TYPE = "text/tab-separated-values";
    private static final String TYPE = "text/*";
    private static final int GET_CONTENT_REQUEST_CODE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadRewrites();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == GET_CONTENT_REQUEST_CODE && resultCode == Activity.RESULT_OK && resultData != null) {
            Uri uri = resultData.getData();
            if (uri != null) {
                handleUtteranceRewriter(loadFromUri(uri));
            }
        }
    }

    private void handleUtteranceRewriter(final UtteranceRewriter utteranceRewriter) {
        if (utteranceRewriter == null) {
            String errorMessage = String.format(getString(R.string.errorLoadRewrites), "");
            toast(errorMessage);
        } else {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            final Resources res = getResources();
            Set<String> keys = PreferenceUtils.getPrefMapKeys(prefs, res, R.string.keyRewritesMap);
            CharSequence[] keysAsArray = new CharSequence[keys.size()];
            int i = 0;
            // Java 8: keys.stream().sorted().collect(...)
            for (String key : keys) {
                keysAsArray[i++] = key;
            }
            Arrays.sort(keysAsArray);
            Utils.getTextEntryWithRadioDialog(
                    this,
                    getString(R.string.dialogTitleChangeRewritesName),
                    keysAsArray,
                    new ExecutableString() {
                        public void execute(String name) {
                            PreferenceUtils.putPrefMapEntry(prefs, res, R.string.keyRewritesMap, name, utteranceRewriter.toTsv());
                            show(res, name, utteranceRewriter);
                        }
                    }
            ).show();
        }
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

    private void loadRewrites() {
        Intent intent = getIntent();
        if (intent.getExtras() == null) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType(TYPE);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            Intent chooser = Intent.createChooser(intent, "");
            startActivityForResult(chooser, GET_CONTENT_REQUEST_CODE);
        } else {
            UtteranceRewriter utteranceRewriter = null;
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text == null) {
                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (uri != null) {
                    utteranceRewriter = loadFromUri(uri);
                }
            } else {
                utteranceRewriter = new UtteranceRewriter(text);
            }
            handleUtteranceRewriter(utteranceRewriter);
        }
    }

    private void show(Resources res, String name, UtteranceRewriter utteranceRewriter) {
        int count = utteranceRewriter.size();
        Intent intent = new Intent(this, RewritesActivity.class);
        intent.putExtra(DetailsActivity.EXTRA_TITLE, name + " · " + res.getQuantityString(R.plurals.statusLoadRewrites, count, count));
        intent.putExtra(DetailsActivity.EXTRA_STRING_ARRAY, utteranceRewriter.toStringArray());
        startActivity(intent);
        finish();
    }

    private void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}