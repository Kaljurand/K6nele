/*
 * Copyright 2015, Institute of Cybernetics at Tallinn University of Technology
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
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.IOException;

import ee.ioc.phon.android.speak.DetailsActivity;
import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.service.UtteranceRewriter;

public class RewriteLoaderActivity extends Activity {

    private static final int GET_CONTENT_REQUEST_CODE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/tab-separated-values");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        Intent chooser = Intent.createChooser(intent, "");
        startActivityForResult(chooser, GET_CONTENT_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == GET_CONTENT_REQUEST_CODE && resultCode == Activity.RESULT_OK && resultData != null) {
            Uri uri = resultData.getData();

            try {
                UtteranceRewriter utteranceRewriter = new UtteranceRewriter(getContentResolver(), uri);
                String rewritesAsTsv = utteranceRewriter.toTsv();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(getString(R.string.keyRewritesFile), rewritesAsTsv);
                editor.apply();
                Log.i(rewritesAsTsv);

                int count = utteranceRewriter.getRewrites().size();

                Intent searchIntent = new Intent(this, RewritesActivity.class);
                searchIntent.putExtra(DetailsActivity.EXTRA_TITLE, getResources().getQuantityString(R.plurals.statusLoadRewrites, count, count));
                searchIntent.putExtra(DetailsActivity.EXTRA_STRING_ARRAY, utteranceRewriter.toStringArray());
                startActivity(searchIntent);
            } catch (IOException e) {
                String errorMessage = String.format(getString(R.string.errorLoadRewrites), e.getLocalizedMessage());
                toast(errorMessage);
            }
        }
        finish();
    }

    protected void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}