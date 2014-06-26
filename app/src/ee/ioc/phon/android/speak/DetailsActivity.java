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

/**
 * <p>Simple activity for displaying String-arrays.
 * Not really meant for the end-users.</p>
 * 
 * @author Kaarel Kaljurand
 */
public class DetailsActivity extends ListActivity {

	public static final String EXTRA_TITLE = "EXTRA_TITLE";
	public static final String EXTRA_STRING_ARRAY = "EXTRA_STRING_ARRAY";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        Uri audioUri = intent.getData();
        if (audioUri != null) {
            playAudio(audioUri);
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
				setListAdapter(new ArrayAdapter<String>(this, R.layout.list_item_detail, stringArray));

				getListView().setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						Intent intentWebSearch = new Intent(Intent.ACTION_WEB_SEARCH);
						intentWebSearch.putExtra(SearchManager.QUERY, ((TextView) view).getText());
						startActivity(intentWebSearch);
						// TODO: can we finish the parent as well? or maybe return the selection to parent?
						finish();
					}
				});
			}
		}
	}

    /**
     * @param uri audio URI to be played
     * TODO: do error checking and put strings to resources
     */
    private void playAudio(Uri uri) {
        final MediaPlayer mp = MediaPlayer.create(this, uri);
        int duration = mp.getDuration();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mp.release();
                Toast.makeText(getApplicationContext(), "Done playing the audio", Toast.LENGTH_LONG).show();
            }
        });
        mp.start();
        Toast.makeText(this, "Playing the recorded audio: " + duration + " ms", Toast.LENGTH_LONG).show();
    }
}
