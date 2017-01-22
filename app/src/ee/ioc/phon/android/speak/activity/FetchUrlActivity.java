/*
 * Copyright 2016-2017, Institute of Cybernetics at Tallinn University of Technology
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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import ee.ioc.phon.android.speechutils.utils.IntentUtils;
import ee.ioc.phon.android.speechutils.utils.JsonUtils;

/**
 * Queries the given URL,
 * optionally interprets its response an a JSON that encodes an intent, and launches this intent.
 * <p>
 * TODO: handle errors
 * TODO: handle latency, e.g. show progress in notification (but notifications are not available on Android Things)
 * TODO: add extras for various HTTP parameters
 *
 * @author Kaarel Kaljurand
 */
public class FetchUrlActivity extends Activity {

    public static final String EXTRA_HTTP_METHOD = "ee.ioc.phon.android.extra.HTTP_METHOD";
    public static final String EXTRA_HTTP_BODY = "ee.ioc.phon.android.extra.HTTP_BODY";
    public static final String EXTRA_LAUNCH_RESPONSE_AS_INTENT = "ee.ioc.phon.android.extra.LAUNCH_RESPONSE_AS_INTENT";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) {
            Uri uri = intent.getData();
            if (uri != null) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) {
                    bundle = new Bundle();
                }
                HttpTask task = new HttpTask(bundle);
                task.execute(uri.toString());
                return;
            }
        }
        finish();
    }

    private void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private static String fetchUrl(String myurl, String method, String body) throws IOException {
        byte[] outputInBytes = null;

        if (body != null) {
            outputInBytes = body.getBytes("UTF-8");
        }

        InputStream is = null;
        // Only display the first x characters of the retrieved
        // web page content.
        // TODO: fix this
        int len = 1024;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(5000 /* milliseconds */);
            conn.setConnectTimeout(5000 /* milliseconds */);
            conn.setRequestMethod(method);
            conn.setDoInput(true);

            if (outputInBytes == null) {
                conn.connect();
            } else {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(outputInBytes);
                os.close();
            }

            //int response = conn.getResponseCode();
            is = conn.getInputStream();

            // Convert the InputStream into a string
            return readIt(is, len);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private static String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }

    private class HttpTask extends AsyncTask<String, Void, String> {

        private final String mHttpMethod;
        private final String mHttpBody;
        private final boolean mLaunchResponseAsIntent;

        private HttpTask(@NonNull Bundle bundle) {
            mHttpMethod = bundle.getString(EXTRA_HTTP_METHOD, "GET");
            mHttpBody = bundle.getString(EXTRA_HTTP_BODY);
            mLaunchResponseAsIntent = bundle.getBoolean(EXTRA_LAUNCH_RESPONSE_AS_INTENT, false);
        }

        @Override
        protected String doInBackground(String... urls) {
            try {
                return fetchUrl(urls[0], mHttpMethod, mHttpBody);
            } catch (IOException e) {
                return "Unable to retrieve " + urls[0] + " " + e.getLocalizedMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO: handle errors differently
            if (mLaunchResponseAsIntent) {
                try {
                    IntentUtils.startActivityIfAvailable(FetchUrlActivity.this, JsonUtils.createIntent(result));
                } catch (JSONException e) {
                    toast(e.getLocalizedMessage());
                }
            } else {
                toast(result);
            }
            finish();
        }
    }
}