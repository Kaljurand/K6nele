package ee.ioc.phon.android.speak.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ee.ioc.phon.android.speak.Executable;
import ee.ioc.phon.android.speak.ExecutableString;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.adapter.RewritesAdapter;
import ee.ioc.phon.android.speak.fragment.K6neleListFragment;
import ee.ioc.phon.android.speak.model.Rewrites;
import ee.ioc.phon.android.speak.utils.Utils;
import ee.ioc.phon.android.speechutils.editor.UtteranceRewriter;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

public class RewritesSelectorActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RewritesSelectorFragment fragment = new RewritesSelectorFragment();
        getFragmentManager().beginTransaction().add(android.R.id.content, fragment).commit();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.rewrites_selector, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuRewritesAdd:
                Intent intent = new Intent(this, RewritesLoaderActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public static class RewritesSelectorFragment extends K6neleListFragment {

        private SharedPreferences mPrefs;
        private Resources mRes;

        @Override
        public void onCreate(Bundle icicle) {
            super.onCreate(icicle);
            mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            mRes = getResources();
        }

        @Override
        public void onResume() {
            super.onResume();
            initAdapter();
            ListView listView = getListView();
            registerForContextMenu(listView);
            setEmptyView(getString(R.string.emptylistRewrites));
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            super.onCreateContextMenu(menu, v, menuInfo);
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.cm_rewrites, menu);
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            final Rewrites rewrites = (Rewrites) getListView().getItemAtPosition(info.position);
            final String name = rewrites.getId();
            switch (item.getItemId()) {
                case R.id.cmRewritesActivate:
                    activate(rewrites);
                    initAdapter();
                    return true;
                case R.id.cmRewritesShare:
                    share(name);
                    return true;
                case R.id.cmRewritesRename:
                    Utils.getTextEntryDialog(
                            getActivity(),
                            getString(R.string.confirmRenameEntry),
                            name,
                            new ExecutableString() {
                                public void execute(String newName) {
                                    if (!newName.isEmpty()) {
                                        rename(name, newName);
                                        initAdapter();
                                    }
                                }
                            }
                    ).show();
                    return true;
                case R.id.cmRewritesDelete:
                    Utils.getYesNoDialog(
                            getActivity(),
                            String.format(getString(R.string.confirmDeleteEntry), name),
                            new Executable() {
                                public void execute() {
                                    delete(name);
                                    initAdapter();
                                }
                            }
                    ).show();
                    return true;
                default:
                    return super.onContextItemSelected(item);
            }
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            show((Rewrites) l.getItemAtPosition(position));
        }

        private void show(Rewrites rewritesTable) {
            String name = rewritesTable.getId();
            String rewrites = PreferenceUtils.getPrefMapEntry(mPrefs, mRes, R.string.keyRewritesMap, name);
            UtteranceRewriter ur = new UtteranceRewriter(rewrites);
            int count = ur.size();
            Intent intent = new Intent(getActivity(), RewritesActivity.class);
            intent.putExtra(DetailsActivity.EXTRA_TITLE, name + " · " + mRes.getQuantityString(R.plurals.statusLoadRewrites, count, count));
            intent.putExtra(DetailsActivity.EXTRA_STRING_ARRAY, ur.toStringArray());
            startActivity(intent);
        }

        private void initAdapter() {
            String currentDefault = PreferenceUtils.getPrefString(mPrefs, mRes, R.string.defaultRewritesName);
            List<String> rewritesIds = new ArrayList<>(PreferenceUtils.getPrefMapKeys(mPrefs, mRes, R.string.keyRewritesMap));
            List<Rewrites> rewritesTables = new ArrayList<>();
            for (String id : rewritesIds) {
                Rewrites rewrites = new Rewrites(id);
                rewrites.setSelected(id.equals(currentDefault));
                rewritesTables.add(rewrites);
            }
            Collections.sort(rewritesTables, Rewrites.SORT_BY_ID);
            RewritesAdapter adapter = new RewritesAdapter(this, rewritesTables);
            setListAdapter(adapter);
        }

        private void activate(Rewrites rewrites) {
            String name = rewrites.getId();
            String currentDefault = PreferenceUtils.getPrefString(mPrefs, mRes, R.string.defaultRewritesName);
            if (name.equals(currentDefault)) {
                PreferenceUtils.putPrefString(mPrefs, mRes, R.string.defaultRewritesName, null);
                toast(String.format(getString(R.string.toastDeactivated), name));
            } else {
                PreferenceUtils.putPrefString(mPrefs, mRes, R.string.defaultRewritesName, name);
                toast(String.format(getString(R.string.toastActivated), name));
            }
        }

        private void share(String name) {
            String rewrites = PreferenceUtils.getPrefMapEntry(mPrefs, mRes, R.string.keyRewritesMap, name);
            UtteranceRewriter ur = new UtteranceRewriter(rewrites);
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, ur.toTsv());
            intent.setType("text/tab-separated-values");
            startActivity(Intent.createChooser(intent, getResources().getText(R.string.labelRewritesShare)));
        }

        private void rename(String oldName, String newName) {
            String rewrites = PreferenceUtils.getPrefMapEntry(mPrefs, mRes, R.string.keyRewritesMap, oldName);
            PreferenceUtils.putPrefMapEntry(mPrefs, mRes, R.string.keyRewritesMap, newName, rewrites);
            delete(oldName);
        }

        private void delete(String name) {
            Set<String> deleteKeys = new HashSet<>();
            deleteKeys.add(name);
            PreferenceUtils.clearPrefMap(mPrefs, mRes, R.string.keyRewritesMap, deleteKeys);
        }
    }
}