/*
 * Copyright 2015-2016, Institute of Cybernetics at Tallinn University of Technology
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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.Toast;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.fragment.K6neleListFragment;
import ee.ioc.phon.android.speak.model.Rewrites;
import ee.ioc.phon.android.speak.utils.Utils;

// TODO: use CursorAdapter to be able to specify the filterting
// TODO: make it possible to select multiple rows to convert them to a new table and test in Kõnele
public class RewritesActivity extends Activity {

    public static final String EXTRA_NAME = "EXTRA_NAME";
    public static final String EXTRA_ERRORS = "EXTRA_ERRORS";
    private Rewrites mRewrites;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setRewrites(extras.getString(EXTRA_NAME), extras.getStringArray(EXTRA_ERRORS));
        getFragmentManager().beginTransaction().add(android.R.id.content, new RewritesFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
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
                                setRewrites(newName, null);
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

        Switch abSwitch = menu.findItem(R.id.menuRewritesToggle).getActionView().findViewById(R.id.abSwitch);
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

    private void setRewrites(String name, String[] errors) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Resources res = getResources();
        mRewrites = new Rewrites(prefs, res, name);

        int ruleCount = mRewrites.size();
        String subtitle = res.getQuantityString(R.plurals.statusLoadRewrites, ruleCount, ruleCount);

        if (errors != null) {
            int errorCount = errors.length;
            if (errorCount > 0) {
                String errorMessage = res.getQuantityString(R.plurals.statusLoadRewritesErrors, errorCount, errorCount);
                showErrors(errorMessage, errors);
                subtitle += " · " + errorMessage;
            }
        }
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(name);
            actionBar.setSubtitle(subtitle);
        }
    }

    private Rewrites getRewrites() {
        return mRewrites;
    }

    private void showErrors(String title, String[] errors) {
        Intent searchIntent = new Intent(this, RewritesErrorsActivity.class);
        searchIntent.putExtra(RewritesErrorsActivity.EXTRA_TITLE, title);
        searchIntent.putExtra(RewritesErrorsActivity.EXTRA_STRING_ARRAY, errors);
        startActivity(searchIntent);
    }

    public static class RewritesFragment extends K6neleListFragment implements SearchView.OnQueryTextListener {

        @Override
        public void onResume() {
            super.onResume();
            Rewrites rewrites = ((RewritesActivity) getActivity()).getRewrites();
            setListAdapter(new ArrayAdapter<>(getActivity(), R.layout.list_item_rewrite, rewrites.getRules()));
            getListView().setFastScrollEnabled(true);
            setEmptyView(getString(R.string.emptylistRewriteRules));
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