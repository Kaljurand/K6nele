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

package ee.ioc.phon.android.speak;

import ee.ioc.phon.android.speak.provider.Server;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.speech.RecognitionService;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Preferences activity. Updates some preference-summaries automatically,
 * if the user changes a preference.</p>
 *
 * @author Kaarel Kaljurand
 */
public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private static final int ACTIVITY_SELECT_SERVER_URL = 1;

    private SharedPreferences mPrefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		SharedPreferences sp = getPreferenceScreen().getSharedPreferences();

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
	}


	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}


	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        populateRecognitionServices();
	}


    // TODO: update summary here
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Preference pref = findPreference(key);
		if (pref instanceof EditTextPreference) {
			EditTextPreference etp = (EditTextPreference) pref;
			pref.setSummary(etp.getText());
		} else if (pref instanceof ListPreference) {
			ListPreference lp = (ListPreference) pref;
			if (lp.getTitle().equals(getString(R.string.titleAutoStopAfterTime))) {
				setSummary(pref, getString(R.string.summaryAutoStopAfterTime), lp.getValue());
			} else if (lp.getTitle().equals(getString(R.string.titleRecordingRate))) {
				setSummary(pref, getString(R.string.summaryRecordingRate), lp.getValue());
			}
		}
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != Activity.RESULT_OK) {
			return;
		}
		switch (requestCode) {
		case ACTIVITY_SELECT_SERVER_URL:
			Uri serverUri = data.getData();
			if (serverUri == null) {
				toast(getString(R.string.errorFailedGetServerUrl));
			} else {
				long id = Long.parseLong(serverUri.getPathSegments().get(1));
				String url = Utils.idToValue(this, Server.Columns.CONTENT_URI, Server.Columns._ID, Server.Columns.URL, id);
				if (url != null) {
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
					SharedPreferences.Editor editor = prefs.edit();
					editor.putString(getString(R.string.keyServerHttp), url);
					editor.commit();
				}
			}
			break;
		}
	}


	private void setSummary(Preference pref, String strText, String strArg) {
        if (pref != null) {
            pref.setSummary(String.format(strText, strArg));
        }
    }

	private void toast(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
	}

    /**
     * TODO: if nothing is selected then set ours as the default
     */
    private void populateRecognitionServices() {
        PackageManager pm = getPackageManager();
        List<ResolveInfo> services = pm.queryIntentServices(
                new Intent(RecognitionService.SERVICE_INTERFACE), 0);

        // Currently selected service (identified by class name)
        String selectedService = Utils.getPrefString(mPrefs, getResources(), R.string.keyImeRecognitionService);
        int selectedIndex = 0;

        Log.i("populateRecognitionServices: " + selectedService);

        CharSequence[] entries = new CharSequence[services.size()];
        CharSequence[] entryValues = new CharSequence[services.size()];

        int index = 0;
        for (ResolveInfo ri : services) {
            ServiceInfo si = ri.serviceInfo;
            if (si == null) {
                Log.i("serviceInfo == null");
                continue;
            }
            String pkg = si.packageName;
            String cls = si.name;
            CharSequence label = si.loadLabel(pm);
            Log.i(label + " :: " + pkg + " :: " + cls);
            entries[index] = label;
            String value = pkg + '|' + cls;
            entryValues[index] = value;
            Log.i("populateRecognitionServices: " + entryValues[index]);
            if (value.equals(selectedService)) {
                selectedIndex = index;
            }
            index++;
        }

        if (services.size() > 0) {
            ListPreference list = (ListPreference) findPreference(getString(R.string.keyImeRecognitionService));
            list.setEntries(entries);
            list.setEntryValues(entryValues);
            list.setValueIndex(selectedIndex);
            list.setSummary(list.getEntry());
        }
    }

}