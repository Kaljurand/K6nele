package ee.ioc.phon.android.speak.activity;

import android.app.Activity;
import android.app.ListFragment;
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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ee.ioc.phon.android.speak.R;
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

    public static class RewritesSelectorFragment extends ListFragment {

        private SharedPreferences mPrefs;
        private Resources mRes;
        private String[] mRewrites;

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
            // TODO
            //listView.setEmptyView(new TextView(this));
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
                                        ContextMenu.ContextMenuInfo menuInfo) {
            super.onCreateContextMenu(menu, v, menuInfo);
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.cm_rewrites, menu);
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            String name = (String) getListView().getItemAtPosition(info.position);
            switch (item.getItemId()) {
                case R.id.cmRewritesActivate:
                    activate(name);
                    return true;
                case R.id.cmRewritesShare:
                    toast("not implemented");
                    return true;
                case R.id.cmRewritesRename:
                    toast("not implemented");
                    return true;
                case R.id.cmRewritesDelete:
                    delete(name);
                    return true;
                default:
                    return super.onContextItemSelected(item);
            }
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            show((String) l.getItemAtPosition(position));
        }

        private void show(String name) {
            String rewrites = PreferenceUtils.getPrefMapEntry(mPrefs, mRes, R.string.keyRewritesMap, name);
            UtteranceRewriter ur = new UtteranceRewriter(rewrites);
            int count = ur.size();
            Intent intent = new Intent(getActivity(), RewritesActivity.class);
            intent.putExtra(DetailsActivity.EXTRA_TITLE, name + " · " + mRes.getQuantityString(R.plurals.statusLoadRewrites, count, count));
            intent.putExtra(DetailsActivity.EXTRA_STRING_ARRAY, ur.toStringArray());
            startActivity(intent);
        }

        private void initAdapter() {
            List<String> keysSorted = new ArrayList<>();
            for (String key : PreferenceUtils.getPrefMapKeys(mPrefs, mRes, R.string.keyRewritesMap)) {
                if (!key.isEmpty()) {
                    keysSorted.add(key);
                }
            }
            Collections.sort(keysSorted);

            mRewrites = keysSorted.toArray(new String[0]);

            setListAdapter(new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_list_item_1, android.R.id.text1, mRewrites));
        }

        private void activate(String name) {
            String currentDefault = PreferenceUtils.getPrefString(mPrefs, mRes, R.string.defaultRewritesName);
            if (name.equals(currentDefault)) {
                PreferenceUtils.putPrefString(mPrefs, mRes, R.string.defaultRewritesName, null);
                toast("Deactivated " + name);
            } else {
                PreferenceUtils.putPrefString(mPrefs, mRes, R.string.defaultRewritesName, name);
                toast("Activated " + name);
            }
        }

        private void delete(String name) {
            Set<String> deleteKeys = new HashSet<>();
            deleteKeys.add(name);
            PreferenceUtils.clearPrefMap(mPrefs, mRes, R.string.keyRewritesMap, deleteKeys);
            toast("Deleted " + name);
        }

        private void toast(String message) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
        }
    }
}