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

import java.io.IOException;
import java.util.Map;

import ee.ioc.phon.android.speechutils.utils.BundleUtils;
import ee.ioc.phon.android.speechutils.utils.HttpUtils;
import ee.ioc.phon.android.speechutils.utils.IntentUtils;

/**
 * Queries the given URL and rewrites the response. Toasts the result unless it was executed as a command.
 * <p>
 * TODO: handle latency, e.g. show progress in notification (but notifications are not available on Android Things)
 * TODO: add extras for error handing (e.g. map 503 to a string)
 *
 * @author Kaarel Kaljurand
 */
public class FetchUrlActivity extends Activity {

    public static final String EXTRA_HTTP_METHOD = "ee.ioc.phon.android.extra.HTTP_METHOD";
    public static final String EXTRA_HTTP_BODY = "ee.ioc.phon.android.extra.HTTP_BODY";
    public static final String EXTRA_HTTP_HEADER = "ee.ioc.phon.android.extra.HTTP_HEADER";

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

    private class HttpTask extends AsyncTask<String, Void, String> {

        private final Bundle mBundle;
        private final String mHttpMethod;
        private final String mHttpBody;
        private final Map<String, String> mHttpHeader;

        private HttpTask(@NonNull Bundle bundle) {
            mBundle = bundle;
            mHttpMethod = bundle.getString(EXTRA_HTTP_METHOD, "GET");
            mHttpBody = bundle.getString(EXTRA_HTTP_BODY);
            // Mapping HTTP_HEADER bundle to a Map<String, String>,
            // assuming that all its elements are strings.
            mHttpHeader = BundleUtils.getBundleAsMapOfString(bundle.getBundle(EXTRA_HTTP_HEADER));
        }

        @Override
        protected String doInBackground(String... urls) {
            try {
                return HttpUtils.fetchUrl(urls[0], mHttpMethod, mHttpBody, mHttpHeader);
            } catch (IOException e) {
                return "Unable to retrieve " + urls[0] + ": " + e.getLocalizedMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO: handle errors differently
            String newResult = IntentUtils.rewriteResultWithExtras(FetchUrlActivity.this, mBundle, result);
            if (newResult != null) {
                toast(newResult);
            }
            finish();
        }
    }
}