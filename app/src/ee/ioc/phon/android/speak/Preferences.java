/*
 * Copyright 2011-2014, Institute of Cybernetics at Tallinn University of Technology
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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.speech.RecognitionService;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.List;

/**
 * <p>Preferences activity. Updates some preference-summaries automatically,
 * if the user changes a preference.</p>
 *
 * @author Kaarel Kaljurand
 */
public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
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
        populateRecognitionServices(getString(R.string.defaultImeRecognizerService));

        // If the K6nele IME is enabled then we remove the link to the IME settings,
        // if not already removed.
        // If the IME is not enabled then we add the link. The position of the link seems
        // to respect the position in preferences.xml.
        if (isK6neleImeEnabled()) {
            PreferenceCategory category = (PreferenceCategory) findPreference(getString(R.string.keyCategoryIme));
            Preference pref = category.findPreference(getString(R.string.keyEnableIme));
            if (pref != null) {
                category.removePreference(pref);
            }
        } else {
            Preference pref = findPreference(getString(R.string.keyEnableIme));
            PreferenceCategory category = (PreferenceCategory) findPreference(getString(R.string.keyCategoryIme));
            category.addPreference(pref);
        }
    }


    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref instanceof ListPreference) {
            ListPreference lp = (ListPreference) pref;
            pref.setSummary(lp.getEntry());
        }
    }


    /**
     * Populates the list of available recognizer services and adds a choice for the system default
     * service. If no service is currently selected (when the user accesses the preferences menu
     * for the first time), then selects the item that points to the preferredService (this is
     * Kõnele's own service).
     *
     * @param preferredService Service to select if none was selected
     */
    private void populateRecognitionServices(String preferredService) {
        PackageManager pm = getPackageManager();
        List<ResolveInfo> services = pm.queryIntentServices(
                new Intent(RecognitionService.SERVICE_INTERFACE), 0);

        int numberOfServices = services.size();

        // This should never happen because K6nele comes with several services
        if (numberOfServices == 0) {
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // Currently selected service (identified by class name)
        String selectedService = Utils.getPrefString(prefs, getResources(), R.string.keyImeRecognitionService);
        int selectedIndex = -1;
        int preferredIndex = 0;

        CharSequence[] entries = new CharSequence[numberOfServices + 1];
        CharSequence[] entryValues = new CharSequence[numberOfServices + 1];

        // System default as the first listed choice
        entries[0] = getString(R.string.labelDefaultRecognitionService);
        entryValues[0] = getString(R.string.keyDefaultRecognitionService);

        int index = 1;
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
            } else if (value.equals(preferredService)) {
                preferredIndex = index;
            }
            index++;
        }

        if (selectedIndex == -1) {
            selectedIndex = preferredIndex;
        }

        ListPreference list = (ListPreference) findPreference(getString(R.string.keyImeRecognitionService));
        list.setEntries(entries);
        list.setEntryValues(entryValues);
        list.setValueIndex(selectedIndex);
        list.setSummary(list.getEntry());
    }


    private boolean isK6neleImeEnabled() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        for (InputMethodInfo imi : imm.getEnabledInputMethodList()) {
            if (imi.getPackageName().equals(getPackageName())) {
                return true;
            }
        }

        return false;
    }
}