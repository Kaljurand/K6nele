/*
 * Copyright 2011-2012, Institute of Cybernetics at Tallinn University of Technology
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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.RecognizerIntent;


/**
 * <p>This is a demo/testing activity which calls the RecognizerIntent.ACTION_RECOGNIZE_SPEECH.
 * If there are results then it adds them to the cumulative list of results
 * and immediately calls the recognizer intent again.</p>
 * 
 * <p>If the recognizer intent is set to start and stop automatically then one can have a completely
 * hands-free experience.</p>
 * 
 * <p>Using the menu, the user can declare that the input conforms to a grammar.</p>
 * 
 * @author Kaarel Kaljurand
 */
public class RepeaterDemo extends AbstractRecognizerDemoActivity implements OnClickListener {

	// We make it static so that it would survive Destroy.
	private static final List<String> mMatches = new ArrayList<String>();

	private final Intent mIntent = createRecognizerIntent();
	private ListView mList;
	private ImageButton speakButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.complex_demo);
		speakButton = (ImageButton) findViewById(R.id.buttonMicrophone);
		mList = (ListView) findViewById(R.id.list_matches);
		updateListView(mMatches);

		if (getRecognizers(mIntent).size() == 0) {
			speakButton.setEnabled(false);
			toast(getString(R.string.errorRecognizerNotPresent));
		} else {
			speakButton.setOnClickListener(this);
		}
	}


	public void onClick(View v) {
		if (v.getId() == R.id.buttonMicrophone) {
			launchRecognizerIntent(mIntent);
		}
	}


	@Override
	protected void onSuccess(Intent intent) {
        ArrayList<String> matches = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
		if (! matches.isEmpty()) {
			mMatches.add(0, matches.get(0));
			updateListView(mMatches);

			// Here we could do something with the transcription, e.g. switch on lights,
			// skip to the next track, change the channel, etc.
			// Instead we just sleep for 0.8 sec.

			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					launchRecognizerIntent(mIntent);
				}
			}, 800);
		}
	}


	private static Intent createRecognizerIntent() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
		return intent;
	}


	private void updateListView(List<String> list) {
		mList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list));
	}
}