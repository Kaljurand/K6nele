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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.Set;

import ee.ioc.phon.android.speak.utils.PreferenceUtils;

/**
 * <p>Preferences activity. Updates some preference-summaries automatically,
 * if the user changes a preference.</p>
 *
 * @author Kaarel Kaljurand
 */
public class Preferences extends Activity implements OnSharedPreferenceChangeListener {

    private SettingsFragment mSettingsFragment;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSettingsFragment = new SettingsFragment();
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content, mSettingsFragment).commit();
    }


    @Override
    protected void onPause() {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        populateRecognitionServiceLanguageList(R.string.keyImeRecognitionServiceLanguage, R.array.defaultImeRecognizerServiceLanguage);

        // If the K6nele IME is enabled then we remove the link to the IME settings,
        // if not already removed.
        // If the IME is not enabled then we add the link. The position of the link seems
        // to respect the position in preferences.xml.
        if (isK6neleImeEnabled()) {
            PreferenceCategory category = (PreferenceCategory) mSettingsFragment.findPreference(getString(R.string.keyCategoryIme));
            Preference pref = category.findPreference(getString(R.string.keyEnableIme));
            if (pref != null) {
                category.removePreference(pref);
            }
        } else {
            Preference pref = mSettingsFragment.findPreference(getString(R.string.keyEnableIme));
            PreferenceCategory category = (PreferenceCategory) mSettingsFragment.findPreference(getString(R.string.keyCategoryIme));
            category.addPreference(pref);
        }
    }


    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        Preference pref = mSettingsFragment.findPreference(key);
        if (pref instanceof ListPreference) {
            ListPreference lp = (ListPreference) pref;
            pref.setSummary(lp.getEntry());
        } else if (pref instanceof MultiSelectListPreference) {
            MultiSelectListPreference mslp = (MultiSelectListPreference) pref;
            // TODO: pretty-print
            pref.setSummary(TextUtils.join("\n", mslp.getValues()));
        }
    }

    private void populateRecognitionServiceLanguageList(int pref, int fallbackCombos) {
        Set<String> combos = PreferenceUtils.getPrefStringSet(mPrefs, getResources(), pref);
        RecognitionServiceManager mngr = new RecognitionServiceManager(this, combos, fallbackCombos);

        MultiSelectListPreference mslp = (MultiSelectListPreference) mSettingsFragment.findPreference(getString(pref));
        mslp.setEntries(mngr.getEntries());
        mslp.setEntryValues(mngr.getEntryValues());
        mslp.setValues(mngr.getValues());
        // TODO: pretty-print
        mslp.setSummary(TextUtils.join("\n", mslp.getValues()));
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
     * Send a broadcast to find out what is the language preference of
     * the speech recognizer service that matches the intent.
     * The expectation is that only one service matches this intent.
     * (Note: According to the {@link BroadcastReceiver} documentation,
     * setPackage is respected only on ICS and later.)
     * <p/>
     * The input specifies the service to be queries (by a flattened component name). If the name
     * is empty then we query the system default recognizer.
     *
     * @param selectedRecognizerService name of the app that is the only one to receive the broadcast
     */
    private void updateSupportedLanguages(String selectedRecognizerService, final ListPreference languageList) {
        boolean isSystemDefault = false;
        if (selectedRecognizerService.length() == 0) {
            isSystemDefault = true;
            // TODO: could not access it via Settings.Secure.VOICE_RECOGNITION_SERVICE
            String serviceType = "voice_recognition_service";
            selectedRecognizerService = Settings.Secure.getString(getContentResolver(), serviceType);
        }

        Log.i("Selected service (system default = " + isSystemDefault + "): " + selectedRecognizerService);

        // If Kõnele is selected then show only "et-ee" as the available language.
        // TODO: this is a temporary solution
        if (ComponentName.unflattenFromString(selectedRecognizerService).getPackageName().equals(getPackageName())) {
            String defaultLanguage = getString(R.string.defaultRecognitionLanguage);
            CharSequence[] entryValues = {defaultLanguage};
            CharSequence[] entries = {Utils.makeLangLabel(defaultLanguage)};
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        // TODO: this seems to be only for activities that implement ACTION_WEB_SEARCH
        //Intent intent = RecognizerIntent.getVoiceDetailsIntent(this);

        ComponentName serviceComponent = ComponentName.unflattenFromString(selectedRecognizerService);
        if (serviceComponent != null) {
            intent.setPackage(serviceComponent.getPackageName());
            // TODO: ideally we would like to query the component, because the package might
            // contain services (= components) with different capabilities.
            //intent.setComponent(serviceComponent);
        }

        // This is needed to include newly installed apps or stopped apps
        // as receivers of the broadcast.
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

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

                // We add the preferred language to the list of supported languages.
                // Normally it should be there anyway.
                if (prefLang != null && !allLangs.contains(prefLang)) {
                    allLangs.add(prefLang);
                }

                // Populate the entry values with the supported languages
                CharSequence[] entryValues = allLangs.toArray(new CharSequence[allLangs.size()]);

                // Populate the entries with human-readable language names
                CharSequence[] entries = new CharSequence[allLangs.size()];
                for (int i = 0; i < allLangs.size(); i++) {
                    String ev = entryValues[i].toString();
                    entries[i] = Utils.makeLangLabel(ev);
                }
            }
        }, null, Activity.RESULT_OK, null, null);
    }
}
