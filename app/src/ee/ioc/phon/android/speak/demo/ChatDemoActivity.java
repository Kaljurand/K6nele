/*
 * Copyright 2016, Institute of Cybernetics at Tallinn University of Technology
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

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.utils.IntentUtils;
import ee.ioc.phon.android.speechutils.Extras;
import ee.ioc.phon.android.speechutils.utils.JsonUtils;


/**
 * Simple chat style interface.
 * <p>
 * TODO: each list item should have at least 3 components: spoken input,
 * pretty-printed output (JSON, or parts of it), formal output (JSON that can be executed)
 */
public class ChatDemoActivity extends AbstractRecognizerDemoActivity {

    private final List<String> mMatches = new ArrayList<>();

    private final Intent mIntent = createRecognizerIntent();
    private ListView mList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_demo);
        Button startButton = (Button) findViewById(R.id.buttonStart);
        startButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                launchRecognizerIntentSimple(mIntent);
            }
        });
        mList = (ListView) findViewById(R.id.list_matches);

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object entry = parent.getAdapter().getItem(position);
                startActivity(entry.toString());
            }
        });
    }


    @Override
    protected void onSuccess(Intent intent) {
        // TODO: inputs is a list of matches before rewriting
        ArrayList<String> inputs = intent.getStringArrayListExtra("TODO:inputs");
        ArrayList<String> matches = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (!matches.isEmpty()) {
            String result = matches.get(0);
            //String resultPp = "voice command (the raw utterance)\n\n" + result;
            try {
                String resultPp = "/* .... */\n" + JsonUtils.parseJson(result).toString(2);
                mMatches.add(resultPp);
            } catch (JSONException e) {
                mMatches.add("ERROR: " + e.getLocalizedMessage());
            }
            // TODO: execute the result
            updateListView(mMatches);
        }
    }

    @Override
    protected void onCancel() {
        // Overriding the parent in order not to finish()
    }

    @Override
    protected void onError(int errorCode) {
        mMatches.add("* " + getErrorMessage(errorCode));
        updateListView(mMatches);
    }

    private void startActivity(String intentAsJson) {
        IntentUtils.startActivityFromJson(this, intentAsJson);
    }

    private static Intent createRecognizerIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say command");
        intent.putExtra(Extras.EXTRA_VOICE_PROMPT, "Say command");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(Extras.EXTRA_AUTO_START, true);
        intent.putExtra(Extras.EXTRA_RETURN_ERRORS, true);
        // TODO: load a specific rewrites file
        return intent;
    }


    private void updateListView(List<String> list) {
        mList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list));
    }
}