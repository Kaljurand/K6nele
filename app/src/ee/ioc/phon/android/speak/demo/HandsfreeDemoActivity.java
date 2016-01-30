/*
 * Copyright 2011-2016, Institute of Cybernetics at Tallinn University of Technology
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
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speechutils.Extras;


/**
 * This is a demo/testing activity which interacts with the recognizer in a hands-free mode.
 * It calls RecognizerIntent.ACTION_RECOGNIZE_SPEECH using EXTRA_AUTO_START,
 * adds the returned results or error messages to a cumulative list of results and
 * immediately calls RecognizerIntent again.
 * <p/>
 * Using the menu, the user can declare that the input conforms to a grammar.
 */
public class HandsfreeDemoActivity extends AbstractRecognizerDemoActivity {

    private final List<String> mMatches = new ArrayList<>();

    private final Intent mIntent = createRecognizerIntent();
    private ListView mList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handsfree_demo);
        Button startButton = (Button) findViewById(R.id.buttonStart);
        startButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                launchRecognizerIntent(mIntent);
            }
        });
        mList = (ListView) findViewById(R.id.list_matches);
    }


    @Override
    protected void onSuccess(Intent intent) {
        ArrayList<String> matches = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (matches.isEmpty()) {
            mMatches.add(0, "No matches");
        } else {
            // Here we could do something with the transcription, e.g. switch on lights,
            // skip to the next track, change the channel, etc.
            mMatches.add(0, matches.get(0));
        }

        updateListView(mMatches);
        restart();
    }

    @Override
    protected void onError(int errorCode) {
        mMatches.add(0, "* " + getErrorMessage(errorCode));
        updateListView(mMatches);
        restart();
    }


    /**
     * Sleep for 0.8 sec and restart the session.
     */
    private void restart() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                launchRecognizerIntent(mIntent);
            }
        }, 800);
    }

    private static Intent createRecognizerIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(Extras.EXTRA_AUTO_START, true);
        intent.putExtra(Extras.EXTRA_RETURN_ERRORS, true);
        return intent;
    }


    private void updateListView(List<String> list) {
        mList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list));
    }
}