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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ee.ioc.phon.android.speak.model.Combo;
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
    private String mKeyMaxResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSettingsFragment = new SettingsFragment();

        mKeyMaxResults = getString(R.string.keyMaxResults);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content, mSettingsFragment).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.preferences_header, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuAbout:
                Intent searchIntent = new Intent(this, AboutActivity.class);
                startActivity(searchIntent);
                return true;

            default:
                return super.onContextItemSelected(item);
        }
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

        String maxResults = mPrefs.getString(mKeyMaxResults, getString(R.string.defaultMaxResults));
        updateSummaryInt(mSettingsFragment.findPreference(mKeyMaxResults), R.plurals.summaryMaxResults, maxResults);

        updateSummary(R.string.keyImeCombo, R.string.emptylistImeCombos);
        updateSummary(R.string.keyCombo, R.string.emptylistCombos);
    }


    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        Preference pref = mSettingsFragment.findPreference(key);
        if (pref instanceof ListPreference) {
            ListPreference lp = (ListPreference) pref;
            if (mKeyMaxResults.equals(key)) {
                updateSummaryInt(lp, R.plurals.summaryMaxResults, lp.getEntry().toString());
            } else {
                pref.setSummary(lp.getEntry());
            }
        }
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

    private void updateSummary(int key, int keyEmpty) {
        mSettingsFragment
                .findPreference(getString(key))
                .setSummary(makeSummary(key, keyEmpty));
    }

    private String makeSummary(int key, int keyEmpty) {
        Collection<String> values = PreferenceUtils.getPrefStringSet(mPrefs, getResources(), key);
        List<Combo> combos = new ArrayList<>();
        for (String value : values) {
            combos.add(new Combo(this, value));
        }
        if (combos.size() == 0) {
            return getString(keyEmpty);
        }
        Collections.sort(combos, Combo.SORT_BY_LANGUAGE);
        return TextUtils.join("\n", combos);
    }

    private void updateSummaryInt(Preference pref, int pluralsResource, String countAsString) {
        int count = Integer.parseInt(countAsString);
        pref.setSummary(getResources().getQuantityString(pluralsResource, count, count));
    }
}
