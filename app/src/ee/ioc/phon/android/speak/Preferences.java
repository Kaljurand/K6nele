/*
 * Copyright 2011-2015, Institute of Cybernetics at Tallinn University of Technology
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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;

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
        populateRecognitionServices();

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


    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        Preference pref = findPreference(key);
        //ListPreference pref = (ListPreference) mSettingsFragment.findPreference(key);
        if (pref instanceof ListPreference) {
            ListPreference lp = (ListPreference) pref;
            pref.setSummary(lp.getEntry());
            if (key.equals(getString(R.string.keyImeRecognitionService))) {
                // Populate the languages available under this service
                populateLanguages(lp.getValue());
            } else if (key.equals(getString(R.string.keyImeRecognitionLanguage))) {
                // Nothing to do
            }
        }
    }


    private void populateRecognitionServices() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Hardcoded "preferred service"
        String preferredService = getString(R.string.defaultImeRecognizerService);
        // Currently selected service (identified by class name)
        String selectedService = Utils.getPrefString(prefs, getResources(), R.string.keyImeRecognitionService);
        RecognitionServiceManager mngr = new RecognitionServiceManager(this, preferredService, selectedService);

        ListPreference list = (ListPreference) findPreference(getString(R.string.keyImeRecognitionService));
        list.setEntries(mngr.getEntries());
        list.setEntryValues(mngr.getEntryValues());
        list.setValueIndex(mngr.getSelectedIndex());
        list.setSummary(list.getEntry());
        populateLanguages(list.getValue());
    }

    /**
     * TODO
     */
    private void populateLanguages(String selectedRecognizerService) {
        ListPreference languageList = (ListPreference) findPreference(getString(R.string.keyImeRecognitionLanguage));
        updateSupportedLanguages(selectedRecognizerService, languageList);
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


    /**
     * Note: According to the <code>BroadcastReceiver</code> documentation,
     * setPackage is respected only on ICS and later.
     *
     * Send a broadcast to find out what is the language preference of
     * the speech recognizer service that matches the intent.
     * The expectation is that only one service matches this intent.
     *
     * TODO: if the Kõnele Ws service is selected (either directly or as a system default) then
     * clear the language selection and disable it. This is a temporary solution until
     * the Ws-service start supporting more languages.
     *
     * @param selectedRecognizerService name of the app that is the only one to receive the broadcast
     */
    private void updateSupportedLanguages(String selectedRecognizerService, final ListPreference languageList) {
        Log.i("Selected service: " + selectedRecognizerService);
        Intent intent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);

        // TODO: do not set package in case of "system default"
        // TODO: how to specify the component (one package can contain different components with
        // different capabilities)
        intent.setPackage(Utils.getComponentName(selectedRecognizerService).getPackageName());
        //intent.setComponent(Utils.getComponentName(selectedRecognizerService));

        // This is needed to include newly installed apps or stopped apps
        // as receivers of the broadcast.
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        final String selectedLang = languageList.getValue();
        sendOrderedBroadcast(intent, null, new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                if (getResultCode() != Activity.RESULT_OK) {
                    // TODO: handle this error
                    //toast(getString(R.string.errorNoDefaultRecognizer));
                    return;
                }

                Bundle results = getResultExtras(true);

                // Supported languages
                String prefLang = results.getString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE);
                ArrayList<CharSequence> allLangs = results.getCharSequenceArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);

                Log.i("Supported langs: " + prefLang + ": " + allLangs);

                if (allLangs == null) {
                    allLangs = new ArrayList<>();
                }

                // Make sure we don't end up with an empty list of languages
                if (allLangs.isEmpty()) {
                    if (prefLang == null) {
                        allLangs.add(getString(R.string.defaultRecognitionLanguage));
                    } else {
                        allLangs.add(prefLang);
                    }
                }

                // Populate the entry values with the supported languages
                CharSequence[] entryValues = allLangs.toArray(new CharSequence[allLangs.size()]);
                languageList.setEntryValues(entryValues);

                // Populate the entries with human-readable language names
                CharSequence[] entries = new CharSequence[allLangs.size()];
                for (int i = 0; i < allLangs.size(); i++) {
                    String ev = entryValues[i].toString();
                    entries[i] = Utils.makeLangLabel(ev);
                }
                languageList.setEntries(entries);

                // Set the selected item
                if (allLangs.contains(selectedLang)) {
                    languageList.setValue(selectedLang);
                } else if (prefLang != null) {
                    languageList.setValue(prefLang);
                } else {
                    languageList.setValueIndex(0);
                }

                // Update the summary to show the selected value.
                // This needs to be done onResume and also when the service changes (handled elsewhere).
                languageList.setSummary(languageList.getEntry());

            }
        }, null, Activity.RESULT_OK, null, null);
    }

}