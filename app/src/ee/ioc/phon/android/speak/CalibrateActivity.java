/*
 * Copyright 2012, Institute of Cybernetics at Tallinn University of Technology
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

package ee.ioc.phon.android.speak;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.RecognizerIntent;
import ee.ioc.phon.android.speak.demo.AbstractRecognizerDemoActivity;


/**
 * TODO: work in progress
 * 
 * TODO: show word error rate, edit distance, or some other measure
 * 
 * @author Kaarel Kaljurand
 */
public class CalibrateActivity extends AbstractRecognizerDemoActivity implements OnClickListener {

	private final List<String> mMatches = new ArrayList<String>();
	private final Intent mIntent = createRecognizerIntent();
	private ListView mList;
	private Button speakButton;
	private Iterator<String> mPhraseIterator;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calibrate);
		speakButton = (Button) findViewById(R.id.buttonMicrophone);
		mList = (ListView) findViewById(R.id.list_matches);

		if (getRecognizers(mIntent).size() == 0) {
			speakButton.setEnabled(false);
			toast(getString(R.string.errorRecognizerNotPresent));
		} else {
			speakButton.setOnClickListener(this);
		}
	}


	public void onClick(View v) {
		if (v.getId() == R.id.buttonMicrophone) {
			URL url = null;
			try {
				url = new URL(getString(R.string.defaultUrlCalibratePhrases));
			} catch (MalformedURLException e) {
				toast("ERROR: malformed URL");
				finish();
				return;
			}
			List<String> phrases = getPhrases(url);
			if (phrases != null && ! phrases.isEmpty()) {
				mPhraseIterator = phrases.iterator();
				launchRecognizerIntent(mIntent, mPhraseIterator.next());
			} else {
				toast("ERROR: No phrases to test with...");
			}
		}
	}


	@Override
	protected void onSuccess(List<String> matches) {
		if (matches.isEmpty()) {
			mMatches.add("(NO MATCH)");
		} else {
			String match = matches.get(0);
			String m1 = match.replaceAll("\\W+", "");
			String m2 = mPhraseIterator.toString().toLowerCase().replaceAll("\\W+", "");
			if (m1.equals(m2)) {
				match = "[OK] " + match;
			}
			mMatches.add(0, match);
		}
		mList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mMatches));

		if (mPhraseIterator.hasNext()) {
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					launchRecognizerIntent(mIntent, mPhraseIterator.next());
				}
			}, 800);
		}
	}


	private static List<String> getPhrases() {
		List<String> phrases = new ArrayList<String>();
		phrases.add("tere");
		phrases.add("elas metsas");
		return phrases;
	}


	private static Intent createRecognizerIntent() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
		return intent;
	}


	private List<String> getPhrases(URL url) {
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
		} catch (IOException e) {
			return null;
		}
		InputStream in = null;
		try {
			in = conn.getInputStream(); 
			int http_status = conn.getResponseCode();
			if (http_status / 100 != 2) {
				conn.disconnect();
				return null;
			}
		} catch (IOException e) {
			conn.disconnect();
			return null;
		}

		try {
			return readFromJson(new InputStreamReader(in, "UTF8"));
		} catch (IOException e) {
			return null;
		} finally {
			conn.disconnect();
		}
	}


	private List<String> readFromJson(InputStreamReader reader) throws IOException {
		List<String> phrases = new ArrayList<String>();
		Object obj = JSONValue.parse(reader);
		if (obj == null) {
			throw new IOException("Server response is not well-formed");
		}

		for (Object o : (JSONArray) obj) {
			add(phrases, o);
		}
		return phrases;
	}


	private static void add(List<String> list, Object obj) {
		if (obj != null) {
			String str = obj.toString();
			if (str.length() > 0) {
				list.add(str);
			}
		}
	}
}