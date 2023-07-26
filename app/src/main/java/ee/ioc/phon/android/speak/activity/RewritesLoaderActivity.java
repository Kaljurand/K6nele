/*
 * Copyright 2015-2020, Institute of Cybernetics at Tallinn University of Technology
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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.databinding.ActivityRewritesLoaderBinding;
import ee.ioc.phon.android.speechutils.editor.UtteranceRewriter;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

/**
 * Loads the rewrites from the EXTRAs of an incoming VIEW- or SEND-intent, or if they are missing,
 * then launches ACTION_GET_CONTENT to load the rewrites from its result data.
 * In case of an incoming VIEW/SEND-intent we only accept "text/tab-separated-values" (see the manifest).
 * However, if the user explicitly launches a file picker from Kõnele, then any "text/*" files
 * can be picked.
 */
public class RewritesLoaderActivity extends AppCompatActivity {

    //private static final String TYPE = "text/tab-separated-values";
    private static final String TYPE = "text/*";

    private UtteranceRewriter utteranceRewriter;

    private ActivityRewritesLoaderBinding binding;

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = result.getData();
                    Uri uri = intent.getData();
                    if (uri != null) {
                        utteranceRewriter = loadFromUri(uri);
                    }
                }
                finishIfFailed();
            });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRewritesLoaderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Resources res = getResources();
        binding.etRewritesNameText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.bRewritesNameOk.performClick();
                return true;
            }
            return false;
        });
        binding.bRewritesNameOk.setOnClickListener(view -> saveAndShow(prefs, res, binding.etRewritesNameText.getText().toString()));
        List<String> keysSorted = new ArrayList<>(PreferenceUtils.getPrefMapKeys(prefs, res, R.string.keyRewritesMap));

        // If there are already some rewrites then we show their names as well
        if (!keysSorted.isEmpty()) {
            Collections.sort(keysSorted);
            String[] names = keysSorted.toArray(new String[0]);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
            binding.etRewritesNameText.setAdapter(adapter);

            binding.llRewritesChooser.setVisibility(View.VISIBLE);
            binding.lvRewrites.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, names));

            binding.lvRewrites.setOnItemClickListener((parent, view, position, id) -> saveAndShow(prefs, res, (String) binding.lvRewrites.getItemAtPosition(position)));
        }

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri == null && intent.getExtras() == null) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType(TYPE);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            Intent chooser = Intent.createChooser(intent, "");
            mStartForResult.launch(chooser);
        } else {
            // Responding to SEND and VIEW actions
            String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            if (subject != null) {
                binding.etRewritesNameText.setText(subject);
                binding.etRewritesNameText.setSelection(subject.length());
            }
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text == null) {
                if (uri == null) {
                    uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                }
                if (uri != null) {
                    if ("k6".equals(uri.getScheme())) {
                        byte[] data = Base64.decode(uri.getSchemeSpecificPart().substring(2), Base64.NO_WRAP | Base64.URL_SAFE);
                        utteranceRewriter = new UtteranceRewriter(new String(data, StandardCharsets.UTF_8));
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
            startActivity(intent);

            // If there were errors then we load another activity to show them.
            // TODO: verify that this is a correct way to start multiple activities
            String[] errors = utteranceRewriter.getErrorsAsStringArray();
            if (errors.length > 0) {
                Intent searchIntent = new Intent(this, RewritesErrorsActivity.class);
                searchIntent.putExtra(RewritesErrorsActivity.EXTRA_TITLE, name);
                searchIntent.putExtra(RewritesErrorsActivity.EXTRA_STRING_ARRAY, errors);
                startActivity(searchIntent);
            }
        }
        finish();
    }

    private void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}