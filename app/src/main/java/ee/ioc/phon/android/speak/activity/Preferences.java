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

package ee.ioc.phon.android.speak.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
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

import ee.ioc.phon.android.speak.AboutActivity;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.model.Combo;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

/**
 * <p>Preferences activity. Updates some preference-summaries automatically,
 * if the user changes a preference.</p>
 *
 * @author Kaarel Kaljurand
 */
public class Preferences extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
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
            case R.id.menuHelp:
                Intent view = new Intent(Intent.ACTION_VIEW);
                view.setData(Uri.parse(getString(R.string.urlDoc)));
                startActivity(view);
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }


    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private String mKeyMaxResults;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            mKeyMaxResults = getString(R.string.keyMaxResults);
        }


        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }


        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            // Remove the shortcut to the system-wide recognizer settings which does not
            // exist pre Android v5.
            // TODO: also remove it on Wear
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                PreferenceCategory category = (PreferenceCategory) findPreference(getString(R.string.keyCategoryDependencies));
                Preference pref = category.findPreference(getString(R.string.keySystemVoiceInputSettings));
                if (pref != null) {
                    category.removePreference(pref);
                }
            }

            // If the K6nele IME is enabled then we remove the link to the IME settings,
            // if not already removed.
            // If the IME is not enabled then we add the link. The position of the link seems
            // to respect the position in preferences.xml.
            PreferenceScreen prefScreen = getPreferenceScreen();
            PreferenceCategory category = (PreferenceCategory) prefScreen.findPreference(getString(R.string.keyCategoryIme));
            Preference pref = prefScreen.findPreference(getString(R.string.keyEnableIme));

            if (isK6neleImeEnabled()) {
                category.setEnabled(true);
                if (pref != null) {
                    prefScreen.removePreference(pref);
                }
            } else {
                category.setEnabled(false);
                prefScreen.addPreference(pref);
            }

            SharedPreferences sp = prefScreen.getSharedPreferences();
            String maxResults = sp.getString(mKeyMaxResults, getString(R.string.defaultMaxResults));
            updateSummaryInt(findPreference(mKeyMaxResults), R.plurals.summaryMaxResults, maxResults);

            Preference prefImeMode = findPreference(getString(R.string.keyImeMode));
            prefImeMode.setSummary(((ListPreference) prefImeMode).getEntry());

            updateSummary(R.string.keyImeCombo, R.string.emptylistImeCombos);
            updateSummary(R.string.keyCombo, R.string.emptylistCombos);
        }


        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            Preference pref = findPreference(key);
            if (pref instanceof ListPreference) {
                ListPreference lp = (ListPreference) pref;
                if (mKeyMaxResults.equals(key)) {
                    updateSummaryInt(lp, R.plurals.summaryMaxResults, lp.getEntry().toString());
                } else {
                    pref.setSummary(lp.getEntry());
                }
            }
        }

        private void updateSummary(int key, int keyEmpty) {
            findPreference(getString(key)).setSummary(makeSummary(key, keyEmpty));
        }

        private String makeSummary(int key, int keyEmpty) {
            SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
            Collection<String> values = PreferenceUtils.getPrefStringSet(sp, getResources(), key);
            List<Combo> combos = new ArrayList<>();
            for (String value : values) {
                combos.add(new Combo(getActivity(), value));
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

        private boolean isK6neleImeEnabled() {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

            for (InputMethodInfo imi : imm.getEnabledInputMethodList()) {
                if (imi.getPackageName().equals(getActivity().getPackageName())) {
                    return true;
                }
            }

            return false;
        }
    }
}