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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.Toast;

import ee.ioc.phon.android.speak.Executable;
import ee.ioc.phon.android.speak.ExecutableString;
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Resources res = getResources();
        Bundle extras = getIntent().getExtras();
        String name = extras.getString(EXTRA_NAME);
        String[] errors = extras.getStringArray(EXTRA_ERRORS);
        mRewrites = new Rewrites(prefs, res, name);

        String subtitle = "";
        if (name != null) {
            subtitle = name;
        }
        int ruleCount = mRewrites.getRules().length;
        subtitle += " · " + res.getQuantityString(R.plurals.statusLoadRewrites, ruleCount, ruleCount);

        if (errors != null) {
            int errorCount = errors.length;
            if (errorCount > 0) {
                String errorMessage = res.getQuantityString(R.plurals.statusLoadRewritesErrors, errorCount, errorCount);
                showErrors(errorMessage, errors);
                subtitle += " · " + errorMessage;
            }
        }
        ActionBar actionBar = getActionBar();
        if (actionBar != null && !subtitle.isEmpty()) {
            actionBar.setSubtitle(subtitle);
        }

        getFragmentManager().beginTransaction().add(android.R.id.content, new RewritesFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuRewritesShare:
                startActivity(Intent.createChooser(mRewrites.getSendIntent(), getResources().getText(R.string.labelRewritesShare)));
                return true;
            case R.id.menuRewritesTest:
                startActivity(mRewrites.getK6neleIntent());
                return true;
            case R.id.menuRewritesRename:
                Utils.getTextEntryDialog(
                        this,
                        getString(R.string.confirmRename),
                        mRewrites.getId(),
                        new ExecutableString() {
                            public void execute(String newName) {
                                if (!newName.isEmpty()) {
                                    mRewrites.rename(newName);
                                }
                            }
                        }
                ).show();
                return true;
            case R.id.menuRewritesDelete:
                Utils.getYesNoDialog(
                        this,
                        String.format(getString(R.string.confirmDelete), mRewrites.getId()),
                        new Executable() {
                            public void execute() {
                                mRewrites.delete();
                            }
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

        Switch abSwitch = (Switch) menu.findItem(R.id.menuRewritesToggle).getActionView().findViewById(R.id.abSwitch);
        abSwitch.setChecked(mRewrites.isSelected());
        abSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mRewrites.toggle()) {
                    toast(String.format(getString(R.string.toastActivated), mRewrites.getId()));
                } else {
                    toast(String.format(getString(R.string.toastDeactivated), mRewrites.getId()));
                }
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

    private Rewrites getRewrites() {
        return mRewrites;
    }

    private void showErrors(String title, String[] errors) {
        Intent searchIntent = new Intent(this, DetailsActivity.class);
        searchIntent.putExtra(DetailsActivity.EXTRA_TITLE, title);
        searchIntent.putExtra(DetailsActivity.EXTRA_STRING_ARRAY, errors);
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