/*
 * Copyright 2011-2013, Institute of Cybernetics at Tallinn University of Technology
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

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import ee.ioc.phon.android.speak.utils.IntentUtils;

/**
 * <p>Simple activity that displays the String-array attached to the intent and plays the
 * audio data contained in the intent data.
 * Not really meant for the end-users.</p>
 *
 * @author Kaarel Kaljurand
 */
public class DetailsActivity extends ListActivity {

	public static final String EXTRA_TITLE = "EXTRA_TITLE";
	public static final String EXTRA_STRING_ARRAY = "EXTRA_STRING_ARRAY";

	private MediaPlayer mMediaPlayer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();

		Uri audioUri = intent.getData();
		if (audioUri != null) {
			playAudio(audioUri, intent.getType());
		}

		Bundle extras = intent.getExtras();
		if (extras != null) {
			String title = extras.getString(EXTRA_TITLE);
			if (title == null) {
				requestWindowFeature(Window.FEATURE_NO_TITLE);
			} else {
				setTitle(title);
			}

			String[] stringArray = extras.getStringArray(EXTRA_STRING_ARRAY);
			if (stringArray != null) {
				setListAdapter(new ArrayAdapter<>(this, R.layout.list_item_detail, stringArray));

				getListView().setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						IntentUtils.startSearchActivity(DetailsActivity.this, ((TextView) view).getText());
                        // TODO: can we finish the parent as well? or maybe return the selection to parent?
                        finish();
                    }
				});
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
		}
	}

	/**
	 * @param uri audio URI to be played
	 */
	private void playAudio(Uri uri, String type) {
		mMediaPlayer = MediaPlayer.create(this, uri);
		if (mMediaPlayer == null) {
            // create can return null, e.g. on Android Wear
			toast(String.format(getString(R.string.errorFailedPlayAudio), uri.toString(), type));
		} else {
			int duration = mMediaPlayer.getDuration();
			mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mediaPlayer) {
					toast(getString(R.string.toastPlayingAudioDone));
				}
			});
			mMediaPlayer.start();
			toast(String.format(getString(R.string.toastPlayingAudio), uri.toString(), type, duration));
		}
	}

	private void toast(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
	}
}