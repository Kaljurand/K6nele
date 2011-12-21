/*
 * Copyright 2011, Institute of Cybernetics at Tallinn University of Technology
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
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

import ee.ioc.phon.android.speak.R;


/**
 * <p>This is the simplest activity that calls the RecognizerIntent.ACTION_RECOGNIZE_SPEECH
 * and populates a list view with the returned extra (EXTRA_RESULTS)
 * (i.e. the words/phrases that alternatively match the input speech).</p>
 * 
 * @author Kaarel Kaljurand
 */
public class SimpleDemo extends AbstractRecognizerDemoActivity {

	private ListView mList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.simple_demo);
		mList = (ListView) findViewById(R.id.list_matches);

		if (getRecognizers().size() == 0) {
			toast(getString(R.string.errorRecognizerNotPresent));
			finish();
		} else {
			launchRecognizerIntent(createRecognizerIntent());
		}
	}


	/**
	 * <p>Show the results i.e. an ordered list of transcription candidates.</p>
	 */
	@Override
	protected void onSuccess(List<String> matches) {
		mList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, matches));
	}


	/**
	 * <p>Constructing the simplest ACTION_RECOGNIZER_SPEECH intent.
	 * The only required extra is EXTRA_LANGUAGE_MODEL.</p>
	 * 
	 * @return Recognizer Intent
	 */
	private Intent createRecognizerIntent() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
		return intent;
	}
}