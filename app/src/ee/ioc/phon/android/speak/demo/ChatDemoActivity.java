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

package ee.ioc.phon.android.speak.demo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.model.CallerInfo;
import ee.ioc.phon.android.speak.utils.Utils;
import ee.ioc.phon.android.speak.view.AbstractSpeechInputViewListener;
import ee.ioc.phon.android.speak.view.SpeechInputView;
import ee.ioc.phon.android.speechutils.editor.UtteranceRewriter;
import ee.ioc.phon.android.speechutils.utils.IntentUtils;
import ee.ioc.phon.android.speechutils.utils.JsonUtils;

/**
 * Simple chat style interface, which demonstrates how to use SpeechInputView.
 * <p>
 * TODO: each list item should have at least 3 components: spoken input,
 * pretty-printed output (JSON, or parts of it), formal output (JSON that can be executed)
 */
public class ChatDemoActivity extends Activity {

    private final List<String> mMatches = new ArrayList<>();

    private SharedPreferences mPrefs;
    private Resources mRes;
    private SpeechInputView mView;
    private ListView mList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_demo);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mRes = getResources();

        mView = (SpeechInputView) findViewById(R.id.vSpeechInputView);
        CallerInfo callerInfo = new CallerInfo(createExtras(), getCallingActivity());
        // TODO: review this
        mView.init(R.array.keysActivity, callerInfo);
        mView.setListener(getSpeechInputViewListener());

        mList = (ListView) findViewById(R.id.list_matches);

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object entry = parent.getAdapter().getItem(position);
                startActivity(entry.toString());
            }
        });
    }

    private void startActivity(String intentAsJson) {
        try {
            IntentUtils.startActivityIfAvailable(this, JsonUtils.createIntent(intentAsJson));
        } catch (JSONException e) {
            toast(e.getLocalizedMessage());
        }
    }

    private void updateListView(List<String> list) {
        mList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list));
    }

    private void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private SpeechInputView.SpeechInputViewListener getSpeechInputViewListener() {
        return new AbstractSpeechInputViewListener() {

            private Iterable<UtteranceRewriter> mRewriters;

            @Override
            public void onComboChange(String language, ComponentName service) {
                mRewriters = Utils.genRewriters(mPrefs, mRes, new String[]{"Base", "Commands"}, language, service, getComponentName());
            }

            @Override
            public void onFinalResult(List<String> results, Bundle bundle) {
                if (!results.isEmpty()) {
                    String result = results.get(0);
                    //String resultPp = "voice command (the raw utterance)\n\n" + result;
                    mMatches.add(result);
                    updateListView(mMatches);
                    // TODO: store the JSON also in the list, so that it can be reexecuted later
                    IntentUtils.launchIfIntent(ChatDemoActivity.this, mRewriters, result);
                }
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // TODO
            }

            @Override
            public void onError(int errorCode) {
                mMatches.add("* ERROR: " + errorCode);
                updateListView(mMatches);
            }

            @Override
            public void onStartListening() {
                // stopTts();
            }
        };
    }

    private static Bundle createExtras() {
        Bundle bundle = new Bundle();
        bundle.putInt(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        return bundle;
    }
}