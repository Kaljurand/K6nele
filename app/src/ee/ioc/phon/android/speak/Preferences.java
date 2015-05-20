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
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.List;
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
        final MultiSelectListPreference mslp = (MultiSelectListPreference) mSettingsFragment.findPreference(getString(pref));

        RecognitionServiceManager mngr = new RecognitionServiceManager(this, combos, fallbackCombos);
        mngr.populateCombos(this, new RecognitionServiceManager.Listener() {
            @Override
            public void onComplete(List<String> combos, List<String> combosPp, Set<String> selectedCombos, List<String> selectedCombosPp) {
                mslp.setEntries(combosPp.toArray(new CharSequence[combosPp.size()]));
                mslp.setEntryValues(combos.toArray(new CharSequence[combos.size()]));
                mslp.setValues(selectedCombos);
                mslp.setSummary(TextUtils.join("\n", selectedCombosPp));
            }
        });
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
