/*
 * Copyright 2015-2020, Institute of Cybernetics at Tallinn University of Technology
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

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NavUtils;
import androidx.core.app.TaskStackBuilder;
import androidx.preference.PreferenceManager;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.fragment.K6neleListFragment;
import ee.ioc.phon.android.speak.model.Rewrites;
import ee.ioc.phon.android.speak.utils.Utils;
import ee.ioc.phon.android.speechutils.editor.CommandMatcher;
import ee.ioc.phon.android.speechutils.editor.CommandMatcherFactory;

// TODO: use CursorAdapter to be able to specify the filtering
// TODO: make it possible to select multiple rows to convert them to a new table and test in Kõnele
public class RewritesActivity extends AppCompatActivity {

    public static final String EXTRA_NAME = "EXTRA_NAME";
    public static final String EXTRA_LOCALE = "EXTRA_LOCALE";
    public static final String EXTRA_APP = "EXTRA_APP";
    public static final String EXTRA_SERVICE = "EXTRA_SERVICE";
    private Rewrites mRewrites;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        /*
        // TODO: do we need this?
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        */
        setRewrites(extras.getString(EXTRA_NAME));
        String locale = extras.getString(EXTRA_LOCALE);
        String service = extras.getString(EXTRA_SERVICE);
        String app = extras.getString(EXTRA_APP);
        CommandMatcher commandMatcher = null;
        String emptyList;
        int resSubtitle = R.plurals.statusLoadRewrites;
        if (locale != null || service != null || app != null) {
            commandMatcher = CommandMatcherFactory.createCommandFilter(
                    locale,
                    service == null ? null : ComponentName.unflattenFromString(service),
                    app == null ? null : ComponentName.unflattenFromString(app)
            );

            emptyList = String.format(getString(R.string.emptylistRewriteRulesFiltered), Rewrites.ppComboMatcher(app, locale, service));
            resSubtitle = R.plurals.statusLoadRewritesFiltered;
        } else {
            emptyList = getString(R.string.emptylistRewriteRules);

        }
        getSupportFragmentManager().beginTransaction().add(android.R.id.content, new RewritesFragment(commandMatcher, emptyList, resSubtitle)).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // TODO: does not go up when this activity is launched from the clipboard
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // This activity is NOT part of this app's task, so create a new task
                    // when navigating up, with a synthesized back stack.
                    TaskStackBuilder.create(this)
                            // Add all of this activity's parents to the back stack
                            .addNextIntentWithParentStack(upIntent)
                            // Navigate up to the closest parent
                            .startActivities();
                } else {
                    // This activity is part of this app's task, so simply
                    // navigate up to the logical parent activity.
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
            case R.id.menuRewritesShare:
                startActivity(Intent.createChooser(mRewrites.getSendIntent(), getResources().getText(R.string.labelRewritesShare)));
                return true;
            case R.id.menuRewritesSendBase64:
                startActivity(mRewrites.getIntentSendBase64());
                return true;
            case R.id.menuRewritesTest:
                startActivity(mRewrites.getK6neleIntent());
                return true;
            case R.id.menuRewritesRename:
                Utils.getTextEntryDialog(
                        this,
                        getString(R.string.confirmRename),
                        mRewrites.getId(),
                        newName -> {
                            if (!newName.isEmpty()) {
                                mRewrites.rename(newName);
                                setRewrites(newName);
                            }
                        }
                ).show();
                return true;
            case R.id.menuRewritesDelete:
                Utils.getYesNoDialog(
                        this,
                        String.format(getString(R.string.confirmDelete), mRewrites.getId()),
                        () -> {
                            mRewrites.delete();
                            toast(String.format(getString(R.string.toastDeleted), mRewrites.getId()));
                            finish();
                        }
                ).show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.rewrites, menu);
        // TODO: review (seems like a detour)
        MenuItem menuItem = menu.findItem(R.id.menuRewritesToggle);
        menuItem.setActionView(R.layout.ab_switch);
        SwitchCompat abSwitch = menuItem.getActionView().findViewById(R.id.abSwitch);
        abSwitch.setChecked(mRewrites.isSelected());
        abSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mRewrites.setSelected(isChecked);
            if (isChecked) {
                toast(String.format(getString(R.string.toastActivated), mRewrites.getId()));
            } else {
                toast(String.format(getString(R.string.toastDeactivated), mRewrites.getId()));
            }
        });

        /*
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchMenuItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setSubmitButtonEnabled(false);
        searchView.setOnQueryTextListener(mFragment);
        */
        return true;
    }

    protected void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void setRewrites(String name) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Resources res = getResources();
        mRewrites = new Rewrites(prefs, res, name);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(name);
        }
    }

    private Rewrites getRewrites() {
        return mRewrites;
    }

    public static class RewritesFragment extends K6neleListFragment implements SearchView.OnQueryTextListener {

        private final CommandMatcher mCommandMatcher;
        private final String mEmptyList;
        private final int mResSubtitle;

        RewritesFragment(CommandMatcher commandMatcher, String emptyList, int resSubtitle) {
            mCommandMatcher = commandMatcher;
            mEmptyList = emptyList;
            mResSubtitle = resSubtitle;
        }

        @Override
        public void onResume() {
            super.onResume();
            RewritesActivity activity = (RewritesActivity) getActivity();
            Rewrites rewrites = activity.getRewrites();
            setListAdapter(new ArrayAdapter<>(activity, R.layout.list_item_rewrite, rewrites.getRules(mCommandMatcher)));
            getListView().setFastScrollEnabled(true);
            int ruleCount = getListView().getAdapter().getCount();
            setEmptyView(mEmptyList);
            String subtitle = getResources().getQuantityString(mResSubtitle, ruleCount, ruleCount);
            activity.getSupportActionBar().setSubtitle(subtitle);
        }

        @Override
        public boolean onQueryTextSubmit(String s) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            ((ArrayAdapter) getListAdapter()).getFilter().filter(s);
            return true;
        }
    }
}