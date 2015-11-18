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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.R;

public class RewriteLoaderActivity extends Activity {

    private static final int GET_CONTENT_REQUEST_CODE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        Intent chooser = Intent.createChooser(intent, "");
        startActivityForResult(chooser, GET_CONTENT_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        Log.i("onActivityResult: " + resultCode + " " + resultData);
        if (requestCode == GET_CONTENT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            if (resultData != null) {
                Uri uri = resultData.getData();
                Log.i("Uri: " + uri.toString());

                try {
                    String str = readTextFromUri(uri);
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(getString(R.string.keyRewritesFile), str);
                    editor.apply();
                    Log.i("Loaded: " + str);
                } catch (IOException e) {
                    // TODO
                }
                finish();
            }
        }
    }


    /**
     * TODO: filter out comments and lines which do not compile as regex
     */
    private String readTextFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            Log.i("Loading: " + line);
            String[] splits = line.split("\t");
            if (splits.length == 2) {
                stringBuilder.append(line);
                stringBuilder.append('\n');
            }
        }
        inputStream.close();
        return stringBuilder.toString();
    }
}